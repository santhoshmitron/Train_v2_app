-- Migration script to add Boom_Lock column to reports table
-- Boom_Lock: Tracks boom lock health status with values like "healthy" and "unhealthy"

-- Add Boom_Lock column
ALTER TABLE `reports` 
ADD COLUMN `Boom_Lock` VARCHAR(20) NULL DEFAULT NULL AFTER `redy`;

-- Rollback script (for reference - run manually if needed)
-- ALTER TABLE `reports` DROP COLUMN `Boom_Lock`;
