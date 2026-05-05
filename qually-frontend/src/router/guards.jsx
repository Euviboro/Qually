import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import DashboardPage from "../pages/Dashboard/DashboardPage";

/**
 * Blocks unauthenticated access.
 *
 * Waits for the /me fetch to complete before deciding — without this,
 * the guard sees user=null on first render and redirects to /login
 * before the cookie is even checked.
 */
export function RequireAuth({ children }) {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

export function RequireQA({ children }) {
  const { isQA, loading } = useAuth();
  if (loading) return null;
  if (!isQA) return <Navigate to="/results" replace />;
  return children;
}

export function IndexRedirect() {
  const { isQA, loading } = useAuth();
  if (loading) return null;
  return isQA ? <DashboardPage /> : <Navigate to="/results" replace />;
}