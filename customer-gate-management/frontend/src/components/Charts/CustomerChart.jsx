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
import { customerAPI } from '../../services/api';
import LoadingSpinner from '../UI/LoadingSpinner';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

const CustomerChart = () => {
  const { data: customerStats, isLoading } = useQuery({
    queryKey: ['customer-stats'],
    queryFn: () => customerAPI.getStats(),
  });

  if (isLoading) return <LoadingSpinner />;

  const overview = customerStats?.data?.overview || {};
  const cityData = customerStats?.data?.cityDistribution?.slice(0, 5) || [];
  const typeData = customerStats?.data?.typeDistribution || [];

  const statusData = [
    { name: 'Active', value: overview.active || 0, color: '#10b981' },
    { name: 'Inactive', value: overview.inactive || 0, color: '#ef4444' },
  ];

  const timeData = [
    { period: 'Today', customers: overview.today_added || 0 },
    { period: 'This Week', customers: overview.week_added || 0 },
    { period: 'This Month', customers: overview.month_added || 0 },
  ];

  return (
    <div className="space-y-6">
      {/* Status Pie Chart */}
      <div>
        <h4 className="text-md font-medium text-gray-700 mb-3">Customer Status</h4>
        <ResponsiveContainer width="100%" height={200}>
          <PieChart>
            <Pie
              data={statusData}
              cx="50%"
              cy="50%"
              innerRadius={40}
              outerRadius={80}
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

      {/* Time-based Bar Chart */}
      <div>
        <h4 className="text-md font-medium text-gray-700 mb-3">Recent Activity</h4>
        <ResponsiveContainer width="100%" height={200}>
          <BarChart data={timeData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="period" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="customers" fill="#3b82f6" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* City Distribution */}
      {cityData.length > 0 && (
        <div>
          <h4 className="text-md font-medium text-gray-700 mb-3">Top Cities</h4>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={cityData} layout="horizontal">
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis type="number" />
              <YAxis dataKey="city" type="category" width={80} />
              <Tooltip />
              <Bar dataKey="count" fill="#10b981" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
};

export default CustomerChart;
