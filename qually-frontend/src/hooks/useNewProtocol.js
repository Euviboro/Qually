/** @module hooks/useNewProtocol */

import { useState, useEffect } from "react";
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

/** @returns {LocalSubattribute} */
export const EMPTY_ATTRIBUTE = () => ({
  id: crypto.randomUUID(),
  subattributeText: "",
  subattributeOptions: ["", ""],
});

/**
 * Manages all state for the New Protocol form.
 *
 * `protocolAbbreviation` added — optional field used in calibration
 * round name generation. Can be set later by editing the protocol.
 */
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

  const canSave =
    !!clientId &&
    !!protocolName.trim() &&
    !!auditLogicType &&
    questions.length > 0 &&
    questions.every((q) => q.confirmed);

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
          subattributeText:    a.subattributeText,
          subattributeOptions: a.subattributeOptions.map((opt) => ({ optionLabel: opt })),
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
    questions,
    clients,              clientsLoading,
    canSave,              saving,         saveError,
    updateQuestion,       removeQuestion, addQuestion,
    handleSave,
  };
}