import { API_BASE } from "../constants";

export async function getUsers(role) {
  const url = role ? `${API_BASE}/users?role=${role}` : `${API_BASE}/users`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("Failed to load users");
  return res.json();
}

export const getUsers = () => api.get("/users");