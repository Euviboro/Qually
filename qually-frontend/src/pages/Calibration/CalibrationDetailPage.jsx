/**
 * @module pages/Calibration/CalibrationDetailPage
 *
 * Detail and answer page for a calibration round.
 *
 * Three modes based on round state and caller role:
 *
 * Participant + Open round:
 *   - Question displayed at top
 *   - One row per interaction with YES/NO/N/A toggles
 *   - Submitted answers lock immediately
 *   - When all answered: "Waiting for round to close" state
 *
 * Participant + Closed round:
 *   - All answers locked
 *   - Each interaction shows own answer + expert answer side by side
 *   - "Needs Calibration" shown if answers differ, nothing if they match
 *   - Expert identity never shown
 *
 * QA Manager (any state):
 *   - All participants' answers shown per interaction
 *   - Expert identified with ★
 *   - Close Round button when open
 *   - Full results after closing
 */

import { useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAsync } from "../../hooks/useAsync";
import { getRoundDetail, submitAnswer, closeAndCompare } from "../../api/calibration";
import { useAuth } from "../../context/AuthContext";
import { COPC_CATEGORY_META } from "../../constants";

const ANSWERS = ["YES", "NO", "N/A"];

// ── Answer toggle ─────────────────────────────────────────────

function AnswerToggle({ value, onChange, disabled }) {
  return (
    <div className="flex gap-1.5">
      {ANSWERS.map((opt) => (
        <button
          key={opt}
          type="button"
          disabled={disabled}
          onClick={() => onChange(opt)}
          className={[
            "px-3 py-1.5 text-xs font-bold rounded-md border transition-all",
            disabled ? "cursor-not-allowed opacity-60" : "cursor-pointer",
            value === opt
              ? opt === "YES" ? "bg-success-surface text-success-on border-transparent"
                : opt === "NO" ? "bg-error-surface text-error-on border-transparent"
                : "bg-bg-tertiary text-text-sec border-transparent"
              : "border-border-sec text-text-ter hover:border-border-pri",
          ].join(" ")}
        >
          {opt}
        </button>
      ))}
    </div>
  );
}

// ── Answer pill (read-only) ───────────────────────────────────

function AnswerPill({ answer }) {
  if (!answer) return <span className="text-text-ter text-xs italic">No answer</span>;
  const cls = answer === "YES" ? "bg-success-surface text-success-on"
            : answer === "NO"  ? "bg-error-surface text-error-on"
            : "bg-bg-tertiary text-text-sec";
  return (
    <span className={`text-xs font-bold px-2.5 py-1 rounded-md uppercase ${cls}`}>
      {answer}
    </span>
  );
}

// ── Page ──────────────────────────────────────────────────────

export default function CalibrationDetailPage() {
  const { id }    = useParams();
  const navigate  = useNavigate();
  const { user }  = useAuth();

  const [submitting,    setSubmitting]    = useState({});   // groupId → bool
  const [submitErrors,  setSubmitErrors]  = useState({});   // groupId → string
  const [localAnswers,  setLocalAnswers]  = useState({});   // groupId → answer (before submit)
  const [closing,       setClosing]       = useState(false);
  const [closeError,    setCloseError]    = useState(null);

  const { data: round, loading, error, refetch } = useAsync(
    () => getRoundDetail(Number(id)),
    [id]
  );

  // ── Derived state ─────────────────────────────────────────

  const isOpen    = round?.isOpen ?? true;
  const groups    = round?.groups ?? [];
  const participants = round?.participants ?? [];

  // Manager if any participant has isExpert !== null (backend only sets this for managers)
  const isManager = participants.some(p => p.isExpert !== null && p.isExpert !== undefined);

  // For participants: find own session in each group
  const getOwnSession = (group) =>
    group.sessions?.find(s => s.userId === user?.userId);

  const hasAnsweredGroup = (group) => !!getOwnSession(group)?.calibrationAnswer;
  const hasAnsweredAll   = groups.length > 0 && groups.every(hasAnsweredGroup);

  // ── Handlers ──────────────────────────────────────────────

  const handleSubmitAnswer = useCallback(async (groupId, answer) => {
    setSubmitting(prev => ({ ...prev, [groupId]: true }));
    setSubmitErrors(prev => ({ ...prev, [groupId]: null }));
    try {
      await submitAnswer(groupId, answer);
      setLocalAnswers(prev => ({ ...prev, [groupId]: null }));
      refetch();
    } catch (err) {
      setSubmitErrors(prev => ({ ...prev, [groupId]: err.message }));
    } finally {
      setSubmitting(prev => ({ ...prev, [groupId]: false }));
    }
  }, [refetch]);

  const handleClose = async () => {
    setClosing(true);
    setCloseError(null);
    try {
      await closeAndCompare(Number(id));
      refetch();
    } catch (err) {
      setCloseError(err.message);
    } finally {
      setClosing(false);
    }
  };

  // ── Loading / error states ────────────────────────────────

  if (loading) return (
    <div className="p-8 flex items-center gap-3 text-text-ter">
      <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
      Loading calibration round…
    </div>
  );

  if (error) return <div className="p-8 text-error-on">{error}</div>;
  if (!round) return null;

  const catMeta = COPC_CATEGORY_META[round.category];

  // ── Render ────────────────────────────────────────────────

  return (
    <div className="max-w-[760px] mx-auto px-8 py-10">

      {/* Header */}
      <header className="mb-8">
        <button onClick={() => navigate("/calibration")}
          className="text-lsg-blue hover:text-lsg-blue-dark text-sm font-medium mb-4 flex items-center gap-1 transition-colors">
          ← Back to Calibration
        </button>

        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <div className="flex items-center gap-2.5 flex-wrap mb-1">
              <span className="font-mono text-base font-bold text-text-pri tracking-wide">
                {round.roundName}
              </span>
              {isOpen ? (
                <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-warning-surface text-warning-text">
                  Open
                </span>
              ) : round.isCalibrated ? (
                <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-success-surface text-success-on">
                  Calibrated
                </span>
              ) : (
                <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full bg-error-surface text-error-on">
                  Needs Calibration
                </span>
              )}
            </div>
            <p className="text-xs text-text-ter">
              {round.clientName} · {round.protocolName}
            </p>
          </div>

          {/* Close Round button — manager only, open rounds only */}
          {isManager && isOpen && (
            <button
              onClick={handleClose}
              disabled={closing}
              className="px-4 py-2 text-sm font-bold text-white bg-lsg-navy hover:bg-lsg-midnight rounded-md transition-colors disabled:opacity-50 whitespace-nowrap shrink-0"
            >
              {closing ? "Closing…" : "Close Round"}
            </button>
          )}
        </div>

        {closeError && (
          <p className="mt-2 text-xs text-error-on bg-error-surface px-3 py-2 rounded-lg">
            {closeError}
          </p>
        )}
      </header>

      {/* Question */}
      <section className="bg-bg-primary border border-border-sec rounded-xl p-5 mb-6 shadow-card">
        <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-2">
          Question being calibrated
        </p>
        {catMeta && (
          <span
            className="inline-block text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full mb-2"
            style={{
              background: `var(${catMeta.bgVar})`,
              color:      `var(${catMeta.textVar})`,
            }}
          >
            {round.category}
          </span>
        )}
        <p className="text-base font-semibold text-text-pri leading-snug">
          {round.questionText}
        </p>
      </section>

      {/* All-answered banner for participants */}
      {!isManager && isOpen && hasAnsweredAll && (
        <div className="mb-6 px-4 py-3 rounded-xl bg-success-surface border border-success-dot/20 text-success-on text-sm font-medium flex items-center gap-2">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <circle cx="8" cy="8" r="7" stroke="currentColor" strokeWidth="1.5"/>
            <path d="M4.5 8l2.5 2.5 4.5-4.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          All interactions answered — waiting for the round to be closed.
        </div>
      )}

      {/* Interactions */}
      <div className="flex flex-col gap-4">
        {groups.map((group, idx) => {
          const ownSession    = getOwnSession(group);
          const alreadyAnswered = !!ownSession?.calibrationAnswer;
          const localAnswer   = localAnswers[group.groupId];
          const isSubmitting  = submitting[group.groupId];
          const submitError   = submitErrors[group.groupId];

          return (
            <div key={group.groupId}
              className="bg-bg-primary border border-border-sec rounded-xl shadow-card overflow-hidden">

              {/* Interaction header */}
              <div className="flex items-center justify-between px-5 py-3 border-b border-border-ter bg-bg-secondary/40">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-mono font-bold text-text-ter">
                    #{String(idx + 1).padStart(2, "0")}
                  </span>
                  <span className="text-sm font-semibold text-text-pri font-mono">
                    {group.interactionId}
                  </span>
                </div>

                {/* Group result badge — closed rounds only */}
                {!isOpen && group.isCalibrated === false && (
                  <span className="text-[10px] font-bold text-warning-text bg-warning-surface px-2 py-0.5 rounded-full">
                    Needs Calibration
                  </span>
                )}
              </div>

              <div className="p-5">

                {/* ── Participant view ─────────────────────── */}
                {!isManager && (
                  <>
                    {/* Open + not yet answered */}
                    {isOpen && !alreadyAnswered && (
                      <div className="flex flex-col gap-3">
                        <p className="text-xs text-text-ter">Select your answer for this interaction:</p>
                        <AnswerToggle
                          value={localAnswer ?? null}
                          onChange={(val) => setLocalAnswers(prev => ({ ...prev, [group.groupId]: val }))}
                          disabled={isSubmitting}
                        />
                        {submitError && (
                          <p className="text-xs text-error-on">{submitError}</p>
                        )}
                        <button
                          onClick={() => handleSubmitAnswer(group.groupId, localAnswer)}
                          disabled={!localAnswer || isSubmitting}
                          className="self-start px-4 py-1.5 text-xs font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                        >
                          {isSubmitting ? "Submitting…" : "Submit Answer"}
                        </button>
                      </div>
                    )}

                    {/* Open + already answered */}
                    {isOpen && alreadyAnswered && (
                      <div className="flex items-center gap-3">
                        <span className="text-xs text-text-ter">Your answer:</span>
                        <AnswerPill answer={ownSession.calibrationAnswer} />
                        <span className="text-xs text-success-on font-medium">✓ Submitted</span>
                      </div>
                    )}

                    {/* Closed — show side-by-side comparison */}
                    {!isOpen && (
                      <div className="flex flex-col gap-3">
                        <div className="grid grid-cols-2 gap-4">
                          <div className="flex flex-col gap-1.5">
                            <span className="text-[10px] font-bold text-text-ter uppercase tracking-wider">
                              Your Answer
                            </span>
                            <AnswerPill answer={ownSession?.calibrationAnswer} />
                          </div>
                          <div className="flex flex-col gap-1.5">
                            <span className="text-[10px] font-bold text-text-ter uppercase tracking-wider">
                              Expert Answer
                            </span>
                            <AnswerPill answer={group.expertAnswer} />
                          </div>
                        </div>

                        {/* Needs Calibration — only shown when answers differ */}
                        {ownSession?.calibrationAnswer &&
                         group.expertAnswer &&
                         ownSession.calibrationAnswer !== group.expertAnswer && (
                          <p className="text-xs font-bold text-warning-text bg-warning-surface px-3 py-1.5 rounded-lg self-start">
                            Needs Calibration
                          </p>
                        )}
                      </div>
                    )}
                  </>
                )}

                {/* ── Manager view ─────────────────────────── */}
                {isManager && (
                  <div className="flex flex-col gap-3">

                    {/* Expert answer (shown after close) */}
                    {!isOpen && group.expertAnswer && (
                      <div className="flex items-center gap-2 pb-3 border-b border-border-ter">
                        <span className="text-[10px] font-bold text-text-ter uppercase tracking-wider">
                          Expert Answer ★
                        </span>
                        <AnswerPill answer={group.expertAnswer} />
                      </div>
                    )}

                    {/* All participants */}
                    <div className="flex flex-col gap-2">
                      {participants.map((participant) => {
                        const session = group.sessions?.find(
                          s => s.userId === participant.userId
                        );
                        const isExpert  = participant.isExpert === true;
                        const needsCal  = !isOpen &&
                                          session?.calibrationAnswer &&
                                          group.expertAnswer &&
                                          session.calibrationAnswer !== group.expertAnswer &&
                                          !isExpert;

                        return (
                          <div key={participant.userId}
                            className="flex items-center justify-between py-1.5 px-3 rounded-lg bg-bg-secondary/40">
                            <div className="flex items-center gap-2">
                              <span className="text-sm text-text-pri">
                                {participant.fullName}
                                {isExpert && (
                                  <span className="ml-1.5 text-[10px] text-lsg-blue font-bold">★ Expert</span>
                                )}
                              </span>
                              <span className="text-[10px] text-text-ter">{participant.roleName}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              {session?.calibrationAnswer ? (
                                <AnswerPill answer={session.calibrationAnswer} />
                              ) : (
                                <span className="text-xs text-text-ter italic">No answer</span>
                              )}
                              {needsCal && (
                                <span className="text-[10px] font-bold text-warning-text bg-warning-surface px-2 py-0.5 rounded-full">
                                  Needs Calibration
                                </span>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Participants summary — manager, open round */}
      {isManager && isOpen && participants.length > 0 && (
        <section className="mt-6 bg-bg-primary border border-border-sec rounded-xl p-5 shadow-card">
          <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-3">
            Completion Status
          </p>
          <div className="flex flex-col gap-2">
            {participants.map(p => (
              <div key={p.userId} className="flex items-center justify-between">
                <span className="text-sm text-text-pri">
                  {p.fullName}
                  {p.isExpert && <span className="ml-1.5 text-[10px] text-lsg-blue font-bold">★</span>}
                </span>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-text-ter tabular-nums">
                    {p.answeredCount}/{p.totalCount}
                  </span>
                  {p.hasAnsweredAll ? (
                    <span className="text-[10px] font-bold text-success-on bg-success-surface px-2 py-0.5 rounded-full">
                      Done
                    </span>
                  ) : (
                    <span className="text-[10px] font-bold text-text-ter bg-bg-tertiary px-2 py-0.5 rounded-full">
                      Pending
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
