import pool from '../config/database.js';

export const getAllContacts = async (req, res) => {
  try {
    const { page = 1, limit = 10, search = '' } = req.query;
    const offset = (page - 1) * limit;
    
    let whereClause = '';
    let queryParams = [];
    
    if (search) {
      whereClause = 'WHERE name LIKE ? OR phone_number LIKE ?';
      queryParams = [`%${search}%`, `%${search}%`];
    }
    
    const countQuery = `SELECT COUNT(*) as total FROM contacts ${whereClause}`;
    const dataQuery = `
      SELECT * FROM contacts 
      ${whereClause} 
      ORDER BY id DESC 
      LIMIT ${limit} OFFSET ${offset}
    `;
    
    const [countResult] = await pool.execute(countQuery, queryParams);
    const [contacts] = await pool.execute(dataQuery, queryParams);
    
    res.json({
      success: true,
      data: contacts,
      pagination: {
        currentPage: parseInt(page),
        totalPages: Math.ceil(countResult[0].total / limit),
        totalItems: countResult[0].total,
        itemsPerPage: parseInt(limit)
      }
    });
  } catch (error) {
    console.error('Get contacts error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch contacts',
      error: error.message 
    });
  }
};

export const getContactById = async (req, res) => {
  try {
    const { id } = req.params;
    const [contacts] = await pool.execute(
      'SELECT * FROM contacts WHERE id = ?', 
      [id]
    );
    
    if (contacts.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Contact not found'
      });
    }
    
    res.json({
      success: true,
      data: contacts[0]
    });
  } catch (error) {
    console.error('Get contact error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to fetch contact',
      error: error.message 
    });
  }
};

export const createContact = async (req, res) => {
  try {
    const { name, phone_number } = req.body;
    
    if (!name || !phone_number) {
      return res.status(400).json({
        success: false,
        message: 'Name and phone number are required fields'
      });
    }
    
    const [result] = await pool.execute(
      'INSERT INTO contacts (name, phone_number) VALUES (?, ?)',
      [name, phone_number]
    );
    
    res.status(201).json({
      success: true,
      message: 'Contact created successfully',
      data: { id: result.insertId }
    });
  } catch (error) {
    console.error('Create contact error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to create contact',
      error: error.message 
    });
  }
};

export const updateContact = async (req, res) => {
  try {
    const { id } = req.params;
    const { name, phone_number } = req.body;
    
    if (!name || !phone_number) {
      return res.status(400).json({
        success: false,
        message: 'Name and phone number are required fields'
      });
    }
    
    // Check if contact exists
    const [existing] = await pool.execute(
      'SELECT id FROM contacts WHERE id = ?',
      [id]
    );
    
    if (existing.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Contact not found'
      });
    }
    
    await pool.execute(
      'UPDATE contacts SET name = ?, phone_number = ? WHERE id = ?',
      [name, phone_number, id]
    );
    
    res.json({
      success: true,
      message: 'Contact updated successfully'
    });
  } catch (error) {
    console.error('Update contact error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to update contact',
      error: error.message 
    });
  }
};

export const deleteContact = async (req, res) => {
  try {
    const { id } = req.params;
    
    const [result] = await pool.execute(
      'DELETE FROM contacts WHERE id = ?',
      [id]
    );
    
    if (result.affectedRows === 0) {
      return res.status(404).json({
        success: false,
        message: 'Contact not found'
      });
    }
    
    res.json({
      success: true,
      message: 'Contact deleted successfully'
    });
  } catch (error) {
    console.error('Delete contact error:', error);
    res.status(500).json({ 
      success: false, 
      message: 'Failed to delete contact',
      error: error.message 
    });
  }
};
