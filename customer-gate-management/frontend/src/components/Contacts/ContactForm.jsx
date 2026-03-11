import React, { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { contactAPI } from '../../services/api';
import toast from 'react-hot-toast';
import LoadingSpinner from '../UI/LoadingSpinner';

const ContactForm = ({ contact, mode, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: '',
    phone_number: ''
  });

  const [errors, setErrors] = useState({});

  // Initialize form with existing contact data if editing or viewing
  useEffect(() => {
    if (contact && mode !== 'create') {
      setFormData({
        name: contact.name || '',
        phone_number: contact.phone_number || ''
      });
    }
  }, [contact, mode]);

  const createMutation = useMutation({
    mutationFn: contactAPI.create,
    onSuccess: () => {
      toast.success('Contact created successfully');
      onSuccess();
    },
    onError: (error) => {
      const errorMsg = error.response?.data?.message || 'Failed to create contact';
      toast.error(errorMsg);
      if (error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      }
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => contactAPI.update(id, data),
    onSuccess: () => {
      toast.success('Contact updated successfully');
      onSuccess();
    },
    onError: (error) => {
      const errorMsg = error.response?.data?.message || 'Failed to update contact';
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
    if (!formData.phone_number.trim()) newErrors.phone_number = 'Phone number is required';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    if (mode === 'edit') {
      updateMutation.mutate({ id: contact.id, data: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const isLoading = createMutation.isLoading || updateMutation.isLoading;

  if (mode === 'view') {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Name</label>
            <p className="text-sm text-gray-900">{contact?.name || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Phone Number</label>
            <p className="text-sm text-gray-900">{contact?.phone_number || 'N/A'}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
            Name <span className="text-red-500">*</span>
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
            placeholder="Enter contact name"
          />
          {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name}</p>}
        </div>

        <div>
          <label htmlFor="phone_number" className="block text-sm font-medium text-gray-700 mb-2">
            Phone Number <span className="text-red-500">*</span>
          </label>
          <input
            type="tel"
            id="phone_number"
            name="phone_number"
            value={formData.phone_number}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              errors.phone_number ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="Enter phone number"
          />
          {errors.phone_number && <p className="mt-1 text-sm text-red-600">{errors.phone_number}</p>}
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
            mode === 'create' ? 'Create Contact' : 'Update Contact'
          )}
        </button>
      </div>
    </form>
  );
};

export default ContactForm;
