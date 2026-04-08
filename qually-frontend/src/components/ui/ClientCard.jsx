import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { getProtocols } from "../../api/protocols";
import { CLIENT_ACCENT_COLORS } from "../../constants";
import { getInitials } from "../../utils/formatters";

export function ClientCard({ client, colorIdx, onFlip, flipped }) {
  const navigate = useNavigate();
  const color = CLIENT_ACCENT_COLORS[colorIdx % CLIENT_ACCENT_COLORS.length];

  const [protocols, setProtocols] = useState([]);
  const [loadingProtocols, setLoadingProtocols] = useState(false);
  const [error, setError] = useState(null);
  const hasFetched = useRef(false); // guard: fetch only once per mount

  useEffect(() => {
    if (flipped && !hasFetched.current) {
      hasFetched.current = true;
      setLoadingProtocols(true);
      getProtocols(client.clientId)
        .then((data) => setProtocols(data))
        .catch((err) => setError(err.message))
        .finally(() => setLoadingProtocols(false));
    }
  }, [flipped, client.clientId]);

  return (
    <div className="h-[200px] cursor-pointer [perspective:1000px]">
      <div
        className={`relative w-full h-full duration-500 [transform-style:preserve-3d] ${
          flipped ? "[transform:rotateY(180deg)]" : ""
        }`}
      >
        {/* FRONT */}
        <div
          onClick={() => onFlip(client.clientId)}
          className="absolute inset-0 [backface-visibility:hidden] bg-white border border-gray-200 rounded-xl p-5 flex flex-col justify-between shadow-sm hover:shadow-md hover:border-gray-300 transition-all duration-200"
        >
          <div>
            <div
              style={{ background: color.bg, color: color.text }}
              className="w-11 h-11 rounded-lg flex items-center justify-center text-[13px] font-semibold tracking-wider font-mono"
            >
              {getInitials(client.clientName)}
            </div>
          </div>

          <div>
            <p className="text-xl font-bold text-gray-900 tracking-tight leading-tight mb-1">
              {client.clientName}
            </p>
            <p className="text-sm text-gray-500">Click to view protocols →</p>
          </div>

          <div className="flex items-center gap-1.5">
            <span
              style={{ background: color.dot }}
              className="w-1.5 h-1.5 rounded-full"
            />
            <span className="text-xs text-gray-400 font-mono">
              Client #{client.clientId}
            </span>
          </div>
        </div>

        {/* BACK */}
        <div className="absolute inset-0 [backface-visibility:hidden] [transform:rotateY(180deg)] bg-white border border-gray-200 rounded-xl p-4 flex flex-col gap-1.5 overflow-hidden shadow-sm">
          <div className="flex items-center justify-between mb-0.5">
            <span style={{ color: color.text }} className="text-sm font-semibold">
              {client.clientName}
            </span>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onFlip(client.clientId);
              }}
              className="text-gray-400 hover:bg-gray-100 w-5 h-5 rounded flex items-center justify-center transition-colors"
            >
              ✕
            </button>
          </div>

          <div className="flex flex-col gap-1 overflow-y-auto flex-1 pr-1">
            {loadingProtocols && <p className="text-sm text-gray-400">Loading...</p>}
            {error && <p className="text-sm text-red-500">{error}</p>}
            {!loadingProtocols && !error && protocols.length === 0 && (
              <p className="text-sm text-gray-400">No protocols found.</p>
            )}
            {protocols.map((p) => (
              <div
                key={p.protocolId}
                onClick={(e) => {
                  e.stopPropagation();
                  navigate(`/protocols/${p.protocolId}`);
                }}
                className="flex items-center justify-between p-2 rounded-md bg-gray-50 hover:bg-blue-50 transition-colors cursor-pointer group"
              >
                <span className="text-sm font-medium text-gray-700 truncate mr-2 group-hover:text-blue-700">
                  {p.protocolName}
                </span>
                <div className="flex gap-1.5 items-center shrink-0">
                  <span className="text-[10px] font-mono text-gray-400">
                    v{p.protocolVersion}
                  </span>
                  <span
                    className={`text-[9px] px-1.5 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                      p.isFinalized
                        ? "bg-green-100 text-green-700"
                        : "bg-amber-100 text-amber-700"
                    }`}
                  >
                    {p.isFinalized ? "Final" : "Draft"}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}