/**
 * @module pages/LogSession/LogSessionPage
 *
 * New in this version:
 * - Resume flow: reads location.state?.session (a SessionResumeDTO) and passes
 *   it to useLogSession so all fields and answers are pre-populated.
 * - Post-save confirmation modal: after Save Draft shows "Draft saved — Go to Drafts".
 *   After Submit shows "Session submitted — View Results".
 */

import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useLogSession } from "../../hooks/useLogSession";
import { useAuth } from "../../context/AuthContext";
import { getAuditableUsers } from "../../api/users";
import { getLobs } from "../../api/lobs";
import { COPC_CATEGORY_META, ANSWERS } from "../../constants";
import { SearchableSelect } from "../../components/ui/SearchableSelect";

// ── Sub-components ────────────────────────────────────────────

function CategoryPill({ category }) {
  const meta = COPC_CATEGORY_META[category];
  if (!meta) return (
    <span className="text-[10px] font-bold text-text-ter uppercase tracking-widest px-2 py-0.5 rounded-full bg-bg-tertiary">
      {category}
    </span>
  );
  return (
    <span
      style={{ background: `var(${meta.bgVar})`, color: `var(${meta.textVar})` }}
      className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
    >
      {meta.label}
    </span>
  );
}

function AnswerToggle({ value, onChange }) {
  return (
    <div className="flex gap-1.5 shrink-0">
      {[ANSWERS.YES, ANSWERS.NO, ANSWERS.NA].map((opt) => (
        <button key={opt} type="button" onClick={() => onChange(opt)}
          className={[
            "px-3 py-1.5 text-xs font-bold rounded-md border transition-all",
            value === opt
              ? opt === ANSWERS.YES ? "bg-success-surface text-success-on border-transparent"
                : opt === ANSWERS.NO ? "bg-error-surface text-error-on border-transparent"
                : "bg-bg-tertiary text-text-sec border-transparent"
              : "border-border-sec text-text-ter hover:border-border-pri",
          ].join(" ")}>
          {opt}
        </button>
      ))}
    </div>
  );
}

function SubattributePanel({ subattributes, selectedOptions, onOptionSelect }) {
  if (!subattributes || subattributes.length === 0) return null;
  return (
    <div className="mt-4 pt-4 border-t border-border-ter space-y-5">
      <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Sub-criteria</p>
      {subattributes.map((sub) => (
        <div key={sub.subattributeId} className="pl-4 border-l-2 border-border-ter">
          <p className="text-sm font-medium text-text-sec mb-3">{sub.subattributeText}</p>
          <div className="flex flex-wrap gap-2">
            {sub.subattributeOptions?.map((opt) => {
              const isSelected = opt.subattributeOptionId === selectedOptions[sub.subattributeId];
              return (
                <button
                  key={opt.subattributeOptionId}
                  type="button"
                  onClick={() => onOptionSelect(sub.subattributeId, opt.subattributeOptionId)}
                  className={[
                    "px-3.5 py-1.5 text-[11px] font-bold rounded-full border transition-all uppercase tracking-wide",
                    isSelected
                      ? "bg-lsg-blue text-white border-transparent"
                      : "border-border-sec text-text-ter hover:border-lsg-blue hover:text-lsg-blue",
                  ].join(" ")}
                >
                  {opt.optionLabel}
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}

function ProgressBar({ answered, total }) {
  const pct = total > 0 ? Math.round((answered / total) * 100) : 0;
  return (
    <div className="flex items-center gap-3">
      <div className="flex-1 h-1.5 bg-bg-tertiary rounded-full overflow-hidden">
        <div className="h-full bg-lsg-blue rounded-full transition-all duration-300" style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs font-bold text-text-ter tabular-nums shrink-0">{answered} / {total}</span>
    </div>
  );
}

// ── Confirm Modal ─────────────────────────────────────────────

function ConfirmModal({ status, sessionId, onClose, navigate }) {
  const isDraft = status === "DRAFT";
  return (
    <div className="fixed inset-0 z-[300] flex items-center justify-center p-4 bg-[rgba(0,20,50,0.4)] backdrop-blur-[2px]">
      <div className="w-full max-w-[400px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-7 flex flex-col gap-6 text-center">
        <div className="flex flex-col items-center gap-3">
          <div className={`w-12 h-12 rounded-full flex items-center justify-center ${isDraft ? "bg-warning-surface" : "bg-success-surface"}`}>
            {isDraft ? (
              <svg width="22" height="22" viewBox="0 0 22 22" fill="none" stroke="var(--color-warning-text)" strokeWidth="2" strokeLinecap="round">
                <path d="M19 3H5a2 2 0 00-2 2v14l4-4h12a2 2 0 002-2V5a2 2 0 00-2-2z"/>
              </svg>
            ) : (
              <svg width="22" height="22" viewBox="0 0 22 22" fill="none" stroke="var(--color-success-text)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            )}
          </div>
          <h2 className="text-lg font-bold text-text-pri">
            {isDraft ? "Draft saved" : "Session submitted"}
          </h2>
          <p className="text-sm text-text-ter">
            {isDraft
              ? "Your progress has been saved. You can resume this session from your drafts list."
              : "The session has been recorded and scores have been calculated."}
          </p>
        </div>
        <div className="flex flex-col gap-2">
          <button
            onClick={() => {
              if (isDraft) {
                navigate("/drafts");
              } else {
                navigate(`/sessions/${sessionId}`);
              }
            }}
            className="w-full px-5 py-2.5 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors"
          >
            {isDraft ? "Go to Drafts" : "View Results"}
          </button>
          <button
            onClick={onClose}
            className="w-full px-5 py-2 text-sm font-medium text-text-sec hover:text-text-pri transition-colors"
          >
            {isDraft ? "Keep editing" : "Back to Dashboard"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────

export default function LogSessionPage() {
  const location = useLocation();
  const navigate  = useNavigate();
  const { user }  = useAuth();

  const protocol        = location.state?.protocol ?? null;
  // existingSession is a SessionResumeDTO injected when navigating from DraftsPage
  const existingSession = location.state?.session  ?? null;

  const [auditableUsers,   setAuditableUsers]   = useState([]);
  const [lobs,             setLobs]             = useState([]);
  const [auditableLoading, setAuditableLoading] = useState(false);
  const [lobsLoading,      setLobsLoading]      = useState(false);

  useEffect(() => {
    if (!protocol?.clientId) return;
    setAuditableLoading(true);
    setLobsLoading(true);
    Promise.all([
      getAuditableUsers(protocol.clientId),
      getLobs(protocol.clientId),
    ]).then(([auditable, lobList]) => {
      setAuditableUsers(auditable.filter((u) => u.userId !== user?.userId));
      setLobs(lobList);
    }).finally(() => {
      setAuditableLoading(false);
      setLobsLoading(false);
    });
  }, [protocol?.clientId, user?.userId]);

  const {
    meta, setMeta,
    answers, setAnswer, setSubattributeAnswer,
    isQuestionExpanded,
    answeredCount, totalCount, isComplete,
    canDraft, canSubmit, missingFields,
    commentsRequired,
    saving, saveError, sessionId, savedStatus,
    saveDraft, submitSession,
  } = useLogSession(protocol, user?.userId, existingSession);

  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [showConfirm,     setShowConfirm]     = useState(false);
  const [confirmedStatus, setConfirmedStatus] = useState(null);
  const [confirmedId,     setConfirmedId]     = useState(null);

  // Show confirm modal when save/submit resolves
  useEffect(() => {
    if (savedStatus && !saving) {
      setConfirmedStatus(savedStatus);
      setConfirmedId(sessionId);
      setShowConfirm(true);
    }
  }, [savedStatus, saving, sessionId]);

  const handleSubmitClick = () => {
    if (!canSubmit) { setSubmitAttempted(true); return; }
    setSubmitAttempted(false);
    submitSession();
  };

  const handleDraftClick = () => {
    saveDraft();
  };

  if (!protocol) {
    return (
      <div className="flex flex-col items-center justify-center h-[60vh] gap-4 px-6 text-center">
        <p className="text-text-pri font-bold text-lg">No session selected</p>
        <p className="text-text-ter text-sm">Use the "Log session" button to choose a client and protocol first.</p>
        <button onClick={() => navigate("/")} className="mt-2 px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors">
          ← Back to Dashboard
        </button>
      </div>
    );
  }

  const questions     = protocol.auditQuestions ?? [];
  const memberOptions = auditableUsers.map((u) => ({ value: u.userId, label: `${u.fullName} — ${u.roleName ?? ""}` }));
  const lobOptions    = lobs.map((l) => ({ value: l.lobId, label: l.lobName }));

  return (
    <div className="max-w-[860px] mx-auto px-8 py-10">

      {/* Header */}
      <header className="mb-8">
        <button onClick={() => navigate(-1)} className="text-lsg-blue hover:text-lsg-blue-dark text-sm font-medium mb-4 flex items-center gap-1 transition-colors">
          ← Back
        </button>
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-text-pri tracking-tight">{protocol.protocolName}</h1>
            <p className="text-text-ter text-sm mt-0.5">
              {protocol.clientName} · {totalCount} question{totalCount !== 1 ? "s" : ""}
              {existingSession && <span className="ml-2 text-warning-text font-medium">Resuming draft</span>}
            </p>
          </div>
        </div>
      </header>

      {/* Session Details */}
      <section className="bg-bg-primary border border-border-sec rounded-xl p-6 mb-6 shadow-card">
        <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-5">Session Details</h2>
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Interaction ID <span className="text-lsg-blue">*</span>
            </label>
            <input
              type="text"
              value={meta.interactionId}
              onChange={(e) => setMeta("interactionId", e.target.value)}
              placeholder="e.g. CALL-2024-00123"
              className="w-full px-3 py-2 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Line of Business <span className="text-lsg-blue">*</span>
            </label>
            <SearchableSelect
              options={lobOptions}
              value={meta.lobId}
              onChange={(val) => setMeta("lobId", val)}
              placeholder="Select LOB…"
              loading={lobsLoading}
              emptyMessage="No LOBs found for this client"
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">Auditor</label>
            <div className="px-3 py-2 text-sm rounded-md border border-border-ter bg-bg-secondary text-text-sec">
              {user?.fullName ?? "—"}
              <span className="text-text-ter ml-2 text-xs">({user?.roleName})</span>
            </div>
          </div>
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Member Audited <span className="text-lsg-blue">*</span>
            </label>
            <SearchableSelect
              options={memberOptions}
              value={meta.memberAuditedUserId}
              onChange={(val) => setMeta("memberAuditedUserId", val)}
              placeholder="Select member…"
              loading={auditableLoading}
              emptyMessage="No auditable members for this client"
            />
          </div>
        </div>
      </section>

      {/* Progress */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-2">
          <span className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Questions</span>
          <span className="text-xs text-text-ter">{isComplete ? "All answered" : `${totalCount - answeredCount} remaining`}</span>
        </div>
        <ProgressBar answered={answeredCount} total={totalCount} />
      </div>

      {/* Questions */}
      {questions.length === 0 ? (
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter">This protocol has no questions.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4 mb-6">
          {questions.map((q, index) => {
            const qAnswer = answers[q.questionId];
            const expanded = isQuestionExpanded(q.questionId);
            const answered = qAnswer?.answer !== null;
            const hasSubattributes = q.subattributes?.length > 0;
            return (
              <div
                key={q.questionId}
                className={[
                  "bg-bg-primary border rounded-xl shadow-card overflow-hidden transition-colors duration-200",
                  answered
                    ? qAnswer.answer === ANSWERS.YES ? "border-l-4 border-l-[var(--color-success-dot)] border-border-sec"
                      : qAnswer.answer === ANSWERS.NO ? "border-l-4 border-l-[var(--color-error)] border-border-sec"
                      : "border-border-sec"
                    : "border-border-sec",
                ].join(" ")}
              >
                <div className="p-6">
                  <div className="flex items-start gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex flex-wrap gap-2 items-center mb-2">
                        <span className="font-mono text-xs text-text-ter">#{String(index + 1).padStart(2, "0")}</span>
                        <CategoryPill category={q.category} />
                      </div>
                      <p className="text-base font-semibold text-text-pri leading-snug">{q.questionText}</p>
                    </div>
                    <AnswerToggle value={qAnswer?.answer ?? null} onChange={(val) => setAnswer(q.questionId, val)} />
                  </div>
                  {expanded && hasSubattributes && (
                    <SubattributePanel
                      subattributes={q.subattributes}
                      selectedOptions={qAnswer?.subattributes ?? {}}
                      onOptionSelect={(subId, optId) => setSubattributeAnswer(q.questionId, subId, optId)}
                    />
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Comments */}
      <section className="bg-bg-primary border border-border-sec rounded-xl p-6 mb-6 shadow-card">
        <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
          Comments
          {commentsRequired
            ? <span className="text-lsg-blue ml-1">*</span>
            : <span className="text-text-ter font-normal ml-1">(optional)</span>}
        </label>
        {commentsRequired && <p className="text-xs text-text-ter mb-2">Required because one or more questions were answered No.</p>}
        <textarea
          value={meta.comments}
          onChange={(e) => setMeta("comments", e.target.value)}
          rows={3}
          placeholder="Add context about this session, particularly for any No answers…"
          className={[
            "w-full px-3 py-2 text-sm rounded-md border bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:ring-3 focus:ring-lsg-blue/10 transition-all resize-none",
            commentsRequired && !meta.comments.trim()
              ? "border-error-on focus:border-error-on"
              : "border-border-sec focus:border-lsg-blue",
          ].join(" ")}
        />
      </section>

      {/* Footer */}
      {totalCount > 0 && (
        <footer className="sticky bottom-6 bg-bg-primary/90 backdrop-blur-md p-4 border border-border-ter rounded-2xl shadow-lg flex items-center justify-between gap-4">
          <div className="min-w-0 flex-1">
            {submitAttempted && missingFields.length > 0 ? (
              <div className="text-xs text-error-on">
                <span className="font-bold">You are missing: </span>
                {missingFields.join(", ")}
              </div>
            ) : (
              <div>
                <p className="text-sm font-semibold text-text-pri truncate">
                  {isComplete ? "All questions answered" : `${answeredCount} of ${totalCount} answered`}
                </p>
                {sessionId && <p className="text-xs text-text-ter mt-0.5">Session #{sessionId}</p>}
                {saveError && <p className="text-xs text-error-on mt-0.5 truncate">{saveError}</p>}
              </div>
            )}
          </div>
          <div className="flex gap-2 shrink-0">
            <button type="button" onClick={handleDraftClick} disabled={saving || !canDraft}
              className="px-4 py-2 text-sm font-bold text-text-sec border border-border-sec bg-bg-secondary hover:bg-bg-tertiary rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
              {saving && savedStatus !== "COMPLETED" ? "Saving…" : "Save Draft"}
            </button>
            <button type="button" onClick={handleSubmitClick} disabled={saving}
              className={["px-5 py-2 text-sm font-bold text-white rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed",
                canSubmit ? "bg-lsg-blue hover:bg-lsg-blue-dark" : "bg-lsg-blue/60 hover:bg-lsg-blue/70"].join(" ")}>
              {saving && savedStatus !== "DRAFT" ? "Submitting…" : "Submit Session"}
            </button>
          </div>
        </footer>
      )}

      {/* Confirm modal */}
      {showConfirm && confirmedStatus && (
        <ConfirmModal
          status={confirmedStatus}
          sessionId={confirmedId}
          onClose={() => {
            setShowConfirm(false);
            navigate(confirmedStatus === "DRAFT" ? "/drafts" : "/");
          }}
          navigate={navigate}
        />
      )}
    </div>
  );
}