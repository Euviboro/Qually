/** @module api/questions */

import { api } from "./apiClient";

/**
 * @typedef {Object} CategoryOption
 * @property {string} value - Enum key (e.g. `"CUSTOMER"`).
 * @property {string} label - Human-readable label (e.g. `"Customer Critical"`).
 */

/**
 * @typedef {Object} SubattributeOptionRequestDTO
 * @property {string} optionLabel - Display label for this answer choice.
 */

/**
 * @typedef {Object} SubattributeOptionResponseDTO
 * @property {number} subattributeOptionId - Auto-generated ID.
 * @property {number} subattributeId       - ID of the parent subattribute.
 * @property {string} optionLabel          - Display label.
 */

/**
 * @typedef {Object} SubattributeRequestDTO
 * @property {string}                        subattributeText    - Title of this sub-criterion.
 * @property {SubattributeOptionRequestDTO[]} [subattributeOptions] - Answer choices.
 */

/**
 * @typedef {Object} SubattributeResponseDTO
 * @property {number}                         subattributeId      - Auto-generated ID.
 * @property {string}                         subattributeText    - Title of this sub-criterion.
 * @property {number}                         questionId          - ID of the parent question.
 * @property {SubattributeOptionResponseDTO[]} subattributeOptions - Answer choices.
 */

/**
 * @typedef {Object} AuditQuestionRequestDTO
 * @property {string}                   questionText  - Full audit question text.
 * @property {"CUSTOMER"|"BUSINESS"|"COMPLIANCE"} category - COPC category.
 * @property {number}                   protocolId    - ID of the owning protocol.
 * @property {SubattributeRequestDTO[]} [subattributes] - Nested sub-criteria.
 */

/**
 * @typedef {Object} AuditQuestionResponseDTO
 * @property {number}                    questionId   - Auto-generated ID.
 * @property {string}                    questionText - Full audit question text.
 * @property {"CUSTOMER"|"BUSINESS"|"COMPLIANCE"} category - COPC category.
 * @property {number}                    protocolId   - ID of the owning protocol.
 * @property {string}                    protocolName - Name of the owning protocol.
 * @property {SubattributeResponseDTO[]} subattributes - Nested sub-criteria.
 */

/**
 * Fetches the available COPC category enum options from the server.
 * Used to populate category dropdowns in the UI.
 *
 * @returns {Promise<CategoryOption[]>}
 */
export const getCategories = () => api.get("/enum/categories");

/**
 * Fully replaces an existing audit question's data (text, category, subattributes, options).
 * The server treats this like create — it clears and rebuilds all subattributes.
 *
 * @param {number} id                    - Question ID to update.
 * @param {AuditQuestionRequestDTO} dto  - Full updated payload.
 * @returns {Promise<AuditQuestionResponseDTO>}
 */
export const updateAuditQuestion = (id, dto) => api.put(`/questions/${id}`, dto);

/**
 * Creates a new audit question within a protocol.
 * Can be called from ShowProtocolPage when the protocol is still in DRAFT status.
 *
 * @param {AuditQuestionRequestDTO} dto - Question payload (must include `protocolId`).
 * @returns {Promise<AuditQuestionResponseDTO>}
 */
export const createAuditQuestion = (dto) => api.post("/questions", dto);