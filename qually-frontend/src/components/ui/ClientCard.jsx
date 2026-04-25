/**
 * @module components/ui/ClientCard
 *
 * Flip card for the Dashboard grid. Front face shows the client name and accent
 * color; flipping reveals a lazy-loaded list of that client's protocols.
 *
 * Protocol row changes:
 * - Version number removed.
 * - Status badge shown only for DRAFT and ARCHIVED — FINALIZED protocols show
 *   no badge (their status is implicit from being visible for auditing).
 * - Two icon buttons replace the badge area:
 *     Eye  → navigate to the protocol's question view (/protocols/:id)
 *     Chart → navigate to the protocol's results (/results?protocolId=:id)
 *             only visible for FINALIZED protocols (DRAFT has no sessions yet).
 */

import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { getProtocols } from "../../api/protocols";
import { CLIENT_ACCENT_COLORS, PROTOCOL_STATUS_META } from "../../constants";
import { getInitials } from "../../utils/formatters";

/**
 * Status badge — only rendered for DRAFT and ARCHIVED protocols.
 * FINALIZED protocols show nothing in this slot.
 *
 * @param {Object} props
 * @param {"DRAFT"|"FINALIZED"|"ARCHIVED"|string} props.status
 */
function ProtocolStatusBadge({ status }) {
  if (status === "FINALIZED") return null;
  const meta = PROTOCOL_STATUS_META[status] ?? PROTOCOL_STATUS_META.DRAFT;
  return (
    <span
      style={{
        background: `var(${meta.bgVar})`,
        color:      `var(${meta.textVar})`,
      }}
      className="text-[9px] px-1.5 py-0.5 rounded-full font-bold uppercase tracking-wider shrink-0"
    >
      {meta.label}
    </span>
  );
}

/**
 * @param {Object}  props
 * @param {import('../../api/clients').ClientResponseDTO} props.client
 * @param {number}  props.colorIdx  - Index into `CLIENT_ACCENT_COLORS` (wraps via modulo).
 * @param {(id: number) => void} props.onFlip
 * @param {boolean} props.flipped
 */
export function ClientCard({ client, colorIdx, onFlip, flipped }) {
  const navigate = useNavigate();
  const color = CLIENT_ACCENT_COLORS[colorIdx % CLIENT_ACCENT_COLORS.length];

  const [protocols,        setProtocols]        = useState([]);
  const [loadingProtocols, setLoadingProtocols] = useState(false);
  const [error,            setError]            = useState(null);
  const hasFetched = useRef(false);

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
        {/* ── FRONT ─────────────────────────────────────────── */}
        <div
          onClick={() => onFlip(client.clientId)}
          className="absolute inset-0 [backface-visibility:hidden] bg-bg-primary border border-border-sec rounded-xl p-5 flex flex-col justify-between shadow-card hover:shadow-md hover:border-border-pri transition-all duration-200"
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
            <p className="text-xl font-bold text-text-pri tracking-tight leading-tight mb-1">
              {client.clientName}
            </p>
            <p className="text-sm text-text-ter">Click to view protocols →</p>
          </div>

          <div className="flex items-center gap-1.5">
            <span style={{ background: color.dot }} className="w-1.5 h-1.5 rounded-full" />
            <span className="text-xs text-text-ter font-mono">Client #{client.clientId}</span>
          </div>
        </div>

        {/* ── BACK ──────────────────────────────────────────── */}
        <div className="absolute inset-0 [backface-visibility:hidden] [transform:rotateY(180deg)] bg-bg-primary border border-border-sec rounded-xl p-4 flex flex-col gap-1.5 overflow-hidden shadow-card">
          <div className="flex items-center justify-between mb-0.5">
            <span style={{ color: color.text }} className="text-sm font-semibold">
              {client.clientName}
            </span>
            <button
              onClick={(e) => { e.stopPropagation(); onFlip(client.clientId); }}
              className="text-text-ter hover:bg-bg-secondary w-5 h-5 rounded flex items-center justify-center transition-colors"
            >
              ✕
            </button>
          </div>

          <div className="flex flex-col gap-1 overflow-y-auto flex-1 pr-1">
            {loadingProtocols && <p className="text-sm text-text-ter">Loading...</p>}
            {error            && <p className="text-sm text-error-on">{error}</p>}
            {!loadingProtocols && !error && protocols.length === 0 && (
              <p className="text-sm text-text-ter">No protocols found.</p>
            )}

            {protocols.map((p) => (
              <div
                key={p.protocolId}
                className="flex items-center justify-between p-2 rounded-md bg-bg-secondary hover:bg-bg-accent transition-colors group"
              >
                {/* Protocol name — clicking navigates to the questions view */}
                <span
                  onClick={(e) => { e.stopPropagation(); navigate(`/protocols/${p.protocolId}`); }}
                  className="text-sm font-medium text-text-sec truncate mr-2 group-hover:text-lsg-blue-dark cursor-pointer flex-1"
                >
                  {p.protocolName}
                </span>

                <div className="flex items-center gap-1 shrink-0">
                  {/* DRAFT / ARCHIVED badge — not shown for FINALIZED */}
                  <ProtocolStatusBadge status={p.protocolStatus} />

                  {/* Eye — view protocol questions */}
                  <button
                    onClick={(e) => { e.stopPropagation(); navigate(`/protocols/${p.protocolId}`); }}
                    title="View protocol"
                    className="w-6 h-6 flex items-center justify-center rounded text-text-ter hover:text-lsg-blue hover:bg-bg-accent transition-colors"
                  >
                    <svg width="13" height="13" viewBox="0 0 15 15" fill="none" stroke="currentColor" strokeWidth="1.4">
                      <path d="M1 7.5S3.5 2.5 7.5 2.5 14 7.5 14 7.5 11.5 12.5 7.5 12.5 1 7.5 1 7.5Z"/>
                      <circle cx="7.5" cy="7.5" r="2"/>
                    </svg>
                  </button>

                  {/* Chart — view results (FINALIZED only) */}
                  {p.protocolStatus === "FINALIZED" && (
                    <button
                      onClick={(e) => { e.stopPropagation(); navigate(`/results?protocolId=${p.protocolId}`); }}
                      title="View results"
                      className="w-6 h-6 flex items-center justify-center rounded text-text-ter hover:text-lsg-blue hover:bg-bg-accent transition-colors"
                    >
                      <svg width="13" height="13" viewBox="0 0 15 15" fill="none" stroke="currentColor" strokeWidth="1.4">
                        <rect x="1.5" y="8" width="2.5" height="5.5" rx="0.5"/>
                        <rect x="6"   y="5" width="2.5" height="8.5" rx="0.5"/>
                        <rect x="10.5" y="2" width="2.5" height="11.5" rx="0.5"/>
                      </svg>
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}