import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { updateAuditQuestion } from "../../api/questions";
import { useProtocol } from "../../hooks/useProtocol";
import { QuestionEditModal } from "../../components/ui/QuestionEditModal";

export default function ShowProtocolPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const { protocol, loading, error, refetch } = useProtocol(id);

  const [editingQuestion, setEditingQuestion] = useState(null);
  const [saveError, setSaveError] = useState(null);

  const handleEditClick = (question) => {
    setSaveError(null);
    setEditingQuestion(question);
  };

  const handleCloseModal = () => {
    setEditingQuestion(null);
    setSaveError(null);
  };

  const handleSaveQuestion = async (updatedData) => {
    try {
      await updateAuditQuestion(updatedData.questionId, updatedData);
      refetch();           // silent background refresh — no loading flash
      setEditingQuestion(null);
      setSaveError(null);
    } catch (err) {
      setSaveError(err.message);
    }
  };

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

  return (
    <div className="max-w-[1000px] mx-auto px-8 py-10">
      {/* Header */}
      <header className="mb-8 flex justify-between items-start">
        <div>
          <button
            onClick={() => navigate("/")}
            className="text-blue-600 hover:text-blue-700 text-sm font-medium mb-4 flex items-center gap-1 transition-colors"
          >
            ← Back to Dashboard
          </button>
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight m-0">
            {protocol.protocolName}
          </h1>
          <p className="text-gray-500 mt-1">
            Version {protocol.protocolVersion} • {protocol.auditQuestions?.length ?? 0} Questions
          </p>
        </div>

        <span
          className={`px-3 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider ${
            protocol.isFinalized
              ? "bg-green-100 text-green-700"
              : "bg-amber-100 text-amber-700 border border-amber-200"
          }`}
        >
          {protocol.isFinalized ? "Finalized" : "Draft Mode"}
        </span>
      </header>

      {/* Questions list */}
      {protocol.auditQuestions?.length === 0 ? (
        <div className="text-center py-20 bg-gray-50 rounded-xl border-2 border-dashed border-gray-200">
          <p className="text-gray-400">No questions added to this protocol yet.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {protocol.auditQuestions?.map((q, index) => (
            <div
              key={q.questionId}
              className="p-6 bg-white border border-gray-200 rounded-xl flex justify-between items-center shadow-sm hover:border-gray-300 transition-all"
            >
              <div className="flex-1">
                <div className="flex gap-2.5 items-center mb-2">
                  <span className="font-mono text-xs text-gray-400">
                    #{String(index + 1).padStart(2, "0")}
                  </span>
                  <span className="text-[10px] font-bold text-blue-600 uppercase tracking-widest bg-blue-50 px-2 py-0.5 rounded">
                    {q.category}
                  </span>
                </div>
                <p className="text-lg font-medium text-gray-800 m-0 leading-snug">
                  {q.questionText}
                </p>
              </div>

              <button
                onClick={() => handleEditClick(q)}
                className="ml-8 px-4 py-2 rounded-md border border-gray-200 bg-white text-gray-700 font-semibold text-sm hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm shrink-0"
              >
                Edit
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Modal */}
      <QuestionEditModal
        isOpen={!!editingQuestion}
        question={editingQuestion}
        onClose={handleCloseModal}
        onSave={handleSaveQuestion}
        saveError={saveError}
      />
    </div>
  );
}