package com.jtrack.receiver;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.levkhomich.akka.tracing.TracingExtension;
import com.github.levkhomich.akka.tracing.TracingExtensionImpl;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jtrack.pojo.*;
import com.jtrack.pojo.ManageGates;
import com.jtrack.util.ConnectionUtil;
import com.jtrack.util.RedisUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalTime;

import org.squbs.actorregistry.japi.ActorLookup;
import scala.concurrent.duration.FiniteDuration;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class New_J_Entity extends AbstractActor{
    final ActorLookup<?> lookup = ActorLookup.create(context());
    LoggingAdapter log = Logging.getLogger(context().system(), this);
    private Cancellable scheduler;
    //FINLRJoinData data = null;
    Config jtrackConfig;
    TracingExtensionImpl trace;
    String logTopic;
    boolean doesUniquenessKeyExistInCache = false;
    String producerTopic;
    public boolean verifyMandatoryParameter;
    private HashMap<String, String> finaldata;
    private ManageGates mangegates = null;
    String ts;
    private String username = null;
    private String password = null;
    private String host = null;
    private Boolean gateStatus = false;
    private Date gate_ls_date = null;
    private Date gate_bs_date = null;
    private LocalTime gate_start_time = null;
    private LocalTime gate_end_time = null;
    private Boolean is_fail=false;
    private Boolean is_train_gate=false;
    private Boolean is_train_handle=false;

    @Override
    public void preStart() {
        // This will schedule a poision Pill to itself with duration as per configuration
        log.debug("PreStart Invoked with TimeWindow for " + jtrackConfig.getInt("jtrack.kafka.consumer.cluster.duration") + " seconds.");
        scheduler = context().system().scheduler().scheduleOnce(
                FiniteDuration.create(jtrackConfig.getInt("jtrack.kafka.consumer.cluster.duration"), TimeUnit.SECONDS), getSelf(), PoisonPill.getInstance(), context().dispatcher(), getSelf());

        //ts = rtrsConfig.getString("rtrs.ignite.Ts.kafka");
    }

    @Override
    public void postStop() throws Exception {
        log.debug("PostStop Invoked for Jtrack");
        scheduler.cancel();
        super.postStop();
    }

    public New_J_Entity() {
        trace = (TracingExtensionImpl) TracingExtension.apply(context().system());
        jtrackConfig = ConfigFactory.load();
        host = jtrackConfig.getString("jtrack.database.host");
        username = jtrackConfig.getString("jtrack.database.username");
        password = jtrackConfig.getString("jtrack.database.password");
        //logTopic = rtrsConfig.getString("rtrs.kafka.log_topic.topic");
       // producerTopic = rtrsConfig.getString("rtrs.kafka.producer_finlr.topic");
        //verifyMandatoryParameter = rtrsConfig.getBoolean("rtrs.mandatoryParameters.verify");
        finaldata = new HashMap<String, String>();

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Gate.class, msg -> {
                    try{
                       //String [] spl = msg.getGateId().split("-");


                         if(mangegates==null){
                             initgate(msg.getGateNum(),msg.getBoom1Id());
                             if(mangegates==null){
                                 log.error("Failed to initialize gate - gateNum: {}, boom1Id: {}. Cannot process message.", msg.getGateNum(), msg.getBoom1Id());
                                 return;
                             }
                             System.out.println("*******initiated********"+mangegates.toString());
                         }

                        if(msg.getBatch().equals("Train")){
                           // checkFailure();
                            is_train_gate =true;
                            is_train_handle=true;
                            gateStatus=false;
                            log.info("*******Double Train********"+mangegates.toString());
                            if (mangegates.getBs1Status().equals("closed")) {
                                mangegates.setBs1Status("closed");
                                    log.info("*******Double Train Boom updated ********"+msg.getBoom1Id());
                                    updateboomstaus("closed", msg.getBoom1Id());
                                     is_train_gate =false;
                            }
                            if((mangegates.getLeverStatus().equals("closed"))){
                                log.info("*******Double Train Boom updated********"+msg.getBoom1Id());
                                updatelevelstatus("closed", msg.getBoom1Id());
                                is_train_handle =false;
                            }

                        }
                        else {
                            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
                            Date date = new Date();
                            String currenttime = parser.format(date);
                            LocalTime time = LocalTime.parse(currenttime);
                            //J_Message pm = new J_Message(mangegates.getSM(), mangegates.getGateNum(),"closed");
                            //  lookup.tell(pm, getSelf());
                            if (msg.getBoom1Id().indexOf("BS") != -1 && msg.getBoom1Id().indexOf("BS2") == -1) {
                                log.info("*******BSGate********"+msg.getBoom1Id()+mangegates.toString());
                                gate_bs_date = java.util.Calendar.getInstance().getTime();
                                log.info("Update TIme for Gate BS  " + msg.getGateNum() + gate_bs_date);
                                int n = Integer.parseInt(mangegates.getBs1Go());
                                Integer bs_min_value = Integer.parseInt(msg.getG_value().toString());
                                if (bs_min_value >= n) {
                                    int i = 0;
                                    if (mangegates.getBs1Status().equals("closed")) {
                                        i = 1;
                                        mangegates.setBs1Status("open");
                                        updateboomstaus("open", msg.getBoom1Id());

                                    }
                                    mangegates.setBs1Status("open");
                                }
                                int j = Integer.parseInt(mangegates.getBs1Gc());

                                if (bs_min_value <= j) {
                                    String previousStatus = mangegates.getBs1Status();
                                    if (mangegates.getBs1Status().equals("open")) {
                                        mangegates.setBs1Status("closed");
                                        updateboomstaus("closed", msg.getBoom1Id());
                                        log.info("*******Message updated - gate transitioned from open to closed********"+msg.getBoom1Id());
                                    } else if (mangegates.getBs1Status().equals("closed")) {
                                        // Gate is already closed, but we should still update to ensure reports are checked/inserted
                                        // This handles the case where all sensors need to be verified as closed
                                        mangegates.setBs1Status("closed");
                                        updateboomstaus("closed", msg.getBoom1Id());
                                        log.info("*******Message updated - gate already closed, verifying all sensors********"+msg.getBoom1Id());
                                    }
                                }

                            }
                            
                            // Process BS2 sensor (threshold-based, similar to BS1)
                            if (msg.getBoom1Id().indexOf("BS2") != -1 && msg.getBs2_value() != null && !msg.getBs2_value().isEmpty()) {
                                log.info("*******BS2Gate********"+msg.getBoom1Id());
                                try {
                                    String sql = "select BS2_GO, BS2_GC from managegates where BOOM1_ID=?";
                                    JdbcTemplate jdbcTemplate1 = ConnectionUtil.getDataSource(host,username,password);
                                    List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, msg.getBoom1Id());
                                    if (rows.size() > 0) {
                                        Map row = rows.get(0);
                                        String bs2Go = (String) row.get("BS2_GO");
                                        String bs2Gc = (String) row.get("BS2_GC");
                                        if (bs2Go != null && !bs2Go.isEmpty() && bs2Gc != null && !bs2Gc.isEmpty()) {
                                            int n = Integer.parseInt(bs2Go);
                                            Integer bs2_min_value = Integer.parseInt(msg.getBs2_value());
                                            if (bs2_min_value >= n) {
                                                jdbcTemplate1.update("update managegates set BS2_STATUS=? where BOOM1_ID=?", new Object[] {"open", msg.getBoom1Id()});
                                                log.info("BS2 status updated to open");
                                            }
                                            int j = Integer.parseInt(bs2Gc);
                                            if (bs2_min_value <= j) {
                                                jdbcTemplate1.update("update managegates set BS2_STATUS=? where BOOM1_ID=?", new Object[] {"closed", msg.getBoom1Id()});
                                                log.info("BS2 status updated to closed");
                                            }
                                            // Update gate status after BS2 change
                                            updateGatestatus(jdbcTemplate1, mangegates.getBs1Status(), mangegates.getLeverStatus(), msg.getBoom1Id());
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("Error processing BS2 sensor for gate: {}", msg.getBoom1Id(), e);
                                }
                            }
                            
                            // Process LT sensor (binary: 0 or 1)
                            if (msg.getBoom1Id().indexOf("LT") != -1 && msg.getLt_value() != null && !msg.getLt_value().isEmpty()) {
                                log.info("*******LTGate********"+msg.getBoom1Id());
                                try {
                                    JdbcTemplate jdbcTemplate1 = ConnectionUtil.getDataSource(host,username,password);
                                    Integer ltValue = Integer.parseInt(msg.getLt_value());
                                    
                                    // LT is binary: value == 1 means closed, value == 0 means open
                                    if (ltValue == 1) {
                                        jdbcTemplate1.update("update managegates set LT_STATUS=? where BOOM1_ID=?", new Object[] {"closed", msg.getBoom1Id()});
                                        log.info("LT status updated to closed (binary: value=1)");
                                    } else {
                                        jdbcTemplate1.update("update managegates set LT_STATUS=? where BOOM1_ID=?", new Object[] {"open", msg.getBoom1Id()});
                                        log.info("LT status updated to open (binary: value=0)");
                                    }
                                    // Update gate status after LT change
                                    updateGatestatus(jdbcTemplate1, mangegates.getBs1Status(), mangegates.getLeverStatus(), msg.getBoom1Id());
                                } catch (Exception e) {
                                    log.error("Error processing LT sensor for gate: {}", msg.getBoom1Id(), e);
                                }
                            }

                            if (msg.getBoom1Id().indexOf("LS") != -1) {
                                log.info("*******LSGate********"+msg.getBoom1Id());
                                gate_ls_date = java.util.Calendar.getInstance().getTime();
                                log.info("Update TIme for Gate BS  " + msg.getBoom1Id() + gate_ls_date);

                                int n = Integer.parseInt(mangegates.getLsGo());
                                Integer ls_min_value = Integer.parseInt(msg.getL_value().toString());

                                if (ls_min_value <= n) {
                                    int i = 0;
                                    if (mangegates.getLeverStatus().equals("closed")) {
                                        i = 1;
                                        mangegates.setLeverStatus("open");
                                        updatelevelstatus("open", msg.getBoom1Id());
                                    }
                                    mangegates.setLeverStatus("open");
                                }
                                int j = Integer.parseInt(mangegates.getLsGc());
                                if (ls_min_value >= j) {
                                    if (mangegates.getLeverStatus().equals("open")) {
                                        mangegates.setLeverStatus("closed");
                                        updatelevelstatus("closed", msg.getBoom1Id());
                                        log.info("*******Message updated - handle transitioned from open to closed********"+msg.getBoom1Id());
                                    } else if (mangegates.getLeverStatus().equals("closed")) {
                                        // Handle is already closed, but we should still update to ensure reports are checked/inserted
                                        // This handles the case where all sensors need to be verified as closed
                                        mangegates.setLeverStatus("closed");
                                        updatelevelstatus("closed", msg.getBoom1Id());
                                        log.info("*******Message updated - handle already closed, verifying all sensors********"+msg.getBoom1Id());
                                    }
                                }
                            }
                        }
                    }catch(Exception e){
                        log.info("Failure parsing or issue in Jtrack " +e.getMessage());
                    }

                })
                .build();
    }

    public void checkFailure() throws  SQLException{

        JdbcTemplate  jdbcTemplate1 = ConnectionUtil.getDataSource(host,username,password);
        String pattern = "yyyy-MM-dd hh:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date date1 = new Date();
        String date_str_1 = simpleDateFormat.format(date1);
        Boolean is_fail =getgateStatus(mangegates.getBoom1Id(),jdbcTemplate1,date_str_1);

    }

    public void updateFailSafe(String id,Boolean status,String str_date) throws  SQLException {
        JdbcTemplate  jdbcTemplate1 = ConnectionUtil.getDataSource(host,username,password);
        log.info("updated status to"+ id+ "+failstatus");
        if(status) {
            int k = jdbcTemplate1.update("update managegates set status = ?,is_failsafe = ? where BOOM1_ID = ? OR handle = ?", new Object[] { "Open", id ,id,status});
        }else{
            int i = jdbcTemplate1.update("update managegates set is_failsafe = ?,added_on = ? where BOOM1_ID = ? OR handle = ?", new Object[] { String.valueOf(status), id ,id,str_date});
        }
    }

    public  boolean isBetween(LocalTime candidate, LocalTime start, LocalTime end) {

        return !candidate.isBefore(start) && !candidate.isAfter(end);  // Inclusive.
    }
    public void updateboomstaus(String status,String gate) throws  SQLException{
        JdbcTemplate  jdbcTemplate1 = ConnectionUtil.getDataSource(host,username,password);
        jdbcTemplate1.update("update managegates set BS1_STATUS=? where BOOM1_ID=?",new Object[] {status,gate});
        log.info("updated boom"+mangegates.toString());
        updateGatestatus(jdbcTemplate1,mangegates.getBs1Status(),mangegates.getLeverStatus(),gate);
        log.info("gate status updated"+gate);
    }
    public void updatelevelstatus(String status,String gate) throws SQLException{

        JdbcTemplate  jdbcTemplate1 = ConnectionUtil.getDataSource(host,username,password);
        jdbcTemplate1.update("update managegates set LEVER_STATUS=? where handle=?",new Object[] {status,gate});
        log.info("updated lever"+mangegates.toString());
        updateGatestatus(jdbcTemplate1,mangegates.getBs1Status(),mangegates.getLeverStatus(),gate);
        log.info("gate status updated"+gate);
    }
    public void updategatestatus(){}

    public void initgate(String gatename,String boom1Id){
        mangegates = new ManageGates();
        try {
            String[] parts = gatename.split("-");
            //gatename = parts[1];
            String sql = "select * from managegates where Gate_Num=? and (BOOM1_ID=? OR handle=?)";
            JdbcTemplate jdbcTemplate1 = ConnectionUtil.getDataSource(host,username,password);
            log.info("InPrestate"+context().self().hashCode());
            List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, new Object[] {gatename,boom1Id,boom1Id});
            log.info("Inside" + rows.size());
            
            // If not found and boom1Id ends with "1" (e.g., E20-750BS1), try without the "1" (E20-750BS)
            if ((rows == null || rows.isEmpty()) && boom1Id != null && boom1Id.endsWith("1") && boom1Id.indexOf("BS") != -1 && boom1Id.indexOf("BS2") == -1) {
                String boom1IdWithoutOne = boom1Id.substring(0, boom1Id.length() - 1);
                log.info("Trying alternative lookup without trailing '1': {} -> {}", boom1Id, boom1IdWithoutOne);
                rows = jdbcTemplate1.queryForList(sql, new Object[] {gatename, boom1IdWithoutOne, boom1IdWithoutOne});
                log.info("Inside (alternative)" + rows.size());
                if (rows != null && !rows.isEmpty()) {
                    boom1Id = boom1IdWithoutOne; // Use the found boom1Id for subsequent operations
                    log.info("Found gate using alternative ID: {}", boom1Id);
                }
            }
            
            if (rows == null || rows.isEmpty()) {
                log.error("Gate not found in managegates table - Gate_Num: {}, BOOM1_ID: {}. Cannot initialize gate.", gatename, boom1Id);
                log.info("Failure parsing or issue in Jtrack Index: 0, Size: 0");
                mangegates = null;
                return;
            }
            
            Map row = rows.get(0);
            mangegates.setBs1Go((String) row.get("BS1_GO"));
            mangegates.setBs1Gc((String) row.get("BS1_GC"));
            mangegates.setLsGc((String) row.get("LS_GC"));
            mangegates.setLsGo((String) row.get("LS_GO"));
            mangegates.setBoom1Id((String) row.get("BOOM1_ID"));
            mangegates.setGateNum((String) row.get("Gate_Num"));
            mangegates.setSM((String) row.get("SM"));
            mangegates.setGM((String) row.get("GM"));
            mangegates.setBs1Status((String) row.get("BS1_STATUS"));
            mangegates.setLeverStatus((String) row.get("LEVER_STATUS"));
            mangegates.setStatus((String) row.get("status"));
            //mangegates.setGate_start_time((String) row.get("starttime"));
            mangegates.setGate_start_time("00:00");
           // mangegates.setGate_end_time((String) row.get("endtime"));
            mangegates.setGate_end_time("23:59");
           // mangegates.setIs_failsafe(Boolean.parseBoolean((String)row.get("is_failsafe")));
            mangegates.setIs_failsafe(false);
            
            // Handle different date types from database (Date, Timestamp, LocalDateTime)
            Date date = null;
            Object addedOnValue = row.get("added_on");
            if (addedOnValue instanceof Date) {
                date = (Date) addedOnValue;
            } else if (addedOnValue instanceof java.sql.Timestamp) {
                date = new Date(((java.sql.Timestamp) addedOnValue).getTime());
            } else if (addedOnValue instanceof java.time.LocalDateTime) {
                java.time.LocalDateTime ldt = (java.time.LocalDateTime) addedOnValue;
                date = java.sql.Timestamp.valueOf(ldt);
            } else if (addedOnValue != null) {
                // Try to parse as string if it's a string representation
                try {
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    date = format.parse(addedOnValue.toString());
                } catch (Exception e) {
                    log.warning("Could not parse added_on value: {}", addedOnValue);
                    date = new Date(); // Use current date as fallback
                }
            } else {
                date = new Date(); // Use current date if null
            }

            Calendar cal=Calendar.getInstance();
            DateFormat format=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            format.format(date);
            cal=format.getCalendar();

            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            LocalTime start_time = LocalTime.parse(mangegates.getGate_start_time());
            gate_start_time = start_time;
            LocalTime end_time = LocalTime.parse(mangegates.getGate_end_time());
            gate_end_time = end_time;
            gate_ls_date = cal.getTime();
            gate_bs_date = cal.getTime();

            log.info(mangegates.toString());
        }
        catch (SQLException sql){
            log.error("SQL Error in fetch for Gate_Num: {}, BOOM1_ID: {}", gatename, boom1Id, sql);
            mangegates = null;
        }
        catch (Exception e){
            log.error("Error in fetch for Gate_Num: {}, BOOM1_ID: {}", gatename, boom1Id, e);
            log.info("Failure parsing or issue in Jtrack " + e.getMessage());
            mangegates = null;
        }
    }

    public Boolean getgateStatus(String gatename,JdbcTemplate jdbcTemplate1,String date_str){
        ManageGates mangegates = new ManageGates();
        try {

            String sql = "select * from managegates where Gate_Num=?";
            jdbcTemplate1 = ConnectionUtil.getDataSource(host,username,password);
            log.info("InPrestate"+context().self().hashCode());
            List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, gatename);
            log.info("Inside" + rows.size());
            Map row = rows.get(0);
            
            // Handle different date types from database (Date, Timestamp, LocalDateTime)
            Date date = null;
            Object addedOnValue = row.get("added_on");
            if (addedOnValue instanceof Date) {
                date = (Date) addedOnValue;
            } else if (addedOnValue instanceof java.sql.Timestamp) {
                date = new Date(((java.sql.Timestamp) addedOnValue).getTime());
            } else if (addedOnValue instanceof java.time.LocalDateTime) {
                java.time.LocalDateTime ldt = (java.time.LocalDateTime) addedOnValue;
                date = java.sql.Timestamp.valueOf(ldt);
            } else if (addedOnValue != null) {
                // Try to parse as string if it's a string representation
                try {
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    date = format.parse(addedOnValue.toString());
                } catch (Exception e) {
                    log.warning("Could not parse added_on value: {}", addedOnValue);
                    date = new Date(); // Use current date as fallback
                }
            } else {
                date = new Date(); // Use current date if null
            }
            
            Calendar cal=Calendar.getInstance();
            DateFormat format=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            format.format(date);
            cal=format.getCalendar();
            Boolean is_status =   Boolean.parseBoolean((String)row.get("is_failsafe"));

            Date time = new Date();
            time = cal.getTime();
            long duration_ls  = gate_ls_date.getTime()-time.getTime();
            log.info("gate__date"+time.getTime());

            long diffInMinutes_ls = TimeUnit.MILLISECONDS.toSeconds(duration_ls);
            log.info("gate_ls_date"+gate_ls_date.getTime());

            log.info("diffInMinutes_ls"+diffInMinutes_ls);
            log.info("gate_bs_date"+gate_bs_date.getTime());

            long duration_bs = gate_bs_date.getTime()-time.getTime();
            long diffInMinutes_bs = TimeUnit.MILLISECONDS.toSeconds(duration_bs);
            log.info("diffInMinutes_bs"+diffInMinutes_bs);
            log.info("called status failstatus for" +mangegates.getBoom1Id() + "bs"+diffInMinutes_bs+"ls"+diffInMinutes_ls);

            if((diffInMinutes_ls>10) && (diffInMinutes_bs>10)){
                //J_Message pm = new J_Message(mangegates.getBoom1Id(),true,true,mangegates.getSM(),"open");
                //   lookup.tell(pm, getSelf());
                is_fail = false;
                log.info("updated to false" +mangegates.getBoom1Id());
                updateFailSafe(mangegates.getBoom1Id(),false,date_str);


            }else  {
                is_fail=true;
                log.info("updated to true" +mangegates.getBoom1Id());
                updateFailSafe(mangegates.getBoom1Id(),true,date_str);
                mangegates.setBs1Status("open");
                mangegates.setLeverStatus("open");
            }

            return Boolean.parseBoolean((String)row.get("is_failsafe"));
        }
        catch (  SQLException sql){
            log.error("Error in fetch");
        }
        return false;
    }


    private  boolean isBetween1(LocalTime candidate, LocalTime start, LocalTime end) {
        return !candidate.isBefore(start) && !candidate.isAfter(end);  // Inclusive.
    }
    private void updateGatestatus(JdbcTemplate jdbcTemplate1,String s1,String s2,String gate){
         API obj3 = getDataFromManageGates(gate);
       // String s1=obj3.getGate_status();
       // String s2=obj3.getHandle_status();
        log.info("In Open Command"+s1+s2);
        
        // Check all 4 sensors: BS1, BS2, LT, LS
        // Query database for all sensor statuses
        String sql = "select BS1_STATUS, BS2_STATUS, LT_STATUS, LEVER_STATUS from managegates where BOOM1_ID=? or handle=?";
        List<Map<String, Object>> statusRows = jdbcTemplate1.queryForList(sql, gate, gate);
        boolean allClosed = false;
        if (statusRows != null && !statusRows.isEmpty()) {
            Map<String, Object> statusRow = statusRows.get(0);
            String gateStatus = (String) statusRow.get("BS1_STATUS");
            String bs2Status = (String) statusRow.get("BS2_STATUS");
            String ltStatus = (String) statusRow.get("LT_STATUS");
            String handleStatus = (String) statusRow.get("LEVER_STATUS");
            
            // Default to "open" if status is null (for backward compatibility)
            if (gateStatus == null) gateStatus = "open";
            if (bs2Status == null) bs2Status = "open";
            if (ltStatus == null) ltStatus = "open";
            if (handleStatus == null) handleStatus = "open";
            
            // All four sensors must be closed
            allClosed = gateStatus.equalsIgnoreCase("closed") 
                && bs2Status.equalsIgnoreCase("closed")
                && ltStatus.equalsIgnoreCase("closed")
                && handleStatus.equalsIgnoreCase("closed");
            
            log.info(String.format("Gate %s status check - BS1: %s, BS2: %s, LT: %s, LS: %s, AllClosed: %s", 
                gate, gateStatus, bs2Status, ltStatus, handleStatus, allClosed));
        }

        if (allClosed) {
            log.info("All sensors are closed for gate: {}", gate);
            int i = jdbcTemplate1.update("update managegates set status=?  where status!=? and (BOOM1_ID=? OR handle=?)",new Object[] {"Closed","Closed",gate,gate });
            log.info("Updated managegates status to Closed - rows affected: {}", i);
            if (gateStatus==false) {
                J_Message pm = new J_Message(mangegates.getBoom1Id(),false,false,mangegates.getSM(),"closed");
                //J_Message pm = new J_Message(mangegates.getSM(), mangegates.getGateNum(),"closed");
                lookup.tell(pm, getSelf());
                
                // Publish close event to Redis
                RedisUtil.publishCloseEvent(pm, log);
                RedisUtil.setGateStatus(mangegates.getBoom1Id(), "closed", log);
                
                gateStatus = true;
                mangegates.setStatus("closed");
                log.info("status updated");
            }
            else
            {
                log.info(" status not updated");
            }
            String pattern = "yyyy-MM-dd hh:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            Date date1 = new Date();
            DateFormat format = new SimpleDateFormat("HH:mm");
            log.info("date");

            int interval = -30;
            LocalTime time1 = LocalTime.parse(format.format(date1));
            if(isBetween(time1, LocalTime.of(00, 0), LocalTime.of(02, 0))){
                simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
                interval =0;
            }
            Calendar newYearsEve = Calendar.getInstance();
            newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
            newYearsEve.add(Calendar.MINUTE, interval);
            // Using DateFormat format method we can create a string
            // representation of a date with the defined format.
            String date = simpleDateFormat.format(newYearsEve.getTime());
            //String date = simpleDateFormat.format(new Date());

            String pattern1 = "HH:mm";
            SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(pattern1);
            String time = simpleDateFormat1.format(new Date());

            int o = jdbcTemplate1.update("update reports set lc_lock_time=?, lc_status=? where lc_status = ? and  lc_lock_time=? and lc_pin=? and ackn !=? and added_on >? and lc_name = ?",new Object[]
                    {time,"Closed","","","","",date,obj3.getLc_name()});
            log.info("Attempted to update existing reports - rows affected: {}", o);
            if (o>0) {
                // Generate closed audio when reports are UPDATED to closed status
                RedisUtil.updateQueue(obj3.getSm(), "closed", obj3.getLc_name(), log);
                log.info("lc_lock_time updated and closed audio generated");
            }
            else {
                // No existing report was updated, check if we need to insert a new one
                // Check if there's already a recent "Closed" report (within last 2 minutes) to avoid duplicates
                Calendar recentCheck = Calendar.getInstance();
                recentCheck.add(Calendar.MINUTE, -2);
                String recentDate = simpleDateFormat.format(recentCheck.getTime());
                
                String checkSql = "select count(*) as cnt from reports where command=? and lc_name=? and added_on >?";
                List<Map<String, Object>> recentReports = jdbcTemplate1.queryForList(checkSql, "Closed", obj3.getLc_name(), recentDate);
                int recentCount = 0;
                if (recentReports != null && !recentReports.isEmpty()) {
                    recentCount = ((Number) recentReports.get(0).get("cnt")).intValue();
                }
                
                // Insert new report if:
                // 1. Gate status was just changed to Closed (i>0), OR
                // 2. No recent "Closed" report exists (to handle case where gate was already closed)
                if (i > 0 || recentCount == 0) {
                    J_Message pm = new J_Message(mangegates.getBoom1Id(),false,false,mangegates.getSM(),"closed");
                    Calendar newYearsEve1 = Calendar.getInstance();
                    newYearsEve1.setTimeInMillis(newYearsEve1.getTimeInMillis());
                    newYearsEve1.add(Calendar.MINUTE, interval);
                    // Using DateFormat format method we can create a string
                    // representation of a date with the defined format.
                    String date2 = simpleDateFormat.format(newYearsEve.getTime());

                    String pattern3 = "HH:mm";
                    SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat(pattern3);
                    String time2 = simpleDateFormat3.format(new Date());
                    insertreportsclose(obj3,time2,date2,jdbcTemplate1);
                    lookup.tell(pm, getSelf());
                    
                    // Publish close event to Redis
                    RedisUtil.publishCloseEvent(pm, log);
                    RedisUtil.setGateStatus(mangegates.getBoom1Id(), "closed", log);
                    log.info("Inserted new Closed report for gate: {}", obj3.getLc_name());
                }
                else {
                    log.info("Recent Closed report already exists for gate: {}, skipping duplicate insert", obj3.getLc_name());
                }
            }
        }

       // else if (s1.equalsIgnoreCase("open") && s2.equalsIgnoreCase("open"))
        else if (s2.equalsIgnoreCase("open") || s1.equalsIgnoreCase("open"))
        {

            gateStatus = false;
            String pattern = "yyyy-MM-dd hh:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            Date date2 = new Date();
            DateFormat format = new SimpleDateFormat("HH:mm");
            log.info("date");

            int interval = -30;
            LocalTime time2 = LocalTime.parse(format.format(date2));
            if(isBetween(time2, LocalTime.of(00, 0), LocalTime.of(02, 0))){
                simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
                interval =0;
            }
            Calendar newYearsEve = Calendar.getInstance();
            newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
            newYearsEve.add(Calendar.MINUTE, interval);
            // Using DateFormat format method we can create a string
            // representation of a date with the defined format.
            String date1 = simpleDateFormat.format(newYearsEve.getTime());

            String pattern3 = "HH:mm";
            SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat(pattern3);
            String time1 = simpleDateFormat3.format(new Date());
            log.info("In Open Command");
           int t =  jdbcTemplate1.update("update managegates set status=? where status=? and BOOM1_ID=? or handle=?",new Object[]
                    {"Open","Closed",gate,gate});
            String sql5 = "select * from reports where lc_pin != ? and ackn != ?  and gm=? and lc_open_time=?";
            List<Map<String, Object>> rows5 = jdbcTemplate1.queryForList(sql5,"","",obj3.getGm(),"");
            boolean isrowsavaialable = false;
            for (Map row : rows5) {
                jdbcTemplate1.update("update reports set lc_open_time=? where lc_pin != ? and ackn != ?  and gm=? and lc_open_time=?",new Object[]
                        {time1,"","",obj3.getGm(),""});

                int i = jdbcTemplate1.update("update  reports set lc_open_time=? where command = ? and lc_pin != ?  and lc_status = ? and gm=? and lc_open_time=?",new Object[]
                        {time1,"Open","","open",obj3.getGm(),""});
                int i1 = jdbcTemplate1.update("update  gate_permission set lc_pin=? where is_open_requested = ? and lc_open_request = ?  and added_on > ?",new Object[]
                        {"yes","yes","yes",date1});
                log.info("Test"+i);
                if(i==0) {
                    insertreports(obj3,time1,date1,jdbcTemplate1);
                    isrowsavaialable = true;
                }
                break;
            }
            if(!isrowsavaialable && t >0){
                insertreports(obj3,time1,date1,jdbcTemplate1);
            }
            isrowsavaialable = false;
            mangegates.setStatus("Open");

        }
    }

    private void insertreports( API obj,String format3,String date,JdbcTemplate jdbcTemplate1 ) {

        jdbcTemplate1.update("insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "","",format3,"Open","",obj.getSm(),obj.getGm(),obj.getLc_name(),obj.getLc_name(),
                new Date(),"Open","","","","",format3,"s");
        
        // Update Redis queue with open status - Similar to jsinfotechapi ackService.updateQueue approach
        RedisUtil.updateQueue(obj.getSm(), "open", obj.getLc_name(), log);
    }

    private void insertreportsclose( API obj,String format3,String date,JdbcTemplate jdbcTemplate1 ) {

        jdbcTemplate1.update("insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "","",format3,"Closed","",obj.getSm(),obj.getGm(),obj.getLc_name(),obj.getLc_name(),
                new Date(),"Closed","","","","",format3,"s");
        
        // Update Redis queue with closed status - Similar to jsinfotechapi ackService.updateQueue approach
        RedisUtil.updateQueue(obj.getSm(), "closed", obj.getLc_name(), log);
        
        J_Message pm = new J_Message(mangegates.getGateNum(),false,false,mangegates.getSM(),"closed");
        //J_Message pm = new J_Message(mangegates.getSM(), mangegates.getGateNum(),"closed");
        lookup.tell(pm, getSelf());
        
        // Publish close event to Redis
        RedisUtil.publishCloseEvent(pm, log);
        RedisUtil.setGateStatus(mangegates.getBoom1Id(), "closed", log);
    }
    private API getDataFromManageGates(String gate) {
        log.info("get manage data");
        API obj;
        obj = new API();
        obj.setBs1Go(mangegates.getBs1Go());
        obj.setBs1Gc(mangegates.getBs1Gc());
        obj.setGate(mangegates.getBoom1Id());
        obj.setLc_name(mangegates.getGateNum());
        obj.setSm(mangegates.getSM());
        obj.setGm(mangegates.getGM());
        obj.setBs1Status(mangegates.getBs1Status());
        obj.setLeverStatus(mangegates.getLeverStatus());
        return obj;
    }
}
