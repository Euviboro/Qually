/** @module api/users */

import { api } from "./apiClient";

/**
 * @typedef {Object} UserResponseDTO
 * @property {number}      userId
 * @property {string}      userEmail
 * @property {string}      fullName
 * @property {number|null} roleId
 * @property {string|null} roleName
 * @property {"QA"|"OPERATIONS"|null} department
 * @property {number|null} managerId
 * @property {string|null} managerName
 */

/**
 * @typedef {Object} UserRequestDTO
 * @property {string}      userEmail
 * @property {string}      fullName
 * @property {number}      [roleId]
 * @property {number}      [managerId]
 */

/**
 * Fetches all users, optionally filtered by role name.
 *
 * @param {string} [roleName] - e.g. `"QA_SPECIALIST"`
 * @returns {Promise<UserResponseDTO[]>}
 */
export const getUsers = (roleName) =>
  api.get(roleName ? `/users?roleName=${encodeURIComponent(roleName)}` : "/users");

/**
 * Fetches a single user by their integer ID.
 *
 * @param {number} id
 * @returns {Promise<UserResponseDTO>}
 */
export const getUserById = (id) =>
  api.get(`/users/${id}`);

/**
 * Creates a new user.
 *
 * @param {UserRequestDTO} dto
 * @returns {Promise<UserResponseDTO>}
 */
export const createUser = (dto) =>
  api.post("/users", dto);

/**
 * Updates an existing user's mutable fields.
 *
 * @param {number} id
 * @param {{ fullName?: string, roleId?: number, managerId?: number }} dto
 * @returns {Promise<UserResponseDTO>}
 */
export const updateUser = (id, dto) =>
  api.put(`/users/${id}`, dto);
