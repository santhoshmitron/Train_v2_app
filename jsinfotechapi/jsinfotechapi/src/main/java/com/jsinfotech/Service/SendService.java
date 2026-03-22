package com.jsinfotech.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Gate;
import com.jsinfotech.Domain.Gate2;
import com.jsinfotech.Domain.ManageGates;
import com.jsinfotech.Domain.Send;
import com.jsinfotech.Service.RedisUserRepository;

@Service
public class SendService {

	private static final Logger logger = LogManager.getLogger(SendService.class);

	@Autowired 
	ManageGatesService managegateservice;

	@Autowired 
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	private RedisUserRepository userRepository;
	
	@Autowired
	private PNGenerationService pnGenerationService;
	
	 @Autowired
		private KafkaTemplate<String, Gate> kafkaTemplate;
	    
	    @Value(value = "${kafka.topic1}")
	    private String topic1;
	    
	    @Value(value = "${kafka.topic2}")
	    private String topic2;

	/**
	 * Validate provided PNs
	 * Checks: 3-digit (111-999), no zeros, no duplicates
	 * 
	 * @param pns Array of PN strings to validate
	 * @return ValidationResult with isValid flag and error message
	 */
	private static class ValidationResult {
		boolean isValid;
		String errorMessage;
		
		ValidationResult(boolean isValid, String errorMessage) {
			this.isValid = isValid;
			this.errorMessage = errorMessage;
		}
	}
	
	private ValidationResult validatePNs(String[] pns) {
		if (pns == null || pns.length == 0) {
			return new ValidationResult(true, ""); // Empty is valid (will auto-generate)
		}
		
		java.util.Set<String> seenPNs = new java.util.HashSet<>();
		
		for (int i = 0; i < pns.length; i++) {
			String pn = pns[i];
			if (pn == null || pn.trim().isEmpty()) {
				continue; // Empty PN is OK (will be auto-generated)
			}
			
			pn = pn.trim();
			
			// Check if it's a valid 3-digit number
			try {
				int pnInt = Integer.parseInt(pn);
				
				// Check range (111-999)
				if (pnInt < 111 || pnInt > 999) {
					return new ValidationResult(false, 
						String.format("PN '%s' at position %d is not in valid range (111-999)", pn, i + 1));
				}
				
				// Check no zeros
				if (pn.indexOf('0') >= 0) {
					return new ValidationResult(false, 
						String.format("PN '%s' at position %d contains zero. PNs must not contain 0", pn, i + 1));
				}
				
				// Check for duplicates
				if (seenPNs.contains(pn)) {
					return new ValidationResult(false, 
						String.format("Duplicate PN '%s' found. Each PN must be unique", pn));
				}
				
				seenPNs.add(pn);
			} catch (NumberFormatException e) {
				return new ValidationResult(false, 
					String.format("PN '%s' at position %d is not a valid number", pn, i + 1));
			}
		}
		
		return new ValidationResult(true, "");
	}

	/**
	 * Get PNs for all gates under an SM user
	 * Returns dash-separated PNs string (e.g., "123-456-789")
	 * ALWAYS generates NEW unique PNs on each API call
	 * Ensures 3-digit, no-zero, 24-hour uniqueness per gate
	 * PNs generated here are marked as used to prevent duplicates within 24 hours
	 * 
	 * @param smUser The SM username
	 * @return Dash-separated PNs string, or empty string if no gates found
	 */
	public String getPNsForSM(String smUser) {
		try {
			// Query all gates for this SM
			String sql = "SELECT * FROM managegates WHERE SM=?";
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, smUser);
			
			if (rows == null || rows.isEmpty()) {
				logger.info("No gates found for SM: {}", smUser);
				return "";
			}
			
			java.util.List<String> pns = new java.util.ArrayList<>();
			
			for (Map<String, Object> row : rows) {
				String boom1Id = (String) row.get("BOOM1_ID");
				String gateNum = (String) row.get("Gate_Num");
				
				// Use boom1Id as primary identifier, fallback to gateNum
				String gateIdentifier = (boom1Id != null && !boom1Id.trim().isEmpty()) ? boom1Id : gateNum;
				
				if (gateIdentifier == null || gateIdentifier.trim().isEmpty()) {
					logger.warn("Skipping gate with no identifier (BOOM1_ID: {}, Gate_Num: {})", boom1Id, gateNum);
					continue;
				}
				
				try {
					// ALWAYS generate a new unique PN for each API call
					// The generateUniquePNForGate method checks 24-hour uniqueness automatically
					logger.info("Generating new PN for gate: {} (BOOM1_ID: {})", gateNum, boom1Id);
					
					// Generate unique PN with 24-hour uniqueness check
					// This method checks both DB and Redis for used PNs in last 24 hours
					// It uses resolveGateIdentifiers internally to get all gate keys
					String pn = pnGenerationService.generateUniquePNForGate(gateIdentifier);
					
					// IMPORTANT: Mark PN as used in Redis for ALL gate lookup keys
					// This ensures the same PN won't be returned for this gate within 24 hours
					// We need to use the same gate identifier resolution that generateUniquePNForGate uses
					// to ensure we mark it for all the same keys it checks
					try {
						// Get all gate lookup keys (same as generateUniquePNForGate checks)
						java.util.Set<String> allGateKeys = pnGenerationService.getAllGateKeysForIdentifier(gateIdentifier);
						
						// Mark PN as used for ALL lookup keys (BOOM1_ID, Gate_Num, etc.)
						// This matches what generateUniquePNForGate checks
						for (String gateKey : allGateKeys) {
							if (gateKey != null && !gateKey.trim().isEmpty()) {
								userRepository.markPNAsUsed(gateKey, pn);
								logger.debug("Marked PN {} as used for gate key: {}", pn, gateKey);
							}
						}
						logger.info("Marked PN {} as used for all gate keys: {}", pn, allGateKeys);
					} catch (Exception markException) {
						// Fallback: mark for the gate identifier we have
						logger.warn("Failed to get gate keys for marking, using fallback. Error: {}", markException.getMessage());
						userRepository.markPNAsUsed(gateIdentifier, pn);
						if (gateNum != null && !gateNum.equals(gateIdentifier)) {
							userRepository.markPNAsUsed(gateNum, pn);
						}
					}
					
					// Validate generated PN format: 3-digit, no zeros
					try {
						int pnInt = Integer.parseInt(pn);
						if (pn.length() == 3 && pn.indexOf('0') < 0 && pnInt >= 111 && pnInt <= 999) {
							pns.add(pn);
							logger.info("Generated and marked PN: {} for gate: {} (BOOM1_ID: {})", pn, gateNum, boom1Id);
						} else {
							logger.error("Generated invalid PN format: {} for gate: {} (BOOM1_ID: {}). Length: {}, Contains 0: {}, Value: {}", 
								pn, gateNum, boom1Id, pn.length(), pn.indexOf('0') >= 0, pnInt);
							// Still add it, but log the error
							pns.add(pn);
						}
					} catch (NumberFormatException e) {
						logger.error("Generated non-numeric PN: {} for gate: {} (BOOM1_ID: {})", pn, gateNum, boom1Id);
						// Still add it, but log the error
						if (pn != null && !pn.isEmpty()) {
							pns.add(pn);
						}
					}
				} catch (Exception e) {
					logger.error("Error generating PN for gate: {} (BOOM1_ID: {}): {}", gateNum, boom1Id, e.getMessage(), e);
					// Continue to next gate instead of adding empty
				}
			}
			
			// Return dash-separated PNs
			String result = String.join("-", pns);
			logger.info("Generated new PNs for SM: {}: {}", smUser, result);
			return result;
		} catch (Exception e) {
			logger.error("Error getting PNs for SM: {}", smUser, e);
			return "";
		}
	}

	public String sendData1(Send send) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("user", send.getUser());
		Send  getdata=  new Send();
		getdata.setTn(send.getTn());
		getdata.setWer(send.getWer());
		getdata.setPn(send.getPn());
		getdata.setUser(send.getUser());
		getdata.setRoles(send.getRoles());
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		String format3 = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
		System.out.println("now time"+format3);
		String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
		System.out.println("@@@@@"+date);
		String date1 = date;
		System.out.println("@@@@@1111"+date1);

		String sql = "select * from managegates where SM=?";


		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql,send.getUser());
        
		// Handle PN: if null or empty, prepare empty array; otherwise split by "-"
		String[] pnlist = null;
		if (send.getPn() != null && !send.getPn().trim().isEmpty()) {
			pnlist = send.getPn().split("-");
			
			// Validate provided PNs
			ValidationResult validation = validatePNs(pnlist);
			if (!validation.isValid) {
				logger.warn("Invalid PNs provided: {}. Error: {}. Will auto-generate PNs instead.", 
					send.getPn(), validation.errorMessage);
				// Auto-generate instead of rejecting
				pnlist = new String[rows.size()];
				for (int i = 0; i < pnlist.length; i++) {
					pnlist[i] = ""; // Empty PN - will be auto-generated
				}
			} else {
				logger.info("Valid PNs provided: {}", send.getPn());
				// Ensure pnlist has enough elements for all gates
				if (pnlist.length < rows.size()) {
					String[] extendedPnlist = new String[rows.size()];
					System.arraycopy(pnlist, 0, extendedPnlist, 0, pnlist.length);
					for (int i = pnlist.length; i < extendedPnlist.length; i++) {
						extendedPnlist[i] = ""; // Empty PN for remaining gates
					}
					pnlist = extendedPnlist;
				}
			}
		} else {
			logger.info("PN is null or empty for train: {}. Will auto-generate PN after report creation.", send.getTn());
			// Create empty array with same length as rows to handle missing PN values
			pnlist = new String[rows.size()];
			for (int i = 0; i < pnlist.length; i++) {
				pnlist[i] = ""; // Empty PN - will be auto-generated
			}
		}
		
		// Collect generated PNs to return in response
		java.util.List<String> generatedPNs = new java.util.ArrayList<>();
		
        int count = 0;
		for (Map row : rows) {
			System.out.println(row);
			ManageGates obj = new ManageGates(); 
			obj.setBoom1Id(((String) row.get("BOOM1_ID")));  
			obj.setGateNum((String)  row.get("Gate_Num"));
			obj.setBs1Status((String) row.get("BS1_STATUS"));
			obj.setGM((String) row.get("GM"));
			obj.setAdded_on(new Date());
			
			// Get PN for this gate (use empty string if not enough values in array)
			String pnValue = (count < pnlist.length) ? pnlist[count] : "";
			if (pnValue == null || pnValue.trim().isEmpty()) {
				pnValue = ""; // Empty PN - will be auto-generated
				logger.info("PN is empty for gate: {} (BOOM1_ID: {}). Will auto-generate after report creation.", 
					obj.getGateNum(), obj.getBoom1Id());
			}
			
			jdbcTemplate.update("insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
					send.getTn(), pnValue, format3, "Close", send.getWer(), send.getUser(), obj.getGM(), obj.getBoom1Id(), obj.getGateNum(),
					date1, "", "", "", "", "", "", ""
					);
			
			// Get the actual report ID that was inserted (query by unique combination)
			Integer reportId = null;
			try {
				java.util.List<java.util.Map<String, Object>> idRows = jdbcTemplate.queryForList(
					"SELECT id FROM reports WHERE tn=? AND sm=? AND gm=? AND lc=? AND command='Close' AND added_on=? ORDER BY id DESC LIMIT 1",
					send.getTn(), send.getUser(), obj.getGM(), obj.getBoom1Id(), date1
				);
				if (idRows != null && !idRows.isEmpty()) {
					Object idObj = idRows.get(0).get("id");
					if (idObj != null) {
						reportId = (idObj instanceof Integer) ? (Integer) idObj : Integer.parseInt(idObj.toString());
					}
				}
			} catch (Exception e) {
				logger.warn("Could not retrieve report ID for gate: {}", obj.getGateNum(), e);
			}
			
			// If PN was provided and validated, ensure it's stored in database
			if (pnValue != null && !pnValue.trim().isEmpty() && reportId != null) {
				try {
					int updated = jdbcTemplate.update("UPDATE reports SET pn = ? WHERE id = ?", pnValue, reportId);
					if (updated > 0) {
						logger.debug("Updated report ID {} with provided PN: {}", reportId, pnValue);
					}
				} catch (Exception e) {
					logger.warn("Error updating PN in database for report ID: {}", reportId, e);
				}
			}
			
			// Auto-generate PN if PN is empty or null
			// This will generate PN for the most recent report with empty PN for this gate
			if (pnValue == null || pnValue.trim().isEmpty()) {
				try {
					// SM PN must be generated at train-send time even if gate is not yet closed
					pnGenerationService.generateSMTrainPNIfMissing(obj.getBoom1Id(), obj.getGM(), send.getUser());
					logger.info("SM PN auto-generation triggered for gate: {} (BOOM1_ID: {}), SM: {}, GM: {} - PN was empty", 
						obj.getGateNum(), obj.getBoom1Id(), send.getUser(), obj.getGM());
					
					// Retrieve the generated PN from database or Redis
					// Add small delay to ensure PN is stored after generation
					if (reportId != null) {
						String generatedPN = null;
						// Try Redis first
						generatedPN = userRepository.findReportIdPN(String.valueOf(reportId));
						
						// If not in Redis, try database (with small retry)
						if (generatedPN == null || generatedPN.trim().isEmpty()) {
							for (int retry = 0; retry < 3; retry++) {
								try {
									if (retry > 0) {
										Thread.sleep(100); // Small delay for retry
									}
									java.util.List<java.util.Map<String, Object>> pnRows = jdbcTemplate.queryForList(
										"SELECT pn FROM reports WHERE id=? AND (pn IS NOT NULL AND pn != '')", reportId
									);
									if (pnRows != null && !pnRows.isEmpty()) {
										Object pnObj = pnRows.get(0).get("pn");
										if (pnObj != null && !pnObj.toString().trim().isEmpty()) {
											generatedPN = pnObj.toString().trim();
											break;
										}
									}
								} catch (Exception e) {
									if (retry == 2) {
										logger.warn("Could not retrieve PN from database for report ID: {} after retries", reportId, e);
									}
								}
							}
						}
						
						if (generatedPN != null && !generatedPN.trim().isEmpty()) {
							generatedPNs.add(generatedPN);
							logger.debug("Retrieved generated PN: {} for report ID: {}", generatedPN, reportId);
						} else {
							logger.warn("Could not retrieve generated PN for report ID: {} - using empty placeholder", reportId);
							generatedPNs.add(""); // Placeholder if PN not found yet
						}
					} else {
						logger.warn("Report ID is null for gate: {} - cannot retrieve generated PN", obj.getGateNum());
						generatedPNs.add(""); // Placeholder if report ID not found
					}
				} catch (Exception pnEx) {
					logger.warn("Error auto-generating SM PN for gate: {} (BOOM1_ID: {}), SM: {}, GM: {}", 
						obj.getGateNum(), obj.getBoom1Id(), send.getUser(), obj.getGM(), pnEx);
					generatedPNs.add(""); // Placeholder on error
				}
			} else {
				logger.debug("PN already provided for gate: {} (BOOM1_ID: {}), PN: {}. Using provided PN.", 
					obj.getGateNum(), obj.getBoom1Id(), pnValue);
				generatedPNs.add(pnValue); // Use provided PN
				
				// Store provided PN in Redis for tracking
				if (reportId != null) {
					try {
						userRepository.saveReportIdPN(String.valueOf(reportId), pnValue);
						// Mark PN as used for this gate (24-hour uniqueness)
						userRepository.markPNAsUsed(obj.getBoom1Id(), pnValue);
						if (obj.getGateNum() != null && !obj.getGateNum().trim().isEmpty()) {
							userRepository.markPNAsUsed(obj.getGateNum(), pnValue);
						}
						logger.info("Stored provided PN: {} for report ID: {} and gate: {}", pnValue, reportId, obj.getGateNum());
					} catch (Exception e) {
						logger.warn("Error storing provided PN in Redis for report ID: {}", reportId, e);
					}
				}
			}
			
			Gate gate1 = new Gate(obj.getGateNum(), obj.getBoom1Id(), String.valueOf("500"),String.valueOf("500"));
			gate1.setBatch("Train");
			gate1.setDate(date);
			kafkaTemplate.send(topic1,gate1);
			System.out.println("Train Sent"+gate1.toString());
			count++;
		}	

		// Return dash-separated PNs string (e.g., "123-456-789")
		return String.join("-", generatedPNs);
	}

	public Boolean cancelTrain(Send send) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("user", send.getUser());
		Send  getdata=  new Send();
		getdata.setTn(send.getTn());
		getdata.setWer(send.getWer());
		getdata.setPn(send.getPn());
		getdata.setUser(send.getUser());
		getdata.setRoles(send.getRoles());
        String[] pnlist =  send.getPn().split("-");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		String date1 = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
		String pattern = "yyyy-MM-dd 00:00:00";
		String format3 = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
		System.out.println("now time"+format3);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		String date = simpleDateFormat.format(new Date());


		String sql = "select * from managegates where SM=?";


		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql,send.getUser());
        int count = 0;

		for (Map row : rows) {
			System.out.println(row);
			ManageGates obj = new ManageGates(); 
			obj.setBoom1Id(((String) row.get("BOOM1_ID")));  
			obj.setGateNum((String)  row.get("Gate_Num"));
			obj.setBs1Status((String) row.get("BS1_STATUS"));
			obj.setGM((String) row.get("GM"));
			obj.setAdded_on(new Date());

			// If gate main status is closed, mark Cancel report as Closed with lock time.
			String mainStatus = "";
			Object statusObj = row.get("status");
			if (statusObj == null) {
				statusObj = row.get("STATUS");
			}
			if (statusObj != null) {
				mainStatus = statusObj.toString();
			}
			boolean isClosed = mainStatus != null && mainStatus.equalsIgnoreCase("closed");
			String cancelLcStatus = isClosed ? "Closed" : "";
			String cancelLcLockTime = isClosed ? format3 : "";

			// IMPORTANT: Do not auto-fill ackn. ackn must remain empty until user explicitly acknowledges via ACK.
			// The previous update set ackn='00:00' for all matching Close rows (by tn+sm), which incorrectly marked
			// unrelated gates/reports as acknowledged.
			// jdbcTemplate.update("update reports set ackn=? where tn=? and command=? and sm=? and added_on > ?","00:00",send.getTn(),"Close",send.getUser(),date);
			jdbcTemplate.update("insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name,added_on,lc_status,lc_lock_time,lc_pin,lc_pin_time,ackn,lc_open_time,redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
					send.getTn(),pnlist[count],format3,"Cancel",send.getWer(),send.getUser(),obj.getGM(),obj.getBoom1Id(),obj.getGateNum(),
					date1,cancelLcStatus,cancelLcLockTime,"","","","",""
					);
			Gate gate1 = new Gate(obj.getGateNum(), obj.getBoom1Id(), String.valueOf("500"),String.valueOf("500"));
			gate1.setBatch("Train");
			gate1.setDate(date);
			kafkaTemplate.send(topic1,gate1);
			System.out.println(gate1.toString());
			count++;
		}	
		

		return true;


	}

	/**
	 * Process command based on field1 (SM) and field2 (direction-status like "UP-0", "UP-1", "DN-0", "DN-1")
	 * - Parse field2 to extract direction (UP/DN) and status (0/1)
	 * - If status is 0: don't check anything, just return
	 * - If status is 1: 
	 *   - Check Redis to see if already processed (to avoid duplicate writes)
	 *   - If not processed, write the record once and store in Redis
	 *   - Store status in Redis with short TTL (status 1 is only for short duration)
	 */
	public Boolean processCommand(String field1, String field2) {
		String direction = null;
		String status = null;
		
		try {
			// Validate input parameters
			if (field1 == null || field1.trim().isEmpty()) {
				logger.error("Invalid field1: field1 is null or empty");
				return false;
			}
			
			if (field2 == null || field2.trim().isEmpty()) {
				logger.error("Invalid field2: field2 is null or empty for SM: {}", field1);
				return false;
			}
			
			// Parse field2 to extract direction and status
			// Expected format: "UP-0", "UP-1", "DN-0", "DN-1"
			try {
				if (field2.contains("-")) {
					String[] parts = field2.split("-");
					if (parts.length == 2) {
						direction = parts[0].toUpperCase().trim(); // UP or DN
						status = parts[1].trim(); // 0 or 1
					}
				}
			} catch (Exception e) {
				logger.error("Error parsing field2: {} for SM: {}", field2, field1, e);
				return false;
			}
			
			if (direction == null || status == null || (!direction.equals("UP") && !direction.equals("DN"))) {
				logger.error("Invalid field2 format: {} for SM: {}. Expected format: UP-0, UP-1, DN-0, or DN-1", field2, field1);
				return false;
			}
			
			if (!status.equals("0") && !status.equals("1")) {
				logger.error("Invalid status value: {} for SM: {}, Direction: {}. Expected 0 or 1", status, field1, direction);
				return false;
			}
			
			// If status is 0, evict the Redis key (if it exists) so that next status 1 will be treated as new event
			if ("0".equals(status)) {
				try {
					// Check if there's a status 1 in Redis and evict it
					String existingStatus = userRepository.getSensorStatus(field1, direction);
					if ("1".equals(existingStatus)) {
						userRepository.deleteSensorStatus(field1, direction);
						logger.info("Status changed from 1 to 0 for SM: {}, Direction: {} - Evicted Redis key", field1, direction);
					} else {
						logger.debug("Status is 0 for SM: {}, Direction: {} - No action taken", field1, direction);
					}
					return true;
				} catch (Exception e) {
					logger.error("Error handling status 0 for SM: {}, Direction: {}", field1, direction, e);
					// Continue execution even if Redis operation fails
					return true;
				}
			}
			
			// Status is 1 - check if we already processed this
			try {
				String existingStatus = userRepository.getSensorStatus(field1, direction);
				if ("1".equals(existingStatus)) {
					logger.info("Status 1 already processed for SM: {}, Direction: {} - Skipping duplicate write", field1, direction);
					return true;
				}
			} catch (Exception e) {
				logger.warn("Error checking Redis status for SM: {}, Direction: {}. Proceeding with record write.", field1, direction, e);
				// Continue execution even if Redis check fails
			}
			
			// Query managegates where SM = field1
			List<Map<String, Object>> rows = null;
			try {
				String sql = "select * from managegates where SM=?";
				rows = jdbcTemplate.queryForList(sql, field1);
			} catch (DataAccessException e) {
				logger.error("Database error querying managegates for SM: {}", field1, e);
				return false;
			} catch (Exception e) {
				logger.error("Unexpected error querying managegates for SM: {}", field1, e);
				return false;
			}
			
			if (rows == null || rows.isEmpty()) {
				logger.warn("No gates found for SM: {}", field1);
				return false;
			}
			
			// Format date and time
			String date1 = null;
			String format3 = null;
			try {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
				date1 = simpleDateFormat.format(new Date());
				LocalDateTime now = LocalDateTime.now();
				format3 = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
			} catch (Exception e) {
				logger.error("Error formatting date/time for SM: {}", field1, e);
				return false;
			}
			
			// Single loop: check all gates and collect open gate names
			boolean allClosed = true;
			StringBuilder openGateNames = new StringBuilder();
			
			try {
				for (Map<String, Object> row : rows) {
					if (row == null) {
						continue;
					}
					
					String gateStatus = null;
					String gateName = null;
					
					try {
						gateStatus = (String) row.get("status");
						gateName = (String) row.get("Gate_Num");
					} catch (Exception e) {
						logger.warn("Error reading gate data from row for SM: {}", field1, e);
						continue;
					}
					
					if (gateStatus != null && !"Closed".equalsIgnoreCase(gateStatus)) {
						allClosed = false;
						if (gateName != null) {
							if (openGateNames.length() > 0) {
								openGateNames.append(", ");
							}
							openGateNames.append(gateName);
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error processing gate rows for SM: {}", field1, e);
				return false;
			}
			
			// If all gates are Closed, don't write any record
			if (allClosed) {
				logger.info("All gates are Closed for SM: {} - No record written", field1);
				// Still store the sensor status in Redis
				try {
					userRepository.setSensorStatus(field1, direction, status);
				} catch (Exception e) {
					logger.warn("Error storing sensor status in Redis for SM: {}, Direction: {}", field1, direction, e);
					// Continue even if Redis operation fails
				}
				return true;
			}
			
			// Write the record to database
			try {
				int rowsAffected = jdbcTemplate.update(
					"insert into reports (tn, pn, tn_time, command, wer, sm, gm, lc, lc_name, added_on, lc_status, lc_lock_time, lc_pin, lc_pin_time, ackn, lc_open_time, redy) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
					"", "", "", "GNC["+format3+"]", "", field1, "", "", "",
					date1, "", "", "", "", "", "", ""
				);
				
				if (rowsAffected <= 0) {
					logger.warn("No rows inserted for SM: {}, Direction: {}", field1, direction);
				} else {
					logger.info("Inserted LCLEARED report for SM: {}, Direction: {} - Open gates: {}", 
						field1, direction, openGateNames.toString());
				}
			} catch (DataAccessException e) {
				logger.error("Database error inserting report for SM: {}, Direction: {}", field1, direction, e);
				return false;
			} catch (Exception e) {
				logger.error("Unexpected error inserting report for SM: {}, Direction: {}", field1, direction, e);
				return false;
			}
			
			// Store sensor status in Redis (status 1) - NO TTL, persists until status 0 deletes it
			try {
				userRepository.setSensorStatus(field1, direction, status);
			} catch (Exception e) {
				logger.warn("Error storing sensor status in Redis for SM: {}, Direction: {}. Record was written but Redis update failed.", 
					field1, direction, e);
				// Continue even if Redis operation fails
			}
			
			// Push audio messages
			try {
				String message = " LC Gates are NOT Closed";
				userRepository.pushAudio(field1, message);
				userRepository.pushAudio(field1, message);
				userRepository.pushAudio(field1, message);
			} catch (Exception e) {
				logger.warn("Error pushing audio messages for SM: {}", field1, e);
				// Continue even if audio push fails
			}
		
			return true;
		} catch (NullPointerException e) {
			logger.error("NullPointerException processing command for field1: {}, field2: {}", field1, field2, e);
			return false;
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException processing command for field1: {}, field2: {}", field1, field2, e);
			return false;
		} catch (DataAccessException e) {
			logger.error("DataAccessException processing command for field1: {}, field2: {}", field1, field2, e);
			return false;
		} catch (Exception e) {
			logger.error("Unexpected error processing command for field1: {}, field2: {}", field1, field2, e);
			return false;
		}
	}
}
