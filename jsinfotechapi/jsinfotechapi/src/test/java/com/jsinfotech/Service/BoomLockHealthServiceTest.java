package com.jsinfotech.Service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jsinfotech.Domain.API;

/**
 * Unit tests for BoomLockHealthService
 * Tests 20-second timer check when LS becomes CLOSED
 */
@RunWith(MockitoJUnitRunner.class)
public class BoomLockHealthServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate1;
    
    @Mock
    private RedisUserRepository userRepository;
    
    @Mock
    private GateStatusService gateStatusService;
    
    @InjectMocks
    private BoomLockHealthService boomLockHealthService;
    
    @Test
    public void testStartHealthCheck_StoresTimestamp() {
        // Setup
        String gateId = "TEST-GATE";
        long currentTime = System.currentTimeMillis();
        
        // Execute
        boomLockHealthService.startHealthCheck(gateId);
        
        // Verify: Timestamp should be stored
        verify(userRepository, times(1)).setHealthCheckTimestamp(eq("boom:health:" + gateId), anyString());
        verify(userRepository, times(1)).releaseLock(eq("boom:unhealthy:" + gateId));
    }
    
    @Test
    public void testClearHealthCheck_RemovesKeys() {
        // Setup
        String gateId = "TEST-GATE";
        
        // Execute
        boomLockHealthService.clearHealthCheck(gateId);
        
        // Verify: Health check keys should be released
        verify(userRepository, times(1)).releaseLock(eq("boom:health:" + gateId));
        verify(userRepository, times(1)).releaseLock(eq("boom:unhealthy:" + gateId));
    }
    
    @Test
    public void testCheckBoomLockHealth_LTNotOneAfter20Seconds_LogsUnhealthy() {
        // Setup: 20 seconds elapsed, LT != 1
        String gateId = "TEST-GATE";
        String healthKey = "boom:health:" + gateId;
        Set<String> healthKeys = new HashSet<>();
        healthKeys.add(healthKey);
        
        long lsCloseTime = System.currentTimeMillis() - 25000; // 25 seconds ago
        when(userRepository.getHealthCheckKeys("boom:health:*")).thenReturn(healthKeys);
        when(userRepository.getHealthCheckTimestamp(healthKey)).thenReturn(String.valueOf(lsCloseTime));
        
        // LT value is 0 (not 1)
        when(userRepository.getLastSensorValue(gateId, "LT")).thenReturn(0);
        
        List<Map<String, Object>> statusRows = new ArrayList<>();
        Map<String, Object> statusRow = new java.util.HashMap<>();
        statusRow.put("lt_status", "open");
        statusRows.add(statusRow);
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(statusRows);
        
        when(userRepository.getLock("boom:unhealthy:" + gateId)).thenReturn(null);
        
        // Execute
        boomLockHealthService.checkBoomLockHealth();
        
        // Verify: Should mark as unhealthy (log once)
        verify(userRepository, times(1)).setLock(eq("boom:unhealthy:" + gateId), eq("unhealthy"), eq(3600L));
    }
    
    @Test
    public void testCheckBoomLockHealth_LTIsOneAfter20Seconds_ClearsHealthCheck() {
        // Setup: 20 seconds elapsed, LT == 1 (healthy)
        String gateId = "TEST-GATE";
        String healthKey = "boom:health:" + gateId;
        Set<String> healthKeys = new HashSet<>();
        healthKeys.add(healthKey);
        
        long lsCloseTime = System.currentTimeMillis() - 25000; // 25 seconds ago
        when(userRepository.getHealthCheckKeys("boom:health:*")).thenReturn(healthKeys);
        when(userRepository.getHealthCheckTimestamp(healthKey)).thenReturn(String.valueOf(lsCloseTime));
        
        // LT value is 1 (healthy)
        when(userRepository.getLastSensorValue(gateId, "LT")).thenReturn(1);
        
        List<Map<String, Object>> statusRows = new ArrayList<>();
        Map<String, Object> statusRow = new java.util.HashMap<>();
        statusRow.put("lt_status", "closed");
        statusRows.add(statusRow);
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(statusRows);
        
        // Execute
        boomLockHealthService.checkBoomLockHealth();
        
        // Verify: Should clear health check (LT is healthy)
        verify(userRepository, times(1)).releaseLock(eq("boom:health:" + gateId));
        verify(userRepository, never()).setLock(eq("boom:unhealthy:" + gateId), anyString(), anyLong());
    }
    
    @Test
    public void testCheckBoomLockHealth_LessThan20Seconds_NoAction() {
        // Setup: Less than 20 seconds elapsed
        String gateId = "TEST-GATE";
        String healthKey = "boom:health:" + gateId;
        Set<String> healthKeys = new HashSet<>();
        healthKeys.add(healthKey);
        
        long lsCloseTime = System.currentTimeMillis() - 10000; // 10 seconds ago
        when(userRepository.getHealthCheckKeys("boom:health:*")).thenReturn(healthKeys);
        when(userRepository.getHealthCheckTimestamp(healthKey)).thenReturn(String.valueOf(lsCloseTime));
        
        // Execute
        boomLockHealthService.checkBoomLockHealth();
        
        // Verify: Should not take any action (timer not expired)
        verify(userRepository, never()).setLock(eq("boom:unhealthy:" + gateId), anyString(), anyLong());
        verify(userRepository, never()).releaseLock(eq("boom:health:" + gateId));
    }
    
    @Test
    public void testCheckBoomLockHealth_AlreadyUnhealthy_NoDuplicateLog() {
        // Setup: Already marked unhealthy (duplicate prevention)
        String gateId = "TEST-GATE";
        String healthKey = "boom:health:" + gateId;
        Set<String> healthKeys = new HashSet<>();
        healthKeys.add(healthKey);
        
        long lsCloseTime = System.currentTimeMillis() - 25000;
        when(userRepository.getHealthCheckKeys("boom:health:*")).thenReturn(healthKeys);
        when(userRepository.getHealthCheckTimestamp(healthKey)).thenReturn(String.valueOf(lsCloseTime));
        
        when(userRepository.getLastSensorValue(gateId, "LT")).thenReturn(0);
        
        List<Map<String, Object>> statusRows = new ArrayList<>();
        Map<String, Object> statusRow = new java.util.HashMap<>();
        statusRow.put("lt_status", "open");
        statusRows.add(statusRow);
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(statusRows);
        
        when(userRepository.getLock("boom:unhealthy:" + gateId)).thenReturn("unhealthy"); // Already unhealthy
        
        // Execute
        boomLockHealthService.checkBoomLockHealth();
        
        // Verify: Should not set lock again (prevents duplicate log)
        verify(userRepository, never()).setLock(eq("boom:unhealthy:" + gateId), anyString(), anyLong());
    }
}

