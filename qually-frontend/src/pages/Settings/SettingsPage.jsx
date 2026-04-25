/**
 * @module pages/Settings/SettingsPage
 *
 * User management page, accessible only to QA department users.
 * Lists all users (active and inactive) with create, edit, and
 * deactivate/reactivate actions.
 */

import { useState, useEffect } from "react";
import { getUsers, createUser, updateUser, deactivateUser, reactivateUser } from "../../api/users";
import { SearchableSelect } from "../../components/ui/SearchableSelect";
import { api } from "../../api/apiClient";

// Fetch roles for the role selector
const getRoles = () => api.get("/roles");
const getClients = () => api.get("/clients");

// ── User Form Modal ───────────────────────────────────────────

function UserFormModal({ user, roles, clients, onClose, onSaved }) {
  const isEdit = !!user;
  const [form, setForm] = useState({
    userEmail:  user?.userEmail  ?? "",
    fullName:   user?.fullName   ?? "",
    roleId:     user?.roleId     ?? null,
    managerId:  user?.managerId  ?? null,
    clientIds:  user?.clientIds  ?? [],
  });
  const [saving, setSaving] = useState(false);
  const [error,  setError]  = useState(null);
  const [allUsers, setAllUsers] = useState([]);

  useEffect(() => {
    getUsers({ activeOnly: true }).then(setAllUsers).catch(() => {});
  }, []);

  const set = (field, val) => setForm((p) => ({ ...p, [field]: val }));

  const toggleClient = (id) => {
    setForm((p) => ({
      ...p,
      clientIds: p.clientIds.includes(id)
        ? p.clientIds.filter((c) => c !== id)
        : [...p.clientIds, id],
    }));
  };

  const handleSubmit = async () => {
    if (!form.fullName.trim() || !form.userEmail.trim() || !form.roleId) {
      setError("Name, email and role are required."); return;
    }
    setSaving(true); setError(null);
    try {
      if (isEdit) {
        await updateUser(user.userId, { fullName: form.fullName, roleId: form.roleId, managerId: form.managerId, clientIds: form.clientIds });
      } else {
        await createUser({ userEmail: form.userEmail, fullName: form.fullName, roleId: form.roleId, managerId: form.managerId, clientIds: form.clientIds });
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const roleOptions   = roles.map((r)   => ({ value: r.roleId,   label: `${r.roleName} (${r.department})` }));
  const managerOptions= allUsers.filter((u) => u.userId !== user?.userId).map((u) => ({ value: u.userId, label: `${u.fullName} — ${u.roleName ?? ""}` }));

  return (
    <div onClick={onClose} className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-[rgba(0,20,50,0.4)] backdrop-blur-[2px]">
      <div onClick={(e) => e.stopPropagation()} className="w-full max-w-[520px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-7 flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        <div className="flex items-start justify-between">
          <h2 className="text-lg font-bold text-text-pri">{isEdit ? "Edit User" : "Create User"}</h2>
          <button onClick={onClose} className="text-text-ter hover:text-text-pri p-1 transition-colors">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
          </button>
        </div>

        <div className="flex flex-col gap-4">
          {/* Email — only on create */}
          {!isEdit && (
            <div>
              <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">Email <span className="text-lsg-blue">*</span></label>
              <input type="email" value={form.userEmail} onChange={(e) => set("userEmail", e.target.value)}
                placeholder="user@company.com"
                className="w-full px-3 py-2 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all" />
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">Full Name <span className="text-lsg-blue">*</span></label>
            <input type="text" value={form.fullName} onChange={(e) => set("fullName", e.target.value)}
              placeholder="First Last"
              className="w-full px-3 py-2 text-sm rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all" />
          </div>

          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">Role <span className="text-lsg-blue">*</span></label>
            <SearchableSelect options={roleOptions} value={form.roleId} onChange={(v) => set("roleId", v)} placeholder="Select role…" emptyMessage="No roles found" />
          </div>

          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">Manager <span className="text-text-ter font-normal">(optional)</span></label>
            <SearchableSelect options={managerOptions} value={form.managerId} onChange={(v) => set("managerId", v)} placeholder="Select manager…" emptyMessage="No users found" />
          </div>

          {/* Client assignment */}
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-2">Client Access <span className="text-text-ter font-normal">(optional)</span></label>
            <div className="flex flex-wrap gap-2">
              {clients.map((c) => {
                const isSelected = form.clientIds.includes(c.clientId);
                return (
                  <button key={c.clientId} type="button" onClick={() => toggleClient(c.clientId)}
                    className={["px-3 py-1.5 rounded-lg border text-xs font-medium transition-all",
                      isSelected ? "border-lsg-blue bg-bg-accent text-lsg-blue-dark" : "border-border-sec text-text-ter hover:border-border-pri"].join(" ")}>
                    {c.clientName}
                  </button>
                );
              })}
              {clients.length === 0 && <p className="text-xs text-text-ter italic">No clients available</p>}
            </div>
          </div>
        </div>

        {error && <p className="text-xs text-error-on bg-error-surface px-3 py-2 rounded-lg">{error}</p>}

        <div className="flex justify-end gap-3 pt-1">
          <button onClick={onClose} className="px-4 py-2 text-sm font-medium text-text-sec border border-border-sec rounded-md hover:bg-bg-secondary transition-colors">Cancel</button>
          <button onClick={handleSubmit} disabled={saving}
            className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40">
            {saving ? "Saving…" : isEdit ? "Save Changes" : "Create User"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────

export default function SettingsPage() {
  const [users,   setUsers]   = useState([]);
  const [roles,   setRoles]   = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editTarget, setEditTarget] = useState(null); // null = closed, {} = create, user = edit
  const [confirmDeactivate, setConfirmDeactivate] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const [u, r, c] = await Promise.all([getUsers(), getRoles(), getClients()]);
      setUsers(u); setRoles(r); setClients(c);
    } catch {} finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const handleDeactivate = async (userId) => {
    await deactivateUser(userId); load(); setConfirmDeactivate(null);
  };
  const handleReactivate = async (userId) => {
    await reactivateUser(userId); load();
  };

  const activeUsers   = users.filter((u) => u.isActive);
  const inactiveUsers = users.filter((u) => !u.isActive);

  return (
    <div className="max-w-[900px] mx-auto px-8 py-10">
      <header className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">User Management</h1>
          <p className="text-text-ter text-sm mt-1">{activeUsers.length} active · {inactiveUsers.length} inactive</p>
        </div>
        <button onClick={() => setEditTarget({})}
          className="flex items-center gap-2 px-4 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/></svg>
          Add User
        </button>
      </header>

      {loading ? (
        <div className="flex items-center gap-3 text-text-ter p-6">
          <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
          Loading users…
        </div>
      ) : (
        <>
          {/* Active users */}
          <UserTable users={activeUsers} onEdit={setEditTarget} onDeactivate={setConfirmDeactivate} onReactivate={handleReactivate} />

          {/* Inactive users */}
          {inactiveUsers.length > 0 && (
            <div className="mt-8">
              <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-3">Inactive Users</p>
              <UserTable users={inactiveUsers} onEdit={setEditTarget} onDeactivate={setConfirmDeactivate} onReactivate={handleReactivate} inactive />
            </div>
          )}
        </>
      )}

      {/* Create / edit modal */}
      {editTarget !== null && (
        <UserFormModal
          user={editTarget?.userId ? editTarget : null}
          roles={roles}
          clients={clients}
          onClose={() => setEditTarget(null)}
          onSaved={load}
        />
      )}

      {/* Deactivate confirm */}
      {confirmDeactivate && (
        <div onClick={() => setConfirmDeactivate(null)} className="fixed inset-0 z-[600] flex items-center justify-center p-4 bg-[rgba(0,20,50,0.4)] backdrop-blur-[2px]">
          <div onClick={(e) => e.stopPropagation()} className="w-full max-w-sm bg-bg-primary rounded-xl border border-border-sec shadow-lg p-7 flex flex-col gap-5">
            <h2 className="text-lg font-bold text-text-pri">Deactivate User?</h2>
            <p className="text-sm text-text-sec">{confirmDeactivate.fullName} will be hidden from all dropdowns but their historical records will be preserved.</p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setConfirmDeactivate(null)} className="px-4 py-2 text-sm font-medium text-text-sec border border-border-sec rounded-md hover:bg-bg-secondary transition-colors">Cancel</button>
              <button onClick={() => handleDeactivate(confirmDeactivate.userId)} className="px-5 py-2 text-sm font-bold text-white bg-error-on hover:opacity-90 rounded-md transition-colors">Deactivate</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function UserTable({ users, onEdit, onDeactivate, onReactivate, inactive = false }) {
  if (users.length === 0) return null;
  return (
    <div className="bg-bg-primary border border-border-sec rounded-xl overflow-hidden shadow-card">
      <table className="min-w-full text-sm">
        <thead>
          <tr className="border-b border-border-ter bg-bg-secondary/50">
            {["Name", "Email", "Role", "Department", "Manager", "Clients", ""].map((h) => (
              <th key={h} className="px-4 py-3 text-left text-[11px] font-bold text-text-ter uppercase tracking-wider">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {users.map((u, idx) => (
            <tr key={u.userId} className={["border-b border-border-ter last:border-0 transition-colors hover:bg-bg-secondary/30",
              inactive ? "opacity-60" : "", idx % 2 === 0 ? "" : "bg-bg-secondary/10"].join(" ")}>
              <td className="px-4 py-3 font-medium text-text-pri whitespace-nowrap">{u.fullName}</td>
              <td className="px-4 py-3 text-text-sec text-xs">{u.userEmail}</td>
              <td className="px-4 py-3 text-text-sec text-xs whitespace-nowrap">{u.roleName ?? "—"}</td>
              <td className="px-4 py-3 text-xs">
                {u.department && (
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${u.department === "QA" ? "bg-bg-accent text-lsg-blue-dark" : "bg-bg-tertiary text-text-sec"}`}>
                    {u.department}
                  </span>
                )}
              </td>
              <td className="px-4 py-3 text-text-sec text-xs whitespace-nowrap">{u.managerName ?? "—"}</td>
              <td className="px-4 py-3 text-text-sec text-xs">{u.clientIds?.length ?? 0} client{u.clientIds?.length !== 1 ? "s" : ""}</td>
              <td className="px-4 py-3">
                <div className="flex items-center gap-2 justify-end">
                  <button onClick={() => onEdit(u)} className="text-xs font-medium text-text-ter hover:text-lsg-blue transition-colors px-2 py-1 rounded hover:bg-bg-accent">Edit</button>
                  {inactive ? (
                    <button onClick={() => onReactivate(u.userId)} className="text-xs font-medium text-success-on hover:opacity-80 transition-colors px-2 py-1 rounded hover:bg-success-surface">Reactivate</button>
                  ) : (
                    <button onClick={() => onDeactivate(u)} className="text-xs font-medium text-error-on hover:opacity-80 transition-colors px-2 py-1 rounded hover:bg-error-surface">Deactivate</button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
