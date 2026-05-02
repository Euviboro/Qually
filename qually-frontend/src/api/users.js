/** @module api/users */

import { api } from "./apiClient";

/**
 * @typedef {Object} UserResponseDTO
 * @property {number}  userId
 * @property {string}  userEmail
 * @property {string}  fullName
 * @property {number}  roleId
 * @property {string}  roleName
 * @property {"QA"|"OPERATIONS"} department
 * @property {number}  hierarchyLevel
 * @property {number}  managerId
 * @property {string}  managerName
 * @property {number[]} clientIds
 * @property {boolean} isActive
 * @property {boolean} canBeAudited
 * @property {boolean} canRaiseDispute
 */

export const getUsers = (params = {}) => {
  const qs = new URLSearchParams();
  if (params.roleName)   qs.set("roleName",   params.roleName);
  if (params.activeOnly) qs.set("activeOnly", "true");
  const query = qs.toString();
  return api.get(query ? `/users?${query}` : "/users");
};

/**
 * Returns active users with auditable roles for a specific client.
 * Used to populate the Member Audited dropdown in Log Session.
 *
 * @param {number} clientId
 * @returns {Promise<UserResponseDTO[]>}
 */
export const getAuditableUsers = (clientId) =>
  api.get(`/users?auditable=true&clientId=${clientId}`);

/**
 * Returns all users eligible to participate in a calibration round:
 * - All active QA users (any level)
 * - Active OPERATIONS Team Leaders and Supervisors (canBeAudited = false)
 *   assigned to the given client
 *
 * Results are ordered by department (QA first) then by name, matching
 * the grouped display in the Create Calibration wizard.
 *
 * @param {number} clientId
 * @returns {Promise<UserResponseDTO[]>}
 */
export const getCalibrationEligibleUsers = (clientId) =>
  api.get(`/users?calibrationEligible=true&clientId=${clientId}`);

export const getUserById    = (id)      => api.get(`/users/${id}`);
export const createUser     = (dto)     => api.post("/users", dto);
export const updateUser     = (id, dto) => api.put(`/users/${id}`, dto);
export const deactivateUser = (id)      => api.patch(`/users/${id}/deactivate`);
export const reactivateUser = (id)      => api.patch(`/users/${id}/reactivate`);