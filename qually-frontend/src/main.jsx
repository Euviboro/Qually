import React, { lazy, Suspense } from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider, Outlet } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import NotFoundPage from "./pages/NotFound/NotFoundPage";
import { PageSpinner } from "./components/ui/PageSpinner";
import { ErrorBoundary } from "./components/ErrorBoundary";
import "./index.css";

const DashboardPage      = lazy(() => import("./pages/Dashboard/DashboardPage"));
const NewProtocolPage    = lazy(() => import("./pages/NewProtocol/NewProtocolPage"));
const ShowProtocolPage   = lazy(() => import("./pages/ShowProtocol/ShowProtocolPage"));
const LogSessionPage     = lazy(() => import("./pages/LogSession/LogSessionPage"));
const SessionResultsPage = lazy(() => import("./pages/SessionResults/SessionResultsPage"));

const router = createBrowserRouter([
  {
    path: "/",
    element: (
      <ErrorBoundary>
        <AppShell />
      </ErrorBoundary>
    ),
    children: [
      {
        element: (
          <ErrorBoundary>
            <Suspense fallback={<PageSpinner />}>
              <Outlet />
            </Suspense>
          </ErrorBoundary>
        ),
        children: [
          { index: true,               element: <DashboardPage /> },
          { path: "protocols/new",     element: <NewProtocolPage /> },
          { path: "protocols/:id",     element: <ShowProtocolPage /> },
          { path: "sessions/log",      element: <LogSessionPage /> },
          { path: "sessions/:id",      element: <SessionResultsPage /> },
        ],
      },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);