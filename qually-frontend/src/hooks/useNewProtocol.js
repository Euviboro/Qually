/** @module hooks/useNewProtocol */

import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getClients } from "../api/clients";
import { createProtocol } from "../api/protocols";

/**
 * @typedef {Object} LocalQuestion
 * @property {string}              id            - Ephemeral UUID used as React key.
 * @property {string}              questionText
 * @property {string}              category      - COPC category key (e.g. `"CUSTOMER"`).
 * @property {boolean}             confirmed
 * @property {LocalSubattribute[]} subattributes
 */

/**
 * @typedef {Object} LocalSubattribute
 * @property {string}   id
 * @property {string}   subattributeText
 * @property {string[]} subattributeOptions - Answer choice labels.
 */

/**
 * @typedef {Object} UseNewProtocolResult
 * @property {number|null} clientId
 * @property {(id: number) => void} setClientId
 * @property {string} protocolName
 * @property {(name: string) => void} setProtocolName
 * @property {string} version
 * @property {(v: string) => void} setVersion
 * @property {"STANDARD"|"ACCOUNTABILITY"|null} auditLogicType
 * @property {(t: string) => void} setAuditLogicType
 * @property {LocalQuestion[]} questions
 * @property {import('../api/clients').ClientResponseDTO[]} clients
 * @property {boolean} clientsLoading
 * @property {boolean} canSave
 * @property {boolean} saving
 * @property {string|null} saveError
 * @property {(id: string, updated: LocalQuestion) => void} updateQuestion
 * @property {(id: string) => void} removeQuestion
 * @property {() => void} addQuestion
 * @property {(status?: "DRAFT"|"FINALIZED") => Promise<void>} handleSave
 */

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
 * `auditLogicType` has been added as a required field, aligning with the
 * {@code audit_logic_type NOT NULL} column on {@code audit_protocols}.
 * `canSave` now requires it to be set alongside the other fields.
 *
 * @returns {UseNewProtocolResult}
 */
export function useNewProtocol() {
  const navigate = useNavigate();

  const [clientId,        setClientId]        = useState(null);
  const [protocolName,    setProtocolName]     = useState("");
  const [version,         setVersion]          = useState("1");
  const [auditLogicType,  setAuditLogicType]   = useState(null);
  const [questions,       setQuestions]        = useState([EMPTY_QUESTION()]);
  const [clients,         setClients]          = useState([]);
  const [clientsLoading,  setClientsLoading]   = useState(true);
  const [saving,          setSaving]           = useState(false);
  const [saveError,       setSaveError]        = useState(null);

  useEffect(() => {
    getClients()
      .then(setClients)
      .catch(() => {})
      .finally(() => setClientsLoading(false));
  }, []);

  /**
   * All required fields must be filled and every question confirmed.
   * `auditLogicType` is now part of this check.
   */
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

  /**
   * Serializes the form and POSTs to `/protocols`.
   * Navigates to the new protocol's detail page on success.
   *
   * @param {"DRAFT"|"FINALIZED"} [status="DRAFT"]
   */
  const handleSave = async (status = "DRAFT") => {
    setSaving(true);
    setSaveError(null);

    const payload = {
      clientId,
      protocolName:    protocolName.trim(),
      protocolVersion: parseInt(version, 10) || 1,
      protocolStatus:  status,
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
    clientId,       setClientId,
    protocolName,   setProtocolName,
    version,        setVersion,
    auditLogicType, setAuditLogicType,
    questions,
    clients,        clientsLoading,
    canSave,        saving,         saveError,
    updateQuestion, removeQuestion, addQuestion,
    handleSave,
  };
}
