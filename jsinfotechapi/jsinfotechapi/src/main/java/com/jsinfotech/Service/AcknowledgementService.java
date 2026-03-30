package com.jsinfotech.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Gate;
import com.jsinfotech.Domain.Gate2;
import com.jsinfotech.Domain.Reports;

@Service
public class AcknowledgementService {

	
	@Autowired
	public RedisUserRepository userRepository;
	
	public static Map<String, Queue<String>> play = new HashMap<String, Queue<String>>();
	public static Map<String, String> flag = new HashMap<String, String>();
	 @Autowired
		private KafkaTemplate<String, Gate> kafkaTemplate;
	    
	    @Value(value = "${kafka.topic1}")
	    private String topic1;
	    
	    @Value(value = "${kafka.topic2}")
	    private String topic2;
	

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	JdbcTemplate jdbcTemplate1;
	
	@Autowired
	private PNGenerationService pnGenerationService;

	public List<Reports> getReports(String userId) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String pattern = "yyyy-MM-dd 00:00:00";
		Calendar current = Calendar.getInstance();
		current.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		current.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DATE), 0, 0, 0);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		parameters.addValue("userId", userId);
		String date1 = simpleDateFormat.format(current.getTime());
		parameters.addValue("today", date1);
		System.out.println("parameters" + parameters.getValue("userId") + parameters.getValue("today"));
		// Close flow: command stays 'Close'; lc_status indicates 'Closed'
		String SQL = "select * from reports where (command='Close' or command='Cancel') and ackn='' and redy='' and gm=(:userId) and added_on >(:today) order by id DESC limit 1";

		List<Reports> reports = jdbcTemplate.query(SQL, parameters, new RowMapper<Reports>() {
			@Override
			public Reports mapRow(ResultSet rs, int i) throws SQLException {
				Reports customer = new Reports();
				customer.setId(rs.getInt("id"));
				customer.setCommand(rs.getString("command"));
				return customer;
			}
		});
		
		if(reports.size()>0) {
			if(reports.get(0).getCommand().equalsIgnoreCase("Cancel")) {
				reports.get(0).setLc_status("true");
			}else {
			reports.get(0).setLc_status("false");
			}
		}
		
		return reports;
	}

	public void updateFailSafe(String id,Boolean status) {
		int i = jdbcTemplate1.update("update managegates set is_failsafe = ? where BOOM1_ID = ? OR handle = ?", new Object[] { String.valueOf(status), id ,id});
		System.out.println("parameters"+i);
		if(status) {
			int k = jdbcTemplate1.update("update managegates set status = ? where BOOM1_ID = ? OR handle = ?", new Object[] { "Open", id ,id});
		}
	}
	public Reports getAckandPN(String userId,String gateId) {
		
		Reports allReport = new Reports();
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String pattern = "yyyy-MM-dd 00:00:00";
		Calendar current = Calendar.getInstance();
		current.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		current.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DATE), 0, 0, 0);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		parameters.addValue("userId", userId);
		String date1 = simpleDateFormat.format(current.getTime());
		parameters.addValue("today", date1);
		System.out.println("parameters" + parameters.getValue("userId") + parameters.getValue("today"));
		// Close flow: command stays 'Close'; lc_status indicates 'Closed'
		String SQL = "select * from reports where command in ('Close','Cancel') and ackn='' and gm=(:userId) and added_on >(:today) order by id asc limit 1";
		
		//String SQL1 = "select * from reports where command='Cancel' and ackn='' and gm=(:userId) and added_on >(:today) order by id DESC limit 1";

		List<Reports> reports = jdbcTemplate.query(SQL, parameters, new RowMapper<Reports>() {
			@Override
			public Reports mapRow(ResultSet rs, int i) throws SQLException {
				Reports customer = new Reports();
				customer.setId(rs.getInt("id"));
				customer.setTn(rs.getString("tn"));
				customer.setWer(rs.getString("wer"));
				customer.setPn(rs.getString("pn"));
				customer.setCommand(rs.getString("command"));
				return customer;
			}
		});
		

	
		String id = checkstatus(userId,gateId);
		   if(id!=null) {
			   // Failsafe rule: if the gate is in active NO-NETWORK (unacknowledged) failsafe for this GM,
			   // do not show/generate expected GM PN.
			   if (isUnacknowledgedNoNetworkFailsafeActive(userId, gateId)) {
				   try {
					   userRepository.deleteReportIdGmPN(id);
				   } catch (Exception e) {
					   // ignore
				   }
				   allReport.setPn(null);
				   allReport.setId(Integer.parseInt(id));
				   return allReport;
			   }
			 // For GM flow: return expected GM PN (for lc_pin), NOT the SM PN (reports.pn)
			 String st = userRepository.findReportIdGmPN(id);
			 if (st == null || st.trim().isEmpty()) {
				 try {
					 // Ensure GM PN differs from SM PN stored in reports.pn
					 String smPn = null;
					 try {
						 List<Map<String, Object>> pnRows = jdbcTemplate1.queryForList("SELECT pn, lc FROM reports WHERE id=?", Integer.parseInt(id));
						 if (pnRows != null && !pnRows.isEmpty()) {
							 smPn = pnRows.get(0).get("pn") != null ? pnRows.get(0).get("pn").toString() : null;
							 String gateKey = pnRows.get(0).get("lc") != null ? pnRows.get(0).get("lc").toString() : gateId;
							 String gmPn = null;
							 for (int attempt = 0; attempt < 10; attempt++) {
								 String candidate = pnGenerationService.generateUniquePNForGate(gateKey);
								 if (smPn == null || smPn.trim().isEmpty() || !candidate.equals(smPn.trim())) {
									 gmPn = candidate;
									 break;
								 }
							 }
							 if (gmPn == null) {
								 gmPn = pnGenerationService.generateUniquePNForGate(gateKey);
							 }
							 st = gmPn;
							 userRepository.saveReportIdGmPN(id, st);
							 try {
								 userRepository.markPNAsUsed(gateKey, st);
							 } catch (Exception e) {
								 // ignore
							 }
						 }
					 } catch (Exception e) {
						 // ignore
					 }
				 } catch (Exception e) {
					 // ignore
				 }
			 }
			 //Query 
			 allReport.setPn(st);
			 allReport.setId(Integer.parseInt(id));
		   } 
		  
		  if(reports.size()>0) {
			  return reports.get(0);
		  }else {
			  
		  } 
		
		return allReport;
	}
	
	public String checkstatus(String username,String garname) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		Calendar newYearsEve = Calendar.getInstance();
		newYearsEve.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
	    newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
	    newYearsEve.add(Calendar.MINUTE, -30);
		// Using DateFormat format method we can create a string 
		// representation of a date with the defined format.
		String todayAsString = formatter.format(newYearsEve.getTime());
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		parameters.addValue("gateid", garname);
		parameters.addValue("added_on", todayAsString);


		String query = "SELECT id from reports where lc_name = (:gateid) and ackn != '' and lc_pin = '' and lc_status = 'Closed' and gm = (:username)  and added_on > (:added_on) order by id desc limit 1";

		List<String> strLst = jdbcTemplate.query(query,parameters, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int i) throws SQLException {
				return rs.getString(1);
				
			}
		});
		
		if ( strLst.isEmpty() ){
			  return null;
			}else if ( strLst.size() == 1 ) { // list contains exactly 1 element
			  return strLst.get(0);
			}
		return null;
	}
	
	public boolean ackCommand(int id) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		String format3 = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
		int i = jdbcTemplate1.update("update reports set ackn = ? where id = ? and (ackn IS NULL OR ackn = '')", new Object[] { format3, id });
		

		Reports managegates = jdbcTemplate1.queryForObject("select * from reports where id=?", new Object[] { id },
				new BeanPropertyRowMapper<Reports>(Reports.class));
		String date1 = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
	    
		Gate gate1 = new Gate(managegates.getLc_name(), managegates.getLc(), String.valueOf("500"),String.valueOf("500"));
		gate1.setBatch("Train");
		gate1.setDate(date1);
		kafkaTemplate.send(topic1,gate1);
		System.out.println("Train  Sent from Ack"+gate1.toString());
		
		if (i == 1) {
			updateQ(id,"ACK");
			return true;
		} else {
			return false;
		}

	}

	public boolean sendpn(int id, int pn) {
		// Validate PN: must be 3 digits, digits 1-9 only (no zero anywhere)
		if (!isValidThreeDigitNoZeroPN(pn)) {
			return false;
		}
		// Validate against expected GM PN for this reportId
		String reportId = String.valueOf(id);
		String expected = userRepository.findReportIdGmPN(reportId);
		if (expected == null || expected.trim().isEmpty()) {
			// No expected PN generated/stored yet -> reject (prevents accepting arbitrary PN)
			return false;
		}
		if (!expected.trim().equals(String.valueOf(pn))) {
			return false;
		}
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		String format3 = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
		int i = jdbcTemplate1.update(
			"update reports set lc_pin = ? ,lc_pin_time = ? where id = ? and (UPPER(lc_status) = ? OR UPPER(command) = ?)",
			new Object[] { pn, format3, id, "CLOSED", "CANCEL" });
		String st = String.valueOf(i);
		//ManageGatesService.reportid.remove(st);
		// Clear GM expected PN mapping once lc_pin is successfully stored
		if (i == 1) {
			userRepository.deleteReportIdGmPN(reportId);
			backfillMissingLcPinForSameGate(id);
			updateQ(id,"PN");
			
			return true;

		} else {
			return false;
		}

	}

	/**
	 * After GM sends PN for the latest report, fill lc_pin for all older Closed reports
	 * of the same gate+GM that are still missing lc_pin. Each backfilled PN is unique (24h).
	 */
	private void backfillMissingLcPinForSameGate(int currentReportId) {
		try {
			String rowSql = "SELECT gm, lc, lc_name FROM reports WHERE id = ? LIMIT 1";
			List<Map<String, Object>> currentRow = jdbcTemplate1.queryForList(rowSql, currentReportId);
			if (currentRow == null || currentRow.isEmpty()) return;
			Map<String, Object> row = currentRow.get(0);
			String gm = row.get("gm") != null ? row.get("gm").toString().trim() : null;
			String lc = row.get("lc") != null ? row.get("lc").toString().trim() : null;
			String lcName = row.get("lc_name") != null ? row.get("lc_name").toString().trim() : null;
			if (gm == null || gm.isEmpty() || (lc == null || lc.isEmpty()) && (lcName == null || lcName.isEmpty())) return;

			String boom1Id = (lc != null && !lc.isEmpty()) ? lc : lcName;
			String gateNum = (lcName != null && !lcName.isEmpty()) ? lcName : lc;

			String missingSql =
				"SELECT id FROM reports WHERE id < ? AND gm = ? " +
				"AND (lc IN (?,?) OR lc_name IN (?,?)) " +
				"AND (UPPER(lc_status) = 'CLOSED' OR UPPER(command) = 'CANCEL') " +
				"AND (tn IS NOT NULL AND tn != '') " +
				"AND (lc_pin IS NULL OR lc_pin = '') " +
				"ORDER BY id ASC";
			List<Map<String, Object>> missing = jdbcTemplate1.queryForList(missingSql,
				currentReportId, gm, boom1Id, gateNum, boom1Id, gateNum);

			if (missing == null || missing.isEmpty()) return;

			ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
			String lcPinTime = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
			String gateKey = (boom1Id != null && !boom1Id.isEmpty()) ? boom1Id : gateNum;

			for (Map<String, Object> m : missing) {
				Object idObj = m.get("id");
				if (idObj == null) continue;
				int olderId = ((Number) idObj).intValue();
				String uniquePn = null;
				for (int attempt = 0; attempt < 15; attempt++) {
					String candidate = pnGenerationService.generateUniquePNForGate(gateKey);
					if (candidate != null && !candidate.trim().isEmpty()) {
						uniquePn = candidate.trim();
						break;
					}
				}
				if (uniquePn == null) uniquePn = pnGenerationService.generateUniquePNForGate(gateKey);
				if (uniquePn == null || uniquePn.isEmpty()) continue;
				try {
					userRepository.markPNAsUsed(gateKey, uniquePn);
					if (gateNum != null && !gateNum.isEmpty() && !gateNum.equals(gateKey)) {
						userRepository.markPNAsUsed(gateNum, uniquePn);
					}
				} catch (Exception e) { /* continue */ }
				int up = jdbcTemplate1.update("UPDATE reports SET lc_pin = ?, lc_pin_time = ? WHERE id = ? AND (lc_pin IS NULL OR lc_pin = '')",
					uniquePn, lcPinTime, olderId);
				if (up <= 0) continue;
			}
		} catch (Exception e) {
			// Don't fail sendpn if backfill fails
		}
	}

	/*
	 * Validation DB queries (run after sendpn backfill to confirm correctness):
	 *
	 * 1) All Closed reports for a gate+gm in last 24h have lc_pin set:
	 *    SELECT id, gm, lc, lc_name, lc_status, lc_pin, lc_pin_time, added_on
	 *    FROM reports
	 *    WHERE gm = :gm AND (lc = :boom1Id OR lc_name = :gateNum)
	 *      AND UPPER(lc_status) = 'CLOSED'
	 *      AND added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
	 *    ORDER BY id ASC;
	 *
	 * 2) No duplicate lc_pin per gate in last 24h (duplicates would indicate a bug):
	 *    SELECT lc, lc_name, lc_pin, COUNT(*) AS cnt
	 *    FROM reports
	 *    WHERE (lc_pin IS NOT NULL AND lc_pin != '')
	 *      AND added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
	 *    GROUP BY lc, lc_name, lc_pin
	 *    HAVING COUNT(*) > 1;
	 *    (Expected: 0 rows.)
	 *
	 * 3) All lc_pin values are 3-digit, no zero (111-999, digits 1-9 only):
	 *    SELECT id, lc_pin FROM reports
	 *    WHERE lc_pin IS NOT NULL AND lc_pin != ''
	 *      AND (LENGTH(lc_pin) != 3 OR lc_pin REGEXP '[0]');
	 *    (Expected: 0 rows.)
	 */
	private static boolean isValidThreeDigitNoZeroPN(int pn) {
		if (pn < 111 || pn > 999) return false;
		return String.valueOf(pn).indexOf('0') < 0;
	}

	/**
	 * Returns true if an unacknowledged NO-NETWORK report exists for the given gate (lc_name)
	 * and GM (gm). When active, expected GM PN must not be generated.
	 */
	private boolean isUnacknowledgedNoNetworkFailsafeActive(String gm, String lcName) {
		if (gm == null || gm.trim().isEmpty()) return false;
		if (lcName == null || lcName.trim().isEmpty()) return false;
		try {
			Integer count = jdbcTemplate1.queryForObject(
				"SELECT COUNT(*) FROM reports WHERE command='NO-NETWORK' AND lc_name=? AND gm=? AND (ackn IS NULL OR ackn = '')",
				Integer.class,
				lcName.trim(),
				gm.trim()
			);
			return count != null && count > 0;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean updateQ(int id,String state) {
		Reports managegates = jdbcTemplate1.queryForObject("select * from reports where id=?", new Object[] { id },
				new BeanPropertyRowMapper<Reports>(Reports.class));
		// After ACK for a closed gate, generate a NEW GM expected PN (different from SM PN) for lc_pin validation.
		// This is separate from SM PN stored in reports.pn.
		try {
			if ((flag.get(managegates.getLc()) != null) && (flag.get(managegates.getLc()).equals("closed")) && (state.equals("ACK"))) {
				// If failsafe is active (unacknowledged NO-NETWORK for this gate+GM), do not generate expected GM PN.
				if (!isUnacknowledgedNoNetworkFailsafeActive(managegates.getGm(), managegates.getLc_name())) {
					String reportId = String.valueOf(id);
					String existing = userRepository.findReportIdGmPN(reportId);
					if (existing == null || existing.trim().isEmpty()) {
						String smPn = managegates.getPn(); // SM PN stored in reports.pn
						String gateKey = managegates.getLc();
						String gateName = managegates.getLc_name();

						String gmPn = null;
						for (int attempt = 0; attempt < 10; attempt++) {
							String candidate = pnGenerationService.generateUniquePNForGate(gateKey);
							if (smPn == null || smPn.trim().isEmpty() || !candidate.equals(smPn.trim())) {
								gmPn = candidate;
								break;
							}
						}
						if (gmPn == null) {
							// Extremely unlikely fallback
							gmPn = pnGenerationService.generateUniquePNForGate(gateKey);
						}

						userRepository.saveReportIdGmPN(reportId, gmPn);
						try {
							// Reserve in Redis uniqueness (per gate) so it won't be reused within 24h
							if (gateKey != null && !gateKey.trim().isEmpty()) {
								userRepository.markPNAsUsed(gateKey.trim(), gmPn);
							}
							if (gateName != null && !gateName.trim().isEmpty()) {
								userRepository.markPNAsUsed(gateName.trim(), gmPn);
							}
						} catch (Exception e) {
							// Not fatal; uniqueness also enforced by DB scan, but Redis helps.
						}
					}
				}
			}
		} catch (Exception e) {
			// Don't block queue update/audio if PN generation fails
		}
		updateQueue(managegates.getSm(),state, managegates.getLc_name());
		return true;
	}

	public  void updateQueue(String username, String status, String gate_id) {
		
		System.out.print("API"+username+status+gate_id);
	
			if ("ACK".equals(status)) {
				
				userRepository.pushAudio(username, gate_id + " acknowledged");

		} else if ("closed".equals(status)) {
			// Commented out - closed audio now managed by J_TRACK only
			// userRepository.pushAudio(username,gate_id + " Closed");


		} else if ("open".equals(status)) {

				userRepository.pushAudio(username,gate_id + " Opened");


			} else if ("PN".equals(status)) {
				userRepository.pushAudio(username,gate_id + "P N received");
			}
			else if ("ROPN".equals(status)) {
				//userRepository.pushAudio(username,gate_id + " Requesting Open P N");
				userRepository.pushAudio1(username,gate_id + " Requesting Open P N");
			}
			else if ("RCPN".equals(status)) {
				userRepository.pushAudio1(username,gate_id + " Requesting Close P N");
			}
			else if ("REOPN".equals(status)) {
				//userRepository.pushAudio(username,gate_id + " Requesting Open P N");
				userRepository.pushAudio(username," Open P N Received");
			}
			
			else if ("RECPN".equals(status)) {
				userRepository.pushAudio(username," Close P N Received");
			}
		

	}

	public static int getRandomNumberWithExclusion( )
	{
	  Random r = new Random();
	  int result = -1;

	  do
	  {
		  	// 3-digit number, digits 1-9 only (no zeros anywhere)
		  	result = 111 + r.nextInt(889);
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

}

