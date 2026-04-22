import { COPC_CATEGORY_META } from "../../constants";

const COPC_CATEGORIES = Object.entries(COPC_CATEGORY_META).map(([value, meta]) => ({
  value,
  label: meta.label,
  bgVar: meta.bgVar,
  textVar: meta.textVar,
}));

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
            style={active ? {
              background: `var(${cat.bgVar})`,
              color: `var(${cat.textVar})`,
              borderColor: "transparent",
            } : {}}
            className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border
              ${active ? "" : "border-border-sec text-text-ter hover:border-border-pri"}
              ${disabled ? "opacity-60 cursor-default" : "cursor-pointer"}`}
          >
            {cat.label}
          </button>
        );
      })}
    </div>
  );
}
