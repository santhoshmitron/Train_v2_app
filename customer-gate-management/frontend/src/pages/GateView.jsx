import React, { useEffect, useState, useCallback, useRef } from "react";
import { managegateAPI, API_BASE_URL } from "../services/api";
import axios from "axios";
import { ArrowPathIcon, MagnifyingGlassIcon } from "@heroicons/react/24/outline";
import {
  DragDropContext,
  Droppable,
  Draggable,
} from "react-beautiful-dnd";
import { useDebounce } from "../hooks/useDebounce";

// Use centralized API base URL
const ES_PROXY_URL = `${API_BASE_URL}/elasticsearch/gate-status`;

// Fixed: Using term query with .keyword for exact match like GateValues
async function fetchGateStatus(gateId, type, mode = "live") {
  const body = {
    size: 10,
    sort: [{ "@timestamp": { order: "desc" } }],
    query: {
      bool: {
        must: [
          // Changed from match to term with .keyword for exact match
          { term: { "parsed_message.gateId.keyword": gateId } },
          {
            range: {
              "@timestamp": {
                gte: mode === "live" ? "now-30s" : "now-1m",
                lte: "now",
              },
            },
          },
        ],
      },
    },
    _source: [
      "@timestamp",
      "parsed_message.gateName",
      "parsed_message.gateId",
      "parsed_message.l_value",
      "parsed_message.g_value",
    ],
  };
  
  try {
    const res = await axios.post(ES_PROXY_URL, body, {
      headers: { "Content-Type": "application/json" },
    });
    
    const hits = res.data?.hits?.hits || [];
    
    // If we found any hits, take the first one (most recent)
    if (hits.length > 0) {
      const latest = hits[0];
      return {
        found: true,
        gateId,
        gateName: latest._source.parsed_message.gateName,
        l_value: latest._source.parsed_message.l_value,
        g_value: latest._source.parsed_message.g_value,
        timestamp: latest._source["@timestamp"],
      };
    }
    
    return { found: false, gateId };
  } catch (e) {
    console.error(`Error fetching gate ${gateId}:`, e.message);
    return { found: false, gateId, error: true };
  }
}

function GateBox({ status, type, index }) {
  const { gateId, gateName, l_value, g_value, found, error } = status;
  const colorClass = error
    ? "bg-gray-500 text-gray-100 border-gray-600"
    : found
    ? "bg-green-300 border-green-600"
    : "bg-red-300 border-red-600";
  const borderClass = found
    ? "border-2 border-green-600"
    : "border-2 border-red-600";
  const ribbonColor =
    type === "BS" ? "bg-blue-400 text-blue-900" : "bg-red-400 text-red-900";

  // Highlight rules for L/G
  const lValueElement =
    type === "LS" ? (
      <span className="ml-1 font-semibold text-green-700 bg-green-100 px-2 py-1 rounded">
        {l_value || "-"}
      </span>
    ) : (
      <span className="ml-1 font-semibold">{l_value || "-"}</span>
    );
  const gValueElement =
    type === "BS" ? (
      <span className="ml-1 font-semibold text-blue-700 bg-blue-100 px-2 py-1 rounded">
        {g_value || "-"}
      </span>
    ) : (
      <span className="ml-1 font-semibold">{g_value || "-"}</span>
    );

  return (
    <Draggable draggableId={gateId} index={index}>
      {(provided, snapshot) => (
        <div
          ref={provided.innerRef}
          {...provided.draggableProps}
          {...provided.dragHandleProps}
          className={`relative flex flex-col items-center justify-center rounded-xl shadow-md ${colorClass} ${borderClass} p-4 transition duration-300 min-w-[120px] min-h-[100px] cursor-move ${
            snapshot.isDragging ? "z-50" : ""
          }`}
          style={provided.draggableProps.style}
        >
          <span
            className={`absolute left-2 top-2 px-2 py-0.5 rounded-full text-xs font-bold ${ribbonColor}`}
            style={{ letterSpacing: 1 }}
          >
            {type}
          </span>
          <div className="flex flex-col items-center mb-2 mt-3 text-center">
            <span className="font-semibold text-base">{gateId}</span>
            {gateName && (
              <span className="text-xs text-gray-800 mt-1">{gateName}</span>
            )}
            {found ? (
              <span className="w-3 h-3 mt-1 rounded-full bg-green-400 inline-block" />
            ) : (
              <span className="w-3 h-3 mt-1 rounded-full bg-red-400 inline-block" />
            )}
          </div>
          <div className="w-full flex justify-between mt-auto">
            <div>
              <span className="text-xs text-black">L:</span>
              {lValueElement}
            </div>
            <div>
              <span className="text-xs text-black">G:</span>
              {gValueElement}
            </div>
          </div>
        </div>
      )}
    </Draggable>
  );
}

export default function GateView() {
  const [bsIds, setBsIds] = useState([]);
  const [lsIds, setLsIds] = useState([]);
  const [boxes, setBoxes] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const debouncedSearchTerm = useDebounce(searchTerm, 300);
  const [loading, setLoading] = useState(false);
  const [mode, setMode] = useState("live");
  const [lastUpdate, setLastUpdate] = useState(null);

  // Use refs to avoid stale closure issues
  const bsIdsRef = useRef(bsIds);
  const lsIdsRef = useRef(lsIds);
  const modeRef = useRef(mode);

  // Update refs when state changes
  useEffect(() => {
    bsIdsRef.current = bsIds;
  }, [bsIds]);

  useEffect(() => {
    lsIdsRef.current = lsIds;
  }, [lsIds]);

  useEffect(() => {
    modeRef.current = mode;
  }, [mode]);

  const fetchGateLists = useCallback(async () => {
    try {
      const data = await managegateAPI.getBSLSIds();
      setBsIds(data.bsIds || []);
      setLsIds(data.lsIds || []);
    } catch (error) {
      console.error("Error fetching gate lists:", error);
      setBsIds([]);
      setLsIds([]);
    }
  }, []);

  const fetchAllStatuses = useCallback(async () => {
    setLoading(true);
    const currentBsIds = bsIdsRef.current;
    const currentLsIds = lsIdsRef.current;
    const currentMode = modeRef.current;

    const allIds = [
      ...currentBsIds.map((id) => ({ id, type: "BS" })),
      ...currentLsIds.map((id) => ({ id, type: "LS" })),
    ];

    if (allIds.length === 0) {
      setLoading(false);
      return;
    }

    try {
      const results = await Promise.all(
        allIds.map(({ id, type }) =>
          fetchGateStatus(id, type, currentMode).then((status) => ({
            ...status,
            type,
          }))
        )
      );
      
      // Use functional update to avoid stale state
      setBoxes(prevBoxes => {
        // If no previous boxes, use new results directly
        if (prevBoxes.length === 0) {
          return results;
        }
        
        // Create a map of new results for quick lookup
        const resultsMap = new Map(results.map(r => [r.gateId, r]));
        
        // Update existing boxes with new data while preserving order
        return prevBoxes.map(prevBox => {
          const newData = resultsMap.get(prevBox.gateId);
          return newData || prevBox;
        });
      });
      
      setLastUpdate(new Date());
    } catch (error) {
      console.error("Error fetching all statuses:", error);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial fetch of gate lists
  useEffect(() => {
    fetchGateLists();
  }, [fetchGateLists]);

  // Set up continuous polling with proper cleanup
  useEffect(() => {
    if (bsIds.length === 0 && lsIds.length === 0) {
      return;
    }

    // Initial fetch
    fetchAllStatuses();

    // Set up interval for continuous polling
    const intervalId = setInterval(() => {
      fetchAllStatuses();
    }, 10000); // Poll every 10 seconds

    // Cleanup on unmount or when dependencies change
    return () => {
      clearInterval(intervalId);
    };
  }, [bsIds, lsIds, mode, fetchAllStatuses]);

  const onDragEnd = (result) => {
    if (!result.destination) return;
    
    setBoxes(prevBoxes => {
      const newBoxes = Array.from(prevBoxes);
      const [removed] = newBoxes.splice(result.source.index, 1);
      newBoxes.splice(result.destination.index, 0, removed);
      return newBoxes;
    });
  };

  // Filter by debounced search term
  const filteredBoxes = boxes.filter(
    (b) =>
      b.gateId?.toLowerCase().includes(debouncedSearchTerm.toLowerCase()) ||
      b.gateName?.toLowerCase().includes(debouncedSearchTerm.toLowerCase())
  );

  // Count stats
  const totalGates = boxes.length;
  const foundGates = boxes.filter(b => b.found).length;
  const notFoundGates = boxes.filter(b => !b.found && !b.error).length;
  const errorGates = boxes.filter(b => b.error).length;

  return (
    <div className="p-4 max-w-7xl mx-auto w-full select-none">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">
            GateView Status Dashboard
          </h2>
          <p className="text-gray-600 mt-1">
            <span className="text-green-600 font-medium">Green</span>: Data found
            for <b>this type only</b>{" "}
            {mode === "live"
              ? "within the last 30 seconds."
              : "within the last 1 minute."}
          </p>
          {lastUpdate && (
            <p className="text-sm text-gray-500 mt-1">
              Last updated: {lastUpdate.toLocaleTimeString()}
            </p>
          )}
        </div>

        <div className="flex items-center space-x-2 mt-4 sm:mt-0">
          <button
            onClick={() => setMode("live")}
            className={`px-3 py-1 rounded ${
              mode === "live"
                ? "bg-blue-500 text-white"
                : "bg-gray-200 text-gray-700"
            } transition`}
          >
            Live (last 30s)
          </button>
          <button
            onClick={() => setMode("latest")}
            className={`px-3 py-1 rounded ${
              mode === "latest"
                ? "bg-blue-500 text-white"
                : "bg-gray-200 text-gray-700"
            } transition`}
          >
            Latest 1 min
          </button>
          <button
            className="flex items-center px-4 py-2 bg-blue-500 text-white rounded-lg shadow hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={fetchAllStatuses}
            disabled={loading}
          >
            <ArrowPathIcon
              className={`h-5 w-5 mr-2 ${loading && "animate-spin"}`}
            />
            Refresh Now
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-4 text-white">
          <p className="text-blue-100 text-sm">Total Gates</p>
          <p className="text-2xl font-bold">{totalGates}</p>
        </div>
        <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-4 text-white">
          <p className="text-green-100 text-sm">Found</p>
          <p className="text-2xl font-bold">{foundGates}</p>
        </div>
        <div className="bg-gradient-to-br from-red-500 to-red-600 rounded-xl p-4 text-white">
          <p className="text-red-100 text-sm">Not Found</p>
          <p className="text-2xl font-bold">{notFoundGates}</p>
        </div>
        <div className="bg-gradient-to-br from-gray-500 to-gray-600 rounded-xl p-4 text-white">
          <p className="text-gray-100 text-sm">Errors</p>
          <p className="text-2xl font-bold">{errorGates}</p>
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative mb-6 max-w-md">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
        <input
          type="text"
          placeholder="Search gate by ID or name..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="pl-10 pr-10 py-2 w-full border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
        {searchTerm && (
          <button
            onClick={() => setSearchTerm("")}
            className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        )}
      </div>

      {/* Results Info */}
      {debouncedSearchTerm && (
        <div className="mb-4 text-sm text-gray-600">
          Showing {filteredBoxes.length} of {totalGates} gates
        </div>
      )}

      {/* Grid */}
      <DragDropContext onDragEnd={onDragEnd}>
        <Droppable droppableId="boxes" direction="horizontal">
          {(provided) => (
            <div
              className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4"
              {...provided.droppableProps}
              ref={provided.innerRef}
            >
              {filteredBoxes.length > 0 ? (
                filteredBoxes.map((box, index) => (
                  <GateBox
                    key={box.gateId || index}
                    status={box}
                    type={box.type}
                    index={index}
                  />
                ))
              ) : (
                <div className="col-span-full text-center py-12">
                  <MagnifyingGlassIcon className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">
                    No gates found
                  </h3>
                  <p className="text-gray-500">
                    {searchTerm
                      ? `No gates match "${debouncedSearchTerm}"`
                      : "No gates available"}
                  </p>
                  {searchTerm && (
                    <button
                      onClick={() => setSearchTerm("")}
                      className="mt-4 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                    >
                      Clear Search
                    </button>
                  )}
                </div>
              )}
              {provided.placeholder}
            </div>
          )}
        </Droppable>
      </DragDropContext>
    </div>
  );
}
