/**
 * @module context/AuthContext
 *
 * Authentication context for Qually.
 *
 * Tokens are stored in httpOnly cookies managed by the browser — not in
 * localStorage. This context stores only the UserResponseDTO (display data)
 * in localStorage for persistence across page refreshes.
 *
 * Logout calls POST /api/auth/logout to clear the server-side cookies, then
 * clears local state. The context also listens for the "qually:logout" custom
 * event dispatched by apiClient when a 401 cannot be recovered — this ensures
 * the user is logged out from anywhere in the app when their session expires.
 *
 * Replace this context with Azure AD / MSAL when Microsoft Auth is available.
 * The rest of the app uses only the values exposed here, so the swap is
 * isolated to this file and LoginPage.
 */

import { createContext, useContext, useState, useCallback, useEffect } from "react";
import { useNavigate } from "react-router-dom";

const AuthContext = createContext(null);

function readPersistedUser() {
  try {
    const raw = localStorage.getItem("qually_user");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readPersistedUser);

  /**
   * Called after a successful POST /api/auth/login.
   * Stores the UserResponseDTO — tokens are already in cookies.
   *
   * @param {import('../api/users').UserResponseDTO} userDto
   */
  const login = useCallback((userDto) => {
    setUser(userDto);
    localStorage.setItem("qually_user", JSON.stringify(userDto));
  }, []);

  /**
   * Clears the session: calls POST /api/auth/logout to clear httpOnly cookies,
   * then removes the persisted user from localStorage.
   */
  const logout = useCallback(async () => {
    try {
      await fetch(`${import.meta.env.VITE_API_BASE}/auth/logout`, {
        method:      "POST",
        credentials: "include",
        headers:     { "Content-Type": "application/json" },
      });
    } catch {
      // Swallow — even if the server call fails, clear local state
    }
    setUser(null);
    localStorage.removeItem("qually_user");
  }, []);

  // Listen for the global logout event dispatched by apiClient
  // when a 401 cannot be recovered (expired refresh token or invalid token).
  useEffect(() => {
    const handler = () => {
      setUser(null);
      localStorage.removeItem("qually_user");
      // Redirect to login — use window.location since we may be outside Router
      window.location.href = "/login";
    };
    window.addEventListener("qually:logout", handler);
    return () => window.removeEventListener("qually:logout", handler);
  }, []);

  const isQA         = user?.department === "QA";
  const isOperations = user?.department === "OPERATIONS";

  return (
    <AuthContext.Provider value={{ user, login, logout, isQA, isOperations }}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * @returns {{ user, login, logout, isQA, isOperations }}
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}