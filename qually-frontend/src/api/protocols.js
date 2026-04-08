import { api } from "./apiClient";

// Protocols
export const getProtocols = (clientId) => 
  api.get(clientId ? `/protocols?clientId=${clientId}` : "/protocols");

export const getProtocolById = (id) => 
  api.get(`/protocols/${id}`);

export const createProtocol = (data) => 
  api.post("/protocols", data);

export const updateProtocol = (id, data) => 
  api.put(`/protocols/${id}`, data);

export const finalizeProtocol = (protocolId) => 
  api.patch(`/protocols/${protocolId}/finalize`);

// Questions (Ideally these should move to src/api/questions.js eventually)
export const getQuestions = (protocolId) => 
  api.get(`/questions?protocolId=${protocolId}`);

export const createQuestion = (data) => 
  api.post("/questions", data);