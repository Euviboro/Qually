/** @module hooks/useQuestionForm */

import { useState, useEffect } from "react";

/**
 * @typedef {import('../api/questions').AuditQuestionResponseDTO} AuditQuestionDTO
 */

/**
 * Internal form shape used by `QuestionEditModal`.
 * Field names deliberately match the backend request DTOs so the object can
 * be sent as-is with no transformation step.
 *
 * @typedef {Object} QuestionFormData
 * @property {number|null} questionId     - `null` when creating a new question.
 * @property {number|null} protocolId     - Required by the backend on every PUT/POST.
 * @property {string}      questionText   - The full audit question text.
 * @property {string}      category       - COPC category key (e.g. `"CUSTOMER"`).
 *   Empty string means no selection; the modal blocks saving until this is set.
 * @property {SubattributeFormEntry[]} subattributes
 */

/**
 * A single subattribute entry inside the form.
 * Field names match `SubattributeRequestDTO` exactly so the object
 * can be sent directly to the API without transformation.
 *
 * @typedef {Object} SubattributeFormEntry
 * @property {number|null}      subattributeId      - `null` for newly added subattributes.
 * @property {string}           subattributeText    - Title / description of the sub-criterion.
 * @property {OptionFormEntry[]} subattributeOptions - Answer choices.
 */

/**
 * A single answer-choice option inside a subattribute.
 * Field name matches `SubattributeOptionRequestDTO` exactly.
 *
 * @typedef {Object} OptionFormEntry
 * @property {string} optionLabel - Display label for this choice.
 */

/**
 * The return shape of `useQuestionForm`.
 *
 * @typedef {Object} UseQuestionFormResult
 * @property {QuestionFormData|null} formData
 *   Current form state. `null` before the first question prop is received.
 * @property {boolean} saving
 *   `true` while the async save is in flight.
 * @property {boolean} canSave
 *   `true` when the minimum required fields (questionText, category) are filled.
 *   Intended to drive the disabled state of the submit button.
 * @property {(e: React.ChangeEvent<HTMLInputElement|HTMLTextAreaElement>) => void} handleFieldChange
 *   Generic change handler for scalar text fields. Reads `name` and `value`
 *   from the event target — attach directly to `<input name="questionText">` etc.
 * @property {(value: string) => void} handleCategoryChange
 *   Dedicated setter for the category field. Accepts the raw enum key string
 *   (e.g. `"CUSTOMER"`) emitted by `CategorySelector`'s `onChange` prop.
 * @property {() => void} addSubattribute
 *   Appends a blank subattribute entry to the list.
 * @property {(idx: number) => void} removeSubattribute
 *   Removes the subattribute at the given index.
 * @property {(idx: number, value: string) => void} updateSubattributeText
 *   Updates the title of the subattribute at `idx`.
 * @property {(subIdx: number) => void} addOption
 *   Appends a blank option to the subattribute at `subIdx`.
 * @property {(subIdx: number, optIdx: number, value: string) => void} updateOption
 *   Updates the label of a single option within a subattribute.
 * @property {(subIdx: number, optIdx: number) => void} removeOption
 *   Removes the option at `optIdx` from the subattribute at `subIdx`.
 * @property {(onSave: (data: QuestionFormData) => Promise<void>) => Promise<void>} handleSubmit
 *   Wraps `onSave` with the `saving` flag. The caller is responsible for the
 *   actual API call.
 */

/**
 * Manages all state and event handlers for `QuestionEditModal`.
 *
 * Works for both the **edit** flow (existing `questionId`) and the **create**
 * flow (`questionId === null`). The hook syncs its internal state from the
 * `question` prop on every change — open the modal with a new object to reset.
 *
 * **Category note:** the category field is driven by `CategorySelector`, which
 * emits a raw string value (e.g. `"CUSTOMER"`) via its `onChange` prop rather
 * than a DOM event. Use `handleCategoryChange` for this field instead of the
 * generic `handleFieldChange`.
 *
 * Previous versions of this hook fetched category options from the server via
 * `getCategories`. That fetch has been removed because `CategorySelector` derives
 * its options statically from `COPC_CATEGORY_META` in `constants/index.js`,
 * eliminating an unnecessary network round-trip on every modal open.
 *
 * @param {AuditQuestionDTO|null} question
 *   The question to edit, or a blank template object for the create flow.
 *   Pass `null` to leave the form uninitialised (modal closed).
 * @returns {UseQuestionFormResult}
 *
 * @example
 * // Inside QuestionEditModal:
 * const { formData, handleFieldChange, handleCategoryChange, handleSubmit } =
 *   useQuestionForm(question);
 */
export function useQuestionForm(question) {
  const [formData, setFormData] = useState(null);
  const [saving,   setSaving]   = useState(false);

  // Sync internal form state whenever a new question is passed in.
  // Field names match the backend request DTO names exactly so `formData` can
  // be PUT/POSTed without any serialization transformation.
  useEffect(() => {
    if (!question) return;
    setFormData({
      questionId:   question.questionId ?? null,
      protocolId:   question.protocolId ?? null,
      questionText: question.questionText ?? "",
      category:     question.category    ?? "",
      subattributes: question.subattributes
        ? question.subattributes.map((s) => ({
            subattributeId:   s.subattributeId ?? null,
            subattributeText: s.subattributeText,
            subattributeOptions: s.subattributeOptions
              ? s.subattributeOptions.map((o) => ({ optionLabel: o.optionLabel }))
              : [],
          }))
        : [],
    });
  }, [question]);

  // ── Derived state ─────────────────────────────────────────

  /**
   * The save button should be disabled until both required fields are filled.
   * This is checked in the hook rather than in the modal so the logic is
   * colocated with the form state that drives it.
   */
  const canSave = Boolean(
    formData?.questionText?.trim() && formData?.category
  );

  // ── Field handlers ────────────────────────────────────────

  /**
   * Generic change handler for scalar text fields (`questionText`).
   * Reads `name` and `value` from the DOM event — attach to any `<input>` or
   * `<textarea>` that has a matching `name` attribute on `formData`.
   *
   * @param {React.ChangeEvent<HTMLInputElement|HTMLTextAreaElement>} e
   */
  const handleFieldChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  /**
   * Dedicated setter for the `category` field.
   *
   * `CategorySelector` calls `onChange(value)` with a raw string (e.g.
   * `"CUSTOMER"`), not a DOM event, so the generic `handleFieldChange` cannot
   * be used here.
   *
   * @param {string} value - COPC category key emitted by `CategorySelector`.
   */
  const handleCategoryChange = (value) => {
    setFormData((prev) => ({ ...prev, category: value }));
  };

  // ── Subattribute handlers ─────────────────────────────────

  /**
   * Appends a blank subattribute entry to the end of the list.
   * Options start empty; the user adds choices manually.
   */
  const addSubattribute = () =>
    setFormData((prev) => ({
      ...prev,
      subattributes: [
        ...prev.subattributes,
        { subattributeId: null, subattributeText: "", subattributeOptions: [] },
      ],
    }));

  /**
   * Removes the subattribute at position `idx`.
   * @param {number} idx
   */
  const removeSubattribute = (idx) =>
    setFormData((prev) => ({
      ...prev,
      subattributes: prev.subattributes.filter((_, i) => i !== idx),
    }));

  /**
   * Replaces the `subattributeText` of the entry at position `idx`.
   * @param {number} idx
   * @param {string} value
   */
  const updateSubattributeText = (idx, value) =>
    setFormData((prev) => {
      const subattributes = [...prev.subattributes];
      subattributes[idx] = { ...subattributes[idx], subattributeText: value };
      return { ...prev, subattributes };
    });

  // ── Option handlers ────────────────────────────────────────

  /**
   * Replaces the `optionLabel` of a single choice within a subattribute.
   * @param {number} subIdx - Index of the parent subattribute.
   * @param {number} optIdx - Index of the option within that subattribute.
   * @param {string} value
   */
  const updateOption = (subIdx, optIdx, value) =>
    setFormData((prev) => {
      const subattributes = [...prev.subattributes];
      const subattributeOptions = [...subattributes[subIdx].subattributeOptions];
      subattributeOptions[optIdx] = { ...subattributeOptions[optIdx], optionLabel: value };
      subattributes[subIdx] = { ...subattributes[subIdx], subattributeOptions };
      return { ...prev, subattributes };
    });

  /**
   * Appends a blank option `{ optionLabel: "" }` to the subattribute at `subIdx`.
   * @param {number} subIdx
   */
  const addOption = (subIdx) =>
    setFormData((prev) => {
      const subattributes = [...prev.subattributes];
      subattributes[subIdx] = {
        ...subattributes[subIdx],
        subattributeOptions: [
          ...subattributes[subIdx].subattributeOptions,
          { optionLabel: "" },
        ],
      };
      return { ...prev, subattributes };
    });

  /**
   * Removes the option at `optIdx` from the subattribute at `subIdx`.
   * @param {number} subIdx
   * @param {number} optIdx
   */
  const removeOption = (subIdx, optIdx) =>
    setFormData((prev) => {
      const subattributes = [...prev.subattributes];
      subattributes[subIdx] = {
        ...subattributes[subIdx],
        subattributeOptions: subattributes[subIdx].subattributeOptions.filter(
          (_, i) => i !== optIdx
        ),
      };
      return { ...prev, subattributes };
    });

  // ── Submit ────────────────────────────────────────────────

  /**
   * Calls `onSave` with the current `formData`, managing the `saving` flag.
   * Because all field names match the backend DTOs, `formData` is sent as-is.
   * Any error thrown by `onSave` propagates up to the modal's `saveError` prop.
   *
   * @param {(data: QuestionFormData) => Promise<void>} onSave
   */
  const handleSubmit = async (onSave) => {
    setSaving(true);
    try {
      await onSave(formData);
    } finally {
      setSaving(false);
    }
  };

  return {
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
  };
}