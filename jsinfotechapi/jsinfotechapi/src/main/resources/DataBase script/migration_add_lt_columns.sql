-- Migration script to add LTSW_ID, LT1_STATUS, and LT2_STATUS columns to managegates table
-- LTSW_ID: Contains LT switch ID like "E20-750LT"
-- LT1_STATUS: Status for LT1 sensor (updated from field2: 0=open, 1=closed)
-- LT2_STATUS: Status for LT2 sensor (updated from field3: 0=open, 1=closed)
-- LT_STATUS: Status for LT sensor (updated from field4: 0=open, 1=closed)

-- Step 1: Add LTSW_ID column
ALTER TABLE managegates 
ADD COLUMN LTSW_ID VARCHAR(100) DEFAULT NULL AFTER BOOM2_ID;

-- Step 2: Add LT1_STATUS column
ALTER TABLE managegates 
ADD COLUMN LT1_STATUS VARCHAR(20) DEFAULT 'open' AFTER LT_STATUS;

-- Step 3: Add LT2_STATUS column
ALTER TABLE managegates 
ADD COLUMN LT2_STATUS VARCHAR(20) DEFAULT 'open' AFTER LT1_STATUS;

-- Step 4: Add index on LTSW_ID for faster lookups (optional but recommended)
ALTER TABLE managegates 
ADD INDEX idx_LTSW_ID (LTSW_ID);

-- Rollback script (for reference - run manually if needed)
-- ALTER TABLE managegates DROP INDEX idx_LTSW_ID;
-- ALTER TABLE managegates DROP COLUMN LT2_STATUS;
-- ALTER TABLE managegates DROP COLUMN LT1_STATUS;
-- ALTER TABLE managegates DROP COLUMN LTSW_ID;
