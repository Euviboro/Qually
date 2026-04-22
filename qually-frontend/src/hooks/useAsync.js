/** @module hooks/useAsync */

import { useState, useCallback, useEffect, useRef } from "react";

/**
 * @template T
 * @typedef {Object} AsyncState
 * @property {T|null}   data    - Resolved value, or `null` while loading / on error.
 * @property {boolean}  loading - `true` while the async function is in flight.
 * @property {string|null} error - Error message string on failure, otherwise `null`.
 * @property {(silent?: boolean) => void} refetch - Re-runs the async function.
 *   Pass `silent = true` to keep the previous `data` visible during the reload
 *   (no loading-flash — useful for background refreshes).
 */

/**
 * Generic async data-fetching hook.
 *
 * Executes `asyncFn` on mount and whenever `deps` change. Manages loading,
 * error, and data state automatically, and exposes a `refetch` callback for
 * manual re-fetching. A mounted-guard prevents state updates after unmount.
 *
 * @template T
 * @param {() => Promise<T>} asyncFn - The async function to invoke.
 * @param {React.DependencyList} [deps=[]] - Dependency array. The function is
 *   re-executed whenever these values change (same semantics as `useEffect`).
 * @returns {AsyncState<T>}
 *
 * @example
 * const { data: protocols, loading, error, refetch } = useAsync(
 *   () => getProtocols(clientId),
 *   [clientId]
 * );
 */
export function useAsync(asyncFn, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const isMounted = useRef(false);

  const run = useCallback(async (silent = false) => {
    isMounted.current = true;
    // silent = true keeps previous data visible while reloading (no flash)
    setState((prev) => ({
      data: silent ? prev.data : null,
      loading: true,
      error: null,
    }));
    try {
      const data = await asyncFn();
      if (isMounted.current) setState({ data, loading: false, error: null });
    } catch (err) {
      if (isMounted.current) setState({ data: null, loading: false, error: err.message });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    run();
    return () => { isMounted.current = false; };
  }, [run]);

  return { ...state, refetch: run };
}