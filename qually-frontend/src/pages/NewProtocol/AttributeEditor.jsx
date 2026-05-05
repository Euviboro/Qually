/**
 * AttributeEditor — edits a single subattribute within the NewProtocol form.
 *
 * In ACCOUNTABILITY mode:
 *  - A "Mark as accountability" toggle appears. At most one subattribute per
 *    question can be the accountability selector; the parent (QuestionCard) is
 *    responsible for clearing the flag on siblings when this one is toggled on.
 *  - Each option shows a "Company accountable" chip toggle. Options marked true
 *    will cause a NO scored against the category; options marked false excuse the NO.
 *
 * Option shape: { label: string, isCompanyAccountable: boolean }
 */
export function AttributeEditor({ attribute, onChange, onRemove, isAccountabilityMode }) {
  const updateText = (text) => onChange({ ...attribute, subattributeText: text });

  const toggleAccountability = () =>
    onChange({ ...attribute, isAccountabilitySubattribute: !attribute.isAccountabilitySubattribute });

  const updateOption = (idx, patch) => {
    const options = attribute.subattributeOptions.map((o, i) =>
      i === idx ? { ...o, ...patch } : o
    );
    onChange({ ...attribute, subattributeOptions: options });
  };

  const addOption = () =>
    onChange({
      ...attribute,
      subattributeOptions: [
        ...attribute.subattributeOptions,
        { label: "", isCompanyAccountable: false },
      ],
    });

  const removeOption = (idx) => {
    if (attribute.subattributeOptions.length <= 2) return;
    onChange({
      ...attribute,
      subattributeOptions: attribute.subattributeOptions.filter((_, i) => i !== idx),
    });
  };

  const isAccountability = attribute.isAccountabilitySubattribute;

  return (
    <div
      className={[
        "border rounded-lg p-3 flex flex-col gap-2.5 transition-colors",
        isAccountabilityMode && isAccountability
          ? "bg-bg-accent border-lsg-blue"
          : "bg-bg-secondary border-border-sec",
      ].join(" ")}
    >
      {/* Header row: text input + accountability toggle + remove */}
      <div className="flex gap-2 items-center">
        <input
          type="text"
          value={attribute.subattributeText}
          onChange={(e) => updateText(e.target.value)}
          placeholder='Subattribute title, e.g. "Accountability"'
          className="flex-1 px-2.5 py-1.5 text-sm font-medium rounded border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue focus:ring-2 focus:ring-lsg-blue/10 transition-all"
        />

        {isAccountabilityMode && (
          <button
            type="button"
            onClick={toggleAccountability}
            title={isAccountability ? "Unmark as accountability subattribute" : "Mark as accountability subattribute"}
            className={[
              "shrink-0 px-2.5 py-1.5 text-[10px] font-bold rounded border transition-all uppercase tracking-wide",
              isAccountability
                ? "border-lsg-blue bg-lsg-blue text-white"
                : "border-border-sec text-text-ter hover:border-lsg-blue hover:text-lsg-blue",
            ].join(" ")}
          >
            Accountability
          </button>
        )}

        <button onClick={onRemove} className="text-text-ter hover:text-error-on p-1 transition-colors">✕</button>
      </div>

      {/* Options */}
      <div className="flex flex-wrap gap-2 items-center">
        {attribute.subattributeOptions.map((opt, optIdx) => (
          <div key={optIdx} className="flex items-center gap-1">
            <input
              type="text"
              value={opt.label}
              onChange={(e) => updateOption(optIdx, { label: e.target.value })}
              placeholder={`Option ${optIdx + 1}`}
              className="w-28 px-2 py-1 text-xs rounded border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all"
            />

            {/* Company-accountable toggle — only shown on accountability subattribute */}
            {isAccountabilityMode && isAccountability && (
              <button
                type="button"
                onClick={() => updateOption(optIdx, { isCompanyAccountable: !opt.isCompanyAccountable })}
                title={opt.isCompanyAccountable ? "Company accountable (click to unmark)" : "Mark as company accountable"}
                className={[
                  "px-1.5 py-0.5 text-[9px] font-bold rounded border transition-all uppercase",
                  opt.isCompanyAccountable
                    ? "border-error-on bg-error-surface text-error-on"
                    : "border-border-sec text-text-ter hover:border-border-pri",
                ].join(" ")}
              >
                Co.
              </button>
            )}

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

      {/* Helper text for accountability subattribute */}
      {isAccountabilityMode && isAccountability && (
        <p className="text-[10px] text-lsg-blue">
          Mark options with <strong>Co.</strong> if the company is responsible — those will count against the score when answered No.
        </p>
      )}
    </div>
  );
}