/**
 * @module pages/Drafts/DraftsPage
 *
 * Lists all DRAFT sessions visible to the current user.
 * Columns: Date, Client, Protocol, LOB, Interaction ID, Member Audited.
 * No scores (not calculated yet), no status column (they are all drafts).
 *
 * "Continue" navigates to /sessions/log with the protocol and resume
 * payload in router state, pre-populating all form fields and answers.
 */

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAsync } from "../../hooks/useAsync";
import { getDraftSessions, getSessionForResume } from "../../api/sessions";
import { getProtocolById } from "../../api/protocols";

const MONTHS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

function formatDate(raw) {
  if (!raw) return "—";
  const d = new Date(raw);
  if (isNaN(d)) return "—";
  return `${MONTHS[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
}

export default function DraftsPage() {
  const navigate = useNavigate();
  const [resumingId, setResumingId] = useState(null);
  const [resumeError, setResumeError] = useState(null);

  const { data: sessions, loading, error } = useAsync(
    () => getDraftSessions(),
    []
  );

  const drafts = sessions ?? [];

  /**
   * Loads the resume payload and the full protocol (with questions and
   * subattributes), then navigates to LogSessionPage with both in state.
   *
   * The full protocol is needed because useLogSession uses
   * protocol.auditQuestions to initialise the answers map — the session
   * DTO alone does not carry question definitions.
   */
  const handleContinue = async (session) => {
    setResumingId(session.sessionId);
    setResumeError(null);
    try {
      const [resumeData, protocol] = await Promise.all([
        getSessionForResume(session.sessionId),
        getProtocolById(session.protocolId),
      ]);
      navigate("/sessions/log", {
        state: { protocol, session: resumeData },
      });
    } catch (err) {
      setResumeError(`Failed to load draft #${session.sessionId}: ${err.message}`);
    } finally {
      setResumingId(null);
    }
  };

  if (loading) return (
    <div className="p-8 flex items-center gap-3 text-text-ter">
      <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
      Loading drafts…
    </div>
  );

  if (error) return <div className="p-8 text-error-on">{error}</div>;

  return (
    <div className="px-6 py-8 max-w-full">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-text-pri tracking-tight">Drafts</h1>
        <p className="text-text-ter text-sm mt-0.5">
          {drafts.length} saved draft{drafts.length !== 1 ? "s" : ""}
        </p>
      </header>

      {resumeError && (
        <div className="mb-4 px-4 py-3 rounded-lg bg-error-surface text-error-on text-sm">
          {resumeError}
        </div>
      )}

      {drafts.length === 0 ? (
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter font-medium">No drafts saved yet.</p>
          <p className="text-text-ter text-sm mt-1">
            Start a session and use "Save Draft" to continue it later.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-border-sec bg-bg-primary shadow-card">
          <table className="min-w-full text-sm border-collapse">
            <thead>
              <tr className="border-b border-border-ter bg-bg-secondary/50">
                {["Date", "Client", "Protocol", "LOB", "Interaction ID", "Member Audited"].map((label) => (
                  <th key={label} className="px-4 py-3 text-left">
                    <span className="text-[11px] font-bold text-text-sec uppercase tracking-wider">
                      {label}
                    </span>
                  </th>
                ))}
                <th className="px-4 py-3"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {drafts.map((s, idx) => (
                <tr
                  key={s.sessionId}
                  className={["border-b border-border-ter transition-colors hover:bg-bg-secondary/40",
                    idx % 2 === 0 ? "" : "bg-bg-secondary/20"].join(" ")}
                >
                  <td className="px-4 py-3 text-xs text-text-ter whitespace-nowrap tabular-nums">
                    {formatDate(s.startedAt)}
                  </td>
                  <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{s.clientName ?? "—"}</td>
                  <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap max-w-[160px] truncate" title={s.protocolName}>
                    {s.protocolName ?? "—"}
                  </td>
                  <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{s.lobName ?? "—"}</td>
                  <td className="px-4 py-3 text-xs text-text-ter font-mono whitespace-nowrap">
                    {s.interactionId && !s.interactionId.startsWith("DRAFT-")
                      ? s.interactionId
                      : "—"}
                  </td>
                  <td className="px-4 py-3 text-xs text-text-pri whitespace-nowrap">{s.memberAuditedName ?? "—"}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleContinue(s)}
                      disabled={resumingId === s.sessionId}
                      className="px-3 py-1.5 text-xs font-bold text-lsg-blue border border-lsg-blue rounded-md hover:bg-bg-accent transition-colors disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                    >
                      {resumingId === s.sessionId ? "Loading…" : "Continue →"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}