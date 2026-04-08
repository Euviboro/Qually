import { useQuestionForm } from "../../hooks/useQuestionForm";

/**
 * @param {{
 *   isOpen: boolean,
 *   question: import('../../api/questions').AuditQuestionDTO | null,
 *   onClose: () => void,
 *   onSave: (data: import('../../api/questions').AuditQuestionDTO) => Promise<void>,
 *   saveError: string | null,
 * }} props
 */
export function QuestionEditModal({ isOpen, question, onClose, onSave, saveError }) {
  const {
    formData,
    saving,
    categories,
    categoriesLoading,
    handleFieldChange,
    addSubattribute,
    removeSubattribute,
    updateSubattributeText,
    addOption,
    updateOption,
    removeOption,
    handleSubmit,
  } = useQuestionForm(question);

  if (!isOpen || !formData) return null;

  return (
    <div className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col rounded-2xl shadow-2xl border border-gray-200">

        {/* Header */}
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
          <div>
            <h2 className="text-xl font-bold text-gray-800">Edit Question</h2>
            <p className="text-xs text-gray-500 mt-1 font-mono">ID: {formData.questionId}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto flex-1 flex flex-col gap-6">

          {/* Question text */}
          <div>
            <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">
              Question Description
            </label>
            <textarea
              name="questionText"
              value={formData.questionText}
              onChange={handleFieldChange}
              rows={3}
              required
              placeholder="Enter the audit question..."
              className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-gray-800 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all resize-none"
            />
          </div>

          {/* Category */}
          <div>
            <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">
              Category (COPC)
            </label>
            <select
              name="category"
              value={formData.category}
              onChange={handleFieldChange}
              disabled={categoriesLoading}
              className="w-full p-2.5 bg-gray-50 border border-gray-200 rounded-lg text-gray-800 focus:border-blue-500 outline-none disabled:opacity-50"
            >
              {categories.map((cat) => (
                <option key={cat.value} value={cat.value}>{cat.label}</option>
              ))}
            </select>
          </div>

          <hr className="border-gray-100" />

          {/* Subattributes */}
          <div>
            <div className="flex justify-between items-center mb-4">
              <label className="text-xs font-bold text-gray-400 uppercase tracking-wider">
                Sub-attributes & Criteria
              </label>
              <button
                type="button"
                onClick={addSubattribute}
                className="text-xs bg-blue-50 text-blue-600 px-3 py-1.5 rounded-lg font-bold hover:bg-blue-100 transition-all flex items-center gap-1"
              >
                <span>+</span> Add Subattribute
              </button>
            </div>

            <div className="space-y-4">
              {formData.subattributes.length === 0 ? (
                <p className="text-sm text-gray-400 italic text-center py-4 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                  No subattributes defined for this question.
                </p>
              ) : (
                formData.subattributes.map((attr, subIdx) => (
                  <div key={subIdx} className="bg-gray-50 border border-gray-200 rounded-xl p-4 flex flex-col gap-3">

                    {/* Subattribute title row */}
                    <div className="flex gap-2 items-center">
                      <span className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full bg-gray-200 text-[10px] font-bold text-gray-500">
                        {subIdx + 1}
                      </span>
                      <input
                        value={attr.subattributeText}
                        onChange={(e) => updateSubattributeText(subIdx, e.target.value)}
                        placeholder="Subattribute title..."
                        required
                        className="flex-1 p-2 bg-white border border-gray-200 rounded-lg text-sm font-medium focus:border-blue-500 outline-none transition-all"
                      />
                      <button
                        type="button"
                        onClick={() => removeSubattribute(subIdx)}
                        className="p-2 text-gray-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                        title="Remove subattribute"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                        </svg>
                      </button>
                    </div>

                    {/* Options (choices) */}
                    <div className="pl-8 flex flex-col gap-2">
                      <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">
                        Choices
                      </p>
                      {attr.options.map((opt, optIdx) => (
                        <div key={optIdx} className="flex gap-2 items-center">
                          <span className="text-[10px] text-gray-400 w-4 text-right shrink-0">
                            {optIdx + 1}.
                          </span>
                          <input
                            value={opt.optionText}
                            onChange={(e) => updateOption(subIdx, optIdx, e.target.value)}
                            placeholder={`Choice ${optIdx + 1}...`}
                            required
                            className="flex-1 px-2.5 py-1.5 text-sm bg-white border border-gray-200 rounded-lg focus:border-blue-500 outline-none transition-all"
                          />
                          <button
                            type="button"
                            onClick={() => removeOption(subIdx, optIdx)}
                            className="text-gray-300 hover:text-red-500 transition-colors p-1"
                          >
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                              <path d="M18 6L6 18M6 6l12 12"/>
                            </svg>
                          </button>
                        </div>
                      ))}
                      <button
                        type="button"
                        onClick={() => addOption(subIdx)}
                        className="self-start mt-1 px-2.5 py-1 text-[11px] font-medium rounded-md border border-dashed border-gray-300 text-gray-400 hover:text-blue-500 hover:border-blue-400 transition-all"
                      >
                        + Add choice
                      </button>
                    </div>

                  </div>
                ))
              )}
            </div>
          </div>

          {/* Save error */}
          {saveError && (
            <div className="px-4 py-3 bg-red-50 border border-red-100 text-red-600 text-sm rounded-lg">
              {saveError}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-gray-100 flex justify-end gap-3 bg-gray-50/30">
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            className="px-5 py-2 text-gray-500 font-semibold hover:text-gray-700 transition-colors disabled:opacity-50"
          >
            Discard
          </button>
          <button
            type="button"
            onClick={() => handleSubmit(onSave)}
            disabled={saving}
            className="px-8 py-2 bg-blue-600 text-white rounded-xl font-bold shadow-lg shadow-blue-600/20 hover:bg-blue-700 active:scale-95 transition-all disabled:opacity-50 disabled:pointer-events-none flex items-center gap-2"
          >
            {saving ? (
              <>
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Saving...
              </>
            ) : "Update Question"}
          </button>
        </div>

      </div>
    </div>
  );
}