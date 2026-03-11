package com.jsinfotech.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled service to check for SMs that haven't received data for the configured timeout
 * and update their gates to "open" status with failsafe flag set to true.
 * 
 * Uses distributed locking to ensure only one instance runs the check in a load-balanced environment.
 */
@Component
public class SMFailsafeScheduler {

    private static final Logger logger = LogManager.getLogger(SMFailsafeScheduler.class);
    
    private static final String LOCK_NAME = "sm-failsafe-check";
    private static final long LOCK_TIMEOUT_SECONDS = 60; // Lock expires after 60 seconds

    @Autowired
    private SMFailsafeService smFailsafeService;
    
    @Autowired
    private RedisUserRepository redisUserRepository;

    /**
     * Check for stale SMs every 30 seconds
     * Fixed delay of 30 seconds (30000 milliseconds)
     * Initial delay of 60 seconds to allow application to start up
     * 
     * Uses distributed lock to ensure only one instance executes this in a load-balanced setup
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 60000)
    public void checkStaleSMs() {
        // Try to acquire distributed lock - only one instance will succeed
        boolean lockAcquired = redisUserRepository.tryAcquireLock(LOCK_NAME, LOCK_TIMEOUT_SECONDS);
        
        if (!lockAcquired) {
            logger.debug("Skipping scheduled check - another instance is already running it");
            return;
        }
        
        try {
            logger.debug("Running scheduled check for stale SMs (lock acquired)...");
            smFailsafeService.checkAndActivateFailsafe();
        } catch (Exception e) {
            logger.error("Error in scheduled SM failsafe check", e);
        } finally {
            // Release the lock when done
            redisUserRepository.releaseLock(LOCK_NAME);
        }
    }
}

