import { useState, useEffect, useRef } from "react";
import { createClient } from "../../api/clients";

export function AddClientModal({ isOpen, onClose, onClientCreated }) {
  const [clientName,         setClientName]         = useState("");
  const [clientAbbreviation, setClientAbbreviation] = useState("");
  const [saving,             setSaving]             = useState(false);
  const [error,              setError]              = useState(null);
  const inputRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setClientName("");
      setClientAbbreviation("");
      setError(null);
      setSaving(false);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const onKey = (e) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!clientName.trim() || saving) return;
    setError(null);
    setSaving(true);
    try {
      const newClient = await createClient({
        clientName:         clientName.trim(),
        clientAbbreviation: clientAbbreviation.trim().toUpperCase() || undefined,
      });
      onClientCreated(newClient);
      onClose();
    } catch (err) {
      const serverErrorMessage = err.response?.data?.message || err.message || "Could not save client.";
      setError(serverErrorMessage);
      setSaving(false);
    }
  };

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-[rgba(0,20,50,0.35)] backdrop-blur-[2px]"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[420px] flex flex-col gap-5 bg-bg-primary rounded-xl border border-border-sec shadow-lg p-7 [animation:modal-in_0.18s_ease]"
      >
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <p className="text-[20px] font-bold text-text-pri tracking-tight mb-1">New Client</p>
            <p className="text-[13px] text-text-sec">Register a new client.</p>
          </div>
          <button
            onClick={onClose}
            aria-label="Close"
            className="text-text-ter hover:text-text-pri text-lg leading-none p-1 rounded cursor-pointer bg-transparent border-none transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-[12px] font-semibold text-text-sec tracking-wide mb-1.5">
              Client name <span className="text-lsg-blue">*</span>
            </label>
            <input
              ref={inputRef}
              type="text"
              value={clientName}
              onChange={(e) => setClientName(e.target.value)}
              placeholder='e.g. "Acme"'
              disabled={saving}
              required
              className="w-full px-3 py-[9px] text-[13px] font-sans bg-bg-primary disabled:bg-bg-secondary text-text-pri placeholder:text-text-ter border border-border-sec rounded-md outline-none focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10 transition-[border-color,box-shadow]"
            />
          </div>

          <div>
            <label className="block text-[12px] font-semibold text-text-sec tracking-wide mb-1.5">
              Abbreviation
              <span className="text-text-ter font-normal ml-1">(optional — used in calibration round names)</span>
            </label>
            <input
              type="text"
              value={clientAbbreviation}
              onChange={(e) => setClientAbbreviation(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ""))}
              placeholder='e.g. "DSV"'
              maxLength={10}
              disabled={saving}
              className="w-full px-3 py-[9px] text-[13px] font-mono bg-bg-primary disabled:bg-bg-secondary text-text-pri placeholder:text-text-ter border border-border-sec rounded-md outline-none focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10 transition-[border-color,box-shadow] uppercase tracking-widest"
            />
            <p className="text-[11px] text-text-ter mt-1">
              2–10 uppercase letters or digits. Auto-formatted as you type.
            </p>
          </div>

          {error && (
            <div className="px-3 py-2 text-[12px] rounded-md bg-error-surface text-error-on border border-[rgba(226,75,74,0.2)]">
              {error}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="px-4 py-2 text-[13px] font-medium font-sans bg-bg-secondary hover:bg-bg-tertiary text-text-pri border border-border-sec rounded-md disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving || !clientName.trim()}
              className="inline-flex items-center gap-1.5 px-4 py-2 text-[13px] font-medium font-sans bg-lsg-blue hover:bg-lsg-blue-dark text-white rounded-md disabled:opacity-45 disabled:cursor-not-allowed transition-colors cursor-pointer"
            >
              {saving ? (
                <>
                  <span className="w-3 h-3 rounded-full border-2 border-white/30 border-t-white [animation:spin_0.7s_linear_infinite]" />
                  Saving…
                </>
              ) : "Create client"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}