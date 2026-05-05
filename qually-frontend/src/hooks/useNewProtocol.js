/** @module hooks/useNewProtocol */

import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { getClients } from "../api/clients";
import { createProtocol } from "../api/protocols";

/** @returns {LocalQuestion} */
export const EMPTY_QUESTION = () => ({
  id: crypto.randomUUID(),
  questionText: "",
  category: "",
  confirmed: false,
  subattributes: [],
});

/**
 * @returns {LocalSubattribute}
 *
 * Options are objects { label, isCompanyAccountable } instead of plain strings.
 * isAccountabilitySubattribute marks this subattribute as the accountability selector —
 * at most one per question is allowed in ACCOUNTABILITY protocols.
 */
export const EMPTY_ATTRIBUTE = () => ({
  id: crypto.randomUUID(),
  subattributeText: "",
  isAccountabilitySubattribute: false,
  subattributeOptions: [
    { label: "", isCompanyAccountable: false },
    { label: "", isCompanyAccountable: false },
  ],
});

export function useNewProtocol() {
  const navigate = useNavigate();

  const [clientId,              setClientId]              = useState(null);
  const [protocolName,          setProtocolName]          = useState("");
  const [protocolAbbreviation,  setProtocolAbbreviation]  = useState("");
  const [version,               setVersion]               = useState("1");
  const [auditLogicType,        setAuditLogicType]        = useState(null);
  const [questions,             setQuestions]             = useState([EMPTY_QUESTION()]);
  const [clients,               setClients]               = useState([]);
  const [clientsLoading,        setClientsLoading]        = useState(true);
  const [saving,                setSaving]                = useState(false);
  const [saveError,             setSaveError]             = useState(null);

  useEffect(() => {
    getClients()
      .then(setClients)
      .catch(() => {})
      .finally(() => setClientsLoading(false));
  }, []);

  const isAccountabilityMode = auditLogicType === "ACCOUNTABILITY";

  // Base requirement: client set, name filled, logic type chosen, all questions confirmed
  const baseCanSave =
    !!clientId &&
    !!protocolName.trim() &&
    !!auditLogicType &&
    questions.length > 0 &&
    questions.every((q) => q.confirmed);

  /**
   * For ACCOUNTABILITY protocols: every confirmed question must have exactly one
   * subattribute marked as the accountability selector before the protocol can
   * be finalized. Drafts are exempt — this only blocks the Finalize button.
   *
   * Returns an array of 1-based question numbers that are missing the flag.
   * Empty array means the protocol is ready to finalize.
   */
  const accountabilityFinalizeErrors = useMemo(() => {
    if (!isAccountabilityMode || !baseCanSave) return [];

    return questions
      .map((q, i) => {
        const count = (q.subattributes ?? []).filter(
          (s) => s.isAccountabilitySubattribute
        ).length;
        return count === 1 ? null : i + 1;
      })
      .filter(Boolean);
  }, [isAccountabilityMode, baseCanSave, questions]);

  const canSave     = baseCanSave;
  const canFinalize = baseCanSave && accountabilityFinalizeErrors.length === 0;

  const updateQuestion = (id, updated) =>
    setQuestions((qs) => qs.map((q) => (q.id === id ? updated : q)));

  const removeQuestion = (id) =>
    setQuestions((qs) => qs.filter((q) => q.id !== id));

  const addQuestion = () =>
    setQuestions((qs) => [...qs, EMPTY_QUESTION()]);

  const handleSave = async (status = "DRAFT") => {
    setSaving(true);
    setSaveError(null);

    const abbrev = protocolAbbreviation.trim().toUpperCase();

    const payload = {
      clientId,
      protocolName:         protocolName.trim(),
      protocolAbbreviation: abbrev || undefined,
      protocolVersion:      parseInt(version, 10) || 1,
      protocolStatus:       status,
      auditLogicType,
      auditQuestions: questions.map((q) => ({
        questionText: q.questionText,
        category:     q.category,
        subattributes: q.subattributes.map((a) => ({
          subattributeText:             a.subattributeText,
          isAccountabilitySubattribute: a.isAccountabilitySubattribute,
          subattributeOptions: a.subattributeOptions.map((opt) => ({
            optionLabel:          opt.label,
            isCompanyAccountable: opt.isCompanyAccountable,
          })),
        })),
      })),
    };

    try {
      const result = await createProtocol(payload);
      navigate(`/protocols/${result.protocolId}`);
    } catch (err) {
      setSaveError(err.message ?? "Something went wrong. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  return {
    clientId,             setClientId,
    protocolName,         setProtocolName,
    protocolAbbreviation, setProtocolAbbreviation,
    version,              setVersion,
    auditLogicType,       setAuditLogicType,
    isAccountabilityMode,
    questions,
    clients,              clientsLoading,
    canSave,              canFinalize,
    accountabilityFinalizeErrors,
    saving,               saveError,
    updateQuestion,       removeQuestion, addQuestion,
    handleSave,
  };
}