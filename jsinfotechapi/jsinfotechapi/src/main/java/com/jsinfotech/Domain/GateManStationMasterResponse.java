package com.jsinfotech.Domain;

public class GateManStationMasterResponse {
    
    private int Code;
    private String Message;
    private GateManStationMaster Details;
    
    public GateManStationMasterResponse() {
    }
    
    public GateManStationMasterResponse(int code, String message, GateManStationMaster details) {
        this.Code = code;
        this.Message = message;
        this.Details = details;
    }
    
    public int getCode() {
        return Code;
    }
    
    public void setCode(int code) {
        this.Code = code;
    }
    
    public String getMessage() {
        return Message;
    }
    
    public void setMessage(String message) {
        this.Message = message;
    }
    
    public GateManStationMaster getDetails() {
        return Details;
    }
    
    public void setDetails(GateManStationMaster details) {
        this.Details = details;
    }
}
