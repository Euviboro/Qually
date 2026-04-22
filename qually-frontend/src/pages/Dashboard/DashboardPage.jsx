import { useState } from "react";
import { useClients } from "../../hooks/useClients";
import { ClientCard } from "../../components/ui/ClientCard";
import { AddClientModal } from "../../components/ui/AddClientModal";

export default function DashboardPage() {
  const { clients, loading, error, refresh } = useClients();

  const [flippedCards, setFlippedCards] = useState({});
  const [search, setSearch] = useState("");
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  const toggleFlip = (id) => {
    setFlippedCards((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const filtered = clients.filter((c) =>
    c.clientName.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="p-10 max-w-7xl mx-auto">
      {/* Header */}
      <header className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col">
          <div className="flex items-center gap-3 mb-1">
            <h1 className="text-3xl font-bold text-text-pri tracking-tight leading-none">Clients</h1>
            <button
              onClick={() => setIsAddModalOpen(true)}
              className="px-3 py-1 text-sm font-semibold bg-lsg-blue text-white rounded-md hover:bg-lsg-blue-dark transition-colors shadow-sm"
            >
              + Add
            </button>
          </div>
          <p className="text-text-ter text-base">
            {loading
              ? "Loading data..."
              : `${clients.length} client${clients.length !== 1 ? "s" : ""} · click a card to view protocols`}
          </p>
        </div>

        {/* Search */}
        <div className="relative">
          <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
            <svg className="w-3.5 h-3.5 text-text-ter" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <input
            type="text"
            placeholder="Search clients..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-60 pl-9 pr-4 py-2 text-sm bg-bg-primary border border-border-sec rounded-md focus:outline-none focus:ring-2 focus:ring-lsg-blue/20 focus:border-lsg-blue transition-all shadow-sm placeholder:text-text-ter text-text-pri"
          />
        </div>
      </header>

      {/* Loading */}
      {loading && (
        <div className="text-center py-20">
          <div className="inline-flex items-center gap-3 text-text-ter">
            <div className="w-5 h-5 border-2 border-border-sec border-t-lsg-blue rounded-full animate-spin" />
            <span>Loading clients...</span>
          </div>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="text-center py-16 text-error-on bg-error-surface rounded-xl border border-[rgba(226,75,74,0.2)]">
          {error} — Make sure the backend is running on port 8080.
        </div>
      )}

      {/* Grid */}
      {!loading && !error && filtered.length > 0 && (
        <div className="grid grid-cols-[repeat(auto-fill,minmax(270px,1fr))] gap-4">
          {filtered.map((client, i) => (
            <ClientCard
              key={client.clientId}
              client={client}
              colorIdx={i}
              onFlip={toggleFlip}
              flipped={!!flippedCards[client.clientId]}
            />
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && filtered.length === 0 && (
        <div className="text-center py-20 text-text-ter">
          {search ? "No clients match your search." : "No clients yet. Add one to get started."}
        </div>
      )}

      <AddClientModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onClientCreated={() => {
          setIsAddModalOpen(false);
          refresh();
        }}
      />
    </div>
  );
}
