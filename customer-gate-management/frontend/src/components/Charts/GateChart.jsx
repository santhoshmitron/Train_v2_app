import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  PieChart, 
  Pie, 
  Cell,
  ResponsiveContainer 
} from 'recharts';
import { managegateAPI } from '../../services/api';
import LoadingSpinner from '../UI/LoadingSpinner';

const COLORS = ['#10b981', '#ef4444', '#3b82f6', '#f59e0b', '#8b5cf6'];

const GateChart = () => {
  const { data: gateStats, isLoading } = useQuery({
    queryKey: ['gate-stats'],
    queryFn: () => managegateAPI.getStats(),
  });

  if (isLoading) return <LoadingSpinner />;

  const overview = gateStats?.data?.overview || {};
  const statusDistribution = gateStats?.data?.statusDistribution || [];

  const statusData = [
    { name: 'Active', value: overview.active || 0, color: '#10b981' },
    { name: 'Inactive', value: overview.inactive || 0, color: '#ef4444' },
  ];

  const gateStatusData = [
    { name: 'Open', value: overview.gates_open || 0, color: '#3b82f6' },
    { name: 'Closed', value: overview.gates_closed || 0, color: '#6b7280' },
  ];

  const summaryData = [
    { status: 'Total Gates', count: overview.total || 0 },
    { status: 'Active', count: overview.active || 0 },
    { status: 'Open Gates', count: overview.gates_open || 0 },
    { status: 'Closed Gates', count: overview.gates_closed || 0 },
  ];

  return (
    <div className="space-y-6">
      {/* Gate Status Pie Chart */}
      <div>
        <h4 className="text-md font-medium text-gray-700 mb-3">Gate Status Distribution</h4>
        <ResponsiveContainer width="100%" height={200}>
          <PieChart>
            <Pie
              data={gateStatusData}
              cx="50%"
              cy="50%"
              innerRadius={40}
              outerRadius={80}
              paddingAngle={5}
              dataKey="value"
            >
              {gateStatusData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>

      {/* Active/Inactive Status */}
      <div>
        <h4 className="text-md font-medium text-gray-700 mb-3">System Status</h4>
        <ResponsiveContainer width="100%" height={200}>
          <PieChart>
            <Pie
              data={statusData}
              cx="50%"
              cy="50%"
              innerRadius={30}
              outerRadius={70}
              paddingAngle={5}
              dataKey="value"
            >
              {statusData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>

      {/* Summary Bar Chart */}
      <div>
        <h4 className="text-md font-medium text-gray-700 mb-3">Overview Summary</h4>
        <ResponsiveContainer width="100%" height={200}>
          <BarChart data={summaryData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="status" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Status Distribution from API */}
      {statusDistribution.length > 0 && (
        <div>
          <h4 className="text-md font-medium text-gray-700 mb-3">Detailed Status</h4>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={statusDistribution}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="gate_status" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#10b981" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
};

export default GateChart;
