import { useNavigate } from "react-router-dom";
import { useNewProtocol } from "../../hooks/useNewProtocol";
import { Btn } from "../../components/ui/Btn";
import { Label } from "../../components/ui/Label";
import { TextInput } from "../../components/ui/TextInput";
import { SectionCard } from "../../components/ui/SectionCard";
import { ClientSelector } from "./ClientSelector";
import { QuestionCard } from "./QuestionCard";

export default function NewProtocolPage() {
  const navigate = useNavigate();
  const {
    clientId, setClientId,
    protocolName, setProtocolName,
    version, setVersion,
    questions,
    clients, clientsLoading,
    canSave, saving, saveError,
    updateQuestion, removeQuestion, addQuestion,
    handleSave,
  } = useNewProtocol();

  return (
    <div className="py-10 px-6 max-w-3xl mx-auto">
      <header className="mb-10">
        <h1 className="text-4xl font-extrabold text-gray-900 tracking-tight mb-1">New Protocol</h1>
        <p className="text-gray-500">Define the protocol, add questions, and save as draft or finalize.</p>
      </header>

      {/* Protocol details */}
      <SectionCard className="mb-6">
        <h2 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-4">Protocol Details</h2>
        <div className="flex flex-col gap-5">
          <div>
            <Label required>Client</Label>
            <ClientSelector
              clients={clients}
              value={clientId}
              onChange={setClientId}
              loading={clientsLoading}
            />
          </div>
          <div className="flex gap-4">
            <div className="flex-1">
              <Label required>Protocol Name</Label>
              <TextInput
                value={protocolName}
                onChange={(e) => setProtocolName(e.target.value)}
                placeholder='e.g. "Customer Service Audit"'
              />
            </div>
            <div className="w-24">
              <Label>Version</Label>
              <TextInput
                value={version}
                onChange={(e) => setVersion(e.target.value.replace(/\D/g, ""))}
                placeholder="1"
              />
            </div>
          </div>
        </div>
      </SectionCard>

      {/* Questions */}
      <div className="space-y-4 mb-10">
        <div className="flex items-center justify-between px-1">
          <h2 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Questions</h2>
          <span className="text-xs text-gray-400 font-medium">
            {questions.filter((q) => q.confirmed).length} confirmed ·{" "}
            {questions.filter((q) => !q.confirmed).length} pending
          </span>
        </div>

        {questions.map((q, i) => (
          <QuestionCard
            key={q.id}
            question={q}
            index={i}
            onChange={(updated) => updateQuestion(q.id, updated)}
            onRemove={() => removeQuestion(q.id)}
            canRemove={questions.length > 1}
          />
        ))}

        <button
          onClick={addQuestion}
          className="w-full py-4 rounded-xl border-2 border-dashed border-gray-200 text-gray-400 font-bold hover:border-blue-300 hover:text-blue-500 transition-all flex items-center justify-center gap-2 group"
        >
          <span className="text-xl group-hover:scale-125 transition-transform">+</span> Add Another Question
        </button>
      </div>

      {/* Save error */}
      {saveError && (
        <div className="mb-4 px-4 py-3 bg-red-50 border border-red-100 text-red-600 text-sm rounded-lg">
          {saveError}
        </div>
      )}

      {/* Footer */}
      <footer className="sticky bottom-6 bg-white/80 backdrop-blur-md p-4 border border-gray-100 rounded-2xl shadow-xl flex items-center justify-between">
        <Btn variant="ghost" onClick={() => navigate("/")}>Cancel</Btn>
        <div className="flex gap-3">
          <Btn variant="secondary" onClick={() => handleSave(false)} disabled={saving || !canSave}>
            Save Draft
          </Btn>
          <Btn variant="primary" onClick={() => handleSave(true)} disabled={saving || !canSave}>
            {saving ? "Saving…" : "Finalize Protocol"}
          </Btn>
        </div>
      </footer>
    </div>
  );
}