package com.jsinfotech.Domain;

public class LogoutRequest {
	
	private String mobilenumber;
	private String userId;

	public LogoutRequest() {
	}

	public LogoutRequest(String mobilenumber, String userId) {
		this.mobilenumber = mobilenumber;
		this.userId = userId;
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
}
