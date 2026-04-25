/**
 * @module components/ui/ColumnFilter
 *
 * Excel-style column filter. Clicking the funnel icon on a column header
 * opens a dropdown listing all unique values as checkboxes. Checking or
 * unchecking values updates the active filter for that column.
 *
 * For date columns, values are grouped by year → month → day as nested
 * checkboxes matching the Excel date filter pattern.
 */

import { useState, useRef, useEffect, useCallback } from "react";

/** Months as short labels for the date grouping UI. */
const MONTH_NAMES = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

/**
 * @param {Object}   props
 * @param {string}   props.label       - Column header text.
 * @param {any[]}    props.values      - All unique raw values for this column.
 * @param {Set<any>} props.activeFilter - Set of currently checked values. Empty = all shown.
 * @param {(next: Set<any>) => void} props.onChange
 * @param {"text"|"date"|"score"} [props.type="text"]
 *   - text: plain checkbox list
 *   - date: nested year → month → day
 *   - score: 0 / 100 checkboxes with colour pills
 */
export function ColumnFilter({ label, values, activeFilter, onChange, type = "text" }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  // Close on outside click
  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const isFiltered = activeFilter.size > 0;

  const toggle = useCallback((value) => {
    const next = new Set(activeFilter);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    onChange(next);
  }, [activeFilter, onChange]);

  const clearAll = () => onChange(new Set());

  // ── Date grouping ─────────────────────────────────────────

  const [expandedYears,  setExpandedYears]  = useState(new Set());
  const [expandedMonths, setExpandedMonths] = useState(new Set());

  const toggleYear  = (y) => setExpandedYears ((prev) => { const n = new Set(prev); n.has(y) ? n.delete(y) : n.add(y); return n; });
  const toggleMonth = (k) => setExpandedMonths((prev) => { const n = new Set(prev); n.has(k) ? n.delete(k) : n.add(k); return n; });

  const dateTree = type === "date" ? buildDateTree(values) : null;

  // ── Render ────────────────────────────────────────────────

  return (
    <div ref={ref} className="relative inline-flex items-center gap-1">
      <span className="text-[11px] font-bold text-text-sec uppercase tracking-wider select-none">
        {label}
      </span>
      <button
        onClick={() => setOpen((v) => !v)}
        className={["p-0.5 rounded transition-colors",
          isFiltered ? "text-lsg-blue" : "text-text-ter hover:text-text-sec"].join(" ")}
        title={isFiltered ? "Filter active" : "Filter"}
      >
        <svg width="11" height="11" viewBox="0 0 12 12" fill="currentColor">
          <path d="M1 2h10L7 6.5V10l-2-1V6.5L1 2z"/>
        </svg>
      </button>

      {open && (
        <div className="absolute top-full left-0 mt-1 z-50 bg-bg-primary border border-border-sec rounded-lg shadow-lg w-52 overflow-hidden">
          <div className="flex items-center justify-between px-3 py-2 border-b border-border-ter">
            <span className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Filter</span>
            {isFiltered && (
              <button onClick={clearAll} className="text-[10px] text-lsg-blue hover:underline">Clear</button>
            )}
          </div>

          <div className="max-h-52 overflow-y-auto py-1">
            {type === "date" && dateTree ? (
              Object.entries(dateTree).map(([year, months]) => (
                <div key={year}>
                  <button
                    onClick={() => toggleYear(year)}
                    className="w-full flex items-center gap-1.5 px-3 py-1.5 text-xs text-text-pri hover:bg-bg-secondary transition-colors"
                  >
                    <span className="text-text-ter">{expandedYears.has(year) ? "▾" : "▸"}</span>
                    {year}
                  </button>
                  {expandedYears.has(year) && Object.entries(months).map(([monthIdx, days]) => {
                    const mk = `${year}-${monthIdx}`;
                    return (
                      <div key={mk}>
                        <button
                          onClick={() => toggleMonth(mk)}
                          className="w-full flex items-center gap-1.5 pl-6 pr-3 py-1.5 text-xs text-text-sec hover:bg-bg-secondary transition-colors"
                        >
                          <span className="text-text-ter">{expandedMonths.has(mk) ? "▾" : "▸"}</span>
                          {MONTH_NAMES[parseInt(monthIdx)]}
                        </button>
                        {expandedMonths.has(mk) && days.map((day) => {
                          const dayVal = `${year}-${String(parseInt(monthIdx)+1).padStart(2,"0")}-${String(day).padStart(2,"0")}`;
                          return (
                            <label key={dayVal} className="flex items-center gap-2 pl-10 pr-3 py-1.5 text-xs text-text-sec hover:bg-bg-secondary cursor-pointer transition-colors">
                              <input type="checkbox" checked={activeFilter.has(dayVal)} onChange={() => toggle(dayVal)} className="accent-lsg-blue" />
                              {day}
                            </label>
                          );
                        })}
                      </div>
                    );
                  })}
                </div>
              ))
            ) : (
              [...new Set(values)].filter(v => v != null).sort().map((val) => (
                <label key={val} className="flex items-center gap-2 px-3 py-1.5 text-xs cursor-pointer hover:bg-bg-secondary transition-colors">
                  <input type="checkbox" checked={activeFilter.has(val)} onChange={() => toggle(val)} className="accent-lsg-blue" />
                  {type === "score" ? (
                    <span className={`font-bold px-2 py-0.5 rounded text-[11px] ${val === 100 ? "bg-success-surface text-success-on" : "bg-error-surface text-error-on"}`}>
                      {val}
                    </span>
                  ) : (
                    <span className="text-text-pri">{String(val)}</span>
                  )}
                </label>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Date tree builder ─────────────────────────────────────────

function buildDateTree(values) {
  const tree = {};
  values.filter(Boolean).forEach((raw) => {
    const d = new Date(raw);
    if (isNaN(d)) return;
    const year  = String(d.getFullYear());
    const month = String(d.getMonth()); // 0-based
    const day   = d.getDate();
    if (!tree[year]) tree[year] = {};
    if (!tree[year][month]) tree[year][month] = [];
    if (!tree[year][month].includes(day)) tree[year][month].push(day);
  });
  Object.values(tree).forEach((months) =>
    Object.values(months).forEach((days) => days.sort((a, b) => a - b))
  );
  return tree;
}
