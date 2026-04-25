/**
 * @module components/ui/DisputeModal
 *
 * Two-step modal for the dispute flow:
 * - Step 1: flag a response (any OPERATIONS user with client access).
 * - Step 2: formally raise a dispute (Team Leader or above).
 *
 * The modal is opened from a question row on SessionResultsPage and receives
 * the response object so it can pre-display the question and original answer.
 */

import { useState, useEffect } from "react";
import { getDisputeReasons, flagResponse, raiseDispute } from "../../api/disputes";
import { SearchableSelect } from "./SearchableSelect";
import { useAuth } from "../../context/AuthContext";

/**
 * @param {Object}   props
 * @param {boolean}  props.isOpen
 * @param {import('../../api/sessions').AuditResponseResultDTO|null} props.response
 * @param {() => void} props.onClose
 * @param {() => void} props.onSuccess - Called after a successful flag or dispute action.
 */
export function DisputeModal({ isOpen, response, onClose, onSuccess }) {
  const { user } = useAuth();

  const [reasons,        setReasons]        = useState([]);
  const [reasonsLoading, setReasonsLoading] = useState(true);
  const [selectedReason, setSelectedReason] = useState(null);
  const [comment,        setComment]        = useState("");
  const [submitting,     setSubmitting]     = useState(false);
  const [error,          setError]          = useState(null);

  // Determine which action this user can perform
  const canFlag    = user?.department === "OPERATIONS";
  const canDispute = canFlag && user?.hierarchyLevel <= 6; // Team Leader or above

  // Is this response already flagged (so we go straight to dispute step)?
  const isFlagged = response?.responseStatus === "FLAGGED";

  useEffect(() => {
    if (!isOpen) return;
    setSelectedReason(null);
    setComment("");
    setError(null);
    if (canDispute) {
      getDisputeReasons()
        .then(setReasons)
        .catch(() => setError("Could not load dispute reasons."))
        .finally(() => setReasonsLoading(false));
    }
  }, [isOpen, canDispute]);

  if (!isOpen || !response) return null;

  const reasonOptions = reasons.map((r) => ({
    value: r.reasonId,
    label: r.reasonText,
  }));

  const handleFlag = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await flagResponse(response.responseId);
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDispute = async () => {
    if (!selectedReason) { setError("Please select a dispute reason."); return; }
    setSubmitting(true);
    setError(null);
    try {
      await raiseDispute({
        responseId:     response.responseId,
        reasonId:       selectedReason,
        disputeComment: comment.trim() || undefined,
      });
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-[rgba(0,20,50,0.4)] backdrop-blur-[2px]"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[480px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-7 flex flex-col gap-5"
      >
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-text-pri">
              {isFlagged ? "Raise Formal Dispute" : "Flag Response"}
            </h2>
            <p className="text-xs text-text-ter mt-0.5">
              {isFlagged
                ? "This response is flagged. Raise a formal dispute to escalate it to QA."
                : "Flag this response for Team Leader review."}
            </p>
          </div>
          <button onClick={onClose} className="text-text-ter hover:text-text-pri p-1 transition-colors">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>

        {/* Question context */}
        <div className="bg-bg-secondary rounded-lg p-4 border border-border-ter">
          <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-1">
            Question
          </p>
          <p className="text-sm text-text-pri font-medium">{response.questionText}</p>
          <div className="flex items-center gap-2 mt-2">
            <span className="text-[10px] text-text-ter uppercase tracking-widest">Original answer:</span>
            <span className={`text-xs font-bold px-2 py-0.5 rounded ${
              response.originalAnswer === "YES"
                ? "bg-success-surface text-success-on"
                : response.originalAnswer === "NO"
                  ? "bg-error-surface text-error-on"
                  : "bg-bg-tertiary text-text-ter"
            }`}>
              {response.originalAnswer}
            </span>
          </div>
        </div>

        {/* Dispute reason — only shown for TL+ on the formal dispute step */}
        {(isFlagged && canDispute) && (
          <>
            <div>
              <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-2">
                Dispute Reason <span className="text-lsg-blue">*</span>
              </label>
              <SearchableSelect
                options={reasonOptions}
                value={selectedReason}
                onChange={setSelectedReason}
                placeholder="Select a reason…"
                loading={reasonsLoading}
                emptyMessage="No reasons available"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-2">
                Comment <span className="text-text-ter font-normal">(optional)</span>
              </label>
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                rows={3}
                placeholder="Add context about why this response is being disputed…"
                className="w-full px-3 py-2 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10 transition-all resize-none"
              />
            </div>
          </>
        )}

        {error && (
          <p className="text-xs text-error-on bg-error-surface px-3 py-2 rounded-lg">{error}</p>
        )}

        {/* Footer */}
        <div className="flex justify-end gap-3 pt-1">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-text-sec border border-border-sec rounded-md hover:bg-bg-secondary transition-colors"
          >
            Cancel
          </button>

          {/* Flag step */}
          {!isFlagged && canFlag && (
            <button
              onClick={handleFlag}
              disabled={submitting}
              className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40"
            >
              {submitting ? "Flagging…" : "Flag Response"}
            </button>
          )}

          {/* Dispute step */}
          {isFlagged && canDispute && (
            <button
              onClick={handleDispute}
              disabled={submitting || !selectedReason}
              className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {submitting ? "Raising dispute…" : "Raise Dispute"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
