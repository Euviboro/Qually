import { useState, useEffect, useRef } from "react";

export function ClientSelector({ clients, value, onChange, loading }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    function handleClick(e) {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const selected = clients.find((c) => c.clientId === value);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        className={`w-full px-3 py-2 text-base rounded-md border flex items-center justify-between transition-all bg-white
        ${open ? "border-blue-500 ring-3 ring-blue-500/10" : "border-gray-200"}
        ${selected ? "text-gray-900" : "text-gray-400"}`}
      >
        <span>{loading ? "Loading clients…" : selected ? selected.clientName : "Select a client"}</span>
        <svg
          className={`w-3 h-3 transition-transform duration-200 ${open ? "rotate-180" : ""}`}
          viewBox="0 0 12 12" fill="none"
        >
          <path d="M2 4l4 4 4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {open && (
        <div className="absolute top-full mt-1.5 left-0 right-0 bg-white border border-gray-200 rounded-md shadow-lg z-50 overflow-hidden max-h-64 overflow-y-auto">
          {clients.map((c) => (
            <button
              key={c.clientId}
              onClick={() => { onChange(c.clientId); setOpen(false); }}
              className={`w-full px-3.5 py-2.5 text-sm text-left flex items-center gap-2 transition-colors
              ${c.clientId === value ? "bg-blue-50 text-blue-600 font-semibold" : "text-gray-700 hover:bg-gray-50"}`}
            >
              <div className="w-3">
                {c.clientId === value && (
                  <svg className="w-3 h-3" viewBox="0 0 12 12" fill="none">
                    <path d="M2 6l3 3 5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                )}
              </div>
              {c.clientName}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}