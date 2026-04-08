export function AttributeEditor({ attribute, onChange, onRemove }) {
  const updateTitle = (title) => onChange({ ...attribute, title });

  const updateChoice = (idx, val) => {
    const choices = [...attribute.choices];
    choices[idx] = val;
    onChange({ ...attribute, choices });
  };

  const addChoice = () =>
    onChange({ ...attribute, choices: [...attribute.choices, ""] });

  const removeChoice = (idx) => {
    if (attribute.choices.length <= 2) return;
    onChange({ ...attribute, choices: attribute.choices.filter((_, i) => i !== idx) });
  };

  return (
    <div className="bg-gray-50 border border-gray-200 rounded-lg p-3 flex flex-col gap-2.5">
      <div className="flex gap-2 items-center">
        <input
          type="text"
          value={attribute.title}
          onChange={(e) => updateTitle(e.target.value)}
          placeholder='Subattribute title, e.g. "Accountability"'
          className="flex-1 px-2.5 py-1.5 text-sm font-medium rounded border border-gray-200 bg-white outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10 transition-all"
        />
        <button onClick={onRemove} className="text-gray-300 hover:text-red-500 p-1 transition-colors">✕</button>
      </div>

      <div className="flex flex-wrap gap-2 items-center">
        {attribute.choices.map((choice, idx) => (
          <div key={idx} className="flex items-center gap-1">
            <input
              type="text"
              value={choice}
              onChange={(e) => updateChoice(idx, e.target.value)}
              placeholder={`Choice ${idx + 1}`}
              className="w-28 px-2 py-1 text-xs rounded border border-gray-200 bg-white outline-none focus:border-blue-500 transition-all"
            />
            {attribute.choices.length > 2 && (
              <button onClick={() => removeChoice(idx)} className="text-gray-300 hover:text-red-500 text-[10px]">✕</button>
            )}
          </div>
        ))}
        <button
          onClick={addChoice}
          className="px-2 py-1 text-[10px] rounded border border-dashed border-gray-300 text-gray-400 hover:text-blue-500 hover:border-blue-500 transition-all"
        >
          + choice
        </button>
      </div>
    </div>
  );
}