-- Migration script to add BS2 and LT sensor support
-- Add columns to managegates table for BS2 and LT sensors
-- LT is binary (0 or 1), no threshold columns needed

ALTER TABLE managegates 
ADD COLUMN IF NOT EXISTS bs2_status VARCHAR(20) DEFAULT 'open',
ADD COLUMN IF NOT EXISTS lt_status VARCHAR(20) DEFAULT 'open',
ADD COLUMN IF NOT EXISTS bs2_go VARCHAR(11) DEFAULT '',
ADD COLUMN IF NOT EXISTS bs2_gc VARCHAR(11) DEFAULT '';

-- Update existing records to have default values
UPDATE managegates 
SET bs2_status = 'open', lt_status = 'open'
WHERE bs2_status IS NULL OR lt_status IS NULL;

-- Note: LT is binary (0 or 1), no threshold columns (lt_go, lt_gc) needed
-- LT closed = value == 1, LT open = value == 0

