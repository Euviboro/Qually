export function AttributeEditor({ attribute, onChange, onRemove }) {
  const updateText = (text) => onChange({ ...attribute, subattributeText: text });

  const updateOption = (idx, val) => {
    const options = [...attribute.subattributeOptions];
    options[idx] = val;
    onChange({ ...attribute, subattributeOptions: options });
  };

  const addOption = () =>
    onChange({ ...attribute, subattributeOptions: [...attribute.subattributeOptions, ""] });

  const removeOption = (idx) => {
    if (attribute.subattributeOptions.length <= 2) return;
    onChange({ ...attribute, subattributeOptions: attribute.subattributeOptions.filter((_, i) => i !== idx) });
  };

  return (
    <div className="bg-bg-secondary border border-border-sec rounded-lg p-3 flex flex-col gap-2.5">
      <div className="flex gap-2 items-center">
        <input
          type="text"
          value={attribute.subattributeText}
          onChange={(e) => updateText(e.target.value)}
          placeholder='Subattribute title, e.g. "Accountability"'
          className="flex-1 px-2.5 py-1.5 text-sm font-medium rounded border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue focus:ring-2 focus:ring-lsg-blue/10 transition-all"
        />
        <button onClick={onRemove} className="text-text-ter hover:text-error-on p-1 transition-colors">✕</button>
      </div>

      <div className="flex flex-wrap gap-2 items-center">
        {attribute.subattributeOptions.map((opt, optIdx) => (
          <div key={optIdx} className="flex items-center gap-1">
            <input
              type="text"
              value={opt}
              onChange={(e) => updateOption(optIdx, e.target.value)}
              placeholder={`Option ${optIdx + 1}`}
              className="w-28 px-2 py-1 text-xs rounded border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
            />
            {attribute.subattributeOptions.length > 2 && (
              <button onClick={() => removeOption(optIdx)} className="text-text-ter hover:text-error-on text-[10px] transition-colors">✕</button>
            )}
          </div>
        ))}
        <button
          onClick={addOption}
          className="px-2 py-1 text-[10px] rounded border border-dashed border-border-sec text-text-ter hover:text-lsg-blue hover:border-lsg-blue transition-all"
        >
          + choice
        </button>
      </div>
    </div>
  );
}
