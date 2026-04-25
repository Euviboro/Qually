/** @module api/results */

import { api } from "./apiClient";

/**
 * @typedef {Object} QuestionAnswerDTO
 * @property {number} questionId
 * @property {string} questionText
 * @property {string} category
 * @property {string} effectiveAnswer
 * @property {string} responseStatus
 */

/**
 * @typedef {Object} ResultsTableRowDTO
 * @property {number}  sessionId
 * @property {string}  interactionId
 * @property {string}  sessionDate
 * @property {number}  clientId
 * @property {string}  clientName
 * @property {number}  lobId
 * @property {string}  lobName
 * @property {number}  protocolId
 * @property {string}  protocolName
 * @property {number}  memberAuditedUserId
 * @property {string}  memberAuditedName
 * @property {number}  auditorUserId
 * @property {string}  auditorName
 * @property {number|null} customerScore
 * @property {number|null} businessScore
 * @property {number|null} complianceScore
 * @property {string}  auditStatus
 * @property {QuestionAnswerDTO[]|null} questionAnswers
 */

/**
 * @typedef {Object} PagedResultsResponseDTO
 * @property {ResultsTableRowDTO[]} content       - Rows for the current page.
 * @property {number}               totalElements - Total matching rows across all pages.
 * @property {number}               totalPages    - Total number of pages.
 * @property {number}               currentPage   - Zero-based current page index.
 * @property {number}               pageSize      - Rows per page as accepted by the server.
 */

/**
 * Fetches a paginated page of result rows visible to the current user.
 *
 * @param {{
 *   protocolId?:    number,
 *   clientId?:      number,
 *   auditorId?:     number,
 *   memberId?:      number,
 *   includeAnswers?: boolean,
 *   page?:          number,
 *   size?:          number,
 * }} [params]
 * @returns {Promise<PagedResultsResponseDTO>}
 */
export const getResults = (params = {}) => {
  const qs = new URLSearchParams();
  if (params.protocolId)    qs.set("protocolId",     params.protocolId);
  if (params.clientId)      qs.set("clientId",        params.clientId);
  if (params.auditorId)     qs.set("auditorId",       params.auditorId);
  if (params.memberId)      qs.set("memberId",        params.memberId);
  if (params.includeAnswers) qs.set("includeAnswers", "true");
  if (params.page != null)  qs.set("page",            params.page);
  if (params.size != null)  qs.set("size",            params.size);
  const query = qs.toString();
  return api.get(query ? `/results?${query}` : "/results");
};
