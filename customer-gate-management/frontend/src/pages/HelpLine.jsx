import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { helplineAPI } from '../services/api';
import LoadingSpinner from '../components/UI/LoadingSpinner';

const HelpLine = () => {
  const [phones, setPhones] = useState({ phone1: '', phone2: '', phone3: '' });
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(true); // loading for fetch

  const handleChange = (e) => {
    const { name, value } = e.target;
    setPhones(prev => ({ ...prev, [name]: value }));
  };

  const fetchHelplinePhones = async () => {
    try {
      setLoadingData(true);
      const response = await helplineAPI.getPhones();
      if (response?.Details) {
        setPhones(response.Details);
      }
    } catch (error) {
      toast.error('Failed to load helpline phone numbers');
    } finally {
      setLoadingData(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await helplineAPI.upsertPhones(phones);
      toast.success('Helpline phone numbers saved successfully');
    } catch (error) {
      toast.error('Failed to save helpline phone numbers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHelplinePhones();
  }, []);

  if (loadingData) return <LoadingSpinner />;

  return (
    <div className="max-w-xl mx-auto p-4 bg-white shadow rounded">
      <h2 className="text-xl font-semibold mb-4">Help Line Phone Numbers</h2>
      <form onSubmit={handleSubmit} className="space-y-4">
        {['phone1', 'phone2', 'phone3'].map((p) => (
          <div key={p}>
            <label htmlFor={p} className="block font-medium text-gray-700 mb-1">
              {p.charAt(0).toUpperCase() + p.slice(1)}
            </label>
            <input
              type="tel"
              id={p}
              name={p}
              value={phones[p] || ''}
              onChange={handleChange}
              placeholder={`Enter ${p}`}
              className="w-full p-2 border rounded"
            />
          </div>
        ))}
        <button
          type="submit"
          disabled={loading}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition"
        >
          {loading ? <LoadingSpinner size="small" /> : 'Save Phones'}
        </button>
      </form>
    </div>
  );
};

export default HelpLine;
