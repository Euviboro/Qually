/**
 * @module pages/NewProtocol/NewProtocolPage
 *
 * Form for creating a new audit protocol.
 * An Audit Logic Type selector has been added — it is now a required field
 * because `audit_logic_type NOT NULL` lives on `audit_protocols` in the DB.
 * Protocol Abbreviation added — optional, used in calibration round names.
 */

import { useNavigate } from "react-router-dom";
import { useNewProtocol } from "../../hooks/useNewProtocol";
import { Btn } from "../../components/ui/Btn";
import { Label } from "../../components/ui/Label";
import { TextInput } from "../../components/ui/TextInput";
import { SectionCard } from "../../components/ui/SectionCard";
import { ClientSelector } from "./ClientSelector";
import { QuestionCard } from "./QuestionCard";
import { AUDIT_LOGIC_TYPE_META } from "../../constants";

export default function NewProtocolPage() {
  const navigate = useNavigate();
  const {
    clientId,             setClientId,
    protocolName,         setProtocolName,
    protocolAbbreviation, setProtocolAbbreviation,
    version,              setVersion,
    auditLogicType,       setAuditLogicType,
    questions,
    clients,              clientsLoading,
    canSave,              saving,           saveError,
    updateQuestion,       removeQuestion,   addQuestion,
    handleSave,
  } = useNewProtocol();

  const logicTypes = Object.entries(AUDIT_LOGIC_TYPE_META).map(([value, m]) => ({ value, ...m }));

  return (
    <div className="py-10 px-6 max-w-3xl mx-auto">
      <header className="mb-10">
        <h1 className="text-4xl font-extrabold text-text-pri tracking-tight mb-1">New Protocol</h1>
        <p className="text-text-ter">Define the protocol, add questions, and save as draft or finalize.</p>
      </header>

      {/* Protocol details */}
      <SectionCard className="mb-6">
        <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-4">Protocol Details</h2>
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

          {/* Protocol Abbreviation — optional, used in calibration round names */}
          <div>
            <Label>
              Abbreviation
              <span className="text-text-ter font-normal ml-1 text-[11px]">
                (optional — used in calibration round names, e.g. "DSP")
              </span>
            </Label>
            <TextInput
              value={protocolAbbreviation}
              onChange={(e) =>
                setProtocolAbbreviation(
                  e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 10)
                )
              }
              placeholder='e.g. "DSP"'
              className="font-mono tracking-widest w-32"
            />
            <p className="text-[11px] text-text-ter mt-1">
              2–10 uppercase letters or digits. Auto-formatted as you type.
            </p>
          </div>

          {/* Audit Logic Type — required, stored on the protocol */}
          <div>
            <Label required>Audit Logic Type</Label>
            <p className="text-xs text-text-ter mb-3">
              Determines how NO answers affect the final score for all sessions using this protocol.
            </p>
            <div className="flex flex-col sm:flex-row gap-3">
              {logicTypes.map((lt) => {
                const isActive = auditLogicType === lt.value;
                return (
                  <button
                    key={lt.value}
                    type="button"
                    onClick={() => setAuditLogicType(lt.value)}
                    className={[
                      "flex-1 text-left px-4 py-3 rounded-lg border-2 transition-all",
                      isActive
                        ? "border-lsg-blue bg-bg-accent"
                        : "border-border-sec hover:border-border-pri bg-bg-primary",
                    ].join(" ")}
                  >
                    <p className={`text-sm font-bold mb-0.5 ${isActive ? "text-lsg-blue-dark" : "text-text-pri"}`}>
                      {lt.label}
                    </p>
                    <p className="text-xs text-text-ter">{lt.description}</p>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </SectionCard>

      {/* Questions */}
      <div className="space-y-4 mb-10">
        <div className="flex items-center justify-between px-1">
          <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Questions</h2>
          <span className="text-xs text-text-ter font-medium">
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
          className="w-full py-4 rounded-xl border-2 border-dashed border-border-sec text-text-ter font-bold hover:border-lsg-blue hover:text-lsg-blue transition-all flex items-center justify-center gap-2 group"
        >
          <span className="text-xl group-hover:scale-125 transition-transform">+</span>
          Add Another Question
        </button>
      </div>

      {saveError && (
        <div className="mb-4 px-4 py-3 bg-error-surface border border-[rgba(226,75,74,0.2)] text-error-on text-sm rounded-xl">
          {saveError}
        </div>
      )}

      <footer className="sticky bottom-6 bg-bg-primary/80 backdrop-blur-md p-4 border border-border-ter rounded-2xl shadow-lg flex items-center justify-between">
        <Btn variant="ghost" onClick={() => navigate("/")}>Cancel</Btn>
        <div className="flex gap-3">
          <Btn variant="secondary" onClick={() => handleSave("DRAFT")} disabled={saving || !canSave}>
            Save Draft
          </Btn>
          <Btn variant="primary" onClick={() => handleSave("FINALIZED")} disabled={saving || !canSave}>
            {saving ? "Saving…" : "Finalize Protocol"}
          </Btn>
        </div>
      </footer>
    </div>
  );
}