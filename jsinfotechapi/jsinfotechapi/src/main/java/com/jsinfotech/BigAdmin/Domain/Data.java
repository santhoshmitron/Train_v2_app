package com.jsinfotech.BigAdmin.Domain;

import java.util.Date;

public class Data {
	
	public String getLcname() {
		return lcname;
	}
	public void setLcname(String lcname) {
		this.lcname = lcname;
	}
	public String getFeild1() {
		return feild1;
	}
	public void setFeild1(String feild1) {
		this.feild1 = feild1;
	}
	public String getFeild2() {
		return feild2;
	}
	public void setFeild2(String feild2) {
		this.feild2 = feild2;
	}
	private String lcname;
	private String feild1;
	private String feild2;
	public Date getData() {
		return data;
	}
	public void setData(Date data) {
		this.data = data;
	}
	private Date data;
	

}

