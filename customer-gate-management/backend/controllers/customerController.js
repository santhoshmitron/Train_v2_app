import pool from '../config/database.js';

export const getAllCustomers = async (req, res) => {
  try {
    const { page = 1, limit = 10, search = '', status = '', roles = '' } = req.query;
    const offset = (page - 1) * limit;
    
    let whereClause = '';
    let queryParams = [];
    
    // Search filter
    if (search) {
      whereClause = 'WHERE (name LIKE ? OR email LIKE ? OR mobile LIKE ? OR shop_name LIKE ? OR username LIKE ?)';
      queryParams = [`%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`];
    }
    
    // Status filter
    if (status) {
      whereClause += whereClause ? ' AND status = ?' : 'WHERE status = ?';
      queryParams.push(status);
    }
    
    // Role filter
    if (roles) {
      whereClause += whereClause ? ' AND roles = ?' : 'WHERE roles = ?';
      queryParams.push(roles);
    }
    
    const countQuery = `SELECT COUNT(*) as total FROM customers ${whereClause}`;
    const dataQuery = `
      SELECT * FROM customers 
      ${whereClause} 
      ORDER BY added_on DESC 
      LIMIT ${limit} OFFSET ${offset}
    `;
    
    const [countResult] = await pool.execute(countQuery, queryParams);
    const [customers] = await pool.execute(dataQuery, queryParams);
    
    res.json({
      success: true,
      data: customers,
      pagination: {
        currentPage: parseInt(page),
        totalPages: Math.ceil(countResult[0].total / limit),
        totalItems: countResult[0].total,
        itemsPerPage: parseInt(limit)
      }
    });
  } catch (error) {
    console.error('Get customers error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch customers',
      error: error.message 
    });
  }
};

// New method to get SM customers with their GM count based on managegates table
export const getSMCustomersWithGateInfo = async (req, res) => {
  try {
    const { page = 1, limit = 10, search = '', status = '' } = req.query;
    const offset = (page - 1) * limit;
    
    let whereClause = 'WHERE c.roles = "SM"';
    let queryParams = [];
    
    if (search) {
      whereClause += ' AND (c.name LIKE ? OR c.email LIKE ? OR c.mobile LIKE ? OR c.shop_name LIKE ? OR c.username LIKE ?)';
      queryParams.push(`%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`);
    }
    
    if (status) {
      whereClause += ' AND c.status = ?';
      queryParams.push(status);
    }
    
    // Get SM customers with gate count
    const dataQuery = `
      SELECT c.*, 
             COUNT(DISTINCT m.id) as gate_count,
             GROUP_CONCAT(DISTINCT m.Gate_Num ORDER BY m.Gate_Num) as managed_gates
      FROM customers c
      LEFT JOIN managegates m ON c.username = m.SM
      ${whereClause}
      GROUP BY c.id
      ORDER BY c.added_on DESC 
      LIMIT ${limit} OFFSET ${offset}
    `;
    
    // Count query for pagination
    const countQuery = `
      SELECT COUNT(DISTINCT c.id) as total
      FROM customers c
      LEFT JOIN managegates m ON c.username = m.SM
      ${whereClause}
    `;
    
    const [countResult] = await pool.execute(countQuery, queryParams);
    const [customers] = await pool.execute(dataQuery, queryParams);
    
    res.json({
      success: true,
      data: customers,
      pagination: {
        currentPage: parseInt(page),
        totalPages: Math.ceil(countResult[0].total / limit),
        totalItems: countResult[0].total,
        itemsPerPage: parseInt(limit)
      }
    });
  } catch (error) {
    console.error('Get SM customers with gate info error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch SM customers with gate information',
      error: error.message 
    });
  }
};

// New method to get GM customers under a specific SM based on managegates table
export const getGMCustomersUnderSM = async (req, res) => {
  try {
    const { smUsername } = req.params;
    const { page = 1, limit = 10, search = '', status = '' } = req.query;
    const offset = (page - 1) * limit;
    
    let searchClause = '';
    let queryParams = [smUsername];
    
    if (search) {
      searchClause += ' AND (c.name LIKE ? OR c.email LIKE ? OR c.mobile LIKE ? OR c.shop_name LIKE ? OR c.username LIKE ?)';
      queryParams.push(`%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`);
    }
    
    if (status) {
      searchClause += ' AND c.status = ?';
      queryParams.push(status);
    }
    
    // Get GM customers under the specified SM with gate information
    // Check which columns exist first, then build query accordingly
    let statusCol = 'gate_status';
    let leverStatusCol = 'handle_status';
    let bs1GoCol = 'go';
    let bs1GcCol = 'gc';
    let lsGoCol = 'ho';
    let lsGcCol = 'hc';
    let hasBoom2Id = false;
    let hasBs2Status = false;
    let hasLtStatus = false;
    let hasBs2Go = false;
    let hasBs2Gc = false;
    
    try {
      const [columns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'managegates' 
        AND COLUMN_NAME IN ('BS1_STATUS', 'gate_status', 'LEVER_STATUS', 'handle_status', 
                           'BS1_GO', 'go', 'BS1_GC', 'gc', 'LS_GO', 'ho', 'LS_GC', 'hc',
                           'BOOM2_ID', 'BS2_STATUS', 'LT_STATUS', 'BS2_GO', 'BS2_GC')
      `);
      
      const columnNames = columns.map(col => col.COLUMN_NAME);
      if (columnNames.includes('BS1_STATUS')) statusCol = 'BS1_STATUS';
      if (columnNames.includes('LEVER_STATUS')) leverStatusCol = 'LEVER_STATUS';
      if (columnNames.includes('BS1_GO')) bs1GoCol = 'BS1_GO';
      if (columnNames.includes('BS1_GC')) bs1GcCol = 'BS1_GC';
      if (columnNames.includes('LS_GO')) lsGoCol = 'LS_GO';
      if (columnNames.includes('LS_GC')) lsGcCol = 'LS_GC';
      hasBoom2Id = columnNames.includes('BOOM2_ID');
      hasBs2Status = columnNames.includes('BS2_STATUS');
      hasLtStatus = columnNames.includes('LT_STATUS');
      hasBs2Go = columnNames.includes('BS2_GO');
      hasBs2Gc = columnNames.includes('BS2_GC');
    } catch (e) {
      console.log('Could not check column existence, using defaults:', e.message);
    }
    
    // Build query with only columns that exist
    const dataQuery = `
      SELECT c.*, 
             m.Gate_Num as gateName,
             m.BOOM1_ID as gateId,
             ${hasBoom2Id ? 'm.BOOM2_ID,' : "'' as BOOM2_ID,"}
             m.handle,
             m.${statusCol} as gate_status,
             m.${leverStatusCol} as handle_status,
             m.${bs1GoCol} as go,
             m.${bs1GcCol} as gc,
             m.${lsGoCol} as ho,
             m.${lsGcCol} as hc,
             m.${statusCol} as BS1_STATUS,
             ${hasBs2Status ? 'm.BS2_STATUS,' : "'open' as BS2_STATUS,"}
             m.${leverStatusCol} as LEVER_STATUS,
             ${hasLtStatus ? 'm.LT_STATUS,' : "'open' as LT_STATUS,"}
             m.${bs1GoCol} as BS1_GO,
             m.${bs1GcCol} as BS1_GC,
             ${hasBs2Go ? 'm.BS2_GO,' : "'' as BS2_GO,"}
             ${hasBs2Gc ? 'm.BS2_GC,' : "'' as BS2_GC,"}
             m.${lsGoCol} as LS_GO,
             m.${lsGcCol} as LS_GC
      FROM managegates m
      JOIN customers c ON m.GM = c.username
      WHERE m.SM = ? AND c.roles = "GM"
      ${searchClause}
      ORDER BY c.added_on DESC, m.Gate_Num ASC
      LIMIT ${limit} OFFSET ${offset}
    `;
    
    // Count query for pagination
    const countQuery = `
      SELECT COUNT(*) as total
      FROM managegates m
      JOIN customers c ON m.GM = c.username
      WHERE m.SM = ? AND c.roles = "GM"
      ${searchClause}
    `;
    
    const [countResult] = await pool.execute(countQuery, queryParams);
    const [gmData] = await pool.execute(dataQuery, queryParams);
    
    res.json({
      success: true,
      data: gmData,
      pagination: {
        currentPage: parseInt(page),
        totalPages: Math.ceil(countResult[0].total / limit),
        totalItems: countResult[0].total,
        itemsPerPage: parseInt(limit)
      }
    });
  } catch (error) {
    console.error('Get GM customers under SM error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch GM customers under SM',
      error: error.message 
    });
  }
};

// Get gate details for a specific SM
export const getGatesUnderSM = async (req, res) => {
  try {
    const { smUsername } = req.params;
    
    const gatesQuery = `
      SELECT m.*, 
             sm_customer.name as sm_name,
             gm_customer.name as gm_name,
             gm_customer.email as gm_email,
             gm_customer.mobile as gm_mobile
      FROM managegates m
      LEFT JOIN customers sm_customer ON m.SM = sm_customer.username
      LEFT JOIN customers gm_customer ON m.GM = gm_customer.username
      WHERE m.SM = ?
      ORDER BY m.Gate_Num ASC
    `;
    
    const [gates] = await pool.execute(gatesQuery, [smUsername]);
    
    res.json({
      success: true,
      data: gates
    });
  } catch (error) {
    console.error('Get gates under SM error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch gates under SM',
      error: error.message 
    });
  }
};

// Keep existing methods
export const getCustomerById = async (req, res) => {
  try {
    const { id } = req.params;
    const [customers] = await pool.execute(
      'SELECT * FROM customers WHERE id = ?', 
      [id]
    );
    
    if (customers.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Customer not found'
      });
    }
    
    res.json({
      success: true,
      data: customers[0]
    });
  } catch (error) {
    console.error('Get customer error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch customer',
      error: error.message 
    });
  }
};

export const createCustomer = async (req, res) => {
  try {
    const {
      username, password, name, email, mobile, phone, 
      address, city, pincode, pic, type, shop_name, 
      target, status, logo, roles
    } = req.body;
    
    // Only mandatory fields validation
    if (!name || !username || !password) {
      return res.status(400).json({
        success: false,
        message: 'Name, username, and password are required fields'
      });
    }

    // Ensure 'target' is null or a number to avoid MySQL integer error
    const targetValue = target ? Number(target) : null;

    const [result] = await pool.execute(
      `INSERT INTO customers 
       (username, password, name, email, mobile, phone, address, city, 
        pincode, pic, type, shop_name, target, status, logo, roles, added_on) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())`,
      [
        username, password, name, 
        email || null, mobile || null, phone || null, 
        address || null, city || null, pincode || null, pic || null, 
        type || null, shop_name || null, targetValue, 
        status || 'Active', logo || null, roles || null
      ]
    );
    
    res.status(201).json({
      success: true,
      message: 'Customer created successfully',
      data: { id: result.insertId }
    });
  } catch (error) {
    console.error('Create customer error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to create customer',
      error: error.message 
    });
  }
};


export const updateCustomer = async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;
    
    if (Object.keys(updates).length === 0) {
      return res.status(400).json({
        success: false,
        message: 'No fields to update'
      });
    }
    
    // Check if customer exists
    const [existing] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [id]
    );
    
    if (existing.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Customer not found'
      });
    }
    
    // Build dynamic update query
    const fields = Object.keys(updates);
    const values = Object.values(updates);
    const setClause = fields.map(field => `${field} = ?`).join(', ');
    
    await pool.execute(
      `UPDATE customers SET ${setClause} WHERE id = ?`,
      [...values, id]
    );
    
    res.json({
      success: true,
      message: 'Customer updated successfully'
    });
  } catch (error) {
    console.error('Update customer error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to update customer',
      error: error.message 
    });
  }
};

export const deleteCustomer = async (req, res) => {
  try {
    const { id } = req.params;
    
    const [result] = await pool.execute(
      'DELETE FROM customers WHERE id = ?',
      [id]
    );
    
    if (result.affectedRows === 0) {
      return res.status(404).json({
        success: false,
        message: 'Customer not found'
      });
    }
    
    res.json({
      success: true,
      message: 'Customer deleted successfully'
    });
  } catch (error) {
    console.error('Delete customer error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to delete customer',
      error: error.message 
    });
  }
};

export const getCustomerStats = async (req, res) => {
  try {
    const [stats] = await pool.execute(`
      SELECT 
        COUNT(*) as total,
        COUNT(CASE WHEN status = 'Active' THEN 1 END) as active,
        COUNT(CASE WHEN status = 'Inactive' THEN 1 END) as inactive,
        COUNT(CASE WHEN DATE(added_on) = CURDATE() THEN 1 END) as today_added,
        COUNT(CASE WHEN WEEK(added_on) = WEEK(CURDATE()) THEN 1 END) as week_added,
        COUNT(CASE WHEN MONTH(added_on) = MONTH(CURDATE()) THEN 1 END) as month_added,
        COUNT(CASE WHEN roles = 'SM' THEN 1 END) as sm_count,
        COUNT(CASE WHEN roles = 'GM' THEN 1 END) as gm_count
      FROM customers
    `);
    
    const [cityStats] = await pool.execute(`
      SELECT city, COUNT(*) as count 
      FROM customers 
      WHERE city IS NOT NULL AND city != ''
      GROUP BY city 
      ORDER BY count DESC 
      LIMIT 10
    `);
    
    const [roleStats] = await pool.execute(`
      SELECT roles, COUNT(*) as count 
      FROM customers 
      WHERE roles IS NOT NULL AND roles != ''
      GROUP BY roles 
      ORDER BY count DESC
    `);
    
    res.json({
      success: true,
      data: {
        overview: stats[0],
        cityDistribution: cityStats,
        roleDistribution: roleStats
      }
    });
  } catch (error) {
    console.error('Get customer stats error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch customer statistics',
      error: error.message 
    });
  }
};

// Submit GateMan or StationMaster Details
export const submitGateManOrStationMasterDetails = async (req, res) => {
  try {
    const { first_name, phone, roles, username } = req.body;
    if (!first_name || !phone || !roles || !username) {
      return res.status(400).json({ Code: 400, Message: "Missing required fields" });
    }
    // Optional: Update if already exists, else insert
    const [existing] = await pool.execute(
      'SELECT id FROM customers WHERE roles = ? AND username = ?', [roles, username]
    );
    if (existing.length > 0) {
      await pool.execute(
        'UPDATE customers SET first_name = ?, phone = ? WHERE roles = ? AND username = ?',
        [first_name, phone, roles, username]
      );
    } else {
      await pool.execute(
        'INSERT INTO customers (first_name, phone, roles, username) VALUES (?, ?, ?, ?)',
        [first_name, phone, roles, username]
      );
    }
    return res.json({ Code: 200, Message: "Successfully submitted" });
  } catch (error) {
    res.status(500).json({ Code: 400, Message: error.message });
  }
};

export const getGateManOrStationMasterDetails = async (req, res) => {
  try {
    const { roles, username } = req.query;
    console.log('Incoming GET params:', { roles, username }); // debug log
    const [rows] = await pool.execute(
      'SELECT first_name, phone FROM customers WHERE roles = ? AND username = ?',
      [roles && roles.trim(), username && username.trim()]
    );
    console.log('SQL returned:', rows); // debug log

    if (rows.length > 0) {
      return res.json({ Code: 200, Message: "Details available", Details: rows[0] });
    }
    return res.json({ Code: 201, Message: "No details available", Details: null });
  } catch (error) {
    res.status(500).json({ Code: 400, Message: error.message, Details: null });
  }
};

// Get All Details by Username (including instruction, phone1, phone2, phone3)
export const getCustomerDetailsByUsername = async (req, res) => {
  try {
    const { username } = req.query;
    if (!username) {
      return res.status(400).json({ Code: 400, Message: "Missing username", Details: null });
    }

    const [rows] = await pool.execute(
      `SELECT 
        first_name, phone, instruction, phone1, phone2, phone3 
       FROM customers WHERE username = ?`,
      [username.trim()]
    );
    if (rows.length > 0) {
      return res.json({ Code: 200, Message: "Details available", Details: rows[0] });
    }
    return res.json({ Code: 201, Message: "No details available", Details: null });
  } catch (error) {
    res.status(500).json({ Code: 400, Message: error.message, Details: null });
  }
};

// Insert or update the helpline phone numbers
export const upsertHelplinePhones = async (req, res) => {
  try {
    const { phone1, phone2, phone3 } = req.body;

    if (
      (phone1 && typeof phone1 !== 'string') ||
      (phone2 && typeof phone2 !== 'string') ||
      (phone3 && typeof phone3 !== 'string')
    ) {
      return res.status(400).json({
        Code: 400,
        Message: "Invalid phone number format",
        Details: null
      });
    }

    // Check if helpline record exists (assuming only one row)
    const [rows] = await pool.execute('SELECT id FROM helpline LIMIT 1');

    if (rows.length > 0) {
      // Update existing row
      await pool.execute(
        'UPDATE helpline SET phone1 = ?, phone2 = ?, phone3 = ? WHERE id = ?',
        [phone1 || null, phone2 || null, phone3 || null, rows[0].id]
      );
    } else {
      // Insert new row
      await pool.execute(
        'INSERT INTO helpline (phone1, phone2, phone3) VALUES (?, ?, ?)',
        [phone1 || null, phone2 || null, phone3 || null]
      );
    }

    return res.json({
      Code: 200,
      Message: "Helpline phone numbers saved successfully",
      Details: null
    });
  } catch (error) {
    return res.status(500).json({
      Code: 500,
      Message: error.message,
      Details: null
    });
  }
};


// Get the current helpline phone numbers (assuming a single row)
export const getHelplinePhones = async (req, res) => {
  try {
    const [rows] = await pool.execute(
      'SELECT phone1, phone2, phone3 FROM helpline LIMIT 1'
    );
    if (rows.length > 0) {
      return res.json({
        Code: 200,
        Message: 'Helpline numbers retrieved',
        Details: rows[0],
      });
    }
    return res.json({
      Code: 201,
      Message: 'No helpline numbers set',
      Details: null,
    });
  } catch (error) {
    return res.status(500).json({
      Code: 500,
      Message: error.message,
      Details: null,
    });
  }
};
