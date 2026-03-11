package com.jsinfotech.Domain;

import java.util.List;

public class ResponseReport {
	
	public List<Reports> getReport() {
		return report;
	}

	public void setReport(List<Reports> report) {
		this.report = report;
	}

	public List<String> getPlay_command() {
		return play_command;
	}

	public void setPlay_command(List<String> play_command) {
		this.play_command = play_command;
	}

	List<Reports> report;
	
	List<String> play_command;

}
