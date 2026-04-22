/**
 * @module components/ui/SearchableSelect
 *
 * A fully controlled, searchable dropdown built on CSS design tokens.
 * Reusable anywhere in the app — clients, protocols, users, LOBs, etc.
 *
 * Features:
 * - Keyboard accessible: Escape closes the list, Enter selects the
 *   highlighted option, Arrow keys navigate the visible results.
 * - Outside-click dismissal via a document mousedown listener.
 * - Loading and disabled states with appropriate visual feedback.
 * - Clears the search input when the dropdown opens so the user starts
 *   with the full list rather than a stale previous query.
 */

import { useState, useEffect, useRef, useCallback } from "react";

/**
 * A single option displayed in the dropdown list.
 *
 * @typedef {Object} SelectOption
 * @property {string|number} value - The value emitted via `onChange`.
 * @property {string}        label - The human-readable display string.
 */

/**
 * @param {Object}         props
 * @param {SelectOption[]} props.options
 *   Full list of options to render. Filtering is applied internally.
 * @param {string|number|null} props.value
 *   Currently selected value. `null` when nothing is selected.
 * @param {(value: string|number) => void} props.onChange
 *   Called with the selected option's `value` when the user picks an item.
 * @param {string}  [props.placeholder="Select…"]
 *   Placeholder shown in the trigger when nothing is selected.
 * @param {string}  [props.searchPlaceholder="Search…"]
 *   Placeholder inside the search input when the list is open.
 * @param {boolean} [props.loading=false]
 *   When `true`, shows a spinner inside the trigger and disables interaction.
 * @param {boolean} [props.disabled=false]
 *   When `true`, the select is non-interactive and visually dimmed.
 * @param {string}  [props.emptyMessage="No results found"]
 *   Message shown when the search query matches nothing.
 * @param {string}  [props.className=""]
 *   Extra classes applied to the root wrapper `<div>`.
 */
export function SearchableSelect({
  options = [],
  value = null,
  onChange,
  placeholder = "Select…",
  searchPlaceholder = "Search…",
  loading = false,
  disabled = false,
  emptyMessage = "No results found",
  className = "",
}) {
  const [open,       setOpen]       = useState(false);
  const [query,      setQuery]      = useState("");
  const [highlight,  setHighlight]  = useState(0);

  const wrapperRef   = useRef(null);
  const searchRef    = useRef(null);
  const listRef      = useRef(null);

  /** The label of the currently selected option, or null. */
  const selectedLabel = options.find((o) => o.value === value)?.label ?? null;

  /** Options filtered by the current search query (case-insensitive). */
  const filtered = query.trim()
    ? options.filter((o) =>
        o.label.toLowerCase().includes(query.toLowerCase())
      )
    : options;

  // ── Open / close ──────────────────────────────────────────

  const openList = () => {
    if (disabled || loading) return;
    setQuery("");        // reset search so user sees the full list
    setHighlight(0);
    setOpen(true);
  };

  const closeList = useCallback(() => {
    setOpen(false);
    setQuery("");
  }, []);

  /** Select an option and close the list. */
  const pick = useCallback((option) => {
    onChange(option.value);
    closeList();
  }, [onChange, closeList]);

  // Auto-focus the search input when the list opens
  useEffect(() => {
    if (open) searchRef.current?.focus();
  }, [open]);

  // Close on outside click
  useEffect(() => {
    const handler = (e) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        closeList();
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [closeList]);

  // ── Keyboard navigation ───────────────────────────────────

  const handleKeyDown = (e) => {
    if (!open) {
      if (e.key === "Enter" || e.key === " " || e.key === "ArrowDown") {
        e.preventDefault();
        openList();
      }
      return;
    }
    if (e.key === "Escape") {
      e.preventDefault();
      closeList();
      return;
    }
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlight((h) => Math.min(h + 1, filtered.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlight((h) => Math.max(h - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (filtered[highlight]) pick(filtered[highlight]);
    }
  };

  // Keep highlighted item scrolled into view
  useEffect(() => {
    if (!listRef.current) return;
    const item = listRef.current.children[highlight];
    item?.scrollIntoView({ block: "nearest" });
  }, [highlight]);

  // Reset highlight when the filtered list changes
  useEffect(() => {
    setHighlight(0);
  }, [query]);

  // ── Render ────────────────────────────────────────────────

  return (
    <div
      ref={wrapperRef}
      className={`relative ${className}`}
      onKeyDown={handleKeyDown}
    >
      {/* ── Trigger button ─────────────────────────────────── */}
      <button
        type="button"
        onClick={open ? closeList : openList}
        disabled={disabled || loading}
        aria-haspopup="listbox"
        aria-expanded={open}
        className={[
          "w-full flex items-center justify-between gap-2 px-3 py-2 text-sm rounded-md border",
          "bg-bg-primary transition-all text-left",
          open
            ? "border-lsg-blue ring-3 ring-lsg-blue/10"
            : "border-border-sec hover:border-border-pri",
          selectedLabel ? "text-text-pri" : "text-text-ter",
          (disabled || loading) ? "opacity-50 cursor-not-allowed" : "cursor-pointer",
        ].join(" ")}
      >
        <span className="truncate flex-1">
          {loading ? "Loading…" : selectedLabel ?? placeholder}
        </span>

        <span className="shrink-0 flex items-center gap-1.5">
          {/* Clear button — visible only when a value is selected */}
          {value !== null && !disabled && !loading && (
            <span
              role="button"
              tabIndex={-1}
              onClick={(e) => {
                e.stopPropagation();
                onChange(null);
              }}
              className="text-text-ter hover:text-text-sec transition-colors leading-none"
              aria-label="Clear selection"
            >
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M2 2l8 8M10 2l-8 8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
              </svg>
            </span>
          )}

          {/* Loading spinner / chevron */}
          {loading ? (
            <span className="w-3.5 h-3.5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
          ) : (
            <svg
              className={`w-3.5 h-3.5 text-text-ter transition-transform duration-200 ${open ? "rotate-180" : ""}`}
              viewBox="0 0 12 12" fill="none"
            >
              <path d="M2 4l4 4 4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          )}
        </span>
      </button>

      {/* ── Dropdown panel ─────────────────────────────────── */}
      {open && (
        <div className="absolute z-50 top-full mt-1.5 left-0 right-0 bg-bg-primary border border-border-sec rounded-md shadow-md overflow-hidden flex flex-col">

          {/* Search input */}
          <div className="p-2 border-b border-border-ter">
            <div className="relative">
              <svg
                className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-text-ter pointer-events-none"
                viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
              >
                <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
              </svg>
              <input
                ref={searchRef}
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={searchPlaceholder}
                className="w-full pl-8 pr-3 py-1.5 text-sm bg-bg-secondary border border-border-sec rounded text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
              />
            </div>
          </div>

          {/* Option list */}
          <ul
            ref={listRef}
            role="listbox"
            className="max-h-52 overflow-y-auto py-1"
          >
            {filtered.length === 0 ? (
              <li className="px-3 py-6 text-sm text-text-ter text-center italic">
                {emptyMessage}
              </li>
            ) : (
              filtered.map((option, idx) => {
                const isSelected    = option.value === value;
                const isHighlighted = idx === highlight;
                return (
                  <li
                    key={option.value}
                    role="option"
                    aria-selected={isSelected}
                    onMouseEnter={() => setHighlight(idx)}
                    onClick={() => pick(option)}
                    className={[
                      "flex items-center gap-2 px-3 py-2 text-sm cursor-pointer transition-colors",
                      isHighlighted ? "bg-bg-accent" : "hover:bg-bg-secondary",
                      isSelected ? "text-lsg-blue-dark font-semibold" : "text-text-sec",
                    ].join(" ")}
                  >
                    {/* Checkmark for the active selection */}
                    <span className="w-3.5 shrink-0">
                      {isSelected && (
                        <svg viewBox="0 0 12 12" fill="none" className="w-3.5 h-3.5">
                          <path d="M2 6l3 3 5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                      )}
                    </span>
                    <span className="truncate">{option.label}</span>
                  </li>
                );
              })
            )}
          </ul>
        </div>
      )}
    </div>
  );
}