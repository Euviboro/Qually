/**
 * @module components/ui/StartSessionModal
 *
 * Two-step modal that collects the client and protocol for a new audit session.
 *
 * Flow:
 * 1. The user selects a client from a searchable list of all clients.
 * 2. Once a client is selected, its FINALIZED protocols are fetched.
 *    If the client has exactly one protocol it is auto-selected.
 * 3. Clicking "Start Session" calls `onConfirm` with the selected protocol
 *    object, then the parent navigates to the LogSessionPage.
 *
 * Only FINALIZED protocols are shown — drafts cannot be audited.
 */

import { useState, useEffect } from "react";
import { getClients } from "../../api/clients";
import { getProtocols } from "../../api/protocols";
import { SearchableSelect } from "./SearchableSelect";

/**
 * @param {Object}   props
 * @param {boolean}  props.isOpen
 *   Controls visibility. Returns `null` when `false`.
 * @param {() => void} props.onClose
 *   Called when the user dismisses the modal.
 * @param {(protocol: import('../../api/protocols').AuditProtocolResponseDTO) => void} props.onConfirm
 *   Called with the selected protocol object once the user clicks "Start Session".
 *   The parent is responsible for navigation.
 */
export function StartSessionModal({ isOpen, onClose, onConfirm }) {
  // ── Client state ───────────────────────────────────────────
  const [clients,        setClients]        = useState([]);
  const [clientsLoading, setClientsLoading] = useState(true);
  const [clientsError,   setClientsError]   = useState(null);
  const [selectedClient, setSelectedClient] = useState(null);

  // ── Protocol state ─────────────────────────────────────────
  const [protocols,        setProtocols]        = useState([]);
  const [protocolsLoading, setProtocolsLoading] = useState(false);
  const [protocolsError,   setProtocolsError]   = useState(null);
  const [selectedProtocol, setSelectedProtocol] = useState(null);

  // ── Fetch clients on mount ─────────────────────────────────
  useEffect(() => {
    if (!isOpen) return;
    setClientsLoading(true);
    setClientsError(null);
    getClients()
      .then(setClients)
      .catch((err) => setClientsError(err.message))
      .finally(() => setClientsLoading(false));
  }, [isOpen]);

  // ── Fetch protocols whenever the selected client changes ───
  useEffect(() => {
    if (!selectedClient) {
      setProtocols([]);
      setSelectedProtocol(null);
      return;
    }

    setProtocolsLoading(true);
    setProtocolsError(null);
    setSelectedProtocol(null);

    getProtocols(selectedClient)
      .then((all) => {
        // Only FINALIZED protocols can be audited
        const finalized = all.filter((p) => p.protocolStatus === "FINALIZED");
        setProtocols(finalized);

        // Auto-select when there is exactly one option
        if (finalized.length === 1) {
          setSelectedProtocol(finalized[0].protocolId);
        }
      })
      .catch((err) => setProtocolsError(err.message))
      .finally(() => setProtocolsLoading(false));
  }, [selectedClient]);

  // ── Reset when the modal closes ────────────────────────────
  useEffect(() => {
    if (!isOpen) {
      setSelectedClient(null);
      setSelectedProtocol(null);
      setProtocols([]);
      setClientsError(null);
      setProtocolsError(null);
    }
  }, [isOpen]);

  // ── Escape key ─────────────────────────────────────────────
  useEffect(() => {
    if (!isOpen) return;
    const handler = (e) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  // ── Derived data ───────────────────────────────────────────

  /** Options shaped for `SearchableSelect`. */
  const clientOptions = clients.map((c) => ({
    value: c.clientId,
    label: c.clientName,
  }));

  const protocolOptions = protocols.map((p) => ({
    value: p.protocolId,
    label: `${p.protocolName} (v${p.protocolVersion})`,
  }));

  /** The full protocol object for the currently selected protocol ID. */
  const protocolObject = protocols.find((p) => p.protocolId === selectedProtocol) ?? null;

  const canStart = Boolean(selectedClient && selectedProtocol && protocolObject);

  const handleConfirm = () => {
    if (!canStart) return;
    onConfirm(protocolObject);
  };

  // ── Render ─────────────────────────────────────────────────

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-[rgba(0,20,50,0.35)] backdrop-blur-[2px]"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[460px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-7 flex flex-col gap-6 [animation:modal-in_0.18s_ease]"
      >
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-xl font-bold text-text-pri tracking-tight mb-1">
              Start Audit Session
            </h2>
            <p className="text-sm text-text-sec">
              Choose which client and protocol to audit.
            </p>
          </div>
          <button
            onClick={onClose}
            aria-label="Close"
            className="text-text-ter hover:text-text-pri p-1 rounded transition-colors"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>

        {/* Body */}
        <div className="flex flex-col gap-5">

          {/* Client selector */}
          <div>
            <label className="block text-xs font-bold text-text-sec uppercase tracking-wider mb-2">
              Client <span className="text-lsg-blue">*</span>
            </label>
            <SearchableSelect
              options={clientOptions}
              value={selectedClient}
              onChange={setSelectedClient}
              placeholder="Select a client…"
              searchPlaceholder="Search clients…"
              loading={clientsLoading}
              emptyMessage="No clients found"
            />
            {clientsError && (
              <p className="mt-1.5 text-xs text-error-on">{clientsError}</p>
            )}
          </div>

          {/* Protocol selector — only shown once a client is chosen */}
          <div>
            <label className={`block text-xs font-bold uppercase tracking-wider mb-2 transition-colors ${selectedClient ? "text-text-sec" : "text-text-ter"}`}>
              Protocol <span className="text-lsg-blue">*</span>
            </label>
            <SearchableSelect
              options={protocolOptions}
              value={selectedProtocol}
              onChange={setSelectedProtocol}
              placeholder={selectedClient ? "Select a protocol…" : "Select a client first"}
              searchPlaceholder="Search protocols…"
              loading={protocolsLoading}
              disabled={!selectedClient || protocolsLoading}
              emptyMessage="No finalized protocols for this client"
            />
            {/* Contextual hints */}
            {selectedClient && !protocolsLoading && protocols.length === 0 && !protocolsError && (
              <p className="mt-1.5 text-xs text-text-ter">
                This client has no finalized protocols yet.
              </p>
            )}
            {selectedClient && !protocolsLoading && protocols.length === 1 && (
              <p className="mt-1.5 text-xs text-lsg-blue-dark font-medium">
                Only one protocol available — auto-selected.
              </p>
            )}
            {protocolsError && (
              <p className="mt-1.5 text-xs text-error-on">{protocolsError}</p>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 pt-1">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-text-sec bg-bg-secondary hover:bg-bg-tertiary border border-border-sec rounded-md transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={!canStart}
            className="px-5 py-2 text-sm font-bold text-white bg-lsg-blue hover:bg-lsg-blue-dark rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            Start Session →
          </button>
        </div>
      </div>
    </div>
  );
}