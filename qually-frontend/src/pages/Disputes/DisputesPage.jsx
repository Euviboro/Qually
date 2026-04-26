/**
 * @module pages/Disputes/DisputesPage
 *
 * Role-aware disputes inbox. Replaces the old DisputeInboxPage which showed
 * only DISPUTED sessions and had no filtering or pagination.
 *
 * Data source: GET /api/disputes/inbox — returns flat response-level rows
 * scoped by the current user's role (see DisputeService.getInbox).
 *
 * Reuses ColumnFilter and Pagination from the Results table for consistency.
 * Columns: Date, Client, Protocol, LOB, Interaction ID, Member Audited,
 *          Question (truncated), Status.
 */

import { useState, useMemo, useCallback, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAsync } from "../../hooks/useAsync";
import { getDisputeInbox } from "../../api/disputes";
import { ColumnFilter } from "../../components/ui/ColumnFilter";
import { Pagination } from "../../components/ui/Pagination";

const PAGE_SIZE      = 100;
const MAX_Q_LEN      = 40;
const MONTHS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

// ── Display status badge ──────────────────────────────────────

const STATUS_META = {
  FLAGGED:  { label: "Flagged",  cls: "bg-warning-surface text-warning-text"  },
  DISPUTED: { label: "Disputed", cls: "bg-error-surface text-error-on"        },
  RESOLVED: { label: "Resolved", cls: "bg-success-surface text-success-on"    },
};

function StatusBadge({ status }) {
  const meta = STATUS_META[status];
  if (!meta) return null;
  return (
    <span className={`text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full ${meta.cls}`}>
      {meta.label}
    </span>
  );
}

// ── Kebab menu ────────────────────────────────────────────────

function KebabMenu({ sessionId, navigate }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div ref={ref} className="relative inline-block">
      <button onClick={() => setOpen((v) => !v)}
        className="p-1.5 rounded-md text-text-ter hover:text-text-pri hover:bg-bg-tertiary transition-colors">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <circle cx="8" cy="3" r="1.5"/><circle cx="8" cy="8" r="1.5"/><circle cx="8" cy="13" r="1.5"/>
        </svg>
      </button>
      {open && (
        <div className="absolute right-0 top-full mt-1 z-50 bg-bg-primary border border-border-sec rounded-lg shadow-lg w-48 py-1">
          <button
            onClick={() => { navigate(`/sessions/${sessionId}`); setOpen(false); }}
            className="w-full text-left px-4 py-2 text-sm text-text-sec hover:bg-bg-secondary transition-colors flex items-center gap-2"
          >
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M1 7s2-4 6-4 6 4 6 4-2 4-6 4-6-4-6-4z"/>
              <circle cx="7" cy="7" r="1.5"/>
            </svg>
            View Session Results
          </button>
        </div>
      )}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────

export default function DisputesPage() {
  const navigate = useNavigate();

  const { data: rows = [], loading, error } = useAsync(
    () => getDisputeInbox(),
    []
  );

  // ── Column filters ─────────────────────────────────────────
  const [filters, setFilters] = useState({});
  const setFilter = useCallback((col, val) =>
    setFilters((prev) => ({ ...prev, [col]: val })), []);

  const uniqueVals = useMemo(() => {
    const get = (key) => [...new Set(rows.map((r) => r[key]).filter(Boolean))].sort();
    return {
      clientName:        get("clientName"),
      protocolName:      get("protocolName"),
      lobName:           get("lobName"),
      memberAuditedName: get("memberAuditedName"),
      displayStatus:     get("displayStatus"),
      sessionDate:       rows.map((r) => r.sessionDate).filter(Boolean),
    };
  }, [rows]);

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      for (const [col, set] of Object.entries(filters)) {
        if (!set || set.size === 0) continue;
        if (col === "sessionDate") {
          const dayStr = r.sessionDate
            ? new Date(r.sessionDate).toISOString().slice(0, 10)
            : null;
          if (!dayStr || !set.has(dayStr)) return false;
        } else {
          if (!set.has(r[col])) return false;
        }
      }
      return true;
    });
  }, [rows, filters]);

  // ── Pagination ─────────────────────────────────────────────
  const [currentPage, setCurrentPage] = useState(0);
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paginated  = filtered.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  const handlePageChange = (page) => {
    setCurrentPage(page);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // Reset to page 0 when filters change
  useEffect(() => { setCurrentPage(0); }, [filters]);

  const hasActiveFilters = Object.values(filters).some((s) => s?.size > 0);

  if (loading) return (
    <div className="p-8 flex items-center gap-3 text-text-ter">
      <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
      Loading disputes…
    </div>
  );

  if (error) return <div className="p-8 text-error-on">{error}</div>;

  return (
    <div className="px-6 py-8 max-w-full">

      <header className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">Disputes</h1>
          <p className="text-text-ter text-sm mt-0.5">
            {filtered.length.toLocaleString()} item{filtered.length !== 1 ? "s" : ""}
            {hasActiveFilters ? " (filtered)" : ""}
          </p>
        </div>
      </header>

      {!loading && filtered.length === 0 ? (
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter font-medium">
            {rows.length === 0 ? "No flagged or disputed responses." : "No items match the current filters."}
          </p>
          {hasActiveFilters && (
            <button onClick={() => setFilters({})} className="mt-3 text-sm text-lsg-blue hover:underline">
              Clear filters
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border border-border-sec bg-bg-primary shadow-card">
            <table className="min-w-full text-sm border-collapse">
              <thead>
                <tr className="border-b border-border-ter bg-bg-secondary/50">
                  {[
                    { key: "sessionDate",       label: "Date",     type: "date" },
                    { key: "clientName",        label: "Client",   type: "text" },
                    { key: "protocolName",      label: "Protocol", type: "text" },
                    { key: "lobName",           label: "LOB",      type: "text" },
                  ].map(({ key, label, type }) => (
                    <th key={key} className="px-4 py-3 text-left whitespace-nowrap">
                      <ColumnFilter label={label} values={uniqueVals[key] ?? []}
                        activeFilter={filters[key] ?? new Set()}
                        onChange={(v) => setFilter(key, v)} type={type} />
                    </th>
                  ))}

                  {/* Interaction ID — plain, no filter */}
                  <th className="px-4 py-3 text-left whitespace-nowrap">
                    <span className="text-[11px] font-bold text-text-sec uppercase tracking-wider">
                      Interaction ID
                    </span>
                  </th>

                  {/* Member Audited */}
                  <th className="px-4 py-3 text-left whitespace-nowrap">
                    <ColumnFilter label="Member Audited" values={uniqueVals.memberAuditedName ?? []}
                      activeFilter={filters.memberAuditedName ?? new Set()}
                      onChange={(v) => setFilter("memberAuditedName", v)} type="text" />
                  </th>

                  {/* Question — plain, no filter (too many unique values) */}
                  <th className="px-4 py-3 text-left" style={{ minWidth: 200, maxWidth: 300 }}>
                    <span className="text-[11px] font-bold text-text-sec uppercase tracking-wider">
                      Question
                    </span>
                  </th>

                  {/* Status */}
                  <th className="px-4 py-3 text-left whitespace-nowrap">
                    <ColumnFilter label="Status" values={uniqueVals.displayStatus ?? []}
                      activeFilter={filters.displayStatus ?? new Set()}
                      onChange={(v) => setFilter("displayStatus", v)} />
                  </th>

                  <th className="px-4 py-3"><span className="sr-only">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {paginated.map((row, idx) => {
                  const d = row.sessionDate ? new Date(row.sessionDate) : null;
                  const dateStr = d
                    ? `${MONTHS[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`
                    : "—";
                  const qText = row.questionText?.length > MAX_Q_LEN
                    ? row.questionText.slice(0, MAX_Q_LEN) + "…"
                    : (row.questionText ?? "—");

                  return (
                    <tr key={`${row.sessionId}-${row.responseId}`}
                      className={["border-b border-border-ter transition-colors hover:bg-bg-secondary/40",
                        idx % 2 === 0 ? "" : "bg-bg-secondary/20"].join(" ")}>
                      <td className="px-4 py-3 text-xs text-text-ter whitespace-nowrap tabular-nums">{dateStr}</td>
                      <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{row.clientName ?? "—"}</td>
                      <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap max-w-[140px] truncate" title={row.protocolName}>{row.protocolName ?? "—"}</td>
                      <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{row.lobName ?? "—"}</td>
                      <td className="px-4 py-3 text-xs text-text-ter font-mono whitespace-nowrap">
                        {row.interactionId && !row.interactionId.startsWith("DRAFT-")
                          ? row.interactionId : "—"}
                      </td>
                      <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{row.memberAuditedName ?? "—"}</td>
                      <td className="px-4 py-3 text-xs text-text-sec max-w-[300px]" title={row.questionText}>
                        {qText}
                        {row.disputeComment && (
                          <p className="text-text-ter mt-0.5 italic truncate">"{row.disputeComment}"</p>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={row.displayStatus} />
                      </td>
                      <td className="px-4 py-3">
                        <KebabMenu sessionId={row.sessionId} navigate={navigate} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={handlePageChange}
          />

          {totalPages > 1 && (
            <p className="text-center text-xs text-text-ter -mt-2 pb-2">
              Page {currentPage + 1} of {totalPages} · {filtered.length.toLocaleString()} total
            </p>
          )}
        </>
      )}
    </div>
  );
}