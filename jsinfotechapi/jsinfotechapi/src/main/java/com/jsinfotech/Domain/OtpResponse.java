package com.jsinfotech.Domain;

public class OtpResponse {
	
	private int status;
	private String message;
	private String otp;

	public OtpResponse() {
	}

	public OtpResponse(int status, String message, String otp) {
		this.status = status;
		this.message = message;
		this.otp = otp;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}
}
