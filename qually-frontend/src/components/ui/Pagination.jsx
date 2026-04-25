/**
 * @module components/ui/Pagination
 *
 * Numbered pagination control. Renders page number buttons with Previous /
 * Next flanking them. When there are many pages, collapses middle pages into
 * an ellipsis so the control never exceeds a fixed width.
 *
 * Always shows: first page, last page, current page, and up to 2 siblings
 * on either side of the current page. Everything else collapses to "…".
 *
 * @example
 * <Pagination
 *   currentPage={0}
 *   totalPages={12}
 *   onPageChange={(page) => setPage(page)}
 * />
 */

/**
 * @param {Object}   props
 * @param {number}   props.currentPage   - Zero-based current page index.
 * @param {number}   props.totalPages    - Total number of pages.
 * @param {(page: number) => void} props.onPageChange
 *   Called with the zero-based page index when the user clicks a page button.
 * @param {number}   [props.siblingCount=2]
 *   Number of page buttons to show on each side of the current page before
 *   collapsing to an ellipsis.
 */
export function Pagination({ currentPage, totalPages, onPageChange, siblingCount = 2 }) {
  if (totalPages <= 1) return null;

  const pages = buildPageList(currentPage, totalPages, siblingCount);

  return (
    <div className="flex items-center justify-center gap-1 py-4 select-none">

      {/* Previous */}
      <PageBtn
        label="←"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        title="Previous page"
      />

      {/* Numbered pages + ellipses */}
      {pages.map((item, idx) =>
        item === "…" ? (
          <span key={`ellipsis-${idx}`} className="px-2 py-1.5 text-sm text-text-ter select-none">
            …
          </span>
        ) : (
          <PageBtn
            key={item}
            label={String(item + 1)}   // display as 1-based
            onClick={() => onPageChange(item)}
            active={item === currentPage}
          />
        )
      )}

      {/* Next */}
      <PageBtn
        label="→"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        title="Next page"
      />
    </div>
  );
}

// ── Internal helpers ──────────────────────────────────────────

/**
 * Builds the ordered list of items to render.
 * Items are either a zero-based page number or the string "…".
 *
 * @param {number} current
 * @param {number} total
 * @param {number} siblings
 * @returns {(number|"…")[]}
 */
function buildPageList(current, total, siblings) {
  // Always show all pages when total is small enough
  if (total <= siblings * 2 + 5) {
    return Array.from({ length: total }, (_, i) => i);
  }

  const left  = Math.max(0, current - siblings);
  const right = Math.min(total - 1, current + siblings);

  const items = [];

  // First page
  items.push(0);

  // Left ellipsis
  if (left > 1) items.push("…");
  else if (left === 1) items.push(1);

  // Window around current page
  for (let i = left; i <= right; i++) {
    if (i !== 0 && i !== total - 1) items.push(i);
  }

  // Right ellipsis
  if (right < total - 2) items.push("…");
  else if (right === total - 2) items.push(total - 2);

  // Last page
  items.push(total - 1);

  // Deduplicate while preserving order (can happen at boundaries)
  return items.filter((item, idx, arr) => arr.indexOf(item) === idx);
}

/** Single page button. */
function PageBtn({ label, onClick, disabled = false, active = false, title }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={[
        "min-w-[32px] h-8 px-2 rounded-md text-sm font-medium transition-all",
        active
          ? "bg-lsg-blue text-white pointer-events-none"
          : disabled
            ? "text-text-ter cursor-not-allowed opacity-40"
            : "text-text-sec hover:bg-bg-tertiary hover:text-text-pri",
      ].join(" ")}
    >
      {label}
    </button>
  );
}
