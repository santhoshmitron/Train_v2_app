import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import compression from 'compression';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

import customerRoutes from './routes/customers.js';
import managegateRoutes from './routes/managegates.js';
import contactRoutes from './routes/contacts.js';
import elasticProxyRouter from './routes/elasticProxy.js';

// Load environment variables
dotenv.config();

const app = express();
const PORT = process.env.PORT || 5001;

// Resolve directory paths (for ES Modules)
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 🔒 Security middleware
app.use(helmet());
app.use(compression());

// ⚙️ Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 50000,
  message: 'Too many requests from this IP, please try again later.'
});
app.use('/api', limiter);


app.use(cors({
  origin: [
    "http://localhost:3000",
    "http://45.79.126.87",
    "http://45.79.126.87:5002"
  ],
  methods: ["GET", "POST", "PUT", "DELETE"],
  credentials: true
}));


// 🧩 Body parsing middleware
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// 🪵 Logging
app.use(morgan('combined'));

// 🧭 API Routes
app.use('/api/customers', customerRoutes);
app.use('/api/managegates', managegateRoutes);
app.use('/api/contacts', contactRoutes);
app.use('/api/elasticsearch', elasticProxyRouter);

// 💓 Health check endpoint
app.get('/api/health', (req, res) => {
  res.status(200).json({
    status: 'OK',
    timestamp: new Date().toISOString(),
    uptime: process.uptime()
  });
});



// 🌍 Serve Frontend Build (React - Vite)
const frontendPath = path.join(__dirname, './frontend-build');
app.use(express.static(frontendPath));

// ✅ Express 5-compatible frontend handler
app.use((req, res, next) => {
  if (!req.path.startsWith('/api')) {
    res.sendFile(path.join(frontendPath, 'index.html'));
  } else {
    next();
  }
});

// ⚠️ Global error handler
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    success: false,
    message: 'Something went wrong!',
    error: process.env.NODE_ENV === 'development' ? err.message : 'Internal Server Error'
  });
});

// 🚫 404 handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Route not found'
  });
});

// 🚀 Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`✅ Server running on port ${PORT}`);
  console.log(`📊 Health: http://localhost:${PORT}/api/health`);
  console.log(`🌐 Frontend served from: ${frontendPath}`);
});
                