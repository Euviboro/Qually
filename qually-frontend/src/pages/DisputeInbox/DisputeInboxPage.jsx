/**
 * @module pages/DisputeInbox/DisputeInboxPage
 *
 * Displays all sessions with status DISPUTED, scoped by the current user's
 * client access. QA users see all disputed sessions. OPERATIONS users only
 * see sessions for their assigned clients (the backend enforces this via the
 * X-User-Id header).
 */

import { useNavigate } from "react-router-dom";
import { useAsync } from "../../hooks/useAsync";
import { getSessions } from "../../api/sessions";
import { useAuth } from "../../context/AuthContext";

export default function DisputeInboxPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const { data: sessions, loading, error } = useAsync(
    () => getSessions({ auditStatus: "DISPUTED" }),
    []
  );

  return (
    <div className="max-w-[900px] mx-auto px-8 py-10">
      <header className="mb-8">
        <h1 className="text-2xl font-bold text-text-pri tracking-tight">Dispute Inbox</h1>
        <p className="text-text-ter text-sm mt-1">
          Sessions pending QA review ·{" "}
          {user?.department === "QA" ? "All clients" : "Your clients only"}
        </p>
      </header>

      {loading && (
        <div className="flex items-center gap-3 text-text-ter p-6">
          <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
          Loading disputes…
        </div>
      )}

      {error && (
        <div className="p-4 text-error-on bg-error-surface rounded-lg">{error}</div>
      )}

      {!loading && !error && sessions?.length === 0 && (
        <div className="text-center py-20 bg-bg-primary rounded-xl border-2 border-dashed border-border-sec">
          <p className="text-text-ter font-medium">No disputed sessions</p>
          <p className="text-text-ter text-sm mt-1">
            All audits are currently up to date.
          </p>
        </div>
      )}

      {!loading && !error && sessions?.length > 0 && (
        <div className="flex flex-col gap-4">
          {sessions.map((s) => (
            <div
              key={s.sessionId}
              className="bg-bg-primary border border-error-surface border-l-4 border-l-[var(--color-error)] rounded-xl p-5 shadow-card"
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-semibold text-text-pri">{s.protocolName}</p>
                  <p className="text-sm text-text-ter mt-0.5">
                    {s.clientName} · Member: {s.memberAudited} · Auditor: {s.auditorName}
                  </p>
                  <p className="text-xs text-text-ter mt-0.5">
                    Session #{s.sessionId} · {new Date(s.startedAt).toLocaleDateString()}
                  </p>
                </div>
                <button
                  onClick={() => navigate(`/sessions/${s.sessionId}`)}
                  className="shrink-0 px-4 py-2 text-xs font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors"
                >
                  Review →
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
