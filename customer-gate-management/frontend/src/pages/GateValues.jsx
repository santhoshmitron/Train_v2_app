import React, { useState } from "react";
import axios from "axios";
import { API_BASE_URL } from "../services/api";

// Use centralized API base URL
const ES_PROXY_URL = `${API_BASE_URL}/elasticsearch/gate-status`;

export default function GateValues() {
  const [gateId, setGateId] = useState("");
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchLastValues = async () => {
    if (!gateId.trim()) {
      setError("Please enter a valid Gate ID like TPJ-136BS");
      return;
    }

    setLoading(true);
    setError(null);
    setRecords([]);

    try {
      const body = {
        size: 10, // last 10 values
        sort: [{ "@timestamp": { order: "desc" } }],
        query: {
          bool: {
            must: [
              { term: { "parsed_message.gateId.keyword": gateId.trim() } }
            ]
          }
        },
        _source: [
          "@timestamp",
          "parsed_message.gateId",
          "parsed_message.l_value",
          "parsed_message.g_value"
        ]
      };

      const res = await axios.post(ES_PROXY_URL, body, {
        headers: { "Content-Type": "application/json" }
      });

      const hits = res.data?.hits?.hits || [];

      if (hits.length === 0) {
        setError("No records found for this Gate ID");
      } else {
        const rows = hits.map(hit => ({
          time: new Date(hit._source["@timestamp"]).toLocaleString(),
          gateId: hit._source.parsed_message.gateId,
          l_value: hit._source.parsed_message.l_value,
          g_value: hit._source.parsed_message.g_value,
        }));

        setRecords(rows);
      }
    } catch (err) {
      setError("Failed to fetch data");
    }

    setLoading(false);
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h2 className="text-2xl font-semibold mb-4">Gate Value Search</h2>

      <div className="flex mb-6 space-x-3">
        <input
          type="text"
          placeholder="Enter Gate ID, e.g. TPJ-136BS"
          value={gateId}
          onChange={(e) => setGateId(e.target.value.toUpperCase())}
          className="flex-grow border border-gray-300 rounded-md px-4 py-2 text-lg"
        />
        <button
          onClick={fetchLastValues}
          disabled={loading}
          className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-blue-400"
        >
          {loading ? "Loading..." : "Search"}
        </button>
      </div>

      {error && <p className="text-red-600 mb-4">{error}</p>}

      {records.length > 0 && (
        <table className="min-w-full border-collapse border border-gray-200">
          <thead>
            <tr>
              <th className="border border-gray-300 px-4 py-2 bg-gray-100">Gate ID</th>
              <th className="border border-gray-300 px-4 py-2 bg-gray-100">Time</th>
              <th className="border border-gray-300 px-4 py-2 bg-gray-100">L Value</th>
              <th className="border border-gray-300 px-4 py-2 bg-gray-100">G Value</th>
            </tr>
          </thead>
          <tbody>
            {records.map((row, i) => (
              <tr key={i} className={i % 2 === 0 ? "bg-white" : "bg-gray-50"}>
                <td className="border border-gray-300 px-4 py-2">{row.gateId}</td>
                <td className="border border-gray-300 px-4 py-2">{row.time}</td>
                <td className="border border-gray-300 px-4 py-2">{row.l_value}</td>
                <td className="border border-gray-300 px-4 py-2">{row.g_value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
