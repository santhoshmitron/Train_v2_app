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

/**
 * Unit tests for PNGenerationService
 * Tests PN generation on transition, duplicate prevention, and Redis locking
 */
@RunWith(MockitoJUnitRunner.class)
public class PNGenerationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate1;
    
    @Mock
    private RedisUserRepository userRepository;
    
    @Mock
    private GateStatusService gateStatusService;
    
    @InjectMocks
    private PNGenerationService pnGenerationService;
    
    private List<Map<String, Object>> mockReportRows;
    
    @Before
    public void setUp() {
        mockReportRows = new ArrayList<>();
    }
    
    @Test
    public void testGeneratePNIfNeeded_GateClosed_NoExistingPN_GeneratesPN() {
        // Setup: Gate is closed, no existing PN, no lock
        when(userRepository.getLock("pn:lock:TEST-GATE")).thenReturn(null);
        when(gateStatusService.isGateClosed("TEST-GATE")).thenReturn(true);
        
        Map<String, Object> reportRow = new HashMap<>();
        reportRow.put("id", 123);
        mockReportRows.add(reportRow);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> castedRows = mockReportRows;
        when(jdbcTemplate1.queryForList(anyString(), any(), any(), any())).thenReturn((List) castedRows);
        
        when(userRepository.findReportIdPN("123")).thenReturn(null);
        when(jdbcTemplate1.update(anyString(), anyString(), anyString())).thenReturn(1);
        
        // Execute
        pnGenerationService.generatePNIfNeeded("TEST-GATE", "gm1", "sm1");
        
        // Verify: PN should be generated and saved
        verify(userRepository, times(1)).setLock(eq("pn:lock:TEST-GATE"), eq("locked"), eq(5L));
        verify(userRepository, times(1)).saveReportIdPN(eq("123"), anyString());
        verify(jdbcTemplate1, times(1)).update(eq("UPDATE reports SET pn = ? WHERE id = ?"), anyString(), eq("123"));
        verify(userRepository, times(1)).releaseLock(eq("pn:lock:TEST-GATE"));
    }
    
    @Test
    public void testGeneratePNIfNeeded_GateNotClosed_SkipsGeneration() {
        // Setup: Gate is not closed
        when(userRepository.getLock("pn:lock:TEST-GATE")).thenReturn(null);
        when(gateStatusService.isGateClosed("TEST-GATE")).thenReturn(false);
        
        // Execute
        pnGenerationService.generatePNIfNeeded("TEST-GATE", "gm1", "sm1");
        
        // Verify: Should not generate PN
        verify(userRepository, never()).saveReportIdPN(anyString(), anyString());
        verify(jdbcTemplate1, never()).update(contains("UPDATE reports SET pn"), any(), any());
    }
    
    @Test
    public void testGeneratePNIfNeeded_LockAlreadyExists_SkipsGeneration() {
        // Setup: Lock already exists (another thread is generating)
        when(userRepository.getLock("pn:lock:TEST-GATE")).thenReturn("locked");
        
        // Execute
        pnGenerationService.generatePNIfNeeded("TEST-GATE", "gm1", "sm1");
        
        // Verify: Should not generate PN (lock prevents duplicate)
        verify(userRepository, never()).saveReportIdPN(anyString(), anyString());
        verify(jdbcTemplate1, never()).update(contains("UPDATE reports SET pn"), any(), any());
    }
    
    @Test
    public void testGeneratePNIfNeeded_ExistingPN_SkipsGeneration() {
        // Setup: PN already exists
        when(userRepository.getLock("pn:lock:TEST-GATE")).thenReturn(null);
        when(gateStatusService.isGateClosed("TEST-GATE")).thenReturn(true);
        
        Map<String, Object> reportRow = new HashMap<>();
        reportRow.put("id", 123);
        mockReportRows.add(reportRow);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> castedRows = mockReportRows;
        when(jdbcTemplate1.queryForList(anyString(), any(), any(), any())).thenReturn((List) castedRows);
        
        when(userRepository.findReportIdPN("123")).thenReturn("42"); // PN already exists
        
        // Execute
        pnGenerationService.generatePNIfNeeded("TEST-GATE", "gm1", "sm1");
        
        // Verify: Should not generate new PN
        verify(userRepository, never()).saveReportIdPN(anyString(), anyString());
        verify(jdbcTemplate1, never()).update(contains("UPDATE reports SET pn"), any(), any());
    }
    
    @Test
    public void testGeneratePNIfNeeded_NoPendingReport_SkipsGeneration() {
        // Setup: No pending report found
        when(userRepository.getLock("pn:lock:TEST-GATE")).thenReturn(null);
        when(gateStatusService.isGateClosed("TEST-GATE")).thenReturn(true);
        when(jdbcTemplate1.queryForList(anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        
        // Execute
        pnGenerationService.generatePNIfNeeded("TEST-GATE", "gm1", "sm1");
        
        // Verify: Should not generate PN
        verify(userRepository, never()).saveReportIdPN(anyString(), anyString());
    }
}

