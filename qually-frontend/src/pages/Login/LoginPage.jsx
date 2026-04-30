/**
 * @module pages/Login/LoginPage
 *
 * Email + PIN login page.
 * Calls POST /api/auth/login, stores the UserResponseDTO in AuthContext,
 * then redirects to the dashboard or /change-pin if forcePinChange is true.
 */

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { usePeek } from "../../hooks/usePeek";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate  = useNavigate();

  const [email,    setEmail]    = useState("");
  const [pin,      setPin]      = useState("");
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState(null);
  const [peekPin, peekPinHandlers] = usePeek();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !pin.trim()) {
      setError("Email and PIN are required.");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch(`${import.meta.env.VITE_API_BASE}/auth/login`, {
        method:      "POST",
        credentials: "include",          // receive httpOnly cookies
        headers:     { "Content-Type": "application/json" },
        body:        JSON.stringify({ email: email.trim().toLowerCase(), pin }),
      });

      const data = await res.json();

      if (!res.ok) {
        setError(data.message ?? "Invalid email or PIN.");
        return;
      }

      // Store display data in AuthContext — tokens are in cookies
      login(data);

      // Redirect based on forcePinChange flag
      if (data.forcePinChange) {
        navigate("/change-pin", { replace: true });
      } else {
        navigate("/", { replace: true });
      }

    } catch {
      setError("Could not reach the server. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-bg-secondary flex items-center justify-center p-6">
      <div className="w-full max-w-[420px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-8">

        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-lsg-blue rounded-xl mb-4">
            <svg width="24" height="24" viewBox="0 0 16 16" fill="none">
              <path d="M3 13L3 6L9 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M7 13L13 10L13 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" opacity="0.7" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">Qually</h1>
          <p className="text-sm text-text-ter mt-1">by Lean Solutions Group</p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
              autoComplete="email"
              autoFocus
              className="w-full px-3 py-2.5 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              PIN
            </label>
            <div className="relative">
              <input
                type={peekPin ? "text" : "password"}
                value={pin}
                onChange={(e) => setPin(e.target.value)}
                placeholder="Enter your PIN"
                autoComplete="current-password"
                className="w-full px-3 py-2.5 pr-10 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
              />
              <button
                type="button"
                {...peekPinHandlers}
                tabIndex={-1}
                aria-label="Hold to reveal PIN"
                className="absolute right-3 top-1/2 -translate-y-1/2 text-text-ter hover:text-text-sec transition-colors select-none"
              >
                {peekPin ? (
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                ) : (
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                    <line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                )}
              </button>
            </div>
          </div>

          {error && (
            <p className="text-xs text-error-on bg-error-surface px-3 py-2 rounded-lg">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {loading ? "Signing in…" : "Sign In"}
          </button>
        </form>

        <p className="text-center text-xs text-text-ter mt-6">
          Your PIN is your first name. Change it after logging in.
        </p>
      </div>
    </div>
  );
}