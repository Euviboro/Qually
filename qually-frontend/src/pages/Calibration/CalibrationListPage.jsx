/**
 * @module pages/Calibration/CalibrationListPage
 *
 * Uses callerRole from the backend to determine what each user sees:
 * - SR_QA / CREATOR: group progress (participant completion pills)
 * - EXPERT / PARTICIPANT: own progress only
 * - OPERATIONS with no rounds: "not assigned" message, no Add button
 * - Button: "Open" for all roles
 * - "Close Calibration" button for SR_QA only
 */

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAsync } from "../../hooks/useAsync";
import { getRounds, closeAndCompare } from "../../api/calibration";
import { useAuth } from "../../context/AuthContext";

const MONTHS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

function formatDate(raw) {
  if (!raw) return "—";
  const d = new Date(raw);
  return `${MONTHS[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
}

function StatusBadge({ isOpen, isCalibrated }) {
  if (isOpen) return (
    <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-warning-surface text-warning-text">
      Open
    </span>
  );
  if (isCalibrated === true) return (
    <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-success-surface text-success-on">
      Calibrated
    </span>
  );
  return (
    <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-error-surface text-error-on">
      Needs Calibration
    </span>
  );
}

function ProgressPill({ answered, total }) {
  const pct  = total > 0 ? Math.round((answered / total) * 100) : 0;
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

export default function CalibrationListPage() {
  const navigate = useNavigate();
  const { isQA } = useAuth();
  const [closing,    setClosing]    = useState(null);
  const [closeError, setCloseError] = useState(null);

  const { data: rounds = [], loading, error, refetch } = useAsync(
    () => getRounds(),
    []
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

  // OPERATIONS users with no assigned rounds
  if (!isQA && rounds.length === 0) {
    return (
      <div className="px-6 py-8 max-w-full">
        <header className="mb-6">
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">Calibration</h1>
        </header>
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter font-medium">
            You have not been assigned to any calibration rounds.
          </p>
          <p className="text-text-ter text-sm mt-1">
            Your QA team will notify you when a round is ready.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="px-6 py-8 max-w-full">
      <header className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">Calibration</h1>
          <p className="text-text-ter text-sm mt-0.5">
            {rounds.length} round{rounds.length !== 1 ? "s" : ""}
          </p>
        </div>
        {isQA && (
          <button
            onClick={() => navigate("/calibration/new")}
            className="flex items-center gap-2 px-4 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors"
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
            New Round
          </button>
        )}
      </header>

      {closeError && (
        <div className="mb-4 px-4 py-3 rounded-lg bg-error-surface text-error-on text-sm">
          {closeError}
        </div>
      )}

      {rounds.length === 0 ? (
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter font-medium">No calibration rounds yet.</p>
          {isQA && (
            <button
              onClick={() => navigate("/calibration/new")}
              className="mt-3 text-sm text-lsg-blue hover:underline"
            >
              Create the first round →
            </button>
          )}
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {rounds.map((round) => {
            const callerRole = round.callerRole;  // SR_QA | CREATOR | EXPERT | PARTICIPANT
            const showGroup  = callerRole === "SR_QA" || callerRole === "CREATOR";
            const isSrQa     = callerRole === "SR_QA";
            const myAnswered = round.callerAnsweredCount ?? 0;
            const total      = round.totalGroupCount ?? 0;

            return (
              <div
                key={round.roundId}
                className="bg-bg-primary border border-border-sec rounded-xl p-5 shadow-card hover:border-border-pri transition-colors"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">

                    {/* Round name + status */}
                    <div className="flex items-center gap-2.5 flex-wrap mb-1">
                      <span className="font-mono text-sm font-bold text-text-pri tracking-wide">
                        {round.roundName}
                      </span>
                      <StatusBadge isOpen={round.isOpen} isCalibrated={round.isCalibrated} />
                    </div>

                    <p className="text-xs text-text-ter mb-2">
                      {round.clientName} · {round.protocolName} · {formatDate(round.createdAt)}
                    </p>

                    <p className="text-sm text-text-sec mb-3 line-clamp-2">
                      <span className="font-medium text-text-ter mr-1.5">Q:</span>
                      {round.questionText}
                    </p>

                    {/* SR_QA / CREATOR: participant completion pills */}
                    {showGroup && round.isOpen && round.participants && (
                      <div className="flex flex-wrap gap-1.5">
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
                            {p.isExpert === true && " ★"}
                          </span>
                        ))}
                      </div>
                    )}

                    {/* PARTICIPANT / EXPERT: own progress */}
                    {!showGroup && round.isOpen && (
                      <div className="flex items-center gap-2">
                        <span className="text-[11px] text-text-ter">Your progress:</span>
                        <ProgressPill answered={myAnswered} total={total} />
                        {myAnswered === total && total > 0 && (
                          <span className="text-[11px] text-success-on font-bold">
                            ✓ All answered
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex flex-col gap-2 shrink-0">
                    <button
                      onClick={() => navigate(`/calibration/${round.roundId}`)}
                      className="px-3.5 py-1.5 text-xs font-bold text-lsg-blue border border-lsg-blue rounded-md hover:bg-bg-accent transition-colors whitespace-nowrap"
                    >
                      Open →
                    </button>

                    {/* Close Calibration — SR_QA only, open rounds only */}
                    {isSrQa && round.isOpen && (
                      <button
                        onClick={() => handleClose(round.roundId)}
                        disabled={closing === round.roundId}
                        className="px-3.5 py-1.5 text-xs font-bold text-white bg-lsg-navy hover:bg-lsg-midnight rounded-md transition-colors disabled:opacity-50 whitespace-nowrap"
                      >
                        {closing === round.roundId ? "Closing…" : "Close Calibration"}
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