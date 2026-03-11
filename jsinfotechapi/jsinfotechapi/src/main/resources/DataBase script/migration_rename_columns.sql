-- Migration script to rename managegates table columns to use clear, descriptive names
-- This migration renames all sensor threshold and status columns

-- BS1 Sensor (Boom Sensor 1) columns
ALTER TABLE managegates 
CHANGE COLUMN go BS1_GO VARCHAR(11) NOT NULL,
CHANGE COLUMN gc BS1_GC VARCHAR(11) NOT NULL,
CHANGE COLUMN gate_status BS1_STATUS VARCHAR(20) NOT NULL DEFAULT 'open';

-- BS2 Sensor (Boom Sensor 2) columns
ALTER TABLE managegates 
CHANGE COLUMN bs2_go BS2_GO VARCHAR(11) DEFAULT '',
CHANGE COLUMN bs2_gc BS2_GC VARCHAR(11) DEFAULT '',
CHANGE COLUMN bs2_status BS2_STATUS VARCHAR(20) DEFAULT 'open';

-- LS/Lever Sensor columns
ALTER TABLE managegates 
CHANGE COLUMN ho LS_GO VARCHAR(11) NOT NULL,
CHANGE COLUMN hc LS_GC VARCHAR(11) NOT NULL,
CHANGE COLUMN handle_status LEVER_STATUS VARCHAR(20) NOT NULL DEFAULT 'open';

-- LT Sensor column
ALTER TABLE managegates 
CHANGE COLUMN lt_status LT_STATUS VARCHAR(20) DEFAULT 'open';

-- Rollback script (for reference - run manually if needed)
-- ALTER TABLE managegates 
-- CHANGE COLUMN BS1_GO go VARCHAR(11) NOT NULL,
-- CHANGE COLUMN BS1_GC gc VARCHAR(11) NOT NULL,
-- CHANGE COLUMN BS1_STATUS gate_status VARCHAR(20) NOT NULL DEFAULT 'open',
-- CHANGE COLUMN BS2_GO bs2_go VARCHAR(11) DEFAULT '',
-- CHANGE COLUMN BS2_GC bs2_gc VARCHAR(11) DEFAULT '',
-- CHANGE COLUMN BS2_STATUS bs2_status VARCHAR(20) DEFAULT 'open',
-- CHANGE COLUMN LS_GO ho VARCHAR(11) NOT NULL,
-- CHANGE COLUMN LS_GC hc VARCHAR(11) NOT NULL,
-- CHANGE COLUMN LEVER_STATUS handle_status VARCHAR(20) NOT NULL DEFAULT 'open',
-- CHANGE COLUMN LT_STATUS lt_status VARCHAR(20) DEFAULT 'open';
