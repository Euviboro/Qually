import { API_BASE } from "../constants";

export async function getSessions({ auditorEmail, auditStatus } = {}) {
  const params = new URLSearchParams();
  if (auditorEmail) params.set("auditorEmail", auditorEmail);
  if (auditStatus) params.set("auditStatus", auditStatus);
  const res = await fetch(`${API_BASE}/sessions?${params}`);
  if (!res.ok) throw new Error("Failed to load sessions");
  return res.json();
}
 
export async function createSession(data) {
  const res = await fetch(`${API_BASE}/sessions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to create session");
  return res.json();
}
 
export async function updateSession(sessionId, data) {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to update session");
  return res.json();
}
 
export async function submitBulkResponses(sessionId, responses) {
  const res = await fetch(`${API_BASE}/responses/bulk`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sessionId, responses }),
  });
  if (!res.ok) throw new Error("Failed to submit responses");
  return res.json();
}
 
export async function getResponsesBySession(sessionId) {
  const res = await fetch(`${API_BASE}/responses?sessionId=${sessionId}`);
  if (!res.ok) throw new Error("Failed to load responses");
  return res.json();
}
