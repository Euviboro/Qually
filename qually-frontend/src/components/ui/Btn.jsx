/**
 * @module components/ui/Btn
 *
 * General-purpose button component that maps a semantic `variant` and `size`
 * to the Qually design-token utility classes defined in `index.css`.
 */

/**
 * @typedef {"primary"|"secondary"|"ghost"|"danger"|"success"} BtnVariant
 * @typedef {"sm"|"md"} BtnSize
 */

/**
 * @param {Object}       props
 * @param {React.ReactNode} props.children  - Button label / content.
 * @param {() => void}   [props.onClick]    - Click handler. Not called when `disabled`.
 * @param {BtnVariant}   [props.variant="secondary"] - Visual style.
 * @param {BtnSize}      [props.size="md"]  - Padding / font-size scale.
 * @param {boolean}      [props.disabled]   - Suppresses clicks and applies reduced opacity.
 * @param {string}       [props.className]  - Extra Tailwind classes for one-off overrides.
 */
export function Btn({ children, onClick, variant = "secondary", disabled, size = "md", className = "" }) {
  const variants = {
    primary:   "bg-lsg-blue text-white hover:bg-lsg-blue-dark shadow-sm",
    secondary: "bg-bg-secondary text-text-pri border border-border-sec hover:bg-bg-tertiary",
    ghost:     "bg-transparent text-text-ter border border-border-sec hover:bg-bg-secondary",
    danger:    "bg-error-surface text-error-on hover:opacity-90",
    success:   "bg-success-surface text-success-on hover:opacity-90",
  };
  const sizes = {
    sm: "text-xs px-3 py-1.5",
    md: "text-base px-4 py-2",
  };

  return (
    <button
      onClick={disabled ? undefined : onClick}
      className={`inline-flex items-center gap-2 rounded-md font-medium transition-all whitespace-nowrap
      ${variants[variant]} ${sizes[size]} ${disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer"} ${className}`}
    >
      {children}
    </button>
  );
}