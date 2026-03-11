-- Migration script to rename gateId to BOOM1_ID, gateName to Gate_Num, and add BOOM2_ID
-- This migration renames gate identification columns and adds support for second boom sensor ID

-- Step 1: Drop the existing index on gateId (if it exists)
-- Check for both possible index names
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics 
                     WHERE table_schema = DATABASE() 
                     AND table_name = 'managegates' 
                     AND index_name = 'idx_managegates_gateId');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE managegates DROP INDEX idx_managegates_gateId;', 'SELECT "Index idx_managegates_gateId does not exist";');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Also try dropping index named 'gateId' if it exists
SET @index_exists2 = (SELECT COUNT(*) FROM information_schema.statistics 
                      WHERE table_schema = DATABASE() 
                      AND table_name = 'managegates' 
                      AND index_name = 'gateId');
SET @sql2 = IF(@index_exists2 > 0, 'ALTER TABLE managegates DROP INDEX gateId;', 'SELECT "Index gateId does not exist";');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Step 2: Rename gateId to BOOM1_ID
ALTER TABLE managegates 
CHANGE COLUMN gateId BOOM1_ID VARCHAR(100) NOT NULL;

-- Step 3: Rename gateName to Gate_Num
ALTER TABLE managegates 
CHANGE COLUMN gateName Gate_Num VARCHAR(255) NOT NULL;

-- Step 4: Add new BOOM2_ID column
ALTER TABLE managegates 
ADD COLUMN BOOM2_ID VARCHAR(100) NOT NULL DEFAULT '' AFTER BOOM1_ID;

-- Step 5: Recreate UNIQUE constraint on BOOM1_ID
ALTER TABLE managegates 
ADD UNIQUE KEY BOOM1_ID (BOOM1_ID);

-- Rollback script (for reference - run manually if needed)
-- ALTER TABLE managegates DROP INDEX BOOM1_ID;
-- ALTER TABLE managegates 
-- CHANGE COLUMN BOOM1_ID gateId VARCHAR(100) NOT NULL,
-- CHANGE COLUMN Gate_Num gateName VARCHAR(255) NOT NULL,
-- DROP COLUMN BOOM2_ID;
-- ALTER TABLE managegates ADD UNIQUE KEY gateId (gateId);
