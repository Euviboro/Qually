/** @module constants */

/** Base URL for all API requests, injected by Vite from `.env`. */
export const API_BASE = import.meta.env.VITE_API_BASE;

/**
 * Valid answer values for audit questions.
 * Mirrors {@code AuditAnswerType} on the backend.
 *
 * @example
 * import { ANSWERS } from '../../constants';
 * if (answer === ANSWERS.NO) { ... }
 */
export const ANSWERS = {
  YES: "YES",
  NO:  "NO",
  NA:  "N/A",
};

/**
 * Display metadata for each COPC category enum value.
 * Keys match the backend `CopcCategory` enum exactly.
 *
 * @type {Record<string, { label: string, bgVar: string, textVar: string, dotVar: string }>}
 */
export const COPC_CATEGORY_META = {
  CUSTOMER:   { label: "Customer Critical",   bgVar: "--color-copc-customer-bg",   textVar: "--color-copc-customer-text",   dotVar: "--color-copc-customer-dot"   },
  BUSINESS:   { label: "Business Critical",   bgVar: "--color-copc-business-bg",   textVar: "--color-copc-business-text",   dotVar: "--color-copc-business-dot"   },
  COMPLIANCE: { label: "Compliance Critical", bgVar: "--color-copc-compliance-bg", textVar: "--color-copc-compliance-text", dotVar: "--color-copc-compliance-dot" },
};

/**
 * Display metadata for each `ProtocolStatus` enum value.
 * Keys match the backend `ProtocolStatus` enum exactly.
 *
 * @type {Record<string, { label: string, bgVar: string, textVar: string }>}
 */
export const PROTOCOL_STATUS_META = {
  DRAFT:     { label: "Draft",     bgVar: "--color-warning-bg",          textVar: "--color-warning-text"    },
  FINALIZED: { label: "Finalized", bgVar: "--color-success-bg",          textVar: "--color-success-text"    },
  ARCHIVED:  { label: "Archived",  bgVar: "--color-background-tertiary", textVar: "--color-text-tertiary"   },
};

/**
 * Display metadata for each `AuditStatus` enum value.
 * Keys match the backend `AuditStatus` enum exactly:
 * `DRAFT`, `COMPLETED`, `DISPUTED`, `RESOLVED`.
 *
 * @type {Record<string, { label: string, bg: string, text: string }>}
 */
export const AUDIT_STATUS_META = {
  DRAFT:     { label: "Draft",     bg: "var(--color-warning-bg)",          text: "var(--color-warning-text)"    },
  COMPLETED: { label: "Completed", bg: "var(--color-success-bg)",          text: "var(--color-success-text)"    },
  DISPUTED:  { label: "Disputed",  bg: "var(--color-error-bg)",            text: "var(--color-error-text)"      },
  RESOLVED:  { label: "Resolved",  bg: "var(--color-background-tertiary)", text: "var(--color-text-secondary)"  },
};

/**
 * Display metadata for each `AuditLogicType` enum value.
 *
 * @type {Record<string, { label: string, description: string }>}
 */
export const AUDIT_LOGIC_TYPE_META = {
  STANDARD:       { label: "Standard",       description: "Any NO marks the category as 0"                    },
  ACCOUNTABILITY: { label: "Accountability", description: "Only company-accountable NOs affect the score"     },
};

/**
 * Rotating palette used to visually distinguish client cards on the Dashboard.
 * All values are CSS variable references so they respect the design token system.
 *
 * @type {{ bg: string, text: string, dot: string }[]}
 */
export const CLIENT_ACCENT_COLORS = [
  { bg: "var(--color-copc-customer-bg)",    text: "var(--color-copc-customer-text)",    dot: "var(--color-copc-customer-dot)"    },
  { bg: "var(--color-background-accent)",   text: "var(--lsg-trust-navy-mid)",          dot: "var(--lsg-lean-blue-dark)"         },
  { bg: "var(--color-copc-business-bg)",    text: "var(--color-copc-business-text)",    dot: "var(--color-copc-business-dot)"    },
  { bg: "var(--color-copc-compliance-bg)",  text: "var(--color-copc-compliance-text)",  dot: "var(--color-copc-compliance-dot)"  },
  { bg: "var(--color-background-tertiary)", text: "var(--color-text-secondary)",        dot: "var(--color-text-tertiary)"        },
  { bg: "var(--color-background-accent)",   text: "var(--lsg-midnight)",                dot: "var(--lsg-lean-blue)"              },
];