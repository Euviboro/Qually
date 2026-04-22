/**
 * @module components/ui/TextInput
 *
 * Controlled single-line text input styled with Qually design tokens.
 */

/**
 * @param {Object}   props
 * @param {string}   props.value            - Controlled value.
 * @param {(e: React.ChangeEvent<HTMLInputElement>) => void} props.onChange
 * @param {string}   [props.placeholder]    - Placeholder text.
 * @param {boolean}  [props.disabled]       - Disables the input and reduces opacity.
 * @param {boolean}  [props.autoFocus]      - Focuses the input on mount.
 * @param {string}   [props.className]      - Extra Tailwind classes for width overrides etc.
 */
export function TextInput({ value, onChange, placeholder, disabled, autoFocus, className = "" }) {
  return (
    <input
      type="text"
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      disabled={disabled}
      autoFocus={autoFocus}
      className={`w-full px-3 py-2 text-base rounded-md border border-border-sec
      bg-bg-primary text-text-pri font-sans outline-none transition-all
      placeholder:text-text-ter
      focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10
      disabled:bg-bg-secondary disabled:text-text-ter ${className}`}
    />
  );
}