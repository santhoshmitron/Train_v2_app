package com.jsinfotech.Domain;

public class GateManStationMaster {
    
    private String first_name;
    private String phone;
    private String roles;
    private String username;
    
    public GateManStationMaster() {
    }
    
    public GateManStationMaster(String first_name, String phone, String roles, String username) {
        this.first_name = first_name;
        this.phone = phone;
        this.roles = roles;
        this.username = username;
    }
    
    public String getFirst_name() {
        return first_name;
    }
    
    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getRoles() {
        return roles;
    }
    
    public void setRoles(String roles) {
        this.roles = roles;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}
