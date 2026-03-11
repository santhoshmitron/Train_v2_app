package com.jsinfotech.Domain;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class API implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7405077898887260693L;
	
	private String gate;
	private String status;
	private String statuss;
	private String pass;
	private String bs1Go;
	private String bs1Gc;
	private int lsGo;
	private int lsGc;
	private  String lc_name;
	private String sm;
	private String gm;
	private String leverStatus;
	private String bs1Status;
	private String bs2Status;
	private String ltStatus;
	private String bs2Go;
	private String bs2Gc;
	private int id;
	private Date added_on;
	
	// Boolean fields for boom status
	private Boolean boomOne;      // BS1 - derived from BS1_STATUS
	private Boolean boomTwo;      // BS2 - derived from BS2_STATUS
	private Boolean boomLock;     // LT_status - derived from LT_STATUS
	private Boolean leverCloser;  // derived from LEVER_STATUS
	
	// Additional gate identification fields
	private String boom1Id;       // BOOM1_ID from database
	private String boom2Id;       // BOOM2_ID from database
	private String ltswId;        // LTSW_ID from database (nullable)
	private String handle;        // handle from database
	
	public Date getAdded_on() {
		return added_on;
	}
	public void setAdded_on(Date added_on) {
		this.added_on = added_on;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getLc_name() {
		return lc_name;
	}
	public void setLc_name(String lc_name) {
		this.lc_name = lc_name;
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
	public String getLeverStatus() {
		return leverStatus;
	}
	public void setLeverStatus(String leverStatus) {
		this.leverStatus = leverStatus;
	}
	public String getBs1Status() {
		return bs1Status;
	}
	public void setBs1Status(String bs1Status) {
		this.bs1Status = bs1Status;
	}
	public int getLsGo() {
		return lsGo;
	}
	public void setLsGo(int lsGo) {
		this.lsGo = lsGo;
	}
	public int getLsGc() {
		return lsGc;
	}
	public void setLsGc(int lsGc) {
		this.lsGc = lsGc;
	}
	
	public String getBs1Go() {
		return bs1Go;
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
	@JsonIgnore
	public String getGate() {
		return gate;
	}
	public void setGate(String gate) {
		this.gate = gate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getStatuss() {
		return statuss;
	}
	public void setStatuss(String statuss) {
		this.statuss = statuss;
	}
	public String getPass() {
		return pass;
	}
	public void setPass(String pass) {
		this.pass = pass;
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
	
	public String getBs2Go() {
		return bs2Go;
	}
	
	public void setBs2Go(String bs2Go) {
		this.bs2Go = bs2Go;
	}
	
	public String getBs2Gc() {
		return bs2Gc;
	}
	
	public void setBs2Gc(String bs2Gc) {
		this.bs2Gc = bs2Gc;
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

	public String getLtswId() {
		return ltswId;
	}

	public void setLtswId(String ltswId) {
		this.ltswId = ltswId;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

}