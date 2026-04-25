/** @module hooks/useAsync */

import { useState, useCallback, useEffect, useRef } from "react";

/**
 * @template T
 * @typedef {Object} AsyncState
 * @property {T|undefined} data - Resolved value, or `undefined` while loading / on error.
 *   Using `undefined` (not `null`) means destructuring defaults work as expected:
 *   `const { data: rows = [] } = useAsync(...)` correctly falls back to `[]`
 *   before the first response arrives.
 * @property {boolean}     loading - `true` while the async function is in flight.
 * @property {string|null} error   - Error message string on failure, otherwise `null`.
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
 * **Why `undefined` instead of `null` for the initial `data` value:**
 * JavaScript destructuring defaults (`= []`, `= {}`) only apply when the
 * value is `undefined` — not when it is `null`. Initialising with `null`
 * caused `.map()` / `.filter()` crashes in every component that used a
 * destructuring default to guard against the loading state. Changing to
 * `undefined` makes the hook behave the way callers naturally expect.
 *
 * @template T
 * @param {() => Promise<T>} asyncFn - The async function to invoke.
 * @param {React.DependencyList} [deps=[]] - Dependency array. The function is
 *   re-executed whenever these values change (same semantics as `useEffect`).
 * @returns {AsyncState<T>}
 *
 * @example
 * // rows is [] during loading — no null-check needed before .map()
 * const { data: rows = [], loading, error } = useAsync(getResults, []);
 */
export function useAsync(asyncFn, deps = []) {
  // data initialises as `undefined` so destructuring defaults (`= []`, `= {}`)
  // apply correctly on the first render before the fetch resolves.
  const [state, setState] = useState({ data: undefined, loading: true, error: null });
  const isMounted = useRef(false);

  const run = useCallback(async (silent = false) => {
    isMounted.current = true;
    setState((prev) => ({
      // silent = true keeps previous data visible while reloading (no flash).
      // Non-silent reset uses `undefined` (not `null`) for the same reason as
      // the initial state — so any component relying on a destructuring default
      // gets the fallback value rather than a null that bypasses the default.
      data: silent ? prev.data : undefined,
      loading: true,
      error: null,
    }));
    try {
      const data = await asyncFn();
      if (isMounted.current) setState({ data, loading: false, error: null });
    } catch (err) {
      // On error, data is `undefined` so callers using `= []` / `= {}` defaults
      // still get a safe empty value instead of null.
      if (isMounted.current) setState({ data: undefined, loading: false, error: err.message });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    run();
    return () => { isMounted.current = false; };
  }, [run]);

  return { ...state, refetch: run };
}
