import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  PlusIcon, 
  MagnifyingGlassIcon,
  FunnelIcon,
  PencilSquareIcon,
  TrashIcon,
  EyeIcon,
  ShieldCheckIcon,
  ShieldExclamationIcon,
  CalendarIcon,
  LockClosedIcon,
  LockOpenIcon
} from '@heroicons/react/24/outline';
import { managegateAPI } from '../services/api';
import toast from 'react-hot-toast';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import Modal from '../components/UI/Modal';
import GateForm from '../components/Gates/GateForm';
import { useDebounce } from '../hooks/useDebounce';


const StatusBadge = ({ status }) => {
  const statusConfig = {
    Active: { color: 'bg-green-100 text-green-800', label: 'Active' },
    Inactive: { color: 'bg-red-100 text-red-800', label: 'Inactive' },
    open: { color: 'bg-blue-100 text-blue-800', label: 'Open' },
    closed: { color: 'bg-gray-100 text-gray-800', label: 'Closed' },
  };
  const config = statusConfig[status] || statusConfig.open;

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.color}`}>
      {config.label}
    </span>
  );
};


const GateCard = ({ gate, onEdit, onDelete, onView }) => {
  const gateStatus = gate.BS1_STATUS || gate.gate_status || 'open';
  const gateName = gate.Gate_Num || gate.gateName || 'N/A';
  const gateId = gate.BOOM1_ID || gate.gateId || 'N/A';
  
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition-all duration-200">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center space-x-3">
          <div className={`h-12 w-12 rounded-full flex items-center justify-center ${
            gateStatus === 'open' 
              ? 'bg-gradient-to-br from-green-500 to-green-600' 
              : 'bg-gradient-to-br from-red-500 to-red-600'
          }`}>
            {gateStatus === 'open' ? (
              <LockOpenIcon className="h-6 w-6 text-white" />
            ) : (
              <LockClosedIcon className="h-6 w-6 text-white" />
            )}
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900">{gateName}</h3>
            <p className="text-sm text-gray-500">Boom 1 ID: {gateId}</p>
            {gate.BOOM2_ID && (
              <p className="text-xs text-gray-400">Boom 2 ID: {gate.BOOM2_ID}</p>
            )}
          </div>
        </div>
      </div>

      <div className="space-y-3 mb-4">
        {gate.handle && (
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">Handle:</span>
            <span className="text-sm font-medium text-gray-900">{gate.handle}</span>
          </div>
        )}
        {gate.SM && (
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">SM:</span>
            <span className="text-sm font-medium text-gray-900">{gate.SM}</span>
          </div>
        )}
        {gate.GM && (
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">GM:</span>
            <span className="text-sm font-medium text-gray-900">{gate.GM}</span>
          </div>
        )}
        <div className="flex items-center justify-between">
          <span className="text-sm text-gray-600">BS1 Status:</span>
          <StatusBadge status={gateStatus} />
        </div>
        {gate.BS2_STATUS && (
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">BS2 Status:</span>
            <StatusBadge status={gate.BS2_STATUS} />
          </div>
        )}
        {(gate.LEVER_STATUS || gate.handle_status) && (
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">Lever Status:</span>
            <StatusBadge status={gate.LEVER_STATUS || gate.handle_status} />
          </div>
        )}
        {gate.LT_STATUS && (
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">LT Status:</span>
            <StatusBadge status={gate.LT_STATUS} />
          </div>
        )}
        {gate.added_on && (
          <div className="flex items-center text-sm text-gray-500">
            <CalendarIcon className="h-4 w-4 mr-2 text-gray-400" />
            {new Date(gate.added_on).toLocaleDateString()}
          </div>
        )}
      </div>

      <div className="flex items-center justify-between pt-4 border-t border-gray-100">
        <div className="flex items-center space-x-2">
          <button
            onClick={() => onView(gate)}
            className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
            title="View Details"
          >
            <EyeIcon className="h-4 w-4" />
          </button>
          <button
            onClick={() => onEdit(gate)}
            className="p-2 text-gray-400 hover:text-yellow-600 hover:bg-yellow-50 rounded-lg transition-colors"
            title="Edit Gate"
          >
            <PencilSquareIcon className="h-4 w-4" />
          </button>
          <button
            onClick={() => onDelete(gate)}
            className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
            title="Delete Gate"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </div>
        <div className="flex items-center space-x-2 text-xs flex-wrap gap-1">
          {(gate.BS1_GO || gate.go) && (
            <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full shadow-sm font-medium">
              BS1 GO: {gate.BS1_GO || gate.go}
            </span>
          )}
          {(gate.BS1_GC || gate.gc) && (
            <span className="bg-red-100 text-red-800 px-2 py-1 rounded-full shadow-sm font-medium">
              BS1 GC: {gate.BS1_GC || gate.gc}
            </span>
          )}
          {gate.BS2_GO && (
            <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded-full shadow-sm font-medium">
              BS2 GO: {gate.BS2_GO}
            </span>
          )}
          {gate.BS2_GC && (
            <span className="bg-purple-100 text-purple-800 px-2 py-1 rounded-full shadow-sm font-medium">
              BS2 GC: {gate.BS2_GC}
            </span>
          )}
          {(gate.LS_GO || gate.ho) && (
            <span className="bg-green-200 text-green-900 px-2 py-1 rounded-full shadow-sm font-medium">
              LS GO: {gate.LS_GO || gate.ho}
            </span>
          )}
          {(gate.LS_GC || gate.hc) && (
            <span className="bg-red-200 text-red-900 px-2 py-1 rounded-full shadow-sm font-medium">
              LS GC: {gate.LS_GC || gate.hc}
            </span>
          )}
        </div>
      </div>
    </div>
  );
};



const Gates = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, 500); // Debounce with 500ms delay
  const [filterStatus, setFilterStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedGate, setSelectedGate] = useState(null);
  const [modalMode, setModalMode] = useState('create');

  const queryClient = useQueryClient();

  // Update query to use debouncedSearchTerm
  const { data: gatesData, isLoading, isFetching } = useQuery({
    queryKey: ['managegates', 
      { page: currentPage, search: debouncedSearchTerm, status: filterStatus }],
    queryFn: () => managegateAPI.getAll({
      page: currentPage,
      limit: 12,
      search: debouncedSearchTerm,
      status: filterStatus
    }),
    keepPreviousData: true, // Keep previous data while fetching new data
  });

  const deleteMutation = useMutation({
    mutationFn: managegateAPI.delete,
    onSuccess: () => {
      toast.success('Gate deleted successfully');
      queryClient.invalidateQueries(['managegates']);
    },
    onError: () => {
      toast.error('Failed to delete gate');
    }
  });

  const handleCreate = () => {
    setSelectedGate(null);
    setModalMode('create');
    setIsModalOpen(true);
  };

  const handleEdit = (gate) => {
    setSelectedGate(gate);
    setModalMode('edit');
    setIsModalOpen(true);
  };

  const handleView = (gate) => {
    setSelectedGate(gate);
    setModalMode('view');
    setIsModalOpen(true);
  };

  const handleDelete = (gate) => {
    if (window.confirm(`Are you sure you want to delete ${gate.gateName}?`)) {
      deleteMutation.mutate(gate.id);
    }
  };

  const gates = gatesData?.data || [];
  const pagination = gatesData?.pagination || {};

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Gate Management</h1>
          <p className="mt-1 text-gray-500">
            Monitor and manage your security gates
          </p>
        </div>
        <button
          onClick={handleCreate}
          className="mt-4 sm:mt-0 inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <PlusIcon className="h-5 w-5 mr-2" />
          Add Gate
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-4 sm:space-y-0 sm:space-x-4">
          <div className="relative flex-1 max-w-md">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search gates..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 pr-10 py-2 w-full border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            {isFetching && searchTerm && (
              <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                <div className="animate-spin h-4 w-4 border-2 border-blue-600 border-t-transparent rounded-full"></div>
              </div>
            )}
          </div>
          <div className="flex items-center space-x-3">
            <FunnelIcon className="h-5 w-5 text-gray-400" />
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">All Status</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
              <option value="open">Open</option>
              <option value="closed">Closed</option>
            </select>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-blue-100">Total Gates</p>
              <p className="text-2xl font-bold">{pagination.totalItems || 0}</p>
            </div>
            <ShieldCheckIcon className="h-8 w-8 text-blue-200" />
          </div>
        </div>
        <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-green-100">Open Gates</p>
              <p className="text-2xl font-bold">
                {gates.filter(g => (g.BS1_STATUS || g.gate_status) === 'open').length}
              </p>
            </div>
            <LockOpenIcon className="h-8 w-8 text-green-200" />
          </div>
        </div>
        <div className="bg-gradient-to-br from-red-500 to-red-600 rounded-xl p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-red-100">Closed Gates</p>
              <p className="text-2xl font-bold">
                {gates.filter(g => (g.BS1_STATUS || g.gate_status) === 'closed').length}
              </p>
            </div>
            <LockClosedIcon className="h-8 w-8 text-red-200" />
          </div>
        </div>
        <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-purple-100">Active Gates</p>
              <p className="text-2xl font-bold">
                {gates.filter(g => g.status === 'Active' || g.status === 'active').length}
              </p>
            </div>
            <ShieldExclamationIcon className="h-8 w-8 text-purple-200" />
          </div>
        </div>
      </div>

      {/* Gates Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {gates.map((gate) => (
          <GateCard
            key={gate.id}
            gate={gate}
            onEdit={handleEdit}
            onDelete={handleDelete}
            onView={handleView}
          />
        ))}
      </div>

      {/* Empty State */}
      {gates.length === 0 && !isFetching && (
        <div className="text-center py-12">
          <ShieldCheckIcon className="h-12 w-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No gates found</h3>
          <p className="text-gray-500 mb-6">
            {searchTerm || filterStatus 
              ? 'Try adjusting your search or filters.' 
              : 'Get started by adding your first gate.'}
          </p>
          {!searchTerm && !filterStatus && (
            <button
              onClick={handleCreate}
              className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <PlusIcon className="h-5 w-5 mr-2" />
              Add Gate
            </button>
          )}
        </div>
      )}

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="flex justify-center">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
              disabled={currentPage === 1}
              className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            
            {Array.from({ length: pagination.totalPages }, (_, i) => i + 1).map((page) => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`px-3 py-2 text-sm font-medium rounded-lg ${
                  currentPage === page
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-500 bg-white border border-gray-300 hover:bg-gray-50'
                }`}
              >
                {page}
              </button>
            ))}
            
            <button
              onClick={() => setCurrentPage(prev => Math.min(prev + 1, pagination.totalPages))}
              disabled={currentPage === pagination.totalPages}
              className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={
          modalMode === 'create' 
            ? 'Add New Gate' 
            : modalMode === 'edit' 
            ? 'Edit Gate' 
            : 'Gate Details'
        }
        size="lg"
      >
        <GateForm
          gate={selectedGate}
          mode={modalMode}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            setIsModalOpen(false);
            queryClient.invalidateQueries(['managegates']);
          }}
        />
      </Modal>
    </div>
  );
};

export default Gates;
