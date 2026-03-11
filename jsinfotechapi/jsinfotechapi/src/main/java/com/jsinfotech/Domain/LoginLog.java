package com.jsinfotech.Domain;

import java.util.Date;

public class LoginLog {
	
	private int id;
	private String userId;
	private String phoneNumber;
	private Date loginTime;
	private Date logoutTime;
	private boolean isLoggedOut;

	public LoginLog() {
	}

	public LoginLog(int id, String userId, String phoneNumber, Date loginTime, Date logoutTime, boolean isLoggedOut) {
		this.id = id;
		this.userId = userId;
		this.phoneNumber = phoneNumber;
		this.loginTime = loginTime;
		this.logoutTime = logoutTime;
		this.isLoggedOut = isLoggedOut;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public Date getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(Date loginTime) {
		this.loginTime = loginTime;
	}

	public Date getLogoutTime() {
		return logoutTime;
	}

	public void setLogoutTime(Date logoutTime) {
		this.logoutTime = logoutTime;
	}

	public boolean isLoggedOut() {
		return isLoggedOut;
	}

	public void setLoggedOut(boolean isLoggedOut) {
		this.isLoggedOut = isLoggedOut;
	}
}
