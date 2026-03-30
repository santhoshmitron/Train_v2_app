package com.jsinfotech.Domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;


public class Reports {


	private int id;
	private String tn;
	private String pn;
	private String tn_time;
	private String command;
	private String wer;
	private String sm;
	private String gm;
	private String lc;
	private String lc_name;
	private String lc_status;
	private String lc_lock_time;
	private String lc_pin;
	private  String lc_pin_time;
	public String getPlay_command() {
		return play_command;
	}
	public void setPlay_command(String play_command) {
		this.play_command = play_command;
	}
	private String play_command;
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private String from;
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private String to;



	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	private String ackn;
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="UTC")
	private Date added_on;
	private String lc_open_time;
	private String redy;
	private String boomLock;
	private Date todayTime;
	public Date getTodayTime() {
		return todayTime;
	}
	public void setTodayTime(Date todayTime) {
		this.todayTime = todayTime;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getTn() {
		return tn;
	}
	public void setTn(String tn) {
		this.tn = tn;
	}
	public String getPn() {
		return pn;
	}
	public void setPn(String pn) {
		this.pn = pn;
	}
	public String getTn_time() {
		return tn_time;
	}
	public void setTn_time(String tn_time) {
		this.tn_time = tn_time;
	}
	public Date getAdded_on() {
		return added_on;
	}

	public void setAdded_on(Date added_on) {
		this.added_on = added_on;
	}

	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public String getWer() {
		return wer;
	}
	public void setWer(String wer) {
		this.wer = wer;
	}
	public String getSm() {
		return sm;
	}
	public void setSm(String sm) {
		this.sm = sm;
	}
	public String getGm() {
		return gm;
	}
	public void setGm(String gm) {
		this.gm = gm;
	}
	public String getLc() {
		return lc;
	}
	public void setLc(String lc) {
		this.lc = lc;
	}
	public String getLc_name() {
		return lc_name;
	}
	public void setLc_name(String lc_name) {
		this.lc_name = lc_name;
	}
	public String getLc_status() {
		return lc_status;
	}
	public void setLc_status(String lc_status) {
		this.lc_status = lc_status;
	}
	public String getLc_lock_time() {
		return lc_lock_time;
	}
	public void setLc_lock_time(String lc_lock_time) {
		this.lc_lock_time = lc_lock_time;
	}
	public String getLc_pin() {
		return lc_pin;
	}
	public void setLc_pin(String lc_pin) {
		this.lc_pin = lc_pin;
	}
	public String getLc_pin_time() {
		return lc_pin_time;
	}
	public void setLc_pin_time(String lc_pin_time) {
		this.lc_pin_time = lc_pin_time;
	}
	public String getAckn() {
		return ackn;
	}
	public void setAckn(String ackn) {
		this.ackn = ackn;
	}


	public String getLc_open_time() {
		return lc_open_time;
	}
	public void setLc_open_time(String lc_open_time) {
		this.lc_open_time = lc_open_time;
	}
	public String getRedy() {
		return redy;
	}
	public void setRedy(String redy) {
		this.redy = redy;
	}
	public String getBoomLock() {
		return boomLock;
	}
	public void setBoomLock(String boomLock) {
		this.boomLock = boomLock;
	}



}
