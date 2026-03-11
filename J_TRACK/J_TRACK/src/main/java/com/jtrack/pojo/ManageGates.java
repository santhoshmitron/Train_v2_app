package com.jtrack.pojo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ManageGates {


    private int id;

    private String gateNum;
    private String boom1Id;
    private String boom2Id;
    private String handle;
    private String SM;
    private String GM;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date added_on;

    private String bs1Go;
    private String bs1Gc;
    private String lsGo;
    private String lsGc;
    private String bs1Status;
    private String leverStatus;
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

/*    @Override
    public String toString() {
        return "ManageGates [id=" + id + ", gateName=" + gateName + ", gateId=" + gateId + ", handle=" + handle
                + ", SM=" + SM + ", GM=" + GM + ", status=" + status + ", added_on=" + added_on + ", go=" + go + ", gc="
                + gc + ", ho=" + ho + ", hc=" + hc + ", gate_status=" + gate_status + ", handle_status=" + handle_status
                + "]";
    }*/

    @Override
    public String toString() {
        return "ManageGates{" +
                "id=" + id +
                ", gateNum='" + gateNum + '\'' +
                ", boom1Id='" + boom1Id + '\'' +
                ", boom2Id='" + boom2Id + '\'' +
                ", handle='" + handle + '\'' +
                ", SM='" + SM + '\'' +
                ", GM='" + GM + '\'' +
                ", status='" + status + '\'' +
                ", added_on=" + added_on +
                ", bs1Go='" + bs1Go + '\'' +
                ", bs1Gc='" + bs1Gc + '\'' +
                ", lsGo='" + lsGo + '\'' +
                ", lsGc='" + lsGc + '\'' +
                ", bs1Status='" + bs1Status + '\'' +
                ", leverStatus='" + leverStatus + '\'' +
                ", is_failsafe=" + is_failsafe +
                ", gate_start_time='" + gate_start_time + '\'' +
                ", gate_end_time='" + gate_end_time + '\'' +
                '}';
    }

    public String getLeverStatus() {
        return leverStatus;
    }

    public void setLeverStatus(String leverStatus) {
        this.leverStatus = leverStatus;
    }

}
