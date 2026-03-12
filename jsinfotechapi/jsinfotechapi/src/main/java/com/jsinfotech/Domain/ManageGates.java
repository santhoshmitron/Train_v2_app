package com.jsinfotech.Domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ManageGates {

	
	private int id;

	private String gateNum;
	
	@JsonIgnore
	private String boom1Id;
	
	@JsonIgnore
	private String boom2Id;
	
	@JsonIgnore
	private String handle;
	
	private String SM;
	private String GM;
	private String status;
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date added_on;
	
	@JsonIgnore
	private String bs1Go;
	
	@JsonIgnore
	private String bs1Gc;
	
	@JsonIgnore
	private String lsGo;
	
	@JsonIgnore
	private String lsGc;
	private String bs1Status;
	private String leverStatus;
	private String bs2Status;
	private String ltStatus;
	
	// Report ID and PN fields for GM API response
	private String reportId;
	private String pn;
	
	// Boolean status fields for API response
	private Boolean boomOne;
	private Boolean boomTwo;
	private Boolean boomLock;
	private Boolean leverCloser;
	
	// Boom_Lock status field (healthy/unhealthy)
	private String boom_lock_play_commad;
	
    public Boolean getIs_failsafe() {
        return is_failsafe;
    }

    public void setIs_failsafe(Boolean is_failsafe) {
        this.is_failsafe = is_failsafe;
    }

    public String getGate_start_time() {
        return gate_start_time;
    }

    public void setGate_start_time(String gate_start_time) {
        this.gate_start_time = gate_start_time;
    }

    public String getGate_end_time() {
        return gate_end_time;
    }

    public void setGate_end_time(String gate_end_time) {
        this.gate_end_time = gate_end_time;
    }

    private Boolean is_failsafe;
    private String gate_start_time;
    private String gate_end_time;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getGateNum() {
		return gateNum;
	}
	public void setGateNum(String gateNum) {
		this.gateNum = gateNum;
	}
	public String getBoom1Id() {
		return boom1Id;
	}
	public void setBoom1Id(String boom1Id) {
		this.boom1Id = boom1Id;
	}
	public String getBoom2Id() {
		return boom2Id;
	}
	public void setBoom2Id(String boom2Id) {
		this.boom2Id = boom2Id;
	}
	public String getHandle() {
		return handle;
	}
	public void setHandle(String handle) {
		this.handle = handle;
	}
	public String getSM() {
		return SM;
	}
	public void setSM(String sM) {
		SM = sM;
	}
	public String getGM() {
		return GM;
	}
	public void setGM(String gM) {
		GM = gM;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	
	public String getBs1Go() {
		return bs1Go;
	}
	public Date getAdded_on() {
		return added_on;
	}
	public void setAdded_on(Date added_on) {
		this.added_on = added_on;
	}
	public void setBs1Go(String bs1Go) {
		this.bs1Go = bs1Go;
	}
	public String getBs1Gc() {
		return bs1Gc;
	}
	public void setBs1Gc(String bs1Gc) {
		this.bs1Gc = bs1Gc;
	}
	public String getLsGo() {
		return lsGo;
	}
	public void setLsGo(String lsGo) {
		this.lsGo = lsGo;
	}
	public String getLsGc() {
		return lsGc;
	}
	public void setLsGc(String lsGc) {
		this.lsGc = lsGc;
	}
	public String getBs1Status() {
		return bs1Status;
	}
	public void setBs1Status(String bs1Status) {
		this.bs1Status = bs1Status;
	}
	@Override
	public String toString() {
		return "ManageGates [id=" + id + ", gateNum=" + gateNum + ", boom1Id=" + boom1Id + ", boom2Id=" + boom2Id + ", handle=" + handle
				+ ", SM=" + SM + ", GM=" + GM + ", status=" + status + ", added_on=" + added_on + ", bs1Go=" + bs1Go + ", bs1Gc="
				+ bs1Gc + ", lsGo=" + lsGo + ", lsGc=" + lsGc + ", bs1Status=" + bs1Status + ", leverStatus=" + leverStatus
				+ ", bs2Status=" + bs2Status + ", ltStatus=" + ltStatus + "]";
	}
	public String getLeverStatus() {
		return leverStatus;
	}
	public void setLeverStatus(String leverStatus) {
		this.leverStatus = leverStatus;
	}
	
	public String getBs2Status() {
		return bs2Status;
	}
	
	public void setBs2Status(String bs2Status) {
		this.bs2Status = bs2Status;
	}
	
	public String getLtStatus() {
		return ltStatus;
	}
	
	public void setLtStatus(String ltStatus) {
		this.ltStatus = ltStatus;
	}
	
	public String getReportId() {
		return reportId;
	}
	
	public void setReportId(String reportId) {
		this.reportId = reportId;
	}
	
	@JsonProperty("gm_pn")
	public String getPn() {
		return pn;
	}
	
	public void setPn(String pn) {
		this.pn = pn;
	}
	
	public Boolean getBoomOne() {
		return boomOne;
	}
	
	public void setBoomOne(Boolean boomOne) {
		this.boomOne = boomOne;
	}
	
	public Boolean getBoomTwo() {
		return boomTwo;
	}
	
	public void setBoomTwo(Boolean boomTwo) {
		this.boomTwo = boomTwo;
	}
	
	public Boolean getBoomLock() {
		return boomLock;
	}
	
	public void setBoomLock(Boolean boomLock) {
		this.boomLock = boomLock;
	}
	
	public Boolean getLeverCloser() {
		return leverCloser;
	}
	
	public void setLeverCloser(Boolean leverCloser) {
		this.leverCloser = leverCloser;
	}

	public String getBoom_lock_play_commad() {
		return boom_lock_play_commad;
	}
	
	public void setBoom_lock_play_commad(String boom_lock_play_commad) {
		this.boom_lock_play_commad = boom_lock_play_commad;
	}




}
