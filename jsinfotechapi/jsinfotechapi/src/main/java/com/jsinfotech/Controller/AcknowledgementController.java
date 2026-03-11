package com.jsinfotech.Controller;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.Domain.Reports;
import com.jsinfotech.Domain.Response;
import com.jsinfotech.Service.AcknowledgementService;

@RestController
@RequestMapping("/jsinfo/ACK")
public class AcknowledgementController {

	@Autowired
	AcknowledgementService ackservice;
	@RequestMapping(value="/Getreports", method = RequestMethod.GET)
	public List<Reports> ackReports(@RequestParam(name = "userid") String userid )
	{
		return ackservice.getReports(userid);

	}
	
	@RequestMapping(value="/Getv3reports", method = RequestMethod.GET)
	public Reports ackV3Reports(@RequestParam(name = "userid") String userid,@RequestParam(name = "gateId") String gateId )
	{
		return ackservice.getAckandPN(userid, gateId);

	}

	@RequestMapping(value="/update", method = RequestMethod.GET)
	public Response ackUpdate(@RequestParam(name = "id") Integer id )
	{
		Response req = new Response();

		if (ackservice.ackCommand(id)) {
			req.setStatus("Success");
			req.setMessage("Successful");
			int a = generateThreeDigitNoZero();
			req.setId(id.toString());
			req.setNumber(a);
			
			return req;
		}
		else
		{
			req.setStatus("Not Success");
			req.setMessage("Not Successful");
			return req;

		}

	}

	private static int generateThreeDigitNoZero() {
		Random r = new Random();
		int result;
		do {
			result = 111 + r.nextInt(889);
		} while (String.valueOf(result).indexOf('0') >= 0);
		return result;
	}
	
	@RequestMapping(value="/sendpn", method = RequestMethod.GET)
	public Response sendpn(@RequestParam(name = "id") Integer id,@RequestParam(name = "pn") Integer pn  )
	{
		Response req = new Response();

		// Validate PN: must be 3 digits, digits 1-9 only (no zero anywhere)
		if (pn == null || !isValidThreeDigitNoZeroPN(pn)) {
			req.setStatus("failed");
			req.setMessage("Invalid PN. PN must be 3 digits (111-999) and must not contain 0.");
			return req;
		}

		if (ackservice.sendpn(id, pn)) {
			req.setStatus("success");
			req.setMessage("Successful");
			return req;
		}
		else
		{
			req.setStatus("failed");
			req.setMessage("Not Successful");
			return req;

		}

	}

	private static boolean isValidThreeDigitNoZeroPN(int pn) {
		if (pn < 111 || pn > 999) return false;
		return String.valueOf(pn).indexOf('0') < 0;
	}
	
}
