package com.jsinfotech.Service;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.API;

@Service
public class GateStatusService {

    private static final Logger logger = LogManager.getLogger(GateStatusService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate1;
    
    @Autowired
    private RedisUserRepository userRepository;
    
    /**
     * Check if gate is closed based on BS1_STATUS, BS2_STATUS, and LEVER_STATUS only
     * Gate is CLOSED only when all three sensors (BS1, BS2, LS) are closed
     * LT_STATUS, LT1_STATUS, and LT2_STATUS are NOT considered for the main status column
     * 
     * @param gateId The gate ID to check (can be BOOM1_ID, BOOM2_ID, or handle)
     * @return true if all sensors (BS1, BS2, LS) are closed, false otherwise
     */
    public boolean isGateClosed(String gateId) {
        try {
            // Query managegates table for BS1_STATUS, BS2_STATUS, and LEVER_STATUS only
            // Check by BOOM1_ID, BOOM2_ID, or handle to support all sensor types
            String sql = "select BS1_STATUS, BS2_STATUS, LEVER_STATUS from managegates where BOOM1_ID=? OR BOOM2_ID=? OR handle=?";
            List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, gateId, gateId, gateId);
            
            if (rows == null || rows.isEmpty()) {
                logger.warn("No gate found for gate ID: {}", gateId);
                return false;
            }
            
            Map<String, Object> row = rows.get(0);
            String bs1Status = (String) row.get("BS1_STATUS");
            String bs2Status = (String) row.get("BS2_STATUS");
            String leverStatus = (String) row.get("LEVER_STATUS");
            
            // Default to "open" if status is null (for backward compatibility)
            if (bs1Status == null) bs1Status = "open";
            if (bs2Status == null) bs2Status = "open";
            if (leverStatus == null) leverStatus = "open";
            
            // Only BS1, BS2, and LS (LEVER_STATUS) must be closed
            // LT_STATUS, LT1_STATUS, and LT2_STATUS are NOT considered
            boolean isClosed = bs1Status.equalsIgnoreCase("closed") 
                && bs2Status.equalsIgnoreCase("closed")
                && leverStatus.equalsIgnoreCase("closed");
            
            logger.debug("Gate {} status check - BS1: {}, BS2: {}, LS: {}, Result: {} (LT statuses are NOT considered)", 
                gateId, bs1Status, bs2Status, leverStatus, isClosed);
            
            return isClosed;
            
        } catch (Exception e) {
            logger.error("Error checking gate status for gate ID: {}", gateId, e);
            return false;
        }
    }
    
    /**
     * Get current status of all sensors for a gate
     * 
     * @param gateId The gate ID
     * @return API object with all sensor statuses, or null if gate not found
     */
    public API getGateSensorStatuses(String gateId) {
        try {
            String sql = "select * from managegates where BOOM1_ID=? or handle=?";
            List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, gateId, gateId);
            
            if (rows == null || rows.isEmpty()) {
                logger.warn("No gate found for BOOM1_ID: {}", gateId);
                return null;
            }
            
            Map<String, Object> row = rows.get(0);
            API api = new API();
            api.setGate((String) row.get("BOOM1_ID"));
            api.setBs1Status((String) row.get("BS1_STATUS"));
            api.setBs2Status((String) row.get("BS2_STATUS"));
            api.setLtStatus((String) row.get("LT_STATUS"));
            api.setLeverStatus((String) row.get("LEVER_STATUS"));
            api.setLc_name((String) row.get("Gate_Num"));
            api.setSm((String) row.get("SM"));
            api.setGm((String) row.get("GM"));
            
            // Handle NULL values for backward compatibility
            if (api.getBs1Status() == null) api.setBs1Status("open");
            if (api.getBs2Status() == null) api.setBs2Status("open");
            if (api.getLtStatus() == null) api.setLtStatus("open");
            if (api.getLeverStatus() == null) api.setLeverStatus("open");
            
            return api;
            
        } catch (Exception e) {
            logger.error("Error getting sensor statuses for BOOM1_ID: {}", gateId, e);
            return null;
        }
    }
}

