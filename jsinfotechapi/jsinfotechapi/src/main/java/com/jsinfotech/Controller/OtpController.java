package com.jsinfotech.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.Domain.Contact;
import com.jsinfotech.Domain.LogoutRequest;
import com.jsinfotech.Domain.LogoutResponse;
import com.jsinfotech.Domain.OtpRequest;
import com.jsinfotech.Domain.OtpResponse;
import com.jsinfotech.Domain.OtpVerifyRequest;
import com.jsinfotech.Domain.OtpVerifyResponse;
import com.jsinfotech.Service.ContactService;
import com.jsinfotech.Service.LoginLogService;
import com.jsinfotech.Service.SmsService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OtpController {

	@Autowired
	private ContactService contactService;

	@Autowired
	private SmsService smsService;

	@Autowired
	private LoginLogService loginLogService;

	@RequestMapping(value = "/generateotp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OtpResponse generateOtp(@RequestBody OtpRequest request) {
		OtpResponse response = new OtpResponse();

		try {
			// Validate request
			if (request == null || request.getMobilenumber() == null || request.getUserId() == null) {
				response.setStatus(400);
				response.setMessage("Mobile number and userId are required");
				response.setOtp(null);
				return response;
			}

			String phoneNumber = request.getMobilenumber();
			String userId = request.getUserId();

			// Check if phone number exists in contacts table
			if (!contactService.phoneNumberExists(phoneNumber)) {
				response.setStatus(400);
				response.setMessage("Number not exist please contact administrator");
				response.setOtp(null);
				return response;
			}

			// Send OTP request to SMS gateway (gateway generates OTP itself)
			boolean smsSent = smsService.sendOtp(phoneNumber);
			if (!smsSent) {
				response.setStatus(400);
				response.setMessage("Failed to send OTP. Please try again");
				response.setOtp(null);
				return response;
			}

			// OTP sent successfully (SMS gateway generated it)
			response.setStatus(200);
			response.setMessage("Otp sent successfully");
			response.setOtp(null); // Don't return OTP - it's sent to mobile
			return response;

		} catch (Exception e) {
			response.setStatus(400);
			response.setMessage("Error: " + e.getMessage());
			response.setOtp(null);
			return response;
		}
	}

	@RequestMapping(value = "/verifyotp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OtpVerifyResponse verifyOtp(@RequestBody OtpVerifyRequest request) {
		OtpVerifyResponse response = new OtpVerifyResponse();

		try {
			// Validate request
			if (request == null || request.getMobilenumber() == null || request.getUserId() == null
					|| request.getOtp() == null) {
				response.setStatus(400);
				response.setMessage("Mobile number, userId, and OTP are required");
				response.setMobilenumber(null);
				response.setUserName(null);
				return response;
			}

			String phoneNumber = request.getMobilenumber();
			String userId = request.getUserId();
			String otp = request.getOtp();

			// Verify OTP using SMS gateway API
			boolean otpValid = smsService.verifyOtp(phoneNumber, otp);
			if (!otpValid) {
				response.setStatus(400);
				response.setMessage("Invalid or expired OTP");
				response.setMobilenumber(null);
				response.setUserName(null);
				return response;
			}

			// Get contact name from contacts table
			Contact contact = contactService.getContactByPhoneNumber(phoneNumber);
			if (contact == null) {
				response.setStatus(400);
				response.setMessage("Contact not found");
				response.setMobilenumber(null);
				response.setUserName(null);
				return response;
			}

			// Create login log entry
			loginLogService.createLoginLog(userId, phoneNumber);

			response.setStatus(200);
			response.setMessage("Otp verified successfully");
			response.setMobilenumber(phoneNumber);
			response.setUserName(contact.getName());
			return response;

		} catch (Exception e) {
			response.setStatus(400);
			response.setMessage("Error: " + e.getMessage());
			response.setMobilenumber(null);
			response.setUserName(null);
			return response;
		}
	}

	@RequestMapping(value = "/resendotp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public OtpResponse resendOtp(@RequestBody OtpRequest request) {
		OtpResponse response = new OtpResponse();

		try {
			// Validate request
			if (request == null || request.getMobilenumber() == null || request.getUserId() == null) {
				response.setStatus(400);
				response.setMessage("Mobile number and userId are required");
				response.setOtp(null);
				return response;
			}

			String phoneNumber = request.getMobilenumber();
			String userId = request.getUserId();

			// Check if phone number exists in contacts table
			if (!contactService.phoneNumberExists(phoneNumber)) {
				response.setStatus(400);
				response.setMessage("Number not exist please contact administrator");
				response.setOtp(null);
				return response;
			}

			// Resend OTP via SMS gateway (gateway generates new OTP)
			boolean smsSent = smsService.sendOtp(phoneNumber);
			if (!smsSent) {
				response.setStatus(400);
				response.setMessage("Failed to send OTP. Please try again");
				response.setOtp(null);
				return response;
			}

			// OTP re-sent successfully
			response.setStatus(200);
			response.setMessage("Otp re-sent successfully");
			response.setOtp(null); // Don't return OTP - it's sent to mobile
			return response;

		} catch (Exception e) {
			response.setStatus(400);
			response.setMessage("Error: " + e.getMessage());
			response.setOtp(null);
			return response;
		}
	}

	@RequestMapping(value = "/logoutUser", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public LogoutResponse logoutUser(@RequestBody LogoutRequest request) {
		LogoutResponse response = new LogoutResponse();

		try {
			// Validate request
			if (request == null || request.getMobilenumber() == null || request.getUserId() == null) {
				response.setStatus(400);
				response.setMessage("Mobile number and userId are required");
				return response;
			}

			String phoneNumber = request.getMobilenumber();
			String userId = request.getUserId();

			// Update logout time and status
			loginLogService.updateLogout(userId, phoneNumber);

			response.setStatus(200);
			response.setMessage("Successfully logout");
			return response;

		} catch (Exception e) {
			response.setStatus(400);
			response.setMessage("Error: " + e.getMessage());
			return response;
		}
	}
}
