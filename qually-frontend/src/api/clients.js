import { api } from "./apiClient";

export const createClient = (data) => api.post("/clients", data);

export const getClients = () => api.get("/clients");