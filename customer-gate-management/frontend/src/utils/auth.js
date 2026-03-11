/**
 * Check if the current user is an admin
 * @returns {boolean} True if user is admin, false otherwise
 */
export const isAdmin = () => {
  const userType = localStorage.getItem('userType');
  const username = localStorage.getItem('username');
  
  // Check if user type is admin or username is admin
  return userType === 'admin' || username === 'admin';
};

/**
 * Get current user type
 * @returns {string|null} User type or null if not set
 */
export const getUserType = () => {
  return localStorage.getItem('userType');
};

/**
 * Set user type in localStorage
 * @param {string} type - User type (e.g., 'admin', 'user')
 */
export const setUserType = (type) => {
  localStorage.setItem('userType', type);
};

/**
 * Set username in localStorage
 * @param {string} username - Username
 */
export const setUsername = (username) => {
  localStorage.setItem('username', username);
};
