package com.jsinfotech.Domain;

public class HelplineResponse {
    
    private int Code;
    private String Message;
    private Helpline Details;
    private String desclaimer;
    
    public HelplineResponse() {
    }
    
    public HelplineResponse(int code, String message, Helpline details) {
        this.Code = code;
        this.Message = message;
        this.Details = details;
        this.desclaimer = "System provided is an aid and the staff need to observe the safety norms without any dilution on account of provision of this system.";
    }
    
    public HelplineResponse(int code, String message, Helpline details, String desclaimer) {
        this.Code = code;
        this.Message = message;
        this.Details = details;
        this.desclaimer = desclaimer;
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
    
    public Helpline getDetails() {
        return Details;
    }
    
    public void setDetails(Helpline details) {
        this.Details = details;
    }
    
    public String getDesclaimer() {
        return desclaimer;
    }
    
    public void setDesclaimer(String desclaimer) {
        this.desclaimer = desclaimer;
    }
}
