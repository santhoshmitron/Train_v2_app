package com.jsinfotech.Domain;

import java.util.List;

public class FailsafeResponse {
	
	private List<Integer> reportid;
	
	private List<String> play_command;

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

}

