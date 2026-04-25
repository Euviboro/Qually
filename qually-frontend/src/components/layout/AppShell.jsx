import { useState } from "react";
import { Outlet, NavLink, useNavigate } from "react-router-dom";
import { StartSessionModal } from "../ui/StartSessionModal";
import { useAuth } from "../../context/AuthContext";

function LSGLogoMark({ size = 30 }) {
  return (
    <div className="flex items-center justify-center flex-shrink-0 bg-lsg-blue rounded-[7px]" style={{ width: size, height: size }}>
      <svg width={size * 0.6} height={size * 0.6} viewBox="0 0 16 16" fill="none">
        <path d="M3 13L3 6L9 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M7 13L13 10L13 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" opacity="0.7" />
      </svg>
    </div>
  );
}

/**
 * Navigation items with visibility rules:
 * - `qaOnly`   → shown only to QA department users
 * - `opsOnly`  → shown only to OPERATIONS department users
 * - neither    → shown to everyone
 *
 * OPERATIONS users see Results and Disputes only.
 * QA users see the full navigation.
 */
const NAV_ITEMS = [
  {
    to: "/", label: "Dashboard", end: true, qaOnly: true, opsOnly: false,
    icon: (p) => <svg {...p} viewBox="0 0 15 15" fill="none"><rect x="1" y="1" width="5.5" height="5.5" rx="1.5" fill="currentColor"/><rect x="8.5" y="1" width="5.5" height="5.5" rx="1.5" fill="currentColor" opacity="0.5"/><rect x="1" y="8.5" width="5.5" height="5.5" rx="1.5" fill="currentColor" opacity="0.5"/><rect x="8.5" y="8.5" width="5.5" height="5.5" rx="1.5" fill="currentColor"/></svg>,
  },
  {
    to: "/protocols/new", label: "New Protocol", end: false, qaOnly: true, opsOnly: false,
    icon: (p) => <svg {...p} viewBox="0 0 15 15" fill="none"><rect x="2" y="1" width="11" height="13" rx="1.5" stroke="currentColor" strokeWidth="1.4"/><path d="M5 5h5M5 8h5M5 11h3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/></svg>,
  },
  {
    to: "/sessions/log", label: "Log Session", end: false, qaOnly: true, opsOnly: false,
    icon: (p) => <svg {...p} viewBox="0 0 15 15" fill="none"><circle cx="7.5" cy="7.5" r="6" stroke="currentColor" strokeWidth="1.4"/><path d="M7.5 4.5v3l2 2" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/></svg>,
  },
  {
    to: "/results", label: "Results", end: false, qaOnly: false, opsOnly: false,
    icon: (p) => <svg {...p} viewBox="0 0 15 15" fill="none"><rect x="1" y="3" width="13" height="10" rx="1.5" stroke="currentColor" strokeWidth="1.4"/><path d="M1 6h13M5 6v7M10 6v7" stroke="currentColor" strokeWidth="1.2"/></svg>,
  },
  {
    to: "/disputes", label: "Dispute Inbox", end: false, qaOnly: false, opsOnly: false,
    icon: (p) => <svg {...p} viewBox="0 0 15 15" fill="none"><path d="M7.5 1L14 13H1L7.5 1Z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round"/><path d="M7.5 6v3M7.5 11v.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/></svg>,
  },
  {
    to: "/settings/users", label: "Settings", end: false, qaOnly: true, opsOnly: false,
    icon: (p) => <svg {...p} viewBox="0 0 15 15" fill="none"><circle cx="7.5" cy="7.5" r="2" stroke="currentColor" strokeWidth="1.4"/><path d="M7.5 1v1.5M7.5 12.5V14M1 7.5h1.5M12.5 7.5H14M2.9 2.9l1.1 1.1M11 11l1.1 1.1M2.9 12.1L4 11M11 4l1.1-1.1" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/></svg>,
  },
];

export function AppShell() {
  const navigate = useNavigate();
  const { user, logout, isQA, isOperations } = useAuth();
  const [sessionModalOpen, setSessionModalOpen] = useState(false);

  const handleSessionConfirm = (protocol) => {
    setSessionModalOpen(false);
    navigate("/sessions/log", { state: { protocol } });
  };

  const initials = user?.fullName
    ? user.fullName.split(" ").map((n) => n[0]).join("").slice(0, 2).toUpperCase()
    : "?";

  // Filter nav items by the current user's department
  const visibleNav = NAV_ITEMS.filter((item) => {
    if (item.qaOnly && !isQA)         return false;
    if (item.opsOnly && !isOperations) return false;
    return true;
  });

  return (
    <div className="flex flex-col min-h-screen font-sans">

      {/* ── TOP BAR ──────────────────────────────────────────── */}
      <header className="sticky top-0 z-[100] flex items-center justify-between h-[var(--topbar-height)] px-6 bg-bg-primary border-b border-border-sec shadow-sm">

        <button
          onClick={() => navigate(isQA ? "/" : "/results")}
          className="flex items-center gap-2.5 p-0 bg-transparent border-none cursor-pointer"
        >
          <LSGLogoMark size={30} />
          <div className="flex flex-col text-left leading-[1.1]">
            <span className="text-[13px] font-bold text-text-pri tracking-tight">Qually</span>
            <span className="text-[10px] text-text-ter tracking-wide">by Lean Solutions Group</span>
          </div>
        </button>

        <div className="flex items-center gap-2">
          {/* QA-only top bar actions */}
          {isQA && (
            <>
              <button
                onClick={() => navigate("/protocols/new")}
                className="flex items-center gap-1.5 px-3.5 py-1.5 text-sm font-medium text-text-sec bg-bg-primary border border-border-sec rounded-md transition-all hover:border-border-pri hover:bg-bg-secondary"
              >
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                  <path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
                New protocol
              </button>
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
            </>
          )}

          {/* User info + logout */}
          <div className="flex items-center gap-2 ml-1 pl-3 border-l border-border-ter">
            <div className="flex flex-col text-right leading-tight">
              <span className="text-[12px] font-semibold text-text-pri">{user?.fullName ?? "Guest"}</span>
              <span className="text-[10px] text-text-ter">{user?.roleName ?? ""}</span>
            </div>
            <div className="flex items-center justify-center w-8 h-8 text-[11px] font-bold text-lsg-blue bg-bg-accent border border-border-sec rounded-full font-mono">
              {initials}
            </div>
            <button
              onClick={() => { logout(); navigate("/login"); }}
              title="Sign out"
              className="text-text-ter hover:text-error-on p-1 transition-colors"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </button>
          </div>
        </div>
      </header>

      {/* ── BODY ─────────────────────────────────────────────── */}
      <div className="flex flex-1">
        <aside className="sticky top-[var(--topbar-height)] flex flex-col w-[var(--sidebar-width)] h-[calc(100vh-var(--topbar-height))] p-3 gap-1 bg-bg-primary border-r border-border-sec overflow-y-auto shrink-0">
          <p className="px-3 mb-1 text-[10px] font-semibold text-text-ter uppercase tracking-widest">
            Navigation
          </p>
          {visibleNav.map((item) => (
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

        <main className="flex-1 min-w-0 bg-bg-secondary overflow-y-auto">
          <Outlet />
        </main>
      </div>

      {/* Session modal — only mounted for QA */}
      {isQA && (
        <StartSessionModal
          isOpen={sessionModalOpen}
          onClose={() => setSessionModalOpen(false)}
          onConfirm={handleSessionConfirm}
        />
      )}
    </div>
  );
}
