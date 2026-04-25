/**
 * @module components/ui/StartSessionModal
 *
 * Two-step modal that collects the client and protocol for a new audit session.
 *
 * Change: protocol version number removed from option labels.
 * The version added noise without useful context in the dropdown since
 * only FINALIZED protocols are shown and there is typically one per client.
 */

import { useState, useEffect } from "react";
import { getClients } from "../../api/clients";
import { getProtocols } from "../../api/protocols";
import { SearchableSelect } from "./SearchableSelect";

/**
 * @param {Object}   props
 * @param {boolean}  props.isOpen
 * @param {() => void} props.onClose
 * @param {(protocol: import('../../api/protocols').AuditProtocolResponseDTO) => void} props.onConfirm
 */
export function StartSessionModal({ isOpen, onClose, onConfirm }) {
  const [clients,        setClients]        = useState([]);
  const [clientsLoading, setClientsLoading] = useState(true);
  const [clientsError,   setClientsError]   = useState(null);
  const [selectedClient, setSelectedClient] = useState(null);

  const [protocols,        setProtocols]        = useState([]);
  const [protocolsLoading, setProtocolsLoading] = useState(false);
  const [protocolsError,   setProtocolsError]   = useState(null);
  const [selectedProtocol, setSelectedProtocol] = useState(null);

  useEffect(() => {
    if (!isOpen) return;
    setClientsLoading(true);
    setClientsError(null);
    getClients()
      .then(setClients)
      .catch((err) => setClientsError(err.message))
      .finally(() => setClientsLoading(false));
  }, [isOpen]);

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
        const finalized = all.filter((p) => p.protocolStatus === "FINALIZED");
        setProtocols(finalized);
        if (finalized.length === 1) setSelectedProtocol(finalized[0].protocolId);
      })
      .catch((err) => setProtocolsError(err.message))
      .finally(() => setProtocolsLoading(false));
  }, [selectedClient]);

  useEffect(() => {
    if (!isOpen) {
      setSelectedClient(null);
      setSelectedProtocol(null);
      setProtocols([]);
      setClientsError(null);
      setProtocolsError(null);
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const handler = (e) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const clientOptions = clients.map((c) => ({
    value: c.clientId,
    label: c.clientName,
  }));

  // Protocol version removed from label — name alone is sufficient
  const protocolOptions = protocols.map((p) => ({
    value: p.protocolId,
    label: p.protocolName,
  }));

  const protocolObject = protocols.find((p) => p.protocolId === selectedProtocol) ?? null;
  const canStart = Boolean(selectedClient && selectedProtocol && protocolObject);

  const handleConfirm = () => {
    if (!canStart) return;
    onConfirm(protocolObject);
  };

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