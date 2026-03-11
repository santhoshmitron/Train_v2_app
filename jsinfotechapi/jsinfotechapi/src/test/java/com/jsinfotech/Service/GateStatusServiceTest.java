package com.jsinfotech.Service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jsinfotech.Domain.API;

/**
 * Unit tests for GateStatusService
 * Tests 4-sensor CLOSED check (BS1, BS2, LT, LS)
 */
@RunWith(MockitoJUnitRunner.class)
public class GateStatusServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate1;
    
    @Mock
    private RedisUserRepository userRepository;
    
    @InjectMocks
    private GateStatusService gateStatusService;
    
    private List<Map<String, Object>> mockRows;
    
    @Before
    public void setUp() {
        mockRows = new ArrayList<>();
    }
    
    @Test
    public void testIsGateClosed_AllSensorsClosed_ReturnsTrue() {
        // Setup: All 4 sensors are closed
        Map<String, Object> row = new HashMap<>();
        row.put("BS1_STATUS", "closed");
        row.put("BS2_STATUS", "closed");
        row.put("LT_STATUS", "closed");
        row.put("LEVER_STATUS", "closed");
        mockRows.add(row);
        
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(mockRows);
        
        // Execute
        boolean result = gateStatusService.isGateClosed("TEST-GATE");
        
        // Verify
        assertTrue("Gate should be closed when all 4 sensors are closed", result);
    }
    
    @Test
    public void testIsGateClosed_OneSensorOpen_ReturnsFalse() {
        // Setup: BS1 is open, others closed
        Map<String, Object> row = new HashMap<>();
        row.put("BS1_STATUS", "open");
        row.put("BS2_STATUS", "closed");
        row.put("LT_STATUS", "closed");
        row.put("LEVER_STATUS", "closed");
        mockRows.add(row);
        
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(mockRows);
        
        // Execute
        boolean result = gateStatusService.isGateClosed("TEST-GATE");
        
        // Verify
        assertFalse("Gate should not be closed when BS1 is open", result);
    }
    
    @Test
    public void testIsGateClosed_BS2Open_ReturnsFalse() {
        // Setup: BS2 is open, others closed
        Map<String, Object> row = new HashMap<>();
        row.put("BS1_STATUS", "closed");
        row.put("BS2_STATUS", "open");
        row.put("LT_STATUS", "closed");
        row.put("LEVER_STATUS", "closed");
        mockRows.add(row);
        
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(mockRows);
        
        // Execute
        boolean result = gateStatusService.isGateClosed("TEST-GATE");
        
        // Verify
        assertFalse("Gate should not be closed when BS2 is open", result);
    }
    
    @Test
    public void testIsGateClosed_LTOpen_ReturnsFalse() {
        // Setup: LT is open, others closed
        Map<String, Object> row = new HashMap<>();
        row.put("BS1_STATUS", "closed");
        row.put("BS2_STATUS", "closed");
        row.put("LT_STATUS", "open");
        row.put("LEVER_STATUS", "closed");
        mockRows.add(row);
        
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(mockRows);
        
        // Execute
        boolean result = gateStatusService.isGateClosed("TEST-GATE");
        
        // Verify
        assertFalse("Gate should not be closed when LT is open", result);
    }
    
    @Test
    public void testIsGateClosed_LSOpen_ReturnsFalse() {
        // Setup: LS is open, others closed
        Map<String, Object> row = new HashMap<>();
        row.put("BS1_STATUS", "closed");
        row.put("BS2_STATUS", "closed");
        row.put("LT_STATUS", "closed");
        row.put("LEVER_STATUS", "open");
        mockRows.add(row);
        
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(mockRows);
        
        // Execute
        boolean result = gateStatusService.isGateClosed("TEST-GATE");
        
        // Verify
        assertFalse("Gate should not be closed when LS is open", result);
    }
    
    @Test
    public void testIsGateClosed_NullValues_DefaultsToOpen_ReturnsFalse() {
        // Setup: NULL values should default to "open"
        Map<String, Object> row = new HashMap<>();
        row.put("gate_status", null);
        row.put("bs2_status", null);
        row.put("lt_status", null);
        row.put("handle_status", null);
        mockRows.add(row);
        
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(mockRows);
        
        // Execute
        boolean result = gateStatusService.isGateClosed("TEST-GATE");
        
        // Verify
        assertFalse("Gate should not be closed when all sensors are NULL (defaults to open)", result);
    }
    
    @Test
    public void testIsGateClosed_GateNotFound_ReturnsFalse() {
        // Setup: No gate found
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(new ArrayList<>());
        
        // Execute
        boolean result = gateStatusService.isGateClosed("NONEXISTENT-GATE");
        
        // Verify
        assertFalse("Gate should not be closed when gate not found", result);
    }
    
    @Test
    public void testGetGateSensorStatuses_ReturnsAllSensorStatuses() {
        // Setup
        Map<String, Object> row = new HashMap<>();
        row.put("gateId", "TEST-GATE");
        row.put("BS1_STATUS", "closed");
        row.put("BS2_STATUS", "closed");
        row.put("LT_STATUS", "closed");
        row.put("LEVER_STATUS", "closed");
        row.put("gateName", "Test Gate");
        row.put("SM", "sm1");
        row.put("GM", "gm1");
        mockRows.add(row);
        
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(mockRows);
        
        // Execute
        API result = gateStatusService.getGateSensorStatuses("TEST-GATE");
        
        // Verify
        assertNotNull("API object should not be null", result);
        assertEquals("Gate ID should match", "TEST-GATE", result.getGate());
        assertEquals("BS1 status should be closed", "closed", result.getBs1Status());
        assertEquals("BS2 status should be closed", "closed", result.getBs2Status());
        assertEquals("LT status should be closed", "closed", result.getLtStatus());
        assertEquals("LS status should be closed", "closed", result.getLeverStatus());
    }
    
    @Test
    public void testGetGateSensorStatuses_NullValues_DefaultsToOpen() {
        // Setup: NULL values
        Map<String, Object> row = new HashMap<>();
        row.put("gateId", "TEST-GATE");
        row.put("gate_status", null);
        row.put("bs2_status", null);
        row.put("lt_status", null);
        row.put("handle_status", null);
        row.put("gateName", "Test Gate");
        row.put("SM", "sm1");
        row.put("GM", "gm1");
        mockRows.add(row);
        
        when(jdbcTemplate1.queryForList(anyString(), anyString(), anyString())).thenReturn(mockRows);
        
        // Execute
        API result = gateStatusService.getGateSensorStatuses("TEST-GATE");
        
        // Verify
        assertNotNull("API object should not be null", result);
        assertEquals("BS1 status should default to open", "open", result.getBs1Status());
        assertEquals("BS2 status should default to open", "open", result.getBs2Status());
        assertEquals("LT status should default to open", "open", result.getLtStatus());
        assertEquals("LS status should default to open", "open", result.getLeverStatus());
    }
}

