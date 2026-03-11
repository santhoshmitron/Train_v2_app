package com.jsinfotech.Domain;

import java.util.List;

public class Response {
	


	private String message;
	
	private String status;
	
    public List<Integer> getNumber() {
		return number;
	}

	public void setNumber(List<Integer> number) {
		this.number = number;
	}
	
	/**
	 * Convenience method to set a single number (for backward compatibility)
	 */
	public void setNumber(int num) {
		if (this.number == null) {
			this.number = new java.util.ArrayList<>();
		}
		this.number.clear();
		this.number.add(num);
	}

	private List<Integer> number;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	private String id;
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	private List<Integer> reportid;
	
	private List<String> play_command;

	private String pns; // Auto-generated PNs (dash-separated, e.g., "123-456-789")

	public List<Integer> getReportid() {
		return reportid;
	}

	public void setReportid(List<Integer> reportid) {
		this.reportid = reportid;
	}

	public List<String> getPlay_command() {
		return play_command;
	}

	public void setPlay_command(List<String> play_command) {
		this.play_command = play_command;
	}
	
	public String getPns() {
		return pns;
	}
	
	public void setPns(String pns) {
		this.pns = pns;
	}

}
