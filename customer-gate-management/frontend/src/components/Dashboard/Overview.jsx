import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  Users, 
  Shield, 
  TrendingUp, 
  Activity,
  UserCheck,
  UserX,
  Calendar,
  MapPin
} from 'lucide-react';
import { customerAPI, managegateAPI } from '../../services/api';
import CustomerChart from '../Charts/CustomerChart';
import GateChart from '../Charts/GateChart';
import LoadingSpinner from '../UI/LoadingSpinner';

const StatCard = ({ title, value, icon: Icon, trend, color = 'blue' }) => {
  const colorClasses = {
    blue: 'bg-blue-500 text-blue-600 bg-blue-50',
    green: 'bg-green-500 text-green-600 bg-green-50',
    red: 'bg-red-500 text-red-600 bg-red-50',
    yellow: 'bg-yellow-500 text-yellow-600 bg-yellow-50',
    purple: 'bg-purple-500 text-purple-600 bg-purple-50',
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 transition-all hover:shadow-md">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-600 mb-1">{title}</p>
          <p className="text-2xl font-bold text-gray-900">{value}</p>
          {trend && (
            <div className="flex items-center mt-2">
              <TrendingUp className="h-4 w-4 text-green-500 mr-1" />
              <span className="text-sm text-green-600">{trend}</span>
            </div>
          )}
        </div>
        <div className={`p-3 rounded-lg ${colorClasses[color].split(' ')[2]}`}>
          <Icon className={`h-6 w-6 ${colorClasses[color].split(' ')[1]}`} />
        </div>
      </div>
    </div>
  );
};

const Overview = () => {
  const { data: customerStats, isLoading: customersLoading } = useQuery({
    queryKey: ['customer-stats'],
    queryFn: () => customerAPI.getStats(),
  });

  const { data: gateStats, isLoading: gatesLoading } = useQuery({
    queryKey: ['gate-stats'],
    queryFn: () => managegateAPI.getStats(),
  });

  if (customersLoading || gatesLoading) {
    return <LoadingSpinner />;
  }

  const customerData = customerStats?.data?.overview || {};
  const gateData = gateStats?.data?.overview || {};

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard Overview</h1>
          <p className="text-gray-600 mt-1">Welcome back! Here's what's happening today.</p>
        </div>
        <div className="flex items-center space-x-2 mt-4 sm:mt-0">
          <Calendar className="h-5 w-5 text-gray-400" />
          <span className="text-sm text-gray-600">
            {new Date().toLocaleDateString('en-US', { 
              weekday: 'long', 
              year: 'numeric', 
              month: 'long', 
              day: 'numeric' 
            })}
          </span>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Customers"
          value={customerData.total || 0}
          icon={Users}
          trend={`+${customerData.today_added || 0} today`}
          color="blue"
        />
        <StatCard
          title="Active Customers"
          value={customerData.active || 0}
          icon={UserCheck}
          color="green"
        />
        <StatCard
          title="Total Gates"
          value={gateData.total || 0}
          icon={Shield}
          trend={`+${gateData.today_added || 0} today`}
          color="purple"
        />
        <StatCard
          title="Active Gates"
          value={gateData.gates_open || 0}
          icon={Activity}
          color="yellow"
        />
      </div>

      {/* Additional Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <StatCard
          title="Inactive Customers"
          value={customerData.inactive || 0}
          icon={UserX}
          color="red"
        />
        <StatCard
          title="This Week"
          value={customerData.week_added || 0}
          icon={Calendar}
          color="blue"
        />
        <StatCard
          title="This Month"
          value={customerData.month_added || 0}
          icon={TrendingUp}
          color="green"
        />
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Customer Analytics</h3>
          <CustomerChart />
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Gate Status</h3>
          <GateChart />
        </div>
      </div>

      {/* City Distribution */}
      {customerStats?.data?.cityDistribution && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <MapPin className="h-5 w-5 mr-2 text-gray-500" />
            Customer Distribution by City
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            {customerStats.data.cityDistribution.slice(0, 10).map((city, index) => (
              <div key={index} className="text-center p-4 bg-gray-50 rounded-lg">
                <p className="text-sm font-medium text-gray-900">{city.city}</p>
                <p className="text-2xl font-bold text-blue-600 mt-1">{city.count}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default Overview;
