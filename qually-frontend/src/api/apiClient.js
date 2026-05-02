/** @module apiClient */

const base = import.meta.env.VITE_API_BASE;

/**
 * Whether a token refresh is already in progress.
 * Prevents multiple parallel refresh calls when several requests 401 at once.
 */
let refreshing = false;
let refreshSubscribers = [];

function onRefreshed() {
  refreshSubscribers.forEach((cb) => cb());
  refreshSubscribers = [];
}

function waitForRefresh() {
  return new Promise((resolve) => refreshSubscribers.push(resolve));
}

/**
 * Attempts to refresh the access token using the refresh cookie.
 * Returns true on success, false on failure.
 */
async function attemptRefresh() {
  try {
    const res = await fetch(`${base}/auth/refresh`, {
      method:      "POST",
      credentials: "include",
      headers:     { "Content-Type": "application/json" },
    });
    return res.ok;
  } catch {
    return false;
  }
}

/**
 * Core HTTP request wrapper.
 *
 * CSRF is handled by SameSite=Strict on the access_token cookie — the
 * browser will never send it on cross-site requests, eliminating the
 * attack vector. No CSRF token header is needed.
 *
 * @param {string} path
 * @param {RequestInit} [options={}]
 * @param {boolean} [isRetry=false] - Internal flag to prevent infinite retry loops.
 * @returns {Promise<unknown>} Parsed JSON body or null for empty responses.
 */
async function request(path, options = {}, isRetry = false) {
  const res = await fetch(`${base}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers ?? {}),
    },
  });

  // ── 401 handling ──────────────────────────────────────────
  if (res.status === 401 && !isRetry) {
    const body = await res.json().catch(() => ({}));

    if (body.code === "TOKEN_EXPIRED") {
      if (refreshing) {
        await waitForRefresh();
        return request(path, options, true);
      }

      refreshing = true;
      const success = await attemptRefresh();
      refreshing = false;
      onRefreshed();

      if (success) {
        return request(path, options, true);
      }
    }

    window.dispatchEvent(new CustomEvent("qually:logout"));
    throw new Error("Session expired. Please log in again.");
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  // Guard against empty bodies (e.g. flag/unflag returning 200 with no body)
  const contentType   = res.headers.get("Content-Type") ?? "";
  const contentLength = res.headers.get("Content-Length");

  if (
    res.status === 204 ||
    contentLength === "0" ||
    !contentType.includes("application/json")
  ) {
    return null;
  }

  return res.json();
}

export const api = {
  get:    (path)       => request(path),
  post:   (path, body) => request(path, { method: "POST",  body: JSON.stringify(body) }),
  put:    (path, body) => request(path, { method: "PUT",   body: JSON.stringify(body) }),
  patch:  (path, body) => request(path, {
    method: "PATCH",
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  }),
  delete: (path)       => request(path, { method: "DELETE" }),
};