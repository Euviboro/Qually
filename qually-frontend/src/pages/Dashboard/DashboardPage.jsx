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
            <h1 className="text-3xl font-bold text-gray-900 tracking-tight leading-none">
              Clients
            </h1>
            <button
              onClick={() => setIsAddModalOpen(true)}
              className="px-3 py-1 text-sm font-semibold bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors shadow-sm"
            >
              + Add
            </button>
          </div>
          <p className="text-gray-500 text-base">
            {loading
              ? "Loading data..."
              : `${clients.length} client${clients.length !== 1 ? "s" : ""} · click a card to view protocols`}
          </p>
        </div>

        {/* Search */}
        <div className="relative">
          <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
            <svg className="w-3.5 h-3.5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <input
            type="text"
            placeholder="Search clients..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-60 pl-9 pr-4 py-2 text-sm bg-white border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all shadow-sm"
          />
        </div>
      </header>

      {/* Loading */}
      {loading && (
        <div className="text-center py-20">
          <div className="inline-flex items-center gap-3 text-gray-400">
            <div className="w-5 h-5 border-2 border-gray-200 border-t-blue-600 rounded-full animate-spin" />
            <span>Loading clients...</span>
          </div>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="text-center py-16 text-red-500 bg-red-50 rounded-xl border border-red-100">
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

      {/* Empty state — only after loading, no error, truly nothing */}
      {!loading && !error && filtered.length === 0 && (
        <div className="text-center py-20 text-gray-400">
          {search ? "No clients match your search." : "No clients yet. Add one to get started."}
        </div>
      )}

      {/* Modal */}
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