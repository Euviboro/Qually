/**
 * @module context/AuthContext
 *
 * Mock authentication context for the demo environment.
 * Stores the selected user in React state and localStorage so the session
 * persists across page refreshes. The full UserResponseDTO is kept in context
 * so all components can read the user's role, department, hierarchy level,
 * and client access without additional API calls.
 *
 * Every request made through apiClient.js automatically includes the user's
 * ID in the X-User-Id header, which the backend reads for permission checks.
 *
 * Replace this context with real authentication (e.g. Azure AD, Auth0) when
 * moving beyond the demo stage — the rest of the app uses only the values
 * exposed by this context, so the swap is isolated here.
 */

import { createContext, useContext, useState, useCallback } from "react";

/**
 * @typedef {import('../api/users').UserResponseDTO} UserResponseDTO
 */

/**
 * @typedef {Object} AuthContextValue
 * @property {UserResponseDTO|null} user  - The currently logged-in user, or null.
 * @property {(user: UserResponseDTO) => void} login  - Store a user in context.
 * @property {() => void} logout - Clear the current user.
 * @property {boolean} isQA         - Shorthand: user is in the QA department.
 * @property {boolean} isOperations - Shorthand: user is in the OPERATIONS department.
 */

const AuthContext = createContext(null);

/**
 * Reads the persisted user from localStorage on first render.
 * Returns null if no user is stored or the stored value is malformed.
 *
 * @returns {UserResponseDTO|null}
 */
function readPersistedUser() {
  try {
    const raw = localStorage.getItem("qually_user");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

/**
 * @param {Object} props
 * @param {React.ReactNode} props.children
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(readPersistedUser);

  const login = useCallback((userDto) => {
    setUser(userDto);
    localStorage.setItem("qually_user", JSON.stringify(userDto));
    localStorage.setItem("userId", String(userDto.userId));
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    localStorage.removeItem("qually_user");
    localStorage.removeItem("userId");
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
 * Access the auth context from any component.
 *
 * @returns {AuthContextValue}
 * @throws if used outside of `AuthProvider`.
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
