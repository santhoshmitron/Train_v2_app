-- One-time migration: shift stored UTC timestamps to IST (+05:30).
-- IMPORTANT: Run this only once, and only if your existing DB rows are truly UTC.
-- MySQL 5.7+ compatible syntax.
--
-- Targets:
-- 1) reports (only rows with redy='ct'):
--    - added_on: shift +05:30 (datetime)
--    - tn_time, lc_pin_time: shift time-of-day string 'HH:mm' by +05:30
-- 2) login_logs:
--    - login_time, logout_time: shift +05:30 (datetime)

START TRANSACTION;

-- reports: shift datetime column for ct rows
UPDATE reports
SET added_on = DATE_ADD(added_on, INTERVAL 5 HOUR 30 MINUTE)
WHERE redy = 'ct' AND added_on IS NOT NULL;

-- reports: shift tn_time (HH:mm) for ct rows
UPDATE reports
SET tn_time = DATE_FORMAT(
    DATE_ADD(STR_TO_DATE(tn_time, '%H:%i'), INTERVAL 5 HOUR 30 MINUTE),
    '%H:%i'
)
WHERE redy = 'ct'
  AND tn_time IS NOT NULL
  AND tn_time <> '';

-- reports: shift lc_pin_time (HH:mm) for ct rows
UPDATE reports
SET lc_pin_time = DATE_FORMAT(
    DATE_ADD(STR_TO_DATE(lc_pin_time, '%H:%i'), INTERVAL 5 HOUR 30 MINUTE),
    '%H:%i'
)
WHERE redy = 'ct'
  AND lc_pin_time IS NOT NULL
  AND lc_pin_time <> '';

-- login_logs: shift login_time
UPDATE login_logs
SET login_time = DATE_ADD(login_time, INTERVAL 5 HOUR 30 MINUTE)
WHERE login_time IS NOT NULL;

-- login_logs: shift logout_time (keep NULLs as NULL)
UPDATE login_logs
SET logout_time = CASE
    WHEN logout_time IS NULL THEN NULL
    ELSE DATE_ADD(logout_time, INTERVAL 5 HOUR 30 MINUTE)
END;

COMMIT;

