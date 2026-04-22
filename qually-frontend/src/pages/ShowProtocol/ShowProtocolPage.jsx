import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { createAuditQuestion, updateAuditQuestion } from "../../api/questions";
import { finalizeProtocol, updateProtocolName } from "../../api/protocols";
import { useProtocol } from "../../hooks/useProtocol";
import { QuestionEditModal } from "../../components/ui/QuestionEditModal";

/**
 * Produces a blank question object for the "Add Question" modal.
 * `questionId: null` signals to `useQuestionForm` that this is a create,
 * not an edit. `protocolId` is populated immediately so the POST body is
 * complete without any transformation in the handler.
 *
 * @param {number|string} protocolId - The current protocol's numeric ID.
 * @returns {import('../../hooks/useQuestionForm').QuestionFormData}
 */
const blankQuestion = (protocolId) => ({
  questionId:    null,
  protocolId:    parseInt(protocolId, 10),
  questionText:  "",
  category:      "",
  subattributes: [],
});

/**
 * Page component for displaying and managing a specific Audit Protocol.
 * Includes inline-editing for the protocol name, adding/editing questions,
 * and the draft/finalization workflow.
 *
 * @returns {JSX.Element}
 */
export default function ShowProtocolPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const { protocol, loading, error, refetch } = useProtocol(id);

  // ── Edit-question modal state ──────────────────────────────
  const [editingQuestion, setEditingQuestion] = useState(null);
  const [editSaveError,   setEditSaveError]   = useState(null);

  // ── Add-question modal state ───────────────────────────────
  const [isAddingQuestion, setIsAddingQuestion] = useState(false);
  const [addSaveError,     setAddSaveError]     = useState(null);

  // ── Inline name-editor state ───────────────────────────────
  const [isEditingName, setIsEditingName] = useState(false);
  const [editedName,    setEditedName]    = useState("");
  const [isSavingName,  setIsSavingName]  = useState(false);

  // ── Name handlers ──────────────────────────────────────────

  const handleEditNameClick = () => {
    setEditedName(protocol.protocolName);
    setIsEditingName(true);
  };

  const handleCancelEditName = () => {
    setIsEditingName(false);
    setEditedName("");
  };

  const handleSaveName = async () => {
    const trimmedName = editedName.trim();
    if (!trimmedName || trimmedName === protocol.protocolName) {
      setIsEditingName(false);
      return;
    }
    try {
      setIsSavingName(true);
      await updateProtocolName(protocol.protocolId, trimmedName);
      await refetch();
      setIsEditingName(false);
    } catch (err) {
      alert("Error updating protocol name: " + (err.response?.data?.message || err.message));
    } finally {
      setIsSavingName(false);
    }
  };

  // ── Edit-question handlers ─────────────────────────────────

  const handleEditClick = (question) => {
    setEditSaveError(null);
    setEditingQuestion(question);
  };

  const handleCloseEditModal = () => {
    setEditingQuestion(null);
    setEditSaveError(null);
  };

  const handleSaveQuestion = async (updatedData) => {
    try {
      await updateAuditQuestion(updatedData.questionId, updatedData);
      refetch();
      setEditingQuestion(null);
      setEditSaveError(null);
    } catch (err) {
      setEditSaveError(err.message);
    }
  };

  // ── Add-question handlers ──────────────────────────────────

  const handleOpenAddModal = () => {
    setAddSaveError(null);
    setIsAddingQuestion(true);
  };

  const handleCloseAddModal = () => {
    setIsAddingQuestion(false);
    setAddSaveError(null);
  };

  /**
   * Creates a new question via POST /questions.
   * `formData.protocolId` is already populated by `blankQuestion(id)` so no
   * extra argument is needed — just pass formData straight to the API.
   *
   * @param {import('../../hooks/useQuestionForm').QuestionFormData} formData
   */
  const handleCreateQuestion = async (formData) => {
    try {
      // FIX: was createAuditQuestion(id, formData) — the function takes one
      // argument (the DTO). Passing `id` as the first arg treated it as the
      // DTO and silently dropped formData, sending the wrong body to the server.
      await createAuditQuestion(formData);
      await refetch();
      setIsAddingQuestion(false);
      setAddSaveError(null);
    } catch (err) {
      // FIX: was alert() — errors should surface inside the modal so the user
      // can see what went wrong without losing their draft input.
      setAddSaveError(err.message);
    }
  };

  // ── Finalize handler ───────────────────────────────────────

  const handleFinalize = async () => {
    if (window.confirm("Once finalized, you cannot edit these questions or the protocol name. Continue?")) {
      try {
        await finalizeProtocol(id);
        refetch();
      } catch (err) {
        alert("Error finalizing protocol: " + err.message);
      }
    }
  };

  // ── Loading / error guards ─────────────────────────────────

  if (loading) {
    return (
      <div className="p-8 flex items-center gap-3 text-gray-400">
        <div className="w-5 h-5 border-2 border-gray-200 border-t-blue-600 rounded-full animate-spin" />
        Loading protocol...
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 m-8 text-red-500 bg-red-50 border border-red-100 rounded-lg">
        Error: {error}
      </div>
    );
  }

  if (!protocol) {
    return <div className="p-8 text-gray-500">Protocol not found.</div>;
  }

  const isDraft = protocol.protocolStatus !== "FINALIZED";

  return (
    <div className="max-w-[1000px] mx-auto px-8 py-10">

      {/* ── Header ──────────────────────────────────────────── */}
      <header className="mb-8 flex justify-between items-start">
        <div className="flex-1">
          <button
            onClick={() => navigate("/")}
            className="text-blue-600 hover:text-blue-700 text-sm font-medium mb-4 flex items-center gap-1 transition-colors"
          >
            ← Back to Dashboard
          </button>

          {/* Inline name editor */}
          {isEditingName ? (
            <div className="flex items-center gap-3 mb-1">
              <input
                type="text"
                value={editedName}
                onChange={(e) => setEditedName(e.target.value)}
                autoFocus
                disabled={isSavingName}
                className="text-3xl font-bold text-gray-900 tracking-tight m-0 border-b-2 border-blue-500 focus:outline-none bg-transparent w-full max-w-md py-1"
                onKeyDown={(e) => {
                  if (e.key === "Enter")  handleSaveName();
                  if (e.key === "Escape") handleCancelEditName();
                }}
              />
              <button
                onClick={handleSaveName}
                disabled={isSavingName}
                className="p-1.5 text-green-600 bg-green-50 hover:bg-green-100 rounded-md transition-colors"
                title="Save Name (Enter)"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                </svg>
              </button>
              <button
                onClick={handleCancelEditName}
                disabled={isSavingName}
                className="p-1.5 text-red-500 bg-red-50 hover:bg-red-100 rounded-md transition-colors"
                title="Cancel (Esc)"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-3 mb-1 group">
              <h1 className="text-3xl font-bold text-gray-900 tracking-tight m-0">
                {protocol.protocolName}
              </h1>
              {isDraft && (
                <button
                  onClick={handleEditNameClick}
                  className="p-1.5 text-gray-400 opacity-0 group-hover:opacity-100 hover:text-blue-600 hover:bg-blue-50 rounded-md transition-all"
                  title="Edit Protocol Name"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                  </svg>
                </button>
              )}
            </div>
          )}

          <p className="text-gray-500 mt-1">
            Version {protocol.protocolVersion} • {protocol.auditQuestions?.length ?? 0} Questions
          </p>
        </div>

        <div className="flex flex-col items-end gap-3 ml-4">
          <span
            className={`px-3 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider border ${
              isDraft
                ? "bg-amber-100 text-amber-700 border-amber-200"
                : "bg-green-100 text-green-700 border-green-200"
            }`}
          >
            {protocol.protocolStatus || "DRAFT"}
          </span>

          {isDraft && (
            <button
              onClick={handleFinalize}
              className="text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 px-4 py-2 rounded shadow-sm transition-all whitespace-nowrap"
            >
              Finalize Protocol
            </button>
          )}
        </div>
      </header>

      {/* ── Questions toolbar (DRAFT only) ───────────────────── */}
      {isDraft && (
        <div className="flex items-center justify-between mb-4 px-1">
          <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Questions</span>
          <button
            onClick={handleOpenAddModal}
            className="flex items-center gap-1.5 text-xs font-bold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-md transition-all"
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
            Add Question
          </button>
        </div>
      )}

      {/* ── Questions list ───────────────────────────────────── */}
      {protocol.auditQuestions?.length === 0 ? (
        <div className="text-center py-20 bg-gray-50 rounded-xl border-2 border-dashed border-gray-200">
          <p className="text-gray-400">No questions added to this protocol yet.</p>
          {isDraft && (
            <button
              onClick={handleOpenAddModal}
              className="mt-4 text-sm font-bold text-blue-600 hover:text-blue-700 transition-colors"
            >
              + Add the first question
            </button>
          )}
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          {protocol.auditQuestions?.map((q, index) => (
            <div
              key={q.questionId}
              className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden"
            >
              <div className="p-6">
                <div className="flex justify-between items-start mb-4">
                  <div className="flex-1">
                    <div className="flex gap-2.5 items-center mb-2">
                      <span className="font-mono text-xs text-gray-400">
                        #{String(index + 1).padStart(2, "0")}
                      </span>
                      <span className="text-[10px] font-bold text-blue-600 uppercase tracking-widest bg-blue-50 px-2 py-0.5 rounded">
                        {q.category}
                      </span>
                    </div>
                    <p className="text-lg font-semibold text-gray-900 leading-snug m-0">
                      {q.questionText}
                    </p>
                  </div>

                  {isDraft && (
                    <button
                      onClick={() => handleEditClick(q)}
                      className="ml-8 px-4 py-2 rounded-md border border-gray-200 bg-white text-gray-700 font-semibold text-sm hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm shrink-0"
                    >
                      Edit
                    </button>
                  )}
                </div>

                {/* Subattributes */}
                {q.subattributes && q.subattributes.length > 0 && (
                  <div className="mt-4 pt-4 border-t border-gray-50 space-y-4">
                    {q.subattributes.map((sub) => (
                      <div key={sub.subattributeId} className="pl-4 border-l-2 border-gray-100">
                        <p className="text-sm font-medium text-gray-600 mb-2">
                          {sub.subattributeText}
                        </p>
                        <div className="flex flex-wrap gap-2">
                          {sub.subattributeOptions?.map((opt) => (
                            <span
                              key={opt.subattributeOptionId}
                              className="text-[10px] font-bold bg-gray-50 text-gray-500 border border-gray-200 px-2 py-1 rounded uppercase"
                            >
                              {opt.optionLabel}
                            </span>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── Edit question modal ──────────────────────────────── */}
      <QuestionEditModal
        isOpen={!!editingQuestion}
        question={editingQuestion}
        onClose={handleCloseEditModal}
        onSave={handleSaveQuestion}
        saveError={editSaveError}
      />

      {/* ── Add question modal ───────────────────────────────── */}
      {/*
        Pass a fresh blank question each render so useQuestionForm's useEffect
        picks up the template on every open. The key forces the modal to remount
        when it opens, which resets form state cleanly.
      */}
      <QuestionEditModal
        isOpen={isAddingQuestion}
        question={isAddingQuestion ? blankQuestion(id) : null}
        onClose={handleCloseAddModal}
        onSave={handleCreateQuestion}
        saveError={addSaveError}
      />
    </div>
  );
}