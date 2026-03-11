import React, { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { customerAPI } from '../../services/api';
import toast from 'react-hot-toast';
import LoadingSpinner from '../UI/LoadingSpinner';

const CustomerForm = ({ customer, mode, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    name: '',
    first_name: '',
    mobile: '',
    phone: '',
    pic: '',
    type: '',
    shop_name: '',
    status: 'active',
    logo: '',
    roles: '', // "SM" or "GM"
    instruction: '',
  });

  const [errors, setErrors] = useState({});

  // Initialize form with existing customer data if editing or viewing
  useEffect(() => {
    if (customer && mode !== 'create') {
      setFormData({
        username: customer.username || '',
        password: customer.password || '',
        name: customer.name || '',
        first_name: customer.first_name || '',
        mobile: customer.mobile || '',
        phone: customer.phone || '',
        pic: customer.pic || '',
        type: customer.type || '',
        shop_name: customer.shop_name || customer.name || '',
        status: customer.status || 'active',
        logo: customer.logo || '',
        roles: customer.roles || '',
        instruction: customer.instruction || '',
      });
    }
  }, [customer, mode]);

  // Sync shop_name with name field live unless user manually edits shop_name
  const [shopNameManuallyEdited, setShopNameManuallyEdited] = useState(false);

  // Update shop_name if name changes and shopName not manually edited
  useEffect(() => {
    if (!shopNameManuallyEdited) {
      setFormData(prev => ({ ...prev, shop_name: prev.name }));
    }
  }, [formData.name, shopNameManuallyEdited]);

  const createMutation = useMutation({
    mutationFn: customerAPI.create,
    onSuccess: () => {
      toast.success('Customer created successfully');
      onSuccess();
    },
    onError: (error) => {
      const errorMsg = error.response?.data?.message || 'Failed to create customer';
      toast.error(errorMsg);
      if (error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      }
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => customerAPI.update(id, data),
    onSuccess: () => {
      toast.success('Customer updated successfully');
      onSuccess();
    },
    onError: (error) => {
      const errorMsg = error.response?.data?.message || 'Failed to update customer';
      toast.error(errorMsg);
      if (error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      }
    }
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setErrors({});
    const newErrors = {};
    if (!formData.name.trim()) newErrors.name = 'Name is required';
    if (!formData.username.trim()) newErrors.username = 'Username is required';
    // if (!formData.mobile.trim()) newErrors.mobile = 'Mobile is required';
    if (!formData.password.trim() && mode === 'create') newErrors.password = 'Password is required';
    if (!formData.roles.trim()) newErrors.roles = 'Role is required';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    if (mode === 'edit') {
      updateMutation.mutate({ id: customer.id, data: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;

    // If user edits shop_name manually, mark flag true
    if (name === 'shop_name') {
      setShopNameManuallyEdited(true);
    }
    // If user edits name, reset manual flag so shop_name updates accordingly
    if (name === 'name') {
      setShopNameManuallyEdited(false);
    }

    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const isLoading = createMutation.isLoading || updateMutation.isLoading;

  // Dynamic labels and placeholders based on role
  const currentRole = formData.roles || customer?.roles || '';
  const labelName = currentRole === 'SM' ? 'Station Name' : currentRole === 'GM' ? 'Gate Name' : 'Name';
  const labelUsername = currentRole === 'SM' ? 'SM App Username' : currentRole === 'GM' ? 'GM App Username' : 'Username';
  const labelFirstName = currentRole === 'SM' ? 'Station Master  Name' : currentRole === 'GM' ? 'Gate Man  Name' : 'First Name';

  if (mode === 'view') {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">{labelName}</label>
            <p className="text-sm text-gray-900">{customer?.name || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">{labelFirstName}</label>
            <p className="text-sm text-gray-900">{customer?.first_name || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">{labelUsername}</label>
            <p className="text-sm text-gray-900">{customer?.username || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Password</label>
            <p className="text-sm text-gray-900">{customer?.password || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Mobile</label>
            <p className="text-sm text-gray-900">{customer?.mobile || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Phone (From App)</label>
            <p className="text-sm text-gray-900">{customer?.phone || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Shop Name (Station/Gate Name)</label>
            <p className="text-sm text-gray-900">{customer?.shop_name || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Status</label>
            <span
              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                customer?.status === 'active'
                  ? 'bg-green-100 text-green-800'
                  : 'bg-red-100 text-red-800'
              }`}
            >
              {customer?.status || 'N/A'}
            </span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
         {/* Roles */}
         <div>
          <label htmlFor="roles" className="block text-sm font-medium text-gray-700 mb-2">
            Role <span className="text-red-500">*</span>
          </label>
          <select
            id="roles"
            name="roles"
            value={formData.roles}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              errors.roles ? 'border-red-500' : 'border-gray-300'
            }`}
          >
            <option value="">Select role</option>
            <option value="SM">SM</option>
            <option value="GM">GM</option>
          </select>
          {errors.roles && <p className="mt-1 text-sm text-red-600">{errors.roles}</p>}
        </div>
        {/* Name */}
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
            {labelName} <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="name"
            name="name"
            value={formData.name}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              errors.name ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder={`Enter ${labelName.toLowerCase()}`}
          />
          {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name}</p>}
        </div>

        {/* Shop Name */}
        <div>
          <label htmlFor="shop_name" className="block text-sm font-medium text-gray-700 mb-2">
            Shop Name (Station/Gate Name)
          </label>
          <input
            type="text"
            id="shop_name"
            name="shop_name"
            value={formData.shop_name}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Shop name - mirrors Station/Gate Name"
          />
        </div>

        {/* First Name */}
        <div>
          <label htmlFor="first_name" className="block text-sm font-medium text-gray-700 mb-2">
            {labelFirstName}
          </label>
          <input
            type="text"
            id="first_name"
            name="first_name"
            value={formData.first_name}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder={`Enter ${labelFirstName.toLowerCase()}`}
          />
        </div>

        {/* Username */}
        <div>
          <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
            {labelUsername} <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="username"
            name="username"
            value={formData.username}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              errors.username ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder={`Enter ${labelUsername.toLowerCase()}`}
          />
          {errors.username && <p className="mt-1 text-sm text-red-600">{errors.username}</p>}
        </div>
        
        {/* Password */}
        <div>
          <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
            Password <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              errors.password ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="Enter password"
            autoComplete="off"
          />
          {errors.password && <p className="mt-1 text-sm text-red-600">{errors.password}</p>}
        </div>

        {/* Mobile */}
        <div>
          <label htmlFor="mobile" className="block text-sm font-medium text-gray-700 mb-2">
            Mobile
          </label>
          <input
            type="tel"
            id="mobile"
            name="mobile"
            value={formData.mobile}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              errors.mobile ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="Enter mobile number"
          />
          {errors.mobile && <p className="mt-1 text-sm text-red-600">{errors.mobile}</p>}
        </div>

        {/* Phone */}
        <div>
          <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-2">
            Phone (From App)
          </label>
          <input
            type="tel"
            id="phone"
            name="phone"
            value={formData.phone}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="From app"
          />
        </div>

       

        {/* Type */}
        <div>
          <label htmlFor="type" className="block text-sm font-medium text-gray-700 mb-2">
            Type
          </label>
          <select
            id="type"
            name="type"
            value={formData.type}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="">Select type</option>
            <option value="user">User</option>
            <option value="admin">Admin</option>
          </select>
        </div>

        {/* Status */}
        <div>
          <label htmlFor="status" className="block text-sm font-medium text-gray-700 mb-2">
            Status
          </label>
          <select
            id="status"
            name="status"
            value={formData.status}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </div>
      </div>

      <div className="flex justify-end space-x-4 pt-6 border-t border-gray-200">
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2 text-gray-700 bg-gray-200 rounded-lg hover:bg-gray-300 transition-colors"
          disabled={isLoading}
        >
          Cancel
        </button>
        <button
          type="submit"
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
          disabled={isLoading}
        >
          {isLoading ? (
            <div className="flex items-center">
              <LoadingSpinner size="small" />
              <span className="ml-2">
                {mode === 'create' ? 'Creating...' : 'Updating...'}
              </span>
            </div>
          ) : (
            mode === 'create' ? 'Create Customer' : 'Update Customer'
          )}
        </button>
      </div>
    </form>
  );
};

export default CustomerForm;
