import axios from 'axios';
import toast from 'react-hot-toast';

// Centralized API Base URL Configuration
// Can be overridden by VITE_API_URL environment variable
export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || error.message || 'Something went wrong';
    if (!error.config._retry) {
      toast.error(message);
    }
    return Promise.reject(error);
  }
);

// Customer API with gate-based relationship methods
export const customerAPI = {
  getAll: (params = {}) => {
    const apiParams = { ...params };
    if (params.role) {
      apiParams.roles = params.role;
      delete apiParams.role;
    }
    return api.get('/customers', { params: apiParams });
  },
  getById: (id) => api.get(`/customers/${id}`),
  create: (data) => api.post('/customers', data),
  update: (id, data) => api.put(`/customers/${id}`, data),
  delete: (id) => api.delete(`/customers/${id}`),
  getStats: () => api.get('/customers/stats'),

  // New methods for gate-based relationships
  getSMWithGates: (params = {}) => api.get('/customers/sm-with-gates', { params }),
  getGMUnderSM: (smUsername, params = {}) => api.get(`/customers/gm-under-sm/${smUsername}`, { params }),
  getGatesUnderSM: (smUsername) => api.get(`/customers/gates-under-sm/${smUsername}`),

};


export const helplineAPI = {
  getPhones: () => api.get('/customers/helpline'),
  upsertPhones: (phones) => api.post('/customers/helpline', phones),
};

// Managegate API (unchanged)
export const managegateAPI = {
  getAll: (params = {}) => api.get('/managegates', { params }),
  getById: (id) => api.get(`/managegates/${id}`),
  create: (data) => api.post('/managegates', data),
  update: (id, data) => api.put(`/managegates/${id}`, data),
  delete: (id) => api.delete(`/managegates/${id}`),
  getStats: () => api.get('/managegates/stats'),
  getBSLSIds: () => api.get('/managegates/bs-ls-ids'), // <-- THIS LINE (NEW)

};

// Contact API
export const contactAPI = {
  getAll: (params = {}) => api.get('/contacts', { params }),
  getById: (id) => api.get(`/contacts/${id}`),
  create: (data) => api.post('/contacts', data),
  update: (id, data) => api.put(`/contacts/${id}`, data),
  delete: (id) => api.delete(`/contacts/${id}`),
};

export default api;
