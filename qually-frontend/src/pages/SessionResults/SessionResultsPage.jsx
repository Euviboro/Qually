/**
 * @module pages/SessionResults/SessionResultsPage
 *
 * Displays the full results of a completed audit session.
 *
 * Header changes:
 * - Interaction ID, LOB, Member Audited added
 * - Protocol version removed
 * - Date shown as "Apr 22, 2026 · 02:38" to avoid dd/mm vs mm/dd ambiguity
 *
 * Response cards:
 * - AnswerPill replaced with AnswerIndicator
 * - ANSWERED response status badge removed (the checkmark/X communicates it)
 * - FLAGGED, DISPUTED, RESOLVED badges remain
 */

import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../context/AuthContext";
import { DisputeModal } from "../../components/ui/DisputeModal";
import { ResolveDisputeModal } from "../../components/ui/ResolveDisputeModal";
import { AnswerIndicator } from "../../components/ui/AnswerIndicator";
import { COPC_CATEGORY_META, AUDIT_STATUS_META } from "../../constants";
import { api } from "../../api/apiClient";

const getSessionResults = (id) => api.get(`/sessions/${id}/results`);

// ── Date formatter ────────────────────────────────────────────

const MONTHS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

/**
 * Formats a date string as "Apr 22, 2026 · 02:38".
 * Uses a spelled-out month abbreviation to avoid dd/mm vs mm/dd ambiguity.
 *
 * @param {string|null|undefined} raw - ISO date string from the backend.
 * @returns {string}
 */
function formatDateTime(raw) {
  if (!raw) return "—";
  const d = new Date(raw);
  if (isNaN(d)) return "—";
  const month  = MONTHS[d.getMonth()];
  const day    = d.getDate();
  const year   = d.getFullYear();
  const hours  = String(d.getHours()).padStart(2, "0");
  const mins   = String(d.getMinutes()).padStart(2, "0");
  return `${month} ${day}, ${year} · ${hours}:${mins}`;
}

// ── Sub-components ────────────────────────────────────────────

function ScoreCard({ category, scores }) {
  const meta     = COPC_CATEGORY_META[category];
  const original = scores.find((s) => s.category === category && !s.isPostDispute);
  const revised  = scores.find((s) => s.category === category && s.isPostDispute);

  return (
    <div className="flex-1 min-w-[140px] rounded-xl border border-border-sec bg-bg-primary p-5 text-center shadow-card">
      <p className="text-[10px] font-bold uppercase tracking-widest text-text-ter mb-3">
        {meta?.label ?? category}
      </p>
      {original ? (
        <>
          <p className={`text-4xl font-extrabold ${original.score === 100 ? "text-success-on" : "text-error-on"}`}>
            {original.score}
          </p>
          {revised && (
            <div className="mt-2 pt-2 border-t border-border-ter">
              <p className="text-[10px] text-text-ter mb-0.5">After dispute</p>
              <p className={`text-2xl font-bold ${revised.score === 100 ? "text-success-on" : "text-error-on"}`}>
                {revised.score}
              </p>
            </div>
          )}
        </>
      ) : (
        <p className="text-2xl font-bold text-text-ter">—</p>
      )}
    </div>
  );
}

function StatusBadge({ status }) {
  const meta = AUDIT_STATUS_META[status];
  if (!meta) return null;
  return (
    <span
      style={{ background: meta.bg, color: meta.text }}
      className="text-[10px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-full"
    >
      {meta.label}
    </span>
  );
}

/**
 * Only renders for active workflow states — FLAGGED, DISPUTED, RESOLVED.
 * ANSWERED is intentionally omitted: the AnswerIndicator already communicates
 * the answered state visually via the checkmark or X icon.
 */
function ResponseStatusBadge({ status }) {
  const cfg = {
    FLAGGED:  { label: "Flagged",  cls: "bg-warning-surface text-warning-on" },
    DISPUTED: { label: "Disputed", cls: "bg-error-surface text-error-on"     },
    RESOLVED: { label: "Resolved", cls: "bg-success-surface text-success-on" },
  }[status];

  if (!cfg) return null;

  return (
    <span className={`text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full ${cfg.cls}`}>
      {cfg.label}
    </span>
  );
}

// ── Page ──────────────────────────────────────────────────────

export default function SessionResultsPage() {
  const { id }   = useParams();
  const navigate = useNavigate();
  const { user, isQA, isOperations } = useAuth();

  const { data: results, loading, error, refetch } = useAsync(
    () => getSessionResults(id),
    [id]
  );

  const [disputeTarget, setDisputeTarget] = useState(null);
  const [resolveTarget, setResolveTarget] = useState(null);

  if (loading) {
    return (
      <div className="p-8 flex items-center gap-3 text-text-ter">
        <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
        Loading results…
      </div>
    );
  }
  if (error)    return <div className="p-8 text-error-on">{error}</div>;
  if (!results) return null;

  const { session, scores, responses } = results;

  // Permission helpers
  const userClientIds   = user?.clientIds ?? [];
  const sessionClientId = session.clientId ?? null;
  const hasClientAccess = isQA || userClientIds.includes(sessionClientId);
  const canFlag         = isOperations && hasClientAccess;
  const canDispute      = canFlag && (user?.hierarchyLevel ?? 99) <= 6;
  const canResolve      = isQA;

  return (
    <div className="max-w-[900px] mx-auto px-8 py-10">

      {/* Back */}
      <button
        onClick={() => navigate(-1)}
        className="text-lsg-blue hover:text-lsg-blue-dark text-sm font-medium mb-6 flex items-center gap-1 transition-colors"
      >
        ← Back
      </button>

      {/* ── Session header ──────────────────────────────────── */}
      <header className="mb-8">
        <div className="flex items-start justify-between gap-4 mb-4">
          <div>
            <h1 className="text-2xl font-bold text-text-pri tracking-tight">
              {session.protocolName}
            </h1>
            <p className="text-text-ter text-sm mt-0.5">
              {session.clientName}
            </p>
          </div>
          <StatusBadge status={session.auditStatus} />
        </div>

        {/* Session metadata grid */}
        <div className="grid grid-cols-2 gap-x-8 gap-y-2 sm:grid-cols-3 bg-bg-primary rounded-xl border border-border-sec p-4">
          <MetaRow label="Interaction ID" value={session.interactionId} />
          <MetaRow label="LOB"            value={session.lobName} />
          <MetaRow label="Date"           value={formatDateTime(session.startedAt)} />
          <MetaRow label="Auditor"        value={session.auditorName} />
          <MetaRow label="Member Audited" value={session.memberAuditedName} />
          {session.comments && (
            <div className="col-span-2 sm:col-span-3 pt-2 border-t border-border-ter mt-1">
              <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-0.5">Comments</p>
              <p className="text-sm text-text-sec">{session.comments}</p>
            </div>
          )}
        </div>
      </header>

      {/* ── COPC Scores ─────────────────────────────────────── */}
      <section className="mb-8">
        <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-3">
          COPC Scores
        </p>
        <div className="flex gap-4 flex-wrap">
          {["CUSTOMER", "BUSINESS", "COMPLIANCE"].map((cat) => (
            <ScoreCard key={cat} category={cat} scores={scores} />
          ))}
        </div>
      </section>

      {/* ── Questions & Answers ──────────────────────────────── */}
      <section>
        <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-3">
          Questions & Answers
        </p>
        <div className="flex flex-col gap-3">
          {responses.map((r, idx) => {
            const meta       = COPC_CATEGORY_META[r.category];
            const isFlagged  = r.responseStatus === "FLAGGED";
            const isDisputed = r.responseStatus === "DISPUTED";
            const isResolved = r.responseStatus === "RESOLVED";
            const isAnswered = r.responseStatus === "ANSWERED";

            return (
              <div
                key={r.responseId}
                className={[
                  "bg-bg-primary border rounded-xl p-5 transition-colors",
                  isDisputed ? "border-error-surface border-l-4 border-l-[var(--color-error)]" :
                  isFlagged  ? "border-warning-surface border-l-4 border-l-[var(--color-warning-dot)]" :
                  isResolved ? "border-success-surface" :
                               "border-border-sec",
                ].join(" ")}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    {/* Category + workflow status badges */}
                    <div className="flex flex-wrap items-center gap-2 mb-1.5">
                      <span className="font-mono text-xs text-text-ter">
                        #{String(idx + 1).padStart(2, "0")}
                      </span>
                      {meta && (
                        <span
                          style={{ background: `var(${meta.bgVar})`, color: `var(${meta.textVar})` }}
                          className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
                        >
                          {meta.label}
                        </span>
                      )}
                      {/* ANSWERED is intentionally omitted — AnswerIndicator communicates it */}
                      <ResponseStatusBadge status={r.responseStatus} />
                    </div>
                    <p className="text-sm font-semibold text-text-pri">{r.questionText}</p>
                  </div>

                  {/* Answer(s) — show override arrow when a dispute changed the answer */}
                  <div className="flex items-center gap-2 shrink-0">
                    <AnswerIndicator answer={r.originalAnswer} />
                    {r.effectiveAnswer !== r.originalAnswer && (
                      <>
                        <span className="text-text-ter text-xs">→</span>
                        <AnswerIndicator answer={r.effectiveAnswer} />
                      </>
                    )}
                  </div>
                </div>

                {/* Dispute detail */}
                {r.dispute && (
                  <div className="mt-3 pt-3 border-t border-border-ter">
                    <p className="text-xs text-text-ter">
                      <span className="font-bold text-text-sec">Dispute:</span>{" "}
                      {r.dispute.reasonText}
                      {r.dispute.disputeComment && ` — "${r.dispute.disputeComment}"`}
                    </p>
                    {r.dispute.resolutionOutcome && (
                      <p className="text-xs text-text-ter mt-0.5">
                        <span className="font-bold text-text-sec">Resolution:</span>{" "}
                        {r.dispute.resolutionOutcome}
                        {r.dispute.resolutionNote && ` — ${r.dispute.resolutionNote}`}
                      </p>
                    )}
                  </div>
                )}

                {/* Action buttons */}
                <div className="flex gap-2 mt-3 flex-wrap">
                  {canFlag && isAnswered && r.originalAnswer !== "YES" && (
                    <button
                      onClick={() => setDisputeTarget(r)}
                      className="text-[11px] font-bold px-3 py-1.5 rounded-md border border-warning-on text-warning-on hover:bg-warning-surface transition-all"
                    >
                      Flag
                    </button>
                  )}
                  {canFlag && isFlagged && (
                    <button
                      onClick={() => setDisputeTarget(r)}
                      className="text-[11px] font-bold px-3 py-1.5 rounded-md border border-error-on text-error-on hover:bg-error-surface transition-all"
                    >
                      {canDispute ? "Raise Dispute" : "Unflag"}
                    </button>
                  )}
                  {canResolve && isDisputed && (
                    <button
                      onClick={() => setResolveTarget(r)}
                      className="text-[11px] font-bold px-3 py-1.5 rounded-md border border-lsg-blue text-lsg-blue hover:bg-bg-accent transition-all"
                    >
                      Resolve
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Modals */}
      <DisputeModal
        isOpen={!!disputeTarget}
        response={disputeTarget}
        onClose={() => setDisputeTarget(null)}
        onSuccess={refetch}
      />
      <ResolveDisputeModal
        isOpen={!!resolveTarget}
        response={resolveTarget}
        onClose={() => setResolveTarget(null)}
        onSuccess={refetch}
      />
    </div>
  );
}

// ── MetaRow ───────────────────────────────────────────────────

/**
 * A labelled metadata row inside the session info grid.
 *
 * @param {Object} props
 * @param {string} props.label
 * @param {string|null|undefined} props.value
 */
function MetaRow({ label, value }) {
  return (
    <div>
      <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-0.5">
        {label}
      </p>
      <p className="text-sm text-text-pri font-medium">{value ?? "—"}</p>
    </div>
  );
}
