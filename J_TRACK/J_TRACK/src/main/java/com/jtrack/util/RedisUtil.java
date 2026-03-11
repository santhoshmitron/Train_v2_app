package com.jtrack.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import com.google.gson.Gson;
import com.jtrack.pojo.J_Message;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.actor.ActorContext;

import java.util.concurrent.TimeUnit;
import java.util.List;

public class RedisUtil {
    
    private static JedisPool jedisPool;
    private static Config config;
    private static Gson gson = new Gson();
    
    static {
        config = ConfigFactory.load();
        initializePool();
    }
    
    private static void initializePool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(config.getInt("jtrack.redis.pool.maxTotal"));
        poolConfig.setMaxIdle(config.getInt("jtrack.redis.pool.maxIdle"));
        poolConfig.setMinIdle(config.getInt("jtrack.redis.pool.minIdle"));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        String host = config.getString("jtrack.redis.host");
        int port = config.getInt("jtrack.redis.port");
        int timeout = config.getInt("jtrack.redis.timeout");
        String password = config.getString("jtrack.redis.password");
        int database = config.getInt("jtrack.redis.database");
        
        if (password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port, timeout, null, database);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
        }
    }
    
    public static void publishCloseEvent(J_Message message, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String channel = "jtrack:close:events";
            String messageJson = gson.toJson(message);
            
            long subscribers = jedis.publish(channel, messageJson);
            log.info("Published close event to Redis channel '{}' with {} subscribers. Message: {}", 
                    channel, subscribers, messageJson);
            
        } catch (Exception e) {
            log.error(e, "Failed to publish close event to Redis: {}", e.getMessage());
        }
    }
    
    public static void publishCloseEvent(String gateId, String gateName, String status, LoggingAdapter log) {
        J_Message message = new J_Message(gateName, gateId, status);
        publishCloseEvent(message, log);
    }
    
    public static void publishCloseEvent(String gateId, String gateName, String status, 
                                       Boolean failsafeMethod, Boolean isFailsafe, LoggingAdapter log) {
        J_Message message = new J_Message(gateId, failsafeMethod, isFailsafe, gateName, status);
        publishCloseEvent(message, log);
    }
    
    public static void setGateStatus(String gateId, String status, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "jtrack:gate:status:" + gateId;
            jedis.setex(key, 3600, status); // Expire after 1 hour
            log.info("Set gate status in Redis - GateId: {}, Status: {}", gateId, status);
        } catch (Exception e) {
            log.error(e, "Failed to set gate status in Redis: {}", e.getMessage());
        }
    }
    
    public static String getGateStatus(String gateId, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "jtrack:gate:status:" + gateId;
            return jedis.get(key);
        } catch (Exception e) {
            log.error(e, "Failed to get gate status from Redis: {}", e.getMessage());
            return null;
        }
    }
    
    // Queue Operations - Similar to jsinfotechapi pushAudio/popAudio approach
    
    /**
     * Push audio/notification message to user queue - Similar to jsinfotechapi pushAudio
     * @param username The username/gate operator
     * @param message The audio/notification message
     * @param log Logger adapter
     */
    public static void pushAudio(String username, String message, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = username; // Use username as key like jsinfotechapi
            long length = jedis.rpush(key, message);
            log.info("Pushed audio message - User: {}, Message: {}, Queue Length: {}", username, message, length);
        } catch (Exception e) {
            log.error(e, "Failed to push audio message to Redis: {}", e.getMessage());
        }
    }
    
    /**
     * Pop audio message from user queue (FIFO) - Similar to jsinfotechapi poppAudio
     * Once popped, the message is removed from the queue
     * @param username The username/gate operator
     * @param log Logger adapter
     * @return The audio message or empty string if none
     */
    public static String popAudio(String username, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = username; // Use username as key like jsinfotechapi
            String message = jedis.lpop(key);
            if (message != null) {
                log.info("Popped audio message - User: {}, Message: {}", username, message);
                return message;
            }
            log.info("poped{}", username);
            return "";
        } catch (Exception e) {
            log.error(e, "Failed to pop audio message from Redis: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Save report ID for tracking - Similar to jsinfotechapi saveReportId
     * @param reportId The report ID
     * @param status The status (e.g., "T" for tracked)
     * @param log Logger adapter
     */
    public static void saveReportId(String reportId, String status, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "1"; // Use "1" as hash key like jsinfotechapi
            jedis.hset(key, reportId, status);
            log.info("Saved tracked report - ReportId: {}, Status: {}", reportId, status);
        } catch (Exception e) {
            log.error(e, "Failed to save tracked report to Redis: {}", e.getMessage());
        }
    }
    
    /**
     * Find tracked report - Similar to jsinfotechapi findReportId
     * @param reportId The report ID
     * @param log Logger adapter
     * @return Status or null if not found
     */
    public static String findReportId(String reportId, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "1"; // Use "1" as hash key like jsinfotechapi
            String status = jedis.hget(key, reportId);
            if (status != null) {
                log.info("Retrieved tracked report - ReportId: {}, Status: {}", reportId, status);
            }
            return status;
        } catch (Exception e) {
            log.error(e, "Failed to find tracked report in Redis: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Save PN report - Similar to jsinfotechapi saveReportIdPN
     * @param reportId The report ID
     * @param pnValue The PN value
     * @param log Logger adapter
     */
    public static void saveReportIdPN(String reportId, String pnValue, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "PN"; // Use "PN" as hash key like jsinfotechapi
            jedis.hset(key, reportId, pnValue);
            log.info("Saved PN report - ReportId: {}, PN: {}", reportId, pnValue);
        } catch (Exception e) {
            log.error(e, "Failed to save PN report to Redis: {}", e.getMessage());
        }
    }
    
    /**
     * Find PN report - Similar to jsinfotechapi findReportIdPN
     * @param reportId The report ID
     * @param log Logger adapter
     * @return PN value or null if not found
     */
    public static String findReportIdPN(String reportId, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "PN"; // Use "PN" as hash key like jsinfotechapi
            String pnValue = jedis.hget(key, reportId);
            if (pnValue != null) {
                log.info("Retrieved PN report - ReportId: {}, PN: {}", reportId, pnValue);
            }
            return pnValue;
        } catch (Exception e) {
            log.error(e, "Failed to find PN report in Redis: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Delete PN report - Similar to jsinfotechapi deletePN
     * @param reportId The report ID to delete
     * @param log Logger adapter
     */
    public static void deletePN(String reportId, LoggingAdapter log) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "PN"; // Use "PN" as hash key like jsinfotechapi
            long deleted = jedis.hdel(key, reportId);
            log.info("Deleted PN report - ReportId: {}, Deleted: {}", reportId, deleted > 0);
        } catch (Exception e) {
            log.error(e, "Failed to delete PN report from Redis: {}", e.getMessage());
        }
    }
    
    /**
     * Update queue with status notifications - Similar to jsinfotechapi ackService.updateQueue
     * This routes different status types to appropriate queues
     * @param username The username/gate operator
     * @param status The status (ACK, closed, open, PN, etc.)
     * @param gateId The gate ID/name
     * @param log Logger adapter
     */
    public static void updateQueue(String username, String status, String gateId, LoggingAdapter log) {
        String message = "";
        
        log.info("Updating queue - User: {}, Status: {}, Gate: {}", username, status, gateId);
        
        switch (status) {
            case "ACK":
                message = gateId + " acknowledged";
                break;
            case "closed":
                message = gateId + " Closed";
                break;
            case "open":
                message = gateId + " Opened";
                break;
            case "PN":
                message = gateId + " P N received";
                break;
            default:
                log.warning("Unknown status for queue update: {}", status);
                return;
        }
        
        pushAudio(username, message, log);
        log.info("Queue updated - User: {}, Status: {}, Gate: {}, Message: {}", username, status, gateId, message);
    }
    
    public static void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}