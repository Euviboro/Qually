/**
 * @module context/AuthContext
 *
 * Authentication context for Qually — OAuth2 / Microsoft Entra ID edition.
 *
 * On startup the context calls GET /api/auth/me to check whether the browser
 * already has a valid access_token cookie (set by the backend after OAuth2 login).
 * If the cookie is present and valid, the user is hydrated silently. If not,
 * any protected route will redirect to /login.
 *
 * Login is initiated by navigating to /oauth2/authorization/azure — the browser
 * goes to Microsoft, authenticates, and the backend sets the cookie on return.
 * No credentials are handled in JavaScript.
 *
 * Logout calls POST /api/auth/logout to clear the httpOnly cookies, then
 * redirects to /login. The context also listens for the "qually:logout" event
 * dispatched by apiClient on unrecoverable 401s.
 */

import {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
} from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null);
  const [loading, setLoading] = useState(true); // true while /me is in flight

  // ── Hydrate on startup ─────────────────────────────────────
  useEffect(() => {
    fetch(`${import.meta.env.VITE_API_BASE}/auth/me`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setUser(data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  // ── Logout ─────────────────────────────────────────────────
  const logout = useCallback(async () => {
    try {
      await fetch(`${import.meta.env.VITE_API_BASE}/auth/logout`, {
        method:      "POST",
        credentials: "include",
      });
    } catch {
      // Swallow — clear local state regardless
    }
    setUser(null);
    window.location.href = "/login";
  }, []);

  // ── Global 401 listener ────────────────────────────────────
  useEffect(() => {
    const handler = () => {
      setUser(null);
      window.location.href = "/login";
    };
    window.addEventListener("qually:logout", handler);
    return () => window.removeEventListener("qually:logout", handler);
  }, []);

  const isQA         = user?.department === "QA";
  const isOperations = user?.department === "OPERATIONS";

  return (
    <AuthContext.Provider value={{ user, loading, logout, isQA, isOperations }}>
      {children}
    </AuthContext.Provider>
  );
}

/** @returns {{ user, loading, logout, isQA, isOperations }} */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}