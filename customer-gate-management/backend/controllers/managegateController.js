import pool from '../config/database.js';
import { getCache, setCache } from '../utils/cache.js';

export const getAllManagegates = async (req, res) => {
  try {
    const { page = 1, limit = 10, search = '', status = '' } = req.query;
    const offset = (page - 1) * limit;
    
    let whereClause = '';
    let queryParams = [];
    
    if (search) {
      whereClause = 'WHERE Gate_Num LIKE ? OR BOOM1_ID LIKE ? OR handle LIKE ?';
      queryParams = [`%${search}%`, `%${search}%`, `%${search}%`];
    }
    
    if (status) {
      whereClause += whereClause ? ' AND status = ?' : 'WHERE status = ?';
      queryParams.push(status);
    }
    
    const countQuery = `SELECT COUNT(*) as total FROM managegates ${whereClause}`;
    const dataQuery = `
      SELECT * FROM managegates 
      ${whereClause} 
      ORDER BY added_on DESC 
      LIMIT ${limit} OFFSET ${offset}
    `;
    
    const [countResult] = await pool.execute(countQuery, queryParams);
    const [gates] = await pool.execute(dataQuery, queryParams);
    
    // Map database field names to frontend field names
    const mappedGates = gates.map(gate => ({
      id: gate.id,
      Gate_Num: gate.Gate_Num,
      BOOM1_ID: gate.BOOM1_ID,
      BOOM2_ID: gate.BOOM2_ID || null,
      handle: gate.handle,
      SM: gate.SM,
      GM: gate.GM,
      status: gate.status,
      added_on: gate.added_on,
      BS1_GO: gate.BS1_GO || gate.go || null,
      BS1_GC: gate.BS1_GC || gate.gc || null,
      BS2_GO: gate.BS2_GO || null,
      BS2_GC: gate.BS2_GC || null,
      LS_GO: gate.LS_GO || gate.ho || null,
      LS_GC: gate.LS_GC || gate.hc || null,
      BS1_STATUS: gate.BS1_STATUS || gate.gate_status || 'open',
      BS2_STATUS: gate.BS2_STATUS || null,
      LEVER_STATUS: gate.LEVER_STATUS || gate.handle_status || 'open',
      LT_STATUS: gate.LT_STATUS || null,
      LTSW_ID: gate.LTSW_ID || null,
      LT1_STATUS: gate.LT1_STATUS || null,
      LT2_STATUS: gate.LT2_STATUS || null,
      // Legacy field names for backward compatibility
      gateName: gate.Gate_Num,
      gateId: gate.BOOM1_ID,
      go: gate.BS1_GO || gate.go || null,
      gc: gate.BS1_GC || gate.gc || null,
      ho: gate.LS_GO || gate.ho || null,
      hc: gate.LS_GC || gate.hc || null,
      gate_status: gate.BS1_STATUS || gate.gate_status || 'open',
      handle_status: gate.LEVER_STATUS || gate.handle_status || 'open'
    }));
    
    res.json({
      success: true,
      data: mappedGates,
      pagination: {
        currentPage: parseInt(page),
        totalPages: Math.ceil(countResult[0].total / limit),
        totalItems: countResult[0].total,
        itemsPerPage: parseInt(limit)
      }
    });
  } catch (error) {
    console.error('Get managegates error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch managegates',
      error: error.message 
    });
  }
};

export const getManagegateById = async (req, res) => {
  try {
    const { id } = req.params;
    const [gates] = await pool.execute(
      'SELECT * FROM managegates WHERE id = ?', 
      [id]
    );
    
    if (gates.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Gate not found'
      });
    }
    
    const gate = gates[0];
    const mappedGate = {
      id: gate.id,
      Gate_Num: gate.Gate_Num,
      BOOM1_ID: gate.BOOM1_ID,
      BOOM2_ID: gate.BOOM2_ID || null,
      handle: gate.handle,
      SM: gate.SM,
      GM: gate.GM,
      status: gate.status,
      added_on: gate.added_on,
      BS1_GO: gate.BS1_GO || gate.go || null,
      BS1_GC: gate.BS1_GC || gate.gc || null,
      BS2_GO: gate.BS2_GO || null,
      BS2_GC: gate.BS2_GC || null,
      LS_GO: gate.LS_GO || gate.ho || null,
      LS_GC: gate.LS_GC || gate.hc || null,
      BS1_STATUS: gate.BS1_STATUS || gate.gate_status || 'open',
      BS2_STATUS: gate.BS2_STATUS || null,
      LEVER_STATUS: gate.LEVER_STATUS || gate.handle_status || 'open',
      LT_STATUS: gate.LT_STATUS || null,
      LTSW_ID: gate.LTSW_ID || null,
      LT1_STATUS: gate.LT1_STATUS || null,
      LT2_STATUS: gate.LT2_STATUS || null,
      // Legacy field names for backward compatibility
      gateName: gate.Gate_Num,
      gateId: gate.BOOM1_ID,
      go: gate.BS1_GO || gate.go || null,
      gc: gate.BS1_GC || gate.gc || null,
      ho: gate.LS_GO || gate.ho || null,
      hc: gate.LS_GC || gate.hc || null,
      gate_status: gate.BS1_STATUS || gate.gate_status || 'open',
      handle_status: gate.LEVER_STATUS || gate.handle_status || 'open'
    };
    
    res.json({
      success: true,
      data: mappedGate
    });
  } catch (error) {
    console.error('Get managegate error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch gate',
      error: error.message 
    });
  }
};

export const createManagegate = async (req, res) => {
  try {
    const {
      Gate_Num, BOOM1_ID, BOOM2_ID, handle, SM, GM, status,
      BS1_GO, BS1_GC, BS2_GO, BS2_GC, LS_GO, LS_GC,
      BS1_STATUS, BS2_STATUS, LEVER_STATUS, LT_STATUS,
      LTSW_ID, LT1_STATUS, LT2_STATUS,
      // Legacy field names for backward compatibility
      gateName, gateId, go, gc, ho, hc, gate_status, handle_status
    } = req.body;
    
    // Map legacy field names to new names if provided
    const gateNum = Gate_Num || gateName;
    const boom1Id = BOOM1_ID || gateId;
    const bs1Go = BS1_GO || go;
    const bs1Gc = BS1_GC || gc;
    const lsGo = LS_GO || ho;
    const lsGc = LS_GC || hc;
    const bs1Status = BS1_STATUS || gate_status || 'open';
    const leverStatus = LEVER_STATUS || handle_status || 'open';
    
    // Validation
    if (!gateNum || !boom1Id) {
      return res.status(400).json({
        success: false,
        message: 'Gate number and Boom 1 ID are required fields'
      });
    }
    
    // Check if BOOM1_ID already exists
    const [existing] = await pool.execute(
      'SELECT id FROM managegates WHERE BOOM1_ID = ?',
      [boom1Id]
    );
    
    if (existing.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'Gate with this Boom 1 ID already exists'
      });
    }
    
    const [result] = await pool.execute(
      `INSERT INTO managegates 
       (Gate_Num, BOOM1_ID, BOOM2_ID, handle, SM, GM, status, 
        BS1_GO, BS1_GC, BS2_GO, BS2_GC, LS_GO, LS_GC,
        BS1_STATUS, BS2_STATUS, LEVER_STATUS, LT_STATUS,
        LTSW_ID, LT1_STATUS, LT2_STATUS, added_on) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())`,
      [
        gateNum, boom1Id, BOOM2_ID || null, handle || null, SM || null, GM || null, 
        status || 'Open',
        bs1Go || null, bs1Gc || null, BS2_GO || null, BS2_GC || null,
        lsGo || null, lsGc || null,
        bs1Status, BS2_STATUS || null, leverStatus, LT_STATUS || null,
        LTSW_ID || null, LT1_STATUS || null, LT2_STATUS || null
      ]
    );
    
    res.status(201).json({
      success: true,
      message: 'Gate created successfully',
      data: { id: result.insertId }
    });
  } catch (error) {
    console.error('Create managegate error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to create gate',
      error: error.message 
    });
  }
};

export const updateManagegate = async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;
    
    if (Object.keys(updates).length === 0) {
      return res.status(400).json({
        success: false,
        message: 'No fields to update'
      });
    }
    
    // Check if gate exists
    const [existing] = await pool.execute(
      'SELECT id FROM managegates WHERE id = ?',
      [id]
    );
    
    if (existing.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Gate not found'
      });
    }
    
    // Map field names from frontend to database columns
    const fieldMapping = {
      Gate_Num: 'Gate_Num',
      gateName: 'Gate_Num',
      BOOM1_ID: 'BOOM1_ID',
      gateId: 'BOOM1_ID',
      BOOM2_ID: 'BOOM2_ID',
      handle: 'handle',
      SM: 'SM',
      GM: 'GM',
      status: 'status',
      BS1_GO: 'BS1_GO',
      go: 'BS1_GO',
      BS1_GC: 'BS1_GC',
      gc: 'BS1_GC',
      BS2_GO: 'BS2_GO',
      BS2_GC: 'BS2_GC',
      LS_GO: 'LS_GO',
      ho: 'LS_GO',
      LS_GC: 'LS_GC',
      hc: 'LS_GC',
      BS1_STATUS: 'BS1_STATUS',
      gate_status: 'BS1_STATUS',
      BS2_STATUS: 'BS2_STATUS',
      LEVER_STATUS: 'LEVER_STATUS',
      handle_status: 'LEVER_STATUS',
      LT_STATUS: 'LT_STATUS',
      LTSW_ID: 'LTSW_ID',
      LT1_STATUS: 'LT1_STATUS',
      LT2_STATUS: 'LT2_STATUS'
    };
    
    // Build update query with mapped field names
    const dbFields = [];
    const values = [];
    
    for (const [key, value] of Object.entries(updates)) {
      const dbField = fieldMapping[key] || key;
      if (dbField && !dbFields.includes(dbField)) {
        dbFields.push(dbField);
        values.push(value);
      }
    }
    
    if (dbFields.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'No valid fields to update'
      });
    }
    
    const setClause = dbFields.map(field => `${field} = ?`).join(', ');
    
    await pool.execute(
      `UPDATE managegates SET ${setClause} WHERE id = ?`,
      [...values, id]
    );
    
    res.json({
      success: true,
      message: 'Gate updated successfully'
    });
  } catch (error) {
    console.error('Update managegate error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to update gate',
      error: error.message 
    });
  }
};

export const deleteManagegate = async (req, res) => {
  try {
    const { id } = req.params;
    
    const [result] = await pool.execute(
      'DELETE FROM managegates WHERE id = ?',
      [id]
    );
    
    if (result.affectedRows === 0) {
      return res.status(404).json({
        success: false,
        message: 'Gate not found'
      });
    }
    
    res.json({
      success: true,
      message: 'Gate deleted successfully'
    });
  } catch (error) {
    console.error('Delete managegate error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to delete gate',
      error: error.message 
    });
  }
};

export const getManagegateStats = async (req, res) => {
  try {
    // Check which status column exists by querying information_schema
    let statusColumn = 'gate_status';
    let statusColumnExists = false;
    
    try {
      const [columns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'managegates' 
        AND COLUMN_NAME IN ('BS1_STATUS', 'gate_status')
      `);
      
      // Check if BS1_STATUS exists
      statusColumnExists = columns.some(col => col.COLUMN_NAME === 'BS1_STATUS');
      if (statusColumnExists) {
        statusColumn = 'BS1_STATUS';
      }
    } catch (e) {
      console.log('Could not check column existence, using default:', e.message);
    }
    
    const [stats] = await pool.execute(`
      SELECT 
        COUNT(*) as total,
        COUNT(CASE WHEN status = 'Active' OR status = 'active' THEN 1 END) as active,
        COUNT(CASE WHEN status = 'Inactive' OR status = 'inactive' THEN 1 END) as inactive,
        COUNT(CASE WHEN ${statusColumn} = 'open' THEN 1 END) as gates_open,
        COUNT(CASE WHEN ${statusColumn} = 'closed' THEN 1 END) as gates_closed,
        COUNT(CASE WHEN DATE(added_on) = CURDATE() THEN 1 END) as today_added
      FROM managegates
    `);
    
    const [statusDistribution] = await pool.execute(`
      SELECT ${statusColumn} as status, COUNT(*) as count 
      FROM managegates 
      WHERE ${statusColumn} IS NOT NULL AND ${statusColumn} != ''
      GROUP BY ${statusColumn}
      ORDER BY count DESC
    `);
    
    res.json({
      success: true,
      data: {
        overview: stats[0],
        statusDistribution
      }
    });
  } catch (error) {
    console.error('Get managegate stats error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch gate statistics',
      error: error.message 
    });
  }
};


// --- Add this new export to managegateController.js ---
export const getBSLSGateIds = async (req, res) => {
  try {
    const cacheKey = 'bsLsGateIds';
    const cached = getCache(cacheKey);
    if (cached) {
      return res.json({ success: true, ...cached, cached: true });
    }

    const [rows] = await pool.execute('SELECT BOOM1_ID, handle FROM managegates');

    const bsIds = [...new Set(rows.map(r => r.BOOM1_ID).filter(id => id && id.endsWith('BS')))];
    const lsIds = [...new Set(rows.map(r => r.handle).filter(id => id && id.endsWith('LS')))];
    
    const result = { bsIds, lsIds };
    setCache(cacheKey, result, 60 * 1000); // Cache for 1 min
    
    res.json({ success: true, ...result, cached: false });
  } catch (error) {
    console.error('Get BS/LS gate ids error:', error);
    res.status(500).json({ success: false, message: 'Failed to fetch gate ids', error: error.message });
  }
};