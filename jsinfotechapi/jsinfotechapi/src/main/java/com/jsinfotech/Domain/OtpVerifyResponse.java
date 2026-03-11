package com.jsinfotech.Domain;

public class OtpVerifyResponse {
	
	private int status;
	private String message;
	private String mobilenumber;
	private String userName;

	public OtpVerifyResponse() {
	}

	public OtpVerifyResponse(int status, String message, String mobilenumber, String userName) {
		this.status = status;
		this.message = message;
		this.mobilenumber = mobilenumber;
		this.userName = userName;
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

	public String getMobilenumber() {
		return mobilenumber;
	}

	public void setMobilenumber(String mobilenumber) {
		this.mobilenumber = mobilenumber;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
