/**
 * @module components/ui/QuestionEditModal
 *
 * Modal for creating or editing a single audit question, including its nested
 * subattributes and answer-choice options.
 *
 * All form state is managed by {@link useQuestionForm}; this component is
 * purely presentational. Category selection uses the shared
 * {@link CategorySelector} component — the same one used by `QuestionCard` in
 * the New Protocol flow — so the two flows look and behave identically.
 *
 * **Save guard:** the submit button is disabled until both `questionText` and
 * `category` are filled. This is enforced via `canSave` from the hook, which
 * keeps validation logic colocated with state rather than scattered across the
 * UI.
 */

import { useQuestionForm } from "../../hooks/useQuestionForm";
import { CategorySelector } from "./CategorySelector";
import { Label } from "./Label";

/**
 * @param {Object}   props
 * @param {boolean}  props.isOpen
 *   Controls visibility. Returns `null` when `false` so the modal fully
 *   unmounts from the DOM.
 * @param {import('../../hooks/useQuestionForm').QuestionFormData|null} props.question
 *   The question to populate the form with. For the create flow, pass a blank
 *   template object with `questionId: null`. For the edit flow, pass the full
 *   `AuditQuestionResponseDTO` as returned by the API.
 * @param {() => void} props.onClose
 *   Called when the user dismisses the modal (× button or Discard).
 * @param {(data: import('../../hooks/useQuestionForm').QuestionFormData) => Promise<void>} props.onSave
 *   Called with the current `formData` on submit. Responsible for the actual
 *   API call (create or update). Errors thrown here surface via `saveError`.
 * @param {string|null} props.saveError
 *   External error message to display inside the modal (e.g. a server
 *   validation error). Managed by the parent, cleared on close.
 */
export function QuestionEditModal({ isOpen, question, onClose, onSave, saveError }) {
  const {
    formData,
    saving,
    canSave,
    handleFieldChange,
    handleCategoryChange,
    addSubattribute,
    removeSubattribute,
    updateSubattributeText,
    addOption,
    updateOption,
    removeOption,
    handleSubmit,
  } = useQuestionForm(question);

  if (!isOpen || !formData) return null;

  /** `true` when there is no `questionId` — adapts labels and submit text. */
  const isCreating = !formData.questionId;

  return (
    <div className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-bg-primary w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col rounded-xl shadow-lg border border-border-sec">

        {/* ── Header ──────────────────────────────────────────── */}
        <div className="p-6 border-b border-border-ter flex justify-between items-center bg-bg-secondary/50">
          <div>
            <h2 className="text-xl font-bold text-text-pri">
              {isCreating ? "Add Question" : "Edit Question"}
            </h2>
            {!isCreating && (
              <p className="text-xs text-text-ter mt-1 font-mono">ID: {formData.questionId}</p>
            )}
          </div>
          <button onClick={onClose} className="text-text-ter hover:text-text-sec transition-colors">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>

        {/* ── Body ────────────────────────────────────────────── */}
        <div className="p-6 overflow-y-auto flex-1 flex flex-col gap-6">

          {/* Question text */}
          <div>
            <Label required>Question Description</Label>
            <textarea
              name="questionText"
              value={formData.questionText}
              onChange={handleFieldChange}
              rows={3}
              placeholder="Enter the audit question..."
              className="w-full p-3 bg-bg-secondary border border-border-sec rounded-xl text-text-pri placeholder:text-text-ter focus:ring-2 focus:ring-lsg-blue/20 focus:border-lsg-blue outline-none transition-all resize-none"
            />
          </div>

          {/* Category — uses the shared CategorySelector, same as QuestionCard.
              `value` pre-selects the active pill from formData (populated by
              useQuestionForm from the question prop), so when editing an existing
              question the current backend value appears selected immediately.
              `handleCategoryChange` is a direct setter that accepts a raw string
              value, matching CategorySelector's onChange(value) signature. */}
          <div>
            <Label required>COPC Category</Label>
            <CategorySelector
              value={formData.category}
              onChange={handleCategoryChange}
            />
          </div>

          <hr className="border-border-ter" />

          {/* Subattributes */}
          <div>
            <div className="flex justify-between items-center mb-4">
              <label className="text-xs font-bold text-text-ter uppercase tracking-wider">
                Sub-attributes & Criteria
              </label>
              <button
                type="button"
                onClick={addSubattribute}
                className="text-xs bg-bg-accent text-lsg-blue-dark px-3 py-1.5 rounded-lg font-bold hover:opacity-90 transition-all flex items-center gap-1"
              >
                <span>+</span> Add Subattribute
              </button>
            </div>

            <div className="space-y-4">
              {formData.subattributes.length === 0 ? (
                <p className="text-sm text-text-ter italic text-center py-4 bg-bg-secondary rounded-xl border border-dashed border-border-sec">
                  No subattributes defined for this question.
                </p>
              ) : (
                formData.subattributes.map((attr, subIdx) => (
                  <div key={subIdx} className="bg-bg-secondary border border-border-sec rounded-xl p-4 flex flex-col gap-3">

                    {/* Subattribute title row */}
                    <div className="flex gap-2 items-center">
                      <span className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full bg-bg-tertiary text-[10px] font-bold text-text-ter">
                        {subIdx + 1}
                      </span>
                      <input
                        value={attr.subattributeText}
                        onChange={(e) => updateSubattributeText(subIdx, e.target.value)}
                        placeholder="Subattribute title..."
                        className="flex-1 p-2 bg-bg-primary border border-border-sec rounded-lg text-sm font-medium text-text-pri focus:border-lsg-blue outline-none transition-all"
                      />
                      <button
                        type="button"
                        onClick={() => removeSubattribute(subIdx)}
                        className="p-2 text-text-ter hover:text-error-on hover:bg-error-surface rounded-lg transition-all"
                        title="Remove subattribute"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                        </svg>
                      </button>
                    </div>

                    {/* Option choices */}
                    <div className="pl-8 flex flex-col gap-2">
                      <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Choices</p>
                      {attr.subattributeOptions.map((opt, optIdx) => (
                        <div key={optIdx} className="flex gap-2 items-center">
                          <span className="text-[10px] text-text-ter w-4 text-right shrink-0">{optIdx + 1}.</span>
                          <input
                            value={opt.optionLabel}
                            onChange={(e) => updateOption(subIdx, optIdx, e.target.value)}
                            placeholder={`Choice ${optIdx + 1}...`}
                            className="flex-1 px-2.5 py-1.5 text-sm bg-bg-primary border border-border-sec rounded-lg text-text-pri focus:border-lsg-blue outline-none transition-all"
                          />
                          <button
                            type="button"
                            onClick={() => removeOption(subIdx, optIdx)}
                            className="text-text-ter hover:text-error-on transition-colors p-1"
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
                        className="self-start mt-1 px-2.5 py-1 text-[11px] font-medium rounded-md border border-dashed border-border-sec text-text-ter hover:text-lsg-blue hover:border-lsg-blue transition-all"
                      >
                        + Add choice
                      </button>
                    </div>

                  </div>
                ))
              )}
            </div>
          </div>

          {/* Server-side save error */}
          {saveError && (
            <div className="px-4 py-3 bg-error-surface border border-[rgba(226,75,74,0.2)] text-error-on text-sm rounded-lg">
              {saveError}
            </div>
          )}
        </div>

        {/* ── Footer ──────────────────────────────────────────── */}
        <div className="p-6 border-t border-border-ter flex justify-end gap-3 bg-bg-secondary/30">
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            className="px-5 py-2 text-text-ter font-semibold hover:text-text-sec transition-colors disabled:opacity-50"
          >
            Discard
          </button>

          {/* Disabled when saving OR when required fields are empty.
              `canSave` from the hook checks questionText.trim() && category. */}
          <button
            type="button"
            onClick={() => handleSubmit(onSave)}
            disabled={saving || !canSave}
            className="px-8 py-2 bg-lsg-blue text-white rounded-xl font-bold shadow-md shadow-lsg-blue/20 hover:bg-lsg-blue-dark active:scale-95 transition-all disabled:opacity-50 disabled:pointer-events-none flex items-center gap-2"
          >
            {saving ? (
              <>
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Saving...
              </>
            ) : isCreating ? "Add Question" : "Update Question"}
          </button>
        </div>

      </div>
    </div>
  );
}