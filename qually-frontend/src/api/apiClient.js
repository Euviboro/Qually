/** @module apiClient */

const base = import.meta.env.VITE_API_BASE;

/**
 * Core HTTP request wrapper. Reads `VITE_API_BASE` from the environment,
 * always sends JSON, and throws a descriptive `Error` on non-2xx responses.
 *
 * @param {string} path    - API path relative to the base URL (e.g. `/protocols`).
 * @param {RequestInit} [options={}] - Additional `fetch` options (method, body, etc.).
 * @returns {Promise<unknown>} Parsed JSON response body.
 * @throws {Error} When the server returns a non-2xx status. The message is taken
 *   from the JSON body's `message` field when available, otherwise falls back to
 *   `"HTTP <status>"`.
 */
async function request(path, options = {}) {
  const res = await fetch(`${base}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  return res.json();
}

/**
 * Thin API client with convenience methods for each HTTP verb.
 * All methods return a Promise that resolves to the parsed JSON response
 * body directly — NOT an axios-style `{ data }` wrapper.
 *
 * @namespace api
 */
export const api = {
  /**
   * Sends a GET request.
   * @param {string} path - API path (e.g. `/protocols?clientId=1`).
   * @returns {Promise<unknown>}
   */
  get: (path) => request(path),

  /**
   * Sends a POST request with a JSON body.
   * @param {string} path - API path.
   * @param {unknown} body - Payload to serialize as JSON.
   * @returns {Promise<unknown>}
   */
  post: (path, body) => request(path, { method: "POST", body: JSON.stringify(body) }),

  /**
   * Sends a PUT request with a JSON body.
   * @param {string} path - API path.
   * @param {unknown} body - Payload to serialize as JSON.
   * @returns {Promise<unknown>}
   */
  put: (path, body) => request(path, { method: "PUT", body: JSON.stringify(body) }),

  /**
   * Sends a PATCH request with an optional JSON body.
   *
   * Accepts an optional `body` for partial-update endpoints (e.g. PATCH /protocols/:id/name).
   * When `body` is omitted the request is sent without a payload, which is correct
   * for state-transition endpoints like PATCH /protocols/:id/finalize.
   *
   * @param {string}  path    - API path.
   * @param {unknown} [body]  - Optional payload to serialize as JSON.
   * @returns {Promise<unknown>}
   */
  patch: (path, body) =>
    request(path, {
      method: "PATCH",
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    }),
};