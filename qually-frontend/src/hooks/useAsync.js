import { useState, useCallback, useEffect, useRef } from "react";

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