package com.jtrack.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import com.google.gson.Gson;
import com.jtrack.pojo.J_Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;

/**
 * Redis subscriber utility to listen for close events
 * This can be used for testing or as a separate service to consume close events
 */
public class RedisSubscriber {
    
    private static JedisPool jedisPool;
    private static Config config;
    private static Gson gson = new Gson();
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    
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
    
    /**
     * Subscribe to close events and process them
     */
    public static void subscribeToCloseEvents() {
        executor.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String channel = "jtrack:close:events";
                System.out.println("Subscribing to Redis channel: " + channel);
                
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        System.out.println("Received close event from channel: " + channel);
                        System.out.println("Message: " + message);
                        
                        try {
                            // Parse the J_Message from JSON
                            J_Message closeEvent = gson.fromJson(message, J_Message.class);
                            System.out.println("Parsed close event: " + closeEvent.toString());
                            
                            // Process the close event here
                            processCloseEvent(closeEvent);
                            
                        } catch (Exception e) {
                            System.err.println("Error processing close event: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        System.out.println("Successfully subscribed to channel: " + channel);
                    }
                    
                    @Override
                    public void onUnsubscribe(String channel, int subscribedChannels) {
                        System.out.println("Unsubscribed from channel: " + channel);
                    }
                }, channel);
                
            } catch (Exception e) {
                System.err.println("Error subscribing to Redis: " + e.getMessage());
            }
        });
    }
    
    /**
     * Process the received close event
     */
    private static void processCloseEvent(J_Message closeEvent) {
        System.out.println("Processing close event:");
        System.out.println("  Gate ID: " + closeEvent.getGateId());
        System.out.println("  Gate Name: " + closeEvent.getUsername());
        System.out.println("  Status: " + closeEvent.getClose());
        System.out.println("  Failsafe Method: " + closeEvent.getFailsafe_method());
        System.out.println("  Is Failsafe: " + closeEvent.getIsfailsafe());
        
        // Add your custom processing logic here
        // For example: send notifications, update external systems, etc.
    }
    
    /**
     * Get current gate status from Redis
     */
    public static String getGateStatus(String gateId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "jtrack:gate:status:" + gateId;
            return jedis.get(key);
        } catch (Exception e) {
            System.err.println("Error getting gate status from Redis: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * List all gate statuses
     */
    public static void listAllGateStatuses() {
        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("Current gate statuses:");
            Set<String> keys = jedis.keys("jtrack:gate:status:*");
            for (String key : keys) {
                String status = jedis.get(key);
                String gateId = key.replace("jtrack:gate:status:", "");
                System.out.println("  Gate ID: " + gateId + " -> Status: " + status);
            }
        } catch (Exception e) {
            System.err.println("Error listing gate statuses: " + e.getMessage());
        }
    }
    
    /**
     * Close the subscriber and cleanup resources
     */
    public static void close() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
    
    /**
     * Main method for testing the subscriber
     */
    public static void main(String[] args) {
        System.out.println("Starting Redis subscriber for close events...");
        subscribeToCloseEvents();
        
        // Keep the program running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("Subscriber interrupted");
        } finally {
            close();
        }
    }
}
