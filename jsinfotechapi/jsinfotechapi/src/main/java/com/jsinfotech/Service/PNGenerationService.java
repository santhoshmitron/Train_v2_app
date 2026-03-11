package com.jsinfotech.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PNGenerationService {

    private static final Logger logger = LogManager.getLogger(PNGenerationService.class);

    /**
     * Allowed 3-digit PN values with no '0' anywhere (digits 1-9 only).
     * Size = 9^3 = 729.
     */
    private static final java.util.List<Integer> ALLOWED_3_DIGIT_PNS;
    static {
        java.util.List<Integer> allowed = new java.util.ArrayList<Integer>(729);
        for (int i = 111; i <= 999; i++) {
            if (isAllowed(i)) {
                allowed.add(i);
            }
        }
        ALLOWED_3_DIGIT_PNS = Collections.unmodifiableList(allowed);
    }
    
    @Autowired
    private JdbcTemplate jdbcTemplate1;
    
    @Autowired
    private RedisUserRepository userRepository;
    
    @Autowired
    private GateStatusService gateStatusService;
    
    /**
     * Generate PN automatically when gate transitions to CLOSED
     * Only generates if no PN exists for the pending report
     * Uses Redis lock to prevent duplicate generation
     * 
     * @param gateId The gate ID
     * @param gm Gate Manager username
     * @param sm Station Master username
     */
    public void generatePNIfNeeded(String gateId, String gm, String sm) {
        GateIdentifiers gateIdentifiers = resolveGateIdentifiers(gateId);
        String canonicalGateKey = gateIdentifiers.getCanonicalGateKey();
        String lockKey = "pn:lock:" + canonicalGateKey;
        
        // Try to acquire lock (5 second timeout)
        boolean lockAcquired = false;
        try {
            // Simple lock mechanism using Redis
            String existingLock = userRepository.getLock(lockKey);
            if (existingLock == null) {
                userRepository.setLock(lockKey, "locked", 5); // 5 second lock
                lockAcquired = true;
            }
            
            if (!lockAcquired) {
                logger.debug("PN generation already in progress for gate: {}", gateId);
                return;
            }
            
            // Check if gate is actually closed
            boolean isClosed = gateStatusService.isGateClosed(gateId);
            logger.info("PN Generation: Gate closed check for {}: {}", gateId, isClosed);
            if (!isClosed) {
                logger.warn("PN Generation: Gate {} is not closed (BS1, BS2, or LS is open), skipping PN generation", gateId);
                return;
            }
            
            // Query for pending report with command='Close' or 'Closed' and pn='' for this gate
            // Find the most recent Close/Closed report with empty PN, regardless of when it was created
            // Query: Check both lc (BOOM1_ID) and lc_name (Gate_Num) fields, command='Close' or 'Closed' (case-insensitive), and pn is empty
            // Primary match on lc field (BOOM1_ID) as that's what we insert
            // Note: Accepts both 'Close' and 'Closed' commands, and handles empty lc_status (for newly created reports)
            String sql = "SELECT id FROM reports WHERE (lc = ? OR lc_name = ? OR lc = ? OR lc_name = ?) AND (UPPER(command) = 'CLOSE' OR UPPER(command) = 'CLOSED') AND (lc_status IS NULL OR lc_status = '' OR UPPER(lc_status) = 'CLOSED') AND (pn IS NULL OR pn = '') AND gm = ? ORDER BY added_on DESC LIMIT 1";
            logger.info("PN Generation: Searching for most recent pending report with gateKey={}, keys={}, gm={} (no time window restriction)",
                canonicalGateKey, gateIdentifiers.getAllKeysForLookup(), gm);
            
            List<Map<String, Object>> rows = null;
            int retryCount = 0;
            int maxRetries = 2;
            
            // Retry logic: query up to 2 times with delay if not found (to handle transaction timing)
            while (retryCount < maxRetries && (rows == null || rows.isEmpty())) {
                if (retryCount > 0) {
                    logger.info("PN Generation: Retry attempt {} for gate: {}", retryCount, gateId);
                    try {
                        Thread.sleep(200); // 200ms delay between retries
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                String k1 = gateIdentifiers.getPrimaryLookupKey();
                String k2 = gateIdentifiers.getSecondaryLookupKey();
                rows = jdbcTemplate1.queryForList(sql, k1, k1, k2, k2, gm);
                retryCount++;
                
                if (rows != null && !rows.isEmpty()) {
                    logger.info("PN Generation: Found report on attempt {} for gate: {}", retryCount, gateId);
                    break;
                }
            }
            
            if (rows == null || rows.isEmpty()) {
                logger.warn("PN Generation: No pending report found for gate: {}, canonicalGateKey: {}, gm: {} after {} attempts (no time restriction)",
                    gateId, canonicalGateKey, gm, retryCount);
                // Debug: Check what reports exist for this gate
                try {
                    String debugSql = "SELECT id, lc, lc_name, command, lc_status, pn, gm, added_on FROM reports WHERE (lc = ? OR lc_name = ? OR lc = ? OR lc_name = ?) AND gm = ? ORDER BY added_on DESC LIMIT 5";
                    String k1 = gateIdentifiers.getPrimaryLookupKey();
                    String k2 = gateIdentifiers.getSecondaryLookupKey();
                    List<Map<String, Object>> debugRows = jdbcTemplate1.queryForList(debugSql, k1, k1, k2, k2, gm);
                    if (debugRows != null && !debugRows.isEmpty()) {
                        logger.info("PN Generation: Found {} reports for gate {} (but none match PN criteria):", debugRows.size(), gateId);
                        for (Map<String, Object> debugRow : debugRows) {
                            logger.info("  Report ID: {}, lc: {}, lc_name: {}, command: {}, lc_status: {}, pn: {}, added_on: {}", 
                                debugRow.get("id"), debugRow.get("lc"), debugRow.get("lc_name"), 
                                debugRow.get("command"), debugRow.get("lc_status"), debugRow.get("pn"), debugRow.get("added_on"));
                        }
                    } else {
                        logger.warn("PN Generation: No reports found at all for gate: {}, gm: {}", gateId, gm);
                    }
                } catch (Exception e) {
                    logger.error("Error in debug query for PN generation", e);
                }
                return;
            }
            
            String reportId = String.valueOf(rows.get(0).get("id"));
            logger.info("PN Generation: Found pending report ID: {} for gate: {} (BOOM1_ID)", reportId, gateId);
            
            // Check if PN already exists in Redis
            String existingPN = userRepository.findReportIdPN(reportId);
            if (existingPN != null && !existingPN.isEmpty()) {
                logger.info("PN Generation: PN already exists in Redis for report ID: {}, PN: {}. Skipping generation.", reportId, existingPN);
                // Also check if it's in database
                try {
                    String checkDbSql = "SELECT pn FROM reports WHERE id = ?";
                    List<Map<String, Object>> pnRows = jdbcTemplate1.queryForList(checkDbSql, reportId);
                    if (pnRows != null && !pnRows.isEmpty()) {
                        String dbPn = (String) pnRows.get(0).get("pn");
                        if (dbPn != null && !dbPn.isEmpty()) {
                            logger.info("PN Generation: PN also exists in database for report ID: {}, PN: {}", reportId, dbPn);
                        } else {
                            logger.warn("PN Generation: PN exists in Redis but not in database for report ID: {}. Updating database.", reportId);
                            int updated = jdbcTemplate1.update("UPDATE reports SET pn = ? WHERE id = ?", existingPN, reportId);
                            if (updated > 0) {
                                logger.info("PN Generation: Updated database with PN: {} for report ID: {}", existingPN, reportId);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error checking PN in database for report: {}", reportId, e);
                }
                return;
            }
            
            // Generate PN with 24-hour uniqueness check
            logger.info("PN Generation: Generating new PN for report ID: {}, gate: {}", reportId, gateId);
            String pnStr = generateUniquePNForGate(canonicalGateKey, gateIdentifiers);
            logger.info("PN Generation: Generated PN: {} for report ID: {}", pnStr, reportId);
            
            // Mark PN as used in Redis (24-hour TTL)
            try {
                // Mark under canonical + any known aliases (Gate_Num) so callers using either key stay unique
                for (String gateKey : gateIdentifiers.getAllKeysForLookup()) {
                    userRepository.markPNAsUsed(gateKey, pnStr);
                }
                logger.info("PN Generation: Marked PN {} as used for gate keys: {} (24-hour TTL)", pnStr, gateIdentifiers.getAllKeysForLookup());
            } catch (Exception e) {
                logger.error("PN Generation: Failed to mark PN as used in Redis for gate: {}, PN: {}", gateId, pnStr, e);
            }
            
            // Save to Redis (for report ID mapping)
            try {
            userRepository.saveReportIdPN(reportId, pnStr);
                logger.info("PN Generation: Saved PN: {} to Redis for report ID: {}", pnStr, reportId);
            } catch (Exception e) {
                logger.error("PN Generation: Failed to save PN to Redis for report ID: {}", reportId, e);
            }
            
            // Update database
            int updated = jdbcTemplate1.update("UPDATE reports SET pn = ? WHERE id = ?", pnStr, reportId);
            
            if (updated > 0) {
                logger.info("PN Generation: SUCCESS - PN {} generated and saved to database for gate: {} (BOOM1_ID), report ID: {}", pnStr, gateId, reportId);
            } else {
                logger.error("PN Generation: FAILED - Could not update PN in database for report ID: {}. Update result: {}", reportId, updated);
            }
            
        } catch (Exception e) {
            logger.error("Error generating PN for gate: {}", gateId, e);
        } finally {
            // Release lock
            if (lockAcquired) {
                userRepository.releaseLock(lockKey);
            }
        }
    }
    
    /**
     * Generate SM PN at train-send time (when report is created with command=Close) even if the gate is not yet closed.
     * - Finds the most recent Close/Closed report with a train number and empty pn for this gate+gm
     * - Generates a 3-digit no-zero PN with 24h uniqueness per gate
     * - Persists to DB (`reports.pn`) and Redis mapping (`PN` hash: reportId -> pn)
     *
     * @param gateId The gate identifier (BOOM1_ID / Gate_Num / handle)
     * @param gm Gate Manager username
     * @param sm Station Master username (not used for query, kept for logging symmetry)
     */
    public void generateSMTrainPNIfMissing(String gateId, String gm, String sm) {
        GateIdentifiers gateIdentifiers = resolveGateIdentifiers(gateId);
        String canonicalGateKey = gateIdentifiers.getCanonicalGateKey();
        String lockKey = "pn:lock:" + canonicalGateKey;

        boolean lockAcquired = false;
        try {
            String existingLock = userRepository.getLock(lockKey);
            if (existingLock == null) {
                userRepository.setLock(lockKey, "locked", 5);
                lockAcquired = true;
            }
            if (!lockAcquired) {
                logger.debug("SM PN generation already in progress for gate: {}", canonicalGateKey);
                return;
            }

            // Find the most recent train report needing PN (no gate-closed requirement)
            String sql =
                "SELECT id FROM reports " +
                "WHERE (lc = ? OR lc_name = ? OR lc = ? OR lc_name = ?) " +
                "AND (UPPER(command) = 'CLOSE' OR UPPER(command) = 'CLOSED') " +
                "AND (tn IS NOT NULL AND tn != '') " +
                "AND (pn IS NULL OR pn = '') " +
                "AND gm = ? " +
                "ORDER BY added_on DESC LIMIT 1";

            String k1 = gateIdentifiers.getPrimaryLookupKey();
            String k2 = gateIdentifiers.getSecondaryLookupKey();
            List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, k1, k1, k2, k2, gm);

            if (rows == null || rows.isEmpty()) {
                logger.debug("SM PN generation: no train report found needing PN for gateKey={}, keys={}, gm={}",
                    canonicalGateKey, gateIdentifiers.getAllKeysForLookup(), gm);
                return;
            }

            String reportId = String.valueOf(rows.get(0).get("id"));

            // If PN already mapped in Redis, ensure DB has it and exit
            String existingPN = userRepository.findReportIdPN(reportId);
            if (existingPN != null && !existingPN.trim().isEmpty()) {
                try {
                    int updated = jdbcTemplate1.update("UPDATE reports SET pn = ? WHERE id = ? AND (pn IS NULL OR pn = '')", existingPN, reportId);
                    if (updated > 0) {
                        logger.info("SM PN generation: Backfilled DB pn='{}' for reportId={}", existingPN, reportId);
                    }
                } catch (Exception e) {
                    logger.warn("SM PN generation: failed to backfill DB PN for reportId={}", reportId, e);
                }
                return;
            }

            // Generate + reserve PN
            String pnStr = generateUniquePNForGate(canonicalGateKey, gateIdentifiers);
            try {
                for (String gateKey : gateIdentifiers.getAllKeysForLookup()) {
                    userRepository.markPNAsUsed(gateKey, pnStr);
                }
            } catch (Exception e) {
                logger.warn("SM PN generation: failed to mark PN as used in Redis for gateKey={}, pn={}", canonicalGateKey, pnStr, e);
            }

            // Persist to Redis mapping and DB
            try {
                userRepository.saveReportIdPN(reportId, pnStr);
            } catch (Exception e) {
                logger.warn("SM PN generation: failed to save PN mapping in Redis for reportId={}", reportId, e);
            }

            int updated = jdbcTemplate1.update("UPDATE reports SET pn = ? WHERE id = ? AND (pn IS NULL OR pn = '')", pnStr, reportId);
            if (updated > 0) {
                logger.info("SM PN generation: SUCCESS - pn='{}' stored for reportId={}, gateKey={}, gm={}, sm={}",
                    pnStr, reportId, canonicalGateKey, gm, sm);
            } else {
                logger.warn("SM PN generation: no DB update occurred (pn maybe already set) for reportId={}, gateKey={}", reportId, canonicalGateKey);
            }

        } catch (Exception e) {
            logger.error("Error generating SM PN at train-send for gate: {}", gateId, e);
        } finally {
            if (lockAcquired) {
                userRepository.releaseLock(lockKey);
            }
        }
    }
    
    /**
     * Generate a random 3-digit PN (111-999) with no zeros in any digit, ensuring 24-hour uniqueness per gate.
     * Digits allowed: 1-9 only (e.g., 101 is NOT allowed).
     * 
     * @param gateIdentifier A gate identifier (BOOM1_ID, Gate_Num, handle, etc.)
     * @return A unique 3-digit PN string without zeros
     */
    public String generateUniquePNForGate(String gateIdentifier) {
        GateIdentifiers ids = resolveGateIdentifiers(gateIdentifier);
        return generateUniquePNForGate(ids.getCanonicalGateKey(), ids);
    }
    
    /**
     * Get all gate lookup keys for a gate identifier (for marking PNs as used)
     * This ensures we mark PNs for the same keys that generateUniquePNForGate checks
     * 
     * @param gateIdentifier A gate identifier (BOOM1_ID, Gate_Num, handle, etc.)
     * @return Set of all gate keys that should be checked/marked for uniqueness
     */
    public java.util.Set<String> getAllGateKeysForIdentifier(String gateIdentifier) {
        GateIdentifiers ids = resolveGateIdentifiers(gateIdentifier);
        return ids.getAllKeysForLookup();
    }

    private String generateUniquePNForGate(String canonicalGateKey, GateIdentifiers ids) {
        // Get PNs used in last 24 hours from database
        Set<String> usedPNsFromDB = getUsedPNsInLast24Hours(ids);
        
        // Get PNs used in last 24 hours from Redis
        Set<String> usedPNsFromRedis = new HashSet<>();
        for (String gateKey : ids.getAllKeysForLookup()) {
            usedPNsFromRedis.addAll(userRepository.getUsedPNsForGate(gateKey));
        }
        
        // Combine both sets
        Set<String> allUsedPNs = new HashSet<>();
        allUsedPNs.addAll(usedPNsFromDB);
        allUsedPNs.addAll(usedPNsFromRedis);
        
        logger.info("PN Generation: Found {} used PNs in last 24 hours for gateKey: {} (keys={}, DB: {}, Redis: {})",
            allUsedPNs.size(), canonicalGateKey, ids.getAllKeysForLookup(), usedPNsFromDB.size(), usedPNsFromRedis.size());
        
        // Remove used PNs from valid list
        java.util.List<Integer> availablePNs = new java.util.ArrayList<Integer>();
        for (Integer pn : ALLOWED_3_DIGIT_PNS) {
            String pnStr = String.valueOf(pn);
            if (!allUsedPNs.contains(pnStr)) {
                availablePNs.add(pn);
            }
        }
        
        if (availablePNs.isEmpty()) {
            logger.warn("PN Generation: All valid 3-digit no-zero PNs are used for gateKey: {} in last 24 hours. Using first valid PN: 111", canonicalGateKey);
            return "111"; // Extreme fallback
        }
        
        // Randomly select from available PNs
        Random r = new Random();
        int randomIndex = r.nextInt(availablePNs.size());
        int selectedPN = availablePNs.get(randomIndex);
        
        logger.info("PN Generation: Selected PN: {} from {} available PNs for gateKey: {}", selectedPN, availablePNs.size(), canonicalGateKey);
        return String.valueOf(selectedPN);
    }
    
    /**
     * Check if a number is allowed (3 digits, 111-999, no '0' in any digit)
     * 
     * @param number The number to check
     * @return true if number is valid, false otherwise
     */
    private static boolean isAllowed(int number) {
        if (number < 111 || number > 999) {
            return false;
        }
        String s = String.valueOf(number);
        return (s.length() == 3) && (s.indexOf('0') < 0);
    }
    
    /**
     * Get PNs used in the last 24 hours for a gate from database
     * 
     * @param gateId The gate ID
     * @return Set of PN strings used in last 24 hours
     */
    private Set<String> getUsedPNsInLast24Hours(GateIdentifiers ids) {
        Set<String> usedPNs = new HashSet<>();
        try {
            // Query for PNs used in last 24 hours for this gate
            String sql = "SELECT DISTINCT pn FROM reports WHERE (lc = ? OR lc_name = ? OR lc = ? OR lc_name = ?) AND pn IS NOT NULL AND pn != '' AND added_on >= DATE_SUB(NOW(), INTERVAL 24 HOUR)";
            String k1 = ids.getPrimaryLookupKey();
            String k2 = ids.getSecondaryLookupKey();
            List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, k1, k1, k2, k2);
            
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    String pn = (String) row.get("pn");
                    if (pn != null && !pn.trim().isEmpty()) {
                        usedPNs.add(pn.trim());
                    }
                }
            }
            
            logger.debug("PN Generation: Found {} PNs used in last 24 hours from database for gate keys: {}", usedPNs.size(), ids.getAllKeysForLookup());
        } catch (Exception e) {
            logger.error("PN Generation: Error querying used PNs from database for gate keys: {}", ids.getAllKeysForLookup(), e);
        }
        return usedPNs;
    }

    private GateIdentifiers resolveGateIdentifiers(String gateId) {
        if (gateId == null || gateId.trim().isEmpty()) {
            return GateIdentifiers.fromSingleKey("");
        }
        String trimmed = gateId.trim();
        try {
            // Try to resolve BOOM1_ID and Gate_Num for this gate identifier so uniqueness works across both.
            // Some flows store BOOM1_ID in reports.lc, others store Gate_Num in reports.lc/lc_name.
            String sql = "SELECT BOOM1_ID, Gate_Num FROM managegates WHERE BOOM1_ID = ? OR Gate_Num = ? OR BOOM2_ID = ? OR handle = ? LIMIT 1";
            List<Map<String, Object>> rows = jdbcTemplate1.queryForList(sql, trimmed, trimmed, trimmed, trimmed);
            if (rows != null && !rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                String boom1 = row.get("BOOM1_ID") != null ? String.valueOf(row.get("BOOM1_ID")) : null;
                String gateNum = row.get("Gate_Num") != null ? String.valueOf(row.get("Gate_Num")) : null;
                return GateIdentifiers.from(trimmed, boom1, gateNum);
            }
        } catch (Exception e) {
            logger.debug("PN Generation: Failed to resolve gate identifiers for: {}", gateId, e);
        }
        return GateIdentifiers.fromSingleKey(trimmed);
    }

    private static final class GateIdentifiers {
        private final String inputKey;
        private final String boom1Id;
        private final String gateNum;
        private final String canonicalGateKey;

        private GateIdentifiers(String inputKey, String boom1Id, String gateNum, String canonicalGateKey) {
            this.inputKey = inputKey;
            this.boom1Id = boom1Id;
            this.gateNum = gateNum;
            this.canonicalGateKey = canonicalGateKey;
        }

        static GateIdentifiers fromSingleKey(String key) {
            String k = key == null ? "" : key.trim();
            return new GateIdentifiers(k, null, null, k);
        }

        static GateIdentifiers from(String inputKey, String boom1Id, String gateNum) {
            String in = inputKey == null ? "" : inputKey.trim();
            String b1 = (boom1Id != null && !boom1Id.trim().isEmpty()) ? boom1Id.trim() : null;
            String gn = (gateNum != null && !gateNum.trim().isEmpty()) ? gateNum.trim() : null;
            String canonical = b1 != null ? b1 : (gn != null ? gn : in);
            return new GateIdentifiers(in, b1, gn, canonical);
        }

        String getCanonicalGateKey() {
            return canonicalGateKey;
        }

        /**
         * Primary lookup key for DB queries / report matching. Prefer BOOM1_ID if available.
         */
        String getPrimaryLookupKey() {
            return (boom1Id != null && !boom1Id.isEmpty()) ? boom1Id : inputKey;
        }

        /**
         * Secondary lookup key for DB queries / report matching. Prefer Gate_Num if available.
         */
        String getSecondaryLookupKey() {
            String secondary = (gateNum != null && !gateNum.isEmpty()) ? gateNum : inputKey;
            String primary = getPrimaryLookupKey();
            return secondary != null ? secondary : primary;
        }

        Set<String> getAllKeysForLookup() {
            Set<String> keys = new HashSet<>();
            if (inputKey != null && !inputKey.isEmpty()) {
                keys.add(inputKey);
            }
            if (boom1Id != null && !boom1Id.isEmpty()) {
                keys.add(boom1Id);
            }
            if (gateNum != null && !gateNum.isEmpty()) {
                keys.add(gateNum);
            }
            if (canonicalGateKey != null && !canonicalGateKey.isEmpty()) {
                keys.add(canonicalGateKey);
            }
            if (keys.isEmpty()) {
                keys.add("");
            }
            return keys;
        }
    }
}

