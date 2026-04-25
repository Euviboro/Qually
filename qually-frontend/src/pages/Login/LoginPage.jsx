/**
 * @module pages/Login/LoginPage
 *
 * Mock login page for the demo environment.
 * Fetches all users from the backend and presents them in a SearchableSelect.
 * On selection, stores the full UserResponseDTO in AuthContext and localStorage,
 * then redirects to the dashboard.
 */

import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { getUsers } from "../../api/users";
import { getUserById } from "../../api/users";
import { SearchableSelect } from "../../components/ui/SearchableSelect";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate   = useNavigate();

  const [users,        setUsers]        = useState([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [selectedId,   setSelectedId]   = useState(null);
  const [logging,      setLogging]       = useState(false);
  const [error,        setError]         = useState(null);

  useEffect(() => {
    getUsers()
      .then(setUsers)
      .catch(() => setError("Could not load users. Is the backend running?"))
      .finally(() => setUsersLoading(false));
  }, []);

  const userOptions = users.map((u) => ({
    value: u.userId,
    label: `${u.fullName} — ${u.roleName ?? "No role"} (${u.department ?? "—"})`,
  }));

  const handleLogin = async () => {
    if (!selectedId) return;
    setLogging(true);
    setError(null);
    try {
      const user = await getUserById(selectedId);
      login(user);
      navigate("/");
    } catch (err) {
      setError(err.message ?? "Login failed. Please try again.");
    } finally {
      setLogging(false);
    }
  };

  return (
    <div className="min-h-screen bg-bg-secondary flex items-center justify-center p-6">
      <div className="w-full max-w-[420px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-8">

        {/* Logo area */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-lsg-blue rounded-xl mb-4">
            <svg width="24" height="24" viewBox="0 0 16 16" fill="none">
              <path d="M3 13L3 6L9 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M7 13L13 10L13 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" opacity="0.7" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">Qually</h1>
          <p className="text-sm text-text-ter mt-1">by Lean Solutions Group</p>
        </div>

        <div className="flex flex-col gap-5">
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-2">
              Select your account
            </label>
            <SearchableSelect
              options={userOptions}
              value={selectedId}
              onChange={setSelectedId}
              placeholder="Choose a user to continue…"
              searchPlaceholder="Search by name or role…"
              loading={usersLoading}
              emptyMessage="No users found"
            />
          </div>

          {error && (
            <p className="text-xs text-error-on bg-error-surface px-3 py-2 rounded-lg">
              {error}
            </p>
          )}

          <button
            onClick={handleLogin}
            disabled={!selectedId || logging}
            className="w-full py-2.5 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {logging ? "Signing in…" : "Continue"}
          </button>
        </div>

        <p className="text-center text-xs text-text-ter mt-6">
          Demo environment — no password required
        </p>
      </div>
    </div>
  );
}
