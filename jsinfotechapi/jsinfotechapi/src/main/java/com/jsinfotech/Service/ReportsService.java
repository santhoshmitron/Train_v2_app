package com.jsinfotech.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jsinfotech.Domain.ManageGates;
import com.jsinfotech.Domain.Reports;


@Service
public class ReportsService {
	
	private static final Logger logger = LogManager.getLogger(ReportsService.class);

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	JdbcTemplate jdbcTemplate1;
	
	@Autowired
	private RedisUserRepository userRepository;
	@Autowired
	AcknowledgementService ackService;
	
	@Autowired
	private PNGenerationService pnGenerationService;

	public  List<Reports> findByUsername(String username,String role) {
		logger.debug("Finding reports for username: {}, role: {}", username, role);

		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String user="";

		String  query = "";
		if (role.indexOf('s')!=-1) {
			query = "select * from reports where sm=(:username) and added_on>=(:added_on) order by id DESC" ;	
			logger.debug("SM reports query: {}", query);

		}
		else if(role.indexOf('g')!=-1)
		{
			query = "select * from reports where gm=(:username) and added_on>=(:added_on) order by id DESC" ;	
			logger.debug("GM reports query: {}", query);

		}
		
		// Return empty list if query is not set (invalid role)
		if (query.isEmpty()) {
			return new ArrayList<>();
		}

		parameters.addValue("user", user);

		try {
			// Calculate current time minus 12 hours (instead of today's midnight)
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR_OF_DAY, -12);  // Subtract 12 hours
			Date twelveHoursAgo = cal.getTime();
			String twelveHoursAgoString = formatter.format(twelveHoursAgo);
			
			logger.debug("Date filter (last 12 hours): {}", twelveHoursAgoString);

			parameters.addValue("added_on", twelveHoursAgoString);

		} catch (Exception e) {
			logger.error("Error calculating 12-hour time window for reports query", e);
			e.printStackTrace();
		}
		return jdbcTemplate.query(query,parameters, new RowMapper<Reports>() {
			@Override
			public Reports mapRow(ResultSet rs, int i) throws SQLException {
				Reports customer = new Reports();
				String ackn = rs.getString("ackn");
				String lc_stat = rs.getString("lc_status");
				String lc_pin = rs.getString("lc_pin");
				String lc_name = rs.getString("lc_name");
				customer.setLc(rs.getString("lc"));
				String lc_lock_time = rs.getString("lc_lock_time");
				if(lc_lock_time == null) {
					lc_lock_time = "";
				}
                String tn = rs.getString("tn");
                // Handle null for tn
                if(tn != null && tn.length()>5) {
                	tn = tn.substring(0,5);
                }
                if(tn == null) {
                	tn = "";
                }
				//customer.setId(rs.getInt("id"));
                customer.setId(i+1);
				customer.setLc_name(lc_name);
				customer.setTn(tn+ " " + "["+rs.getString("wer")+"]");
				customer.setCommand(rs.getString("command"));
				customer.setPn(rs.getString("pn")+" " + "["+rs.getString("tn_time")+"]");
				// Handle null for lc_stat - set to empty string if null
				if(lc_stat == null) {
					lc_stat = "";
				}
				
				// Handle null for lc_open_time - set to empty string if null
				String lc_open_time = rs.getString("lc_open_time");
				if(lc_open_time == null) {
					lc_open_time = "";
				}
				
				if(lc_stat.equalsIgnoreCase("Closed")) {
				customer.setLc_status(lc_stat+"" + "["+lc_lock_time+"]");
				}
				else {
					customer.setLc_status(lc_stat+"" + "["+lc_open_time+"]");
				}				
				customer.setLc_lock_time(lc_lock_time);
				
				// Handle null for lc_pin - set to empty string if null
				if(lc_pin == null) {
					lc_pin = "";
				}
				// Handle null for lc_pin_time - set to empty string if null
				String lc_pin_time = rs.getString("lc_pin_time");
				if(lc_pin_time == null) {
					lc_pin_time = "";
				}
				customer.setLc_pin(lc_pin+"" + "["+lc_pin_time+"]");
				customer.setAckn(ackn);
				customer.setSm(rs.getString("sm"));
				customer.setLc_open_time("[ "+lc_open_time+" ]");
				
				// Handle null for Boom_Lock - set to empty string if null
				String boomLock = rs.getString("Boom_Lock");
				if (boomLock == null) {
					boomLock = "";
				}
				customer.setBoomLock(boomLock);
						
				return customer;
			}
		});


	}

	public  List<Reports> findByUsernamegm(String username,String role) {

		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String user="";
		System.out.println(username.indexOf('s'));

		String  query = "";
		if (role.indexOf('s')!=-1) {
			query = "select * from reports where sm=(:username) and added_on>=(:added_on) order by id DESC" ;	

		}
		else if(role.indexOf('g')!=-1)
		{
			query = "select * from reports where gm=(:username) and added_on>=(:added_on) order by id DESC" ;
			System.out.println("GetReportsgqdfdfdf"+query);


		}

		parameters.addValue("user", user);

		try {
			// Calculate current time minus 12 hours (instead of today's midnight)
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR_OF_DAY, -12);  // Subtract 12 hours
			Date twelveHoursAgo = cal.getTime();
			String twelveHoursAgoString = formatter.format(twelveHoursAgo);
			
			logger.debug("Date filter (last 12 hours): {}", twelveHoursAgoString);

			parameters.addValue("added_on", twelveHoursAgoString);

		} catch (Exception e) {
			logger.error("Error calculating 12-hour time window for reports query", e);
			e.printStackTrace();
		}
		List<Reports> li = jdbcTemplate.query(query,parameters, new RowMapper<Reports>() {
			@Override
			public Reports mapRow(ResultSet rs, int i) throws SQLException {
				Reports customer = new Reports();
				String ackn = rs.getString("ackn");
				String lc_stat = rs.getString("lc_status");
				String lc_pin = rs.getString("lc_pin");
				String lc_name = rs.getString("lc_name");
				customer.setLc(rs.getString("lc"));
				
				// Handle null for lc_lock_time
				String lc_lock_time = rs.getString("lc_lock_time");
				if(lc_lock_time == null) {
					lc_lock_time = "";
				}
				
				// Handle null for lc_open_time
				String lc_open_time = rs.getString("lc_open_time");
				if(lc_open_time == null) {
					lc_open_time = "";
				}
				
                String tn = rs.getString("tn");
                if(tn != null && tn.length()>5) {
                	tn = tn.substring(0,5);
                }
                if(tn == null) {
                	tn = "";
                }
				//customer.setId(rs.getInt("id"));
                customer.setId(i+1);
				customer.setLc_name(lc_name);
				customer.setTn(tn+ " " + "["+rs.getString("wer")+"]");
				customer.setCommand(rs.getString("command"));
				customer.setPn(rs.getString("pn")+" " + "["+rs.getString("tn_time")+"]");
				
				// Handle null for lc_stat
				if(lc_stat == null) {
					lc_stat = "";
				}
				
				if(lc_stat.equalsIgnoreCase("Closed")) {
					customer.setLc_status(lc_stat+"" + "["+lc_lock_time+"]");
				} else {
					customer.setLc_status(lc_stat+"" + "["+lc_open_time+"]");
				}
				customer.setLc_lock_time(lc_lock_time);
				
				// Handle null for lc_pin
				if(lc_pin == null) {
					lc_pin = "";
				}
				
				// Handle null for lc_pin_time
				String lc_pin_time = rs.getString("lc_pin_time");
				if(lc_pin_time == null) {
					lc_pin_time = "";
				}
				
				customer.setLc_pin(lc_pin+"" + "["+lc_pin_time+"]");
				// Ensure ackn is properly set (handle null case)
				customer.setAckn(ackn != null ? ackn : "");
				customer.setSm(rs.getString("sm"));
				// Handle null for lc_open_time
				String lc_open_time_val = rs.getString("lc_open_time");
				if(lc_open_time_val == null) {
					lc_open_time_val = "";
				}
				customer.setLc_open_time(lc_open_time_val);
				
				// Handle null for Boom_Lock - set to empty string if null
				String boomLock = rs.getString("Boom_Lock");
				if (boomLock == null) {
					boomLock = "";
				}
				customer.setBoomLock(boomLock);
				
				return customer;
			}
		});
		
		/*String query1 = "select * from reports where sm=(:sm) and lc=(:lc) and added_on>(:added_on) and gm='' and redy='s' order by id DESC" ;	

		if(li.size()<0) {
			return li;
		}else {
			parameters.addValue("sm", li.get(0).getSm());
			parameters.addValue("lc",li.get(0).getLc() );
		}

		List<Reports> li1 = jdbcTemplate.query(query1,parameters, new RowMapper<Reports>() {
			@Override
			public Reports mapRow(ResultSet rs, int i) throws SQLException {
				Reports customer = new Reports();
				String lc_stat = rs.getString("lc_status");
				String lc_name = rs.getString("lc_name");
				customer.setId(rs.getInt("id"));
				customer.setLc_name(lc_name);
				customer.setLc_status(lc_stat);
				customer.setLc_lock_time(rs.getString("lc_lock_time"));
				customer.setLc(rs.getString("lc"));
				
				return customer;
			}
		});
        int k = 0;
		for(int i = 0 ; i<li.size();i++) {
			if(i<li1.size()) {
			if((li.get(i).getId() < li1.get(k).getId()) && (li.get(i).getLc().equals(li1.get(k).getLc()))) {
				li.get(i).setLc_open_time(li1.get(k).getLc_lock_time());
				k++;
			}
			}
			li.get(i).setLc_status(li.get(i).getLc_status()+" "+"["+li.get(i).getLc_lock_time()+"]");

		}*/
		
		return li;

	}


	public void add(Reports reports) {
		String pattern = "yyyy-MM-dd hh:mm:ss";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

		jdbcTemplate1.update("insert into reports (tn, pn,tn_time,command,wer,sm,gm,lc,lc_name,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,added_on,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
				reports.getTn(),
				reports.getPn(),
				reports.getTn_time(),
				reports.getCommand(),
				reports.getWer(),
				reports.getSm(),
				reports.getGm(),
				reports.getLc(),
				reports.getLc_name(),
				reports.getLc_status(),
				reports.getLc_lock_time(),
				reports.getLc_pin(),
				reports.getLc_pin_time(),
				reports.getAckn(),
				simpleDateFormat.format(reports.getAdded_on()),
				reports.getLc_open_time(),
				reports.getRedy());

	}
	
	/**
	 * Get unacknowledged failsafe reports for a given username (SM or GM)
	 * Returns a list of maps containing id and lc_name (gateId) for all unacknowledged failsafe reports
	 */
	public List<Map<String, Object>> getUnacknowledgedFailsafeReports(String username) {
		try {
			String query = "SELECT id, lc_name FROM reports WHERE command = 'NO-NETWORK' AND (ackn IS NULL OR ackn = '') AND (sm = ? OR gm = ?) ORDER BY added_on DESC";
			List<Map<String, Object>> results = jdbcTemplate1.queryForList(query, username, username);
			return results != null ? results : new java.util.ArrayList<>();
		} catch (Exception e) {
			logger.error("Error getting unacknowledged failsafe reports for user: {}", username, e);
			return new java.util.ArrayList<>();
		}
	}
	
	/**
	 * Get gate name from managegates table using BOOM1_ID or Gate_Num
	 * Returns Gate_Num if found, otherwise returns the input value
	 * Note: lc_name in reports table may contain either BOOM1_ID (e.g., "E20-750BS") or Gate_Num (e.g., "750")
	 */
	public String getGateNameFromGateId(String gateIdOrName) {
		try {
			if (gateIdOrName == null || gateIdOrName.isEmpty()) {
				return gateIdOrName;
			}
			// First check if it's already a Gate_Num by searching in managegates
			String queryByName = "SELECT Gate_Num FROM managegates WHERE Gate_Num = ? LIMIT 1";
			List<Map<String, Object>> nameResults = jdbcTemplate1.queryForList(queryByName, gateIdOrName);
			if (nameResults != null && !nameResults.isEmpty()) {
				// It's already a Gate_Num, return it
				return gateIdOrName;
			}
			// If not found as Gate_Num, try to find it as BOOM1_ID or handle
			String query = "SELECT Gate_Num FROM managegates WHERE BOOM1_ID = ? OR handle = ? LIMIT 1";
			List<Map<String, Object>> results = jdbcTemplate1.queryForList(query, gateIdOrName, gateIdOrName);
			if (results != null && !results.isEmpty()) {
				Object gateNameObj = results.get(0).get("Gate_Num");
				if (gateNameObj != null) {
					return gateNameObj.toString();
				}
			}
		} catch (Exception e) {
			logger.error("Error getting gate name for BOOM1_ID/Name: {}", gateIdOrName, e);
		}
		// If not found, return input value as fallback (might already be a Gate_Num)
		return gateIdOrName;
	}
	
	/**
	 * Get failsafe response data for a given username (SM or GM)
	 * Returns FailsafeResponse with report IDs and formatted play commands
	 * This method handles deduplication of gate names and formatting of messages
	 */
	public com.jsinfotech.Domain.FailsafeResponse getFailsafeResponseData(String username) {
		com.jsinfotech.Domain.FailsafeResponse response = new com.jsinfotech.Domain.FailsafeResponse();
		try {
			if (username == null || username.isEmpty()) {
				response.setPlay_command(new java.util.ArrayList<>());
				response.setReportid(new java.util.ArrayList<>());
				return response;
			}
			
			java.util.List<java.util.Map<String, Object>> failsafeReports = getUnacknowledgedFailsafeReports(username);
			if (failsafeReports != null && !failsafeReports.isEmpty()) {
				java.util.List<String> playCommands = new java.util.ArrayList<>();
				java.util.List<Integer> reportIds = new java.util.ArrayList<>();
				java.util.Set<String> seenGateNames = new java.util.HashSet<>(); // Track unique gate names for deduplication
				
				for (java.util.Map<String, Object> report : failsafeReports) {
					Object idObj = report.get("id");
					Object lcNameObj = report.get("lc_name");
					
					if (idObj != null && lcNameObj != null) {
						Integer reportId = null;
						if (idObj instanceof Integer) {
							reportId = (Integer) idObj;
						} else if (idObj instanceof Number) {
							reportId = ((Number) idObj).intValue();
						}
						
						// Always add report ID to the list
						if (reportId != null) {
							reportIds.add(reportId);
						}
						
						String gateId = lcNameObj.toString();
						// Get gate name from managegates table
						String gateName = getGateNameFromGateId(gateId);
						
						// Deduplicate: only add message if we haven't seen this gate name before
						if (gateName != null && !gateName.isEmpty() && !seenGateNames.contains(gateName)) {
							seenGateNames.add(gateName);
							// Format message: "LC {gateName} No network ,Operate with Manual PN"
							String message = "LC " + gateName + " No network ,Operate with Manual PN";
							playCommands.add(message);
						}
					}
				}
				
				response.setPlay_command(playCommands);
				response.setReportid(reportIds);
			} else {
				// No unacknowledged failsafe, return empty (no buzz)
				response.setPlay_command(new java.util.ArrayList<>());
				response.setReportid(new java.util.ArrayList<>());
			}
		} catch (Exception e) {
			System.err.println("Error getting failsafe response data for user: " + username);
			e.printStackTrace();
			response.setPlay_command(new java.util.ArrayList<>());
			response.setReportid(new java.util.ArrayList<>());
		}
		return response;
	}
	
	/**
	 * Update acknowledgement for a failsafe report
	 * Returns true if update was successful, false otherwise
	 */
	public boolean updateFailsafeAck(int reportId, String ackn) {
		try {
			int updated = jdbcTemplate1.update(
				"UPDATE reports SET ackn = ? WHERE id = ? AND command = 'NO-NETWORK'",
				ackn, reportId
			);
			return updated > 0;
		} catch (Exception e) {
			System.err.println("Error updating failsafe acknowledgement for report: " + reportId);
			e.printStackTrace();
			return false;
		}
	}

	public void addOpen(String username,String rolename) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		String  query = "";
		if (rolename.equalsIgnoreCase("sm")) {
			query = "select * from managegates where SM=(:username)";

		}
		else if(rolename.equalsIgnoreCase("gm"))
		{
			query = "select * from managegates where GM=(:username)";

		}
		int k =1;
		List<ManageGates> l = jdbcTemplate.query(query,parameters, new RowMapper<ManageGates>() {
			@Override
			public ManageGates mapRow(ResultSet rs, int i) throws SQLException {
				ManageGates customer = new ManageGates();
				customer.setId(rs.getInt("id"));
				customer.setStatus(rs.getString("status").toLowerCase());
				customer.setBoom1Id(rs.getString("BOOM1_ID"));
				customer.setGateNum(rs.getString("Gate_Num"));
				customer.setSM(rs.getString("sm"));
				return customer;
			}
		});
		String pattern = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		 String pattern3 = "HH:mm";
         SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat(pattern3);
         String time1 = simpleDateFormat3.format(new Date());
         String time2 = simpleDateFormat.format(new Date());
         for(ManageGates m:l) {
		 jdbcTemplate1.update("insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
	                "","",time1,"R-Open","",m.getSM(),username,m.getGateNum(),m.getGateNum(),
	                time2,"","",getRandomNumberWithExclusion(),time1,"","","ct");
			ackService.updateQueue(m.getSM(), "ROPN",m.getGateNum());

         }
         System.out.println(time1);
		 
	    System.out.println("In addOpen method");


	}
	
public void addOpenClose(String command,String username,String gates) {
	
		String rolename = "sm";
		System.out.println(command+username+gates+"inaddOpenClose");
		
		String [] str = gates.split(",");
		String pattern = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		 String pattern3 = "HH:mm";
         SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat(pattern3);
         String time1 = simpleDateFormat3.format(new Date());
         
         String close = "Close";
         String rclose = "R-Close";
         String open = "Open";
         String ropen = "R-Open";
         HashMap<String,String>map =addcloseandpush(username,"sm");
         System.out.println("addOpenClose");
         System.out.println(map);
         for(String s:str) {
        	 if("R-Close".equals(command)) {
        	int i = jdbcTemplate1.update("update reports set tn_time=?, pn=?, command = ? where command = ? and sm=? and lc_name=? and redy=?",new Object[]
                     {time1,pnGenerationService.generateUniquePNForGate(s),close,rclose,username,s,"ct"}); 
     		 if(i >= 1) {
        	 userRepository.poppAudio2(username+"1");
 			 ackService.updateQueue(map.get(s), "RECPN",s);
     		 }

        	 }else {
        		 int i = jdbcTemplate1.update("update reports set tn_time=?, pn=?, command = ? where command = ? and sm=? and lc_name=? and redy=?",new Object[]
                         {time1,pnGenerationService.generateUniquePNForGate(s),open,ropen,username,s,"ct"}); 
        		 if(i >= 1) {
                	 userRepository.poppAudio2(username+"1");
          			ackService.updateQueue(map.get(s), "REOPN",s);
        		 }

        	 }
         }
		 
	    System.out.println("In addOpen method");


	}
	
	public static int getRandomNumberWithExclusion( )
	{
	  Random r = new Random();
	  int result = -1;

	  do
	  {
		  	// 3-digit number, digits 1-9 only (no zeros anywhere)
		  	result = 111 + r.nextInt(889); // 111..999 inclusive-ish; filtered by isAllowed()
	  }//do
	  while( !isAllowed( result ) );

	  return result;

	}//met
	
	private static boolean isAllowed( int number )
	{
		if (number < 111 || number > 999) {
			return false;
		}
		String s = String.valueOf(number);
		return s.length() == 3 && s.indexOf('0') < 0;
	}//met
	
	public void addclose(String username,String rolename) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		String  query = "";
		if (rolename.equalsIgnoreCase("sm")) {
			query = "select * from managegates where SM=(:username)";

		}
		else if(rolename.equalsIgnoreCase("gm"))
		{
			query = "select * from managegates where GM=(:username)";

		}
		int k =1;
		List<ManageGates> l = jdbcTemplate.query(query,parameters, new RowMapper<ManageGates>() {
			@Override
			public ManageGates mapRow(ResultSet rs, int i) throws SQLException {
				ManageGates customer = new ManageGates();
				customer.setId(rs.getInt("id"));
				customer.setStatus(rs.getString("status").toLowerCase());
				customer.setBoom1Id(rs.getString("BOOM1_ID"));
				customer.setGateNum(rs.getString("Gate_Num"));
				customer.setSM(rs.getString("sm"));
				return customer;
			}
		});
		String pattern = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		 String pattern3 = "HH:mm";
         SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat(pattern3);
         String time1 = simpleDateFormat3.format(new Date());
         String time2 = simpleDateFormat.format(new Date());

         for(ManageGates m:l) {
		 jdbcTemplate1.update("insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
	                "","",time1,"R-Close","",m.getSM(),username,m.getGateNum(),m.getGateNum(),
	                time2,"","",getRandomNumberWithExclusion(),time1,"","","ct");
			ackService.updateQueue(m.getSM(), "RCPN",m.getGateNum());

         }
		 
	    System.out.println("In addOpen method");

	}

	
public HashMap<String,String> addcloseandpush(String username,String rolename) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		String  query = "";
		if (rolename.equalsIgnoreCase("sm")) {
			query = "select * from managegates where SM=(:username)";

		}

		int k =1;
		HashMap<String,String> hash = new HashMap<String,String>();
		
		List<ManageGates> l = jdbcTemplate.query(query,parameters, new RowMapper<ManageGates>() {
			@Override
			public ManageGates mapRow(ResultSet rs, int i) throws SQLException {
				ManageGates customer = new ManageGates();
				customer.setId(rs.getInt("id"));
				customer.setStatus(rs.getString("status").toLowerCase());
				customer.setBoom1Id(rs.getString("BOOM1_ID"));
				customer.setGateNum(rs.getString("Gate_Num"));
				customer.setSM(rs.getString("sm"));
				hash.put(rs.getString("Gate_Num"), rs.getString("gm"));
				return customer;
			}
		});
		
	    System.out.println("In addcloseandpush method");
	    return hash;

	}

	public List<Reports> findreports(String username,String role, String from, String to) {
		System.out.println("In findreports method");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		Reports dates=  new Reports();
		dates.setFrom(from);
		dates.setTo(to);
		parameters.addValue("username", username);
		parameters.addValue("from", dates.getFrom());
		parameters.addValue("to",  dates.getTo());
		System.out.println("jsinfo date::"+"added_on");

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date fromdate=formatter.parse(from);
			Date todate=formatter.parse(to);
			System.out.println(dates.getFrom()+"from date");
			System.out.println(dates.getTo()+"to date");

		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		System.out.println(username.indexOf('s'));

		String  query = "";
		if (role.indexOf('s')!=-1) {
			query = "select * from reports where sm=(:username) and added_on>=(:from) and added_on<=(:to)  order by id DESC" ;	

		}
		else if(role.indexOf('g')!=-1)
		{
			query = "select * from reports where gm=(:username) and added_on>=(:from) and added_on<=(:to)  order by id DESC" ;	

		}



		List<Reports> li = jdbcTemplate.query(query,parameters, new RowMapper<Reports>() {
			@Override
			public Reports mapRow(ResultSet rs, int i) throws SQLException {
				Reports customer = new Reports();
				String ackn = rs.getString("ackn");
				String lc_stat = rs.getString("lc_status");
				String lc_pin = rs.getString("lc_pin");
				String lc_name = rs.getString("lc_name");
				customer.setLc(rs.getString("lc"));
				
				// Handle null for lc_lock_time
				String lc_lock_time = rs.getString("lc_lock_time");
				if(lc_lock_time == null) {
					lc_lock_time = "";
				}
				
				// Handle null for lc_open_time
				String lc_open_time = rs.getString("lc_open_time");
				if(lc_open_time == null) {
					lc_open_time = "";
				}
				
                String tn = rs.getString("tn");
                if(tn != null && tn.length()>5) {
                	tn = tn.substring(0,5);
                }
                if(tn == null) {
                	tn = "";
                }
				//customer.setId(rs.getInt("id"));
                customer.setId(i);
				customer.setLc_name(lc_name);
				customer.setTn(tn+ " " + "["+rs.getString("wer")+"]");
				customer.setCommand(rs.getString("command"));
				customer.setPn(rs.getString("pn")+" " + "["+rs.getString("tn_time")+"]");
				
				// Handle null for lc_stat
				if(lc_stat == null) {
					lc_stat = "";
				}
				
				if(lc_stat.equalsIgnoreCase("Closed")) {
					customer.setLc_status(lc_stat+"" + "["+lc_lock_time+"]");
				} else {
					customer.setLc_status(lc_stat+"" + "["+lc_open_time+"]");
				}
				customer.setLc_lock_time(lc_lock_time);
				
				// Handle null for lc_pin
				if(lc_pin == null) {
					lc_pin = "";
				}
				
				// Handle null for lc_pin_time
				String lc_pin_time = rs.getString("lc_pin_time");
				if(lc_pin_time == null) {
					lc_pin_time = "";
				}
				
				customer.setLc_pin(lc_pin+"" + "["+lc_pin_time+"]");
				customer.setAckn(ackn);
				customer.setSm(rs.getString("sm"));
				customer.setLc_open_time("[ "+lc_open_time+" ]");
				
				// Handle null for Boom_Lock - set to empty string if null
				String boomLock2 = rs.getString("Boom_Lock");
				if (boomLock2 == null) {
					boomLock2 = "";
				}
				customer.setBoomLock(boomLock2);
				
				
				return customer;
			}
		});
		
		return li;

	}



	public   Boolean getReportstatus(String username,String gateid,String status) {
		
	   // ManageGates managegates = jdbcTemplate1.queryForObject("select * from managegates where BOOM1_ID=?", new Object[] { gateid },new BeanPropertyRowMapper<ManageGates>(ManageGates.class));
	   // System.out.println("DDDD"+managegates);
		
		//AcknowledgementService.updateQueue(username,status,gateid);

		return true;
	
	}

	/**
	 * Generate play commands from unacknowledged reports (Open, Closed, Boom Lock)
	 * Returns list of play command strings for reports that haven't been acknowledged yet
	 * 
	 * @param username The username (SM or GM)
	 * @param role The role (sm or gm)
	 * @return List of play command strings (e.g., "750 Opened", "750 Closed", "750 boom lock is healthy.")
	 */
	public List<String> getPlayCommandsFromReports(String username, String role) {
		List<String> playCommands = new ArrayList<>();
		try {
			if (username == null || username.isEmpty()) {
				return playCommands;
			}
			
			// Determine which field to query based on role
			String roleField = "";
			if (role != null && role.indexOf('s') != -1) {
				roleField = "sm";
			} else if (role != null && role.indexOf('g') != -1) {
				roleField = "gm";
			} else {
				return playCommands; // Invalid role
			}
			
			// Get LATEST unacknowledged report per gate (regardless of command type)
			// Then determine play command based on that latest report's command field
			String latestReportQuery = "SELECT lc_name, command, Boom_Lock FROM reports " +
				"WHERE " + roleField + " = ? " +
				"AND (ackn IS NULL OR ackn = '') " +
				"AND added_on >= DATE_SUB(NOW(), INTERVAL 12 HOUR) " +
				"AND id IN (" +
				"  SELECT MAX(id) FROM reports " +
				"  WHERE " + roleField + " = ? " +
				"  AND (ackn IS NULL OR ackn = '') " +
				"  AND added_on >= DATE_SUB(NOW(), INTERVAL 12 HOUR) " +
				"  GROUP BY lc_name" +
				") " +
				"ORDER BY added_on DESC, id DESC";
			
			List<Map<String, Object>> latestRows = jdbcTemplate1.queryForList(latestReportQuery, username, username);
			for (Map<String, Object> row : latestRows) {
				Object lcNameObj = row.get("lc_name");
				Object commandObj = row.get("command");
				Object boomLockObj = row.get("Boom_Lock");
				
				if (lcNameObj == null) {
					continue;
				}
				
				String lcName = lcNameObj.toString().trim();
				if (lcName.isEmpty()) {
					continue;
				}
				
				// Check command field case-insensitively (handle Open, Closed, Close)
				String command = commandObj != null ? commandObj.toString().trim() : "";
				String commandUpper = command.toUpperCase();
				
				if ("OPEN".equals(commandUpper)) {
					// Latest report is Open → add "Opened"
					playCommands.add(lcName + " Opened");
				} else if ("CLOSED".equals(commandUpper) || "CLOSE".equals(commandUpper)) {
					// Latest report is Closed or Close → add "Closed"
					playCommands.add(lcName + " Closed");
					
					// If Closed report has Boom_Lock, also add boom lock status
					if (boomLockObj != null) {
						String boomLockValue = boomLockObj.toString().trim();
						if (!boomLockValue.isEmpty()) {
							// Extract status from format "Healthy[HH:mm]" or "Unhealthy[HH:mm]"
							String status = boomLockValue.toLowerCase();
							if (status.startsWith("healthy")) {
								playCommands.add(lcName + " boom lock is healthy.");
							} else if (status.startsWith("unhealthy")) {
								playCommands.add(lcName + " boom lock is unhealthy.");
							}
						}
					}
				}
				// Ignore other command types (NO-NETWORK, etc.)
			}
			
		} catch (Exception e) {
			logger.error("Error generating play commands from reports for username: {}, role: {}", username, role, e);
		}
		
		return playCommands;
	}

	/**
	 * Get play command for a single gate based on main status and latest unacknowledged report
	 * Returns play command string (e.g., "750 Opened", "750 Closed", "750 boom lock is healthy.")
	 * 
	 * @param username The username (SM or GM)
	 * @param role The role (sm or gm)
	 * @param gateNum The gate number (lc_name)
	 * @param boom1Id The BOOM1_ID
	 * @param mainStatus The main status from managegates table ("open" or "closed")
	 * @return Play command string or null if no unacknowledged report found
	 */
	public String getPlayCommandForGate(String username, String role, String gateNum, String boom1Id, String mainStatus, String reportId) {
		try {
			if (username == null || username.isEmpty() || gateNum == null || gateNum.trim().isEmpty()) {
				return null;
			}
			
			// Determine which field to query based on role
			String roleField = "";
			if (role != null && role.indexOf('s') != -1) {
				roleField = "sm";
			} else if (role != null && role.indexOf('g') != -1) {
				roleField = "gm";
			} else {
				return null; // Invalid role
			}
			
			String b1 = (boom1Id != null) ? boom1Id.trim() : "";
			String gn = (gateNum != null) ? gateNum.trim() : "";
			
			// Priority 1: Check Boom_Lock first (takes precedence over main status)
			// If reportId is provided, first check if there's a NEWER report with Boom_Lock updated
			// If newer report has Boom_Lock, use that status; otherwise use reportId's Boom_Lock
			if (reportId != null && !reportId.trim().isEmpty()) {
				try {
					int reportIdInt = Integer.parseInt(reportId.trim());
					
					// First, check if there's a NEWER report (id > reportId) with Boom_Lock for this gate
					// This handles cases where Boom_Lock status updates in a newer row
					String newerBoomLockQuery = "SELECT id, Boom_Lock FROM reports " +
						"WHERE " + roleField + " = ? " +
						"AND id > ? " +
						"AND added_on >= DATE_SUB(NOW(), INTERVAL 12 HOUR) " +
						"AND (lc IN (?,?) OR lc_name IN (?,?)) " +
						"AND (Boom_Lock IS NOT NULL AND Boom_Lock != '') " +
						"ORDER BY id DESC " +
						"LIMIT 1";
					
					List<Map<String, Object>> newerRows = jdbcTemplate1.queryForList(newerBoomLockQuery, username, reportIdInt, b1, gn, b1, gn);
					
					// If newer report with Boom_Lock found, use that status
					if (newerRows != null && !newerRows.isEmpty()) {
						Map<String, Object> row = newerRows.get(0);
						
						// Try to find Boom_Lock column - check all possible case variations
						Object boomLockObj = null;
						for (String key : row.keySet()) {
							if (key != null && key.equalsIgnoreCase("Boom_Lock")) {
								boomLockObj = row.get(key);
								break;
							}
						}
						
						// If still not found, try common variations
						if (boomLockObj == null) {
							boomLockObj = row.get("Boom_Lock");
						}
						if (boomLockObj == null) {
							boomLockObj = row.get("BOOM_LOCK");
						}
						if (boomLockObj == null) {
							boomLockObj = row.get("boom_lock");
						}
						
						if (boomLockObj != null) {
							String boomLockValue = boomLockObj.toString().trim();
							if (!boomLockValue.isEmpty() && !boomLockValue.equalsIgnoreCase("null") && !boomLockValue.equalsIgnoreCase("NULL")) {
								String status = boomLockValue.toLowerCase();
								if (status.startsWith("healthy")) {
									return gn + " boom lock is healthy.";
								} else if (status.startsWith("unhealthy")) {
									return gn + " boom lock is unhealthy.";
								}
							}
						}
					}
					
					// If no newer report with Boom_Lock, check the specific reportId's Boom_Lock
					String reportBoomLockQuery = "SELECT Boom_Lock FROM reports WHERE id = ?";
					List<Map<String, Object>> reportRows = jdbcTemplate1.queryForList(reportBoomLockQuery, reportIdInt);
					
					if (reportRows != null && !reportRows.isEmpty()) {
						Map<String, Object> row = reportRows.get(0);
						
						// Try to find Boom_Lock column - check all possible case variations
						Object boomLockObj = null;
						for (String key : row.keySet()) {
							if (key != null && key.equalsIgnoreCase("Boom_Lock")) {
								boomLockObj = row.get(key);
								break;
							}
						}
						
						// If still not found, try common variations
						if (boomLockObj == null) {
							boomLockObj = row.get("Boom_Lock");
						}
						if (boomLockObj == null) {
							boomLockObj = row.get("BOOM_LOCK");
						}
						if (boomLockObj == null) {
							boomLockObj = row.get("boom_lock");
						}
						
						// If this specific reportId has Boom_Lock, return boom lock status message
						if (boomLockObj != null) {
							String boomLockValue = boomLockObj.toString().trim();
							if (!boomLockValue.isEmpty() && !boomLockValue.equalsIgnoreCase("null") && !boomLockValue.equalsIgnoreCase("NULL")) {
								String status = boomLockValue.toLowerCase();
								if (status.startsWith("healthy")) {
									return gn + " boom lock is healthy.";
								} else if (status.startsWith("unhealthy")) {
									return gn + " boom lock is unhealthy.";
								}
							}
						}
					}
				} catch (NumberFormatException e) {
					logger.warn("Invalid reportId format: {}", reportId);
				} catch (Exception e) {
					logger.warn("Error checking Boom_Lock for reportId {}: {}", reportId, e.getMessage());
				}
			}
			
			// If reportId not provided or doesn't have Boom_Lock, check latest report for this gate
			// Use same query structure as findLatestClosedReportIdForGate for consistency
			String latestReportQuery = "SELECT id, Boom_Lock FROM reports " +
				"WHERE " + roleField + " = ? " +
				"AND added_on >= DATE_SUB(NOW(), INTERVAL 12 HOUR) " +
				"AND (lc IN (?,?) OR lc_name IN (?,?)) " +
				"ORDER BY id DESC " +
				"LIMIT 1";
			
			List<Map<String, Object>> latestRows = jdbcTemplate1.queryForList(latestReportQuery, username, b1, gn, b1, gn);
			
			// Check Boom_Lock in the latest report
			if (latestRows != null && !latestRows.isEmpty()) {
				Map<String, Object> row = latestRows.get(0);
				
				// Try to find Boom_Lock column - check all possible case variations
				Object boomLockObj = null;
				for (String key : row.keySet()) {
					if (key != null && key.equalsIgnoreCase("Boom_Lock")) {
						boomLockObj = row.get(key);
						break;
					}
				}
				
				// If still not found, try common variations
				if (boomLockObj == null) {
					boomLockObj = row.get("Boom_Lock");
				}
				if (boomLockObj == null) {
					boomLockObj = row.get("BOOM_LOCK");
				}
				if (boomLockObj == null) {
					boomLockObj = row.get("boom_lock");
				}
				
				// If latest report has Boom_Lock, return boom lock status message (priority)
				// This takes precedence over main status - once Boom_Lock is updated, show that status
				if (boomLockObj != null) {
					String boomLockValue = boomLockObj.toString().trim();
					// Check if Boom_Lock has a valid value (not empty, not "null" string)
					if (!boomLockValue.isEmpty() && !boomLockValue.equalsIgnoreCase("null") && !boomLockValue.equalsIgnoreCase("NULL")) {
						// Extract status from format "Healthy[HH:mm]" or "Unhealthy[HH:mm]"
						String status = boomLockValue.toLowerCase();
						if (status.startsWith("healthy")) {
							return gn + " boom lock is healthy.";
						} else if (status.startsWith("unhealthy")) {
							return gn + " boom lock is unhealthy.";
						}
						// If Boom_Lock has a value but doesn't match healthy/unhealthy, still return it
						// This handles any edge cases where format might be slightly different
						return gn + " boom lock is " + status.split("\\[")[0] + ".";
					}
				}
			}
			
			// Priority 2: Check main status from managegates table (not command field from reports)
			// Use main status to determine Open/Closed play commands
			if (mainStatus != null && !mainStatus.trim().isEmpty()) {
				String mainStatusLower = mainStatus.toLowerCase().trim();
				if ("open".equals(mainStatusLower)) {
					return gn + " Opened";
				} else if ("closed".equals(mainStatusLower)) {
					return gn + " Closed";
				}
			}
			
			// No valid play command found
			return null;
			
		} catch (Exception e) {
			logger.error("Error getting play command for gate: {} (BOOM1_ID: {}), username: {}, role: {}", 
				gateNum, boom1Id, username, role, e);
			return null;
		}
	}

}
