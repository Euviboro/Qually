import React, { lazy, Suspense } from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider, Outlet, Navigate } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { PageSpinner } from "./components/ui/PageSpinner";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { RequireAuth, RequirePinChanged, RequireQA, IndexRedirect } from "./router/guards";
import "./index.css";

const DashboardPage          = lazy(() => import("./pages/Dashboard/DashboardPage"));
const NewProtocolPage        = lazy(() => import("./pages/NewProtocol/NewProtocolPage"));
const ShowProtocolPage       = lazy(() => import("./pages/ShowProtocol/ShowProtocolPage"));
const LogSessionPage         = lazy(() => import("./pages/LogSession/LogSessionPage"));
const DraftsPage             = lazy(() => import("./pages/Drafts/DraftsPage"));
const SessionResultsPage     = lazy(() => import("./pages/SessionResults/SessionResultsPage"));
const ResultsPage            = lazy(() => import("./pages/Results/ResultsPage"));
const DisputesPage           = lazy(() => import("./pages/Disputes/DisputesPage"));
const SettingsPage           = lazy(() => import("./pages/Settings/SettingsPage"));
const LoginPage              = lazy(() => import("./pages/Login/LoginPage"));
const ChangePinPage          = lazy(() => import("./pages/ChangePin/ChangePinPage"));
const CalibrationListPage    = lazy(() => import("./pages/Calibration/CalibrationListPage"));
const CreateCalibrationPage  = lazy(() => import("./pages/Calibration/CreateCalibrationPage"));






const router = createBrowserRouter([
  {
    path: "/login",
    element: <Suspense fallback={<PageSpinner />}><LoginPage /></Suspense>,
  },
  {
    path: "/change-pin",
    element: (
      <RequireAuth>
        <Suspense fallback={<PageSpinner />}><ChangePinPage /></Suspense>
      </RequireAuth>
    ),
  },
  {
    path: "/",
    element: (
      <RequireAuth>
        <RequirePinChanged>
          <ErrorBoundary><AppShell /></ErrorBoundary>
        </RequirePinChanged>
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
          { path: "protocols/new",    element: <RequireQA><NewProtocolPage /></RequireQA> },
          { path: "protocols/:id",    element: <RequireQA><ShowProtocolPage /></RequireQA> },
          { path: "sessions/log",     element: <RequireQA><LogSessionPage /></RequireQA> },
          { path: "drafts",           element: <RequireQA><DraftsPage /></RequireQA> },
          { path: "calibration",      element: <RequireQA><CalibrationListPage /></RequireQA> },
          { path: "calibration/new",  element: <RequireQA><CreateCalibrationPage /></RequireQA> },

          // Shared routes
          { path: "sessions/:id",     element: <SessionResultsPage /> },
          { path: "results",          element: <ResultsPage /> },
          { path: "disputes",         element: <DisputesPage /> },

          // QA-only settings
          { path: "settings/users",   element: <RequireQA><SettingsPage /></RequireQA> },
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