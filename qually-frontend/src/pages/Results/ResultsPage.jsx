/**
 * @module pages/Results/ResultsPage

 */

import { useState, useMemo, useCallback, useRef, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { createPortal } from "react-dom";
import { useAsync } from "../../hooks/useAsync";
import { getResults } from "../../api/results";
import { COPC_CATEGORY_META, AUDIT_STATUS_META } from "../../constants";
import { ColumnFilter } from "../../components/ui/ColumnFilter";
import { Pagination } from "../../components/ui/Pagination";
import { useAuth } from "../../context/AuthContext";

const PAGE_SIZE = 100;

// ── Score pill ────────────────────────────────────────────────

function ScorePill({ score }) {
  if (score == null) return <span className="text-text-ter text-xs">—</span>;
  return (
    <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-bold tabular-nums ${
      score === 100 ? "bg-success-surface text-success-on" : "bg-error-surface text-error-on"
    }`}>
      {score}
    </span>
  );
}

// ── Status badge ──────────────────────────────────────────────

function StatusBadge({ status }) {
  const meta = AUDIT_STATUS_META[status];
  if (!meta) return <span className="text-xs text-text-ter">{status}</span>;
  return (
    <span style={{ background: meta.bg, color: meta.text }}
      className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full">
      {meta.label}
    </span>
  );
}

// ── Answer pill ───────────────────────────────────────────────

function AnswerPill({ answer }) {
  if (!answer) return <span className="text-text-ter text-xs">—</span>;
  const cls = answer === "YES" ? "bg-success-surface text-success-on"
            : answer === "NO"  ? "bg-error-surface text-error-on"
            : "bg-bg-tertiary text-text-ter";
  return <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded uppercase ${cls}`}>{answer}</span>;
}

// ── Protocol guard modal ──────────────────────────────────────

function ProtocolGuardModal({ protocols, onSelect, onClose }) {
  const [selected, setSelected] = useState(null);
  return (
    <div onClick={onClose} className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-[rgba(0,20,50,0.4)] backdrop-blur-[2px]">
      <div onClick={(e) => e.stopPropagation()} className="w-full max-w-[420px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-7 flex flex-col gap-5">
        <div>
          <h2 className="text-lg font-bold text-text-pri">Select a Protocol</h2>
          <p className="text-xs text-text-ter mt-1">Question columns are only available when viewing a single protocol.</p>
        </div>
        <div className="flex flex-col gap-2 max-h-60 overflow-y-auto">
          {protocols.map((p) => (
            <button key={p.id} onClick={() => setSelected(p.id)}
              className={["w-full text-left px-4 py-2.5 rounded-lg border-2 text-sm transition-all",
                selected === p.id ? "border-lsg-blue bg-bg-accent text-lsg-blue-dark font-semibold"
                : "border-border-sec text-text-sec hover:border-border-pri"].join(" ")}>
              {p.name}
            </button>
          ))}
        </div>
        <div className="flex justify-end gap-3">
          <button onClick={onClose} className="px-4 py-2 text-sm font-medium text-text-sec border border-border-sec rounded-md hover:bg-bg-secondary transition-colors">Cancel</button>
          <button onClick={() => selected && onSelect(selected)} disabled={!selected}
            className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
            Show Questions
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Kebab menu — portal-based, anchored to click point ────────

function KebabMenu({ sessionId, navigate }) {
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const btnRef  = useRef(null);
  const menuRef = useRef(null);

  const handleOpen = () => {
    const rect = btnRef.current.getBoundingClientRect();
    setPos({ top: rect.bottom + window.scrollY + 4, left: rect.left });
    setOpen(true);
  };

  useEffect(() => {
    if (!open) return;
    const handler = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target) &&
          btnRef.current  && !btnRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [open]);

  return (
    <>
      <button ref={btnRef} onClick={handleOpen}
        className="p-1.5 rounded-md text-text-ter hover:text-text-pri hover:bg-bg-tertiary transition-colors">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <circle cx="8" cy="3" r="1.5"/><circle cx="8" cy="8" r="1.5"/><circle cx="8" cy="13" r="1.5"/>
        </svg>
      </button>

      {open && createPortal(
        <div ref={menuRef}
          style={{ position: "fixed", top: pos.top, left: pos.left, zIndex: 9999 }}
          className="bg-bg-primary border border-border-sec rounded-lg shadow-lg w-48 py-1">
          <button
            onClick={() => { navigate(`/sessions/${sessionId}`); setOpen(false); }}
            className="w-full text-left px-4 py-2 text-sm text-text-sec hover:bg-bg-secondary transition-colors flex items-center gap-2"
          >
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M1 7s2-4 6-4 6 4 6 4-2 4-6 4-6-4-6-4z"/>
              <circle cx="7" cy="7" r="1.5"/>
            </svg>
            View Audit Results
          </button>
        </div>,
        document.body
      )}
    </>
  );
}

// ── Page ──────────────────────────────────────────────────────

const MAX_QUESTION_LABEL_LEN = 20;

export default function ResultsPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isQA } = useAuth();

  const protocolId = searchParams.get("protocolId") ? Number(searchParams.get("protocolId")) : undefined;
  const clientId   = searchParams.get("clientId")   ? Number(searchParams.get("clientId"))   : undefined;
  const auditorId  = searchParams.get("auditorId")  ? Number(searchParams.get("auditorId"))  : undefined;
  const memberId   = searchParams.get("memberId")   ? Number(searchParams.get("memberId"))   : undefined;

  const [currentPage,          setCurrentPage]          = useState(0);
  const [showQuestions,        setShowQuestions]        = useState(false);
  const [protocolGuardOpen,    setProtocolGuardOpen]    = useState(false);
  const [activeProtocolFilter, setActiveProtocolFilter] = useState(protocolId ?? null);

  useEffect(() => { setCurrentPage(0); }, [protocolId, clientId, auditorId, memberId]);

  const { data: pagedResult, loading, error } = useAsync(
    () => getResults({
      protocolId:     activeProtocolFilter ?? protocolId,
      clientId,
      auditorId,
      memberId,
      includeAnswers: showQuestions,
      page:           currentPage,
      size:           PAGE_SIZE,
    }),
    [activeProtocolFilter, showQuestions, protocolId, clientId, auditorId, memberId, currentPage]
  );

  const allRows       = pagedResult?.content       ?? [];
  const totalElements = pagedResult?.totalElements ?? 0;
  const totalPages    = pagedResult?.totalPages    ?? 0;

  const rows = useMemo(
    () => allRows.filter((r) => r.auditStatus !== "DRAFT"),
    [allRows]
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
      customerScore:     [...new Set(rows.map((r) => r.customerScore).filter((v) => v != null))].sort(),
      businessScore:     [...new Set(rows.map((r) => r.businessScore).filter((v) => v != null))].sort(),
      complianceScore:   [...new Set(rows.map((r) => r.complianceScore).filter((v) => v != null))].sort(),
      auditStatus:       get("auditStatus"),
      sessionDate:       rows.map((r) => r.sessionDate).filter(Boolean),
    };
  }, [rows]);

  const filtered = useMemo(() => rows.filter((r) => {
    for (const [col, set] of Object.entries(filters)) {
      if (!set || set.size === 0) continue;
      if (col === "sessionDate") {
        const dayStr = r.sessionDate ? new Date(r.sessionDate).toISOString().slice(0, 10) : null;
        if (!dayStr || !set.has(dayStr)) return false;
      } else {
        if (!set.has(r[col])) return false;
      }
    }
    return true;
  }), [rows, filters]);

  // ── Question columns ───────────────────────────────────────
  const questionHeaders = useMemo(() => {
    if (!showQuestions || !filtered.length) return [];
    const first = filtered.find((r) => r.questionAnswers?.length);
    return first?.questionAnswers ?? [];
  }, [showQuestions, filtered]);

  const distinctProtocols = useMemo(() => {
    const seen = new Map();
    rows.forEach((r) => { if (r.protocolId) seen.set(r.protocolId, r.protocolName); });
    return [...seen.entries()].map(([id, name]) => ({ id, name }));
  }, [rows]);

  const handleToggleQuestions = () => {
    if (showQuestions) { setShowQuestions(false); return; }
    if (distinctProtocols.length > 1 && !activeProtocolFilter) {
      setProtocolGuardOpen(true);
    } else {
      setShowQuestions(true);
    }
  };

  const handleProtocolSelect = (pid) => {
    setActiveProtocolFilter(pid);
    setProtocolGuardOpen(false);
    setShowQuestions(true);
    setCurrentPage(0);
  };

  const handlePageChange = (page) => {
    setCurrentPage(page);
    setFilters({});
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const pageTitle = clientId  ? "Client Results"
    : protocolId ? "Protocol Results"
    : auditorId  ? "Auditor Results"
    : memberId   ? "My Results"
    : "All Results";

  const hasActiveFilters = Object.values(filters).some((s) => s?.size > 0);

  if (loading && currentPage === 0) return (
    <div className="p-8 flex items-center gap-3 text-text-ter">
      <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
      Loading results…
    </div>
  );

  if (error) return <div className="p-8 text-error-on">{error}</div>;

  return (
    <div className="px-6 py-8 max-w-full">

      {/* Header */}
      <header className="flex items-center justify-between mb-6">
        <div>
          {isQA && (
            <button onClick={() => navigate(-1)} className="text-lsg-blue hover:text-lsg-blue-dark text-sm font-medium mb-2 flex items-center gap-1 transition-colors">← Back</button>
          )}
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">{pageTitle}</h1>
          <p className="text-text-ter text-sm mt-0.5">
            {loading ? "Loading…"
              : `${totalElements.toLocaleString()} total session${totalElements !== 1 ? "s" : ""}${hasActiveFilters ? ` · ${filtered.length} shown (filtered)` : ""}`}
          </p>
        </div>
        <button onClick={handleToggleQuestions}
          className={["flex items-center gap-2 px-4 py-2 text-sm font-bold rounded-md border transition-all",
            showQuestions ? "bg-lsg-blue text-white border-transparent" : "border-border-sec text-text-sec hover:border-border-pri"].join(" ")}>
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5">
            <rect x="1" y="1" width="12" height="12" rx="1.5"/>
            <path d="M1 5h12M5 5v8M9 5v8"/>
          </svg>
          {showQuestions ? "Hide Questions" : "Show Questions"}
        </button>
      </header>

      {!loading && filtered.length === 0 ? (
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter font-medium">
            {totalElements === 0 ? "No sessions found." : "No sessions match the current column filters."}
          </p>
          {hasActiveFilters && (
            <button onClick={() => setFilters({})} className="mt-3 text-sm text-lsg-blue hover:underline">Clear column filters</button>
          )}
        </div>
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border border-border-sec bg-bg-primary shadow-card">
            <table className="min-w-full text-sm border-collapse">
              <thead>
                <tr className="border-b border-border-ter bg-bg-secondary/50">

                  {/* Actions — first column, 40px */}
                  <th className="w-10 px-2 py-3"><span className="sr-only">Actions</span></th>

                  {/* Filterable columns */}
                  {[
                    { key: "sessionDate",  label: "Date",     type: "date" },
                    { key: "clientName",   label: "Client",   type: "text" },
                    { key: "protocolName", label: "Protocol", type: "text" },
                    { key: "lobName",      label: "LOB",      type: "text" },
                  ].map(({ key, label, type }) => (
                    <th key={key} className="px-4 py-3 text-left whitespace-nowrap">
                      <ColumnFilter label={label} values={uniqueVals[key] ?? []}
                        activeFilter={filters[key] ?? new Set()}
                        onChange={(v) => setFilter(key, v)} type={type} />
                    </th>
                  ))}

                  {/* Interaction ID — plain, no filter */}
                  <th className="px-4 py-3 text-left whitespace-nowrap">
                    <span className="text-[11px] font-bold text-text-sec uppercase tracking-wider">Interaction ID</span>
                  </th>

                  {/* Member Audited */}
                  <th className="px-4 py-3 text-left whitespace-nowrap">
                    <ColumnFilter label="Member Audited" values={uniqueVals.memberAuditedName ?? []}
                      activeFilter={filters.memberAuditedName ?? new Set()}
                      onChange={(v) => setFilter("memberAuditedName", v)} type="text" />
                  </th>

                  {/* Scores */}
                  {[
                    { key: "customerScore",   label: "Customer"   },
                    { key: "businessScore",   label: "Business"   },
                    { key: "complianceScore", label: "Compliance" },
                  ].map(({ key, label }) => (
                    <th key={key} className="px-4 py-3 text-left whitespace-nowrap">
                      <ColumnFilter label={label} values={uniqueVals[key] ?? []}
                        activeFilter={filters[key] ?? new Set()}
                        onChange={(v) => setFilter(key, v)} type="score" />
                    </th>
                  ))}

                  {/* Status */}
                  <th className="px-4 py-3 text-left whitespace-nowrap">
                    <ColumnFilter label="Status" values={uniqueVals.auditStatus}
                      activeFilter={filters.auditStatus ?? new Set()}
                      onChange={(v) => setFilter("auditStatus", v)} />
                  </th>

                  {/* Question columns */}
                  {showQuestions && questionHeaders.map((q) => {
                    const truncated = q.questionText.length > MAX_QUESTION_LABEL_LEN
                      ? q.questionText.slice(0, MAX_QUESTION_LABEL_LEN) + "…"
                      : q.questionText;
                    const catMeta = COPC_CATEGORY_META[q.category];
                    return (
                      <th key={q.questionId} style={{ maxWidth: 180, minWidth: 120 }} className="px-3 py-3 text-left">
                        <div className="flex flex-col gap-0.5">
                          {catMeta && (
                            <span style={{ color: `var(${catMeta.textVar})` }} className="text-[9px] font-bold uppercase tracking-widest">
                              {catMeta.label}
                            </span>
                          )}
                          <span className="text-[10px] font-semibold text-text-sec truncate" title={q.questionText}>
                            {truncated}
                          </span>
                        </div>
                      </th>
                    );
                  })}
                </tr>
              </thead>
              <tbody>
                {filtered.map((row, idx) => (
                  <tr key={row.sessionId}
                    className={["border-b border-border-ter transition-colors hover:bg-bg-secondary/40",
                      loading ? "opacity-50" : "",
                      idx % 2 === 0 ? "" : "bg-bg-secondary/20"].join(" ")}>

                    {/* Actions — first */}
                    <td className="w-10 px-2 py-3 text-center">
                      <KebabMenu sessionId={row.sessionId} navigate={navigate} />
                    </td>

                    <td className="px-4 py-3 text-xs text-text-ter whitespace-nowrap tabular-nums">
                      {row.sessionDate ? new Date(row.sessionDate).toLocaleDateString() : "—"}
                    </td>
                    <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{row.clientName ?? "—"}</td>
                    <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap max-w-[160px] truncate" title={row.protocolName}>{row.protocolName ?? "—"}</td>
                    <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{row.lobName ?? "—"}</td>
                    <td className="px-4 py-3 text-xs text-text-ter font-mono whitespace-nowrap">{row.interactionId ?? "—"}</td>
                    <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{row.memberAuditedName ?? "—"}</td>
                    <td className="px-4 py-3 text-center"><ScorePill score={row.customerScore} /></td>
                    <td className="px-4 py-3 text-center"><ScorePill score={row.businessScore} /></td>
                    <td className="px-4 py-3 text-center"><ScorePill score={row.complianceScore} /></td>
                    <td className="px-4 py-3"><StatusBadge status={row.auditStatus} /></td>

                    {showQuestions && questionHeaders.map((q) => {
                      const ans = row.questionAnswers?.find((a) => a.questionId === q.questionId);
                      return (
                        <td key={q.questionId} className="px-3 py-3 text-center" style={{ maxWidth: 120 }}>
                          <AnswerPill answer={ans?.effectiveAnswer} />
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={handlePageChange} />

          {totalPages > 1 && (
            <p className="text-center text-xs text-text-ter -mt-2 pb-2">
              Page {currentPage + 1} of {totalPages} · {totalElements.toLocaleString()} total
            </p>
          )}
        </>
      )}

      {protocolGuardOpen && (
        <ProtocolGuardModal
          protocols={distinctProtocols}
          onSelect={handleProtocolSelect}
          onClose={() => setProtocolGuardOpen(false)}
        />
      )}
    </div>
  );
}