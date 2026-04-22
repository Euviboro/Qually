/** @module api/clients */

import { api } from "./apiClient";

/**
 * @typedef {Object} ClientRequestDTO
 * @property {string} clientName - Display name of the new client.
 */

/**
 * @typedef {Object} ClientResponseDTO
 * @property {number} clientId   - Auto-generated unique identifier.
 * @property {string} clientName - Display name of the client.
 */

/**
 * Creates a new client.
 *
 * @param {ClientRequestDTO} data - New client payload.
 * @returns {Promise<ClientResponseDTO>} The created client.
 */
export const createClient = (data) => api.post("/clients", data);

/**
 * Fetches all clients.
 *
 * @returns {Promise<ClientResponseDTO[]>} Array of all clients.
 */
export const getClients = () => api.get("/clients");