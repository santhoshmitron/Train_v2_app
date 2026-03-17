package com.jsinfotech.Controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.Domain.Reports;
import com.jsinfotech.Domain.ResponseReport;
import com.jsinfotech.Service.RedisUserRepository;
import com.jsinfotech.Service.ReportsService;


@RestController
@RequestMapping("/reports")
public class ReportsController {

	
	@Autowired
	ReportsService Service;
	
	@Autowired
	public RedisUserRepository userRepository;
	
    private static final Logger logger = LogManager.getLogger(ReportsController.class);

	
	@RequestMapping(value = "/getreports/{username}/role/{role}", method = RequestMethod.GET)
	public ResponseReport getReports(@PathVariable("username") String username,@PathVariable("role") String role,@RequestParam(name = "from",required= false,defaultValue = "t") String from,@RequestParam(name = "to",required= false,defaultValue="") String to)
	{
		
		ResponseReport report = new ResponseReport();
        
		    List<String> commands = new ArrayList<String>();
			
			System.out.println("GetReports"+username+role);

			if(!from.contentEquals("t")) 
				{
				//report.setPlay_command(commands);
				report.setReport(Service.findreports(username,role,from,to));
				return report;
					
				}else if(role.indexOf('g')!=-1) {
					// Get Redis audio only (play commands moved to getstatus API)
					String com = userRepository.poppAudio(username);
					if (com != null && !com.trim().isEmpty()) {
				    commands.add(com);
					}
					
					// Remove empty strings
					commands.removeIf(cmd -> cmd == null || cmd.trim().isEmpty());
					
					report.setPlay_command(commands);
					report.setReport(Service.findByUsernamegm(username,role));
					System.out.println("GetReportsg"+username+role);

					return report;
				}else {
					  // Get Redis audio only (play commands moved to getstatus API)
					  String com = userRepository.poppAudio(username);
					  String com1 = userRepository.poppAudio1(username);
					  
					  // Add Redis audio (only non-empty)
					  if (com != null && !com.trim().isEmpty()) {
				    	commands.add(com);
				    }
					  if (com1 != null && !com1.trim().isEmpty()) {
				    	commands.add(com1);
				    }
					  
					  // Remove empty strings
					  commands.removeIf(cmd -> cmd == null || cmd.trim().isEmpty());
					  
					report.setPlay_command(commands);
					logger.info("GetReportsm"+username+role);
					report.setReport(Service.findByUsername(username,role));
					return report;
				}

				

	}
	
	

	@RequestMapping(value="/addStatus", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	public void insert(@RequestBody Reports reports)
	{
		Service.add(reports);
	}
	
	@RequestMapping(value="/ropen/{username}/role/{role}",  method = RequestMethod.GET)
	public ResponseReport ropen(@PathVariable("username") String username,@PathVariable("role") String role)
	{
		ResponseReport report = new ResponseReport();

		Service.addOpen(username, role);
		
		return report;
	}
	
	@RequestMapping(value="/rclose/{username}/role/{role}",  method = RequestMethod.GET)
	public ResponseReport rclose(@PathVariable("username") String username,@PathVariable("role") String role)
	{
		ResponseReport report = new ResponseReport();
		Service.addclose(username, role);
		return report;
	}

	@RequestMapping(value="/rcloseopen",  method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseReport rcloseopen(@RequestBody Reports reports)
	{	
		ResponseReport report = new ResponseReport();
	    System.out.println("In rcloseopen method");
		Service.addOpenClose(reports.getCommand(), reports.getSm(),reports.getLc_name());
		return report;
	}
	@RequestMapping(value = "/getReportstatus/{username}/{gateid}/{status}", method = RequestMethod.GET)
	public Boolean getReportstatus(@PathVariable("username") String username,@PathVariable("gateid") String gateid,@PathVariable("status") String status)
	{
		 Service.getReportstatus(username,gateid,status);
		 
		 return true;
		
	}
	
	/**
	 * Unified endpoint for failsafe check and acknowledgement
	 * GET: Check for unacknowledged failsafe reports (returns formatted messages with reportId if exists for buzzing)
	 * POST: Acknowledge a failsafe report
	 */
	@RequestMapping(value = "/failsafe", method = RequestMethod.GET)
	public com.jsinfotech.Domain.FailsafeResponse getFailsafeReport(@RequestParam(name = "username", required = false) String username) {
		try {
			if (username == null || username.isEmpty()) {
				logger.warn("getFailsafeReport called without username");
				return new com.jsinfotech.Domain.FailsafeResponse();
			}
			
			com.jsinfotech.Domain.FailsafeResponse response = Service.getFailsafeResponseData(username);
			logger.info("Failsafe report data retrieved for user: {} ({} report IDs, {} play commands)", 
				username, response.getReportid() != null ? response.getReportid().size() : 0, 
				response.getPlay_command() != null ? response.getPlay_command().size() : 0);
			return response;
		} catch (Exception e) {
			logger.error("Error getting failsafe report for user: {}", username, e);
			com.jsinfotech.Domain.FailsafeResponse response = new com.jsinfotech.Domain.FailsafeResponse();
			response.setPlay_command(new java.util.ArrayList<>());
			response.setReportid(new java.util.ArrayList<>());
			return response;
		}
	}
	
	@RequestMapping(value = "/failsafe", method = RequestMethod.POST)
	public ResponseReport acknowledgeFailsafe(@RequestParam(name = "reportId") Integer reportId) {
		ResponseReport response = new ResponseReport();
		try {
			if (reportId == null) {
				logger.warn("acknowledgeFailsafe called without reportId");
				return response;
			}
			
			java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("HH:mm");
			String ackn = formatter.format(new java.util.Date());
			
			boolean success = Service.updateFailsafeAck(reportId, ackn);
			if (success) {
				logger.info("Successfully acknowledged failsafe report: {}", reportId);
			} else {
				logger.warn("Failed to acknowledge failsafe report: {}", reportId);
			}
		} catch (Exception e) {
			logger.error("Error acknowledging failsafe report: {}", reportId, e);
		}
		return response;
	}
	
	
	
}
