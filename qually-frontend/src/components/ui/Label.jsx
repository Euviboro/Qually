/**
 * @module components/ui/Label
 *
 * Form field label with optional required indicator.
 */

/**
 * @param {Object}       props
 * @param {React.ReactNode} props.children - Label text.
 * @param {boolean}      [props.required]  - When `true`, appends a blue asterisk.
 */
export function Label({ children, required }) {
  return (
    <p className="mb-1.5 text-sm font-semibold text-text-sec tracking-wide">
      {children}
      {required && <span className="text-lsg-blue ml-1">*</span>}
    </p>
  );
}