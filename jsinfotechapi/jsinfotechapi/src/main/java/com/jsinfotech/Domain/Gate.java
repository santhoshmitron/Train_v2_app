package com.jsinfotech.Domain;

public class Gate {

    public Gate(String gateNum, String boom1_id, String g_value,String l_value) {
        this.gateNum = gateNum;
        this.boom1Id = boom1_id;
        this.g_value = g_value;
        this.l_value = l_value;
    }

    private String gateNum;
    private String handle;
    private String lever;
	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	private String date;


    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    private String batch;

    public String getL_value() {
        return l_value;
    }

    public void setL_value(String l_value) {
        this.l_value = l_value;
    }

    public String getG_value() {
        return g_value;
    }

    public void setG_value(String g_value) {
        this.g_value = g_value;
    }

    private String l_value;
    private String g_value;


    public String getBoom1Id() {
        return boom1Id;
    }

    public void setBoom1Id(String boom1Id) {
        this.boom1Id = boom1Id;
    }

    private String boom1Id;

    public String getGateNum() {
        return gateNum;
    }

    public void setGateNum(String gateNum) {
        this.gateNum = gateNum;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getLever() {
        return lever;
    }

    public void setLever(String lever) {
        this.lever = lever;
    }

    @Override
    public String toString() {
        return "Gate{" +
                "gateNum='" + gateNum + '\'' +
                ", handle='" + handle + '\'' +
                ", lever='" + lever + '\'' +
                ", l_value='" + l_value + '\'' +
                ", g_value='" + g_value + '\'' +
                ", boom1Id='" + boom1Id + '\'' +
                '}';
    }
}
