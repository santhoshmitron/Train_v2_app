package com.jsinfotech.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.Domain.GateManStationMaster;
import com.jsinfotech.Domain.GateManStationMasterResponse;
import com.jsinfotech.Domain.Helpline;
import com.jsinfotech.Domain.HelplineResponse;
import com.jsinfotech.Domain.Login;
import com.jsinfotech.Service.LoginService;


@RestController
@RequestMapping("/jsinfo")
public class LoginController {
	
	@Autowired
	LoginService loginservice;
	@RequestMapping(value="/login", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*")
	public Login insert(@RequestBody Login login)
	{
		return loginservice.checkLogin(login);
		
	}
	
	@RequestMapping(value="/helpline", method = RequestMethod.GET)
	@CrossOrigin(origins = "*")
	public HelplineResponse getHelpline()
	{
		Helpline helpline = loginservice.getHelplinePhones();
		if (helpline != null) {
			return new HelplineResponse(200, "Helpline numbers retrieved", helpline);
		} else {
			return new HelplineResponse(201, "No helpline numbers set", null);
		}
	}
	
	@RequestMapping(value="/submit-gateman-stationmaster", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*")
	public GateManStationMasterResponse submitGateManStationMaster(@RequestBody GateManStationMaster gateManStationMaster)
	{
		// Validate required fields
		if (gateManStationMaster.getFirst_name() == null || gateManStationMaster.getFirst_name().trim().isEmpty() ||
			gateManStationMaster.getPhone() == null || gateManStationMaster.getPhone().trim().isEmpty() ||
			gateManStationMaster.getRoles() == null || gateManStationMaster.getRoles().trim().isEmpty() ||
			gateManStationMaster.getUsername() == null || gateManStationMaster.getUsername().trim().isEmpty()) {
			return new GateManStationMasterResponse(400, "Missing required fields", null);
		}
		
		boolean success = loginservice.submitGateManOrStationMaster(gateManStationMaster);
		if (success) {
			return new GateManStationMasterResponse(200, "Successfully submitted", null);
		} else {
			return new GateManStationMasterResponse(500, "Failed to submit", null);
		}
	}
	
	@RequestMapping(value="/get-gateman-stationmaster", method = RequestMethod.GET)
	@CrossOrigin(origins = "*")
	public GateManStationMasterResponse getGateManStationMaster(@RequestParam String roles, @RequestParam String username)
	{
		GateManStationMaster details = loginservice.getGateManOrStationMasterDetails(roles, username);
		if (details != null) {
			return new GateManStationMasterResponse(200, "Details available", details);
		} else {
			return new GateManStationMasterResponse(201, "No details available", null);
		}
	}

}
