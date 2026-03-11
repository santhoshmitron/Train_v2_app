import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import Sidebar from './components/UI/Sidebar';
import Header from './components/UI/Header';
import Overview from './components/Dashboard/Overview';
import Customers from './pages/Customers';
import Gates from './pages/Gates';
import GateView from './pages/GateView';
import GateValues from './pages/GateValues';
import Login from './pages/Login';
import HelpLine from './pages/HelpLine';
import Contacts from './pages/Contacts';



const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000,
    },
  },
});

function Layout() {
  const location = useLocation();
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
  const isGateRoute = ['/gateview', '/gatevalues'].includes(location.pathname);
  const isLoginPage = location.pathname === '/';

  if (!isLoggedIn && !isGateRoute && !isLoginPage) {
    return <Navigate to="/" replace />;
  }

  if (isLoginPage) {
    return (
      <Routes>
        <Route path="/" element={<Login />} />
      </Routes>
    );
  }

  return (
    <div className="bg-gray-50 min-h-screen flex">
      {/* Sidebar */}
      <Sidebar />

      {/* Main content */}
      <div
        className="
          flex flex-col flex-1 
          lg:pl-64   /* ✅ Content starts after sidebar width */
          transition-all duration-300
        "
      >
        {/* Header (not for Gate routes) */}
        {!isGateRoute && <Header />}

        {/* Main section */}
        <main className="flex-1 p-4 md:p-6">
          <Routes>
            <Route path="/overview" element={<Overview />} />
            <Route path="/customers" element={<Customers />} />
            <Route path="/gates" element={<Gates />} />
            <Route path="/gateview" element={<GateView />} />
            <Route path="/gatevalues" element={<GateValues />} />
            <Route path="/helpline" element={<HelpLine />} />
            <Route path="/contacts" element={<Contacts />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3000,
            style: { background: '#363636', color: '#fff' },
          }}
        />
        <Layout />
      </Router>
    </QueryClientProvider>
  );
}

export default App;
