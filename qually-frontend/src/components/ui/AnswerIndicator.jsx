/**
 * @module components/ui/AnswerIndicator
 *
 * Displays an audit answer (YES / NO / N/A) with a consistent visual treatment.
 *
 * - YES  → "Yes" label + green checkmark icon (right side)
 * - NO   → "No" label + red X icon (right side)
 * - N/A  → plain "N/A" text, neutral styling, no icon
 */

import { ANSWERS } from "../../constants";

/**
 * @param {Object} props
 * @param {"YES"|"NO"|"N/A"|null|undefined} props.answer
 * @param {"sm"|"md"} [props.size="md"]
 */
export function AnswerIndicator({ answer, size = "md" }) {
  const textCls  = size === "sm" ? "text-xs" : "text-sm";
  const iconSize = size === "sm" ? 13 : 15;

  if (!answer) {
    return <span className={`${textCls} text-text-ter`}>—</span>;
  }

  if (answer === ANSWERS.NA) {
    return <span className={`${textCls} font-medium text-text-ter`}>N/A</span>;
  }

  if (answer === ANSWERS.YES) {
    return (
      <span className={`inline-flex items-center gap-1.5 ${textCls} font-semibold text-success-on`}>
        Yes
        <svg width={iconSize} height={iconSize} viewBox="0 0 15 15" fill="none" aria-label="Yes">
          <circle cx="7.5" cy="7.5" r="7" fill="var(--color-success-dot)" opacity="0.15" />
          <path d="M4 7.5l2.5 2.5 5-5"
            stroke="var(--color-success-dot)" strokeWidth="1.6"
            strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </span>
    );
  }

  if (answer === ANSWERS.NO) {
    return (
      <span className={`inline-flex items-center gap-1.5 ${textCls} font-semibold text-error-on`}>
        No
        <svg width={iconSize} height={iconSize} viewBox="0 0 15 15" fill="none" aria-label="No">
          <circle cx="7.5" cy="7.5" r="7" fill="var(--color-error)" opacity="0.15" />
          <path d="M5 5l5 5M10 5l-5 5"
            stroke="var(--color-error)" strokeWidth="1.6" strokeLinecap="round" />
        </svg>
      </span>
    );
  }

  return <span className={`${textCls} text-text-ter`}>{answer}</span>;
}
