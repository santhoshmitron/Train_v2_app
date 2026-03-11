package com.jsinfotech.Domain;

import java.io.Serializable;

public class J_Message implements Serializable{

   /** public J_Message(String username, String gateId,String close) {
        this.close = close;
        this.gateId=gateId;
        this.username=username;
    }**/
	
	

    public J_Message(String boom1Id,Boolean failsafe_method,Boolean isfailsafe, String username,String close) {
        this.close = close;
        this.boom1Id=boom1Id;
        this.username=username;
        this.failsafe_method = failsafe_method;
        this.isfailsafe = isfailsafe;
    }
    public J_Message() {
	// TODO Auto-generated constructor stub
     }
	private String boom1Id ;

    public Boolean getFailsafe_method() {
        return failsafe_method;
    }

    public void setFailsafe_method(Boolean failsafe_method) {
        this.failsafe_method = failsafe_method;
    }

    private Boolean failsafe_method = false;


    public Boolean getIsfailsafe() {
        return isfailsafe;
    }

    public void setIsfailsafe(Boolean isfailsafe) {
        this.isfailsafe = isfailsafe;
    }

    private Boolean isfailsafe=false;
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;


    public String getBoom1Id() {
        return boom1Id;
    }

    public void setBoom1Id(String boom1Id) {
        this.boom1Id = boom1Id;
    }

    public String getClose() {
        return close;
    }

    public void setClose(String close) {
        this.close = close;
    }

    private String close;

/**	@Override
	public String toString() {
		return "{" +"gateId:" + gateId + ", failsafe_method:" + failsafe_method + ", isfailsafe:" + isfailsafe
				+ ", username:" + username + ", close:" + close + "}";
	}**/
    
    @Override
    public String toString() {
        return "{" + "boom1Id:" + boom1Id+ ", failsafe_method:" + failsafe_method+", isfailsafe:" + isfailsafe+", close:" + close + ", username:" + username + '}';
    }



   /* @Override
    public String toString() {
        return "{" + "gateId:" + gateId+ ", close:" + close + ", username:" + username + '}';
    }*/
}
