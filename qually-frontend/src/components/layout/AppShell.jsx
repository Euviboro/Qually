/**
 * @module components/layout/AppShell
 *
 * Root layout shell shared by every page. Renders the top bar, sidebar
 * navigation, and the main content outlet.
 *
 * The "Log session" top-bar button opens `StartSessionModal` rather than
 * navigating directly to `/sessions/log`. The modal collects the client and
 * protocol, then navigates with the selected protocol passed via router state
 * so `LogSessionPage` can start immediately without a second fetch.
 */

import { useState } from "react";
import { Outlet, NavLink, useNavigate } from "react-router-dom";
import { StartSessionModal } from "../ui/StartSessionModal";

// ── Logo ──────────────────────────────────────────────────────

function LSGLogoMark({ size = 30 }) {
  return (
    <div
      className="flex items-center justify-center flex-shrink-0 bg-lsg-blue rounded-[7px]"
      style={{ width: size, height: size }}
    >
      <svg width={size * 0.6} height={size * 0.6} viewBox="0 0 16 16" fill="none">
        <path d="M3 13L3 6L9 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M7 13L13 10L13 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" opacity="0.7" />
      </svg>
    </div>
  );
}

// ── Navigation items ──────────────────────────────────────────

const NAV_ITEMS = [
  {
    to: "/",
    label: "Dashboard",
    end: true,
    icon: (props) => (
      <svg {...props} viewBox="0 0 15 15" fill="none">
        <rect x="1" y="1" width="5.5" height="5.5" rx="1.5" fill="currentColor" />
        <rect x="8.5" y="1" width="5.5" height="5.5" rx="1.5" fill="currentColor" opacity="0.5" />
        <rect x="1" y="8.5" width="5.5" height="5.5" rx="1.5" fill="currentColor" opacity="0.5" />
        <rect x="8.5" y="8.5" width="5.5" height="5.5" rx="1.5" fill="currentColor" />
      </svg>
    ),
  },
  {
    to: "/protocols/new",
    label: "New Protocol",
    end: false,
    icon: (props) => (
      <svg {...props} viewBox="0 0 15 15" fill="none">
        <rect x="2" y="1" width="11" height="13" rx="1.5" stroke="currentColor" strokeWidth="1.4" />
        <path d="M5 5h5M5 8h5M5 11h3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
      </svg>
    ),
  },
  {
    to: "/sessions/log",
    label: "Log Session",
    end: false,
    icon: (props) => (
      <svg {...props} viewBox="0 0 15 15" fill="none">
        <circle cx="7.5" cy="7.5" r="6" stroke="currentColor" strokeWidth="1.4" />
        <path d="M7.5 4.5v3l2 2" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
      </svg>
    ),
  },
];

// ── Shell ─────────────────────────────────────────────────────

/**
 * Root layout component. Mounts the `StartSessionModal` so the "Log session"
 * top-bar button can open it from any page without lifting state higher.
 *
 * On modal confirmation, navigates to `/sessions/log` and passes the selected
 * `protocol` object via `location.state` so `LogSessionPage` can render
 * immediately without a redundant network request.
 *
 * @returns {JSX.Element}
 */
export function AppShell() {
  const navigate = useNavigate();
  const [sessionModalOpen, setSessionModalOpen] = useState(false);

  /**
   * Called by `StartSessionModal` when the user clicks "Start Session".
   * Navigates to the log page carrying the full protocol object in router state.
   *
   * @param {import('../../api/protocols').AuditProtocolResponseDTO} protocol
   */
  const handleSessionConfirm = (protocol) => {
    setSessionModalOpen(false);
    navigate("/sessions/log", { state: { protocol } });
  };

  return (
    <div className="flex flex-col min-h-screen font-sans">

      {/* ── TOP BAR ──────────────────────────────────────────── */}
      <header className="sticky top-0 z-[100] flex items-center justify-between h-[var(--topbar-height)] px-6 bg-bg-primary border-b border-border-sec shadow-sm">

        {/* Brand */}
        <button
          onClick={() => navigate("/")}
          className="flex items-center gap-2.5 p-0 bg-transparent border-none cursor-pointer"
        >
          <LSGLogoMark size={30} />
          <div className="flex flex-col text-left leading-[1.1]">
            <span className="text-[13px] font-bold text-text-pri tracking-tight">Qually</span>
            <span className="text-[10px] text-text-ter tracking-wide">by Lean Solutions Group</span>
          </div>
        </button>

        {/* Global Actions */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate("/protocols/new")}
            className="flex items-center gap-1.5 px-3.5 py-1.5 text-sm font-medium text-text-sec bg-bg-primary border border-border-sec rounded-md transition-all hover:border-border-pri hover:bg-bg-secondary"
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
            New protocol
          </button>

          {/* Opens the session modal instead of navigating directly */}
          <button
            onClick={() => setSessionModalOpen(true)}
            className="flex items-center gap-1.5 px-3.5 py-1.5 text-sm font-medium text-white bg-lsg-navy rounded-md transition-all hover:bg-lsg-midnight"
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <circle cx="6" cy="6" r="4.5" stroke="white" strokeWidth="1.5" />
              <path d="M6 3.5v2.5l1.5 1.5" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
            Log session
          </button>

          <div className="flex items-center justify-center w-8 h-8 ml-1 text-[11px] font-bold text-lsg-blue bg-bg-accent border border-border-sec rounded-full cursor-pointer font-mono">
            EU
          </div>
        </div>
      </header>

      {/* ── BODY ─────────────────────────────────────────────── */}
      <div className="flex flex-1">

        {/* Sidebar */}
        <aside className="sticky top-[var(--topbar-height)] flex flex-col w-[var(--sidebar-width)] h-[calc(100vh-var(--topbar-height))] p-3 gap-1 bg-bg-primary border-r border-border-sec overflow-y-auto shrink-0">
          <p className="px-3 mb-1 text-[10px] font-semibold text-text-ter uppercase tracking-widest">
            Navigation
          </p>

          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg transition-all duration-200 ${
                  isActive
                    ? "bg-bg-accent text-lsg-blue-dark font-semibold"
                    : "text-text-sec hover:bg-bg-secondary"
                }`
              }
            >
              <item.icon className="w-4 h-4" />
              <span className="text-[14px]">{item.label}</span>
            </NavLink>
          ))}
        </aside>

        {/* Page content */}
        <main className="flex-1 min-w-0 bg-bg-secondary overflow-y-auto">
          <Outlet />
        </main>
      </div>

      {/* Session setup modal — mounted at shell level so the top-bar button works everywhere */}
      <StartSessionModal
        isOpen={sessionModalOpen}
        onClose={() => setSessionModalOpen(false)}
        onConfirm={handleSessionConfirm}
      />
    </div>
  );
}