/** @module apiClient */

const base = import.meta.env.VITE_API_BASE;

/**
 * Reads the XSRF-TOKEN cookie that Spring Security sets automatically.
 * The value is sent back as X-XSRF-Token on every mutating request.
 *
 * @returns {string|null}
 */
function getCsrfToken() {
  const match = document.cookie
    .split("; ")
    .find((row) => row.startsWith("XSRF-TOKEN="));
  return match ? decodeURIComponent(match.split("=")[1]) : null;
}

const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

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
      headers: {
        "Content-Type": "application/json",
        ...(getCsrfToken() ? { "X-XSRF-Token": getCsrfToken() } : {}),
      },
    });
    return res.ok;
  } catch {
    return false;
  }
}

/**
 * Core HTTP request wrapper.
 *
 * Changes from the previous version:
 * - X-User-Id header removed — identity comes from the JWT access_token cookie
 * - credentials: "include" added — sends httpOnly cookies on every request
 * - X-XSRF-Token header added to all mutating requests (POST/PUT/PATCH/DELETE)
 *   to satisfy Spring Security's CSRF protection
 * - 401 interceptor: TOKEN_EXPIRED triggers a silent refresh and retry once;
 *   TOKEN_INVALID triggers logout and redirect to /login
 *
 * @param {string} path
 * @param {RequestInit} [options={}]
 * @param {boolean} [isRetry=false] - Internal flag to prevent infinite retry loops.
 * @returns {Promise<unknown>} Parsed JSON body or null for empty responses.
 */
async function request(path, options = {}, isRetry = false) {
  const method = (options.method ?? "GET").toUpperCase();
  const csrfToken = getCsrfToken();

  const res = await fetch(`${base}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(MUTATING_METHODS.has(method) && csrfToken
        ? { "X-XSRF-Token": csrfToken }
        : {}),
      ...(options.headers ?? {}),
    },
  });

  // ── 401 handling ──────────────────────────────────────────
  if (res.status === 401 && !isRetry) {
    const body = await res.json().catch(() => ({}));

    if (body.code === "TOKEN_EXPIRED") {
      // Refresh in flight — wait for it rather than triggering another
      if (refreshing) {
        await waitForRefresh();
        return request(path, options, true);
      }

      refreshing = true;
      const success = await attemptRefresh();
      refreshing = false;
      onRefreshed();

      if (success) {
        // Retry the original request with the new access token cookie
        return request(path, options, true);
      }
    }

    // TOKEN_INVALID or refresh failed — log out and redirect
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