#!/bin/bash

# Test script to demonstrate Redis close event functionality
# This script shows how to test the Redis integration

echo "=== J-Track Redis Close Events Test ==="
echo ""

echo "1. Testing Redis connection..."
redis-cli ping

echo ""
echo "2. Subscribing to close events channel..."
echo "Run this in a separate terminal to listen for events:"
echo "redis-cli subscribe jtrack:close:events"
echo ""

echo "3. Testing gate status storage..."
echo "Setting test gate status..."
redis-cli set "jtrack:gate:status:TEST_GATE_001" "closed"
redis-cli expire "jtrack:gate:status:TEST_GATE_001" 3600

echo ""
echo "4. Checking gate status..."
redis-cli get "jtrack:gate:status:TEST_GATE_001"

echo ""
echo "5. Listing all gate statuses..."
redis-cli keys "jtrack:gate:status:*"

echo ""
echo "6. Publishing a test close event..."
redis-cli publish "jtrack:close:events" '{"gateId":"TEST_GATE_001","username":"Test Gate","close":"closed","failsafe_method":false,"isfailsafe":false}'

echo ""
echo "=== Test completed ==="
echo "Check the subscriber terminal to see if the event was received."
