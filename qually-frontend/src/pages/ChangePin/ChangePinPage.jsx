/**
 * @module pages/ChangePin/ChangePinPage
 *
 * Shown immediately after login when the user's forcePinChange flag is true.
 * The user cannot access the rest of the app until they set a new PIN.
 *
 * Calls POST /api/auth/change-pin with their current (temporary) PIN and
 * their chosen new PIN. On success the backend re-issues cookies with an
 * updated token, and this page redirects to the dashboard.
 *
 * Remove this page when Microsoft Auth replaces the PIN login flow.
 */

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { api } from "../../api/apiClient";

export default function ChangePinPage() {
  const { login } = useAuth();
  const navigate  = useNavigate();

  const [currentPin,    setCurrentPin]    = useState("");
  const [newPin,        setNewPin]        = useState("");
  const [confirmPin,    setConfirmPin]    = useState("");
  const [loading,       setLoading]       = useState(false);
  const [error,         setError]         = useState(null);

  const handleSubmit = async (e) => {
  e.preventDefault();
  setError(null);

  if (!currentPin || !newPin || !confirmPin) {
    setError("All fields are required.");
    return;
  }
  if (newPin !== confirmPin) {
    setError("New PIN and confirmation do not match.");
    return;
  }
  if (newPin === currentPin) {
    setError("New PIN must be different from your current PIN.");
    return;
  }
  if (newPin.length < 4) {
    setError("PIN must be at least 4 characters.");
    return;
  }

  setLoading(true);
  try {
    const data = await api.post("/auth/change-pin", { currentPin, newPin });
    login(data);
    navigate("/", { replace: true });
  } catch (err) {
    setError(err.message ?? "Failed to change PIN. Please try again.");
  } finally {
    setLoading(false);
  }
};

  return (
    <div className="min-h-screen bg-bg-secondary flex items-center justify-center p-6">
      <div className="w-full max-w-[420px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-8">

        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-warning-surface rounded-xl mb-4">
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none" stroke="var(--color-warning-text)" strokeWidth="2" strokeLinecap="round">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/>
              <path d="M12 8v4M12 16h.01"/>
            </svg>
          </div>
          <h1 className="text-xl font-bold text-text-pri tracking-tight">Change your PIN</h1>
          <p className="text-sm text-text-ter mt-1">
            You must set a new PIN before continuing.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Current PIN
            </label>
            <input
              type="password"
              value={currentPin}
              onChange={(e) => setCurrentPin(e.target.value)}
              placeholder="Your temporary PIN (first name)"
              autoComplete="current-password"
              autoFocus
              className="w-full px-3 py-2.5 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              New PIN
            </label>
            <input
              type="password"
              value={newPin}
              onChange={(e) => setNewPin(e.target.value)}
              placeholder="Choose a new PIN"
              autoComplete="new-password"
              className="w-full px-3 py-2.5 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Confirm New PIN
            </label>
            <input
              type="password"
              value={confirmPin}
              onChange={(e) => setConfirmPin(e.target.value)}
              placeholder="Repeat your new PIN"
              autoComplete="new-password"
              className="w-full px-3 py-2.5 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
            />
          </div>

          {error && (
            <p className="text-xs text-error-on bg-error-surface px-3 py-2 rounded-lg">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed mt-1"
          >
            {loading ? "Saving…" : "Set New PIN"}
          </button>
        </form>
      </div>
    </div>
  );
}