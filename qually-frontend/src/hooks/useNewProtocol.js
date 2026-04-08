import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getClients } from "../api/clients";
import { createProtocol, finalizeProtocol } from "../api/protocols";
import { createQuestion } from "../api/protocols";

const EMPTY_QUESTION = () => ({
  id: crypto.randomUUID(),
  text: "",
  category: "",
  confirmed: false,
  subattributes: [],
});

const EMPTY_ATTRIBUTE = () => ({
  id: crypto.randomUUID(),
  title: "",
  choices: ["", ""],
});

export { EMPTY_QUESTION, EMPTY_ATTRIBUTE };

export function useNewProtocol() {
  const navigate = useNavigate();

  const [clientId, setClientId]           = useState(null);
  const [protocolName, setProtocolName]   = useState("");
  const [version, setVersion]             = useState("1");
  const [questions, setQuestions]         = useState([EMPTY_QUESTION()]);
  const [clients, setClients]             = useState([]);
  const [clientsLoading, setClientsLoading] = useState(true);
  const [saving, setSaving]               = useState(false);
  const [saveError, setSaveError]         = useState(null);

  useEffect(() => {
    getClients()
      .then(setClients)
      .catch(() => {})
      .finally(() => setClientsLoading(false));
  }, []);

  const canSave = !!clientId && !!protocolName.trim() && questions.every((q) => q.confirmed);

  const updateQuestion = (id, updated) =>
    setQuestions((qs) => qs.map((q) => (q.id === id ? updated : q)));

  const removeQuestion = (id) =>
    setQuestions((qs) => qs.filter((q) => q.id !== id));

  const addQuestion = () =>
    setQuestions((qs) => [...qs, EMPTY_QUESTION()]);

  const handleSave = async (finalize = false) => {
    setSaving(true);
    setSaveError(null);
    try {
      const protocol = await createProtocol({
        clientId,
        protocolName: protocolName.trim(),
        protocolVersion: parseInt(version, 10) || 1,
      });

      await Promise.all(
        questions.map((q) =>
          createQuestion({
            protocolId: protocol.protocolId,
            questionText: q.text,
            category: q.category,
            subattributes: q.subattributes.map((a) => ({
              subattributeText: a.title,
              options: a.choices.filter(Boolean).map((c) => ({ optionText: c })),
            })),
          })
        )
      );

      if (finalize) await finalizeProtocol(protocol.protocolId);

      navigate(finalize ? `/protocols/${protocol.protocolId}` : "/");
    } catch (err) {
      setSaveError(err.message);
      setSaving(false);
    }
  };

  return {
    // form state
    clientId, setClientId,
    protocolName, setProtocolName,
    version, setVersion,
    questions,
    // clients
    clients, clientsLoading,
    // actions
    canSave, saving, saveError,
    updateQuestion, removeQuestion, addQuestion,
    handleSave,
  };
}