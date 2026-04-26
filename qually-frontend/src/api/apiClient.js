/** @module apiClient */

const base = import.meta.env.VITE_API_BASE;

/**
 * Core HTTP request wrapper. Reads `VITE_API_BASE` from the environment,
 * always sends JSON, and throws a descriptive `Error` on non-2xx responses.
 *
 * Includes the current user's ID as an `X-User-Id` header on every request
 * so the backend can apply permission checks for the dispute module without
 * a real authentication layer.
 *
 * @param {string} path    - API path relative to the base URL.
 * @param {RequestInit} [options={}]
 * @returns {Promise<unknown>} Parsed JSON response body, or null for empty responses.
 */
async function request(path, options = {}) {
  const userId = localStorage.getItem("userId");

  const res = await fetch(`${base}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(userId ? { "X-User-Id": userId } : {}),
      ...(options.headers ?? {}),
    },
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  // Some endpoints return 200/204 with no body (e.g. flag/unflag).
  // Calling .json() on an empty response throws "Unexpected end of JSON input".
  // Guard: only parse JSON when the response actually has a body.
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
  get:   (path)        => request(path),
  post:  (path, body)  => request(path, { method: "POST",  body: JSON.stringify(body) }),
  put:   (path, body)  => request(path, { method: "PUT",   body: JSON.stringify(body) }),
  patch: (path, body)  => request(path, {
    method: "PATCH",
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  }),
  delete: (path) => request(path, { method: "DELETE" }),
};