/** @module api/lobs */

import { api } from "./apiClient";

/**
 * @typedef {Object} LobResponseDTO
 * @property {number} lobId
 * @property {string} lobName
 * @property {number} clientId
 * @property {string} clientName
 */

/**
 * Fetches all LOBs, optionally filtered by client.
 *
 * @param {number} [clientId]
 * @returns {Promise<LobResponseDTO[]>}
 */
export const getLobs = (clientId) =>
  api.get(clientId ? `/teams?clientId=${clientId}` : "/teams");
