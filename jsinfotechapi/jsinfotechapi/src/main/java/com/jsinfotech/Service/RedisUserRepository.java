package com.jsinfotech.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsinfotech.Domain.API;
import com.jsinfotech.Domain.Priority;
@Service
public class RedisUserRepository {

	
	private HashOperations hashOperations;
	

	@Autowired
    private RedisTemplate redisTemplate;
	
	@Value(value = "${sensor.status.redis.timeout.seconds:30}")
	private long sensorStatusRedisTimeoutSeconds;
	
    private static final Logger logger = LogManager.getLogger(RedisUserRepository.class);


    public RedisUserRepository(RedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setValueSerializer(new StringRedisSerializer());
        this.hashOperations = this.redisTemplate.opsForHash();
    }

    private static String boomPlayKey(String username, String gateNum) {
        String u = (username != null) ? username.trim() : "";
        String g = (gateNum != null) ? gateNum.trim() : "";
        return "boom:play:last:" + u + ":" + g;
    }

    public String getLastBoomPlayCommand(String username, String gateNum) {
        try {
            Object v = this.redisTemplate.opsForValue().get(boomPlayKey(username, gateNum));
            return v != null ? v.toString() : null;
        } catch (Exception e) {
            logger.warn("Failed to read last boom play command (user: {}, gate: {})", username, gateNum, e);
            return null;
        }
    }

    public void setLastBoomPlayCommand(String username, String gateNum, String command, long ttlSeconds) {
        try {
            if (command == null || command.trim().isEmpty()) {
                return;
            }
            long ttl = ttlSeconds > 0 ? ttlSeconds : 10;
            this.redisTemplate.opsForValue().set(boomPlayKey(username, gateNum), command, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Failed to store last boom play command (user: {}, gate: {})", username, gateNum, e);
        }
    }

    public void save(String gate,API user){
        hashOperations.put("1", gate, user);
        logger.info("Hit" +user.getLc_name() );
    }
    
    public void saveTrade(String ticker,Priority user){
    	
    	// Creating Object of ObjectMapper define in Jakson Api
        ObjectMapper Obj = new ObjectMapper();
 
        try {
 
            // get Oraganisation object as a json string
            String jsonStr = Obj.writeValueAsString(user);
            //hashOperations.put(ticker, ticker, jsonStr);
            this.redisTemplate.opsForValue().set(ticker, jsonStr,600000, TimeUnit.SECONDS);
            // Displaying JSON String
            System.out.println(jsonStr);
        }
 
        catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("#######Ticker Saved#######" +ticker);
    }
    
    public String findTrade(String ticker){
       Object trade = hashOperations.get(ticker, ticker);
        logger.info("#######get Ticker#######" +(trade));
       // return (Priority)trade;
        return (String)trade;
    }
    public void saveReportId(String id,String user){
        hashOperations.put("1", id, "T");
        logger.info("report Id for Open" +id);
    }
    
    public String findReportId(String id){
    	
    	Object t = hashOperations.get("1", id) ;
    	if(t==null) {
    		return null;
    	}
        logger.info("Hit retrievied" +t );
        return (String)t ;
    }
    
    public void saveReportIdPN(String id,String pn){
        hashOperations.put("PN", id, pn);
        logger.info("report Id for Open" +id);
    }
    
    public String findReportIdPN(String id){
    	
    	Object t = hashOperations.get("PN", id) ;
    	if(t==null) {
    		return null;
    	}
        logger.info("Hit retrievied" +t );
        return (String)t ;
    }
    
    // ---------------------------------------------------------------------
    // GM expected PN (for lc_pin validation) - stored separately from SM PN.
    // Key: Hash "GM_PN", field: reportId, value: gmPn
    // ---------------------------------------------------------------------
    public void saveReportIdGmPN(String reportId, String pn) {
        hashOperations.put("GM_PN", reportId, pn);
    }

    public String findReportIdGmPN(String reportId) {
        Object t = hashOperations.get("GM_PN", reportId);
        return t != null ? String.valueOf(t) : null;
    }

    public void deleteReportIdGmPN(String reportId) {
        hashOperations.delete("GM_PN", reportId);
    }
    
    public void save_key(String user,String status){
        hashOperations.put("1", user, status);
        
    }
    public List findAll(){
        return hashOperations.values("USER");
    }

    public API findById(String id){
    	
    	Object t = hashOperations.get("1", id) ;
    	if(t==null) {
    		return null;
    	}
        logger.info("Hit retrievied" );
        return (API)t ;
    }

    public void update(String id,API user){
        save(id,user);
    }

    public void push(String user,String type){
    	long t = this.redisTemplate.opsForList().leftPush(user+type, "true".toString());
    	logger.info("pshed"+t);
    	
    	}
    
    public Boolean pop(String user,String type){

        String t = (String)this.redisTemplate.opsForList().leftPop(user+type);
        logger.info("poped"+t + user+type);
        if(t == null) {
        	return false;
        }

    	return Boolean.valueOf(t);
    	}
    
       public void pushAudio(String gate,String event){
    	long t = this.redisTemplate.opsForList().rightPush(gate, event);
    	logger.info("pshed"+t);
    	
    	}
    
       public void pushAudio1(String gate,String event){
       	long t = this.redisTemplate.opsForList().rightPush(gate+"1", event);
      	this.redisTemplate.expire(gate+"1", 6000, TimeUnit.SECONDS);
       	logger.info("pshed11"+t);
       	}
       public String poppAudio1(String gate){

           List<String> t1 = (List<String>)this.redisTemplate.opsForList().range(gate+"1", 0, 0);

           logger.info("poped"+t1);
           if(t1.size()==0) {
           	return "";
           }

       	return t1.get(0);
       	}

      public List<String> popAllAudio1(String gate){
          try {
              List<String> items = (List<String>) this.redisTemplate.opsForList().range(gate+"1", 0, -1);
              if (items == null || items.isEmpty()) {
                  return new java.util.ArrayList<>();
              }
              this.redisTemplate.delete(gate+"1");
              return items;
          } catch (Exception e) {
              logger.warn("Failed to pop all audio1 for key: {}", gate+"1", e);
              return new java.util.ArrayList<>();
          }
      }
    public String poppAudio(String gate){

        String t = (String)this.redisTemplate.opsForList().leftPop(gate);
        logger.info("poped"+gate);
        if(t == null) {
        	return "";
        }

    	return t;
    	}

    public List<String> popAllAudio(String gate){
        try {
            List<String> items = (List<String>) this.redisTemplate.opsForList().range(gate, 0, -1);
            if (items == null || items.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            this.redisTemplate.delete(gate);
            return items;
        } catch (Exception e) {
            logger.warn("Failed to pop all audio for key: {}", gate, e);
            return new java.util.ArrayList<>();
        }
    }
    
    public void poppAudio2(String gate){

    	this.redisTemplate.delete(gate);
        String t = (String)this.redisTemplate.opsForList().leftPop(gate);
    	this.redisTemplate.delete(gate);
        logger.info("poped"+gate);
          

    	}

    
    public void delete(String id){
        hashOperations.delete("USER", id);
    }
    
    public void deletePN(String id){
        hashOperations.delete("PN", id);
    }
    
    /**
     * Store the last update timestamp for an SM in Redis
     * Key format: "sm:lastupdate:{sm}"
     */
    public void setSMLastUpdateTime(String sm) {
        String key = "sm:lastupdate:" + sm;
        long timestamp = System.currentTimeMillis();
        this.redisTemplate.opsForValue().set(key, String.valueOf(timestamp));
        logger.debug("Updated last update time for SM: {}", sm);
    }
    
    /**
     * Get the last update timestamp for an SM from Redis
     * Returns null if SM has never received data
     */
    public Long getSMLastUpdateTime(String sm) {
        String key = "sm:lastupdate:" + sm;
        String value = (String) this.redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.error("Invalid timestamp format for SM: {}", sm, e);
            return null;
        }
    }
    
    /**
     * Set failsafe flag for an SM in Redis
     * Key format: "sm:failsafe:{sm}"
     */
    public void setSMFailsafe(String sm, boolean failsafe) {
        String key = "sm:failsafe:" + sm;
        this.redisTemplate.opsForValue().set(key, String.valueOf(failsafe));
        logger.debug("Set failsafe flag for SM: {} to {}", sm, failsafe);
    }
    
    /**
     * Get failsafe flag for an SM from Redis
     * Returns false if not set
     */
    public boolean getSMFailsafe(String sm) {
        String key = "sm:failsafe:" + sm;
        String value = (String) this.redisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Get all SM keys that have last update timestamps in Redis
     */
    public java.util.Set<String> getAllTrackedSMs() {
        java.util.Set<String> keys = this.redisTemplate.keys("sm:lastupdate:*");
        java.util.Set<String> sms = new java.util.HashSet<>();
        for (String key : keys) {
            // Extract SM from "sm:lastupdate:{sm}"
            String sm = key.replace("sm:lastupdate:", "");
            sms.add(sm);
        }
        return sms;
    }
    
    /**
     * Store the last update timestamp for a gate in Redis
     * Also clears failsafe details if gate was in failsafe mode (gate recovered)
     * Key format: "gate:lastupdate:{gateId}"
     */
    public void setGateLastUpdateTime(String gateId) {
        String key = "gate:lastupdate:" + gateId;
        long timestamp = System.currentTimeMillis();
        this.redisTemplate.opsForValue().set(key, String.valueOf(timestamp));
        logger.debug("Updated last update time for gate: {}", gateId);
        // NOTE:
        // Do NOT clear failsafe details here.
        // For a gate, we track failsafe per-sensor (BS1/BS2/LS). With "failsafe if ANY sensor missing",
        // a single sensor update does NOT mean the gate recovered.
        // Recovery + eviction of failsafe state must be handled centrally in SMFailsafeService
        // when ALL sensors for the gate are healthy again.
    }
    
    /**
     * Get the last update timestamp for a gate from Redis
     * Returns null if gate has never received data
     */
    public Long getGateLastUpdateTime(String gateId) {
        String key = "gate:lastupdate:" + gateId;
        String value = (String) this.redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.error("Invalid timestamp format for gate: {}", gateId, e);
            return null;
        }
    }
    
    /**
     * Get all gate IDs that have last update timestamps in Redis
     */
    public java.util.Set<String> getAllTrackedGates() {
        java.util.Set<String> keys = this.redisTemplate.keys("gate:lastupdate:*");
        java.util.Set<String> gateIds = new java.util.HashSet<>();
        for (String key : keys) {
            // Extract gateId from "gate:lastupdate:{gateId}"
            String gateId = key.replace("gate:lastupdate:", "");
            gateIds.add(gateId);
        }
        return gateIds;
    }
    
    /**
     * Try to acquire a distributed lock using Redis SETNX pattern
     * Key format: "lock:{lockName}"
     * 
     * @param lockName The name of the lock
     * @param lockTimeoutSeconds Lock expiration time in seconds (auto-releases after this time)
     * @return true if lock was acquired, false if lock already exists
     */
    public boolean tryAcquireLock(String lockName, long lockTimeoutSeconds) {
        String lockKey = "lock:" + lockName;
        try {
            // Use setIfAbsent (SETNX equivalent) to atomically set the lock
            Boolean acquired = this.redisTemplate.opsForValue().setIfAbsent(lockKey, "locked");
            if (acquired != null && acquired) {
                // Set expiration so lock auto-releases even if process crashes
                this.redisTemplate.expire(lockKey, lockTimeoutSeconds, TimeUnit.SECONDS);
                logger.debug("Acquired distributed lock: {}", lockName);
                return true;
            } else {
                logger.debug("Failed to acquire lock (already held): {}", lockName);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error acquiring lock: {}", lockName, e);
            return false;
        }
    }
    
    /**
     * Release a distributed lock
     * 
     * @param lockName The name of the lock to release
     */
    public void releaseLock(String lockName) {
        String lockKey = "lock:" + lockName;
        try {
            this.redisTemplate.delete(lockKey);
            logger.debug("Released distributed lock: {}", lockName);
        } catch (Exception e) {
            logger.error("Error releasing lock: {}", lockName, e);
        }
    }
    
    /**
     * Store failsafe details for a gate in Redis
     * Stores gateId, gateName, SM, and GM in a hash structure
     * Also maintains Redis sets as indexes for efficient lookups
     * Key format: "failsafe:gate:{gateId}"
     * 
     * @param gateId The gate ID
     * @param gateName The gate name
     * @param sm The Station Master (SM) associated with this gate
     * @param gm The Gate Master (GM) associated with this gate
     */
    public void setFailsafeGateDetails(String gateId, String gateName, String sm, String gm) {
        String key = "failsafe:gate:" + gateId;
        try {
            hashOperations.put(key, "gateId", gateId);
            hashOperations.put(key, "gateName", gateName != null ? gateName : "");
            hashOperations.put(key, "sm", sm != null ? sm : "");
            hashOperations.put(key, "gm", gm != null ? gm : "");
            hashOperations.put(key, "timestamp", String.valueOf(System.currentTimeMillis()));
            // IMPORTANT: Do NOT reset reportCreatedAt on every scheduler tick.
            // If this gate is already in failsafe and a NO-NETWORK report was created, we must preserve it
            // to prevent repeated prints until recovery.
            Object existingReportCreatedAt = hashOperations.get(key, "reportCreatedAt");
            if (existingReportCreatedAt == null) {
            hashOperations.put(key, "reportCreatedAt", "");
            }
            Object existingRecovered = hashOperations.get(key, "recovered");
            if (existingRecovered == null) {
            hashOperations.put(key, "recovered", "false");
            }
            
            // Maintain Redis sets as indexes for efficient lookups
            if (sm != null && !sm.isEmpty()) {
                String smSetKey = "failsafe:sm:" + sm + ":gates";
                this.redisTemplate.opsForSet().add(smSetKey, gateId);
            }
            if (gm != null && !gm.isEmpty()) {
                String gmSetKey = "failsafe:gm:" + gm + ":gates";
                this.redisTemplate.opsForSet().add(gmSetKey, gateId);
            }
            
            logger.info("Stored failsafe details in Redis - GateId: {}, GateName: {}, SM: {}, GM: {}", gateId, gateName, sm, gm);
        } catch (Exception e) {
            logger.error("Error storing failsafe details for gate: {}", gateId, e);
        }
    }
    
    /**
     * Mark that a failsafe report was created for a gate
     * This tracks when the NO-NETWORK report was inserted into database
     */
    public void setFailsafeReportCreated(String gateId) {
        String key = "failsafe:gate:" + gateId;
        try {
            hashOperations.put(key, "reportCreatedAt", String.valueOf(System.currentTimeMillis()));
            hashOperations.put(key, "recovered", "false");
            logger.debug("Marked failsafe report as created for gate: {}", gateId);
        } catch (Exception e) {
            logger.error("Error marking failsafe report as created for gate: {}", gateId, e);
        }
    }
    
    /**
     * Check if a failsafe report was already created for this gate
     * Returns true if report was created and gate hasn't recovered yet
     */
    public boolean hasFailsafeReportBeenCreated(String gateId) {
        String key = "failsafe:gate:" + gateId;
        try {
            Object reportCreatedAtObj = hashOperations.get(key, "reportCreatedAt");
            Object recoveredObj = hashOperations.get(key, "recovered");
            String reportCreatedAt = reportCreatedAtObj != null ? reportCreatedAtObj.toString() : null;
            String recovered = recoveredObj != null ? recoveredObj.toString() : null;
            // Report exists if reportCreatedAt is not null/empty AND gate hasn't recovered
            return reportCreatedAt != null && !reportCreatedAt.isEmpty() && 
                   (recovered == null || !"true".equalsIgnoreCase(recovered));
        } catch (Exception e) {
            logger.error("Error checking if failsafe report was created for gate: {}", gateId, e);
            return false;
        }
    }
    
    /**
     * Mark a gate as recovered (values came back after failsafe)
     * This allows a new failsafe report to be created if values stop again
     */
    public void markGateAsRecovered(String gateId) {
        String key = "failsafe:gate:" + gateId;
        try {
            hashOperations.put(key, "recovered", "true");
            logger.debug("Marked gate {} as recovered - values came back", gateId);
        } catch (Exception e) {
            logger.error("Error marking gate as recovered: {}", gateId, e);
        }
    }
    
    /**
     * Get failsafe details for a gate from Redis
     * Returns a map with gateId, gateName, sm, and timestamp
     * 
     * @param gateId The gate ID
     * @return Map containing failsafe details or null if not found
     */
    public java.util.Map<String, String> getFailsafeGateDetails(String gateId) {
        String key = "failsafe:gate:" + gateId;
        try {
            java.util.Map<Object, Object> details = hashOperations.entries(key);
            if (details == null || details.isEmpty()) {
                return null;
            }
            java.util.Map<String, String> result = new java.util.HashMap<>();
            for (java.util.Map.Entry<Object, Object> entry : details.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
            return result;
        } catch (Exception e) {
            logger.error("Error retrieving failsafe details for gate: {}", gateId, e);
            return null;
        }
    }
    
    /**
     * Remove failsafe details for a gate from Redis (when gate recovers)
     * Also removes gateId from SM and GM index sets
     * 
     * @param gateId The gate ID
     */
    public void removeFailsafeGateDetails(String gateId) {
        String key = "failsafe:gate:" + gateId;
        try {
            // Get details before deleting to remove from index sets
            java.util.Map<String, String> details = getFailsafeGateDetails(gateId);
            if (details != null) {
                String sm = details.get("sm");
                String gm = details.get("gm");
                
                // Remove from SM index set
                if (sm != null && !sm.isEmpty()) {
                    String smSetKey = "failsafe:sm:" + sm + ":gates";
                    this.redisTemplate.opsForSet().remove(smSetKey, gateId);
                }
                
                // Remove from GM index set
                if (gm != null && !gm.isEmpty()) {
                    String gmSetKey = "failsafe:gm:" + gm + ":gates";
                    this.redisTemplate.opsForSet().remove(gmSetKey, gateId);
                }
            }
            
            this.redisTemplate.delete(key);
            logger.debug("Removed failsafe details for gate: {}", gateId);
        } catch (Exception e) {
            logger.error("Error removing failsafe details for gate: {}", gateId, e);
        }
    }
    
    /**
     * Get all gates that are currently in failsafe mode
     * Returns a set of gate IDs
     */
    public java.util.Set<String> getAllFailsafeGates() {
        java.util.Set<String> keys = this.redisTemplate.keys("failsafe:gate:*");
        java.util.Set<String> gateIds = new java.util.HashSet<>();
        for (String key : keys) {
            // Extract gateId from "failsafe:gate:{gateId}"
            String gateId = key.replace("failsafe:gate:", "");
            gateIds.add(gateId);
        }
        return gateIds;
    }
    
    /**
     * Get all gates in failsafe mode for a specific SM
     * Uses efficient Redis set lookup instead of scanning all gates
     * Returns a list of gate details (gateId, gateName, SM, GM) for the given SM
     */
    public java.util.List<java.util.Map<String, String>> getFailsafeGatesForSM(String sm) {
        java.util.List<java.util.Map<String, String>> result = new java.util.ArrayList<>();
        try {
            if (sm == null || sm.isEmpty()) {
                return result;
            }
            
            // Use efficient Redis set lookup
            String smSetKey = "failsafe:sm:" + sm + ":gates";
            java.util.Set<String> gateIds = this.redisTemplate.opsForSet().members(smSetKey);
            
            if (gateIds != null) {
                for (String gateId : gateIds) {
                    java.util.Map<String, String> details = getFailsafeGateDetails(gateId);
                    if (details != null) {
                        result.add(details);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting failsafe gates for SM: {}", sm, e);
        }
        return result;
    }
    
    /**
     * Get all gates in failsafe mode for a specific GM
     * Uses efficient Redis set lookup instead of scanning all gates
     * Returns a list of gate details (gateId, gateName, SM, GM) for the given GM
     */
    public java.util.List<java.util.Map<String, String>> getFailsafeGatesForGM(String gm) {
        java.util.List<java.util.Map<String, String>> result = new java.util.ArrayList<>();
        try {
            if (gm == null || gm.isEmpty()) {
                return result;
            }
            
            // Use efficient Redis set lookup
            String gmSetKey = "failsafe:gm:" + gm + ":gates";
            java.util.Set<String> gateIds = this.redisTemplate.opsForSet().members(gmSetKey);
            
            if (gateIds != null) {
                for (String gateId : gateIds) {
                    java.util.Map<String, String> details = getFailsafeGateDetails(gateId);
                    if (details != null) {
                        result.add(details);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting failsafe gates for GM: {}", gm, e);
        }
        return result;
    }
    
    /**
     * Store sensor status in Redis for a given SM and direction
     * Key format: "sensor:status:{sm}:{direction}"
     * Stores status 1 WITHOUT TTL - persists until status 0 explicitly deletes it
     * This ensures status 1 won't create new records until status 0 is received
     * 
     * @param sm The Station Master (SM) identifier
     * @param direction The direction (UP or DN)
     * @param status The status value (1 or 0)
     */
    public void setSensorStatus(String sm, String direction, String status) {
        String key = "sensor:status:" + sm + ":" + direction;
        try {
            // Store status 1 WITHOUT TTL - it will persist until status 0 deletes it
            // This prevents new records from being created after any time period
            // Only when status 0 is sent will the key be deleted, allowing new status 1 to create records
            if ("1".equals(status)) {
                // Store without expiration - persists until explicitly deleted by status 0
                this.redisTemplate.opsForValue().set(key, status);
                logger.info("Stored sensor status 1 for SM: {}, Direction: {} - No TTL (will persist until status 0)", sm, direction);
            }
            // For status 0, we don't store it - the calling code handles deletion via deleteSensorStatus()
        } catch (Exception e) {
            logger.error("Error storing sensor status for SM: {}, Direction: {}", sm, direction, e);
        }
    }
    
    /**
     * Get sensor status from Redis for a given SM and direction
     * Returns the status string or null if not found/expired
     * 
     * @param sm The Station Master (SM) identifier
     * @param direction The direction (UP or DN)
     * @return Status string ("1" or "0") or null if not found
     */
    public String getSensorStatus(String sm, String direction) {
        String key = "sensor:status:" + sm + ":" + direction;
        try {
            String value = (String) this.redisTemplate.opsForValue().get(key);
            return value;
        } catch (Exception e) {
            logger.error("Error getting sensor status for SM: {}, Direction: {}", sm, direction, e);
            return null;
        }
    }
    
    /**
     * Delete/evict sensor status from Redis for a given SM and direction
     * This is called when status changes from 1 to 0, so that the next status 1 will be treated as a new event
     * 
     * @param sm The Station Master (SM) identifier
     * @param direction The direction (UP or DN)
     */
    public void deleteSensorStatus(String sm, String direction) {
        String key = "sensor:status:" + sm + ":" + direction;
        try {
            this.redisTemplate.delete(key);
            logger.debug("Evicted sensor status for SM: {}, Direction: {}", sm, direction);
        } catch (Exception e) {
            logger.error("Error deleting sensor status for SM: {}, Direction: {}", sm, direction, e);
        }
    }
    
    /**
     * Store last known sensor value for a gate
     * Key format: "sensor:last:{gateId}:{sensorType}"
     * 
     * @param gateId The gate ID
     * @param sensorType The sensor type (BS, BS2, LS, LT)
     * @param value The sensor value
     */
    public void setLastSensorValue(String gateId, String sensorType, Integer value) {
        String key = "sensor:last:" + gateId + ":" + sensorType;
        try {
            this.redisTemplate.opsForValue().set(key, String.valueOf(value), sensorStatusRedisTimeoutSeconds, TimeUnit.SECONDS);
            logger.debug("Stored last known {} value for gate {}: {}", sensorType, gateId, value);
        } catch (Exception e) {
            logger.error("Error storing last sensor value for gate: {}, sensor: {}", gateId, sensorType, e);
        }
    }
    
    /**
     * Get last known sensor value for a gate
     * Key format: "sensor:last:{gateId}:{sensorType}"
     * 
     * @param gateId The gate ID
     * @param sensorType The sensor type (BS, BS2, LS, LT)
     * @return The last known sensor value, or null if not found
     */
    public Integer getLastSensorValue(String gateId, String sensorType) {
        String key = "sensor:last:" + gateId + ":" + sensorType;
        try {
            String value = (String) this.redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            logger.error("Error getting last sensor value for gate: {}, sensor: {}", gateId, sensorType, e);
            return null;
        }
    }
    
    /**
     * Set a lock with expiration time
     * Key format: "lock:{lockName}"
     * 
     * @param lockName The name of the lock
     * @param value The lock value
     * @param timeoutSeconds Lock expiration time in seconds
     */
    public void setLock(String lockName, String value, long timeoutSeconds) {
        String lockKey = "lock:" + lockName;
        try {
            this.redisTemplate.opsForValue().set(lockKey, value, timeoutSeconds, TimeUnit.SECONDS);
            logger.debug("Set lock: {} with timeout: {} seconds", lockName, timeoutSeconds);
        } catch (Exception e) {
            logger.error("Error setting lock: {}", lockName, e);
        }
    }
    
    /**
     * Get a lock value
     * Key format: "lock:{lockName}"
     * 
     * @param lockName The name of the lock
     * @return The lock value, or null if not found/expired
     */
    public String getLock(String lockName) {
        String lockKey = "lock:" + lockName;
        try {
            return (String) this.redisTemplate.opsForValue().get(lockKey);
        } catch (Exception e) {
            logger.error("Error getting lock: {}", lockName, e);
            return null;
        }
    }
    
    /**
     * Get handle status for a gate from Redis cache
     * 
     * @param gateId The gate ID
     * @return The handle status, or null if not found
     */
    public String getHandleStatus(String gateId) {
        try {
            API api = findById(gateId);
            if (api != null) {
                return api.getLeverStatus();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting handle status for gate: {}", gateId, e);
            return null;
        }
    }
    
    /**
     * Set health check timestamp for boom lock health monitoring
     * Key format: "boom:health:{gateId}"
     * 
     * @param key The health check key
     * @param timestamp The timestamp as string
     */
    public void setHealthCheckTimestamp(String key, String timestamp) {
        try {
            this.redisTemplate.opsForValue().set(key, timestamp);
            logger.debug("Set health check timestamp for key: {}", key);
        } catch (Exception e) {
            logger.error("Error setting health check timestamp for key: {}", key, e);
        }
    }
    
    /**
     * Get health check timestamp
     * Key format: "boom:health:{gateId}"
     * 
     * @param key The health check key
     * @return The timestamp as string, or null if not found
     */
    public String getHealthCheckTimestamp(String key) {
        try {
            return (String) this.redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Error getting health check timestamp for key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Get all health check keys matching a pattern
     * 
     * @param pattern The pattern to match (e.g., "boom:health:*")
     * @return Set of matching keys
     */
    public java.util.Set<String> getHealthCheckKeys(String pattern) {
        try {
            return this.redisTemplate.keys(pattern);
        } catch (Exception e) {
            logger.error("Error getting health check keys for pattern: {}", pattern, e);
            return new java.util.HashSet<>();
        }
    }
    
    /**
     * Delete a health check key from Redis
     * Key format: "boom:health:{gateId}" or "boom:lock:status:{gateId}"
     * 
     * @param key The health check key to delete
     */
    public void deleteHealthCheckKey(String key) {
        try {
            this.redisTemplate.delete(key);
            logger.debug("Deleted health check key: {}", key);
        } catch (Exception e) {
            logger.error("Error deleting health check key: {}", key, e);
        }
    }
    
    /**
     * Store OTP for a phone number in Redis with expiration
     * Key format: "otp:{phoneNumber}"
     * 
     * @param phoneNumber The phone number
     * @param otp The OTP value
     * @param expiryMinutes Expiration time in minutes
     */
    public void saveOTP(String phoneNumber, String otp, long expiryMinutes) {
        String key = "otp:" + phoneNumber;
        try {
            this.redisTemplate.opsForValue().set(key, otp, expiryMinutes, TimeUnit.MINUTES);
            logger.debug("Saved OTP for phone number: {} with expiry: {} minutes", phoneNumber, expiryMinutes);
        } catch (Exception e) {
            logger.error("Error saving OTP for phone number: {}", phoneNumber, e);
        }
    }
    
    /**
     * Get OTP for a phone number from Redis
     * Key format: "otp:{phoneNumber}"
     * 
     * @param phoneNumber The phone number
     * @return The OTP value, or null if not found/expired
     */
    public String getOTP(String phoneNumber) {
        String key = "otp:" + phoneNumber;
        try {
            return (String) this.redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Error getting OTP for phone number: {}", phoneNumber, e);
            return null;
        }
    }
    
    /**
     * Delete OTP for a phone number from Redis
     * Key format: "otp:{phoneNumber}"
     * 
     * @param phoneNumber The phone number
     */
    public void deleteOTP(String phoneNumber) {
        String key = "otp:" + phoneNumber;
        try {
            this.redisTemplate.delete(key);
            logger.debug("Deleted OTP for phone number: {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Error deleting OTP for phone number: {}", phoneNumber, e);
        }
    }
    
    /**
     * Mark a PN as used for a gate in Redis with 24-hour expiration
     * Key format: "pn:used:{gateId}:{pn}"
     * 
     * @param gateId The gate ID
     * @param pn The PN value
     */
    public void markPNAsUsed(String gateId, String pn) {
        String key = "pn:used:" + gateId + ":" + pn;
        try {
            // Store with 24-hour expiration (86400 seconds)
            this.redisTemplate.opsForValue().set(key, "1", 86400, TimeUnit.SECONDS);
            logger.debug("Marked PN {} as used for gate: {} (24-hour TTL)", pn, gateId);
        } catch (Exception e) {
            logger.error("Error marking PN as used for gate: {}, PN: {}", gateId, pn, e);
        }
    }
    
    /**
     * Check if a PN was used in the last 24 hours for a gate
     * Key format: "pn:used:{gateId}:{pn}"
     * 
     * @param gateId The gate ID
     * @param pn The PN value to check
     * @return true if PN was used in last 24 hours, false otherwise
     */
    public boolean isPNUsedInLast24Hours(String gateId, String pn) {
        String key = "pn:used:" + gateId + ":" + pn;
        try {
            String value = (String) this.redisTemplate.opsForValue().get(key);
            boolean isUsed = (value != null);
            logger.debug("PN {} for gate {} - used in last 24h: {}", pn, gateId, isUsed);
            return isUsed;
        } catch (Exception e) {
            logger.error("Error checking if PN was used for gate: {}, PN: {}", gateId, pn, e);
            return false;
        }
    }
    
    /**
     * Get all PNs used for a gate from Redis (within last 24 hours)
     * Returns set of PN values that are currently tracked in Redis
     * 
     * @param gateId The gate ID
     * @return Set of PN strings used for the gate
     */
    public java.util.Set<String> getUsedPNsForGate(String gateId) {
        java.util.Set<String> usedPNs = new java.util.HashSet<>();
        try {
            String pattern = "pn:used:" + gateId + ":*";
            java.util.Set<String> keys = this.redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    // Extract PN from key format: "pn:used:{gateId}:{pn}"
                    String pn = key.substring(key.lastIndexOf(":") + 1);
                    usedPNs.add(pn);
                }
            }
            logger.debug("Found {} used PNs for gate: {} in Redis", usedPNs.size(), gateId);
        } catch (Exception e) {
            logger.error("Error getting used PNs for gate: {}", gateId, e);
        }
        return usedPNs;
    }

}
