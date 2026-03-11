package com.jsinfotech.Domain;

public class OtpVerifyRequest {
	
	private String mobilenumber;
	private String userId;
	private String otp;

	public OtpVerifyRequest() {
	}

	public OtpVerifyRequest(String mobilenumber, String userId, String otp) {
		this.mobilenumber = mobilenumber;
		this.userId = userId;
		this.otp = otp;
	}

	public String getMobilenumber() {
		return mobilenumber;
	}

	public void setMobilenumber(String mobilenumber) {
		this.mobilenumber = mobilenumber;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}
}
