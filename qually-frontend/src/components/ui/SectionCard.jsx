/**
 * @module components/ui/SectionCard
 *
 * White card with a subtle border and shadow. Used as the primary content
 * grouping surface throughout forms and detail pages.
 */

/**
 * @param {Object}       props
 * @param {React.ReactNode} props.children   - Card content.
 * @param {string}       [props.className]   - Extra Tailwind classes (e.g. for borders, rings).
 */
export function SectionCard({ children, className = "" }) {
  return (
    <div className={`bg-bg-primary border border-border-ter rounded-xl p-6 shadow-card ${className}`}>
      {children}
    </div>
  );
}