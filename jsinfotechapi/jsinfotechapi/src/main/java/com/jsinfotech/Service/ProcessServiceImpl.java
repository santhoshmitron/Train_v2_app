package com.jsinfotech.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.API;

@Service
public class ProcessServiceImpl{

    private static final Logger logger = LogManager.getLogger(ProcessServiceImpl.class);
    
    @Autowired
	JdbcTemplate jdbcTemplate1;
	
	@Autowired
	private RedisUserRepository userRepository;
	
	@Autowired
	CacheStatusUpdate cacheupdate;
	
	@Autowired
	AcknowledgementService ackService;
	
	@Autowired
	GateStatusService gateStatusService;
	
	@Autowired
	PNGenerationService pnGenerationService;
	
	@Autowired
	BoomLockHealthService boomLockHealthService;

	@Autowired
	SMFailsafeService smFailsafeService;

	private static final class CanonicalGate {
		private final String boom1Id;
		private final String gateNum;
		private final String sm;
		private final String gm;
		private final String bs1Status;
		private final String bs2Status;
		private final String leverStatus;
		private final String mainStatus;

		private CanonicalGate(String boom1Id, String gateNum, String sm, String gm,
				String bs1Status, String bs2Status, String leverStatus, String mainStatus) {
			this.boom1Id = boom1Id;
			this.gateNum = gateNum;
			this.sm = sm;
			this.gm = gm;
			this.bs1Status = bs1Status;
			this.bs2Status = bs2Status;
			this.leverStatus = leverStatus;
			this.mainStatus = mainStatus;
		}
	}

	private CanonicalGate resolveCanonicalGate(String gateId) {
		if (gateId == null || gateId.trim().isEmpty()) {
			return null;
		}
		String gid = gateId.trim();

		// Try exact ID lookups first (covers BOOM1_ID, BOOM2_ID, handle, LTSW_ID).
		String sql =
			"SELECT BOOM1_ID, Gate_Num, SM, GM, BS1_STATUS, BS2_STATUS, LEVER_STATUS, status " +
			"FROM managegates " +
			"WHERE BOOM1_ID=? OR BOOM2_ID=? OR handle=? OR LTSW_ID=? " +
			"LIMIT 1";
		List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, gid, gid, gid, gid);

		// BS1 devices sometimes send BOOM1_ID with a trailing '1' (e.g., E20-750BS1 vs E20-750BS)
		// If not found, retry without the trailing '1' to normalize to BOOM1_ID.
		if ((rows == null || rows.isEmpty()) && gid.endsWith("1")) {
			String gidNoOne = gid.substring(0, gid.length() - 1);
			rows = jdbcTemplate1.queryForList(sql, gidNoOne, gidNoOne, gidNoOne, gidNoOne);
		}

		if (rows == null || rows.isEmpty()) {
			return null;
		}

		Map<String, Object> row = rows.get(0);
		String boom1Id = row.get("BOOM1_ID") != null ? row.get("BOOM1_ID").toString() : null;
		String gateNum = row.get("Gate_Num") != null ? row.get("Gate_Num").toString() : null;
		String sm = row.get("SM") != null ? row.get("SM").toString() : null;
		String gm = row.get("GM") != null ? row.get("GM").toString() : null;
		String bs1Status = row.get("BS1_STATUS") != null ? row.get("BS1_STATUS").toString() : "open";
		String bs2Status = row.get("BS2_STATUS") != null ? row.get("BS2_STATUS").toString() : "open";
		String leverStatus = row.get("LEVER_STATUS") != null ? row.get("LEVER_STATUS").toString() : "open";
		String mainStatus = row.get("status") != null ? row.get("status").toString() : "Open";

		return new CanonicalGate(boom1Id, gateNum, sm, gm, bs1Status, bs2Status, leverStatus, mainStatus);
	}

    @Async("processExecutor")
    public void updateRecord(String gate, int status, int statuss) {
		// Legacy method - call new method with -1 for BS2 and LT (will use last known values)
		updateRecord(gate, status, statuss, -1, -1);
	}
	
	@Async("processExecutor")
    public void updateRecord(String gate, int status, int statuss, int bs2Value, int ltValue) {
		updateRecord(gate, status, statuss, bs2Value, ltValue, null);
	}
	
	@Async("processExecutor")
    public void updateRecord(String gate, int status, int statuss, int bs2Value, int ltValue, String pass) {
		if (gate!=null) 
		{
			// Determine sensor type based on gate ID
			boolean isBS1 = gate.indexOf("BS") != -1 && gate.indexOf("BS2") == -1;
			boolean isBS2 = gate.indexOf("BS2") != -1;
			boolean isLS = gate.indexOf("LS") != -1;
			boolean isLT = gate.indexOf("LT") != -1;
			
			// For BS1: use field2 (status) only
			if (isBS1) {
				if (status < 0) {
					Integer lastStatus = userRepository.getLastSensorValue(gate, "BS");
					if (lastStatus != null) {
						status = lastStatus;
						logger.info("Using last known BS value for gate {}: {}", gate, status);
					} else {
						logger.warn("No last known BS value for gate {}, skipping processing", gate);
						return;
					}
				} else {
					// Store current value as last known
					userRepository.setLastSensorValue(gate, "BS", status);
				}
			}
			
			// For BS2: use field2 (status) only, ignore field5 (bs2Value)
			if (isBS2) {
				// Use status (field2) for BS2 processing
				int bs2ValueToUse = status;
				if (bs2ValueToUse < 0) {
					Integer lastBs2Value = userRepository.getLastSensorValue(gate, "BS2");
					if (lastBs2Value != null) {
						bs2ValueToUse = lastBs2Value;
						logger.info("Using last known BS2 value for gate {}: {}", gate, bs2ValueToUse);
					} else {
						logger.warn("No last known BS2 value for gate {}, skipping processing", gate);
						return;
					}
				} else {
					// Store current value as last known
					userRepository.setLastSensorValue(gate, "BS2", bs2ValueToUse);
				}
				// Update bs2Value for use in BS2 processing section
				bs2Value = bs2ValueToUse;
			}
			
			// For LS: use field3 (statuss) only
			if (isLS) {
				if (statuss < 0) {
					Integer lastStatuss = userRepository.getLastSensorValue(gate, "LS");
					if (lastStatuss != null) {
						statuss = lastStatuss;
						logger.info("Using last known LS value for gate {}: {}", gate, statuss);
					} else {
						logger.warn("No last known LS value for gate {}, skipping processing", gate);
						return;
					}
				} else {
					// Store current value as last known
					userRepository.setLastSensorValue(gate, "LS", statuss);
				}
			}
			
			// For LT: use field6 (ltValue) - keep existing logic
			if (isLT) {
				if (ltValue < 0) {
					Integer lastLtValue = userRepository.getLastSensorValue(gate, "LT");
					if (lastLtValue != null) {
						ltValue = lastLtValue;
						logger.info("Using last known LT value for gate {}: {}", gate, ltValue);
					}
				} else {
					userRepository.setLastSensorValue(gate, "LT", ltValue);
				}
			}
			
			// Continue with processing using the values (original or last known)
			API obj = null;
			//select managegate by checking BOOM1_ID here BOOM1_ID is unique key so it returns only one record will be selected
			logger.debug("Processing gate: {}", gate);
			
			// Process BS1 sensor ONLY - must contain "BS" but NOT "BS2"
			// This ensures BS1 and BS2 are completely independent
			if(isBS1 && !isBS2) {
				logger.info("Processing BS1 sensor for gate: {}, field2 value: {}", gate, status);
				String sql = "select * from managegates where BOOM1_ID=?";
				List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql,gate);
				logger.info("BS1 query result count: {}", rows.size());

				// If not found, try without trailing "1" (e.g., E20-750BS1 -> E20-750BS)
				if ((rows == null || rows.isEmpty()) && gate.endsWith("1")) {
					String gateWithoutOne = gate.substring(0, gate.length() - 1);
					logger.debug("Trying alternative lookup without trailing '1': {}", gateWithoutOne);
					rows = jdbcTemplate1.queryForList(sql, gateWithoutOne);
					logger.info("Inside (alternative)"+rows.size());
					if (rows != null && !rows.isEmpty()) {
						gate = gateWithoutOne; // Use the found BOOM1_ID for subsequent operations
						logger.info("Found gate using alternative ID: {}", gate);
					}
				}

				if (rows == null || rows.isEmpty()) {
					logger.error("BS gate not found in managegates table: BOOM1_ID={}. Cannot proceed with BS processing.", gate);
					return;
				}

				try {
					Map row = rows.get(0);
					obj = new API(); 
					obj.setBs1Go((String) row.get("BS1_GO"));
					obj.setBs1Gc((String) row.get("BS1_GC"));
					obj.setGate((String) row.get("BOOM1_ID"));
					obj.setLc_name((String) row.get("Gate_Num"));
					obj.setSm((String) row.get("SM"));
					obj.setGm((String) row.get("GM"));
					obj.setBs1Status((String) row.get("BS1_STATUS"));
					obj.setLeverStatus((String) row.get("LEVER_STATUS"));
					userRepository.save(gate,obj);
					logger.debug("Cached BS1 gate data for: {}", gate);
				} catch (Exception e) {
					logger.error("Error extracting BS gate data from managegates for gate: {}", gate, e);
					return;
				}
			
			//obj=userRepository.findById(gate);
			if (obj != null) {
				String previousStatus = obj.getBs1Status();
				int n = Integer.parseInt(obj.getBs1Go());
				int j = Integer.parseInt(obj.getBs1Gc());
				
				// BS1 logic: value >= BS1_GO means open, value <= BS1_GC means closed
				// Use else-if to ensure only one condition executes (mutually exclusive)
				if (status>=n) {
					int i = jdbcTemplate1.update("update managegates set BS1_STATUS=? where BOOM1_ID=?",new Object[] {"open",gate });
					if(i == 1) {
						obj.setBs1Status("open");
						userRepository.update(gate, obj);
						SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
						timeFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
						String timestamp = timeFormat.format(new Date());
						logger.info("\n" +
							"───────────────────────────────────────────────────────────────\n" +
							"[BS1] [{}] Status Change [{}]\n" +
							"───────────────────────────────────────────────────────────────\n" +
							"Previous: {} → New: open\n" +
							"Field 2 (BS1): {} (>= threshold: {})\n" +
							"Field 3 (LS): {}\n" +
							"Field 4 (Pass): {}\n" +
							"Field 5 (BS2): {}\n" +
							"Field 6 (LT): {}\n" +
							"───────────────────────────────────────────────────────────────",
							gate, timestamp, previousStatus != null ? previousStatus : "null", status, n, 
							statuss >= 0 ? String.valueOf(statuss) : "N/A",
							pass != null ? pass : "N/A",
							bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
							ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
						
						// Immediately update main status to "Open" when BS1 opens and create report if status changed
						updateMainStatusToOpenAndCreateReportIfNeeded(gate, gate);
					} else {	
						logger.warn("[BS1] [{}] FAILED to update status to open - no rows affected. Check if BOOM1_ID exists: {}", gate, gate);
					}
				} else if (status<=j) {
					int i = jdbcTemplate1.update("update managegates set BS1_STATUS=? where BOOM1_ID=?",new Object[] {"closed",gate });
					if(i == 1) {
						obj.setBs1Status("closed");
						userRepository.update(gate, obj);
						SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
						timeFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
						String timestamp = timeFormat.format(new Date());
						logger.info("\n" +
							"───────────────────────────────────────────────────────────────\n" +
							"[BS1] [{}] Status Change [{}]\n" +
							"───────────────────────────────────────────────────────────────\n" +
							"Previous: {} → New: closed\n" +
							"Field 2 (BS1): {} (<= threshold: {})\n" +
							"Field 3 (LS): {}\n" +
							"Field 4 (Pass): {}\n" +
							"Field 5 (BS2): {}\n" +
							"Field 6 (LT): {}\n" +
							"───────────────────────────────────────────────────────────────",
							gate, timestamp, previousStatus != null ? previousStatus : "null", status, j,
							statuss >= 0 ? String.valueOf(statuss) : "N/A",
							pass != null ? pass : "N/A",
							bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
							ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
					} else {	
						logger.warn("[BS1] [{}] FAILED to update status to closed - no rows affected. Check if BOOM1_ID exists: {}", gate, gate);
					}
				} else {
					// Value is between BS1_GO and BS1_GC - no status change
					logger.debug("[BS1] [{}] no status change (value: {} between {} and {})", gate, status, n, j);
				}
			} else {
				logger.error("[BS1] [{}] Cannot update status - gate object is null", gate);
			}
			}
			
			// Process BS2 sensor ONLY - must contain "BS2" and NOT be BS1
			// This ensures BS1 and BS2 are completely independent
			if(isBS2 && !isBS1 && status >= 0) {
				// For BS2, query by BOOM2_ID directly (e.g., E20-750BS2)
				// BOOM2_ID and BOOM1_ID are in the same row
				logger.info("Processing BS2 sensor ONLY for gate: {}, field2 value: {} (BS1 processing is skipped)", gate, status);
				String sql = "select * from managegates where BOOM2_ID=?";
				List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, gate);
				
				// If not found by BOOM2_ID, try BOOM1_ID (in case of data inconsistency)
				if(rows == null || rows.isEmpty()) {
					logger.debug("BS2 not found by BOOM2_ID={}, trying BOOM1_ID", gate);
					sql = "select * from managegates where BOOM1_ID=? OR BOOM2_ID=?";
					rows = jdbcTemplate1.queryForList(sql, gate, gate);
				}
				
				if(rows != null && rows.size() > 0) {
					Map<String, Object> row = rows.get(0);
					String bs2Go = (String) row.get("BS2_GO");
					String bs2Gc = (String) row.get("BS2_GC");
					// Get BOOM1_ID from the row to use in UPDATE query (since BOOM1_ID is the unique key)
					String boom1Id = (String) row.get("BOOM1_ID");
					
					if(bs2Go != null && !bs2Go.isEmpty() && bs2Gc != null && !bs2Gc.isEmpty() && boom1Id != null) {
						// Get current BS2_STATUS for comparison
						String previousBs2Status = (String) row.get("BS2_STATUS");
						int n = Integer.parseInt(bs2Go);
						int j = Integer.parseInt(bs2Gc);
						
						// Use status (field2) for BS2 processing
						if (status >= n) {
							int i = jdbcTemplate1.update("update managegates set BS2_STATUS=? where BOOM1_ID=?",new Object[] {"open", boom1Id });
							if(i == 1) {
								SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
								String timestamp = timeFormat.format(new Date());
								logger.info("\n" +
									"───────────────────────────────────────────────────────────────\n" +
									"[BS2] [{}] Status Change [{}]\n" +
									"───────────────────────────────────────────────────────────────\n" +
									"Previous: {} → New: open\n" +
									"Field 2 (BS2): {} (>= threshold: {})\n" +
									"Field 3 (LS): {}\n" +
									"Field 4 (Pass): {}\n" +
									"Field 5 (BS2): {}\n" +
									"Field 6 (LT): {}\n" +
									"───────────────────────────────────────────────────────────────",
									gate, timestamp, previousBs2Status != null ? previousBs2Status : "null", status, n,
									statuss >= 0 ? String.valueOf(statuss) : "N/A",
									pass != null ? pass : "N/A",
									bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
									ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
								
								// Immediately update main status to "Open" when BS2 opens and create report if status changed
								updateMainStatusToOpenAndCreateReportIfNeeded(gate, boom1Id);
							} else {
								logger.warn("[BS2] [{}] FAILED to update status to open - no rows affected. Check BOOM1_ID: {}", gate, boom1Id);
							}
						} else if (status <= j) {
							int i = jdbcTemplate1.update("update managegates set BS2_STATUS=? where BOOM1_ID=?",new Object[] {"closed", boom1Id });
							if(i == 1) {
								SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
								String timestamp = timeFormat.format(new Date());
								logger.info("\n" +
									"───────────────────────────────────────────────────────────────\n" +
									"[BS2] [{}] Status Change [{}]\n" +
									"───────────────────────────────────────────────────────────────\n" +
									"Previous: {} → New: closed\n" +
									"Field 2 (BS2): {} (<= threshold: {})\n" +
									"Field 3 (LS): {}\n" +
									"Field 4 (Pass): {}\n" +
									"Field 5 (BS2): {}\n" +
									"Field 6 (LT): {}\n" +
									"───────────────────────────────────────────────────────────────",
									gate, timestamp, previousBs2Status != null ? previousBs2Status : "null", status, j,
									statuss >= 0 ? String.valueOf(statuss) : "N/A",
									pass != null ? pass : "N/A",
									bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
									ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
							} else {
								logger.warn("[BS2] [{}] FAILED to update status to closed - no rows affected. Check BOOM1_ID: {}", gate, boom1Id);
							}
						} else {
							logger.debug("[BS2] [{}] no status change (value: {} between {} and {})", gate, status, n, j);
						}
					} else {
						logger.warn("BS2 thresholds (BS2_GO or BS2_GC) are null or empty, or BOOM1_ID not found for BOOM2_ID: {}", gate);
					}
				} else {
					logger.error("BS2 gate not found in managegates table: BOOM2_ID={}", gate);
				}
			}
			
			// Process LT sensor with new LTSW_ID, LT1_STATUS, LT2_STATUS, and LT_STATUS
			// This is independent of other sensor processing
			// field1 = LTSW_ID (e.g., "E20-750LT")
			// field2 = LT1_STATUS value (0=open, 1=closed)
			// field3 = LT2_STATUS value (0=open, 1=closed)
			// field4 = LT_STATUS value (0=open, 1=closed) - passed as String "pass"
			if(isLT && gate.indexOf("LT") != -1) {
				logger.info("Processing LT sensor with LTSW_ID: {}, field2 (LT1): {}, field3 (LT2): {}, field4 (LT): {}", 
					gate, status, statuss, pass);
				
				// Query by LTSW_ID first
				String sql = "select * from managegates where LTSW_ID=?";
				List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, gate);
				
				// If not found by LTSW_ID, try alternative lookup by BOOM1_ID, BOOM2_ID, or handle
				if(rows == null || rows.isEmpty()) {
					logger.debug("LT not found by LTSW_ID={}, trying BOOM1_ID, BOOM2_ID, or handle", gate);
					sql = "select * from managegates where BOOM1_ID=? OR BOOM2_ID=? OR handle=?";
					rows = jdbcTemplate1.queryForList(sql, gate, gate, gate);
				}
				
				if(rows != null && rows.size() > 0) {
					Map<String, Object> row = rows.get(0);
					String boom1Id = (String) row.get("BOOM1_ID");
					
					if(boom1Id != null) {
						// Get current LT statuses for comparison
						String previousLt1Status = (String) row.get("LT1_STATUS");
						String previousLt2Status = (String) row.get("LT2_STATUS");
						String previousLtStatus = (String) row.get("LT_STATUS");
						
						// Process LT1_STATUS from field2 (status) - 0=open, 1=closed
						if(status >= 0) {
							String lt1Status = (status == 1) ? "closed" : "open";
							int i = jdbcTemplate1.update("update managegates set LT1_STATUS=? where BOOM1_ID=?", 
								new Object[] {lt1Status, boom1Id});
							if(i == 1) {
								SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
								String timestamp = timeFormat.format(new Date());
								logger.info("\n" +
									"───────────────────────────────────────────────────────────────\n" +
									"[LT1] [{}] Status Change [{}]\n" +
									"───────────────────────────────────────────────────────────────\n" +
									"Previous: {} → New: {}\n" +
									"Field 2 (LT1): {} ({} = closed, {} = open)\n" +
									"Field 3 (LT2): {}\n" +
									"Field 4 (LT): {}\n" +
									"Field 5 (BS2): {}\n" +
									"Field 6 (LT): {}\n" +
									"───────────────────────────────────────────────────────────────",
									gate, timestamp, previousLt1Status != null ? previousLt1Status : "null", lt1Status, status, 1, 0,
									statuss >= 0 ? String.valueOf(statuss) : "N/A",
									pass != null ? pass : "N/A",
									bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
									ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
							} else {
								logger.warn("[LT1] [{}] FAILED to update status - no rows affected. Check BOOM1_ID: {}", gate, boom1Id);
							}
						}
						
						// Process LT2_STATUS from field3 (statuss) - 0=open, 1=closed
						if(statuss >= 0) {
							String lt2Status = (statuss == 1) ? "closed" : "open";
							int i = jdbcTemplate1.update("update managegates set LT2_STATUS=? where BOOM1_ID=?", 
								new Object[] {lt2Status, boom1Id});
							if(i == 1) {
								SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
								String timestamp = timeFormat.format(new Date());
								logger.info("\n" +
									"───────────────────────────────────────────────────────────────\n" +
									"[LT2] [{}] Status Change [{}]\n" +
									"───────────────────────────────────────────────────────────────\n" +
									"Previous: {} → New: {}\n" +
									"Field 2 (LT1): {}\n" +
									"Field 3 (LT2): {} ({} = closed, {} = open)\n" +
									"Field 4 (LT): {}\n" +
									"Field 5 (BS2): {}\n" +
									"Field 6 (LT): {}\n" +
									"───────────────────────────────────────────────────────────────",
									gate, timestamp, previousLt2Status != null ? previousLt2Status : "null", lt2Status,
									status >= 0 ? String.valueOf(status) : "N/A",
									statuss, 1, 0,
									pass != null ? pass : "N/A",
									bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
									ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
							} else {
								logger.warn("[LT2] [{}] FAILED to update status - no rows affected. Check BOOM1_ID: {}", gate, boom1Id);
							}
						}
						
						// Process LT_STATUS from field4 (pass - converted to int) - 0=open, 1=closed
						if(pass != null && !pass.isEmpty()) {
							try {
								int ltStatusValue = Integer.parseInt(pass);
								String ltStatus = (ltStatusValue == 1) ? "closed" : "open";
								int i = jdbcTemplate1.update("update managegates set LT_STATUS=? where BOOM1_ID=?", 
									new Object[] {ltStatus, boom1Id});
								if(i == 1) {
									SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
									String timestamp = timeFormat.format(new Date());
									logger.info("\n" +
										"───────────────────────────────────────────────────────────────\n" +
										"[LT] [{}] Status Change [{}]\n" +
										"───────────────────────────────────────────────────────────────\n" +
										"Previous: {} → New: {}\n" +
										"Field 2 (LT1): {}\n" +
										"Field 3 (LT2): {}\n" +
										"Field 4 (LT): {} ({} = closed, {} = open)\n" +
										"Field 5 (BS2): {}\n" +
										"Field 6 (LT): {}\n" +
										"───────────────────────────────────────────────────────────────",
										gate, timestamp, previousLtStatus != null ? previousLtStatus : "null", ltStatus,
										status >= 0 ? String.valueOf(status) : "N/A",
										statuss >= 0 ? String.valueOf(statuss) : "N/A",
										ltStatusValue, 1, 0,
										bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
										ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
									// Clear health check when LT opens
									if(ltStatusValue == 0) {
										boomLockHealthService.clearHealthCheck(boom1Id);
									}
								} else {
									logger.warn("[LT] [{}] FAILED to update status - no rows affected. Check BOOM1_ID: {}", gate, boom1Id);
								}
							} catch (NumberFormatException e) {
								logger.warn("[LT] [{}] field4 (LT_STATUS) is not a valid integer: {}, skipping LT_STATUS update", gate, pass);
							}
						}
					} else {
						logger.error("BOOM1_ID not found in row for LTSW_ID: {}", gate);
					}
				} else {
					logger.error("LT gate not found in managegates table: LTSW_ID={}", gate);
				}
				
				// Return early to skip other sensor processing for LT
				return;
			}
			
			//getting managegates according to handle here handle is not an unique coloumn
			
			if(gate.indexOf("LS")!=-1 &&userRepository.findById(gate)==null ) {
				String handlesql = "select * from managegates where handle=?";
				List<Map<String, Object>> handlerows = jdbcTemplate1.queryForList(handlesql,gate);
				
				if (handlerows == null || handlerows.isEmpty()) {
					logger.warn("LS gate handle not found in managegates table: handle={}. Trying alternative lookup by BOOM1_ID.", gate);
					// Try alternative lookup using BOOM1_ID
					String gateIdSql = "select * from managegates where BOOM1_ID=?";
					handlerows = jdbcTemplate1.queryForList(gateIdSql, gate);
					if (handlerows == null || handlerows.isEmpty()) {
						logger.error("LS gate not found in managegates table by handle or BOOM1_ID: {}. Cannot proceed with LS processing.", gate);
						return;
					}
				}
				
				try {
					API obj1 = new API(); 
					Map row = handlerows.get(0);
					
					String lsGoStr = (String) row.get("LS_GO");
					String lsGcStr = (String) row.get("LS_GC");
					
					if (lsGoStr != null && !lsGoStr.isEmpty()) {
						obj1.setLsGo(Integer.parseInt(lsGoStr));
					} else {
						logger.error("LS_GO is null or empty in database for handle: {}", gate);
						return;
					}
					
					if (lsGcStr != null && !lsGcStr.isEmpty()) {
						obj1.setLsGc(Integer.parseInt(lsGcStr));
					} else {
						logger.error("LS_GC is null or empty in database for handle: {}", gate);
						return;
					}
					
					obj1.setPass((String) row.get("handle"));
					obj1.setSm((String) row.get("SM"));
					obj1.setGm((String) row.get("GM"));
					obj1.setBs1Status((String) row.get("BS1_STATUS"));
					obj1.setLeverStatus((String) row.get("LEVER_STATUS"));
					logger.info("LS gate data loaded and cached for handle: {}, LS_GO: {}, LS_GC: {}", gate, obj1.getLsGo(), obj1.getLsGc());

					userRepository.save(gate, obj1);
				} catch (Exception e) {
					logger.error("Error processing LS gate data for handle: {}", gate, e);
					return;
				}
			}
			
			if(gate.indexOf("LS")!=-1) {
				// Always reload LS_GO and LS_GC from database to ensure we have the latest values
				// Cache might have stale or default (0) values
				API obj1 = userRepository.findById(gate);
				logger.debug("Retrieved LS gate data from cache for handle: {}, LS_GO: {}, LS_GC: {}", gate, 
					obj1 != null ? obj1.getLsGo() : "null", obj1 != null ? obj1.getLsGc() : "null");
				
				// Always reload LS_GO and LS_GC from database to ensure accuracy
				String handlesql = "select * from managegates where handle=?";
				List<Map<String, Object>> handlerows = jdbcTemplate1.queryForList(handlesql, gate);
				
				if (handlerows == null || handlerows.isEmpty()) {
					logger.warn("LS gate handle not found in managegates table: handle={}. Trying alternative lookup by BOOM1_ID or BOOM2_ID.", gate);
					String gateIdSql = "select * from managegates where BOOM1_ID=? OR BOOM2_ID=? OR handle=?";
					handlerows = jdbcTemplate1.queryForList(gateIdSql, gate, gate, gate);
					if (handlerows == null || handlerows.isEmpty()) {
						logger.error("LS gate not found in managegates table by handle, BOOM1_ID, or BOOM2_ID: {}. Cannot proceed with LS processing.", gate);
						return;
					}
				}
				
				try {
					// Create new object or reuse existing one
					if (obj1 == null) {
						obj1 = new API();
					}
					
					Map row = handlerows.get(0);
					String lsGoStr = (String) row.get("LS_GO");
					String lsGcStr = (String) row.get("LS_GC");
					
					if (lsGoStr != null && !lsGoStr.isEmpty()) {
						obj1.setLsGo(Integer.parseInt(lsGoStr));
					} else {
						logger.error("LS_GO is null or empty in database for handle: {}", gate);
						return;
					}
					
					if (lsGcStr != null && !lsGcStr.isEmpty()) {
						obj1.setLsGc(Integer.parseInt(lsGcStr));
					} else {
						logger.error("LS_GC is null or empty in database for handle: {}", gate);
						return;
					}
					
					// Update other fields if obj1 was null
					if (obj1.getPass() == null) {
						obj1.setPass((String) row.get("handle"));
						obj1.setSm((String) row.get("SM"));
						obj1.setGm((String) row.get("GM"));
						obj1.setBs1Status((String) row.get("BS1_STATUS"));
						obj1.setLeverStatus((String) row.get("LEVER_STATUS"));
					}
					
					logger.info("LS gate data loaded from database for handle: {}, LS_GO: {}, LS_GC: {}", gate, obj1.getLsGo(), obj1.getLsGc());
					
					// Update cache with correct values
					userRepository.save(gate, obj1);
				} catch (Exception e) {
					logger.error("Error loading LS gate data from database for handle: {}", gate, e);
					return;
				}
				
				// Final validation - ensure we have valid values before proceeding
				if (obj1 == null || obj1.getLsGo() == 0 || obj1.getLsGc() == 0) {
					logger.error("LS gate data is invalid after database load. Cannot proceed. LS_GO: {}, LS_GC: {}", 
						obj1 != null ? obj1.getLsGo() : "null", obj1 != null ? obj1.getLsGc() : "null");
					return;
				}
				
				logger.debug("Processing LS gate status update for handle: {}, field3 value: {}, LS_GO: {}, LS_GC: {}", gate, statuss, obj1.getLsGo(), obj1.getLsGc());
				String previousHandleStatus = obj1.getLeverStatus();

				// LS logic: value <= LS_GO means open, value >= LS_GC means closed
				// Use else-if to ensure only one condition executes
				if (statuss <= obj1.getLsGo()) {
					int i = jdbcTemplate1.update("update managegates set LEVER_STATUS=? where handle=?",new Object[] {"open",gate });
					if(i == 1) {
						SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
						timeFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
						String timestamp = timeFormat.format(new Date());
						logger.info("\n" +
							"───────────────────────────────────────────────────────────────\n" +
							"[LS] [{}] Status Change [{}]\n" +
							"───────────────────────────────────────────────────────────────\n" +
							"Previous: {} → New: open\n" +
							"Field 2 (BS1/BS2): {}\n" +
							"Field 3 (LS): {} (<= threshold: {})\n" +
							"Field 4 (Pass): {}\n" +
							"Field 5 (BS2): {}\n" +
							"Field 6 (LT): {}\n" +
							"───────────────────────────────────────────────────────────────",
							gate, timestamp, previousHandleStatus != null ? previousHandleStatus : "null",
							status >= 0 ? String.valueOf(status) : "N/A",
							statuss, obj1.getLsGo(),
							pass != null ? pass : "N/A",
							bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
							ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
						obj1.setLeverStatus("open");
						userRepository.update(gate, obj1);
						
						// Immediately update main status to "Open" when LS opens and create report if status changed
						// Get BOOM1_ID for this handle to update main status
						String boom1IdForLS = null;
						try {
							String getBoom1IdSql = "SELECT BOOM1_ID FROM managegates WHERE handle=? LIMIT 1";
							boom1IdForLS = (String) jdbcTemplate1.queryForObject(getBoom1IdSql, new Object[] {gate}, String.class);
						} catch (Exception e) {
							logger.warn("Could not get BOOM1_ID for handle: {}", gate);
						}
						
						if (boom1IdForLS != null && !boom1IdForLS.isEmpty()) {
							updateMainStatusToOpenAndCreateReportIfNeeded(gate, boom1IdForLS);
						} else {
							// Fallback: resolve canonical BOOM1_ID from managegates and update by BOOM1_ID
							CanonicalGate cg = resolveCanonicalGate(gate);
							if (cg != null && cg.boom1Id != null && !cg.boom1Id.trim().isEmpty()) {
								updateMainStatusToOpenAndCreateReportIfNeeded(gate, cg.boom1Id.trim());
							} else {
								logger.warn("Could not resolve BOOM1_ID for LS handle: {}. Skipping main status Open update.", gate);
							}
						}
					} else {	
						logger.warn("[LS] [{}] FAILED to update status to open - no rows affected. Check if handle exists: {}", gate, gate);
					}
				} else if (statuss >= obj1.getLsGc()) {
					int i = jdbcTemplate1.update("update managegates set LEVER_STATUS=? where handle=?",new Object[] {"closed",gate });
					if(i == 1) {
						SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
						timeFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
						String timestamp = timeFormat.format(new Date());
						logger.info("\n" +
							"───────────────────────────────────────────────────────────────\n" +
							"[LS] [{}] Status Change [{}]\n" +
							"───────────────────────────────────────────────────────────────\n" +
							"Previous: {} → New: closed\n" +
							"Field 2 (BS1/BS2): {}\n" +
							"Field 3 (LS): {} (>= threshold: {})\n" +
							"Field 4 (Pass): {}\n" +
							"Field 5 (BS2): {}\n" +
							"Field 6 (LT): {}\n" +
							"───────────────────────────────────────────────────────────────",
							gate, timestamp, previousHandleStatus != null ? previousHandleStatus : "null",
							status >= 0 ? String.valueOf(status) : "N/A",
							statuss, obj1.getLsGc(),
							pass != null ? pass : "N/A",
							bs2Value >= 0 ? String.valueOf(bs2Value) : "N/A",
							ltValue >= 0 ? String.valueOf(ltValue) : "N/A");
						obj1.setLeverStatus("closed");
						userRepository.update(gate, obj1);
						// Start health check when LS becomes CLOSED (requirement: when LS closes, start 20 sec timer)
						if (previousHandleStatus == null || !previousHandleStatus.equalsIgnoreCase("closed")) {
							boomLockHealthService.startHealthCheck(gate);
						}
					} else {	
						logger.warn("[LS] [{}] FAILED to update status to closed - no rows affected. Check if handle exists: {}", gate, gate);
					}
				} else {
					// Value is between LS_GO and LS_GC - no status change
					logger.debug("[LS] [{}] no status change (value: {} between {} and {})", gate, statuss, obj1.getLsGo(), obj1.getLsGc());
				}
			}
			//checking Gate_status and handle status  and update lc_lock_time and lc_status
			
			// Add small delay to ensure all database updates are committed before checking gate status
			try {
				Thread.sleep(50); // 50ms delay to ensure database commit
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
			// Explicitly query latest sensor statuses from database to avoid stale cache data
			logger.debug("Querying latest sensor statuses from database for gate: {}", gate);
			CanonicalGate canonical = resolveCanonicalGate(gate);
			if (canonical == null || canonical.boom1Id == null || canonical.boom1Id.trim().isEmpty()) {
				logger.error("Cannot proceed with gate status update - gate {} not found/cannot resolve canonical BOOM1_ID in managegates table", gate);
				return;
			}

			String dbBs1Status = canonical.bs1Status != null ? canonical.bs1Status : "open";
			String dbBs2Status = canonical.bs2Status != null ? canonical.bs2Status : "open";
			String dbLeverStatus = canonical.leverStatus != null ? canonical.leverStatus : "open";
			String dbStatus = canonical.mainStatus;
			
			logger.info("Latest sensor statuses from database for gate {}: BS1_STATUS={}, BS2_STATUS={}, LEVER_STATUS={}, managegates.status={}", 
				gate, dbBs1Status, dbBs2Status, dbLeverStatus, dbStatus);

			API obj3 = getDataFromManageGates(gate);
			if (obj3 == null) {
				logger.error("Cannot proceed with gate status update - gate {} not found in managegates table", gate);
				return;
			}
			String s1=obj3.getBs1Status();
			String s2=obj3.getLeverStatus();
			logger.info("In Open Command - BS1_STATUS: {}, LEVER_STATUS: {}", s1, s2);

			// Use GateStatusService to check if required sensors (BS1, BS2, LS) are closed
			// Status column depends ONLY on BS1_STATUS, BS2_STATUS, and LEVER_STATUS
			// LT_STATUS, LT1_STATUS, and LT2_STATUS are NOT considered for the main status column
			// Check previous status to detect transition
			String previousStatus = dbStatus;
			logger.info("Previous gate status from database: {}", previousStatus);
			
			// Check if gate is closed using latest database values
			boolean isGateClosed = dbBs1Status.equalsIgnoreCase("closed") 
				&& dbBs2Status.equalsIgnoreCase("closed")
				&& dbLeverStatus.equalsIgnoreCase("closed");
			
			logger.info("Gate closed check result: {} (BS1: {}, BS2: {}, LS: {})", isGateClosed, dbBs1Status, dbBs2Status, dbLeverStatus);
			
			if (isGateClosed) {
				logger.info("in if condition - gate is closed (BS1, BS2, and LS are all closed - LT statuses are NOT considered)");
				// Get BOOM1_ID from the found row to use in UPDATE (since BOOM1_ID is the unique key)
				String boom1IdForUpdate = canonical.boom1Id != null ? canonical.boom1Id.trim() : null;
				String gateNumForUpdate = canonical.gateNum != null ? canonical.gateNum.trim() : obj3.getLc_name();
				String gmForUpdate = canonical.gm != null ? canonical.gm.trim() : obj3.getGm();
				String smForUpdate = canonical.sm != null ? canonical.sm.trim() : obj3.getSm();

				// Keep API object consistent with canonical row (report printing depends on gm + gate number)
				if (gateNumForUpdate != null && !gateNumForUpdate.isEmpty()) obj3.setLc_name(gateNumForUpdate);
				if (gmForUpdate != null && !gmForUpdate.isEmpty()) obj3.setGm(gmForUpdate);
				if (smForUpdate != null && !smForUpdate.isEmpty()) obj3.setSm(smForUpdate);
				if (boom1IdForUpdate != null && !boom1IdForUpdate.isEmpty()) obj3.setGate(boom1IdForUpdate);

				int i = jdbcTemplate1.update("update managegates set status=? where BOOM1_ID=?",new Object[] {"Closed", boom1IdForUpdate });
				if (i>0) {
					//userRepository.save_key(obj3.getLc_name(), "Closed");
					// ackService.updateQueue(m.getSM(), "closed",m.getGateNum());

					logger.info("status updated");
					
					// Start health check when status becomes CLOSED (only if status changed from non-closed to closed)
					if (previousStatus == null || !previousStatus.equalsIgnoreCase("Closed")) {
						// Requirement: if GM controls multiple gates, update Boom_Lock for ALL gates under that GM that are currently CLOSED
						if (gmForUpdate != null && !gmForUpdate.trim().isEmpty()) {
							boomLockHealthService.startHealthCheckForClosedGatesOfGM(gmForUpdate);
							logger.info("Started boom lock health checks for GM: {} (trigger gate: {}, status changed to CLOSED)", gmForUpdate, gate);
						} else {
							// Fallback: start only for this gate
						boomLockHealthService.startHealthCheck(boom1IdForUpdate);
							logger.info("Started boom lock health check for gate: {} (status changed to CLOSED, GM missing)", gate);
						}
					}
				}
				else
				{
					logger.info(" status not updated");

				}
				String pattern = "yyyy-MM-dd HH:mm:ss";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
				simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				
				Calendar newYearsEve = Calendar.getInstance();
		 	    newYearsEve.setTimeInMillis(newYearsEve.getTimeInMillis());
		 	    newYearsEve.add(Calendar.MINUTE, -30);
		 		// Using DateFormat format method we can create a string 
		 		// representation of a date with the defined format.
		 		String date = simpleDateFormat.format(newYearsEve.getTime());
				//String date = simpleDateFormat.format(new Date());

				String pattern1 = "HH:mm";
				SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(pattern1);
				simpleDateFormat1.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				String time = simpleDateFormat1.format(new Date());

				// Failsafe rule:
				// If an unacknowledged NO-NETWORK report exists for this gate+GM,
				// do not update closed reports (lc_status) and do not generate PN.
				boolean gateInFailsafeForThisGM = false;
				if (gateNumForUpdate != null && !gateNumForUpdate.trim().isEmpty()
					&& gmForUpdate != null && !gmForUpdate.trim().isEmpty()) {
					try {
						Integer failsafeCount = jdbcTemplate1.queryForObject(
							"SELECT COUNT(*) FROM reports WHERE command='NO-NETWORK' AND lc_name=? AND gm=? AND (ackn IS NULL OR ackn = '')",
							Integer.class,
							gateNumForUpdate,
							gmForUpdate
						);
						gateInFailsafeForThisGM = (failsafeCount != null && failsafeCount > 0);
					} catch (Exception e) {
						logger.warn("Failsafe check failed (gate: {}, gm: {}), continuing normal closed flow: {}",
							gateNumForUpdate, gmForUpdate, e.getMessage());
					}
				}
				
				if (gateInFailsafeForThisGM) {
					logger.info("Gate sensors are closed but failsafe is active for gate '{}' and GM '{}'. Skipping lc_status update and PN generation.",
						gateNumForUpdate, gmForUpdate);
					return;
				}

				// Always check for existing "Close" report with train number when sensors are closed
				// This should happen regardless of previous status - we need to update train rows even if status was already "Closed"
				boolean shouldCreateClosedReport = (previousStatus == null || !previousStatus.equalsIgnoreCase("Closed"));
				boolean reportExists = false;
				int insertResult = 0;
				
				logger.info("Gate sensors are closed (previousStatus: {}). Checking for existing 'Close' report with train number for gate: {} (BOOM1_ID: {}, lc_name: {})", 
					previousStatus, gate, boom1IdForUpdate, obj3.getLc_name());
				
				// Always check if there's an existing "Close" report with train number (tn) for this gate
				// This happens when train number was sent before sensors closed
				// Check even if status was already "Closed" - we need to update train rows
				String findCloseReportSql = "SELECT id, tn, pn, wer, sm, gm FROM reports WHERE command='Close' AND (tn IS NOT NULL AND tn != '') AND (lc=? OR lc_name=?) AND (lc_status IS NULL OR lc_status = '') AND added_on > DATE_SUB(NOW(), INTERVAL 30 MINUTE) ORDER BY added_on DESC LIMIT 1";
				List<Map<String, Object>> closeReports = jdbcTemplate1.queryForList(findCloseReportSql, boom1IdForUpdate, gateNumForUpdate);
				
				Integer existingReportId = null;
				String existingTn = null;
				String existingPn = null;
				String existingWer = null;
				
				if (closeReports != null && !closeReports.isEmpty()) {
					Map<String, Object> closeReport = closeReports.get(0);
					existingReportId = ((Number) closeReport.get("id")).intValue();
					existingTn = (String) closeReport.get("tn");
					existingPn = (String) closeReport.get("pn");
					existingWer = (String) closeReport.get("wer");
					logger.info("Found existing 'Close' report with train number for gate: {} (BOOM1_ID: {}). Report ID: {}, tn: {}, pn: {}. Will update lc_status to 'Closed'.", 
						gate, boom1IdForUpdate, existingReportId, existingTn, existingPn);
				} else {
					logger.info("No existing 'Close' report with train number found for gate: {} (BOOM1_ID: {})", 
						gate, boom1IdForUpdate);
				}
				
				// Validate required fields before creating/updating report
				logger.info("Validating fields for Closed report - gate: {}, SM: '{}', GM: '{}', lc_name: '{}'", 
					gate, smForUpdate, gmForUpdate, gateNumForUpdate);
				if (smForUpdate == null || smForUpdate.isEmpty()) {
					logger.error("Cannot create/update Closed report - SM is null or empty for gate: {}", gate);
					reportExists = false;
				} else if (gmForUpdate == null || gmForUpdate.isEmpty()) {
					logger.error("Cannot create/update Closed report - GM is null or empty for gate: {}", gate);
					reportExists = false;
				} else if (gateNumForUpdate == null || gateNumForUpdate.isEmpty()) {
					logger.error("Cannot create/update Closed report - lc_name is null or empty for gate: {}", gate);
					reportExists = false;
				} else {
					// If existing "Close" report with train number found, UPDATE it regardless of previous status
					// This ensures train rows are updated even when status was already "Closed"
					if (existingReportId != null) {
						logger.info("Updating existing 'Close' report (ID: {}) to 'Closed' for gate: {} (BOOM1_ID: {}, lc_name: {}). Preserving tn: {}, pn: {}, wer: {}. Previous status: {}", 
							existingReportId, gate, boom1IdForUpdate, obj3.getLc_name(), existingTn, existingPn, existingWer, previousStatus);
						try {
							// Update existing report: keep command as "Close", set lc_status and lc_lock_time
							// Keep existing tn, pn, wer, sm, gm, etc.
							int updateResult = jdbcTemplate1.update("UPDATE reports SET command=?, lc_status=?, lc_lock_time=? WHERE id=?",
									"Close", "Closed", time, existingReportId);
							if (updateResult > 0) {
								reportExists = true;
								logger.info("SUCCESS: Updated existing 'Close' report (ID: {}) to 'Closed' for gate: {} (BOOM1_ID: {}, lc_name: {}). Update result: {}", 
									existingReportId, gate, boom1IdForUpdate, obj3.getLc_name(), updateResult);
								
								// Ensure Boom_Lock is evaluated and written to the latest Closed row even if main status was already Closed.
								// This is critical for the "train not sent" flow as well.
								try {
									if (gmForUpdate != null && !gmForUpdate.trim().isEmpty()) {
										boomLockHealthService.startHealthCheckForClosedGatesOfGM(gmForUpdate);
									} else {
										boomLockHealthService.startHealthCheck(boom1IdForUpdate);
									}
								} catch (Exception ignore) {
									// Do not break report flow
								}
							} else {
								logger.error("FAILED: Could not update existing 'Close' report (ID: {}) for gate: {} (BOOM1_ID: {}). Update result: {}", 
									existingReportId, gate, boom1IdForUpdate, updateResult);
							}
						} catch (Exception e) {
							logger.error("EXCEPTION: Error updating existing 'Close' report (ID: {}) for gate: {} (BOOM1_ID: {})", 
								existingReportId, gate, boom1IdForUpdate, e);
						}
					} else {
						// No existing "Close" report with train number found
						// Only skip Closed report if status was already "Closed" (no status change)
						// If status changed from "Open" (or null) to "Closed", always create report (even if duplicate exists)
						boolean statusAlreadyClosed = (previousStatus != null && previousStatus.equalsIgnoreCase("Closed"));
						
						if (statusAlreadyClosed) {
							// Even if managegates.status is already Closed, we still want a Closed report row
							// when the gate is physically closed, but avoid spamming duplicates.
							try {
								String recentClosedSql =
									"SELECT id FROM reports " +
									"WHERE gm = ? " +
									"AND (lc IN (?,?) OR lc_name IN (?,?)) " +
									"AND UPPER(lc_status)='CLOSED' " +
									"AND added_on >= DATE_SUB(NOW(), INTERVAL 30 SECOND) " +
									"ORDER BY added_on DESC LIMIT 1";
								List<Map<String, Object>> recentClosed = jdbcTemplate1.queryForList(
									recentClosedSql, gmForUpdate, boom1IdForUpdate, gateNumForUpdate, boom1IdForUpdate, gateNumForUpdate
								);
								if (recentClosed != null && !recentClosed.isEmpty()) {
									logger.info("Status already 'Closed' and a recent Closed report exists (last 30s). Skipping Closed report creation for gate: {} (BOOM1_ID: {})",
										gate, boom1IdForUpdate);
									reportExists = false;
								} else {
									logger.info("Status already 'Closed' but no recent Closed report exists. Creating Closed report for gate: {} (BOOM1_ID: {}, lc_name: {})",
										gate, boom1IdForUpdate, obj3.getLc_name());
									// Create a Closed report row (no train). Use redy='' so getReports returns it and mobile can ack → "acknowledged" sound plays
									String patternNow = "yyyy-MM-dd HH:mm:ss";
									SimpleDateFormat sdfNow = new SimpleDateFormat(patternNow);
									sdfNow.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
									String currentDateTime = sdfNow.format(new Date());
									// Final rule: while gate is in failsafe, do NOT save non-ct Open/Close reports.
									// These Close rows are non-ct (redy = '' or 's').
									boolean blockNonCtCloseInFailsafe = isGateInFailsafeForNonCtReports(gateNumForUpdate, boom1IdForUpdate);
									if (blockNonCtCloseInFailsafe) {
										logger.info("Gate is in failsafe (managegates.is_failsafe=true). Skipping non-ct Close report insert for gate: {} (BOOM1_ID: {}, lc_name: {})",
											gate, boom1IdForUpdate, gateNumForUpdate);
										insertResult = 0;
									} else {
									insertResult = jdbcTemplate1.update(
										"insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
										"","",time,"Close","",smForUpdate,gmForUpdate,boom1IdForUpdate,gateNumForUpdate,
										currentDateTime,"Closed",time,"","","","",""
									);
									}
									if (insertResult > 0) {
										reportExists = true;
										logger.info("SUCCESS: Created Closed report (status already Closed) for gate: {} (BOOM1_ID: {}, lc_name: {})",
											gate, boom1IdForUpdate, obj3.getLc_name());
										// Enqueue Boom_Lock update for latest Closed row
										try {
											if (gmForUpdate != null && !gmForUpdate.trim().isEmpty()) {
												boomLockHealthService.startHealthCheckForClosedGatesOfGM(gmForUpdate);
											} else {
												boomLockHealthService.startHealthCheck(boom1IdForUpdate);
											}
										} catch (Exception ignore) {}
									}
								}
							} catch (Exception e) {
								logger.warn("Error while deciding Closed report creation when status already Closed for gate: {} (BOOM1_ID: {})",
									gate, boom1IdForUpdate, e);
								reportExists = false;
							}
						} else {
							logger.info("Status changed from '{}' to 'Closed'. Creating Closed report for gate: {} (BOOM1_ID: {}, lc_name: {})", 
								previousStatus, gate, boom1IdForUpdate, obj3.getLc_name());
							
							logger.info("Creating new Closed report for gate: {} (BOOM1_ID: {}, lc_name: {}, sm: {}, gm: {}, time: {}, previousStatus: {})", 
								gate, boom1IdForUpdate, obj3.getLc_name(), obj3.getSm(), obj3.getGm(), time, previousStatus);
							try {
								// Format current date/time as string to ensure IST timezone (same as NO-NETWORK)
								simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
								String currentDateTime = simpleDateFormat.format(new Date());
								// Final rule: while gate is in failsafe, do NOT save non-ct Open/Close reports.
								boolean blockNonCtCloseInFailsafe = isGateInFailsafeForNonCtReports(gateNumForUpdate, boom1IdForUpdate);
								if (blockNonCtCloseInFailsafe) {
									logger.info("Gate is in failsafe (managegates.is_failsafe=true). Skipping non-ct Close report insert for gate: {} (BOOM1_ID: {}, lc_name: {})",
										gate, boom1IdForUpdate, gateNumForUpdate);
									insertResult = 0;
								} else {
									insertResult = jdbcTemplate1.update("insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
											"","",time,"Close","",smForUpdate,gmForUpdate,boom1IdForUpdate,gateNumForUpdate,
											currentDateTime,"Closed",time,"","","","","s");
								}
								if (insertResult > 0) {
									reportExists = true;
									logger.info("SUCCESS: Created new Closed report for gate: {} (BOOM1_ID: {}, lc_name: {}). Insert result: {}", 
										gate, boom1IdForUpdate, obj3.getLc_name(), insertResult);
									
									// Ensure Boom_Lock is evaluated and written to the latest Closed row (train sent or not)
									try {
										if (gmForUpdate != null && !gmForUpdate.trim().isEmpty()) {
											boomLockHealthService.startHealthCheckForClosedGatesOfGM(gmForUpdate);
										} else {
											boomLockHealthService.startHealthCheck(boom1IdForUpdate);
										}
									} catch (Exception ignore) {
										// Do not break report flow
									}
								} else {
									logger.error("FAILED: Could not create Closed report for gate: {} (BOOM1_ID: {}). Insert result: {}", 
										gate, boom1IdForUpdate, insertResult);
								}
							} catch (Exception e) {
								logger.error("EXCEPTION: Error creating Closed report for gate: {} (BOOM1_ID: {})", gate, boom1IdForUpdate, e);
							}
						}
					}
				}
				
				// Trigger PN generation for train reports with empty PN when gates close
				// This handles the case where train was sent before gates closed
				if (reportExists) {
					try {
						// Check if there's a report with train number and empty PN for this gate
						// This could be the report we just updated/created, or an existing one
						String findTrainReportSql = "SELECT id, tn, pn, gm FROM reports WHERE (lc = ? OR lc_name = ?) AND (UPPER(command) = 'CLOSE' OR UPPER(command) = 'CLOSED') AND (tn IS NOT NULL AND tn != '') AND (pn IS NULL OR pn = '') AND gm = ? ORDER BY added_on DESC LIMIT 1";
						List<Map<String, Object>> trainReports = jdbcTemplate1.queryForList(findTrainReportSql, boom1IdForUpdate, gateNumForUpdate, gmForUpdate);
						
						if (trainReports != null && !trainReports.isEmpty()) {
							Map<String, Object> trainReport = trainReports.get(0);
							String reportPn = (String) trainReport.get("pn");
							String reportTn = (String) trainReport.get("tn");
							String reportGm = (String) trainReport.get("gm");
							
							// Only generate PN if it's empty and report has train number
							if ((reportPn == null || reportPn.trim().isEmpty()) && reportTn != null && !reportTn.trim().isEmpty() && reportGm != null) {
								logger.info("Gate closed - Found train report with empty PN for gate: {} (BOOM1_ID: {}), tn: {}, gm: {}. Triggering PN generation.", 
									gate, boom1IdForUpdate, reportTn, reportGm);
								pnGenerationService.generatePNIfNeeded(boom1IdForUpdate, reportGm, smForUpdate);
							} else {
								logger.debug("Gate closed - Train report already has PN or missing train number for gate: {} (BOOM1_ID: {}), pn: {}, tn: {}", 
									gate, boom1IdForUpdate, reportPn, reportTn);
							}
						} else {
							logger.debug("Gate closed - No train report with empty PN found for gate: {} (BOOM1_ID: {}), gm: {}", 
								gate, boom1IdForUpdate, obj3.getGm());
						}
					} catch (Exception pnEx) {
						logger.error("Error triggering PN generation when gate closed for gate: {} (BOOM1_ID: {}), SM: {}, GM: {}", 
							gate, boom1IdForUpdate, obj3.getSm(), obj3.getGm(), pnEx);
					}
				} else {
					logger.debug("Gate is CLOSED but no report was created/updated. Skipping PN generation check.");
				}
			}

			// Check if gate is open (any sensor is open)
			else if (!isGateClosed)
			{
				logger.info("Gate is OPEN - BS1: {}, BS2: {}, LS: {}", dbBs1Status, dbBs2Status, dbLeverStatus);
				
				// Get BOOM1_ID for update
				String boom1IdForOpenUpdate = canonical.boom1Id != null ? canonical.boom1Id.trim() : null;
				String gateNumForOpenUpdate = canonical.gateNum != null ? canonical.gateNum.trim() : obj3.getLc_name();
				String gmForOpenUpdate = canonical.gm != null ? canonical.gm.trim() : obj3.getGm();
				String smForOpenUpdate = canonical.sm != null ? canonical.sm.trim() : obj3.getSm();

				if (gateNumForOpenUpdate != null && !gateNumForOpenUpdate.isEmpty()) obj3.setLc_name(gateNumForOpenUpdate);
				if (gmForOpenUpdate != null && !gmForOpenUpdate.isEmpty()) obj3.setGm(gmForOpenUpdate);
				if (smForOpenUpdate != null && !smForOpenUpdate.isEmpty()) obj3.setSm(smForOpenUpdate);
				if (boom1IdForOpenUpdate != null && !boom1IdForOpenUpdate.isEmpty()) obj3.setGate(boom1IdForOpenUpdate);

				String pattern = "yyyy-MM-dd HH:mm:ss";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
				simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));

				String pattern3 = "HH:mm";
				SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat(pattern3);
				simpleDateFormat3.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				String time1 = simpleDateFormat3.format(new Date());
				
				logger.info("In Open Command - gate is OPEN (previousStatus: {}, current sensor statuses: BS1={}, BS2={}, LS={})", 
					previousStatus, dbBs1Status, dbBs2Status, dbLeverStatus);
				
				// Update managegates status to Open
				int statusUpdateResult = jdbcTemplate1.update("update managegates set status=? where BOOM1_ID=?",new Object[] 
						{"Open", boom1IdForOpenUpdate});
				logger.info("Gate status update result: {}", statusUpdateResult);
				
				// Clear health check when status changes to Open
				if (previousStatus != null && previousStatus.equalsIgnoreCase("Closed")) {
					boomLockHealthService.clearHealthCheck(gate);
					logger.info("Cleared boom lock health check for gate: {} (status changed to OPEN)", gate);
				}
				
				// Only skip Open report if status is already "Open" (no status change)
				// If status changed from "Closed" to "Open", always create report (even if duplicate exists)
				boolean statusAlreadyOpen = (previousStatus != null && previousStatus.equalsIgnoreCase("Open"));
				
				if (statusAlreadyOpen) {
					logger.info("Status was already 'Open' (previousStatus: {}). No status change detected. Skipping Open report creation for gate: {} (BOOM1_ID: {})", 
						previousStatus, gate, boom1IdForOpenUpdate);
				} else {
					logger.info("Status changed from '{}' to 'Open'. Creating Open report for gate: {} (BOOM1_ID: {}, lc_name: {})", 
						previousStatus, gate, boom1IdForOpenUpdate, obj3.getLc_name());
					
					// Validate required fields before creating report
					if (smForOpenUpdate == null || smForOpenUpdate.isEmpty()) {
						logger.error("Cannot create Open report - SM is null or empty for gate: {}", gate);
					} else if (gmForOpenUpdate == null || gmForOpenUpdate.isEmpty()) {
						logger.error("Cannot create Open report - GM is null or empty for gate: {}", gate);
					} else if (gateNumForOpenUpdate == null || gateNumForOpenUpdate.isEmpty()) {
						logger.error("Cannot create Open report - lc_name is null or empty for gate: {}", gate);
					} else {
						logger.info("Creating new Open report for gate: {} (BOOM1_ID: {}, lc_name: {}, sm: {}, gm: {}, time: {}, previousStatus: {})", 
							gate, boom1IdForOpenUpdate, gateNumForOpenUpdate, smForOpenUpdate, gmForOpenUpdate, time1, previousStatus);
						try {
							insertreports(obj3, time1, "");
							logger.info("SUCCESS: Created new Open report for gate: {} (BOOM1_ID: {}, lc_name: {})", 
								gate, boom1IdForOpenUpdate, gateNumForOpenUpdate);
						} catch (Exception e) {
							logger.error("EXCEPTION: Error creating Open report for gate: {} (BOOM1_ID: {})", gate, boom1IdForOpenUpdate, e);
						}
					}
				}

			}	

		}
	}

	/**
	 * Update main status to "Open" when any sensor (BS1, BS2, or LS) opens
	 * Create "Open" report ONLY when status changes from "Closed" to "Open"
	 * If status is already "Open", don't create duplicate report
	 * @param gateId The gate ID (can be BOOM1_ID, BOOM2_ID, or handle)
	 * @param boom1Id The BOOM1_ID to use for status update and report creation
	 */
	private void updateMainStatusToOpenAndCreateReportIfNeeded(String gateId, String boom1Id) {
		try {
			// Get current main status BEFORE updating to check if status is changing
			String previousStatus = null;
			try {
				String getStatusSql = "SELECT status FROM managegates WHERE BOOM1_ID=? LIMIT 1";
				previousStatus = (String) jdbcTemplate1.queryForObject(getStatusSql, new Object[] {boom1Id}, String.class);
			} catch (Exception e) {
				logger.debug("Could not get previous status for BOOM1_ID: {}", boom1Id);
			}
			
			logger.info("Previous main status for BOOM1_ID {}: {}", boom1Id, previousStatus);
			
			// Update main status to "Open"
			int mainStatusUpdate = jdbcTemplate1.update("update managegates set status=? where BOOM1_ID=?", new Object[] {"Open", boom1Id});
			if (mainStatusUpdate > 0) {
				logger.info("Main status updated to 'Open' for BOOM1_ID: {} (gateId: {})", boom1Id, gateId);
				
				// Only create Open report if status changed from "Closed" to "Open"
				// If status was already "Open", skip report creation (avoid duplicate)
				boolean statusChangedFromClosedToOpen = (previousStatus == null || previousStatus.equalsIgnoreCase("Closed"));
				
				if (statusChangedFromClosedToOpen) {
					logger.info("Status changed from '{}' to 'Open' for BOOM1_ID: {}. Creating Open report.", previousStatus, boom1Id);
					
					// Get gate data for report creation
					API obj3 = getDataFromManageGates(gateId);
					if (obj3 != null && obj3.getSm() != null && !obj3.getSm().isEmpty() && 
					    obj3.getGm() != null && !obj3.getGm().isEmpty() &&
					    obj3.getLc_name() != null && !obj3.getLc_name().isEmpty()) {
						
						// Format time for report
						String pattern3 = "HH:mm";
						SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat(pattern3);
						simpleDateFormat3.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
						String time1 = simpleDateFormat3.format(new Date());
						
						// Create Open report (only when status changes from Closed to Open)
						insertreports(obj3, time1, "");
						logger.info("Created Open report for gate: {} (BOOM1_ID: {}, lc_name: {})", gateId, boom1Id, obj3.getLc_name());
					} else {
						logger.warn("Cannot create Open report - missing required fields (SM, GM, or lc_name) for gate: {} (BOOM1_ID: {})", gateId, boom1Id);
					}
					
					// Clear health check when status changes to Open
					boomLockHealthService.clearHealthCheck(gateId);
				} else {
					logger.info("Status was already 'Open' (previousStatus: {}). Skipping Open report creation to avoid duplicate for BOOM1_ID: {}", previousStatus, boom1Id);
				}
			} else {
				logger.warn("Main status update failed for BOOM1_ID: {}", boom1Id);
			}
		} catch (Exception e) {
			logger.error("Error updating main status to Open for gate: {} (BOOM1_ID: {})", gateId, boom1Id, e);
		}
	}

	private API getDataFromManageGates(String gate) {
		logger.info("get manage data for gate: {}", gate);

		API obj = null;
		// Check BOOM1_ID, BOOM2_ID, handle, or LTSW_ID to find the gate
		String sql = "select * from managegates where BOOM1_ID=? OR BOOM2_ID=? OR handle=? OR LTSW_ID=? LIMIT 1";
		List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql,gate,gate,gate,gate);
		logger.info("Inside"+rows.size());

		if (rows == null || rows.isEmpty()) {
			logger.error("Gate not found in managegates table: BOOM1_ID/BOOM2_ID/handle={}. Cannot proceed with processing.", gate);
			return null;
		}

		try {
			Map row = rows.get(0);
			obj = new API(); 
			obj.setBs1Go((String) row.get("BS1_GO"));
			obj.setBs1Gc((String) row.get("BS1_GC"));
			obj.setGate((String) row.get("BOOM1_ID"));
			obj.setLc_name((String) row.get("Gate_Num"));
			obj.setSm((String) row.get("SM"));
			obj.setGm((String) row.get("GM"));
			obj.setBs1Status((String) row.get("BS1_STATUS"));
			obj.setLeverStatus((String) row.get("LEVER_STATUS"));
			obj.setBs2Status((String) row.get("BS2_STATUS"));
			obj.setLtStatus((String) row.get("LT_STATUS"));
		} catch (Exception e) {
			logger.error("Error extracting data from managegates for gate: {}", gate, e);
			return null;
		}
		return obj;
	}

	private boolean isGateInFailsafeForNonCtReports(String gateNum, String boom1Id) {
		// Failsafe definition for blocking non-ct Open/Close saves:
		// 1) managegates.is_failsafe is truthy OR
		// 2) gate appears in live failsafe tracker (same source as /gate/api/failsafe)
		// 3) an unacknowledged NO-NETWORK report exists for this gate
		try {
			String gn = gateNum != null ? gateNum.trim() : "";
			String b1 = boom1Id != null ? boom1Id.trim() : "";

			// 1) managegates.is_failsafe truthy
			try {
				String raw = jdbcTemplate1.queryForObject(
					"SELECT LOWER(COALESCE(CAST(is_failsafe AS CHAR), 'false')) FROM managegates WHERE Gate_Num=? OR BOOM1_ID=? OR handle=? LIMIT 1",
					new Object[] { gn, b1, b1 },
					String.class
				);
				String v = raw == null ? "" : raw.trim();
				boolean mgFailsafe = !(v.isEmpty() || "false".equals(v) || "0".equals(v) || "null".equals(v));
				if (mgFailsafe) return true;
			} catch (Exception ignore) {}

			// 2) live failsafe tracker (Redis-backed) used by /gate/api/failsafe and getstatus
			try {
				List<Map<String, Object>> gateRows = jdbcTemplate1.queryForList(
					"SELECT SM, GM, Gate_Num, BOOM1_ID FROM managegates WHERE Gate_Num=? OR BOOM1_ID=? OR handle=? LIMIT 1",
					gn, b1, b1
				);
				if (gateRows != null && !gateRows.isEmpty()) {
					Map<String, Object> row = gateRows.get(0);
					String sm = row.get("SM") != null ? row.get("SM").toString().trim() : "";
					String gm = row.get("GM") != null ? row.get("GM").toString().trim() : "";
					String gateNumResolved = row.get("Gate_Num") != null ? row.get("Gate_Num").toString().trim() : gn;
					String boom1Resolved = row.get("BOOM1_ID") != null ? row.get("BOOM1_ID").toString().trim() : b1;

					if (!sm.isEmpty()) {
						List<String> smFailsafeGates = smFailsafeService.getFailsafeGateNamesForSM(sm);
						if (smFailsafeGates != null && (smFailsafeGates.contains(gateNumResolved) || smFailsafeGates.contains(boom1Resolved))) {
							return true;
						}
					}
					if (!gm.isEmpty()) {
						List<String> gmFailsafeGates = smFailsafeService.getFailsafeGateNamesForGM(gm);
						if (gmFailsafeGates != null && (gmFailsafeGates.contains(gateNumResolved) || gmFailsafeGates.contains(boom1Resolved))) {
							return true;
						}
					}
				}
			} catch (Exception ignore) {}

			// 3) unacknowledged NO-NETWORK exists for gate (by lc_name or lc)
			Integer cnt = jdbcTemplate1.queryForObject(
				"SELECT COUNT(*) FROM reports WHERE command='NO-NETWORK' AND (ackn IS NULL OR ackn='') AND (lc_name=? OR lc=? OR lc_name=? OR lc=?)",
				new Object[] { gn, b1, b1, gn },
				Integer.class
			);
			return cnt != null && cnt.intValue() > 0;
		} catch (Exception e) {
			return false;
		}
	}
	
	private void insertreports( API obj,String format3,String date ) {
			// Use BOOM1_ID (obj.getGate()) for lc field to match closed report format
			// Use Gate_Num (obj.getLc_name()) for lc_name field
			try {
				logger.info("Inserting Open report - lc: {}, lc_name: {}, command: Open, sm: {}, gm: {}, time: {}", 
					obj.getGate(), obj.getLc_name(), obj.getSm(), obj.getGm(), format3);
				// Format current date/time as string to ensure IST timezone (same as NO-NETWORK)
				String pattern = "yyyy-MM-dd HH:mm:ss";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
				simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				String currentDateTime = simpleDateFormat.format(new Date());
				// Final rule: while gate is in failsafe, do NOT save non-ct Open/Close reports.
				boolean blockNonCtOpenInFailsafe = isGateInFailsafeForNonCtReports(obj.getLc_name(), obj.getGate());
				int insertResult;
				if (blockNonCtOpenInFailsafe) {
					logger.info("Gate is in failsafe (managegates.is_failsafe=true). Skipping non-ct Open report insert for gate: {} (BOOM1_ID: {}, lc_name: {})",
						obj.getGate(), obj.getGate(), obj.getLc_name());
					insertResult = 0;
				} else {
					insertResult = jdbcTemplate1.update("insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
							"","",format3,"Open","",obj.getSm(),obj.getGm(),obj.getGate(),obj.getLc_name(),
							currentDateTime,"Open","","","","",format3,"s");
				}
				if (insertResult > 0) {
					logger.info("SUCCESS: Created new Open report for gate: {} (BOOM1_ID: {}, lc_name: {}). Insert result: {}", 
						obj.getGate(), obj.getGate(), obj.getLc_name(), insertResult);
				} else {
					logger.error("FAILED: Could not create Open report for gate: {} (BOOM1_ID: {}). Insert result: {}", 
						obj.getGate(), obj.getGate(), insertResult);
				}
			} catch (Exception e) {
				logger.error("ERROR: Exception while creating Open report for gate: {} (BOOM1_ID: {}, lc_name: {})", 
					obj.getGate(), obj.getGate(), obj.getLc_name(), e);
				throw e; // Re-throw to allow caller to handle
			}
		}
}
