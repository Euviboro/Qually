import { useState, useEffect } from "react";
import { useAsync } from "./useAsync";
import { getCategories } from "../api/questions";

/**
 * @typedef {import('../api/questions').SubattributeDTO} SubattributeDTO
 * @typedef {import('../api/questions').AuditQuestionDTO} AuditQuestionDTO
 */

/**
 * Manages all state and handlers for the QuestionEditModal form.
 * Mirrors the pattern of useClients/useProtocol: async data via useAsync,
 * form state managed locally, actions returned for the UI to call.
 *
 * @param {AuditQuestionDTO | null} question - The question to edit, or null when modal is closed.
 */
export function useQuestionForm(question) {
  const [formData, setFormData] = useState(null);
  const [saving, setSaving] = useState(false);

  const { data: categories, loading: categoriesLoading } = useAsync(getCategories, []);

  // Sync form state whenever a new question is passed in
  useEffect(() => {
    if (!question) return;
    setFormData({
      questionId:    question.questionId,
      questionText:  question.questionText,
      category:      question.category,
      subattributes: question.subattributes
        ? question.subattributes.map((s) => ({
            subattributeId:   s.subattributeId ?? null,
            subattributeText: s.subattributeText,
            options: s.options ? [...s.options] : [],
          }))
        : [],
    });
  }, [question]);

  // ── Field handlers ────────────────────────────────────────

  /** @param {React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>} e */
  const handleFieldChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  // ── Subattribute handlers ─────────────────────────────────

  const addSubattribute = () =>
    setFormData((prev) => ({
      ...prev,
      subattributes: [
        ...prev.subattributes,
        { subattributeId: null, subattributeText: "", options: [] },
      ],
    }));

  /** @param {number} idx */
  const removeSubattribute = (idx) =>
    setFormData((prev) => ({
      ...prev,
      subattributes: prev.subattributes.filter((_, i) => i !== idx),
    }));

  /**
   * @param {number} idx
   * @param {string} value
   */
  const updateSubattributeText = (idx, value) =>
    setFormData((prev) => {
      const subattributes = [...prev.subattributes];
      subattributes[idx] = { ...subattributes[idx], subattributeText: value };
      return { ...prev, subattributes };
    });

  // ── Option handlers (choices within a subattribute) ───────

  /**
   * @param {number} subIdx - Index of the parent subattribute
   * @param {number} optIdx - Index of the option within that subattribute
   * @param {string} value
   */
  const updateOption = (subIdx, optIdx, value) =>
    setFormData((prev) => {
      const subattributes = [...prev.subattributes];
      const options = [...subattributes[subIdx].options];
      options[optIdx] = { ...options[optIdx], optionText: value };
      subattributes[subIdx] = { ...subattributes[subIdx], options };
      return { ...prev, subattributes };
    });

  /** @param {number} subIdx */
  const addOption = (subIdx) =>
    setFormData((prev) => {
      const subattributes = [...prev.subattributes];
      subattributes[subIdx] = {
        ...subattributes[subIdx],
        options: [...subattributes[subIdx].options, { optionText: "" }],
      };
      return { ...prev, subattributes };
    });

  /**
   * @param {number} subIdx
   * @param {number} optIdx
   */
  const removeOption = (subIdx, optIdx) =>
    setFormData((prev) => {
      const subattributes = [...prev.subattributes];
      subattributes[subIdx] = {
        ...subattributes[subIdx],
        options: subattributes[subIdx].options.filter((_, i) => i !== optIdx),
      };
      return { ...prev, subattributes };
    });

  // ── Submit ────────────────────────────────────────────────

  /**
   * @param {(data: AuditQuestionDTO) => Promise<void>} onSave
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
    categories: categories ?? [],
    categoriesLoading,
    handleFieldChange,
    addSubattribute,
    removeSubattribute,
    updateSubattributeText,
    addOption,
    updateOption,
    removeOption,
    handleSubmit,
  };
}