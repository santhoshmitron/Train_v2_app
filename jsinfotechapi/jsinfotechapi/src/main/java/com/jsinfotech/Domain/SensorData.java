package com.jsinfotech.Domain;

import java.util.Date;

/**
 * Domain object for sensor data received from devices
 * Used for Kafka/Elasticsearch publishing
 */
public class SensorData {
    
    private String gateId;          // field1: e.g., "E20-750BS1"
    private String field2;           // BS1/BS2/LT1 value
    private String field3;           // LS/LT2 value
    private String field4;          // Pass/Batch/LT_STATUS
    private String field5;          // BS2 value (optional)
    private String field6;          // LT value (optional)
    private String sensorType;      // BS1, BS2, LS, LT
    private String timestamp;        // HH:mm:ss format
    private Date receivedAt;        // Full timestamp
    private String clientIp;        // Device IP address
    
    public SensorData() {
        this.receivedAt = new Date();
    }
    
    public SensorData(String gateId, String field2, String field3, String field4, 
                     String field5, String field6, String sensorType, String timestamp, String clientIp) {
        this.gateId = gateId;
        this.field2 = field2;
        this.field3 = field3;
        this.field4 = field4;
        this.field5 = field5;
        this.field6 = field6;
        this.sensorType = sensorType;
        this.timestamp = timestamp;
        this.clientIp = clientIp;
        this.receivedAt = new Date();
    }
    
    // Getters and Setters
    public String getGateId() {
        return gateId;
    }
    
    public void setGateId(String gateId) {
        this.gateId = gateId;
    }
    
    public String getField2() {
        return field2;
    }
    
    public void setField2(String field2) {
        this.field2 = field2;
    }
    
    public String getField3() {
        return field3;
    }
    
    public void setField3(String field3) {
        this.field3 = field3;
    }
    
    public String getField4() {
        return field4;
    }
    
    public void setField4(String field4) {
        this.field4 = field4;
    }
    
    public String getField5() {
        return field5;
    }
    
    public void setField5(String field5) {
        this.field5 = field5;
    }
    
    public String getField6() {
        return field6;
    }
    
    public void setField6(String field6) {
        this.field6 = field6;
    }
    
    public String getSensorType() {
        return sensorType;
    }
    
    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public Date getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    @Override
    public String toString() {
        return "SensorData{" +
                "gateId='" + gateId + '\'' +
                ", sensorType='" + sensorType + '\'' +
                ", field2='" + field2 + '\'' +
                ", field3='" + field3 + '\'' +
                ", field4='" + field4 + '\'' +
                ", field5='" + field5 + '\'' +
                ", field6='" + field6 + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
