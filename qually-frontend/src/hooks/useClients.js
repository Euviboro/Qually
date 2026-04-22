/** @module hooks/useClients */

import { useAsync } from "./useAsync";
import { getClients } from "../api/clients";

/**
 * @typedef {Object} UseClientsResult
 * @property {import('../api/clients').ClientResponseDTO[]} clients
 *   Stable array of all clients. Never `null` — resolves to `[]` while loading.
 * @property {boolean} loading - `true` on the initial fetch.
 * @property {string|null} error - Error message if the fetch failed, otherwise `null`.
 * @property {() => void} refresh - Silently re-fetches the client list without
 *   clearing the current data (no loading-flash). Call after creating a new client.
 */

/**
 * Fetches and manages the list of all clients.
 *
 * Wraps `useAsync` for the `/clients` endpoint, exposing a convenience
 * `refresh()` method that uses the silent-reload strategy so the grid
 * doesn't flash blank while updating.
 *
 * @returns {UseClientsResult}
 *
 * @example
 * const { clients, loading, error, refresh } = useClients();
 */
export function useClients() {
  const { data, loading, error, refetch } = useAsync(getClients, []);

  return {
    clients: data ?? [],       // never null — components can map directly
    loading,
    error,
    refresh: () => refetch(true), // silent: no spinner flash on re-fetch
  };
}