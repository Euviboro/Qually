/**
 * @module components/ui/ResolveDisputeModal
 *
 * Modal for QA users to resolve an open dispute.
 * Shows the original answer and dispute context, then lets the resolver
 * select UNCHANGED (agree with original) or MODIFIED (override the answer).
 * When MODIFIED, a toggle shows the two valid alternative answers (i.e. the
 * two that are not the original answer).
 */

import { useState } from "react";
import { resolveDispute } from "../../api/disputes";

const ALL_ANSWERS = ["YES", "NO", "N/A"];

/**
 * @param {Object}   props
 * @param {boolean}  props.isOpen
 * @param {import('../../api/sessions').AuditResponseResultDTO|null} props.response
 * @param {() => void} props.onClose
 * @param {() => void} props.onSuccess
 */
export function ResolveDisputeModal({ isOpen, response, onClose, onSuccess }) {
  const [outcome,    setOutcome]    = useState(null);  // "UNCHANGED" | "MODIFIED"
  const [newAnswer,  setNewAnswer]  = useState(null);
  const [note,       setNote]       = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error,      setError]      = useState(null);

  if (!isOpen || !response?.dispute) return null;

  const { dispute, originalAnswer } = response;
  const alternativeAnswers = ALL_ANSWERS.filter((a) => a !== originalAnswer);

  const canSubmit = outcome !== null && (outcome === "UNCHANGED" || newAnswer !== null);

  const reset = () => {
    setOutcome(null);
    setNewAnswer(null);
    setNote("");
    setError(null);
  };

  const handleClose = () => { reset(); onClose(); };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await resolveDispute(dispute.disputeId, {
        resolutionOutcome: outcome,
        newAnswer:         outcome === "MODIFIED" ? newAnswer : undefined,
        resolutionNote:    note.trim() || undefined,
      });
      onSuccess();
      reset();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      onClick={handleClose}
      className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-[rgba(0,20,50,0.4)] backdrop-blur-[2px]"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[520px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-7 flex flex-col gap-5"
      >
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-text-pri">Resolve Dispute</h2>
            <p className="text-xs text-text-ter mt-0.5">Review the dispute and record your decision.</p>
          </div>
          <button onClick={handleClose} className="text-text-ter hover:text-text-pri p-1 transition-colors">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>

        {/* Question context */}
        <div className="bg-bg-secondary rounded-lg p-4 border border-border-ter space-y-2">
          <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Question</p>
          <p className="text-sm text-text-pri font-medium">{response.questionText}</p>
          <div className="flex items-center gap-3 pt-1">
            <div className="flex items-center gap-1.5">
              <span className="text-[10px] text-text-ter uppercase tracking-widest">Original:</span>
              <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                originalAnswer === "YES"
                  ? "bg-success-surface text-success-on"
                  : originalAnswer === "NO"
                    ? "bg-error-surface text-error-on"
                    : "bg-bg-tertiary text-text-ter"
              }`}>{originalAnswer}</span>
            </div>
          </div>
        </div>

        {/* Dispute context */}
        <div className="border border-border-ter rounded-lg p-4 space-y-1.5">
          <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Dispute Reason</p>
          <p className="text-sm text-text-pri">{dispute.reasonText}</p>
          {dispute.disputeComment && (
            <p className="text-xs text-text-sec italic mt-1">"{dispute.disputeComment}"</p>
          )}
          <p className="text-[10px] text-text-ter mt-1">
            Raised by {dispute.raisedByName} · {new Date(dispute.raisedAt).toLocaleDateString()}
          </p>
        </div>

        {/* Resolution choice */}
        <div>
          <p className="text-xs font-bold text-text-sec uppercase tracking-wider mb-3">
            Your Decision <span className="text-lsg-blue">*</span>
          </p>
          <div className="flex gap-3">
            <button
              onClick={() => { setOutcome("UNCHANGED"); setNewAnswer(null); }}
              className={[
                "flex-1 py-3 rounded-lg border-2 text-sm font-bold transition-all",
                outcome === "UNCHANGED"
                  ? "border-success-on bg-success-surface text-success-on"
                  : "border-border-sec text-text-ter hover:border-border-pri",
              ].join(" ")}
            >
              Unchanged
              <p className="text-[11px] font-normal mt-0.5">Original answer stands</p>
            </button>
            <button
              onClick={() => setOutcome("MODIFIED")}
              className={[
                "flex-1 py-3 rounded-lg border-2 text-sm font-bold transition-all",
                outcome === "MODIFIED"
                  ? "border-lsg-blue bg-bg-accent text-lsg-blue-dark"
                  : "border-border-sec text-text-ter hover:border-border-pri",
              ].join(" ")}
            >
              Modified
              <p className="text-[11px] font-normal mt-0.5">Override with new answer</p>
            </button>
          </div>
        </div>

        {/* New answer — only shown when MODIFIED */}
        {outcome === "MODIFIED" && (
          <div>
            <p className="text-xs font-bold text-text-sec uppercase tracking-wider mb-2">
              New Answer <span className="text-lsg-blue">*</span>
            </p>
            <div className="flex gap-2">
              {alternativeAnswers.map((ans) => (
                <button
                  key={ans}
                  onClick={() => setNewAnswer(ans)}
                  className={[
                    "flex-1 py-2 rounded-md border-2 text-xs font-bold transition-all uppercase",
                    newAnswer === ans
                      ? ans === "YES"
                        ? "border-success-on bg-success-surface text-success-on"
                        : ans === "NO"
                          ? "border-error-on bg-error-surface text-error-on"
                          : "border-lsg-blue bg-bg-accent text-lsg-blue-dark"
                      : "border-border-sec text-text-ter hover:border-border-pri",
                  ].join(" ")}
                >
                  {ans}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Resolution note */}
        <div>
          <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-2">
            Resolution Note <span className="text-text-ter font-normal">(optional)</span>
          </label>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            rows={2}
            placeholder="Explain your resolution decision…"
            className="w-full px-3 py-2 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10 transition-all resize-none"
          />
        </div>

        {error && (
          <p className="text-xs text-error-on bg-error-surface px-3 py-2 rounded-lg">{error}</p>
        )}

        {/* Footer */}
        <div className="flex justify-end gap-3 pt-1">
          <button
            onClick={handleClose}
            className="px-4 py-2 text-sm font-medium text-text-sec border border-border-sec rounded-md hover:bg-bg-secondary transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={submitting || !canSubmit}
            className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {submitting ? "Saving…" : "Save Resolution"}
          </button>
        </div>
      </div>
    </div>
  );
}
