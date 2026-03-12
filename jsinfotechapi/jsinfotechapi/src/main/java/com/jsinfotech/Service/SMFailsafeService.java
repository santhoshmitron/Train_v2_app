package com.jsinfotech.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service to handle SM (Station Master) failsafe logic
 * Tracks when data is received for each SM and manages failsafe status
 */
@Service
public class SMFailsafeService {

    private static final Logger logger = LogManager.getLogger(SMFailsafeService.class);

    @Autowired
    private RedisUserRepository userRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value(value = "${failsafe.timeout.ms:120000}")
    private long failsafeTimeoutMs;

    /**
     * Record that data was received for an SM
     * This should be called whenever /gate/api receives data
     */
    public void recordSMDataReceived(String sm) {
        if (sm != null && !sm.isEmpty()) {
            userRepository.setSMLastUpdateTime(sm);
            // Clear failsafe flag since data is being received
            userRepository.setSMFailsafe(sm, false);
            logger.debug("Recorded data received for SM: {}", sm);
        }
    }

    /**
     * Check if an SM is in failsafe mode (hasn't received data for timeout period)
     */
    public boolean isSMInFailsafe(String sm) {
        if (sm == null || sm.isEmpty()) {
            return false;
        }
        return userRepository.getSMFailsafe(sm);
    }
    
    /**
     * Get list of gate names that are in failsafe mode for a given SM
     * Returns a list of unique gate names (empty list if no gates in failsafe or SM not found)
     */
    public java.util.List<String> getFailsafeGateNamesForSM(String sm) {
        java.util.Set<String> gateNamesSet = new java.util.HashSet<>();
        if (sm == null || sm.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        try {
            java.util.List<java.util.Map<String, String>> failsafeGates = userRepository.getFailsafeGatesForSM(sm);
            for (java.util.Map<String, String> gateDetails : failsafeGates) {
                String gateName = gateDetails.get("gateName");
                if (gateName != null && !gateName.isEmpty()) {
                    gateNamesSet.add(gateName);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting failsafe gate names for SM: {}", sm, e);
        }
        
        // Convert Set to List to maintain return type and ensure uniqueness
        return new java.util.ArrayList<>(gateNamesSet);
    }
    
    /**
     * Get list of gate names that are in failsafe mode for a given GM
     * Returns a list of unique gate names (empty list if no gates in failsafe or GM not found)
     */
    public java.util.List<String> getFailsafeGateNamesForGM(String gm) {
        java.util.Set<String> gateNamesSet = new java.util.HashSet<>();
        if (gm == null || gm.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        try {
            java.util.List<java.util.Map<String, String>> failsafeGates = userRepository.getFailsafeGatesForGM(gm);
            for (java.util.Map<String, String> gateDetails : failsafeGates) {
                String gateName = gateDetails.get("gateName");
                if (gateName != null && !gateName.isEmpty()) {
                    gateNamesSet.add(gateName);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting failsafe gate names for GM: {}", gm, e);
        }
        
        // Convert Set to List to maintain return type and ensure uniqueness
        return new java.util.ArrayList<>(gateNamesSet);
    }
    
    /**
     * Check if a GM is in failsafe mode
     */
    public boolean isGMInFailsafe(String gm) {
        if (gm == null || gm.isEmpty()) {
            return false;
        }
        java.util.List<java.util.Map<String, String>> failsafeGates = userRepository.getFailsafeGatesForGM(gm);
        return failsafeGates != null && !failsafeGates.isEmpty();
    }

    /**
     * Check for stale gates and activate failsafe if needed
     * Processes ALL tracked gates to check healthy gates that need failsafe eviction
     * This should be called by a scheduled task
     */
    public void checkAndActivateFailsafe() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // --------------------------------------------------------------------
            // 1. Load all tracked gates
            // --------------------------------------------------------------------
            Set<String> trackedGates = userRepository.getAllTrackedGates();
            if (trackedGates.isEmpty()) {
                return;
            }
            
            // --------------------------------------------------------------------
            // 2. Load ALL managegates data in ONE query
            // --------------------------------------------------------------------
            String mgSql = "SELECT BOOM1_ID, BOOM2_ID, handle, Gate_Num, SM, GM FROM managegates";
            List<Map<String, Object>> mgRows = jdbcTemplate.queryForList(mgSql);
            
            // Build maps: Gate_Num → List<sensorIds>, Gate_Num → (SM, GM), sensorId → Gate_Num
            java.util.Map<String, java.util.List<String>> sensorsByGateName = new java.util.HashMap<>();
            java.util.Map<String, java.util.Map<String, String>> ownerByGateName = new java.util.HashMap<>();
            java.util.Map<String, String> gateNameBySensorId = new java.util.HashMap<>();
            
            for (Map<String, Object> row : mgRows) {
                Object boom1IdObj = row.get("BOOM1_ID");
                Object boom2IdObj = row.get("BOOM2_ID");
                Object handleObj = row.get("handle");
                Object gateNumObj = row.get("Gate_Num");
                Object smObj = row.get("SM");
                Object gmObj = row.get("GM");
                
                String boom1Id = boom1IdObj != null ? boom1IdObj.toString() : null;
                String boom2Id = boom2IdObj != null ? boom2IdObj.toString() : null;
                String handle = handleObj != null ? handleObj.toString() : null;
                String gateNum = gateNumObj != null ? gateNumObj.toString() : null;
                String sm = smObj != null ? smObj.toString() : "";
                String gm = gmObj != null ? gmObj.toString() : "";
                
                if (gateNum == null || gateNum.isEmpty()) {
                    continue;
                }
                
                // Build Gate_Num → sensorIds map
                sensorsByGateName.computeIfAbsent(gateNum, k -> new java.util.ArrayList<>());
                if (boom1Id != null && !boom1Id.isEmpty()) {
                    sensorsByGateName.get(gateNum).add(boom1Id);
                    gateNameBySensorId.put(boom1Id, gateNum);
                }
                if (boom2Id != null && !boom2Id.isEmpty()) {
                    sensorsByGateName.get(gateNum).add(boom2Id);
                    gateNameBySensorId.put(boom2Id, gateNum);
                }
                if (handle != null && !handle.isEmpty()) {
                    sensorsByGateName.get(gateNum).add(handle);
                    gateNameBySensorId.put(handle, gateNum);
                }
                
                // Store owner info (SM, GM) for each Gate_Num (use first occurrence)
                if (!ownerByGateName.containsKey(gateNum)) {
                    java.util.Map<String, String> owner = new java.util.HashMap<>();
                    owner.put("sm", sm);
                    owner.put("gm", gm);
                    ownerByGateName.put(gateNum, owner);
                }
            }
            
            // --------------------------------------------------------------------
            // 3. Process ALL tracked gates grouped by gateName
            // --------------------------------------------------------------------
            Set<String> processedGateNames = new java.util.HashSet<>();
            Set<String> affectedSMs = new java.util.HashSet<>();
            
            // Group tracked gates by gateName
            java.util.Map<String, java.util.Set<String>> trackedGatesByGateName = new java.util.HashMap<>();
            for (String sensorId : trackedGates) {
                String gateName = gateNameBySensorId.get(sensorId);
                if (gateName != null && !gateName.isEmpty()) {
                    trackedGatesByGateName.computeIfAbsent(gateName, k -> new java.util.HashSet<>()).add(sensorId);
                }
            }
            
            // Process ALL gateNames that have tracked sensors (not just first one)
            // This ensures all gates without data get failsafe reports
            for (String gateName : sensorsByGateName.keySet()) {
                if (processedGateNames.contains(gateName)) {
                    continue;
                }
                processedGateNames.add(gateName);
                
                java.util.List<String> sensorIds = sensorsByGateName.get(gateName);
                java.util.Map<String, String> owner = ownerByGateName.get(gateName);
                
                if (sensorIds == null || sensorIds.isEmpty() || owner == null) {
                    logger.debug("Skipping gateName: {} - missing sensorIds or owner info", gateName);
                    continue;
                }
                
                logger.debug("Processing failsafe check for gateName: {} with {} sensors", gateName, sensorIds.size());
                
                String sm = owner.get("sm");
                String gm = owner.get("gm");
                
                try {
                    // --------------------------------------------------------------------
                    // 4A. Check if ALL sensors (BS1, BS2, LS) have recent data
                    // --------------------------------------------------------------------
                    boolean allSensorsHaveRecentData = true;
                    for (String sensorId : sensorIds) {
                        Long lastUpdateTime = userRepository.getGateLastUpdateTime(sensorId);
                        if (lastUpdateTime == null || (currentTime - lastUpdateTime) > failsafeTimeoutMs) {
                            // This sensor is missing data or data is stale
                            allSensorsHaveRecentData = false;
                            break;
                        }
                    }
                    
                    // --------------------------------------------------------------------
                    // 4B. If ALL sensors have recent data → mark gate as recovered and evict failsafe from Redis
                    // --------------------------------------------------------------------
                    if (allSensorsHaveRecentData) {
                        // Values came back - remove failsafe state and acknowledge any existing failsafe reports
                        // This allows a new failsafe report to be created if values stop again
                        for (String sensorId : sensorIds) {
                            try {
                                java.util.Map<String, String> failsafeDetails = userRepository.getFailsafeGateDetails(sensorId);
                                if (failsafeDetails != null) {
                                    // Mark as recovered before removing - this allows new failsafe report if values stop again
                                    userRepository.markGateAsRecovered(sensorId);
                                    logger.debug("Marked gate {} as recovered - values came back", sensorId);
                                }
                            } catch (Exception e) {
                                logger.warn("Error marking gate as recovered: {}", sensorId, e);
                            }
                            userRepository.removeFailsafeGateDetails(sensorId);
                        }
                        
                        // Acknowledge any existing unacknowledged NO-NETWORK reports for this gate
                        // This allows a new failsafe report to be created if values stop again
                        try {
                            String lcNameValue = (gateName != null && !gateName.isEmpty()) ? gateName : "";
                            String smValue = (sm != null && !sm.isEmpty()) ? sm : "";
                            String gmValue = (gm != null && !gm.isEmpty()) ? gm : "";
                            
                            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("HH:mm");
                            formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                            String ackTime = formatter.format(new java.util.Date());
                            
                            int ackRows = jdbcTemplate.update(
                                "UPDATE reports SET ackn = ? WHERE command='NO-NETWORK' AND lc_name=? AND sm=? AND gm=? AND (ackn IS NULL OR ackn = '')",
                                ackTime, lcNameValue, smValue, gmValue
                            );
                            
                            if (ackRows > 0) {
                                logger.info("Acknowledged {} existing NO-NETWORK report(s) for gateName: {} - values came back", ackRows, gateName);
                            }
                        } catch (Exception ackEx) {
                            logger.warn("Error acknowledging NO-NETWORK reports for gateName: {}", gateName, ackEx);
                        }
                        
                        logger.debug("All sensors have recent data for gateName: {}, evicted NO-NETWORK failsafe", gateName);
                        continue;
                    }
                    
                    // --------------------------------------------------------------------
                    // 4C. If ANY sensor (BS1, BS2, LS) is missing recent data → check if already logged, then log if needed
                    // --------------------------------------------------------------------
                    // Store failsafe details in Redis for all sensorIds (BS and LS)
                    for (String sensorId : sensorIds) {
                        userRepository.setFailsafeGateDetails(sensorId, gateName, sm, gm);
                    }

                    // Strong suppression: if a failsafe report has already been created for this gate
                    // and gate has not recovered yet, do not print again (even if DB check fails).
                    boolean alreadyPrintedInThisFailsafeEpisode = false;
                    try {
                        if (sensorIds != null && !sensorIds.isEmpty()) {
                            // Any sensorId key can represent this gate's failsafe episode, since we store for all sensorIds.
                            alreadyPrintedInThisFailsafeEpisode = userRepository.hasFailsafeReportBeenCreated(sensorIds.get(0));
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    
                    // Check if failsafe report should be created
                    // Only create if no unacknowledged NO-NETWORK report exists for this gate
                    // This ensures we don't print duplicates until values come back and report is acknowledged
                    boolean shouldLogReport = true;

                    if (alreadyPrintedInThisFailsafeEpisode) {
                        shouldLogReport = false;
                        logger.debug("NO-NETWORK suppressed for gateName: {} - already printed in current failsafe episode (until recovery)", gateName);
                    }
                    
                    try {
                        String lcNameValue = (gateName != null && !gateName.isEmpty()) ? gateName : "";
                        String smValue = (sm != null && !sm.isEmpty()) ? sm : "";
                        String gmValue = (gm != null && !gm.isEmpty()) ? gm : "";
                        
                        // Check if unacknowledged NO-NETWORK report exists (not acknowledged yet)
                        // Only create new report if no unacknowledged report exists
                        Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM reports WHERE command='NO-NETWORK' AND lc_name=? AND sm=? AND gm=? AND (ackn IS NULL OR ackn = '')",
                            Integer.class,
                            lcNameValue, smValue, gmValue
                        );
                        
                        if (count != null && count > 0) {
                            // Unacknowledged NO-NETWORK report already exists - don't create duplicate
                            shouldLogReport = false;
                            logger.debug("NO-NETWORK report for gateName: {} skipped - unacknowledged report already exists (count: {})", gateName, count);
                        } else {
                            // No unacknowledged report exists - allow creating new report
                            logger.debug("NO-NETWORK report for gateName: {} allowed - no unacknowledged report found", gateName);
                        }
                    } catch (Exception checkEx) {
                        logger.warn("Error checking for existing NO-NETWORK report for gateName: {}, will proceed with logging", gateName, checkEx);
                        // On error, proceed with logging to be safe
                        shouldLogReport = true;
                    }
                    
                    // Log NO-NETWORK only if no existing report found (print only once)
                    // and we have at least one owner (SM or GM) available.
                    // (Old condition used `||` with `!sm.isEmpty()` which could be evaluated even when sm is null)
                    boolean hasOwner = (sm != null && !sm.isEmpty()) || (gm != null && !gm.isEmpty());
                    if (shouldLogReport && hasOwner) {
                        try {
                            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                            String formattedTime = formatter.format(new java.util.Date());
                            String lcNameValue = (gateName != null && !gateName.isEmpty()) ? gateName : "";
                            int rowsAffected = jdbcTemplate.update(
                                "insert into reports (command, lc_name, sm, gm, added_on, tn, pn, tn_time, wer, lc, lc_status, redy) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                "NO-NETWORK",
                                lcNameValue,
                                sm != null && !sm.isEmpty() ? sm : "",
                                gm != null && !gm.isEmpty() ? gm : "",
                                formattedTime,
                                "", "", "", "", "", "", "s"
                            );
                            
                            // Only push audio if record was successfully inserted
                            if (rowsAffected > 0) {
                                logger.info("Logged NO-NETWORK report for gateName: {} - data not coming for 2 min 30 sec", gateName);
                                
                                // Mark report created in Redis for ALL sensorIds so it won't print again until recovery
                                try {
                                    for (String sensorId : sensorIds) {
                                        userRepository.setFailsafeReportCreated(sensorId);
                                    }
                                } catch (Exception markEx) {
                                    // ignore
                                }
                                
                                // Push audio message only when record is inserted
                                try {
                                    String audioMessage = "LC " + gateName + " No network ,Operate with Manual PN";
                                    if (sm != null && !sm.isEmpty()) {
                                        userRepository.pushAudio(sm, audioMessage);
                                        logger.info("Pushed audio message for SM: {} - {}", sm, audioMessage);
                                    }
                                } catch (Exception audioEx) {
                                    logger.warn("Error pushing audio message for gateName: {}, SM: {}", gateName, sm, audioEx);
                                    // Continue even if audio push fails
                                }
                            } else {
                                logger.warn("Failed to insert NO-NETWORK report for gateName: {}", gateName);
                            }
                        } catch (Exception reportEx) {
                            logger.error("Error logging NO-NETWORK report for gateName: {}", gateName, reportEx);
                        }
                    } else {
                        logger.debug("NO-NETWORK report skipped for gateName: {} - within 2 min 30 sec threshold", gateName);
                    }
                    
                    // Failsafe mechanism: Do NOT update individual sensor statuses (BS1_STATUS, BS2_STATUS, LEVER_STATUS)
                    // These should only be updated when actual sensor data comes through the API
                    // The main status column will be calculated by GateStatusService based on current sensor statuses
                    // We only log the failsafe condition here, but don't force sensor statuses to change
                    for (String sensorId : sensorIds) {
                        logger.debug("Failsafe activated for sensor {} (gateName: {}), but sensor statuses remain unchanged until API data is received", 
                            sensorId, gateName);
                    }
                    
                    if (sm != null && !sm.isEmpty()) {
                        affectedSMs.add(sm);
                    }
                } catch (Exception e) {
                    logger.error("Error updating failsafe for gateName: {}", gateName, e);
                }
            }
            
            // --------------------------------------------------------------------
            // 5. Set failsafe flag to true for affected SMs
            // --------------------------------------------------------------------
            for (String sm : affectedSMs) {
                userRepository.setSMFailsafe(sm, true);
                logger.info("Set failsafe flag to true for SM: {} (has stale gates)", sm);
            }
        } catch (Exception e) {
            logger.error("Error in gate failsafe check", e);
        }
    }
}

