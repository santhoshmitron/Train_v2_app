package com.jsinfotech.Controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.Domain.Response;
import com.jsinfotech.Domain.Send;
import com.jsinfotech.Service.SendService;


@RestController
@RequestMapping("/jsinfo")
public class SendCommandController {
	
	private static final Logger logger = LogManager.getLogger(SendCommandController.class);
	
	@Autowired
	SendService sendservice;
	

	@RequestMapping(value="/send", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	public  Response insertsend(@RequestBody Send send) {
		Response req = new Response();
		try {
			if (send == null) {
				req.setStatus("Failed");
				req.setMessage("Request body is null");
				logger.error("Request body is null for /send endpoint");
				return req;
			}
			req.setStatus("Success");
			req.setMessage("Successful");
			// Send data and get auto-generated PNs (dash-separated string)
			String generatedPNs = sendservice.sendData1(send);
			if (generatedPNs != null && !generatedPNs.trim().isEmpty()) {
				req.setPns(generatedPNs);
				logger.info("Auto-generated PNs returned in response: {}", generatedPNs);
			}
			return req;
		} catch (Exception e) {
			logger.error("Error in insertsend endpoint", e);
			req.setStatus("Error");
			req.setMessage("Error processing send request: " + e.getMessage());
			return req;
		}
	}
	
	@RequestMapping(value="/cancel", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	public  Response updateCancel(@RequestBody Send send) {
		Response req = new Response();
		try {
			if (send == null) {
				req.setStatus("Failed");
				req.setMessage("Request body is null");
				logger.error("Request body is null for /cancel endpoint");
				return req;
			}
			req.setStatus("Success");
			req.setMessage("Successful");
			sendservice.cancelTrain(send);
			return req;
		} catch (Exception e) {
			logger.error("Error in updateCancel endpoint", e);
			req.setStatus("Error");
			req.setMessage("Error processing cancel request: " + e.getMessage());
			return req;
		}
	}
	
	@RequestMapping(value="/getPn", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public Response getPn(@RequestBody Send send) {
		Response req = new Response();
		try {
			if (send == null || send.getUser() == null || send.getUser().trim().isEmpty()) {
				req.setStatus("Failed");
				req.setMessage("Request body or user is null");
				logger.error("Request body or user is null for /getPn endpoint");
				return req;
			}
			
			// Get PNs for all gates under this SM
			String pns = sendservice.getPNsForSM(send.getUser());
			req.setPns(pns);
			req.setStatus("Success");
			req.setMessage("Successful");
			logger.info("Retrieved PNs for SM: {}: {}", send.getUser(), pns);
			return req;
		} catch (Exception e) {
			logger.error("Error in getPn endpoint", e);
			req.setStatus("Error");
			req.setMessage("Error retrieving PNs: " + e.getMessage());
			req.setPns("");
			return req;
		}
	}
	
	@RequestMapping(value="/clearline", method = RequestMethod.GET)
	public Response processCommand(@RequestParam(name = "field1") String field1, 
	                                @RequestParam(name = "field2") String field2) {
		Response req = new Response();
		try {
			// Validate required parameters
			if (field1 == null || field1.trim().isEmpty()) {
				req.setStatus("Failed");
				req.setMessage("field1 parameter is required and cannot be empty");
				logger.error("field1 parameter is missing or empty");
				return req;
			}
			
			if (field2 == null || field2.trim().isEmpty()) {
				req.setStatus("Failed");
				req.setMessage("field2 parameter is required and cannot be empty");
				logger.error("field2 parameter is missing or empty for SM: {}", field1);
				return req;
			}
			
			logger.info("Processing clearline command for SM: {}, field2: {}", field1, field2);
			boolean result = sendservice.processCommand(field1, field2);
			
			if (result) {
				req.setStatus("Success");
				req.setMessage("Command processed successfully");
			} else {
				req.setStatus("Failed");
				req.setMessage("Command processing failed. Please check logs for details.");
			}
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException in processCommand for field1: {}, field2: {}", field1, field2, e);
			req.setStatus("Failed");
			req.setMessage("Invalid parameter: " + e.getMessage());
		} catch (NullPointerException e) {
			logger.error("NullPointerException in processCommand for field1: {}, field2: {}", field1, field2, e);
			req.setStatus("Error");
			req.setMessage("Null pointer error: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error in processCommand for field1: {}, field2: {}", field1, field2, e);
			req.setStatus("Error");
			req.setMessage("Error processing command: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
		}
		return req;
	}
}
