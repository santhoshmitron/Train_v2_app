import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  UserPlusIcon, 
  MagnifyingGlassIcon,
  FunnelIcon,
  PencilSquareIcon,
  TrashIcon,
  EyeIcon,
  PhoneIcon,
  MapPinIcon,
  UserIcon,
  ArrowLeftIcon,
  ChevronRightIcon,
  UsersIcon,
  ShieldCheckIcon,
  IdentificationIcon
} from '@heroicons/react/24/outline';
import { customerAPI } from '../services/api';
import toast from 'react-hot-toast';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import Modal from '../components/UI/Modal';
import CustomerForm from '../components/Customers/CustomerForm';
import { useDebounce } from '../hooks/useDebounce';


const StatusBadge = ({ status }) => {
  const statusConfig = {
    Active: { color: 'bg-green-100 text-green-800', label: 'Active' },
    Inactive: { color: 'bg-red-100 text-red-800', label: 'Inactive' },
  };
  
  const config = statusConfig[status] || statusConfig.Inactive;
  
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.color}`}>
      {config.label}
    </span>
  );
};


const RoleBadge = ({ role }) => {
  const roleConfig = {
    SM: { color: 'bg-blue-100 text-blue-800', label: 'SM', fullName: 'Station Master' },
    GM: { color: 'bg-purple-100 text-purple-800', label: 'GM', fullName: 'Gate Man' },
  };
  
  const config = roleConfig[role] || { color: 'bg-gray-100 text-gray-800', label: role };
  
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.color}`} title={config.fullName}>
      {config.label}
    </span>
  );
};


const SMCard = ({ customer, onEdit, onDelete, onView, onViewGMs }) => (
  <div className="bg-white rounded-xl shadow border border-gray-200 p-6 hover:shadow-lg transition-all duration-200 flex flex-col justify-between">
    <div>
      <div className="flex items-center space-x-3 mb-4">
        <div className="h-12 w-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full flex items-center justify-center">
          <UserIcon className="h-6 w-6 text-white" />
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="text-lg font-semibold text-gray-900 truncate">{customer.name}</h3>
          <p className="text-sm text-gray-500 truncate">@{customer.username || 'N/A'}</p>
          
          <div className="flex items-center space-x-2 mt-1">
            <RoleBadge role={customer.roles} />
            <StatusBadge status={customer.status} />
          </div>
        </div>
      </div>

      <div className="relative p-4 border rounded-lg bg-white shadow-sm hover:shadow-md transition-shadow duration-200">
        {customer.gate_count !== undefined && (
          <div className="absolute top-2 right-3 flex items-center text-xs text-blue-700 font-semibold bg-blue-50 px-2 py-1 rounded-full shadow-sm">
            <ShieldCheckIcon className="h-4 w-4 mr-1 text-blue-600" />
            Gates: {customer.gate_count}
          </div>
        )}

        <div className="space-y-3 mt-2">
          <div className="flex items-center text-sm text-gray-700">
            <UsersIcon className="h-5 w-5 mr-2 text-gray-500" />
            <span className="font-medium w-28">SM Name:</span>
            <span className="text-gray-800 truncate">{customer.first_name || 'N/A'}</span>
          </div>

          {customer.mobile && (
            <div className="flex items-center text-sm text-gray-700">
              <PhoneIcon className="h-5 w-5 mr-2 text-gray-500" />
              <span className="font-medium w-28">Phone:</span>
              <span className="text-gray-800">{customer.mobile}</span>
            </div>
          )}

          {customer.managed_gates && (
            <div className="flex items-center text-sm text-gray-700">
              <ShieldCheckIcon className="h-5 w-5 mr-2 text-gray-500" />
              <span className="font-medium w-28">Gate No:</span>
              <span className="text-gray-800 bg-blue-200 px-2 py-1 rounded-full shadow-sm">{customer.managed_gates}</span>
            </div>
          )}
        </div>
      </div>
    </div>

    <div className="flex items-center justify-between pt-4 border-t border-gray-100 relative">
      <div className="flex items-center space-x-2">
        <button
          onClick={() => onView(customer)}
          className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
          title="View Details"
        >
          <EyeIcon className="h-4 w-4" />
        </button>

        <button
          onClick={() => onEdit(customer)}
          className="p-2 text-gray-400 hover:text-yellow-600 hover:bg-yellow-50 rounded-lg transition-colors"
          title="Edit Station Master"
        >
          <PencilSquareIcon className="h-4 w-4" />
        </button>

        <button
          onClick={() => onDelete(customer)}
          className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          title="Delete Station Master"
        >
          <TrashIcon className="h-4 w-4" />
        </button>
      </div>

      <button
        onClick={() => onViewGMs(customer)}
        className="absolute bottom-2 right-3 flex items-center bg-gradient-to-r from-blue-500 to-indigo-500 text-white text-sm font-semibold px-3 py-1.5 rounded-full shadow-md hover:shadow-lg hover:from-blue-600 hover:to-indigo-600 transition-all duration-200"
        title="View Gates"
      >
        <UsersIcon className="h-4 w-4 mr-1" />
        View Gates
      </button>
    </div>
  </div>
);


const GMCard = ({ gmData, onEdit, onDelete, onView }) => (
  <div className="relative bg-white rounded-xl shadow border border-gray-200 p-6 hover:shadow-lg transition-all duration-200 flex flex-col justify-between">
    <div>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-3">
          <div className="h-12 w-12 bg-gradient-to-br from-purple-500 to-purple-600 rounded-full flex items-center justify-center shadow-md">
            <UserIcon className="h-6 w-6 text-white" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-semibold text-gray-900 truncate">
              {gmData.name}
            </h3>
            <p className="text-sm text-gray-500 truncate">
              @{gmData.username || "N/A"}
            </p>
            <div className="flex items-center space-x-2 mt-1">
              <RoleBadge role={gmData.roles} />
              <StatusBadge status={gmData.status} />
            </div>
          </div>
        </div>

        <div className="flex flex-col items-end space-y-2">
          {(() => {
            const gateStatus = gmData.BS1_STATUS || gmData.gate_status || "open";
            const leverStatus = gmData.LEVER_STATUS || gmData.handle_status || "open";
            const gateOpen = gateStatus === "open";
            const handleOpen = leverStatus === "open";
            const combinedStatus = gateOpen || handleOpen ? "open" : "closed";
            const badgeColor = combinedStatus === "open" ? "bg-blue-50 text-blue-700" : "bg-gray-100 text-gray-700";
            
            return (
              <span
                className={`text-xs font-semibold px-2 py-1 rounded-full shadow-sm ${badgeColor}`}
              >
                {combinedStatus}
              </span>
            );
          })()}
        </div>
      </div>

      <div className="bg-gray-50 rounded-lg p-4 space-y-2">
        <div className="flex items-center text-sm text-gray-700">
          <UserIcon className="h-4 w-4 mr-2 text-gray-400" />
          <span>
            <span className="font-medium">GM Name:</span>{" "}
            {gmData.first_name || "N/A"}
          </span>
        </div>

        {gmData.mobile && (
          <div className="flex items-center text-sm text-gray-700">
            <PhoneIcon className="h-4 w-4 mr-2 text-gray-400" />
            <span>
              <span className="font-medium">Phone:</span> {gmData.mobile}
            </span>
          </div>
        )}

        {(gmData.BOOM1_ID || gmData.gateId) && (
          <div className="flex items-center text-sm text-gray-700">
            <IdentificationIcon className="h-4 w-4 mr-2 text-gray-400" />
            <span>
              <span className="font-medium"> Boom 1 ID:</span>  {gmData.BOOM1_ID || gmData.gateId}
            </span>
          </div>
        )}
        {gmData.BOOM2_ID && (
          <div className="flex items-center text-sm text-gray-700">
            <IdentificationIcon className="h-4 w-4 mr-2 text-gray-400" />
            <span>
              <span className="font-medium"> Boom 2 ID:</span>  {gmData.BOOM2_ID}
            </span>
          </div>
        )}

        {gmData.handle && (
          <div className="flex items-center text-sm text-gray-700">
            <ShieldCheckIcon className="h-4 w-4 mr-2 text-gray-400" />
            <span>
              <span className="font-medium">Handle:</span> {gmData.handle}
            </span>
          </div>
        )}

        {(gmData.LEVER_STATUS || gmData.handle_status) && (
          <div className="flex items-center text-sm">
            <span className="font-medium text-gray-700 mr-1">
              Lever Status:
            </span>
            <span
              className={`font-semibold ${
                (gmData.LEVER_STATUS || gmData.handle_status) === "open"
                  ? "text-blue-600"
                  : "text-gray-600"
              }`}
            >
              {gmData.LEVER_STATUS || gmData.handle_status}
            </span>
          </div>
        )}

        {(gmData.BS1_STATUS || gmData.gate_status) && (
          <div className="flex items-center text-sm">
            <span className="font-medium text-gray-700 mr-1">
              BS1 Status:
            </span>
            <span
              className={`font-semibold ${
                (gmData.BS1_STATUS || gmData.gate_status) === "open"
                  ? "text-blue-600"
                  : "text-gray-600"
              }`}
            >
              {gmData.BS1_STATUS || gmData.gate_status}
            </span>
          </div>
        )}
        {gmData.BS2_STATUS && (
          <div className="flex items-center text-sm">
            <span className="font-medium text-gray-700 mr-1">
              BS2 Status:
            </span>
            <span
              className={`font-semibold ${
                gmData.BS2_STATUS === "open"
                  ? "text-blue-600"
                  : "text-gray-600"
              }`}
            >
              {gmData.BS2_STATUS}
            </span>
          </div>
        )}
      </div>
    </div>

    <div className="flex items-center justify-between pt-4 border-t border-gray-100 mt-4">
      <div className="flex items-center space-x-2">
        <button
          onClick={() => onView(gmData)}
          className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
          title="View Details"
        >
          <EyeIcon className="h-4 w-4" />
        </button>

        <button
          onClick={() => onEdit(gmData)}
          className="p-2 text-gray-400 hover:text-yellow-600 hover:bg-yellow-50 rounded-lg transition-colors"
          title="Edit Gate Man"
        >
          <PencilSquareIcon className="h-4 w-4" />
        </button>

        <button
          onClick={() => onDelete(gmData)}
          className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          title="Delete Gate Man"
        >
          <TrashIcon className="h-4 w-4" />
        </button>
      </div>

      <div className="flex items-center space-x-2 text-xs flex-wrap gap-1">
        {(gmData.BS1_GO || gmData.go) && (
          <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full shadow-sm font-medium">
            BS1 GO: {gmData.BS1_GO || gmData.go}
          </span>
        )}
        {(gmData.BS1_GC || gmData.gc) && (
          <span className="bg-red-100 text-red-800 px-2 py-1 rounded-full shadow-sm font-medium">
            BS1 GC: {gmData.BS1_GC || gmData.gc}
          </span>
        )}
        {gmData.BS2_GO && (
          <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded-full shadow-sm font-medium">
            BS2 GO: {gmData.BS2_GO}
          </span>
        )}
        {gmData.BS2_GC && (
          <span className="bg-purple-100 text-purple-800 px-2 py-1 rounded-full shadow-sm font-medium">
            BS2 GC: {gmData.BS2_GC}
          </span>
        )}
        {(gmData.LS_GO || gmData.ho) && (
          <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full shadow-sm font-medium">
            LS GO: {gmData.LS_GO || gmData.ho}
          </span>
        )}
        {(gmData.LS_GC || gmData.hc) && (
          <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full shadow-sm font-medium">
            LS GC: {gmData.LS_GC || gmData.hc}
          </span>
        )}
      </div>
    </div>
  </div>
);


const Customers = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, 500);
  const [filterStatus, setFilterStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [modalMode, setModalMode] = useState('create');

  const [viewMode, setViewMode] = useState('SM');
  const [selectedSM, setSelectedSM] = useState(null);

  const queryClient = useQueryClient();

  // Reset to page 1 when search term or filter changes
  useEffect(() => {
    setCurrentPage(1);
  }, [debouncedSearchTerm, filterStatus]);

  // Fetch SM customers
  const { data: smCustomersData, isLoading: smLoading, isFetching: smFetching } = useQuery({
    queryKey: ['sm-customers-with-gates', { page: currentPage, search: debouncedSearchTerm, status: filterStatus }],
    queryFn: () => customerAPI.getSMWithGates({
      page: currentPage,
      limit: 12,
      search: debouncedSearchTerm,
      status: filterStatus
    }),
    enabled: viewMode === 'SM',
    keepPreviousData: true,
  });

  // Fetch GM customers under selected SM
  const { data: gmCustomersData, isLoading: gmLoading, isFetching: gmFetching } = useQuery({
    queryKey: ['gm-customers-under-sm', selectedSM?.username, { page: currentPage, search: debouncedSearchTerm, status: filterStatus }],
    queryFn: () => customerAPI.getGMUnderSM(selectedSM.username, {
      page: currentPage,
      limit: 12,
      search: debouncedSearchTerm,
      status: filterStatus
    }),
    enabled: viewMode === 'GM' && !!selectedSM,
    keepPreviousData: true,
  });

  const deleteMutation = useMutation({
    mutationFn: customerAPI.delete,
    onSuccess: () => {
      toast.success('Customer deleted successfully');
      queryClient.invalidateQueries(['sm-customers-with-gates']);
      queryClient.invalidateQueries(['gm-customers-under-sm']);
    },
    onError: () => {
      toast.error('Failed to delete customer');
    }
  });

  const handleCreate = () => {
    setSelectedCustomer(null);
    setModalMode('create');
    setIsModalOpen(true);
  };

  const handleEdit = (customer) => {
    setSelectedCustomer(customer);
    setModalMode('edit');
    setIsModalOpen(true);
  };

  const handleView = (customer) => {
    setSelectedCustomer(customer);
    setModalMode('view');
    setIsModalOpen(true);
  };

  const handleDelete = (customer) => {
    if (window.confirm(`Are you sure you want to delete ${customer.name}?`)) {
      deleteMutation.mutate(customer.id);
    }
  };

  const handleViewGMs = (smCustomer) => {
    setSelectedSM(smCustomer);
    setViewMode('GM');
    setCurrentPage(1);
    setSearchTerm('');
    setFilterStatus('');
  };

  const handleBackToSMs = () => {
    setViewMode('SM');
    setSelectedSM(null);
    setCurrentPage(1);
    setSearchTerm('');
    setFilterStatus('');
  };

  const currentData = viewMode === 'SM' ? smCustomersData : gmCustomersData;
  const isLoading = viewMode === 'SM' ? smLoading : gmLoading;
  const isFetching = viewMode === 'SM' ? smFetching : gmFetching;
  const customers = currentData?.data || [];
  const pagination = currentData?.pagination || {};

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          {viewMode === 'GM' && (
            <button
              onClick={handleBackToSMs}
              className="text-sm text-blue-600 hover:text-blue-800 flex items-center mb-2"
            >
              <ArrowLeftIcon className="h-4 w-4 mr-1" /> Back to Station Masters
            </button>
          )}
          <h1 className="text-2xl font-bold text-gray-900">
            {viewMode === 'SM' ? '🚉 Station Masters' : `👷 Gate Team under ${selectedSM?.name}`}
          </h1>
          <p className="mt-1 text-gray-500">
            {viewMode === 'SM' 
              ? 'Manage railway station masters and their assigned gates' 
              : `Viewing gate men managed by ${selectedSM?.username}`
            }
          </p>
        </div>
        <button
          onClick={handleCreate}
          className="mt-4 sm:mt-0 inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <UserPlusIcon className="h-5 w-5 mr-2" />
          Add {viewMode === 'SM' ? 'Station Master' : 'Gate Man'}
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-4 sm:space-y-0 sm:space-x-4">
          <div className="relative flex-1 max-w-md">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder={`Search ${viewMode === 'SM' ? 'station masters' : 'gate men'}...`}
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
              <option value="Active">Active</option>
              <option value="Inactive">Inactive</option>
            </select>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className={`rounded-xl p-6 text-white ${
          viewMode === 'SM' ? 'bg-gradient-to-br from-blue-500 to-blue-600' : 'bg-gradient-to-br from-purple-500 to-purple-600'
        }`}>
          <div className="flex items-center justify-between">
            <div>
              <p className={viewMode === 'SM' ? 'text-blue-100' : 'text-purple-100'}>
                Total {viewMode === 'SM' ? 'Station Masters' : 'Gate Men'}
              </p>
              <p className="text-2xl font-bold">{pagination.totalItems || 0}</p>
            </div>
            <UserIcon className={`h-8 w-8 ${viewMode === 'SM' ? 'text-blue-200' : 'text-purple-200'}`} />
          </div>
        </div>
        <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-green-100">Active</p>
              <p className="text-2xl font-bold">
                {customers.filter(c => c.status === 'Active').length}
              </p>
            </div>
            <UserIcon className="h-8 w-8 text-green-200" />
          </div>
        </div>
        <div className="bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-orange-100">Current Page</p>
              <p className="text-2xl font-bold">{customers.length}</p>
            </div>
            <UserIcon className="h-8 w-8 text-orange-200" />
          </div>
        </div>
      </div>

      {/* Customer Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {customers.map((customer) => (
          viewMode === 'SM' ? (
            <SMCard
              key={customer.id}
              customer={customer}
              onEdit={handleEdit}
              onDelete={handleDelete}
              onView={handleView}
              onViewGMs={handleViewGMs}
            />
          ) : (
            <GMCard
              key={customer.id}
              gmData={customer}
              onEdit={handleEdit}
              onDelete={handleDelete}
              onView={handleView}
            />
          )
        ))}
      </div>

      {/* Empty State */}
      {customers.length === 0 && !isFetching && (
        <div className="text-center py-12">
          <UserIcon className="h-12 w-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">
            No {viewMode === 'SM' ? 'station masters' : 'gate men'} found
          </h3>
          <p className="text-gray-500 mb-6">
            {searchTerm || filterStatus
              ? 'Try adjusting your search or filters.'
              : viewMode === 'SM' 
                ? 'Get started by adding your first station master.' 
                : `No gate men found under ${selectedSM?.name}.`
            }
          </p>
          {!searchTerm && !filterStatus && (
            <button
              onClick={handleCreate}
              className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <UserPlusIcon className="h-5 w-5 mr-2" />
              Add {viewMode === 'SM' ? 'Station Master' : 'Gate Man'}
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
            
            {Array.from({ length: Math.min(pagination.totalPages, 5) }, (_, i) => {
              let pageNumber;
              if (pagination.totalPages <= 5) {
                pageNumber = i + 1;
              } else if (currentPage <= 3) {
                pageNumber = i + 1;
              } else if (currentPage >= pagination.totalPages - 2) {
                pageNumber = pagination.totalPages - 4 + i;
              } else {
                pageNumber = currentPage - 2 + i;
              }
              
              return (
                <button
                  key={pageNumber}
                  onClick={() => setCurrentPage(pageNumber)}
                  className={`px-3 py-2 text-sm font-medium rounded-lg ${
                    currentPage === pageNumber
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-500 bg-white border border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {pageNumber}
                </button>
              );
            })}
            
            {pagination.totalPages > 5 && currentPage < pagination.totalPages - 2 && (
              <>
                <span className="px-2 text-gray-500">...</span>
                <button
                  onClick={() => setCurrentPage(pagination.totalPages)}
                  className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
                >
                  {pagination.totalPages}
                </button>
              </>
            )}
            
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

      {/* Pagination Info */}
      {pagination.totalPages > 0 && (
        <div className="text-center text-sm text-gray-600">
          Showing page {currentPage} of {pagination.totalPages} ({pagination.totalItems} total items)
        </div>
      )}

      {/* Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={
          modalMode === 'create' 
            ? `Add New ${viewMode === 'SM' ? 'Station Details' : 'Gate Details'}`
            : modalMode === 'edit' 
            ? `Edit ${viewMode === 'SM' ? 'Station Details' : 'Gate Details'}` 
            : 'Details'
        }
        size="lg"
      >
        <CustomerForm
          customer={selectedCustomer}
          mode={modalMode}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            setIsModalOpen(false);
            queryClient.invalidateQueries(['sm-customers-with-gates']);
            queryClient.invalidateQueries(['gm-customers-under-sm']);
          }}
          defaultRole={viewMode}
        />
      </Modal>
    </div>
  );
};

export default Customers;
