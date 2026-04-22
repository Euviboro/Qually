/** @module hooks/useProtocol */

import { useAsync } from "./useAsync";
import { getProtocolById } from "../api/protocols";

/**
 * @typedef {Object} UseProtocolResult
 * @property {import('../api/protocols').AuditProtocolResponseDTO|null} protocol
 *   The fetched protocol (including nested questions and subattributes), or
 *   `null` while loading or on error.
 * @property {boolean}    loading - `true` on the initial fetch.
 * @property {string|null} error  - Error message if the fetch failed, otherwise `null`.
 * @property {(silent?: boolean) => void} refetch
 *   Re-fetches the protocol. Pass `silent = true` to avoid clearing the current
 *   data while refreshing (e.g. after saving a question).
 */

/**
 * Fetches a single protocol by ID, including all nested questions and subattributes.
 *
 * Wraps `useAsync` and re-fetches automatically whenever `id` changes.
 * Used by `ShowProtocolPage` to drive its entire data layer.
 *
 * @param {number|string} id - The protocol ID (typically from `useParams`).
 * @returns {UseProtocolResult}
 *
 * @example
 * const { protocol, loading, error, refetch } = useProtocol(id);
 */
export function useProtocol(id) {
  const { data, loading, error, refetch } = useAsync(
    () => getProtocolById(id),
    [id]
  );

  return { protocol: data, loading, error, refetch };
}