import { api } from "./apiClient";

/**
 * @typedef {Object} CategoryOption
 * @property {string} value
 * @property {string} label
 */

/**
 * @typedef {Object} OptionDTO
 * @property {number} [optionId]
 * @property {string} optionText
 */

/**
 * @typedef {Object} SubattributeDTO
 * @property {number} [subattributeId]
 * @property {string} subattributeText
 * @property {OptionDTO[]} options
 */

/**
 * @typedef {Object} AuditQuestionDTO
 * @property {number} questionId
 * @property {string} questionText
 * @property {string} category
 * @property {SubattributeDTO[]} subattributes
 */

/**
 * Fetches available COPC category options for the category selector.
 * @returns {Promise<CategoryOption[]>}
 */
export const getCategories = () => api.get("/enum/categories");

/**
 * Updates an existing audit question.
 * @param {number} id
 * @param {AuditQuestionDTO} dto
 * @returns {Promise<AuditQuestionDTO>}
 */
export const updateAuditQuestion = (id, dto) => api.put(`/questions/${id}`, dto);

/**
 * Creates a new audit question within a protocol.
 * @param {AuditQuestionDTO} dto
 * @returns {Promise<AuditQuestionDTO>}
 */
export const createAuditQuestion = (dto) => api.post("/questions", dto);