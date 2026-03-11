# Monitoring Setup for BS2/LT Sensor Upgrade

## Overview
This document outlines monitoring requirements for the BS2/LT sensor upgrade to ensure safety-critical system reliability.

## Key Metrics to Monitor

### 1. PN Generation Rate
- **Metric**: Number of PNs generated per gate per hour
- **Alert Threshold**: > 2 PNs per gate per hour (indicates duplicate generation or system issue)
- **Query**: Count PN generation events from logs or database
- **Dashboard**: Track PN generation rate over time, identify anomalies

### 2. Boom Lock Health Check Alerts
- **Metric**: Number of unhealthy boom lock detections
- **Alert Threshold**: Any unhealthy detection (immediate alert)
- **Query**: Search logs for "Boom lock unhealthy" messages
- **Dashboard**: Track unhealthy events per gate, identify patterns

### 3. Sensor Missing Alerts
- **Metric**: Time since last sensor update for BS2/LT
- **Alert Threshold**: No update received for > 5 minutes
- **Query**: Check Redis last known sensor value timestamps
- **Dashboard**: Show sensor status for all gates, highlight missing sensors

### 4. False CLOSED Detection
- **Metric**: Gate marked CLOSED but any sensor is open
- **Alert Threshold**: Any false CLOSED detection (critical alert)
- **Query**: Compare gate status with individual sensor statuses
- **Dashboard**: Real-time gate status vs sensor status comparison

### 5. Database Performance
- **Metric**: Query execution time for sensor status checks
- **Alert Threshold**: Query time > 100ms
- **Query**: Monitor JDBC query execution times
- **Dashboard**: Track query performance, identify slow queries

### 6. Redis Lock Contention
- **Metric**: Number of failed lock acquisitions for PN generation
- **Alert Threshold**: > 10 failed locks per minute (indicates high contention)
- **Query**: Count "PN generation already in progress" log messages
- **Dashboard**: Track lock contention, identify bottlenecks

## Logging Requirements

### Critical Events (Log Level: INFO or WARN)
1. **PN Generation**: Log when PN is generated with gate ID, report ID, PN value
2. **Boom Lock Unhealthy**: Log when boom lock is detected as unhealthy
3. **Sensor Status Changes**: Log when any sensor status changes (BS1, BS2, LS, LT)
4. **Gate Status Transitions**: Log when gate transitions between OPEN/CLOSED
5. **Lock Acquisition Failures**: Log when Redis lock cannot be acquired

### Example Log Messages
```
INFO: PN 42 generated and saved for gate: TEST-GATE, report: 123
WARN: Boom lock unhealthy for gate: TEST-GATE - LS closed 25000ms ago but LT != 1
INFO: Gate TEST-GATE status check - BS1: closed, BS2: closed, LT: closed, LS: closed, Result: true
INFO: Gate TEST-GATE transitioned to CLOSED, generating PN
DEBUG: PN generation already in progress for gate: TEST-GATE
```

## Dashboard Recommendations

### Real-Time Dashboard
1. **Gate Status Overview**: Show all gates with 4-sensor status (BS1, BS2, LT, LS)
2. **PN Generation Activity**: Real-time feed of PN generation events
3. **Health Check Status**: Show active health checks and unhealthy gates
4. **Sensor Update Rate**: Track sensor update frequency per gate

### Historical Dashboard
1. **PN Generation Trends**: PN generation rate over time
2. **Health Check History**: Timeline of boom lock health events
3. **Sensor Reliability**: Uptime/downtime for each sensor type
4. **Performance Metrics**: Query times, lock contention over time

## Alert Configuration

### Critical Alerts (P0 - Immediate Response)
- False CLOSED detection
- Boom lock unhealthy
- PN generation failure

### High Priority Alerts (P1 - Response within 1 hour)
- Sensor missing for > 5 minutes
- PN generation rate anomaly
- Database performance degradation

### Medium Priority Alerts (P2 - Response within 4 hours)
- Lock contention high
- Query performance slow
- Sensor update rate low

## Implementation Notes

### Log Aggregation
- Use centralized logging (e.g., ELK stack, Splunk, CloudWatch)
- Ensure all services log to same aggregation point
- Set up log parsing for structured data extraction

### Metrics Collection
- Use application metrics (e.g., Micrometer, Prometheus)
- Expose metrics endpoints for scraping
- Set up time-series database for metrics storage

### Alerting
- Configure alert channels (email, SMS, PagerDuty, Slack)
- Set up alert routing based on severity
- Implement alert escalation policies

## Testing Monitoring

### Test Scenarios
1. **PN Generation**: Verify alert triggers when > 2 PNs generated per hour
2. **Health Check**: Verify alert triggers when boom lock unhealthy
3. **Sensor Missing**: Verify alert triggers when sensor missing > 5 minutes
4. **False CLOSED**: Verify alert triggers when gate falsely marked CLOSED

### Monitoring Validation
- Run test scenarios and verify alerts are triggered
- Verify dashboard displays correct data
- Validate log aggregation captures all events
- Test alert delivery channels

## Maintenance

### Regular Reviews
- Weekly review of alert frequency and false positives
- Monthly review of dashboard effectiveness
- Quarterly review of metric thresholds

### Continuous Improvement
- Adjust alert thresholds based on historical data
- Add new metrics as system evolves
- Optimize dashboard queries for performance

