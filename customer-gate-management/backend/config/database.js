import mysql from 'mysql2/promise';
import dotenv from 'dotenv';

dotenv.config();

const dbConfig = {
  host: '172.105.40.104',
  port: 3306,
  user: 'jsinfo',
  password: 'Mitron@123',
  database: 'wmirchic_jsinfo',
  connectionLimit: 10,
  charset: 'utf8mb4',
  // Removed invalid options: acquireTimeout, timeout, reconnect
};

// Create connection pool
const pool = mysql.createPool(dbConfig);

// Test database connection
const testConnection = async () => {
  try {
    const connection = await pool.getConnection();
    console.log('✅ Database connected successfully');
    connection.release();
  } catch (error) {
    console.error('❌ Database connection failed:', error.message);
    process.exit(1);
  }
};

testConnection();

export default pool;
