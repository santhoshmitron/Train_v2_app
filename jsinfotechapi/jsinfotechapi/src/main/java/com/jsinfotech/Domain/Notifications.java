package com.jsinfotech.Domain;

import java.util.Date;


public class Notifications {
	
	private String id;
	private String notification;
	private Date added_on;
	private String noti_to;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getNotification() {
		return notification;
	}
	public void setNotification(String notification) {
		this.notification = notification;
	}
	public Date getAdded_on() {
		return added_on;
	}
	public void setAdded_on(Date added_on) {
		this.added_on = added_on;
	}
	public String getNoti_to() {
		return noti_to;
	}
	public void setNoti_to(String noti_to) {
		this.noti_to = noti_to;
	}



}
