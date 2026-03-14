package com.jsinfotech.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.BigAdmin.Controller.DashboardController;
import com.jsinfotech.BigAdmin.Domain.Data;
import com.jsinfotech.Domain.API;
import com.jsinfotech.Domain.Gate;
import com.jsinfotech.Domain.Gate2;
import com.jsinfotech.Domain.Gates;
import com.jsinfotech.Domain.ManageGates;


@Service
public class ManageGatesService {

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	JdbcTemplate jdbcTemplate1;
	
	@Autowired
	private ProcessServiceImpl processService;
	
	@Autowired
	AcknowledgementService ackService;
	
	@Autowired
	private ReportsService reportsService;
	
	@Autowired
	private RedisUserRepository userRepository;
	
	@Autowired
	private SMFailsafeService smFailsafeService;
	
    @Autowired
	private PNGenerationService pnGenerationService;
	
    @Autowired
	private KafkaTemplate<String, Gate> kafkaTemplate;
    
    @Value(value = "${kafka.topic1}")
    private String topic1;
    
    @Value(value = "${kafka.topic2}")
    private String topic2;

	
    private static final Logger logger = LogManager.getLogger(DashboardController.class);

	
	public static Map<String,Integer> status = new HashMap<String, Integer>();
	public static Map<String,String> reportid = new HashMap<String, String>();
	public static Map<String,String> reportid1 = new HashMap<String, String>();



	public List<ManageGates> findByUsername(String username,String rolename) {

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
				
				// Get status and convert to lowercase
				String mainStatus = rs.getString("status");
				if(mainStatus != null) {
					customer.setStatus(mainStatus.toLowerCase());
				} else {
					customer.setStatus("open");
				}
				
				// Set internal fields (not exposed in JSON due to @JsonIgnore)
				customer.setBoom1Id(rs.getString("BOOM1_ID"));
				customer.setBoom2Id(rs.getString("BOOM2_ID"));
				customer.setHandle(rs.getString("handle"));
				
				customer.setGateNum(rs.getString("Gate_Num"));
				customer.setSM(rs.getString("sm"));
				customer.setGM(rs.getString("GM"));
				
				// Set actual sensor statuses from database
				String bs1Status = rs.getString("BS1_STATUS");
				String bs2Status = rs.getString("BS2_STATUS");
				String ltStatus = rs.getString("LT_STATUS");
				String leverStatus = rs.getString("LEVER_STATUS");
				
				// Set sensor statuses (default to "open" if null)
				String bs1StatusLower = bs1Status != null ? bs1Status.toLowerCase() : "open";
				String bs2StatusLower = bs2Status != null ? bs2Status.toLowerCase() : "open";
				String ltStatusLower = ltStatus != null ? ltStatus.toLowerCase() : "open";
				String leverStatusLower = leverStatus != null ? leverStatus.toLowerCase() : "open";
				
				customer.setBs1Status(bs1StatusLower);
				customer.setBs2Status(bs2StatusLower);
				customer.setLtStatus(ltStatusLower);
				customer.setLeverStatus(leverStatusLower);
				
				// Set boolean fields: closed = true, open = false
				customer.setBoomOne("closed".equalsIgnoreCase(bs1StatusLower));
				customer.setBoomTwo("closed".equalsIgnoreCase(bs2StatusLower));
				customer.setBoomLock("closed".equalsIgnoreCase(ltStatusLower));
				customer.setLeverCloser("closed".equalsIgnoreCase(leverStatusLower));
				
				return customer;
			}
		});
		
		for(ManageGates m:l) {
			
			m.setId(k);
			k++;
			
			// Populate boom_lock_play_commad field based on latest unacknowledged report
			// Shows: "750 Opened", "750 Closed", or "750 boom lock is healthy/unhealthy"
			try {
				String boom1Id = m.getBoom1Id();
				String gateNum = m.getGateNum();
				
				// Get play command based on latest unacknowledged report for this gate
				String playCommand = reportsService.getPlayCommandForGate(username, rolename, gateNum, boom1Id);
				m.setBoom_lock_play_commad(playCommand);
			} catch (Exception e) {
				logger.warn("Error getting play command for gate: {} (BOOM1_ID: {}): {}", m.getGateNum(), m.getBoom1Id(), e.getMessage());
				m.setBoom_lock_play_commad(null); // Set null on error
			}
			
			// Populate failsafe status (play_command field removed)
			try {
				String boom1Id = m.getBoom1Id();
				String gateNum = m.getGateNum();
				
				// Check if gate has unacknowledged NO-NETWORK reports
				String failsafeCheckQuery = "SELECT COUNT(*) as cnt FROM reports WHERE command='NO-NETWORK' AND (lc_name = ? OR lc = ? OR lc_name = ?) AND (ackn IS NULL OR ackn = '')";
				Integer failsafeCount = jdbcTemplate1.queryForObject(failsafeCheckQuery, new Object[] {gateNum, boom1Id, gateNum}, Integer.class);
				
				boolean isFailsafe = (failsafeCount != null && failsafeCount > 0);
				m.setIs_failsafe(isFailsafe);
			} catch (Exception e) {
				logger.warn("Error querying failsafe status for gate: {} (BOOM1_ID: {}): {}", m.getGateNum(), m.getBoom1Id(), e.getMessage());
				m.setIs_failsafe(false);
			}
		}
		if(rolename.indexOf('s')!=-1) {
			  for(ManageGates m: l) {
				  logger.debug("Checking open status for gate: {} (BOOM1_ID: {})", m.getGateNum(), m.getBoom1Id());
				  String id = checkopenstatus(username,m.getBoom1Id());
				   if(id!=null) {
						 logger.debug("Gate opened for SM: {} - report ID: {}", username, id);
//						if(reportid1.get(id)==null) {
//							reportid1.put(id, "T");
//							ackService.updateQueue(m.getSM(), "open",m.getGateNum());
//						 }
						
						if(userRepository.findReportId(id)==null) {
							userRepository.saveReportId(id, "T");
							ackService.updateQueue(m.getSM(), "open",m.getGateNum());
						 }

					 }
				   } 
			  }
		if(rolename.indexOf('g')!=-1) {
		  for(ManageGates m: l) {
			  // Only set reportId and pn when status is "closed"
			  String currentStatus = m.getStatus();
			  if(currentStatus != null && currentStatus.equalsIgnoreCase("closed")) {
				  // Always use the LATEST Closed report row for this gate (by lc_status),
				  // not an older "pending PN" row. This ensures getstatus returns the latest reportId only.
				  String id = findLatestClosedReportIdForGate(username, m.getBoom1Id(), m.getGateNum());
				  
				   if(id!=null) {
					 // Set reportId field (previously misused as bs1Go)
					 m.setReportId(id);
					 // Keep bs1Go for backward compatibility (internal use only)
					 m.setBs1Go(id);
					 
					 // Only expose gm_pn when the selected reportId has a train number (tn).
					 // If tn is empty, return gm_pn as null and do not generate/store expected GM PN.
					 boolean hasTn = reportHasNonEmptyTn(id);
					 if (!hasTn) {
						 m.setPn(null);
						 m.setBs1Gc(null);
						 continue;
					 }

					 // If lc_pin is already set for this report, GM PN has already been applied/sent.
					 // Do not show/generate a new gm_pn for the same reportId.
					 boolean hasLcPin = reportHasNonEmptyLcPin(id);
					 if (hasLcPin) {
						 m.setPn(null);
						 m.setBs1Gc(null);
						 continue;
					 }
					 
					 // If report is not acknowledged yet (ackn is empty), do not show gm_pn.
					 // gm_pn should only be shown after user acknowledges the report via ACK/sendpn.
					 boolean hasAckn = reportHasNonEmptyAckn(id);
					 if (!hasAckn) {
						 m.setPn(null);
						 m.setBs1Gc(null);
						 continue;
					 }
					 
					 // GM expected PN (for lc_pin). This must be different from SM PN stored in reports.pn.
					 // After ACK, we need to show gm_pn so user can send it via ACK/sendpn.
					 String st = userRepository.findReportIdGmPN(id);
					 
					 // If not found in Redis, generate on-demand (fallback) and store as GM PN.
					 // This ensures gm_pn is always available after ACK, even if updateQ() didn't generate it.
					 if (st == null || st.trim().isEmpty()) {
						 try {
							 // Read SM PN from DB so we can ensure GM PN differs
							 String smPn = getPnFromDatabase(id);
							 String gmPn = null;
							 for (int attempt = 0; attempt < 10; attempt++) {
								 String candidate = pnGenerationService.generateUniquePNForGate(m.getBoom1Id());
								 if (smPn == null || smPn.trim().isEmpty() || !candidate.equals(smPn.trim())) {
									 gmPn = candidate;
									 break;
								 }
							 }
							 if (gmPn == null) {
								 // Extremely unlikely fallback
								 gmPn = pnGenerationService.generateUniquePNForGate(m.getBoom1Id());
							 }
							 st = gmPn;
							 // Store as GM PN (not SM PN) so it can be validated in sendpn()
							 userRepository.saveReportIdGmPN(id, st);
							 try {
								 // Reserve in Redis uniqueness (per gate) so it won't be reused within 24h
								 userRepository.markPNAsUsed(m.getBoom1Id(), st);
								 if (m.getGateNum() != null && !m.getGateNum().trim().isEmpty()) {
									 userRepository.markPNAsUsed(m.getGateNum(), st);
								 }
							 } catch (Exception e) {
								 // Not fatal; uniqueness also enforced by DB scan, but Redis helps.
								 logger.debug("Failed to mark GM PN as used in Redis for gate keys, reportId: {}, PN: {}", id, st, e);
							 }
						 } catch (Exception e) {
							 // If generation fails, log warning but continue - we'll try one more time below
							 logger.warn("Failed to generate GM PN on-demand for reportId: {}, gate: {} (BOOM1_ID: {})",
								 id, m.getGateNum(), m.getBoom1Id(), e);
						 }
					 }
					 
					 // Final fallback: if GM PN generation above failed, try one more time
					 // This ensures gm_pn is always available after ACK.
					 if (st == null || st.trim().isEmpty()) {
						 try {
							 String gmPn = pnGenerationService.generateUniquePNForGate(m.getBoom1Id());
							 st = gmPn;
							 userRepository.saveReportIdGmPN(id, st);
							 try {
								 userRepository.markPNAsUsed(m.getBoom1Id(), st);
								 if (m.getGateNum() != null && !m.getGateNum().trim().isEmpty()) {
									 userRepository.markPNAsUsed(m.getGateNum(), st);
								 }
							 } catch (Exception e) {
								 logger.debug("Failed to mark final fallback GM PN as used in Redis, reportId: {}, PN: {}", id, st, e);
							 }
						 } catch (Exception e) {
							 logger.warn("Final fallback GM PN generation failed for reportId: {}, gate: {} (BOOM1_ID: {})",
								 id, m.getGateNum(), m.getBoom1Id(), e);
							 // If all attempts fail, st will remain null and gm_pn will be null
						 }
					 }
					 // Set pn field as GM PN (exposed as gm_pn via @JsonProperty)
					 m.setPn(st);
					 // Keep bs1Gc for backward compatibility (internal use only)
					 m.setBs1Gc(st);
					 //Query 
				   } else {
					   // No report found, set to null
					   m.setReportId(null);
					   m.setPn(null);
				   }
			  } else {
				  // Status is "open" or not "closed", set reportId and pn to null
				  m.setReportId(null);
				  m.setPn(null);
			  }
		  }
		}

        return l;
	}

	/**
	 * Return the latest reports.id for this GM + gate where lc_status is Closed (case-insensitive).
	 * Uses both BOOM1_ID and Gate_Num matches against reports.lc and reports.lc_name.
	 * This is used for GM getstatus response so we always show ONLY the latest reportId per gate.
	 */
	private String findLatestClosedReportIdForGate(String gmUsername, String boom1Id, String gateNum) {
		try {
			// Prefer BOOM1_ID, but include Gate_Num as well since reports might store either in lc/lc_name.
			String b1 = (boom1Id != null) ? boom1Id.trim() : "";
			String gn = (gateNum != null) ? gateNum.trim() : "";
			if (b1.isEmpty() && gn.isEmpty()) {
				return null;
			}
			
			String sql =
				"SELECT id FROM reports " +
				"WHERE gm = ? " +
				"AND UPPER(lc_status) = 'CLOSED' " +
				"AND added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
				"AND (lc IN (?,?) OR lc_name IN (?,?)) " +
				"ORDER BY added_on DESC, id DESC " +
				"LIMIT 1";
			
			// Use empty strings safely; IN (?,?) with "" will just not match anything.
			List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, gmUsername, b1, gn, b1, gn);
			if (rows == null || rows.isEmpty()) {
				return null;
			}
			Object idObj = rows.get(0).get("id");
			return idObj != null ? idObj.toString() : null;
		} catch (Exception e) {
			logger.warn("Error finding latest Closed reportId for GM: {}, BOOM1_ID: {}, Gate_Num: {}", gmUsername, boom1Id, gateNum, e);
			return null;
		}
	}
	
	/**
	 * True if reports.tn is non-null and non-empty for the given reportId.
	 */
	private boolean reportHasNonEmptyTn(String reportId) {
		try {
			if (reportId == null || reportId.trim().isEmpty()) {
				return false;
			}
			List<Map<String, Object>> rows = jdbcTemplate1.queryForList("SELECT tn FROM reports WHERE id=? LIMIT 1", reportId.trim());
			if (rows == null || rows.isEmpty()) {
				return false;
			}
			Object tnObj = rows.get(0).get("tn");
			String tn = tnObj != null ? tnObj.toString() : null;
			return tn != null && !tn.trim().isEmpty();
		} catch (Exception e) {
			logger.warn("Error checking tn for reportId: {}", reportId, e);
			return false;
		}
	}
	
	/**
	 * True if reports.lc_pin is non-null and non-empty for the given reportId.
	 * If lc_pin is already present, GM PN should NOT be shown/generated again for that report.
	 */
	private boolean reportHasNonEmptyLcPin(String reportId) {
		try {
			if (reportId == null || reportId.trim().isEmpty()) {
				return false;
			}
			List<Map<String, Object>> rows = jdbcTemplate1.queryForList("SELECT lc_pin FROM reports WHERE id=? LIMIT 1", reportId.trim());
			if (rows == null || rows.isEmpty()) {
				return false;
			}
			Object pinObj = rows.get(0).get("lc_pin");
			String lcPin = pinObj != null ? pinObj.toString() : null;
			return lcPin != null && !lcPin.trim().isEmpty();
		} catch (Exception e) {
			logger.warn("Error checking lc_pin for reportId: {}", reportId, e);
			return false;
		}
	}
	
	/**
	 * True if reports.ackn is non-null and non-empty for the given reportId.
	 * If ackn is empty, the report has not been acknowledged yet, so gm_pn should NOT be shown.
	 */
	private boolean reportHasNonEmptyAckn(String reportId) {
		try {
			if (reportId == null || reportId.trim().isEmpty()) {
				return false;
			}
			List<Map<String, Object>> rows = jdbcTemplate1.queryForList("SELECT ackn FROM reports WHERE id=? LIMIT 1", reportId.trim());
			if (rows == null || rows.isEmpty()) {
				return false;
			}
			Object acknObj = rows.get(0).get("ackn");
			String ackn = acknObj != null ? acknObj.toString() : null;
			return ackn != null && !ackn.trim().isEmpty();
		} catch (Exception e) {
			logger.warn("Error checking ackn for reportId: {}", reportId, e);
			return false;
		}
	}

	public List<API> findByUsernameAsAPI(String username, String rolename) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		String query = "";
		if (rolename.equalsIgnoreCase("sm")) {
			query = "select * from managegates where SM=(:username)";
		} else if (rolename.equalsIgnoreCase("gm")) {
			query = "select * from managegates where GM=(:username)";
		}
		
		List<API> apiList = jdbcTemplate.query(query, parameters, new RowMapper<API>() {
			@Override
			public API mapRow(ResultSet rs, int i) throws SQLException {
				API api = new API();
				api.setId(rs.getInt("id"));
				// gate field removed - use boom1_id instead
				api.setLc_name(rs.getString("Gate_Num"));
				api.setSm(rs.getString("SM"));
				api.setGm(rs.getString("GM"));
				
				// Set gate identification fields
				api.setBoom1Id(rs.getString("BOOM1_ID"));
				api.setBoom2Id(rs.getString("BOOM2_ID"));
				api.setLtswId(rs.getString("LTSW_ID"));  // nullable field
				api.setHandle(rs.getString("handle"));
				
				// Get status fields
				String bs1Status = rs.getString("BS1_STATUS");
				String bs2Status = rs.getString("BS2_STATUS");
				String ltStatus = rs.getString("LT_STATUS");
				String leverStatus = rs.getString("LEVER_STATUS");
				
				// Set string status fields
				api.setBs1Status(bs1Status != null ? bs1Status : "open");
				api.setBs2Status(bs2Status != null ? bs2Status : "open");
				api.setLtStatus(ltStatus != null ? ltStatus : "open");
				api.setLeverStatus(leverStatus != null ? leverStatus : "open");
				
				// Convert string status to boolean fields
				// "closed" (case-insensitive) → true, "open" or null → false
				api.setBoomOne(bs1Status != null && bs1Status.equalsIgnoreCase("closed"));
				api.setBoomTwo(bs2Status != null && bs2Status.equalsIgnoreCase("closed"));
				api.setBoomLock(ltStatus != null && ltStatus.equalsIgnoreCase("closed"));
				api.setLeverCloser(leverStatus != null && leverStatus.equalsIgnoreCase("closed"));
				
				// Set other fields if available
				api.setStatus(rs.getString("status"));
				api.setBs1Go(rs.getString("BS1_GO"));
				api.setBs1Gc(rs.getString("BS1_GC"));
				api.setBs2Go(rs.getString("BS2_GO"));
				api.setBs2Gc(rs.getString("BS2_GC"));
				
				// LS_GO and LS_GC are VARCHAR in database, need to parse to int
				String lsGoStr = rs.getString("LS_GO");
				String lsGcStr = rs.getString("LS_GC");
				try {
					api.setLsGo(lsGoStr != null && !lsGoStr.isEmpty() ? Integer.parseInt(lsGoStr) : 0);
				} catch (NumberFormatException e) {
					api.setLsGo(0);
				}
				try {
					api.setLsGc(lsGcStr != null && !lsGcStr.isEmpty() ? Integer.parseInt(lsGcStr) : 0);
				} catch (NumberFormatException e) {
					api.setLsGc(0);
				}
				
				// Pass field doesn't exist in managegates table, set to null
				api.setPass(null);
				
				java.sql.Timestamp addedOn = rs.getTimestamp("added_on");
				if (addedOn != null) {
					api.setAdded_on(new Date(addedOn.getTime()));
				}
				
				return api;
			}
		});
		
		return apiList;
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

	public String checkstatus(String username,String garname) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		parameters.addValue("gateid", garname);

		// Query for MOST RECENT report that needs PN: acknowledged, no lc_pin set yet, closed status
		// Changed: Removed strict 30-minute time restriction, use 24-hour window instead
		// Changed: Order by id DESC to get the newest report first
		// Once lc_pin is set via sendpn endpoint, this report won't be returned
		String query = "SELECT id from reports where lc = (:gateid) and ackn != '' and (lc_pin IS NULL OR lc_pin = '') and lc_status = 'Closed' and gm = (:username) and added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR) order by id DESC limit 1";

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
	public  boolean isBetween(LocalTime candidate, LocalTime start, LocalTime end) {
		  return !candidate.isBefore(start) && !candidate.isAfter(end);  // Inclusive.
		}
	
	/**
	 * Find the most recent closed report with train number for a gate manager
	 * This is a fallback method when checkstatus() doesn't find a report
	 * More flexible query that doesn't require all strict conditions
	 * Fixed: Removed strict 30-minute time restriction, use 24-hour window and order by id DESC
	 */
	private String findRecentClosedReport(String username, String gateId) {
		try {
			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("username", username);
			parameters.addValue("gateid", gateId);
			
			// More flexible query: find closed reports with train number, prioritize those needing PN
			// First try: acknowledged reports without PN (lc_pin is empty/null)
			// Changed: Use 24-hour window instead of 30-minute, order by id DESC to get newest first
			// Once lc_pin is set via sendpn endpoint, these reports won't be returned
			String query1 = "SELECT id from reports where (lc = (:gateid) OR lc_name = (:gateid)) and ackn != '' and (lc_pin IS NULL OR lc_pin = '') and (UPPER(lc_status) = 'CLOSED' OR UPPER(command) = 'CLOSED') and gm = (:username) and (tn IS NOT NULL AND tn != '') and added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR) order by id DESC limit 1";
			List<String> strLst = jdbcTemplate.query(query1, parameters, new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int i) throws SQLException {
					return rs.getString(1);
				}
			});
			
			if(strLst != null && !strLst.isEmpty()) {
				return strLst.get(0);
			}
			
			// Second try: any closed report with train number that doesn't have lc_pin set yet
			// Exclude reports where lc_pin is already set (via sendpn)
			// Changed: Use 24-hour window instead of 30-minute, order by id DESC to get newest first
			String query2 = "SELECT id from reports where (lc = (:gateid) OR lc_name = (:gateid)) and (UPPER(lc_status) = 'CLOSED' OR UPPER(command) = 'CLOSED') and gm = (:username) and (tn IS NOT NULL AND tn != '') and (lc_pin IS NULL OR lc_pin = '') and added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR) order by id DESC limit 1";
			strLst = jdbcTemplate.query(query2, parameters, new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int i) throws SQLException {
					return rs.getString(1);
				}
			});
			
			if(strLst != null && !strLst.isEmpty()) {
				return strLst.get(0);
			}
			
			// Third try: any closed report (with or without train number) for this gate and GM
			// This ensures we find the latest Closed report even if train wasn't sent
			String query3 = "SELECT id from reports where (lc = (:gateid) OR lc_name = (:gateid)) and (UPPER(lc_status) = 'CLOSED' OR UPPER(command) = 'CLOSED') and gm = (:username) and added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR) order by id DESC limit 1";
			strLst = jdbcTemplate.query(query3, parameters, new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int i) throws SQLException {
					return rs.getString(1);
				}
			});
			
			if(strLst != null && !strLst.isEmpty()) {
				return strLst.get(0);
			}
			
		} catch (Exception e) {
			logger.error("Error in findRecentClosedReport for gate: {}, username: {}", gateId, username, e);
		}
		return null;
	}
	
	/**
	 * Get PN from database directly (not just Redis)
	 */
	private String getPnFromDatabase(String reportId) {
		try {
			String query = "SELECT pn FROM reports WHERE id = ? AND (pn IS NOT NULL AND pn != '')";
			List<String> pnList = jdbcTemplate1.query(query, new Object[]{reportId}, new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int i) throws SQLException {
					return rs.getString("pn");
				}
			});
			
			if(pnList != null && !pnList.isEmpty()) {
				String pn = pnList.get(0);
				if(pn != null && !pn.trim().isEmpty()) {
					return pn.trim();
				}
			}
		} catch (Exception e) {
			System.err.println("Error getting PN from database: " + e.getMessage());
		}
		return null;
	}
	public String checkopenstatus(String username,String garname) {
		
		Date date = new Date();
		DateFormat format = new SimpleDateFormat("HH:mm");
		format.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        int interval = -30;
		LocalTime time = LocalTime.parse(format.format(date));
		if(isBetween(time, LocalTime.of(00, 0), LocalTime.of(02, 0))){
			formatter = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
			formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
			interval =0;
		}
		
		Calendar newYearsEve = Calendar.getInstance();
		newYearsEve.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
	    newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
	    newYearsEve.add(Calendar.MINUTE, interval);
	    newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
		// Using DateFormat format method we can create a string 
		// representation of a date with the defined format.
		String todayAsString = formatter.format(newYearsEve.getTime());
		SimpleDateFormat formatter1 = new SimpleDateFormat("HH:mm:ss");
		formatter1.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));

		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		parameters.addValue("gateid", garname);
		parameters.addValue("added_on", todayAsString);


		// Fixed: Use gm instead of sm for GM role, check both lc and lc_name, remove restrictive conditions
		// Priority: Latest Closed report for this gate and GM, regardless of train number
		String query = "SELECT id from reports where (lc = (:gateid) OR lc_name = (:gateid)) and (UPPER(lc_status) = 'CLOSED' OR UPPER(command) = 'CLOSED') and gm = (:username) and added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR) order by id desc limit 1";

		List<String> strLst = jdbcTemplate.query(query,parameters, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int i) throws SQLException {
				return rs.getString(1);
				
			}
		});
		
		// Fixed: Changed size check from > 1 to > 0 (critical bug fix)
		if (strLst != null && strLst.size() > 0) {
			  return strLst.get(0);
		} else {
				return null;
			}
	}

	public void insertGateData(String gateId,String field2,String field3) {

		if(gateId!=null){


		}

	}


	
	/**public static Timer t;

	public synchronized void startPollingTimer() {
	        if (t == null) {
	            TimerTask task = new TimerTask() {
	                @Override
	                public void run() {
	                	GetGateTimeDiff("","");	                }
	            };

	            t = new Timer();
	            t.scheduleAtFixedRate(task, 0, 1000);
	        }
	    }**/
	
	public Boolean GetGateFailSafeStatus(String username,String role) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);
		
		Boolean is_sm=false;
		Boolean is_gm=true;

		String  query = "";
		if (role.indexOf('s')!=-1) {
			query = "select * from managegates where SM=(:username)";
			List<ManageGates> li = jdbcTemplate.query(query,parameters, new RowMapper<ManageGates>() {		
				@Override
				public ManageGates mapRow(ResultSet rs, int i) throws SQLException {
					ManageGates gateid =  new ManageGates();
					gateid.setIs_failsafe(Boolean.parseBoolean(rs.getString("is_failsafe")));
					gateid.setGateNum(rs.getString("Gate_Num"));
					gateid.setBoom1Id(rs.getString("BOOM1_ID"));
					return gateid;
				}
			});
			for(ManageGates g:li) {
				is_sm = is_sm || g.getIs_failsafe();
			}
			return is_sm;
            
		}
		else if(role.indexOf('g')!=-1)
		{

			query = "select * from managegates where GM=(:username)";
			List<ManageGates> li = jdbcTemplate.query(query,parameters, new RowMapper<ManageGates>() {		
				@Override
				public ManageGates mapRow(ResultSet rs, int i) throws SQLException {
					ManageGates gateid =  new ManageGates();
					gateid.setIs_failsafe(Boolean.parseBoolean(rs.getString("is_failsafe")));
					gateid.setGateNum(rs.getString("Gate_Num"));
					gateid.setBoom1Id(rs.getString("BOOM1_ID"));
					return gateid;
				}
			});
			
			String name = null;
			String[] parts = li.get(0).getBoom1Id().split("-");
			name = parts[1];
			if(parts[1].indexOf("BS")!=-1 ) {
				name = name.replace("BS", "");
			}else {
				name = name.replace("LS", "");
			}
			String pattern = "yyyy-MM-dd hh:mm:ss";
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
			simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
			
			Calendar newYearsEve = Calendar.getInstance();
			newYearsEve.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
	 	    newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
	 		// Using DateFormat format method we can create a string 
	 		// representation of a date with the defined format.
	 		String date = simpleDateFormat.format(newYearsEve.getTime());
	 		
			Gate2 gate1 = new Gate2(name, li.get(0).getBoom1Id(), String.valueOf("434"),String.valueOf("343"));
			gate1.setBatch("Fail");
			gate1.setDate(date);
			//kafkaTemplate.send(topic1,gate1);
			
			System.out.print("fafa"+li.get(0).getIs_failsafe());
			
			return li.get(0).getIs_failsafe();

		}
		
		return false;
		 
	}
	public Boolean failsafeinit() {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", "");
		
		String query = "select * from managegates";
		List<ManageGates> li = jdbcTemplate.query(query,parameters, new RowMapper<ManageGates>() {		
			@Override
			public ManageGates mapRow(ResultSet rs, int i) throws SQLException {
				ManageGates gateid =  new ManageGates();
				gateid.setIs_failsafe(Boolean.parseBoolean(rs.getString("is_failsafe")));
				gateid.setGateNum(rs.getString("Gate_Num"));
				gateid.setBoom1Id(rs.getString("BOOM1_ID"));
				return gateid;
			}
		});
		
		
		for(ManageGates manageGates : li) {
			
			String name = null;
			String[] parts = manageGates.getBoom1Id().split("-");
			name = parts[1];
			if(parts[1].indexOf("BS")!=-1 ) {
				name = name.replace("BS", "");
			}else {
				name = name.replace("LS", "");
			}
			logger.debug("Processing gate for failsafe init: {} (BOOM1_ID: {})", name, manageGates.getBoom1Id());
			String pattern = "yyyy-MM-dd hh:mm:ss";
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
			simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
			
			Calendar newYearsEve = Calendar.getInstance();
			newYearsEve.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
	 	    newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
	 		// Using DateFormat format method we can create a string 
	 		// representation of a date with the defined format.
	 		String date = simpleDateFormat.format(newYearsEve.getTime());
	 		
			Gate gate1 = new Gate(name, manageGates.getBoom1Id(), String.valueOf("434"),String.valueOf("343"));
			gate1.setBatch("Fail");
			gate1.setDate(date);
			kafkaTemplate.send(topic1,gate1);
		}
		return true;
	}

	public Boolean GetGateTimeDiff(String username,String role) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("username", username);

		String  query = "";
		if (role.indexOf('s')!=-1) {
			query = "select * from managegates where SM=(:username)";

		}
		else if(role.indexOf('g')!=-1)
		{

			query = "select * from managegates where GM=(:username)";

		}	


		List<Gates> li = jdbcTemplate.query(query,parameters, new RowMapper<Gates>() {		
			@Override
			public Gates mapRow(ResultSet rs, int i) throws SQLException {
				Gates gateid =  new Gates();
				gateid.setGate(rs.getString("BOOM1_ID"));
				gateid.setId(rs.getInt("Id"));
				return gateid;
			}
		});
		List<Gates> failedidslist = null;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		Calendar newYearsEve = Calendar.getInstance();
		newYearsEve.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
	    newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
	    newYearsEve.add(Calendar.MINUTE, -1);
		// Using DateFormat format method we can create a string 
		// representation of a date with the defined format.
		String todayAsString = formatter.format(newYearsEve.getTime());
		Boolean status = false;

		for(Gates l : li) {
			 	
			parameters.addValue("ids", l.getGate());
			parameters.addValue("added_on", todayAsString);
			String sql="select id,gate,TIMESTAMPDIFF(SECOND,added_on,now()) as time_diff from  gates where gate IN (:ids) and added_on > (:added_on) order by id desc limit 1";		
	        try {
			 failedidslist= jdbcTemplate.query(sql,parameters, new RowMapper<Gates>() {		
				@Override
				public Gates mapRow(ResultSet rs, int i) throws SQLException {
				   Gates failedids =  new Gates();
				   failedids.setStatus("false");
                   return failedids;

				}
			 });
			if(failedidslist.size()<1)	{
				        status = true;
						int i1 = jdbcTemplate1.update("update managegates set status = 'Open' where Id = ?", new Object[] { l.getId()});
						//jdbcTemplate1.update("insert into reports (tn,pn,lc_name,tn_time,command,wer,sm,lc_status,gm) values(?,?,?,?,?,?,?,?,?)","Train","",todayAsString,"","","","",l.getGate(),"Fail Safe");
						status = true;
						logger.debug("Failsafe condition met for gate: {}", l.getGate());
					}
			}catch(Exception e) {
		logger.error("Error in failsafe check: {}", e.getMessage(), e);
	}
	}

		return status;


	}




	public List<API> addstatusofgate(int gate2, int status2, int statuss2,int pass2, HttpServletRequest request ) {
		API api =  new  API();
		int gate= gate2;
		int status= status2;
		int statuss= statuss2;
		int pass= pass2;
		logger.debug("Processing gate: {}, status: {}, statuss: {}, pass: {}", gate, status, statuss, pass);
		
		// Record SM data received for failsafe tracking
		recordSMDataForFailsafe(String.valueOf(gate));
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();



		if (gate!=0) 
		{
			//select managegate by checking gateID here gateId is unique key so it returns only one record will be selected
			String sql = "select * from managegates where BOOM1_ID=?";
			List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql,gate);
			API obj = new API(); 

			for (Map row : rows) {
				obj.setBs1Go((String) row.get("BS1_GO"));
				obj.setBs1Gc((String) row.get("BS1_GC"));

			}

			System.out.println("Gate"+obj.getBs1Go());

			int n=Integer.parseInt(obj.getBs1Go());
			System.out.println("no::::"+n);
			if (status>=n) {
				int i = jdbcTemplate1.update("update managegates set BS1_STATUS=? where BOOM1_ID=?",new Object[] {"open",gate });
				if(i ==1) {
					System.out.println(" open command updated");
				}else {	
					System.out.println("open Not updated");
				}
			}
			int j=Integer.parseInt(obj.getBs1Gc());
			if (status<=j) {
				int i = jdbcTemplate1.update("update managegates set BS1_STATUS=? where BOOM1_ID=?",new Object[] {"closed",gate });
				if(i ==1) {
					System.out.println(" closed  command updated");
				}else {	
					System.out.println("closed command  not updated");
				}
			}

			//getting managegates according to handle here handle is not an unique coloumn
			String handlesql = "select * from managegates where handle=?";
			List<Map<String, Object>> handlerows = jdbcTemplate1.queryForList(handlesql,gate);
			API obj1 = new API(); 
			if(handlerows.size()>1) {
				for (Map row : handlerows) {
					obj1.setLsGo((Integer) row.get("LS_GO"));
					obj1.setLsGc((Integer) row.get("LS_GC"));

				}	
				if (statuss<=obj1.getLsGo()) {
					int i = jdbcTemplate1.update("update managegates set LEVER_STATUS=? where handle=?",new Object[] {"open",gate });
					if(i ==1) {
						System.out.println(" Handle status updated");
					}else {	
						System.out.println("Handle status Not updated");
					}
				}

				if (statuss>=obj1.getLsGc()) {
					int i = jdbcTemplate1.update("update managegates set LEVER_STATUS=? where handle=?",new Object[] {"closed",gate });
					if(i ==1) {
						System.out.println(" Handle status updated");
					}else {	
						System.out.println("Handle status Not updated");
					}
				}
			}
			//checking Gate_status and handle status  and update lc_lock_time and lc_status
			String sql2 = "select * from managegates where BOOM1_ID=? OR handle=?";
			List<Map<String, Object>> rows2 = jdbcTemplate1.queryForList(sql2,gate,gate);
			API obj3 = new API(); 

			for (Map row : rows2) {
				obj3.setLc_name((String) row.get("Gate_Num"));
				obj3.setSm((String) row.get("SM"));
				obj3.setGm((String) row.get("GM"));
				obj3.setBs1Status((String) row.get("BS1_STATUS"));
				obj3.setLeverStatus((String) row.get("LEVER_STATUS"));
			}

			String s1=obj3.getBs1Status();
			String s2=obj3.getLeverStatus();
			if (s1.equals(s2)) {
				int i = jdbcTemplate1.update("update managegates set status=?  where BOOM1_ID=? OR handle=?",new Object[] {"Closed",gate,gate });
				if (i > 0) {
					logger.info("[GATE] [{}] main status updated to Closed", gate);
				} else {
					logger.warn("[GATE] [{}] FAILED to update main status", gate);
				}
				String pattern = "yyyy-MM-dd hh:mm:ss";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
				simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				String date = simpleDateFormat.format(new Date());

				String pattern1 = "hh:mm:ss";
				SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(pattern1);
				simpleDateFormat1.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				String time = simpleDateFormat1.format(new Date());

				int o = jdbcTemplate1.update("update reports set lc_lock_time=?, lc_status=? where lc_pin=? and ackn !=? and added_on >?",new Object[] 
						{time,"Closed","","",date});
				if (o > 0) {
					logger.info("[GATE] [{}] lc_lock_time updated", gate);
				} else {
					logger.warn("[GATE] [{}] FAILED to update lc_lock_time", gate);
				}


			}



			else
			{

				String pattern2 = "yyyy-MM-dd hh:mm:ss";
				SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat(pattern2);
				simpleDateFormat2.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				String date1 = simpleDateFormat2.format(new Date());

				String pattern3 = "hh:mm:ss";
				SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat(pattern3);
				simpleDateFormat3.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				String time1 = simpleDateFormat3.format(new Date());
				jdbcTemplate1.update("update managegates set status=? where status=? and BOOM1_ID=? or handle=?",new Object[] 
						{"Open","Closed",gate,gate});





				String sql5 = "select * from reports where lc_pin != ? and ackn != ? and added_on > ? and gm=? and lc_open_time=? ";
				List<Map<String, Object>> rows5 = jdbcTemplate1.queryForList(sql5,"","",date1,obj3.getGm(),"");

				for (Map row : rows5) {
					jdbcTemplate1.update("update reports set lc_open_time=? where lc_pin != ? and ackn != ? and added_on > ? and gm=? and lc_open_time=?",new Object[] 
							{time1,"","",date1,obj3.getGm(),""});

					jdbcTemplate1.update("insert into reports (lc_name, lc_status, lc_lock_time, lc, sm) values(?,?, ?,?,?)",
							obj3.getLc_name(),"Open",time1,gate,obj3.getSm());
					break;


				}
			}
			jdbcTemplate1.update("insert into gates (gate, status,statuss, pass, post_ip, act) values(?,?, ?,?,?,?)",
					gate,status,statuss,pass,request.getRemoteAddr(),"success");



		}


		String sql= "select * from gates order by id DESC LIMIT 0, 3";
		List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql);
		ArrayList<API> list =  new ArrayList<API>();
		for (Map row : rows) {
			api.setId((Integer) row.get("id"));
			api.setGate((String) row.get("gate"));
			api.setStatus((String) row.get("status"));
			api.setStatuss((String) row.get("statuss"));
			api.setAdded_on((Date) row.get("added_on"));
			list.add(api);

		}
		return list;	

	}

	public List<API> addstatusofgate(String gate2, int status2, int statuss2,String pass2, HttpServletRequest request ) {
		// Legacy method - call new method with -1 for BS2 and LT (will use last known values)
		return addstatusofgate(gate2, status2, statuss2, pass2, -1, -1, request);
	}
	
	public List<API> addstatusofgate(String gate2, int status2, int statuss2,String pass2, int bs2Value, int ltValue, HttpServletRequest request ) {
		API api =  new  API();
		String gate= gate2;
		int status= status2;
		int statuss= statuss2;
		String pass= pass2;
		logger.debug("Processing gate: {}, status: {}, statuss: {}, pass: {}, bs2: {}, lt: {}", gate, status, statuss, pass, bs2Value, ltValue);
		
		// Record SM data received for failsafe tracking
		recordSMDataForFailsafe(gate);
		
		// Call ProcessServiceImpl with BS2 and LT values - use gate ID exactly as received
		// Pass field4 (pass) for LT_STATUS processing when gate contains "LT"
		processService.updateRecord(gate, status, statuss, bs2Value, ltValue, pass);
		
		String name = null;
		String[] parts = gate.split("-");
		if(parts.length > 1) {
			name = parts[1];
			if(name.indexOf("BS")!=-1 && name.indexOf("BS2")==-1) {
				name = name.replace("BS", "");
			} else if(name.indexOf("BS2")!=-1) {
				name = name.replace("BS2", "");
			} else if(name.indexOf("LS")!=-1) {
				name = name.replace("LS", "");
			} else if(name.indexOf("LT")!=-1) {
				name = name.replace("LT", "");
			}
		}
		String pattern = "yyyy-MM-dd hh:mm:ss";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		
		Calendar newYearsEve = Calendar.getInstance();
		newYearsEve.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
 	    newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
 		// Using DateFormat format method we can create a string 
 		// representation of a date with the defined format.
 		String date = simpleDateFormat.format(newYearsEve.getTime());
 		
		// Use gate ID exactly as received from API
		Gate gate1 = new Gate(name, gate, String.valueOf(status),String.valueOf(statuss));
		gate1.setBatch(pass);
		gate1.setDate(date);
		kafkaTemplate.send(topic1,gate1);
        kafkaTemplate.send(topic2, new Gate(name, gate, String.valueOf(status),String.valueOf(statuss)));
		ArrayList<API> list =  new ArrayList<API>();
	
		return list;	

	}
	
	public List<Data> getdata() {
		API api =  new  API();
		String sql= "select * from gates order by id DESC limit 3";
		List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql);
		ArrayList<Data> list =  new ArrayList<Data>();
		for (Map row : rows) {
			Data test = new Data();
			test.setLcname((String) row.get("gate"));
			test.setFeild1((String) row.get("status"));
			test.setFeild2((String) row.get("statuss"));
			test.setData((Date) row.get("added_on"));
			list.add(test);

		}
		return list;	
	
	}

	/**
	 * New method to record gate data received for failsafe tracking
	 * Call this from addstatusofgate methods when data is received
	 * Tracks both gate-level and SM-level timestamps
	 */
	public void recordSMDataForFailsafe(String gate) {
		try {
			// Record gate-level timestamp
			userRepository.setGateLastUpdateTime(gate);			
		
		} catch (Exception e) {
			logger.error("Error recording gate data for failsafe: {}", gate, e);
		}
	}
	
	/**
	 * New method to get failsafe status for an SM or GM
	 * Determines if userid is SM or GM based on naming pattern (ends with "_SM")
	 */
	public boolean getSMFailsafeStatus(String userid) {
		if (userid == null || userid.isEmpty()) {
			return false;
		}
		// Check if it's SM (ends with "_SM") or GM
		if (userid.endsWith("_SM")) {
			return userRepository.getSMFailsafe(userid);
		} else {
			// It's a GM
			return smFailsafeService.isGMInFailsafe(userid);
		}
	}
	
	/**
	 * Get list of gate names that are in failsafe mode for a given SM or GM
	 * Determines if userid is SM or GM based on naming pattern (ends with "_SM")
	 * Returns a list of gate names (empty list if no gates in failsafe or userid not found)
	 */
	public java.util.List<String> getFailsafeGateNamesForSM(String userid) {
		if (userid == null || userid.isEmpty()) {
			return new java.util.ArrayList<>();
		}
		// Check if it's SM (ends with "_SM") or GM
		if (userid.endsWith("_SM")) {
			return smFailsafeService.getFailsafeGateNamesForSM(userid);
		} else {
			// It's a GM
			return smFailsafeService.getFailsafeGateNamesForGM(userid);
		}
	}

	
}
