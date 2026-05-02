import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import DashboardPage from "../pages/Dashboard/DashboardPage";

export function RequireAuth({ children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

export function RequirePinChanged({ children }) {
  const { user } = useAuth();
  if (user?.forcePinChange) return <Navigate to="/change-pin" replace />;
  return children;
}

export function RequireQA({ children }) {
  const { isQA } = useAuth();
  if (!isQA) return <Navigate to="/results" replace />;
  return children;
}

export function IndexRedirect() {
  const { isQA } = useAuth();
  return isQA ? <DashboardPage /> : <Navigate to="/results" replace />;
}