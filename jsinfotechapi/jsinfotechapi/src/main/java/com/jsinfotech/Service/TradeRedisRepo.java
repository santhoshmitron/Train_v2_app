package com.jsinfotech.Service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Customers;
import com.jsinfotech.Domain.Priority;
import com.jsinfotech.Domain.Reports;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import com.zerodhatech.models.User;

@Service
public class TradeRedisRepo {

	
	@Autowired
	private RedisUserRepository userRepository;
	
    private KiteConnect kiteConnect = null;
    
    @Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

    @Async("processExecutor")
	public Boolean custructPojo(String apikey) {
	
		   if(kiteConnect==null) {
	            try {
	            	
	            	String SQL = "select  stock_ticker,stock_price from stock_price_alert limit 10";
	        		MapSqlParameterSource parameters = new MapSqlParameterSource();

	        		List<Reports> reports = jdbcTemplate.query(SQL, parameters, new RowMapper<Reports>() {
	        			@Override
	        			public Reports mapRow(ResultSet rs, int i) throws SQLException {
	        				Reports customer = new Reports();
	        				customer.setCommand(rs.getString("stock_ticker"));
	        				customer.setAckn(rs.getString("stock_price"));
	        				return customer;
	        			}
	        		});
	        		
					kiteConnect = getConnection("c9xon4xfwcav7ee4", "vbkr561nphacu1lnlinhdlqblqsoqdxd", apikey);
					for(Reports report:reports) {
					String ticker = report.getCommand();
					if(report.getAckn()==null) {
						continue;
					}
					 try {
					HistoricalData historicalData = 	getHistoricalData(kiteConnect,report.getAckn(),"day");
					HistoricalData historicalData1 = 	getHistoricalData(kiteConnect,report.getAckn(),"5minute");
					
					Priority priority = new Priority();
					Map<String,List<Long>> fivemvolumnes = new HashMap<String,List<Long>>();
					Map<String,List<Long>> volumnes = new HashMap<String,List<Long>>();
					List<Long> volList = new ArrayList<Long>();
					Map<String,List<Double>> ltps = new HashMap<String,List<Double>>();
					List<Double> ltpList = new ArrayList<Double>();
					Map<String,Double> volumneAvg = new HashMap<String,Double>();
					Map<String,Double> lpsAvg = new HashMap<String,Double>();
					for(HistoricalData hd1:historicalData1.dataArrayList) {
						System.out.println("#####Outer"+hd1.timeStamp);
						String date1 = new String(hd1.timeStamp);
						String key = date1.substring(11, 19);
						if(!fivemvolumnes.containsKey(key)) {
							List<Long> volList_5minute = new ArrayList<Long>();
							volList_5minute.add(hd1.volume);
							fivemvolumnes.put(key, volList_5minute);
						}else {
							fivemvolumnes.get(key).add(hd1.volume);
						}
					}
					System.out.println("fivemvolumnes"+fivemvolumnes);
					for(HistoricalData hd:historicalData.dataArrayList) {
						System.out.println("######Inner"+hd.timeStamp);
						volList.add(hd.volume);
						ltpList.add(hd.close);
					}
					volumnes.put(ticker, volList);
					ltps.put(ticker, ltpList);
					volumneAvg.put(ticker, volList.stream().mapToLong(Long::longValue).average().orElse(0));
					lpsAvg.put(ticker, ltpList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
					priority.setVolumnes(volumnes);
					priority.setLtps(ltps);
					priority.setLpsAvg(lpsAvg);
					priority.setVolumneAvg(volumneAvg);
					priority.setFivemvolumnes(fivemvolumnes);
					userRepository.saveTrade(ticker, priority);
					Thread.sleep(1000);
					 }catch (IOException | KiteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							
						}
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	       }
		return true;
	}
	
    /** Get historical data for an instrument.*/
    public static HistoricalData getHistoricalData(KiteConnect kiteConnect,String token,String interval) throws KiteException, IOException {
        /** Get historical data dump, requires from and to date, intrument token, interval, continuous (for expired F&O contracts), oi (open interest)
         * returns historical data object which will have list of historical data inside the object.*/
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 15:30:00");
        Calendar c1 = Calendar.getInstance();
        c1.setTime(new Date());
        String output1 = formatter.format(c1.getTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 09:15:00");
        Calendar c = Calendar.getInstance();
        c.setTime(new Date()); // Using today's date
       
        
        Date from =  new Date();
        Date to = new Date();
        if(interval.equals("day")) {
            c.add(Calendar.DATE, -15); // Adding 5 days
        }else {
            c.add(Calendar.DATE, -5); // Adding 5 days
        }
        
        String output = sdf.format(c.getTime());
        System.out.println(output);
        try {
            from = sdf.parse(output);
            to = formatter.parse(output1);
        }catch (ParseException e) {
            e.printStackTrace();
        }
        HistoricalData historicalData = kiteConnect.getHistoricalData(from, to, token, interval, false, true);
        System.out.println(historicalData.dataArrayList.size());
        System.out.println(historicalData.dataArrayList.get(0).volume);
        System.out.println(historicalData.dataArrayList.get(historicalData.dataArrayList.size() - 1).volume);
        System.out.println(historicalData.dataArrayList.get(0).oi);
        
        return historicalData;
        
    }
	
	public  KiteConnect getConnection(String apikey,String apiSecret,String publictoken) throws SQLException {
        if(kiteConnect==null) {
             kiteConnect = new KiteConnect(apikey);
            kiteConnect.setUserId("ZA9268");
            try {
                User user = kiteConnect.generateSession(publictoken, apiSecret);
                kiteConnect.setAccessToken(user.accessToken);
                kiteConnect.setPublicToken(user.publicToken);
                System.out.println("connection establihed");
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("connection returned"+e.getMessage());

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (KiteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("connection returned"+e.getMessage());

            }
            return kiteConnect;
        }else{
            System.out.println("connection returned");

            return kiteConnect;
        }
	}
	
	public String getPojo(String ticker) {
		return userRepository.findTrade(ticker);
	}
}
