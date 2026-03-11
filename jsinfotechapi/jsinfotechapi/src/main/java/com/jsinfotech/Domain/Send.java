package com.jsinfotech.Domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Send {
	private String tn;
	private String user;
	private String pn;
	private String wer;
	private String roles;
	private String gateId;
	private String gateName;
	private String gate_status;
	private String GM;
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date added_on;
	
	public Date getAdded_on() {
		return added_on;
	}
	public void setAdded_on(Date added_on) {
		this.added_on = added_on;
	}
	public String getGateId() {
		return gateId;
	}
	public void setGateId(String gateId) {
		this.gateId = gateId;
	}
	public String getGateName() {
		return gateName;
	}
	public void setGateName(String gateName) {
		this.gateName = gateName;
	}
	public String getGate_status() {
		return gate_status;
	}
	public void setGate_status(String gate_status) {
		this.gate_status = gate_status;
	}
	public String getGM() {
		return GM;
	}
	public void setGM(String gM) {
		GM = gM;
	}
	
	public String getTn() {
		return tn;
	}
	public void setTn(String tn) {
		this.tn = tn;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPn() {
		return pn;
	}
	public void setPn(String pn) {
		this.pn = pn;
	}
	public String getWer() {
		return wer;
	}
	public void setWer(String wer) {
		this.wer = wer;
	}
	public String getRoles() {
		return roles;
	}
	public void setRoles(String roles) {
		this.roles = roles;
	}
	
	
	
}
