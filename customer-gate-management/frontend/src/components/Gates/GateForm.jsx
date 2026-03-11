import React, { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { managegateAPI } from '../../services/api';
import toast from 'react-hot-toast';
import LoadingSpinner from '../UI/LoadingSpinner';

const GateForm = ({ gate, mode, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    Gate_Num: '',
    BOOM1_ID: '',
    BOOM2_ID: '',
    handle: '',
    SM: '',
    GM: '',
    status: 'Active',
    BS1_GO: '',
    BS1_GC: '',
    BS2_GO: '',
    BS2_GC: '',
    LS_GO: '',
    LS_GC: '',
    BS1_STATUS: 'open',
    BS2_STATUS: 'open',
    LEVER_STATUS: 'open',
    LT_STATUS: 'open',
    LTSW_ID: '',
    LT1_STATUS: 'open',
    LT2_STATUS: 'open',
    // Legacy field names for backward compatibility
    gateName: '',
    gateId: '',
    go: '',
    gc: '',
    ho: '',
    hc: '',
    gate_status: 'open',
    handle_status: 'open'
  });

  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (gate && mode !== 'create') {
      setFormData({
        Gate_Num: gate.Gate_Num || gate.gateName || '',
        BOOM1_ID: gate.BOOM1_ID || gate.gateId || '',
        BOOM2_ID: gate.BOOM2_ID || '',
        handle: gate.handle || '',
        SM: gate.SM || '',
        GM: gate.GM || '',
        status: gate.status || 'Active',
        BS1_GO: gate.BS1_GO || gate.go || '',
        BS1_GC: gate.BS1_GC || gate.gc || '',
        BS2_GO: gate.BS2_GO || '',
        BS2_GC: gate.BS2_GC || '',
        LS_GO: gate.LS_GO || gate.ho || '',
        LS_GC: gate.LS_GC || gate.hc || '',
        BS1_STATUS: gate.BS1_STATUS || gate.gate_status || 'open',
        BS2_STATUS: gate.BS2_STATUS || 'open',
        LEVER_STATUS: gate.LEVER_STATUS || gate.handle_status || 'open',
        LT_STATUS: gate.LT_STATUS || 'open',
        LTSW_ID: gate.LTSW_ID || '',
        LT1_STATUS: gate.LT1_STATUS || 'open',
        LT2_STATUS: gate.LT2_STATUS || 'open',
        // Legacy field names for backward compatibility
        gateName: gate.Gate_Num || gate.gateName || '',
        gateId: gate.BOOM1_ID || gate.gateId || '',
        go: gate.BS1_GO || gate.go || '',
        gc: gate.BS1_GC || gate.gc || '',
        ho: gate.LS_GO || gate.ho || '',
        hc: gate.LS_GC || gate.hc || '',
        gate_status: gate.BS1_STATUS || gate.gate_status || 'open',
        handle_status: gate.LEVER_STATUS || gate.handle_status || 'open'
      });
    }
  }, [gate, mode]);

  const createMutation = useMutation({
    mutationFn: managegateAPI.create,
    onSuccess: () => {
      toast.success('Gate created successfully');
      onSuccess();
    },
    onError: (error) => {
      const errorMsg = error.response?.data?.message || 'Failed to create gate';
      toast.error(errorMsg);
      if (error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      }
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => managegateAPI.update(id, data),
    onSuccess: () => {
      toast.success('Gate updated successfully');
      onSuccess();
    },
    onError: (error) => {
      const errorMsg = error.response?.data?.message || 'Failed to update gate';
      toast.error(errorMsg);
      if (error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      }
    }
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setErrors({});

    // Basic validation
    const newErrors = {};
    if (!formData.Gate_Num.trim() && !formData.gateName.trim()) {
      newErrors.Gate_Num = 'Gate number is required';
    }
    if (!formData.BOOM1_ID.trim() && !formData.gateId.trim()) {
      newErrors.BOOM1_ID = 'Boom 1 ID is required';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // Prepare data with both new and legacy field names for API compatibility
    const submitData = {
      Gate_Num: formData.Gate_Num || formData.gateName,
      BOOM1_ID: formData.BOOM1_ID || formData.gateId,
      BOOM2_ID: formData.BOOM2_ID || null,
      handle: formData.handle || null,
      SM: formData.SM || null,
      GM: formData.GM || null,
      status: formData.status || 'Active',
      BS1_GO: formData.BS1_GO || formData.go || null,
      BS1_GC: formData.BS1_GC || formData.gc || null,
      BS2_GO: formData.BS2_GO || null,
      BS2_GC: formData.BS2_GC || null,
      LS_GO: formData.LS_GO || formData.ho || null,
      LS_GC: formData.LS_GC || formData.hc || null,
      BS1_STATUS: formData.BS1_STATUS || formData.gate_status || 'open',
      BS2_STATUS: formData.BS2_STATUS || 'open',
      LEVER_STATUS: formData.LEVER_STATUS || formData.handle_status || 'open',
      LT_STATUS: formData.LT_STATUS || 'open',
      LTSW_ID: formData.LTSW_ID || null,
      LT1_STATUS: formData.LT1_STATUS || 'open',
      LT2_STATUS: formData.LT2_STATUS || 'open'
    };

    if (mode === 'create') {
      createMutation.mutate(submitData);
    } else if (mode === 'edit') {
      updateMutation.mutate({ id: gate.id, data: submitData });
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
            <label className="block text-sm font-medium text-gray-700 mb-2">Gate Number</label>
            <p className="text-sm text-gray-900">{gate?.Gate_Num || gate?.gateName || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Boom 1 ID</label>
            <p className="text-sm text-gray-900">{gate?.BOOM1_ID || gate?.gateId || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Boom 2 ID</label>
            <p className="text-sm text-gray-900">{gate?.BOOM2_ID || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Handle/Lever</label>
            <p className="text-sm text-gray-900">{gate?.handle || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">SM</label>
            <p className="text-sm text-gray-900">{gate?.SM || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">GM</label>
            <p className="text-sm text-gray-900">{gate?.GM || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Status</label>
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              (gate?.status === 'Active' || gate?.status === 'active')
                ? 'bg-green-100 text-green-800' 
                : 'bg-red-100 text-red-800'
            }`}>
              {gate?.status || 'N/A'}
            </span>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">BS1 Status</label>
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              (gate?.BS1_STATUS || gate?.gate_status) === 'open' 
                ? 'bg-blue-100 text-blue-800' 
                : 'bg-gray-100 text-gray-800'
            }`}>
              {gate?.BS1_STATUS || gate?.gate_status || 'N/A'}
            </span>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">BS2 Status</label>
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              gate?.BS2_STATUS === 'open' 
                ? 'bg-blue-100 text-blue-800' 
                : gate?.BS2_STATUS === 'closed' ? 'bg-gray-100 text-gray-800' : 'bg-gray-50 text-gray-500'
            }`}>
              {gate?.BS2_STATUS || 'N/A'}
            </span>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Lever Status</label>
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              (gate?.LEVER_STATUS || gate?.handle_status) === 'open' 
                ? 'bg-blue-100 text-blue-800' 
                : 'bg-gray-100 text-gray-800'
            }`}>
              {gate?.LEVER_STATUS || gate?.handle_status || 'N/A'}
            </span>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">LT Status</label>
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              gate?.LT_STATUS === 'open' 
                ? 'bg-blue-100 text-blue-800' 
                : gate?.LT_STATUS === 'closed' ? 'bg-gray-100 text-gray-800' : 'bg-gray-50 text-gray-500'
            }`}>
              {gate?.LT_STATUS || 'N/A'}
            </span>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">BS1 GO</label>
            <p className="text-sm text-gray-900">{gate?.BS1_GO || gate?.go || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">BS1 GC</label>
            <p className="text-sm text-gray-900">{gate?.BS1_GC || gate?.gc || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">BS2 GO</label>
            <p className="text-sm text-gray-900">{gate?.BS2_GO || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">BS2 GC</label>
            <p className="text-sm text-gray-900">{gate?.BS2_GC || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">LS GO</label>
            <p className="text-sm text-gray-900">{gate?.LS_GO || gate?.ho || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">LS GC</label>
            <p className="text-sm text-gray-900">{gate?.LS_GC || gate?.hc || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">LTSW ID</label>
            <p className="text-sm text-gray-900">{gate?.LTSW_ID || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">LT1 Status</label>
            <p className="text-sm text-gray-900">{gate?.LT1_STATUS || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">LT2 Status</label>
            <p className="text-sm text-gray-900">{gate?.LT2_STATUS || 'N/A'}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label htmlFor="Gate_Num" className="block text-sm font-medium text-gray-700 mb-2">
            Gate Number <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="Gate_Num"
            name="Gate_Num"
            value={formData.Gate_Num}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              errors.Gate_Num ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="Enter gate number"
          />
          {errors.Gate_Num && <p className="mt-1 text-sm text-red-600">{errors.Gate_Num}</p>}
        </div>

        <div>
          <label htmlFor="BOOM1_ID" className="block text-sm font-medium text-gray-700 mb-2">
            Boom 1 ID <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="BOOM1_ID"
            name="BOOM1_ID"
            value={formData.BOOM1_ID}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              errors.BOOM1_ID ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="Enter Boom 1 ID"
          />
          {errors.BOOM1_ID && <p className="mt-1 text-sm text-red-600">{errors.BOOM1_ID}</p>}
        </div>

        <div>
          <label htmlFor="BOOM2_ID" className="block text-sm font-medium text-gray-700 mb-2">
            Boom 2 ID
          </label>
          <input
            type="text"
            id="BOOM2_ID"
            name="BOOM2_ID"
            value={formData.BOOM2_ID}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter Boom 2 ID"
          />
        </div>

        <div>
          <label htmlFor="handle" className="block text-sm font-medium text-gray-700 mb-2">
            Handle/Lever
          </label>
          <input
            type="text"
            id="handle"
            name="handle"
            value={formData.handle}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter handle"
          />
        </div>

        <div>
          <label htmlFor="SM" className="block text-sm font-medium text-gray-700 mb-2">
            SM
          </label>
          <input
            type="text"
            id="SM"
            name="SM"
            value={formData.SM}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter SM value"
          />
        </div>

        <div>
          <label htmlFor="GM" className="block text-sm font-medium text-gray-700 mb-2">
            GM
          </label>
          <input
            type="text"
            id="GM"
            name="GM"
            value={formData.GM}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter GM value"
          />
        </div>

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
            <option value="Active">Active</option>
            <option value="Inactive">Inactive</option>
          </select>
        </div>

        <div>
          <label htmlFor="BS1_STATUS" className="block text-sm font-medium text-gray-700 mb-2">
            BS1 Status
          </label>
          <select
            id="BS1_STATUS"
            name="BS1_STATUS"
            value={formData.BS1_STATUS}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="open">Open</option>
            <option value="closed">Closed</option>
          </select>
        </div>

        <div>
          <label htmlFor="BS2_STATUS" className="block text-sm font-medium text-gray-700 mb-2">
            BS2 Status
          </label>
          <select
            id="BS2_STATUS"
            name="BS2_STATUS"
            value={formData.BS2_STATUS}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="open">Open</option>
            <option value="closed">Closed</option>
          </select>
        </div>

        <div>
          <label htmlFor="LEVER_STATUS" className="block text-sm font-medium text-gray-700 mb-2">
            Lever Status
          </label>
          <select
            id="LEVER_STATUS"
            name="LEVER_STATUS"
            value={formData.LEVER_STATUS}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="open">Open</option>
            <option value="closed">Closed</option>
          </select>
        </div>

        <div>
          <label htmlFor="LT_STATUS" className="block text-sm font-medium text-gray-700 mb-2">
            LT Status
          </label>
          <select
            id="LT_STATUS"
            name="LT_STATUS"
            value={formData.LT_STATUS}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="open">Open</option>
            <option value="closed">Closed</option>
          </select>
        </div>

        <div>
          <label htmlFor="BS1_GO" className="block text-sm font-medium text-gray-700 mb-2">
            BS1 GO
          </label>
          <input
            type="text"
            id="BS1_GO"
            name="BS1_GO"
            value={formData.BS1_GO}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter BS1 GO value"
          />
        </div>

        <div>
          <label htmlFor="BS1_GC" className="block text-sm font-medium text-gray-700 mb-2">
            BS1 GC
          </label>
          <input
            type="text"
            id="BS1_GC"
            name="BS1_GC"
            value={formData.BS1_GC}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter BS1 GC value"
          />
        </div>

        <div>
          <label htmlFor="BS2_GO" className="block text-sm font-medium text-gray-700 mb-2">
            BS2 GO
          </label>
          <input
            type="text"
            id="BS2_GO"
            name="BS2_GO"
            value={formData.BS2_GO}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter BS2 GO value"
          />
        </div>

        <div>
          <label htmlFor="BS2_GC" className="block text-sm font-medium text-gray-700 mb-2">
            BS2 GC
          </label>
          <input
            type="text"
            id="BS2_GC"
            name="BS2_GC"
            value={formData.BS2_GC}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter BS2 GC value"
          />
        </div>

        <div>
          <label htmlFor="LS_GO" className="block text-sm font-medium text-gray-700 mb-2">
            LS GO
          </label>
          <input
            type="text"
            id="LS_GO"
            name="LS_GO"
            value={formData.LS_GO}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter LS GO value"
          />
        </div>

        <div>
          <label htmlFor="LS_GC" className="block text-sm font-medium text-gray-700 mb-2">
            LS GC
          </label>
          <input
            type="text"
            id="LS_GC"
            name="LS_GC"
            value={formData.LS_GC}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter LS GC value"
          />
        </div>

        <div>
          <label htmlFor="LTSW_ID" className="block text-sm font-medium text-gray-700 mb-2">
            LTSW ID
          </label>
          <input
            type="text"
            id="LTSW_ID"
            name="LTSW_ID"
            value={formData.LTSW_ID}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter LTSW ID"
          />
        </div>

        <div>
          <label htmlFor="LT1_STATUS" className="block text-sm font-medium text-gray-700 mb-2">
            LT1 Status
          </label>
          <select
            id="LT1_STATUS"
            name="LT1_STATUS"
            value={formData.LT1_STATUS}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="open">Open</option>
            <option value="closed">Closed</option>
          </select>
        </div>

        <div>
          <label htmlFor="LT2_STATUS" className="block text-sm font-medium text-gray-700 mb-2">
            LT2 Status
          </label>
          <select
            id="LT2_STATUS"
            name="LT2_STATUS"
            value={formData.LT2_STATUS}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="open">Open</option>
            <option value="closed">Closed</option>
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
            mode === 'create' ? 'Create Gate' : 'Update Gate'
          )}
        </button>
      </div>
    </form>
  );
};

export default GateForm;
