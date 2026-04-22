/**
 * @module pages/LogSession/LogSessionPage
 *
 * Active audit session page.
 *
 * Schema-alignment changes from the previous version:
 * - Audit Logic Type selector removed — the scoring strategy is fixed on the
 *   protocol (`protocol.auditLogicType`). It is now shown as a read-only badge
 *   in the header so the auditor can see which mode applies.
 * - `memberAudited` field added to Session Details — the name or ID of the
 *   person whose work is being audited. Required by the DB.
 * - Auditor field changed from a free-text email input to a `SearchableSelect`
 *   that fetches all users and stores the selected `userId` (Integer).
 */

import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useLogSession } from "../../hooks/useLogSession";
import { getUsers } from "../../api/users";
import { COPC_CATEGORY_META, AUDIT_LOGIC_TYPE_META } from "../../constants";
import { SearchableSelect } from "../../components/ui/SearchableSelect";

// ── Presentational sub-components ────────────────────────────

/**
 * Coloured pill for the COPC category of a question.
 *
 * @param {Object} props
 * @param {string} props.category
 */
function CategoryPill({ category }) {
  const meta = COPC_CATEGORY_META[category];
  if (!meta) {
    return (
      <span className="text-[10px] font-bold text-text-ter uppercase tracking-widest px-2 py-0.5 rounded-full bg-bg-tertiary">
        {category}
      </span>
    );
  }
  return (
    <span
      style={{ background: `var(${meta.bgVar})`, color: `var(${meta.textVar})` }}
      className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
    >
      {meta.label}
    </span>
  );
}

/**
 * YES / NO binary toggle.
 *
 * @param {Object}                  props
 * @param {"YES"|"NO"|null}         props.value
 * @param {(v: "YES"|"NO") => void} props.onChange
 */
function YesNoToggle({ value, onChange }) {
  return (
    <div className="flex gap-1.5 shrink-0">
      <button
        type="button"
        onClick={() => onChange("YES")}
        className={[
          "px-4 py-1.5 text-xs font-bold rounded-md border transition-all",
          value === "YES"
            ? "bg-success-surface text-success-on border-transparent"
            : "border-border-sec text-text-ter hover:border-success-on hover:text-success-on",
        ].join(" ")}
      >
        YES
      </button>
      <button
        type="button"
        onClick={() => onChange("NO")}
        className={[
          "px-4 py-1.5 text-xs font-bold rounded-md border transition-all",
          value === "NO"
            ? "bg-error-surface text-error-on border-transparent"
            : "border-border-sec text-text-ter hover:border-error-on hover:text-error-on",
        ].join(" ")}
      >
        NO
      </button>
    </div>
  );
}

/**
 * Subattribute panel — visible only when the parent question is answered NO.
 *
 * @param {Object} props
 * @param {import('../../api/questions').SubattributeResponseDTO[]} props.subattributes
 * @param {Record<number, number|null>} props.selectedOptions
 * @param {(subattributeId: number, optionId: number) => void} props.onOptionSelect
 */
function SubattributePanel({ subattributes, selectedOptions, onOptionSelect }) {
  if (!subattributes || subattributes.length === 0) {
    return (
      <p className="mt-4 pt-4 border-t border-border-ter text-sm text-text-ter italic">
        No sub-criteria defined for this question.
      </p>
    );
  }
  return (
    <div className="mt-4 pt-4 border-t border-border-ter space-y-5">
      <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest">
        Sub-criteria
      </p>
      {subattributes.map((sub) => {
        const selectedOptionId = selectedOptions[sub.subattributeId] ?? null;
        return (
          <div key={sub.subattributeId} className="pl-4 border-l-2 border-border-ter">
            <p className="text-sm font-medium text-text-sec mb-3">{sub.subattributeText}</p>
            <div className="flex flex-wrap gap-2">
              {sub.subattributeOptions?.map((opt) => {
                const isSelected = opt.subattributeOptionId === selectedOptionId;
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
        );
      })}
    </div>
  );
}

/**
 * Thin horizontal progress bar.
 *
 * @param {Object} props
 * @param {number} props.answered
 * @param {number} props.total
 */
function ProgressBar({ answered, total }) {
  const pct = total > 0 ? Math.round((answered / total) * 100) : 0;
  return (
    <div className="flex items-center gap-3">
      <div className="flex-1 h-1.5 bg-bg-tertiary rounded-full overflow-hidden">
        <div
          className="h-full bg-lsg-blue rounded-full transition-all duration-300"
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="text-xs font-bold text-text-ter tabular-nums shrink-0">
        {answered} / {total}
      </span>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────

/**
 * Log Session page. Reads the protocol from React Router location state.
 *
 * @returns {JSX.Element}
 */
export default function LogSessionPage() {
  const location = useLocation();
  const navigate  = useNavigate();

  const protocol = location.state?.protocol ?? null;

  // ── User list for the auditor SearchableSelect ─────────────
  const [users,        setUsers]        = useState([]);
  const [usersLoading, setUsersLoading] = useState(true);

  useEffect(() => {
    getUsers()
      .then(setUsers)
      .catch(() => {})
      .finally(() => setUsersLoading(false));
  }, []);

  const userOptions = users.map((u) => ({
    value: u.userId,
    label: `${u.fullName} — ${u.userEmail}`,
  }));

  // ── Session state ──────────────────────────────────────────
  const {
    meta, setMeta,
    answers,
    setAnswer,
    setSubattributeAnswer,
    isQuestionExpanded,
    answeredCount,
    totalCount,
    isComplete,
    canSave,
    saving,
    saveError,
    sessionId,
    savedStatus,
    saveDraft,
    submitSession,
  } = useLogSession(protocol);

  // ── Guard: no protocol in state ────────────────────────────
  if (!protocol) {
    return (
      <div className="flex flex-col items-center justify-center h-[60vh] gap-4 px-6 text-center">
        <div className="w-14 h-14 rounded-full bg-bg-tertiary flex items-center justify-center">
          <svg className="w-7 h-7 text-text-ter" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
            <circle cx="12" cy="12" r="9"/>
            <path d="M12 8v4M12 16h.01" strokeLinecap="round"/>
          </svg>
        </div>
        <div>
          <p className="text-text-pri font-bold text-lg">No session selected</p>
          <p className="text-text-ter text-sm mt-1">
            Use the "Log session" button to choose a client and protocol first.
          </p>
        </div>
        <button
          onClick={() => navigate("/")}
          className="mt-2 px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors"
        >
          ← Back to Dashboard
        </button>
      </div>
    );
  }

  const questions  = protocol.auditQuestions ?? [];
  const logicMeta  = AUDIT_LOGIC_TYPE_META[protocol.auditLogicType];

  // ── Render ─────────────────────────────────────────────────

  return (
    <div className="max-w-[860px] mx-auto px-8 py-10">

      {/* ── Page header ─────────────────────────────────────── */}
      <header className="mb-8">
        <button
          onClick={() => navigate("/")}
          className="text-lsg-blue hover:text-lsg-blue-dark text-sm font-medium mb-4 flex items-center gap-1 transition-colors"
        >
          ← Back to Dashboard
        </button>
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-text-pri tracking-tight leading-snug">
              {protocol.protocolName}
            </h1>
            <div className="flex items-center flex-wrap gap-2 mt-1.5">
              <p className="text-text-ter text-sm">
                {protocol.clientName} · v{protocol.protocolVersion} · {totalCount} question{totalCount !== 1 ? "s" : ""}
              </p>
              {/* Read-only logic type badge — sourced from the protocol */}
              {logicMeta && (
                <span className="px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-widest rounded-full bg-bg-accent text-lsg-blue-dark border border-border-sec">
                  {logicMeta.label}
                </span>
              )}
            </div>
          </div>

          {savedStatus && (
            <span className={[
              "shrink-0 px-3 py-1.5 rounded-full text-xs font-bold border border-transparent",
              savedStatus === "COMPLETED"
                ? "bg-success-surface text-success-on"
                : "bg-warning-surface text-warning-on",
            ].join(" ")}>
              {savedStatus === "COMPLETED" ? "✓ Submitted" : "Draft saved"}
            </span>
          )}
        </div>
      </header>

      {/* ── Section 1: Session metadata ─────────────────────── */}
      <section className="bg-bg-primary border border-border-sec rounded-xl p-6 mb-6 shadow-card">
        <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-5">
          Session Details
        </h2>
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">

          {/* Interaction ID */}
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Interaction ID <span className="text-lsg-blue">*</span>
            </label>
            <input
              type="text"
              value={meta.interactionId}
              onChange={(e) => setMeta("interactionId", e.target.value)}
              placeholder="e.g. CALL-2024-00123"
              className="w-full px-3 py-2 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10 transition-all"
            />
          </div>

          {/* Member Audited — new required field from DB schema */}
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Member Audited <span className="text-lsg-blue">*</span>
            </label>
            <input
              type="text"
              value={meta.memberAudited}
              onChange={(e) => setMeta("memberAudited", e.target.value)}
              placeholder="Name or ID of the person being audited"
              className="w-full px-3 py-2 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10 transition-all"
            />
          </div>

          {/* Auditor — SearchableSelect keyed to user_id */}
          <div className="sm:col-span-2">
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Auditor <span className="text-lsg-blue">*</span>
            </label>
            <SearchableSelect
              options={userOptions}
              value={meta.auditorUserId}
              onChange={(val) => setMeta("auditorUserId", val)}
              placeholder="Select the conducting auditor…"
              searchPlaceholder="Search by name or email…"
              loading={usersLoading}
              emptyMessage="No users found"
            />
          </div>

          {/* Comments — full width */}
          <div className="sm:col-span-2">
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Comments
            </label>
            <textarea
              value={meta.comments}
              onChange={(e) => setMeta("comments", e.target.value)}
              rows={3}
              placeholder="Optional notes about this session…"
              className="w-full px-3 py-2 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10 transition-all resize-none"
            />
          </div>
        </div>
      </section>

      {/* ── Section 2: Progress ─────────────────────────────── */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-2">
          <span className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Questions</span>
          <span className="text-xs text-text-ter">
            {isComplete ? "All answered" : `${totalCount - answeredCount} remaining`}
          </span>
        </div>
        <ProgressBar answered={answeredCount} total={totalCount} />
      </div>

      {/* ── Section 3: Questions ────────────────────────────── */}
      {questions.length === 0 ? (
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter">This protocol has no questions.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {questions.map((q, index) => {
            const qAnswer  = answers[q.questionId];
            const expanded = isQuestionExpanded(q.questionId);
            const answered = qAnswer?.answer !== null;

            return (
              <div
                key={q.questionId}
                className={[
                  "bg-bg-primary border rounded-xl shadow-card overflow-hidden transition-colors duration-200",
                  answered
                    ? qAnswer.answer === "YES"
                      ? "border-l-4 border-l-[var(--color-success-dot)] border-border-sec"
                      : "border-l-4 border-l-[var(--color-error)] border-border-sec"
                    : "border-border-sec",
                ].join(" ")}
              >
                <div className="p-6">
                  <div className="flex items-start gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex flex-wrap gap-2 items-center mb-2">
                        <span className="font-mono text-xs text-text-ter">
                          #{String(index + 1).padStart(2, "0")}
                        </span>
                        <CategoryPill category={q.category} />
                      </div>
                      <p className="text-base font-semibold text-text-pri leading-snug">
                        {q.questionText}
                      </p>
                    </div>
                    <YesNoToggle
                      value={qAnswer?.answer ?? null}
                      onChange={(val) => setAnswer(q.questionId, val)}
                    />
                  </div>

                  {expanded && (
                    <SubattributePanel
                      subattributes={q.subattributes}
                      selectedOptions={qAnswer?.subattributes ?? {}}
                      onOptionSelect={(subId, optId) =>
                        setSubattributeAnswer(q.questionId, subId, optId)
                      }
                    />
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── Sticky footer ────────────────────────────────────── */}
      {totalCount > 0 && (
        <footer className="sticky bottom-6 mt-8 bg-bg-primary/90 backdrop-blur-md p-4 border border-border-ter rounded-2xl shadow-lg flex items-center justify-between gap-4">
          <div className="min-w-0">
            <p className="text-sm font-semibold text-text-pri truncate">
              {isComplete ? "All questions answered" : `${answeredCount} of ${totalCount} answered`}
            </p>
            {sessionId && (
              <p className="text-xs text-text-ter mt-0.5">Session #{sessionId}</p>
            )}
            {saveError && (
              <p className="text-xs text-error-on mt-0.5 truncate">{saveError}</p>
            )}
          </div>

          <div className="flex gap-2 shrink-0">
            <button
              type="button"
              onClick={saveDraft}
              disabled={saving || !canSave}
              className="px-4 py-2 text-sm font-bold text-text-sec border border-border-sec bg-bg-secondary hover:bg-bg-tertiary rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {saving && savedStatus !== "COMPLETED" ? "Saving…" : "Save Draft"}
            </button>
            <button
              type="button"
              onClick={submitSession}
              disabled={saving || !canSave || !isComplete}
              className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {saving && savedStatus !== "DRAFT" ? "Submitting…" : "Submit Session"}
            </button>
          </div>
        </footer>
      )}
    </div>
  );
}
