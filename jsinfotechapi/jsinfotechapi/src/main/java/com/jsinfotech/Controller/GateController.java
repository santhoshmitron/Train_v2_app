package com.jsinfotech.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.BigAdmin.Domain.Data;
import com.jsinfotech.Domain.API;
import com.jsinfotech.Domain.Gate;
import com.jsinfotech.Domain.Gate2;
import com.jsinfotech.Domain.ManageGates;
import com.jsinfotech.Domain.Response;
import com.jsinfotech.Service.ManageGatesService;
import com.jsinfotech.Service.ReportsService;

@RestController
@RequestMapping("/gate")
public class GateController {
	@Autowired 
	JdbcTemplate jdbcTemplate;

	@Autowired
	ManageGatesService Service;
	
	@Autowired
	ReportsService reportsService;
	
	@Autowired
    private KafkaTemplate<String, Gate> kafkaTemplate;
	
    private static final Logger logger = LogManager.getLogger(GateController.class);


	@RequestMapping(value = "/getstatus/{username}/role/{rolename}", method = RequestMethod.GET)
	public List<ManageGates> getGateStatus(@PathVariable("username") String username,@PathVariable("rolename") String rolename)
	{
		return Service.findByUsername(username,rolename);

	}

	@RequestMapping(value = "/getstatus1/{username}/role/{rolename}", method = RequestMethod.GET)
	public List<API> getGateStatus1(@PathVariable("username") String username, @PathVariable("rolename") String rolename)
	{
		return Service.findByUsernameAsAPI(username, rolename);
	}

	@RequestMapping(value = "/api", method = RequestMethod.GET)
	public List<API>addStatus(@RequestParam(name = "field1") String field1,@RequestParam(name = "field2") String field2,@RequestParam(name = "field3") String field3,@RequestParam(name = "field4") String field4,@RequestParam(name = "field5", required = false) String field5,@RequestParam(name = "field6", required = false) String field6,HttpServletRequest request)
	{
		String gateId = field1;
		
		// Format timestamp
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		timeFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
		String timestamp = timeFormat.format(new Date());
		
		// Determine sensor type
		String sensorType = "Unknown";
		if (gateId != null) {
			if (gateId.indexOf("LT") != -1) {
				sensorType = "LT";
			} else if (gateId.indexOf("BS2") != -1) {
				sensorType = "BS2";
			} else if (gateId.indexOf("BS") != -1) {
				sensorType = "BS1";
			} else if (gateId.indexOf("LS") != -1) {
				sensorType = "LS";
			}
		}
		
		// Beautiful formatted log entry
		logger.info("\n" +
			"═══════════════════════════════════════════════════════════════\n" +
			"📡 SENSOR DATA RECEIVED [{}]\n" +
			"═══════════════════════════════════════════════════════════════\n" +
			"Gate ID    : {}\n" +
			"Sensor Type: {}\n" +
			"Field 2    : {} ({})\n" +
			"Field 3    : {} ({})\n" +
			"Field 4    : {} ({})\n" +
			"Field 5    : {} ({})\n" +
			"Field 6    : {} ({})\n" +
			"───────────────────────────────────────────────────────────────",
			timestamp,
			gateId,
			sensorType,
			field2, (gateId != null && gateId.indexOf("LT") != -1) ? "LT1_STATUS" : "BS1/BS2 value",
			field3, (gateId != null && gateId.indexOf("LT") != -1) ? "LT2_STATUS" : "LS value",
			field4, (gateId != null && gateId.indexOf("LT") != -1) ? "LT_STATUS" : "Pass/Batch",
			field5 != null ? field5 : "N/A", "BS2 value",
			field6 != null ? field6 : "N/A", "LT value"
		);
		
		// Check if this is an LT gate (contains "LT" in the ID)
		if (gateId != null && gateId.indexOf("LT") != -1) {
			// Handle LT gates: field2=LT1_STATUS, field3=LT2_STATUS, field4=LT_STATUS
			// Values: 1 = "closed", 0 = "open"
			try {
				int lt1StatusValue = Integer.parseInt(field2);
				int lt2StatusValue = Integer.parseInt(field3);
				int ltStatusValue = Integer.parseInt(field4);
				
				// Convert to string values
				String lt1Status = (lt1StatusValue == 1) ? "closed" : "open";
				String lt2Status = (lt2StatusValue == 1) ? "closed" : "open";
				String ltStatus = (ltStatusValue == 1) ? "closed" : "open";
				
				logger.info("Updating LT statuses for gateId: {}, LT1_STATUS: {} ({}), LT2_STATUS: {} ({}), LT_STATUS: {} ({})", 
					gateId, lt1Status, lt1StatusValue, lt2Status, lt2StatusValue, ltStatus, ltStatusValue);
				
				// Update database directly for the gate ID
				// Try LTSW_ID first (for LT gates), then BOOM1_ID, BOOM2_ID, or handle
				int rowsAffected = 0;
				
				// First try LTSW_ID lookup (for LT gates like "E20-750LT")
				rowsAffected = jdbcTemplate.update(
					"UPDATE managegates SET LT1_STATUS=?, LT2_STATUS=?, LT_STATUS=? WHERE LTSW_ID=?",
					lt1Status, lt2Status, ltStatus, gateId
				);
				
				// If not found by LTSW_ID, try BOOM1_ID, BOOM2_ID, or handle
				if (rowsAffected == 0) {
					rowsAffected = jdbcTemplate.update(
						"UPDATE managegates SET LT1_STATUS=?, LT2_STATUS=?, LT_STATUS=? WHERE BOOM1_ID=? OR BOOM2_ID=? OR handle=?",
						lt1Status, lt2Status, ltStatus, gateId, gateId, gateId
					);
				}
				
				if (rowsAffected > 0) {
					// Update timestamp for status update log
					timestamp = timeFormat.format(new Date());
					logger.info("\n" +
						"───────────────────────────────────────────────────────────────\n" +
						"[LT] [{}] Status Updated [{}]\n" +
						"───────────────────────────────────────────────────────────────\n" +
						"LT1_STATUS: {} (value: {})\n" +
						"LT2_STATUS: {} (value: {})\n" +
						"LT_STATUS: {} (value: {})\n" +
						"───────────────────────────────────────────────────────────────",
						gateId, timestamp, lt1Status, lt1StatusValue, lt2Status, lt2StatusValue, ltStatus, ltStatusValue);
				} else {
					logger.warn("No rows updated for gateId: {} - gate may not exist (tried LTSW_ID, BOOM1_ID, BOOM2_ID, handle)", gateId);
				}
			} catch (Exception e) {
				logger.error("Error updating LT statuses for gateId: {}", gateId, e);
			}
			
			// Return empty list as before (maintaining API contract)
			return new java.util.ArrayList<API>();
		} else {
			// Handle BS1, BS2, LS gates: use original logic
			// field2 = sensor value for BS1/BS2, field3 = sensor value for LS, field4 = pass/batch
			int status = Integer.parseInt(field2);
			int statuss = Integer.parseInt(field3);
			String pass = field4;
			int bs2Value = field5 != null && !field5.isEmpty() ? Integer.parseInt(field5) : -1;
			int ltValue = field6 != null && !field6.isEmpty() ? Integer.parseInt(field6) : -1;
			
			logger.info("Processing BS1/BS2/LS gate - gateId: {}, status: {}, statuss: {}, pass: {}, bs2Value: {}, ltValue: {}", 
				gateId, status, statuss, pass, bs2Value, ltValue);
			
			// Call original service method to process BS1, BS2, LS sensors
			return Service.addstatusofgate(gateId, status, statuss, pass, bs2Value, ltValue, request);
		}
	}


	@RequestMapping(value = "/gateFailStatus/{username}/role/{role}", method = RequestMethod.GET)
	public Response getGateTimeStatus(@PathVariable("username") String username,@PathVariable("role") String role)
	{
		Response report = new Response();
		//String status = Service.GetGateTimeDiff(username,role).toString();
		//String status = Service.GetGateFailSafeStatus(username,role).toString();
		report.setStatus("false");
		return report;

	}
	
	
	@RequestMapping(value = "/initfailsafe", method = RequestMethod.GET)
	public Response failsafeinit()
	{
		Response report = new Response();
		Boolean status = Service.failsafeinit();
		//report.setStatus(status);
		return report;

	}
	
	@RequestMapping(value = "/api/data", method = RequestMethod.GET)
	public List<Data>getrecords()
	{
	
		logger.info("returning data");

		return Service.getdata();
	}
	
	/**
	 * New endpoint to check failsafe status for an SM or GM
	 * Returns failsafe status and list of gate numbers that are in failsafe mode
	 * field1 = SM (e.g., "PURA_SM") or GM (e.g., "PURA_GM")
	 * Determines SM vs GM based on naming pattern (ends with "_SM" for SM, otherwise GM)
	 */
	@RequestMapping(value = "/api/failsafe", method = RequestMethod.GET)
	public Response getFailsafeStatus(@RequestParam(name = "field1") String field1) {
		Response response = new Response();
		try {
			String userid = field1;
			boolean isSM = userid.endsWith("_SM");
			logger.info("Checking failsafe status for {}: {}", isSM ? "SM" : "GM", userid);
			
			boolean failsafe = Service.getSMFailsafeStatus(userid);
			java.util.List<String> failsafeGateNames = Service.getFailsafeGateNamesForSM(userid);
			
			// Extract gate numbers from gate names and convert to integers (using Set to ensure uniqueness)
			java.util.Set<Integer> gateNumbersSet = new java.util.HashSet<>();
			for (String gateName : failsafeGateNames) {
				try {
					// Try to parse the gate name as integer (gate names might be numeric like "751")
					int gateNumber = Integer.parseInt(gateName.trim());
					gateNumbersSet.add(gateNumber);
				} catch (NumberFormatException e) {
					// If gate name is not a number, try to extract numbers from it
					// For example, if gate name is "LC 750", extract 750
					String numericPart = gateName.replaceAll("[^0-9]", "");
					if (!numericPart.isEmpty()) {
						try {
							gateNumbersSet.add(Integer.parseInt(numericPart));
						} catch (NumberFormatException ex) {
							logger.warn("Could not extract number from gate name: {}", gateName);
						}
					}
				}
			}
			
			// Convert Set to sorted List to ensure unique and sorted gate numbers
			java.util.List<Integer> gateNumbers = new java.util.ArrayList<>(gateNumbersSet);
			java.util.Collections.sort(gateNumbers);
			
			if (failsafe) {
				response.setStatus("true");
				if (!gateNumbers.isEmpty()) {
					// Format message based on number of gates
					StringBuilder messageBuilder = new StringBuilder("LC ");
					for (int i = 0; i < gateNumbers.size(); i++) {
						if (i > 0) {
							messageBuilder.append(", ");
						}
						messageBuilder.append(gateNumbers.get(i));
					}
					messageBuilder.append(",  No network detected. Operate with Manual PN");
					response.setMessage(messageBuilder.toString());
					response.setNumber(gateNumbers);
				} else {
					response.setMessage("Failsafe is active - SM has no gates that haven't received data");
					response.setNumber(new java.util.ArrayList<>());
				}
			} else {
				response.setStatus("false");
				response.setMessage("Failsafe is not active - All gates are receiving data normally");
				response.setNumber(new java.util.ArrayList<>());
			}
			
			// Get failsafe report data and merge into response
			com.jsinfotech.Domain.FailsafeResponse failsafeResponse = reportsService.getFailsafeResponseData(userid);
			if (failsafeResponse != null) {
				response.setReportid(failsafeResponse.getReportid());
				response.setPlay_command(failsafeResponse.getPlay_command());
			} else {
				response.setReportid(new java.util.ArrayList<>());
				response.setPlay_command(new java.util.ArrayList<>());
			}
			
			return response;
		} catch (Exception e) {
			logger.error("Error getting failsafe status for userid: {}", field1, e);
			response.setStatus("false");
			response.setMessage("Error checking failsafe status: " + e.getMessage());
			response.setNumber(new java.util.ArrayList<>());
			response.setReportid(new java.util.ArrayList<>());
			response.setPlay_command(new java.util.ArrayList<>());
			return response;
		}
	}


}
