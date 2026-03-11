import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  HomeIcon,
  UsersIcon,
  ShieldCheckIcon,
  ChartBarIcon,
  Bars3Icon,
  XMarkIcon,
  PhoneIcon,
} from '@heroicons/react/24/outline';
import { isAdmin } from '../../utils/auth';

const Sidebar = () => {
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
  const isGateRoute = ['/gateview', '/gatevalues'].includes(location.pathname);
  const userIsAdmin = isAdmin();

  const mainNavigation = [
    { name: 'Overview', href: '/overview', icon: HomeIcon },
    { name: 'Customers', href: '/customers', icon: UsersIcon },
    { name: 'Gates', href: '/gates', icon: ShieldCheckIcon },
    { name: 'Grafana', href: '/gateview', icon: ShieldCheckIcon },
    { name: 'Sensors Values', href: '/gatevalues', icon: ShieldCheckIcon },
    { name: 'Help Line', href: '/helpline', icon: UsersIcon },
    // Contacts - only visible to admin
    ...(userIsAdmin ? [{ name: 'Contacts', href: '/contacts', icon: PhoneIcon }] : []),
  ];

  const gateNavigation = [
    { name: 'Gate View', href: '/gateview', icon: ShieldCheckIcon },
    { name: 'Gate Values', href: '/gatevalues', icon: ShieldCheckIcon },
  ];

  const navigation = isGateRoute && !isLoggedIn ? gateNavigation : mainNavigation;

  return (
    <>
      {/* Hamburger (mobile only) */}
      <button
        className="fixed top-4 left-4 z-50 lg:hidden flex items-center px-2 py-2 rounded-md text-gray-900 bg-white shadow border border-gray-200"
        onClick={() => setSidebarOpen(true)}
        aria-label="Open sidebar"
      >
        <Bars3Icon className="h-6 w-6" />
      </button>

      {/* Sidebar container */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 bg-white shadow-lg border-r border-gray-200 transform transition-transform duration-300 ease-in-out
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
          lg:translate-x-0
        `}
      >
        {/* Close button for mobile */}
        <div className="flex lg:hidden justify-end p-4">
          <button onClick={() => setSidebarOpen(false)} aria-label="Close sidebar">
            <XMarkIcon className="h-6 w-6 text-gray-900" />
          </button>
        </div>

        {/* Sidebar content */}
        <div className="flex flex-col h-full">
          {/* Logo section */}
          <div className="flex items-center h-16 px-6 border-b border-gray-200">
            <ChartBarIcon className="h-8 w-8 text-blue-600" />
            <span className="ml-2 text-xl font-bold text-gray-900">Management</span>
          </div>

          {/* Navigation list */}
          <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
            {navigation.map((item) => (
              <NavLink
                key={item.name}
                to={item.href}
                className={({ isActive }) =>
                  `flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors ${
                    isActive
                      ? 'bg-blue-50 text-blue-700 border-r-2 border-blue-600'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  }`
                }
                onClick={() => setSidebarOpen(false)}
              >
                <item.icon className="h-5 w-5 mr-3" />
                {item.name}
              </NavLink>
            ))}
          </nav>
        </div>
      </aside>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-40 z-30 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        ></div>
      )}
    </>
  );
};

export default Sidebar;
