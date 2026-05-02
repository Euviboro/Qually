/**
 * @module pages/Calibration/CalibrationListPage
 *
 * Lists all calibration rounds visible to the current user.
 * QA Specialists see rounds they are enrolled in.
 * QA Managers see all rounds in their management chain with a Close button.
 */

import { useNavigate } from "react-router-dom";
import { useAsync } from "../../hooks/useAsync";
import { getRounds, closeAndCompare } from "../../api/calibration";
import { useAuth } from "../../context/AuthContext";
import { useState } from "react";

const MONTHS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

function formatDate(raw) {
  if (!raw) return "—";
  const d = new Date(raw);
  return `${MONTHS[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
}

// ── Status badge ──────────────────────────────────────────────

function StatusBadge({ isOpen, isCalibrated }) {
  if (isOpen) {
    return (
      <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-warning-surface text-warning-text">
        Open
      </span>
    );
  }
  if (isCalibrated === true) {
    return (
      <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-success-surface text-success-on">
        Calibrated
      </span>
    );
  }
  return (
    <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-error-surface text-error-on">
      Not Calibrated
    </span>
  );
}

// ── Progress bar ──────────────────────────────────────────────

function ProgressPill({ answered, total }) {
  const pct = total > 0 ? Math.round((answered / total) * 100) : 0;
  const done = answered === total && total > 0;
  return (
    <div className="flex items-center gap-2">
      <div className="w-20 h-1.5 bg-bg-tertiary rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${done ? "bg-success-dot" : "bg-lsg-blue"}`}
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="text-[11px] text-text-ter tabular-nums">{answered}/{total}</span>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────

export default function CalibrationListPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [closing, setClosing] = useState(null);
  const [closeError, setCloseError] = useState(null);

  const { data: rounds = [], loading, error, refetch } = useAsync(
    () => getRounds(),
    []
  );

  // Determine if the user is a manager based on whether any round
  // shows isExpert as non-null for a participant (only managers get that)
  // A simpler check: manager if they see rounds they didn't create as enrolled
  // We use the presence of isExpert=true/false in participants as a signal
  const isManager = rounds.some(r =>
    r.participants?.some(p => p.isExpert !== null && p.isExpert !== undefined)
  );

  const handleClose = async (roundId) => {
    setClosing(roundId);
    setCloseError(null);
    try {
      await closeAndCompare(roundId);
      refetch();
    } catch (err) {
      setCloseError(err.message);
    } finally {
      setClosing(null);
    }
  };

  if (loading) return (
    <div className="p-8 flex items-center gap-3 text-text-ter">
      <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
      Loading calibration rounds…
    </div>
  );

  if (error) return <div className="p-8 text-error-on">{error}</div>;

  return (
    <div className="px-6 py-8 max-w-full">
      <header className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">Calibration</h1>
          <p className="text-text-ter text-sm mt-0.5">
            {rounds.length} round{rounds.length !== 1 ? "s" : ""}
          </p>
        </div>
        <button
          onClick={() => navigate("/calibration/new")}
          className="flex items-center gap-2 px-4 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors"
        >
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
          </svg>
          New Round
        </button>
      </header>

      {closeError && (
        <div className="mb-4 px-4 py-3 rounded-lg bg-error-surface text-error-on text-sm">
          {closeError}
        </div>
      )}

      {rounds.length === 0 ? (
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter font-medium">No calibration rounds yet.</p>
          <button
            onClick={() => navigate("/calibration/new")}
            className="mt-3 text-sm text-lsg-blue hover:underline"
          >
            Create the first round →
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {rounds.map((round) => {
            const myAnswered = round.callerAnsweredCount ?? 0;
            const total      = round.totalGroupCount ?? 0;
            const allDone    = myAnswered === total && total > 0;

            return (
              <div
                key={round.roundId}
                className="bg-bg-primary border border-border-sec rounded-xl p-5 shadow-card hover:border-border-pri transition-colors"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    {/* Round name + status */}
                    <div className="flex items-center gap-2.5 mb-1 flex-wrap">
                      <span className="font-mono text-sm font-bold text-text-pri tracking-wide">
                        {round.roundName}
                      </span>
                      <StatusBadge isOpen={round.isOpen} isCalibrated={round.isCalibrated} />
                    </div>

                    {/* Meta */}
                    <p className="text-xs text-text-ter mb-3">
                      {round.clientName} · {round.protocolName} · {formatDate(round.createdAt)}
                    </p>

                    {/* Question */}
                    <p className="text-sm text-text-sec mb-3 line-clamp-2">
                      <span className="font-medium text-text-ter mr-1.5">Q:</span>
                      {round.questionText}
                    </p>

                    {/* Progress — only for open rounds */}
                    {round.isOpen && (
                      <div className="flex items-center gap-2">
                        <span className="text-[11px] text-text-ter">Your progress:</span>
                        <ProgressPill answered={myAnswered} total={total} />
                        {allDone && (
                          <span className="text-[11px] text-success-on font-bold">
                            ✓ All answered
                          </span>
                        )}
                      </div>
                    )}

                    {/* Manager: participant summary */}
                    {isManager && round.participants && (
                      <div className="mt-2 flex items-center gap-1.5 flex-wrap">
                        {round.participants.map((p) => (
                          <span
                            key={p.userId}
                            title={`${p.fullName} — ${p.answeredCount}/${p.totalCount} answered`}
                            className={[
                              "text-[10px] px-2 py-0.5 rounded-full font-medium",
                              p.hasAnsweredAll
                                ? "bg-success-surface text-success-on"
                                : "bg-bg-secondary text-text-ter",
                            ].join(" ")}
                          >
                            {p.fullName.split(" ")[0]}
                            {p.isExpert && " ★"}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex flex-col gap-2 shrink-0">
                    <button
                      onClick={() => navigate(`/calibration/${round.roundId}`)}
                      className="px-3.5 py-1.5 text-xs font-bold text-lsg-blue border border-lsg-blue rounded-md hover:bg-bg-accent transition-colors whitespace-nowrap"
                    >
                      {round.isOpen ? "Answer →" : "View Results →"}
                    </button>

                    {/* Close button — manager only, open rounds only */}
                    {isManager && round.isOpen && (
                      <button
                        onClick={() => handleClose(round.roundId)}
                        disabled={closing === round.roundId}
                        className="px-3.5 py-1.5 text-xs font-bold text-white bg-lsg-navy hover:bg-lsg-midnight rounded-md transition-colors disabled:opacity-50 whitespace-nowrap"
                      >
                        {closing === round.roundId ? "Closing…" : "Close Round"}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}