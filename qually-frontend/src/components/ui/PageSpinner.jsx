/**
 * @module components/ui/PageSpinner
 *
 * Full-height centered loading indicator. Rendered by `Suspense` boundaries
 * during lazy-loaded page transitions and by individual pages while their data
 * is in flight.
 */

/**
 * Renders a spinning ring vertically centered within a 60 vh container.
 * No props required.
 */
export function PageSpinner() {
  return (
    <div className="flex items-center justify-center h-[60vh]">
      <div className="w-6 h-6 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
    </div>
  );
}