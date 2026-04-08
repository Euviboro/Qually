const COPC_CATEGORIES = [
  { value: "CUSTOMER",   label: "Customer",   color: "text-blue-700",    bg: "bg-blue-100"   },
  { value: "BUSINESS",   label: "Business",   color: "text-emerald-700", bg: "bg-emerald-100"},
  { value: "COMPLIANCE", label: "Compliance", color: "text-purple-700",  bg: "bg-purple-100" },
];

export { COPC_CATEGORIES };

export function CategorySelector({ value, onChange, disabled }) {
  return (
    <div className="flex gap-2 flex-wrap">
      {COPC_CATEGORIES.map((cat) => {
        const active = value === cat.value;
        return (
          <button
            key={cat.value}
            onClick={() => !disabled && onChange(cat.value)}
            className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border
            ${active ? `${cat.bg} ${cat.color} border-transparent` : "border-gray-200 text-gray-400 hover:border-gray-300"}
            ${disabled ? "opacity-60 cursor-default" : "cursor-pointer"}`}
          >
            {cat.label}
          </button>
        );
      })}
    </div>
  );
}