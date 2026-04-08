import { useState, useEffect, useRef } from "react";
import { createClient } from "../../api/clients";

export  function AddClientModal({ isOpen, onClose, onClientCreated }) {
  const [clientName, setClientName] = useState("");
  const [saving, setSaving]         = useState(false);
  const [error, setError]           = useState(null);
  const inputRef                    = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setClientName("");
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
      const newClient = await createClient({ clientName: clientName.trim() });
      onClientCreated(newClient);
      onClose();
    } catch (err) {
      const serverErrorMessage = err.response?.data?.message || err.message || "Could not save client.";
      setError(serverErrorMessage);
      setSaving(false);
    }
  };

  return (
    /* Backdrop */
    <div
      onClick={onClose}
      className="fixed inset-0 z-[200] flex items-center justify-center p-4
                 bg-[rgba(0,20,50,0.35)] backdrop-blur-[2px]"
    >
      {/* Panel */}
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[420px] flex flex-col gap-5 
        bg-white rounded-[16px] border border-[#D8E6F2] 
        shadow-[0_8px_24px_rgba(0,47,101,0.14)]
        p-7 [animation:modal-in_0.18s_ease]"
      >
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <p className="text-[20px] font-bold text-[#002F65] tracking-tight mb-1">
              New Client
            </p>
            <p className="text-[13px] text-[#3A5272]">
              Register a new client.
            </p>
          </div>
          <button
            onClick={onClose}
            aria-label="Close"
            className="text-[#7A92AD] hover:text-[#002F65] text-lg leading-none
            p-1 rounded cursor-pointer bg-transparent border-none
            transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">

          {/* Client name */}
          <div>
            <label className="block text-[12px] font-semibold text-[#3A5272]
                              tracking-wide mb-1.5">
              Client name <span className="text-[#0096FF]">*</span>
            </label>
            <input
              ref={inputRef}
              type="text"
              value={clientName}
              onChange={(e) => setClientName(e.target.value)}
              placeholder='e.g. "Acme"'
              disabled={saving}
              required
              className="w-full px-3 py-[9px] text-[13px] font-sans
                         bg-white disabled:bg-[#F4F7FB]
                         text-[#002F65] placeholder:text-[#7A92AD]
                         border border-[#D8E6F2] rounded-[8px]
                         outline-none
                         focus:border-[#0096FF] focus:ring-3 focus:ring-[#0096FF]/10
                         transition-[border-color,box-shadow]"
            />
          </div>

          {/* Error */}
          {error && (
            <div className="px-3 py-2 text-[12px] rounded-[8px]
                            bg-[#FDECEA] text-[#7A1010]
                            border border-[rgba(226,75,74,0.2)]">
              {error}
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="px-4 py-2 text-[13px] font-medium font-sans
                         bg-[#F4F7FB] hover:bg-[#EAF0F7]
                         text-[#002F65]
                         border border-[#D8E6F2] rounded-[8px]
                         disabled:opacity-50 disabled:cursor-not-allowed
                         transition-colors cursor-pointer"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={saving || !clientName.trim()}
              className="inline-flex items-center gap-1.5
                         px-4 py-2 text-[13px] font-medium font-sans
                         bg-[#0096FF] hover:bg-[#006EF4]
                         text-white rounded-[8px]
                         disabled:opacity-45 disabled:cursor-not-allowed
                         transition-colors cursor-pointer"
            >
              {saving ? (
                <>
                  <span className="w-3 h-3 rounded-full border-2
                                   border-white/30 border-t-white
                                   [animation:spin_0.7s_linear_infinite]" />
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