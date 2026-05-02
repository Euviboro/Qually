/**
 * @module pages/Calibration/CreateCalibrationPage
 *
 * 4-step wizard for creating a calibration round.
 * Step 1: Context (client → protocol → question)
 * Step 2: Interactions (up to 10 interaction IDs)
 * Step 3: Calibrators (dynamic searchable selects, creator locked, up to 10, min 2)
 * Step 4: Expert + Review
 *
 * Eligible calibrators: QA hierarchy_level > 3 (Sr. Specialist and below),
 * OPERATIONS hierarchy_level > 4 (Supervisor, Team Leader, Team Member).
 * All must be assigned to the selected client.
 */

import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { getClients } from "../../api/clients";
import { getProtocols } from "../../api/protocols";
import { getCalibrationEligibleUsers } from "../../api/users";
import { createRound } from "../../api/calibration";
import { COPC_CATEGORY_META } from "../../constants";
import { SearchableSelect } from "../../components/ui/SearchableSelect";

const STEPS = [
  { label: "Context" },
  { label: "Interactions" },
  { label: "Calibrators" },
  { label: "Expert & Review" },
];

function StepIndicator({ current }) {
  return (
    <div className="flex items-center gap-0 mb-8">
      {STEPS.map((step, idx) => {
        const done   = idx < current;
        const active = idx === current;
        return (
          <div key={idx} className="flex items-center flex-1 last:flex-none">
            <div className="flex flex-col items-center gap-1">
              <div className={[
                "w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold border-2 transition-all",
                done   ? "bg-lsg-blue border-lsg-blue text-white"
                : active ? "bg-bg-primary border-lsg-blue text-lsg-blue"
                : "bg-bg-tertiary border-border-sec text-text-ter",
              ].join(" ")}>
                {done ? (
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                    <path d="M2 7l3.5 3.5L12 3" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                ) : idx + 1}
              </div>
              <span className={`text-[10px] font-medium whitespace-nowrap ${active ? "text-lsg-blue" : "text-text-ter"}`}>
                {step.label}
              </span>
            </div>
            {idx < STEPS.length - 1 && (
              <div className={`flex-1 h-0.5 mx-2 mb-4 transition-all ${done ? "bg-lsg-blue" : "bg-border-sec"}`} />
            )}
          </div>
        );
      })}
    </div>
  );
}

export default function CreateCalibrationPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [step, setStep] = useState(0);

  const [clients,          setClients]          = useState([]);
  const [clientId,         setClientId]         = useState(null);
  const [protocols,        setProtocols]        = useState([]);
  const [protocolId,       setProtocolId]       = useState(null);
  const [questionId,       setQuestionId]       = useState(null);
  const [loadingClients,   setLoadingClients]   = useState(true);
  const [loadingProtocols, setLoadingProtocols] = useState(false);

  const [interactionIds, setInteractionIds] = useState([""]);

  const [eligibleUsers,  setEligibleUsers]  = useState([]);
  const [loadingUsers,   setLoadingUsers]   = useState(false);
  // Index 0 = creator (locked). Subsequent entries are null or a userId.
  const [calibratorRows, setCalibratorRows] = useState([user.userId, null]);

  const [expertUserId, setExpertUserId] = useState(null);
  const [submitting,   setSubmitting]   = useState(false);
  const [submitError,  setSubmitError]  = useState(null);

  useEffect(() => {
    getClients().then(setClients).finally(() => setLoadingClients(false));
  }, []);

  useEffect(() => {
    if (!clientId) { setProtocols([]); setProtocolId(null); setQuestionId(null); return; }
    setLoadingProtocols(true);
    setProtocolId(null);
    setQuestionId(null);
    getProtocols(clientId)
      .then((all) => setProtocols(all.filter(p => p.protocolStatus === "FINALIZED")))
      .finally(() => setLoadingProtocols(false));
  }, [clientId]);

  useEffect(() => {
    if (!clientId) { setEligibleUsers([]); setCalibratorRows([user.userId, null]); return; }
    setLoadingUsers(true);
    getCalibrationEligibleUsers(clientId)
      .then(setEligibleUsers)
      .finally(() => setLoadingUsers(false));
  }, [clientId, user.userId]);

  const selectedProtocol = protocols.find(p => p.protocolId === protocolId);
  const questions        = selectedProtocol?.auditQuestions ?? [];
  const selectedQuestion = questions.find(q => q.questionId === questionId);

  const clientOptions   = clients.map(c => ({ value: c.clientId,   label: c.clientName }));
  const protocolOptions = protocols.map(p => ({ value: p.protocolId, label: p.protocolName }));
  const questionOptions = questions.map(q => ({
    value: q.questionId,
    label: `[${q.category}] ${q.questionText}`,
  }));

  const validInteractionIds        = interactionIds.filter(id => id.trim() !== "");
  const uniqueInteractionIds       = [...new Set(validInteractionIds.map(id => id.trim()))];
  const hasDuplicateInteractionIds = uniqueInteractionIds.length !== validInteractionIds.length;

  const filledCalibratorIds       = calibratorRows.filter(Boolean);
  const hasDuplicateCalibratorIds = new Set(filledCalibratorIds).size !== filledCalibratorIds.length;

  const creatorEntry      = { userId: user.userId, fullName: user.fullName, roleName: user.roleName };
  const reviewParticipants = [
    creatorEntry,
    ...eligibleUsers.filter(u => filledCalibratorIds.includes(u.userId) && u.userId !== user.userId),
  ];
  const expertOptions = reviewParticipants.map(u => ({
    value: u.userId,
    label: `${u.fullName} — ${u.roleName ?? ""}`,
  }));

  const step1Valid = !!clientId && !!protocolId && !!questionId;
  const step2Valid = uniqueInteractionIds.length > 0 && !hasDuplicateInteractionIds;
  const step3Valid = filledCalibratorIds.length >= 2 && !hasDuplicateCalibratorIds;
  const step4Valid = !!expertUserId && filledCalibratorIds.includes(expertUserId);
  const canNext    = [step1Valid, step2Valid, step3Valid, step4Valid][step];

  const updateInteractionId = (idx, val) =>
    setInteractionIds(prev => prev.map((id, i) => i === idx ? val : id));
  const addInteractionId = () => {
    if (interactionIds.length < 10) setInteractionIds(prev => [...prev, ""]);
  };
  const removeInteractionId = (idx) => {
    if (interactionIds.length === 1) return;
    setInteractionIds(prev => prev.filter((_, i) => i !== idx));
  };

  const updateCalibratorRow = (idx, userId) => {
    if (expertUserId === calibratorRows[idx]) setExpertUserId(null);
    setCalibratorRows(prev => prev.map((id, i) => i === idx ? userId : id));
  };
  const addCalibratorRow = () => {
    if (calibratorRows.length < 10) setCalibratorRows(prev => [...prev, null]);
  };
  const removeCalibratorRow = (idx) => {
    if (expertUserId === calibratorRows[idx]) setExpertUserId(null);
    setCalibratorRows(prev => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const round = await createRound({
        clientId,
        protocolId,
        questionId,
        interactionIds:     uniqueInteractionIds,
        participantUserIds: filledCalibratorIds,
        expertUserId,
      });
      navigate(`/calibration/${round.roundId}`);
    } catch (err) {
      setSubmitError(err.message ?? "Failed to create round. Please try again.");
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-[680px] mx-auto px-8 py-10">
      <header className="mb-6">
        <button onClick={() => navigate("/calibration")}
          className="text-lsg-blue hover:text-lsg-blue-dark text-sm font-medium mb-4 flex items-center gap-1 transition-colors">
          ← Back to Calibration
        </button>
        <h1 className="text-2xl font-bold text-text-pri tracking-tight">New Calibration Round</h1>
      </header>

      <StepIndicator current={step} />

      {step === 0 && (
        <div className="bg-bg-primary border border-border-sec rounded-xl p-6 flex flex-col gap-5 shadow-card">
          <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Context</h2>
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-1.5">
              Client <span className="text-lsg-blue">*</span>
            </label>
            <SearchableSelect options={clientOptions} value={clientId} onChange={setClientId}
              placeholder="Select a client…" loading={loadingClients} emptyMessage="No clients found" />
          </div>
          <div>
            <label className={`block text-xs font-bold uppercase tracking-wider mb-1.5 ${clientId ? "text-text-sec" : "text-text-ter"}`}>
              Protocol <span className="text-lsg-blue">*</span>
            </label>
            <SearchableSelect options={protocolOptions} value={protocolId}
              onChange={(v) => { setProtocolId(v); setQuestionId(null); }}
              placeholder={clientId ? "Select a protocol…" : "Select a client first"}
              loading={loadingProtocols} disabled={!clientId}
              emptyMessage="No finalized protocols for this client" />
          </div>
          <div>
            <label className={`block text-xs font-bold uppercase tracking-wider mb-1.5 ${protocolId ? "text-text-sec" : "text-text-ter"}`}>
              Question to Calibrate <span className="text-lsg-blue">*</span>
            </label>
            <SearchableSelect options={questionOptions} value={questionId} onChange={setQuestionId}
              placeholder={protocolId ? "Select a question…" : "Select a protocol first"}
              disabled={!protocolId} emptyMessage="No questions in this protocol" />
            {selectedQuestion && (
              <div className="mt-2 px-3 py-2 rounded-lg bg-bg-secondary border border-border-ter">
                <span className="inline-block text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full mb-1"
                  style={{
                    background: `var(${COPC_CATEGORY_META[selectedQuestion.category]?.bgVar ?? "--color-bg-tertiary"})`,
                    color: `var(${COPC_CATEGORY_META[selectedQuestion.category]?.textVar ?? "--color-text-sec"})`,
                  }}>
                  {selectedQuestion.category}
                </span>
                <p className="text-sm text-text-pri">{selectedQuestion.questionText}</p>
              </div>
            )}
          </div>
        </div>
      )}

      {step === 1 && (
        <div className="bg-bg-primary border border-border-sec rounded-xl p-6 flex flex-col gap-5 shadow-card">
          <div>
            <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Interaction IDs</h2>
            <p className="text-xs text-text-ter mt-1">
              Enter the ID of each interaction to be calibrated. Typically 1–3. Each must be unique.
            </p>
          </div>
          <div className="flex flex-col gap-2">
            {interactionIds.map((id, idx) => (
              <div key={idx} className="flex items-center gap-2">
                <span className="text-xs font-mono text-text-ter w-5 shrink-0 text-right">{idx + 1}.</span>
                <input type="text" value={id} onChange={(e) => updateInteractionId(idx, e.target.value)}
                  placeholder={`Interaction ID ${idx + 1}`}
                  className="flex-1 px-3 py-2 text-sm font-mono rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue transition-all" />
                <button type="button" onClick={() => removeInteractionId(idx)}
                  disabled={interactionIds.length === 1}
                  className="w-7 h-7 flex items-center justify-center rounded text-text-ter hover:text-error-on hover:bg-error-surface transition-colors disabled:opacity-30">
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <path d="M2 2l8 8M10 2l-8 8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                </button>
              </div>
            ))}
          </div>
          {hasDuplicateInteractionIds && (
            <p className="text-xs text-error-on">Duplicate interaction IDs — each must be unique.</p>
          )}
          {interactionIds.length < 10 && (
            <button type="button" onClick={addInteractionId}
              className="flex items-center gap-1.5 text-xs font-bold text-lsg-blue hover:text-lsg-blue-dark transition-colors self-start">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
              </svg>
              Add interaction
            </button>
          )}
        </div>
      )}

      {step === 2 && (
        <div className="bg-bg-primary border border-border-sec rounded-xl p-6 flex flex-col gap-5 shadow-card">
          <div>
            <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Calibrators</h2>
          </div>
          {loadingUsers ? (
            <div className="flex items-center gap-2 text-text-ter text-sm py-4">
              <div className="w-4 h-4 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin"/>
              Loading eligible calibrators…
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {calibratorRows.map((rowUserId, idx) => {
                const isCreator = idx === 0;
                const takenIds  = calibratorRows.filter((id, i) => i !== idx && Boolean(id));
                const rowOptions = eligibleUsers
                  .filter(u => !takenIds.includes(u.userId))
                  .map(u => ({
                    value: u.userId,
                    label: `${u.fullName} — ${u.roleName} (${u.department})`,
                  }));
                return (
                  <div key={idx} className="flex items-center gap-2">
                    <span className="text-xs font-mono text-text-ter w-5 shrink-0 text-right">{idx + 1}.</span>
                    {isCreator ? (
                      <div className="flex-1 px-3 py-2 text-sm rounded-md border border-border-ter bg-bg-secondary text-text-sec flex items-center justify-between">
                        <span>{user.fullName}</span>
                        <span className="text-[10px] text-lsg-blue font-bold ml-2">You</span>
                      </div>
                    ) : (
                      <div className="flex-1">
                        <SearchableSelect
                          options={rowOptions}
                          value={rowUserId || null}
                          onChange={(val) => updateCalibratorRow(idx, val)}
                          placeholder="Select a calibrator…"
                          emptyMessage="No eligible calibrators for this client"
                        />
                      </div>
                    )}
                    {!isCreator && (
                      <button type="button" onClick={() => removeCalibratorRow(idx)}
                        className="w-7 h-7 flex items-center justify-center rounded text-text-ter hover:text-error-on hover:bg-error-surface transition-colors shrink-0">
                        <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                          <path d="M2 2l8 8M10 2l-8 8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                        </svg>
                      </button>
                    )}
                  </div>
                );
              })}
              {calibratorRows.length < 10 && (
                <button type="button" onClick={addCalibratorRow}
                  className="flex items-center gap-1.5 text-xs font-bold text-lsg-blue hover:text-lsg-blue-dark transition-colors self-start mt-1">
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                  Add calibrator
                </button>
              )}
              {filledCalibratorIds.length < 2}
              {hasDuplicateCalibratorIds && (
                <p className="text-xs text-error-on mt-1">Each calibrator can only be added once.</p>
              )}
            </div>
          )}
        </div>
      )}

      {step === 3 && (
        <div className="flex flex-col gap-4">
          <div className="bg-bg-primary border border-border-sec rounded-xl p-6 shadow-card">
            <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-4">Expert</h2>
            <p className="text-xs text-text-ter mb-3">
              The expert's answer is the reference — all other calibrators' answers are compared against it.
              Their identity is hidden from other participants.
            </p>
            <SearchableSelect options={expertOptions} value={expertUserId} onChange={setExpertUserId}
              placeholder="Select the expert…" emptyMessage="No calibrators selected" />
          </div>
          <div className="bg-bg-primary border border-border-sec rounded-xl p-6 shadow-card">
            <h2 className="text-[10px] font-bold text-text-ter uppercase tracking-widest mb-4">Review</h2>
            <div className="flex flex-col gap-3 text-sm">
              <Row label="Client"       value={clients.find(c => c.clientId === clientId)?.clientName} />
              <Row label="Protocol"     value={selectedProtocol?.protocolName} />
              <Row label="Question"     value={selectedQuestion?.questionText} />
              <Row label="Interactions" value={`${uniqueInteractionIds.length} interaction${uniqueInteractionIds.length !== 1 ? "s" : ""}: ${uniqueInteractionIds.join(", ")}`} />
              <Row label="Calibrators"  value={`${filledCalibratorIds.length} calibrator${filledCalibratorIds.length !== 1 ? "s" : ""}`} />
              <Row label="Expert"       value={reviewParticipants.find(p => p.userId === expertUserId)?.fullName ?? "—"} />
            </div>
          </div>
          {submitError && (
            <p className="text-xs text-error-on bg-error-surface px-3 py-2 rounded-lg">{submitError}</p>
          )}
        </div>
      )}

      <div className="flex items-center justify-between mt-6">
        <button type="button"
          onClick={() => step === 0 ? navigate("/calibration") : setStep(s => s - 1)}
          className="px-4 py-2 text-sm font-medium text-text-sec border border-border-sec rounded-md hover:bg-bg-secondary transition-colors">
          {step === 0 ? "Cancel" : "← Back"}
        </button>
        {step < 3 ? (
          <button type="button" onClick={() => setStep(s => s + 1)} disabled={!canNext}
            className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
            Next →
          </button>
        ) : (
          <button type="button" onClick={handleSubmit} disabled={!step4Valid || submitting}
            className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
            {submitting ? "Creating…" : "Create Round"}
          </button>
        )}
      </div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex gap-3">
      <span className="text-text-ter w-28 shrink-0">{label}</span>
      <span className="text-text-pri font-medium">{value ?? "—"}</span>
    </div>
  );
}