-- INSERT script for Gate 750 (PURA) with BS2 and LT sensor support
-- Sensor IDs:
--   BS1: E20-750BS1
--   BS2: E20-750BS2
--   LS:  E20-750LS
--   LT:  E20-750LT

INSERT INTO managegates (
    Gate_Num,
    BOOM1_ID,
    BOOM2_ID,
    handle,
    SM,
    GM,
    status,
    BS1_GO,
    BS1_GC,
    LS_GO,
    LS_GC,
    BS1_STATUS,
    LEVER_STATUS,
    BS2_STATUS,
    LT_STATUS,
    BS2_GO,
    BS2_GC
) VALUES (
    '750',                              -- Gate_Num
    'E20-750BS1',                       -- BOOM1_ID (BS1 sensor ID)
    '',                                 -- BOOM2_ID (BS2 sensor ID, can be empty initially)
    'E20-750LS',                        -- handle (LS sensor ID)
    'PURA_SM',                          -- SM (Station Master)
    'PURA_GM',                          -- GM (Gate Manager)
    'Open',                             -- status (default)
    '530',                              -- BS1_GO (BS1 threshold - open, adjust as needed)
    '520',                              -- BS1_GC (BS1 threshold - close, adjust as needed)
    '500',                              -- LS_GO (LS threshold - open, adjust as needed)
    '540',                              -- LS_GC (LS threshold - close, adjust as needed)
    'open',                             -- BS1_STATUS (BS1 status)
    'open',                             -- LEVER_STATUS (LS status)
    'open',                             -- BS2_STATUS (BS2 status)
    'open',                             -- LT_STATUS (LT status)
    '530',                              -- BS2_GO (BS2 threshold - open, adjust as needed)
    '520'                               -- BS2_GC (BS2 threshold - close, adjust as needed)
);

-- Note: 
-- - Threshold values (BS1_GO, BS1_GC, LS_GO, LS_GC, BS2_GO, BS2_GC) should be adjusted based on actual sensor calibration
-- - LT is binary (0 or 1), no threshold columns needed
-- - LT closed = value == 1, LT open = value == 0
-- - All four sensors (BS1, BS2, LS, LT) must be closed for gate to be CLOSED


