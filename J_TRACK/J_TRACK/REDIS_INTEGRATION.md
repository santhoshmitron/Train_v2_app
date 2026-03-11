# Redis Integration for J-Track Close Events

This document explains how Redis integration has been added to the J-Track application to publish close events to Redis in addition to the existing Kafka functionality.

## Overview

The application now publishes close events to both:
1. **Kafka** (existing functionality) - for message queuing and processing
2. **Redis** (new functionality) - for real-time event distribution and caching

## Configuration

### Redis Configuration in `application.conf`

```hocon
jtrack {
  redis {
    host = "localhost"
    port = 6379
    password = ""
    database = 0
    timeout = 2000
    pool {
      maxTotal = 8
      maxIdle = 8
      minIdle = 0
    }
  }
}
```

### Dependencies Added

The following dependency has been added to `build.sbt`:
```scala
"redis.clients" % "jedis" % "4.3.1"
```

## Implementation Details

### 1. RedisUtil Class (`src/main/java/com/jtrack/util/RedisUtil.java`)

This utility class provides:
- Redis connection pooling
- Close event publishing to Redis channels
- Gate status caching in Redis
- Error handling and logging

**Key Methods:**
- `publishCloseEvent(J_Message message, LoggingAdapter log)` - Publishes close events to Redis
- `setGateStatus(String gateId, String status, LoggingAdapter log)` - Caches gate status
- `getGateStatus(String gateId, LoggingAdapter log)` - Retrieves gate status

### 2. Integration Points

Close events are now published to Redis at these locations:

#### In `New_J_Entity.java`:
- When gate status changes to "closed"
- When reports are inserted for closed gates
- When handle status is updated to "closed"

#### In `KafkaJ_Producer.java`:
- As a backup/additional channel when publishing to Kafka

### 3. Redis Channels and Keys

**Channels:**
- `jtrack:close:events` - Pub/Sub channel for close events

**Keys:**
- `jtrack:gate:status:{gateId}` - Cached gate status (expires after 1 hour)

## Usage

### Publishing Close Events

Close events are automatically published when:
1. A gate is detected as closed
2. A close event message is sent to Kafka
3. Gate status is updated in the database

### Subscribing to Close Events

Use the `RedisSubscriber` class to listen for close events:

```java
// Subscribe to close events
RedisSubscriber.subscribeToCloseEvents();

// Get current gate status
String status = RedisSubscriber.getGateStatus("GATE_001");

// List all gate statuses
RedisSubscriber.listAllGateStatuses();
```

### Testing Redis Integration

1. **Start Redis server:**
   ```bash
   redis-server
   ```

2. **Test the integration:**
   ```bash
   ./test_redis_events.sh
   ```

3. **Subscribe to events manually:**
   ```bash
   redis-cli subscribe jtrack:close:events
   ```

## Event Format

Close events are published as JSON messages with the following structure:

```json
{
  "gateId": "GATE_001",
  "username": "Gate Name",
  "close": "closed",
  "failsafe_method": false,
  "isfailsafe": false
}
```

## Benefits

1. **Real-time Event Distribution**: Redis pub/sub provides immediate event delivery
2. **Status Caching**: Gate statuses are cached for quick access
3. **Redundancy**: Events are published to both Kafka and Redis
4. **Monitoring**: Easy to monitor gate statuses and events
5. **Integration**: Simple integration with other systems via Redis

## Monitoring

### Check Redis Connection
```bash
redis-cli ping
```

### Monitor Close Events
```bash
redis-cli monitor
```

### Check Gate Statuses
```bash
redis-cli keys "jtrack:gate:status:*"
redis-cli get "jtrack:gate:status:GATE_001"
```

## Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   - Check if Redis server is running
   - Verify host/port configuration
   - Check network connectivity

2. **Events Not Received**
   - Verify Redis pub/sub is working
   - Check channel name: `jtrack:close:events`
   - Verify application is publishing events

3. **Memory Issues**
   - Monitor Redis memory usage
   - Adjust pool settings in configuration
   - Set appropriate TTL for cached data

### Logs

Check application logs for Redis-related messages:
- Connection status
- Event publishing success/failure
- Error messages

## Future Enhancements

1. **Redis Cluster Support**: For high availability
2. **Event Filtering**: Filter events by gate type or status
3. **Metrics**: Add Redis metrics monitoring
4. **Authentication**: Add Redis password authentication
5. **Persistence**: Configure Redis persistence for critical data
