package com.jtrack.pojo;
import java.io.Serializable;
import java.util.Date;

public class API implements Serializable {
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
    private int id;
    private Date added_on;

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
}
