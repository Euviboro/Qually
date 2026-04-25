/**
 * @fileoverview Shared application constants.
 *
 * Import individual exports rather than the whole module to keep
 * tree-shaking effective.
 */

// ── Answer values ─────────────────────────────────────────────
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

// ── Client card accent colours ────────────────────────────────
/**
 * Cycling palette for client card avatars.
 * Each entry has `bg` (surface), `text` (foreground), `dot` (indicator).
 * Values are plain hex / CSS colour strings applied as inline styles.
 *
 * @type {{ bg: string, text: string, dot: string }[]}
 */
export const CLIENT_ACCENT_COLORS = [
  { bg: "#E6F4FF", text: "#003D84", dot: "#0096FF" }, // LSG blue
  { bg: "#FFF0E6", text: "#7A3200", dot: "#FF8021" }, // LSG orange
  { bg: "#E1F5EE", text: "#085041", dot: "#1D9E75" }, // success green
  { bg: "#EDE9FF", text: "#3B0090", dot: "#7C3AED" }, // violet
  { bg: "#E6F0FF", text: "#002F65", dot: "#003D84" }, // deep navy
  { bg: "#FFF8E1", text: "#7A5200", dot: "#F59E0B" }, // amber
];

// ── Protocol status display metadata ─────────────────────────
/**
 * Display metadata for {@code AuditProtocol.protocolStatus} values.
 * {@code bgVar} and {@code textVar} are CSS custom-property names
 * (without the wrapping `var()`) used by components as inline styles.
 */
export const PROTOCOL_STATUS_META = {
  DRAFT: {
    label:   "Draft",
    bgVar:   "--color-bg-tertiary",
    textVar: "--color-text-ter",
  },
  FINALIZED: {
    label:   "Finalized",
    bgVar:   "--color-bg-accent",
    textVar: "--color-lsg-navy",
  },
  ARCHIVED: {
    label:   "Archived",
    bgVar:   "--color-bg-secondary",
    textVar: "--color-text-ter",
  },
};

// ── COPC category display metadata ───────────────────────────
/**
 * Display metadata for COPC audit question categories.
 * Variable names reference the `--color-copc-*` tokens defined in index.css.
 */
export const COPC_CATEGORY_META = {
  CUSTOMER: {
    label:   "Customer Critical",
    bgVar:   "--color-copc-customer-bg",
    textVar: "--color-copc-customer-text",
  },
  BUSINESS: {
    label:   "Business Critical",
    bgVar:   "--color-copc-business-bg",
    textVar: "--color-copc-business-text",
  },
  COMPLIANCE: {
    label:   "Compliance Critical",
    bgVar:   "--color-copc-compliance-bg",
    textVar: "--color-copc-compliance-text",
  },
};

// ── Audit session status display metadata ────────────────────
export const AUDIT_STATUS_META = {
  DRAFT:     { label: "Draft",     bg: "var(--color-bg-tertiary)",     text: "var(--color-text-ter)"      },
  COMPLETED: { label: "Completed", bg: "var(--color-success-surface)", text: "var(--color-success-on)"    },
  DISPUTED:  { label: "Disputed",  bg: "var(--color-error-surface)",   text: "var(--color-error-on)"      },
  RESOLVED:  { label: "Resolved",  bg: "var(--color-bg-accent)",       text: "var(--color-lsg-blue-dark)" },
};

// ── Audit logic type display metadata ────────────────────────
export const AUDIT_LOGIC_TYPE_META = {
  STANDARD: {
    label:       "Standard",
    description: "Each NO answer reduces the score proportionally.",
  },
  ACCOUNTABILITY: {
    label:       "Accountability",
    description: "Any single NO answer on an accountability question scores 0.",
  },
};
