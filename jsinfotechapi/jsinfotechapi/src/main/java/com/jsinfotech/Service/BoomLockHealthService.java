package com.jsinfotech.Service;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.API;

@Service
public class BoomLockHealthService {

    private static final Logger logger = LogManager.getLogger(BoomLockHealthService.class);
    private static final long HEALTH_CHECK_TIMEOUT_MS = 10000; // 10 seconds
    private static final long MAX_HEALTH_CHECK_AGE_MS = 60000; // 1 minute - maximum age for health check to be considered valid
    
    @Autowired
    private JdbcTemplate jdbcTemplate1;
    
    @Autowired
    private RedisUserRepository userRepository;
    
    @Autowired
    private GateStatusService gateStatusService;
    
    /**
     * Resolve any gate identifier (BOOM1_ID / BOOM2_ID / handle) to canonical BOOM1_ID.
     * Returns original input if not found.
     */
    private String resolveBoom1Id(String gateId) {
        if (gateId == null || gateId.trim().isEmpty()) {
            return gateId;
        }
        try {
            String gateSql = "SELECT BOOM1_ID FROM managegates WHERE BOOM1_ID=? OR BOOM2_ID=? OR handle=? LIMIT 1";
            List<java.util.Map<String, Object>> gateRows = jdbcTemplate1.queryForList(gateSql, gateId, gateId, gateId);
            if (gateRows != null && !gateRows.isEmpty()) {
                Object boom1Obj = gateRows.get(0).get("BOOM1_ID");
                if (boom1Obj != null) {
                    String boom1Id = boom1Obj.toString();
                    if (!boom1Id.trim().isEmpty()) {
                        return boom1Id;
                    }
                }
            }
        } catch (Exception e) {
            // ignore and fallback to original
        }
        return gateId;
    }

    /**
     * Start Boom_Lock health checks for ALL gates under a GM that are currently CLOSED.
     * This is required when one GM controls multiple gates and we must update Boom_Lock for each closed gate.
     */
    public void startHealthCheckForClosedGatesOfGM(String gmUsername) {
        if (gmUsername == null || gmUsername.trim().isEmpty()) {
            logger.warn("startHealthCheckForClosedGatesOfGM called with empty gmUsername");
            return;
        }
        try {
            String sql = "SELECT BOOM1_ID FROM managegates WHERE GM=? AND LOWER(status)='closed'";
            List<java.util.Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, gmUsername);
            if (rows == null || rows.isEmpty()) {
                logger.debug("No CLOSED gates found for GM: {}. Nothing to enqueue.", gmUsername);
                return;
            }
            java.util.List<String> boom1Ids = new java.util.ArrayList<>();
            for (java.util.Map<String, Object> row : rows) {
                Object boom1Obj = row.get("BOOM1_ID");
                if (boom1Obj != null && !boom1Obj.toString().trim().isEmpty()) {
                    boom1Ids.add(boom1Obj.toString().trim());
                }
            }
            logger.info("Enqueuing Boom_Lock health check for GM: {}. CLOSED gates count: {}. BOOM1_IDs: {}", gmUsername, boom1Ids.size(), boom1Ids);
            for (String boom1Id : boom1Ids) {
                startHealthCheck(boom1Id);
            }
        } catch (Exception e) {
            logger.error("Error enqueuing Boom_Lock health checks for GM: {}", gmUsername, e);
        }
    }
    
    /**
     * Start health check when gate status becomes CLOSED
     * Stores timestamp in Redis for monitoring
     * Requirement: When status becomes CLOSED, start 10 sec timer. After timer, check LT_STATUS and update Boom_Lock in reports table
     * 
     * @param gateId The gate ID
     */
    public void startHealthCheck(String gateId) {
        try {
            // Normalize to BOOM1_ID so keys are consistent and multi-alias gates don't cause missed updates
            String boom1Id = resolveBoom1Id(gateId);
            String healthKey = "boom:health:" + boom1Id;
            String lockStatusKey = "boom:lock:status:" + boom1Id;
            String lockUpdatedKey = "boom:lock:updated:" + boom1Id;

            // IMPORTANT: Make startHealthCheck idempotent.
            // If a health check is already active for this BOOM1_ID, do NOT clear lockStatusKey or reset timers.
            // Otherwise, repeated calls while gate remains CLOSED will keep forcing "first check" and create duplicates.
            try {
                String existingHealthTs = userRepository.getHealthCheckTimestamp(healthKey);
                if (existingHealthTs != null && !existingHealthTs.trim().isEmpty()) {
                    logger.debug("Health check already active for gate: {} (BOOM1_ID: {}). Skipping re-start to avoid duplicate Boom_Lock prints.", gateId, boom1Id);
                    return;
                }
            } catch (Exception ignore) {
                // If Redis read fails, continue with start behavior (better to have checks than miss them)
            }
            
                        // Clear previous Boom_Lock status to mark start of new cycle
                        // This ensures first check after status becomes "Closed" will always print
            try {
                        userRepository.deleteHealthCheckKey(lockStatusKey);
                userRepository.deleteHealthCheckKey(lockUpdatedKey); // allow exactly one UPDATE for this CLOSED cycle
                        logger.debug("Cleared lockStatusKey for BOOM1_ID: {} when starting health check for gateId: {}", boom1Id, gateId);
            } catch (Exception e) {
                logger.warn("Could not clear lockStatusKey for BOOM1_ID: {} (gateId: {}), will clear on first check", boom1Id, gateId, e);
            }
            
            // Store current timestamp when status closes, but add 10-second delay before first check
            long currentTime = System.currentTimeMillis();
            long startTimeWithDelay = currentTime + 10000; // Add 10-second delay
            userRepository.setHealthCheckTimestamp(healthKey, String.valueOf(startTimeWithDelay));
            
            logger.info("Started boom lock health check for gate: {} (BOOM1_ID: {}, status became CLOSED). Health check will begin after 10-second delay.", gateId, boom1Id);
            
        } catch (Exception e) {
            logger.error("Error starting health check for gate: {}", gateId, e);
        }
    }
    
    /**
     * Clear health check when gate status opens
     * 
     * @param gateId The gate ID
     */
    public void clearHealthCheck(String gateId) {
        try {
            String boom1Id = resolveBoom1Id(gateId);
            String healthKey = "boom:health:" + boom1Id;
            String lockStatusKey = "boom:lock:status:" + boom1Id;
            String lockUpdatedKey = "boom:lock:updated:" + boom1Id;
            
            userRepository.deleteHealthCheckKey(healthKey);
            userRepository.deleteHealthCheckKey(lockStatusKey);
            userRepository.deleteHealthCheckKey(lockUpdatedKey);
            
            logger.debug("Cleared health check for gate: {} (BOOM1_ID: {})", gateId, boom1Id);
            
        } catch (Exception e) {
            logger.error("Error clearing health check for gate: {}", gateId, e);
        }
    }
    
    /**
     * Scheduled task to check all active health checks every 10 seconds
     * Requirement: When status becomes CLOSED, start 10 sec timer. After timer, check LT_STATUS and update Boom_Lock in reports table
     * Logic: 
     * - First check after status becomes "Closed": Always print Boom_Lock status (Healthy/Unhealthy) based on LT_STATUS
     * - Subsequent checks: Only print when Boom_Lock status changes (Healthy ↔ Unhealthy)
     * - Continue checking every 10 seconds until main status becomes "Open"
     */
    @Scheduled(fixedDelay = 10000) // Check every 10 seconds
    public void checkBoomLockHealth() {
        try {
            // Get all active health check keys from Redis
            Set<String> healthKeys = userRepository.getHealthCheckKeys("boom:health:*");
            
            if (healthKeys == null || healthKeys.isEmpty()) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            
            // Track which BOOM1_IDs we've already processed in this cycle
            // This prevents multiple gateIds (handle, BOOM1_ID, BOOM2_ID) from creating duplicate reports
            Set<String> processedBoom1Ids = new java.util.HashSet<>();
            
            for (String healthKey : healthKeys) {
                try {
                    // Extract gateId from key (format: "boom:health:{gateId}")
                    String gateId = healthKey.substring("boom:health:".length());
                    
                    // Get timestamp when status closed
                    String timestampStr = userRepository.getHealthCheckTimestamp(healthKey);
                    if (timestampStr == null || timestampStr.isEmpty()) {
                        continue;
                    }
                    
                    long statusCloseTime = Long.parseLong(timestampStr);
                    long elapsedTime = currentTime - statusCloseTime;
                    
                    // Validate health check is not stale (not older than 1 hour - allow continuous checking)
                    if (elapsedTime > 3600000) { // 1 hour max
                        logger.warn("Health check for gate: {} is stale ({}ms old, max: 3600000ms). Clearing health check.", 
                            gateId, elapsedTime);
                        clearHealthCheck(gateId);
                        continue;
                    }
                    
                    // Check if 10-second delay has passed (elapsedTime >= 0 means delay has passed)
                    if (elapsedTime >= 0) {
                        // Get gate information (BOOM1_ID, Gate_Num, and status) from managegates
                        String gateSql = "SELECT BOOM1_ID, Gate_Num, status FROM managegates WHERE BOOM1_ID=? OR BOOM2_ID=? OR handle=? LIMIT 1";
                        List<java.util.Map<String, Object>> gateRows = jdbcTemplate1.queryForList(gateSql, gateId, gateId, gateId);
                        
                        if (gateRows == null || gateRows.isEmpty()) {
                            logger.warn("Gate not found in managegates for gateId: {}. Clearing health check.", gateId);
                            clearHealthCheck(gateId);
                            continue;
                        }
                        
                        String boom1Id = (String) gateRows.get(0).get("BOOM1_ID");
                        String gateNum = (String) gateRows.get(0).get("Gate_Num");
                        String gateStatus = (String) gateRows.get(0).get("status");
                        
                        // First check if main status is still "closed" - if not, skip LT_STATUS check and Boom_Lock update
                        String gateStatusNorm = gateStatus != null ? gateStatus.trim() : null;
                        if (gateStatusNorm == null || !gateStatusNorm.equalsIgnoreCase("closed")) {
                            // Sometimes managegates.status can lag or be inconsistent; if there is a recent Closed report row,
                            // we still want to update Boom_Lock for the latest Closed row.
                            boolean hasRecentClosedReport = false;
                            try {
                                String recentClosedSql =
                                    "SELECT id FROM reports " +
                                    "WHERE (lc IN (?,?) OR lc_name IN (?,?)) " +
                                    "AND UPPER(command)='CLOSED' " +
                                    "AND added_on >= DATE_SUB(NOW(), INTERVAL 10 MINUTE) " +
                                    "ORDER BY added_on DESC LIMIT 1";
                                List<java.util.Map<String, Object>> recentClosedRows =
                                    jdbcTemplate1.queryForList(recentClosedSql, boom1Id, gateNum, boom1Id, gateNum);
                                hasRecentClosedReport = (recentClosedRows != null && !recentClosedRows.isEmpty());
                            } catch (Exception ignore) {
                                hasRecentClosedReport = false;
                            }
                            
                            if (!hasRecentClosedReport) {
                                logger.info("Gate status is '{}' (not 'closed') and no recent Closed report. Clearing health check for gate: {} (BOOM1_ID: {}).", 
                                gateStatus, gateId, boom1Id);
                            clearHealthCheck(gateId);
                            continue;
                            }
                            logger.debug("managegates.status is '{}' but a recent Closed report exists; continuing Boom_Lock evaluation for gate: {} (BOOM1_ID: {}).",
                                gateStatus, gateId, boom1Id);
                        }
                        
                        // Skip if we've already processed this BOOM1_ID in this cycle
                        // This prevents duplicate reports when multiple gateIds map to the same gate
                        if (processedBoom1Ids.contains(boom1Id)) {
                            logger.debug("Already processed BOOM1_ID: {} in this cycle. Skipping gateId: {}.", boom1Id, gateId);
                            // Still update timestamp for this healthKey to maintain 10-second interval
                            long nextCheckTime = currentTime + 10000;
                            userRepository.setHealthCheckTimestamp(healthKey, String.valueOf(nextCheckTime));
                            continue;
                        }
                        
                        // Mark this BOOM1_ID as processed
                        processedBoom1Ids.add(boom1Id);
                        
                        // Status is still "closed" - proceed with LT_STATUS check
                        logger.debug("Gate status is still 'closed' for gate: {} (BOOM1_ID: {}). Proceeding with LT_STATUS check.", 
                            gateId, boom1Id);
                        
                        // Get LT_STATUS from database using BOOM1_ID to ensure consistency
                        String ltSql = "SELECT LT_STATUS FROM managegates WHERE BOOM1_ID=? LIMIT 1";
                        List<java.util.Map<String, Object>> ltRows = jdbcTemplate1.queryForList(ltSql, boom1Id);
                        String dbLtStatus = null;
                        if (ltRows != null && !ltRows.isEmpty()) {
                            dbLtStatus = (String) ltRows.get(0).get("LT_STATUS");
                        }
                        
                        // Determine Boom_Lock status based on LT_STATUS
                        String currentBoomLockStatus = null;
                        if (dbLtStatus != null && dbLtStatus.equalsIgnoreCase("closed")) {
                            currentBoomLockStatus = "Healthy";
                        } else {
                            currentBoomLockStatus = "Unhealthy";
                        }
                        
                        // Format timestamp in HH:mm format (IST timezone)
                        java.text.SimpleDateFormat timeFormatter = new java.text.SimpleDateFormat("HH:mm");
                        timeFormatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                        String timeStamp = timeFormatter.format(new java.util.Date());
                        
                        // Format Boom_Lock value with timestamp: "Healthy[HH:mm]" or "Unhealthy[HH:mm]"
                        String currentBoomLockValue = currentBoomLockStatus + "[" + timeStamp + "]";
                        
                        // Debug: Verify timestamp format is correct
                        logger.debug("Formatted Boom_Lock value: '{}' (status: {}, timestamp: {})", currentBoomLockValue, currentBoomLockStatus, timeStamp);
                        
                        // Use BOOM1_ID for lockStatusKey to handle multiple gateIds mapping to same gate
                        String lockStatusKey = "boom:lock:status:" + boom1Id;
                        String lockUpdatedKey = "boom:lock:updated:" + boom1Id;
                        String previousBoomLockValue = userRepository.getHealthCheckTimestamp(lockStatusKey);
                        String hasUpdatedInitialRow = userRepository.getHealthCheckTimestamp(lockUpdatedKey);
                        
                        // Extract status from previous value (if it has format "Healthy[HH:mm]" or just "Healthy")
                        String previousBoomLockStatus = null;
                        if (previousBoomLockValue != null && !previousBoomLockValue.isEmpty()) {
                            // Extract status part (before '[' if present, otherwise use whole string)
                            if (previousBoomLockValue.contains("[")) {
                                previousBoomLockStatus = previousBoomLockValue.substring(0, previousBoomLockValue.indexOf("["));
                            } else {
                                previousBoomLockStatus = previousBoomLockValue;
                            }
                        }
                        
                        // Check if this is the first check after status became "Closed"
                        // First check requires:
                        // - previousBoomLockValue is null (no previous status stored)
                        // - AND we have NOT already updated the initial Closed row in this CLOSED cycle
                        // This happens when status first becomes "Closed" and we clear the key in startHealthCheck()
                        // IMPORTANT: When LT opens and then closes again, clearHealthCheck() clears the key,
                        // which makes previousBoomLockValue null. But we should NEVER update old rows that already have Boom_Lock.
                        boolean isFirstCheck = (previousBoomLockValue == null) && (hasUpdatedInitialRow == null || hasUpdatedInitialRow.trim().isEmpty());
                        
                        // CRITICAL: Before proceeding, check if there are any Closed rows with Boom_Lock for this gate
                        // If yes, this is NOT a true first check - it's a status change scenario
                        // We should create a new row, not update old ones
                        boolean hasExistingBoomLockRows = false;
                        try {
                            String checkExistingSql =
                                "SELECT COUNT(*) as cnt FROM reports " +
                                "WHERE (lc IN (?,?) OR lc_name IN (?,?)) " +
                                "AND (UPPER(command)='CLOSED' OR UPPER(command)='CLOSE') " +
                                "AND Boom_Lock IS NOT NULL AND Boom_Lock != '' " +
                                "AND added_on >= DATE_SUB(NOW(), INTERVAL 1 HOUR)";
                            List<java.util.Map<String, Object>> existingRows = jdbcTemplate1.queryForList(
                                checkExistingSql, boom1Id, gateNum, boom1Id, gateNum
                            );
                            if (existingRows != null && !existingRows.isEmpty()) {
                                Object cntObj = existingRows.get(0).get("cnt");
                                int count = 0;
                                if (cntObj instanceof Number) {
                                    count = ((Number) cntObj).intValue();
                                } else if (cntObj != null) {
                                    count = Integer.parseInt(cntObj.toString());
                                }
                                hasExistingBoomLockRows = (count > 0);
                            }
                        } catch (Exception e) {
                            logger.warn("Could not check for existing Boom_Lock rows for BOOM1_ID: {}", boom1Id, e);
                        }
                        
                        // CRITICAL: Check if status has changed (even if previousBoomLockValue is null)
                        // If status changed, we MUST create a new row and NEVER update old rows
                        boolean statusChanged = false;
                        if (previousBoomLockStatus != null && !previousBoomLockStatus.equals(currentBoomLockStatus)) {
                            statusChanged = true;
                            logger.info("Status change detected: Previous: '{}', Current: '{}'. Will create NEW row only, NEVER update old rows. Gate: {} (BOOM1_ID: {})", 
                                previousBoomLockStatus, currentBoomLockStatus, gateId, boom1Id);
                        }
                        
                        // If there are existing Boom_Lock rows OR status changed, treat this as a status change (not first check)
                        // This prevents updating old rows when LT opens/closes and health check is cleared
                        if ((hasExistingBoomLockRows || statusChanged) && isFirstCheck) {
                            logger.info("Found existing Boom_Lock rows OR status changed for gate: {} (BOOM1_ID: {}). Treating as status change, not first check. Will create NEW row only.", 
                                gateId, boom1Id);
                            isFirstCheck = false; // Force status change behavior (create new row, don't update old ones)
                        }
                        
                        // Determine if we should print:
                        // - First check: Always print (don't check previous status)
                        // - Subsequent checks: Only print if status changed (Healthy ↔ Unhealthy)
                        boolean shouldPrint = false;
                        if (isFirstCheck) {
                            // First check after status becomes "Closed" - always print
                            // BUT: Only if status hasn't changed and no existing Boom_Lock rows exist
                            shouldPrint = true;
                            logger.info("First Boom_Lock check for gate: {} (BOOM1_ID: {}). Current status: {}. Will update train report row.", 
                                gateId, boom1Id, currentBoomLockStatus);
                        } else {
                            // Subsequent check - only print if status changed
                            shouldPrint = statusChanged;
                            if (!statusChanged) {
                                logger.debug("Boom_Lock status unchanged for BOOM1_ID: {}. Current: {}, Previous: {}. Skipping update.", 
                                    boom1Id, currentBoomLockStatus, previousBoomLockStatus);
                            } else {
                                logger.info("Boom_Lock status changed for BOOM1_ID: {}. Previous: {}, Current: {}. Will create new report row.", 
                                    boom1Id, previousBoomLockStatus, currentBoomLockStatus);
                            }
                        }
                        
                        // Update timestamp for all healthKeys for this BOOM1_ID to maintain 10-second interval
                        // Find all healthKeys that map to this BOOM1_ID
                        for (String otherHealthKey : healthKeys) {
                            String otherGateId = otherHealthKey.substring("boom:health:".length());
                            String otherTimestampStr = userRepository.getHealthCheckTimestamp(otherHealthKey);
                            if (otherTimestampStr != null && !otherTimestampStr.isEmpty()) {
                                try {
                                    // Check if this healthKey also maps to the same BOOM1_ID
                                    List<java.util.Map<String, Object>> otherGateRows = jdbcTemplate1.queryForList(
                                        "SELECT BOOM1_ID FROM managegates WHERE (BOOM1_ID=? OR BOOM2_ID=? OR handle=?) AND BOOM1_ID=? LIMIT 1",
                                        otherGateId, otherGateId, otherGateId, boom1Id);
                                    if (otherGateRows != null && !otherGateRows.isEmpty()) {
                                        long nextCheckTime = currentTime + 10000;
                                        userRepository.setHealthCheckTimestamp(otherHealthKey, String.valueOf(nextCheckTime));
                                    }
                                } catch (Exception e) {
                                    // Ignore errors when updating other healthKeys
                                }
                            }
                        }
                        
                        if (!shouldPrint) {
                            // Status hasn't changed (and this is not first check), skip update but continue checking
                            continue;
                        }
                        
                        // CRITICAL: Update Redis IMMEDIATELY before database operations to prevent race conditions
                        // This ensures that if multiple scheduled tasks run concurrently, they see the updated status
                        // and don't create duplicate rows
                        userRepository.setHealthCheckTimestamp(lockStatusKey, currentBoomLockValue);
                        logger.debug("Updated Redis key '{}' with value '{}' BEFORE database operations to prevent race conditions", 
                            lockStatusKey, currentBoomLockValue);
                        
                        // Print condition met: either first check (always print) or status changed
                        try {
                            // CRITICAL FINAL CHECK: If status changed, ALWAYS create new row, NEVER update old rows
                            // This is a final safeguard to prevent any possibility of updating old rows when status changes
                            if (statusChanged) {
                                logger.warn("FINAL SAFEGUARD: Status changed detected. Forcing new row creation, skipping any update path. Gate: {} (BOOM1_ID: {})", 
                                    gateId, boom1Id);
                                // Force to else block (create new row)
                                isFirstCheck = false;
                            }
                            
                            if (isFirstCheck && !statusChanged) {
                                // First check: UPDATE the latest CLOSED status row (most recent command='Closed') for this gate
                                // ONLY if status has NOT changed
                                logger.info("First Boom_Lock check for gate: {} (BOOM1_ID: {}). Current status: {}. Updating train report row.", 
                                    gateId, boom1Id, currentBoomLockStatus);
                                
                                // CRITICAL FIX: First find the ABSOLUTE LATEST Closed row (regardless of Boom_Lock)
                                // Then check if it already has Boom_Lock - if yes, create new row; if no, update it
                                // This prevents finding and updating older rows when the latest row already has Boom_Lock
                                String findLatestReportSql =
                                    "SELECT id, added_on, Boom_Lock FROM reports " +
                                    "WHERE (lc IN (?,?) OR lc_name IN (?,?)) " +
                                    "AND (UPPER(command)='CLOSED' OR UPPER(command)='CLOSE') " +
                                    "AND added_on >= DATE_SUB(NOW(), INTERVAL 10 MINUTE) " +
                                    "ORDER BY added_on DESC, id DESC LIMIT 1";
                                List<java.util.Map<String, Object>> latestReportRows = jdbcTemplate1.queryForList(
                                    findLatestReportSql, boom1Id, gateNum, boom1Id, gateNum
                                );
                                
                                // Check if the latest row already has Boom_Lock
                                boolean latestRowHasBoomLock = false;
                                Integer reportId = null;
                                String rowAddedOn = "unknown";
                                
                                String latestBoomLockStatus = null; // status-only (Healthy/Unhealthy)
                                if (latestReportRows != null && !latestReportRows.isEmpty()) {
                                    Object boomLockObj = latestReportRows.get(0).get("Boom_Lock");
                                    String latestBoomLock = null;
                                    if (boomLockObj != null) {
                                        latestBoomLock = boomLockObj.toString().trim();
                                    }
                                    if (latestBoomLock != null && !latestBoomLock.isEmpty()) {
                                        // Extract status part from DB value (before '[' if present)
                                        if (latestBoomLock.contains("[")) {
                                            latestBoomLockStatus = latestBoomLock.substring(0, latestBoomLock.indexOf("[")).trim();
                                        } else {
                                            latestBoomLockStatus = latestBoomLock.trim();
                                        }
                                        latestRowHasBoomLock = true;
                                        logger.warn("LATEST Closed row already has Boom_Lock='{}'. Will create NEW row instead of updating. Gate: {} (BOOM1_ID: {})", 
                                            latestBoomLock, gateId, boom1Id);
                        } else {
                                        // Latest row doesn't have Boom_Lock, we can update it
                                        try {
                                            Object idObj = latestReportRows.get(0).get("id");
                                            if (idObj instanceof Number) {
                                                reportId = ((Number) idObj).intValue();
                                            } else if (idObj != null) {
                                                reportId = Integer.parseInt(idObj.toString());
                                            }
                                            rowAddedOn = latestReportRows.get(0).get("added_on") != null ? latestReportRows.get(0).get("added_on").toString() : "unknown";
                                        } catch (Exception idEx) {
                                            logger.warn("Could not parse report id from latest row. gateId: {}, BOOM1_ID: {}, Gate_Num: {}", 
                                                gateId, boom1Id, gateNum);
                                        }
                                    }
                                }
                                
                                // If latest row has Boom_Lock, create new row instead
                                if (latestRowHasBoomLock) {
                                    // ABSOLUTE RULE: If latest DB Boom_Lock status is the SAME as current status,
                                    // then LT has NOT actually flipped -> NEVER insert duplicates.
                                    if (latestBoomLockStatus != null && latestBoomLockStatus.equalsIgnoreCase(currentBoomLockStatus)) {
                                        logger.info("Skipping INSERT: Latest Closed row already has same Boom_Lock status '{}' as current '{}'. No LT flip. Gate: {} (BOOM1_ID: {})",
                                            latestBoomLockStatus, currentBoomLockStatus, gateId, boom1Id);
                                        continue;
                                    }
                                    String ownerSql = "SELECT SM, GM FROM managegates WHERE BOOM1_ID=? LIMIT 1";
                                    List<java.util.Map<String, Object>> ownerRows = jdbcTemplate1.queryForList(ownerSql, boom1Id);
                                    String sm = "";
                                    String gm = "";
                                    if (ownerRows != null && !ownerRows.isEmpty()) {
                                        sm = ownerRows.get(0).get("SM") != null ? ownerRows.get(0).get("SM").toString() : "";
                                        gm = ownerRows.get(0).get("GM") != null ? ownerRows.get(0).get("GM").toString() : "";
                                    }
                                    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                                    String formattedTime = formatter.format(new java.util.Date());
                                    int insertResult = jdbcTemplate1.update(
                                        "INSERT INTO reports (command, lc_name, sm, gm, added_on, tn, pn, tn_time, wer, lc, lc_status, redy, Boom_Lock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                        "", gateNum, sm, gm, formattedTime, "", "", "", "", boom1Id, "", "s", currentBoomLockValue
                                    );
                                    if (insertResult > 0) {
                                        logger.info("Created NEW report row with Boom_Lock='{}' (latest Closed row already has Boom_Lock). Gate: {} (BOOM1_ID: {})", 
                                            currentBoomLockValue, gateId, boom1Id);
                                    }
                                    continue;
                                }
                                
                                // Latest row doesn't have Boom_Lock, proceed with update
                                // reportId and rowAddedOn already extracted above (lines 450-457)
                                if (reportId == null) {
                                    logger.warn("Skipping Boom_Lock update because reportId is null. gateId: {}, BOOM1_ID: {}, Gate_Num: {}", gateId, boom1Id, gateNum);
                                    // Redis already updated above, no need to update again
                                    continue;
                                }
                                
                                // Boom_Lock check already done above (latestRowHasBoomLock check)
                                // Proceed directly to update since we confirmed latest row doesn't have Boom_Lock
                                logger.info("Updating LATEST Closed row (first time) - ID: {}, added_on: {}, with Boom_Lock: '{}' (status: '{}', time: '{}') for gate: {} (BOOM1_ID: {})", 
                                    reportId, rowAddedOn, currentBoomLockValue, currentBoomLockStatus, timeStamp, gateId, boom1Id);
                                
                                // CRITICAL: Re-verify the row doesn't have Boom_Lock before updating
                                // This prevents race conditions where row gets Boom_Lock between SELECT and UPDATE
                                String verifyBeforeUpdateSql = "SELECT Boom_Lock FROM reports WHERE id=?";
                                List<java.util.Map<String, Object>> verifyBeforeUpdateRows = jdbcTemplate1.queryForList(verifyBeforeUpdateSql, reportId);
                                if (verifyBeforeUpdateRows != null && !verifyBeforeUpdateRows.isEmpty()) {
                                    Object verifyBoomLockObj = verifyBeforeUpdateRows.get(0).get("Boom_Lock");
                                    String verifyBoomLock = null;
                                    if (verifyBoomLockObj != null) {
                                        verifyBoomLock = verifyBoomLockObj.toString().trim();
                                    }
                                    if (verifyBoomLock != null && !verifyBoomLock.isEmpty()) {
                                        logger.warn("RE-VERIFY FAILED: Row ID: {} now has Boom_Lock='{}'. Skipping update and creating new row instead. Gate: {} (BOOM1_ID: {})", 
                                            reportId, verifyBoomLock, gateId, boom1Id);
                                        // Create new row instead of updating
                                        String ownerSql = "SELECT SM, GM FROM managegates WHERE BOOM1_ID=? LIMIT 1";
                                        List<java.util.Map<String, Object>> ownerRows = jdbcTemplate1.queryForList(ownerSql, boom1Id);
                                        String sm = "";
                                        String gm = "";
                                        if (ownerRows != null && !ownerRows.isEmpty()) {
                                            sm = ownerRows.get(0).get("SM") != null ? ownerRows.get(0).get("SM").toString() : "";
                                            gm = ownerRows.get(0).get("GM") != null ? ownerRows.get(0).get("GM").toString() : "";
                                        }
                                        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                        formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                                        String formattedTime = formatter.format(new java.util.Date());
                                        int insertResult = jdbcTemplate1.update(
                                            "INSERT INTO reports (command, lc_name, sm, gm, added_on, tn, pn, tn_time, wer, lc, lc_status, redy, Boom_Lock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                            "", gateNum, sm, gm, formattedTime, "", "", "", "", boom1Id, "", "s", currentBoomLockValue
                                        );
                                        if (insertResult > 0) {
                                            logger.info("Created NEW report row with Boom_Lock='{}' (re-verify detected existing Boom_Lock in row ID: {}). Gate: {} (BOOM1_ID: {})", 
                                                currentBoomLockValue, reportId, gateId, boom1Id);
                                        }
                                        // Redis already updated above, continue to next iteration
                                        continue;
                                    }
                                }
                                
                                // ABSOLUTE FINAL CHECK: Re-query the row one more time to get the CURRENT Boom_Lock value
                                // This is the last line of defense before UPDATE
                                String finalCheckSql = "SELECT Boom_Lock FROM reports WHERE id=?";
                                List<java.util.Map<String, Object>> finalCheckRows = jdbcTemplate1.queryForList(finalCheckSql, reportId);
                                if (finalCheckRows != null && !finalCheckRows.isEmpty()) {
                                    Object finalBoomLockObj = finalCheckRows.get(0).get("Boom_Lock");
                                    String finalBoomLock = null;
                                    if (finalBoomLockObj != null) {
                                        finalBoomLock = finalBoomLockObj.toString().trim();
                                    }
                                    if (finalBoomLock != null && !finalBoomLock.isEmpty()) {
                                        logger.error("ABSOLUTE FINAL CHECK FAILED: Row ID: {} already has Boom_Lock='{}'. ABORTING UPDATE. Will create new row instead. Gate: {} (BOOM1_ID: {})", 
                                            reportId, finalBoomLock, gateId, boom1Id);
                                        // Create new row instead
                                        String ownerSql = "SELECT SM, GM FROM managegates WHERE BOOM1_ID=? LIMIT 1";
                                        List<java.util.Map<String, Object>> ownerRows = jdbcTemplate1.queryForList(ownerSql, boom1Id);
                                        String sm = "";
                                        String gm = "";
                                        if (ownerRows != null && !ownerRows.isEmpty()) {
                                            sm = ownerRows.get(0).get("SM") != null ? ownerRows.get(0).get("SM").toString() : "";
                                            gm = ownerRows.get(0).get("GM") != null ? ownerRows.get(0).get("GM").toString() : "";
                                        }
                                        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                        formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                                        String formattedTime = formatter.format(new java.util.Date());
                                        int insertResult = jdbcTemplate1.update(
                                            "INSERT INTO reports (command, lc_name, sm, gm, added_on, tn, pn, tn_time, wer, lc, lc_status, redy, Boom_Lock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                            "", gateNum, sm, gm, formattedTime, "", "", "", "", boom1Id, "", "s", currentBoomLockValue
                                        );
                                        if (insertResult > 0) {
                                            logger.info("Created NEW report row with Boom_Lock='{}' (absolute final check detected existing Boom_Lock='{}' in row ID: {}). Gate: {} (BOOM1_ID: {})", 
                                                currentBoomLockValue, finalBoomLock, reportId, gateId, boom1Id);
                                        }
                                        continue;
                                    }
                                }
                                
                                // Update the existing report row's Boom_Lock column
                                // CRITICAL: Add WHERE clause to prevent updating rows that already have Boom_Lock
                                // This is a quadruple safety measure - query filter, code check, re-verification, and UPDATE WHERE clause
                                logger.info("Executing UPDATE for row ID: {} with Boom_Lock='{}'. Gate: {} (BOOM1_ID: {})", 
                                    reportId, currentBoomLockValue, gateId, boom1Id);
                                int updateResult = jdbcTemplate1.update(
                                    "UPDATE reports SET Boom_Lock=? WHERE id=? AND (Boom_Lock IS NULL OR Boom_Lock = '')",
                                    currentBoomLockValue, reportId
                                );
                                
                                if (updateResult > 0) {
                                    logger.info("SUCCESS: Updated existing report row (ID: {}) with Boom_Lock='{}' for BOOM1_ID: {} (Gate_Num: {}, status: {}, LT_STATUS: {}).", 
                                        reportId, currentBoomLockValue, boom1Id, gateNum, gateStatus, dbLtStatus);

                                    // LATCH: We have performed the one allowed UPDATE for this CLOSED cycle.
                                    // From now on, any LT flip should INSERT a new row, and repeats must never UPDATE again.
                                    try {
                                        userRepository.setHealthCheckTimestamp(lockUpdatedKey, "1");
                                    } catch (Exception ignore) {}
                                    
                                    // Redis already updated above, no need to update again
                                    
                                    // Verify the value was stored correctly by reading it back
                                    try {
                                        String verifySql = "SELECT Boom_Lock FROM reports WHERE id=?";
                                        List<java.util.Map<String, Object>> verifyRows = jdbcTemplate1.queryForList(verifySql, reportId);
                                        if (verifyRows != null && !verifyRows.isEmpty()) {
                                            String storedValue = (String) verifyRows.get(0).get("Boom_Lock");
                                            logger.info("VERIFY: Stored Boom_Lock value in database: '{}'", storedValue);
                                        }
                                    } catch (Exception verifyEx) {
                                        logger.warn("Could not verify stored Boom_Lock value", verifyEx);
                                    }
                                } else {
                                    logger.error("FAILED: Could not update report row (ID: {}) for BOOM1_ID: {} (Gate_Num: {}). Update result: {}. This may indicate the report was deleted or ID is invalid, or row already has Boom_Lock.", 
                                        reportId, boom1Id, gateNum, updateResult);
                                    // Redis already updated above, no need to update again
                                }
                            } else {
                                // Status changed: CREATE a new report row (NEVER update old rows)
                                // When LT status changes (open/close), always create a NEW row with Boom_Lock
                                // CRITICAL: Do NOT update any existing Closed rows - they should remain unchanged
                                // This includes rows that were created when gate first closed
                                logger.info("Boom_Lock status changed for gate: {} (BOOM1_ID: {}). Previous: {}, Current: {}. Creating NEW report row ONLY (will NEVER update old rows).", 
                                    gateId, boom1Id, previousBoomLockStatus, currentBoomLockStatus);
                                
                                // SAFETY CHECK: Verify we're not accidentally updating any existing row
                                // Even though we're in the "else" block (status changed), double-check no updates happen
                                logger.debug("Status change detected - ensuring no existing rows will be updated for gate: {} (BOOM1_ID: {})", gateId, boom1Id);
                                
                                // DUPLICATE PREVENTION: Check if a row with the same Boom_Lock status was created in the last 30 seconds
                                // This prevents creating duplicate rows when scheduled task runs multiple times quickly
                                // ABSOLUTE RULE: If the latest DB Boom_Lock status is already the same as current status,
                                // then LT has NOT flipped -> NEVER insert a new row.
                                try {
                                    String latestStatusSql =
                                        "SELECT Boom_Lock FROM reports " +
                                        "WHERE (lc IN (?,?) OR lc_name IN (?,?)) " +
                                        "AND Boom_Lock IS NOT NULL AND Boom_Lock != '' " +
                                        "AND added_on >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
                                        "ORDER BY added_on DESC, id DESC LIMIT 1";
                                    List<java.util.Map<String, Object>> latestStatusRows = jdbcTemplate1.queryForList(
                                        latestStatusSql, boom1Id, gateNum, boom1Id, gateNum
                                    );
                                    if (latestStatusRows != null && !latestStatusRows.isEmpty()) {
                                        Object latestObj = latestStatusRows.get(0).get("Boom_Lock");
                                        if (latestObj != null) {
                                            String latestVal = latestObj.toString().trim();
                                            String latestStatusOnly = latestVal.contains("[")
                                                ? latestVal.substring(0, latestVal.indexOf("[")).trim()
                                                : latestVal;
                                            if (!latestStatusOnly.isEmpty() && latestStatusOnly.equalsIgnoreCase(currentBoomLockStatus)) {
                                                logger.info("Skipping INSERT: Latest Boom_Lock status in DB is already '{}' and current is '{}'. No LT flip. Gate: {} (BOOM1_ID: {})",
                                                    latestStatusOnly, currentBoomLockStatus, gateId, boom1Id);
                                                continue;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.warn("Could not fetch latest Boom_Lock status for duplicate guard. BOOM1_ID: {}", boom1Id, e);
                                }
                                boolean duplicateFound = false;
                                try {
                                    String duplicateCheckSql =
                                        "SELECT COUNT(*) as cnt FROM reports " +
                                        "WHERE (lc IN (?,?) OR lc_name IN (?,?)) " +
                                        "AND Boom_Lock = ? " +
                                        "AND added_on >= DATE_SUB(NOW(), INTERVAL 30 SECOND)";
                                    List<java.util.Map<String, Object>> duplicateRows = jdbcTemplate1.queryForList(
                                        duplicateCheckSql, boom1Id, gateNum, boom1Id, gateNum, currentBoomLockValue
                                    );
                                    if (duplicateRows != null && !duplicateRows.isEmpty()) {
                                        Object cntObj = duplicateRows.get(0).get("cnt");
                                        int count = 0;
                                        if (cntObj instanceof Number) {
                                            count = ((Number) cntObj).intValue();
                                        } else if (cntObj != null) {
                                            count = Integer.parseInt(cntObj.toString());
                                        }
                                        duplicateFound = (count > 0);
                                    }
                                } catch (Exception e) {
                                    logger.warn("Could not check for duplicate Boom_Lock rows for BOOM1_ID: {}", boom1Id, e);
                                }
                                
                                if (duplicateFound) {
                                    logger.warn("DUPLICATE PREVENTION: Found existing row with Boom_Lock='{}' created in last 30 seconds. Skipping duplicate creation. Gate: {} (BOOM1_ID: {})", 
                                        currentBoomLockValue, gateId, boom1Id);
                                    // Redis already updated above, no need to create duplicate row
                                    continue;
                                }
                                
                            // Get SM and GM from managegates using BOOM1_ID
                            String ownerSql = "SELECT SM, GM FROM managegates WHERE BOOM1_ID=? LIMIT 1";
                            List<java.util.Map<String, Object>> ownerRows = jdbcTemplate1.queryForList(ownerSql, boom1Id);
                            String sm = "";
                            String gm = "";
                            if (ownerRows != null && !ownerRows.isEmpty()) {
                                sm = ownerRows.get(0).get("SM") != null ? ownerRows.get(0).get("SM").toString() : "";
                                gm = ownerRows.get(0).get("GM") != null ? ownerRows.get(0).get("GM").toString() : "";
                            }
                            
                                // Create new report entry with Boom_Lock status (with timestamp format)
                            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                            String formattedTime = formatter.format(new java.util.Date());
                            
                            int insertResult = jdbcTemplate1.update(
                                "INSERT INTO reports (command, lc_name, sm, gm, added_on, tn, pn, tn_time, wer, lc, lc_status, redy, Boom_Lock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                    "", gateNum, sm, gm, formattedTime, "", "", "", "", boom1Id, "", "s", currentBoomLockValue
                            );
                            
                            if (insertResult > 0) {
                                logger.info("Created new report row with Boom_Lock='{}' for BOOM1_ID: {} (Gate_Num: {}, status: {}, LT_STATUS: {}).", 
                                    currentBoomLockValue, boom1Id, gateNum, gateStatus, dbLtStatus);
                            } else {
                                logger.warn("Failed to create new report for BOOM1_ID: {} (Gate_Num: {})", 
                                    boom1Id, gateNum);
                            }
                            }
                        } catch (Exception updateEx) {
                            logger.error("Error updating/creating report for BOOM1_ID: {} (Gate_Num: {})", 
                                boom1Id, gateNum, updateEx);
                            // Redis already updated above, no need to update again even on error
                        }
                        
                        // Redis already updated at the beginning (before try block) to prevent race conditions
                        // No need to update again here
                    }
                    
                } catch (Exception e) {
                    logger.error("Error checking health for key: {}", healthKey, e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled boom lock health check", e);
        }
    }
    
    /**
     * Fallback scheduled task to ensure Boom_Lock is updated for all Closed gates
     * Runs every 30 seconds to catch any Closed gates that might have been missed
     * This ensures Boom_Lock is always updated even if health check wasn't started properly
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000) // Check every 30 seconds, start after 30 seconds
    public void checkAndUpdateBoomLockForAllClosedGates() {
        try {
            logger.debug("Running fallback Boom_Lock check for all Closed gates");
            
            // Find all gates that are currently Closed
            // For each gate, we'll find the LATEST Closed report row and update it
            // Update regardless of whether train was sent (tn) or not - check both 'Close' and 'Closed' commands
            String findClosedGatesSql =
                "SELECT DISTINCT mg.BOOM1_ID, mg.Gate_Num, mg.LT_STATUS, mg.status " +
                "FROM managegates mg " +
                "WHERE (mg.status IS NOT NULL AND LOWER(TRIM(mg.status)) = 'closed') " +
                "AND EXISTS (" +
                "    SELECT 1 FROM reports r " +
                "    WHERE (r.lc = mg.BOOM1_ID OR r.lc_name = mg.BOOM1_ID OR r.lc = mg.Gate_Num OR r.lc_name = mg.Gate_Num) " +
                "    AND (UPPER(r.command) = 'CLOSED' OR UPPER(r.command) = 'CLOSE') " +
                "    AND r.added_on >= DATE_SUB(NOW(), INTERVAL 1 HOUR)" +
                ")";
            
            List<java.util.Map<String, Object>> closedGates = jdbcTemplate1.queryForList(findClosedGatesSql);
            
            if (closedGates == null || closedGates.isEmpty()) {
                logger.debug("No Closed gates found without Boom_Lock");
                return;
            }
            
            logger.info("Found {} Closed gate(s) without Boom_Lock. Processing...", closedGates.size());
            
            for (java.util.Map<String, Object> gateRow : closedGates) {
                try {
                    String boom1Id = (String) gateRow.get("BOOM1_ID");
                    String gateNum = (String) gateRow.get("Gate_Num");
                    String ltStatus = (String) gateRow.get("LT_STATUS");
                    String gateStatus = (String) gateRow.get("status");
                    
                    if (boom1Id == null || boom1Id.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Check if gate is still closed (status might have changed)
                    if (gateStatus == null || !gateStatus.trim().equalsIgnoreCase("closed")) {
                        logger.debug("Skipping gate {} - status is not 'closed'", boom1Id);
                        continue;
                    }
                    
                    // Determine Boom_Lock status based on LT_STATUS
                    String currentBoomLockStatus = null;
                    if (ltStatus != null && ltStatus.equalsIgnoreCase("closed")) {
                        currentBoomLockStatus = "Healthy";
                    } else {
                        currentBoomLockStatus = "Unhealthy";
                    }
                    
                    // Format timestamp in HH:mm format (IST timezone)
                    java.text.SimpleDateFormat timeFormatter = new java.text.SimpleDateFormat("HH:mm");
                    timeFormatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                    String timeStamp = timeFormatter.format(new java.util.Date());
                    
                    // Format Boom_Lock value with timestamp: "Healthy[HH:mm]" or "Unhealthy[HH:mm]"
                    String currentBoomLockValue = currentBoomLockStatus + "[" + timeStamp + "]";
                    
                    // CRITICAL FIX: First find the ABSOLUTE LATEST Closed row (regardless of Boom_Lock)
                    // Then check if it already has Boom_Lock - if yes, create new row; if no, update it
                    // This prevents finding and updating older rows when the latest row already has Boom_Lock
                    String findLatestReportSql =
                        "SELECT id, added_on, Boom_Lock FROM reports " +
                        "WHERE (lc IN (?,?) OR lc_name IN (?,?)) " +
                        "AND (UPPER(command)='CLOSED' OR UPPER(command)='CLOSE') " +
                        "AND added_on >= DATE_SUB(NOW(), INTERVAL 10 MINUTE) " +
                        "ORDER BY added_on DESC, id DESC LIMIT 1";
                    
                    List<java.util.Map<String, Object>> latestReportRows = jdbcTemplate1.queryForList(
                        findLatestReportSql, boom1Id, gateNum, boom1Id, gateNum
                    );
                    
                    // Check if the latest row already has Boom_Lock
                    boolean latestRowHasBoomLock = false;
                    Integer reportId = null;
                    String rowAddedOn = "unknown";
                    
                    if (latestReportRows != null && !latestReportRows.isEmpty()) {
                        Object boomLockObj = latestReportRows.get(0).get("Boom_Lock");
                        String latestBoomLock = null;
                        if (boomLockObj != null) {
                            latestBoomLock = boomLockObj.toString().trim();
                        }
                        if (latestBoomLock != null && !latestBoomLock.isEmpty()) {
                            latestRowHasBoomLock = true;
                            logger.warn("FALLBACK: LATEST Closed row already has Boom_Lock='{}'. Will create NEW row instead of updating. BOOM1_ID: {} (Gate_Num: {})", 
                                latestBoomLock, boom1Id, gateNum);
                        } else {
                            // Latest row doesn't have Boom_Lock, we can update it
                            try {
                                Object idObj = latestReportRows.get(0).get("id");
                                if (idObj instanceof Number) {
                                    reportId = ((Number) idObj).intValue();
                                } else if (idObj != null) {
                                    reportId = Integer.parseInt(idObj.toString());
                                }
                                rowAddedOn = latestReportRows.get(0).get("added_on") != null ? latestReportRows.get(0).get("added_on").toString() : "unknown";
                            } catch (Exception idEx) {
                                logger.warn("FALLBACK: Could not parse report id from latest row. BOOM1_ID: {}, Gate_Num: {}", boom1Id, gateNum);
                            }
                        }
                    }
                    
                    // IMPORTANT: Fallback must NEVER create new rows.
                    // It exists only to fill Boom_Lock for the latest Closed row that is still missing Boom_Lock.
                    // If the latest Closed row already has Boom_Lock, do NOTHING (otherwise it will print duplicates every 30s).
                    if (latestRowHasBoomLock) {
                        logger.debug("FALLBACK: Skipping BOOM1_ID: {} (Gate_Num: {}) because latest Closed row already has Boom_Lock", boom1Id, gateNum);
                        continue;
                    }
                    
                    // Latest row doesn't have Boom_Lock, proceed with update (one-time fill)
                    if (reportId == null) {
                        logger.warn("FALLBACK: Skipping Boom_Lock update because reportId is null. BOOM1_ID: {}, Gate_Num: {}", boom1Id, gateNum);
                        continue;
                    }
                    
                    // Boom_Lock check already done above (latestRowHasBoomLock check)
                    // Proceed directly to update since we confirmed latest row doesn't have Boom_Lock
                    logger.info("FALLBACK: Updating LATEST Closed row (first time) - ID: {}, added_on: {}, with Boom_Lock: '{}' for BOOM1_ID: {} (Gate_Num: {}, LT_STATUS: {})", 
                        reportId, rowAddedOn, currentBoomLockValue, boom1Id, gateNum, ltStatus);
                    
                    // CRITICAL: Re-verify the row doesn't have Boom_Lock before updating
                    // This prevents race conditions where row gets Boom_Lock between SELECT and UPDATE
                    String verifyBeforeUpdateSql = "SELECT Boom_Lock FROM reports WHERE id=?";
                    List<java.util.Map<String, Object>> verifyBeforeUpdateRows = jdbcTemplate1.queryForList(verifyBeforeUpdateSql, reportId);
                    if (verifyBeforeUpdateRows != null && !verifyBeforeUpdateRows.isEmpty()) {
                        Object verifyBoomLockObj = verifyBeforeUpdateRows.get(0).get("Boom_Lock");
                        String verifyBoomLock = null;
                        if (verifyBoomLockObj != null) {
                            verifyBoomLock = verifyBoomLockObj.toString().trim();
                        }
                        if (verifyBoomLock != null && !verifyBoomLock.isEmpty()) {
                            logger.warn("FALLBACK RE-VERIFY FAILED: Row ID: {} now has Boom_Lock='{}'. Skipping update. BOOM1_ID: {} (Gate_Num: {})", 
                                reportId, verifyBoomLock, boom1Id, gateNum);
                            continue;
                        }
                    }
                    
                    // ABSOLUTE FINAL CHECK: Re-query the row one more time to get the CURRENT Boom_Lock value
                    // This is the last line of defense before UPDATE
                    String finalCheckSql = "SELECT Boom_Lock FROM reports WHERE id=?";
                    List<java.util.Map<String, Object>> finalCheckRows = jdbcTemplate1.queryForList(finalCheckSql, reportId);
                    if (finalCheckRows != null && !finalCheckRows.isEmpty()) {
                        Object finalBoomLockObj = finalCheckRows.get(0).get("Boom_Lock");
                        String finalBoomLock = null;
                        if (finalBoomLockObj != null) {
                            finalBoomLock = finalBoomLockObj.toString().trim();
                        }
                        if (finalBoomLock != null && !finalBoomLock.isEmpty()) {
                            logger.error("FALLBACK ABSOLUTE FINAL CHECK FAILED: Row ID: {} already has Boom_Lock='{}'. ABORTING UPDATE. BOOM1_ID: {} (Gate_Num: {})", 
                                reportId, finalBoomLock, boom1Id, gateNum);
                            continue;
                        }
                    }
                    
                    // Update the report row's Boom_Lock column
                    // CRITICAL: Add WHERE clause to prevent updating rows that already have Boom_Lock
                    // This is a quadruple safety measure - query filter, code check, re-verification, and UPDATE WHERE clause
                    logger.info("FALLBACK: Executing UPDATE for row ID: {} with Boom_Lock='{}'. BOOM1_ID: {} (Gate_Num: {})", 
                        reportId, currentBoomLockValue, boom1Id, gateNum);
                    int updateResult = jdbcTemplate1.update(
                        "UPDATE reports SET Boom_Lock=? WHERE id=? AND (Boom_Lock IS NULL OR Boom_Lock = '')",
                        currentBoomLockValue, reportId
                    );
                    
                    if (updateResult > 0) {
                        logger.info("FALLBACK SUCCESS: Updated report ID: {} with Boom_Lock='{}' for BOOM1_ID: {} (Gate_Num: {}, LT_STATUS: {})",
                            reportId, currentBoomLockValue, boom1Id, gateNum, ltStatus);
                        // Store in Redis for API visibility
                        String lockStatusKey = "boom:lock:status:" + boom1Id;
                        userRepository.setHealthCheckTimestamp(lockStatusKey, currentBoomLockValue);
                    } else {
                        logger.error("FALLBACK FAILED: Could not update report row (ID: {}) for BOOM1_ID: {} (Gate_Num: {}). Update result: {}. This may indicate the report was deleted or ID is invalid, or row already has Boom_Lock.", 
                            reportId, boom1Id, gateNum, updateResult);
                    }
                } catch (Exception e) {
                    String gateIdForError = (String) gateRow.get("BOOM1_ID");
                    logger.error("Error processing gate {} in fallback check", gateIdForError != null ? gateIdForError : "unknown", e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in fallback Boom_Lock check for all Closed gates", e);
        }
    }
}
