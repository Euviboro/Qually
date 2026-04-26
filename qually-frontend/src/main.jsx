import React, { lazy, Suspense } from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider, Outlet, Navigate } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { PageSpinner } from "./components/ui/PageSpinner";
import { ErrorBoundary } from "./components/ErrorBoundary";
import "./index.css";

const DashboardPage      = lazy(() => import("./pages/Dashboard/DashboardPage"));
const NewProtocolPage    = lazy(() => import("./pages/NewProtocol/NewProtocolPage"));
const ShowProtocolPage   = lazy(() => import("./pages/ShowProtocol/ShowProtocolPage"));
const LogSessionPage     = lazy(() => import("./pages/LogSession/LogSessionPage"));
const DraftsPage         = lazy(() => import("./pages/Drafts/DraftsPage"));
const SessionResultsPage = lazy(() => import("./pages/SessionResults/SessionResultsPage"));
const ResultsPage        = lazy(() => import("./pages/Results/ResultsPage"));
const DisputeInboxPage   = lazy(() => import("./pages/DisputeInbox/DisputeInboxPage"));
const SettingsPage       = lazy(() => import("./pages/Settings/SettingsPage"));
const LoginPage          = lazy(() => import("./pages/Login/LoginPage"));

function RequireAuth({ children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

function RequireQA({ children }) {
  const { isQA } = useAuth();
  if (!isQA) return <Navigate to="/results" replace />;
  return children;
}

function IndexRedirect() {
  const { isQA } = useAuth();
  return isQA ? <DashboardPage /> : <Navigate to="/results" replace />;
}

const router = createBrowserRouter([
  {
    path: "/login",
    element: <Suspense fallback={<PageSpinner />}><LoginPage /></Suspense>,
  },
  {
    path: "/",
    element: (
      <RequireAuth>
        <ErrorBoundary><AppShell /></ErrorBoundary>
      </RequireAuth>
    ),
    children: [
      {
        element: (
          <ErrorBoundary>
            <Suspense fallback={<PageSpinner />}><Outlet /></Suspense>
          </ErrorBoundary>
        ),
        children: [
          { index: true, element: <IndexRedirect /> },

          // QA-only routes
          { path: "protocols/new", element: <RequireQA><NewProtocolPage /></RequireQA> },
          { path: "protocols/:id", element: <RequireQA><ShowProtocolPage /></RequireQA> },
          { path: "sessions/log",  element: <RequireQA><LogSessionPage /></RequireQA> },
          { path: "drafts",        element: <RequireQA><DraftsPage /></RequireQA> },

          // Shared routes
          { path: "sessions/:id",   element: <SessionResultsPage /> },
          { path: "results",        element: <ResultsPage /> },
          { path: "disputes",       element: <DisputeInboxPage /> },

          // QA-only settings
          { path: "settings/users", element: <RequireQA><SettingsPage /></RequireQA> },
        ],
      },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  </React.StrictMode>
);