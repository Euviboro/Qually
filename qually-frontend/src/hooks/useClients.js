import { useAsync } from "./useAsync";
import { getClients } from "../api/clients";

export function useClients() {
  const { data, loading, error, refetch } = useAsync(getClients, []);

  return {
    clients: data ?? [],       // never null — components can map directly
    loading,
    error,
    refresh: () => refetch(true), // silent: no spinner flash on re-fetch
  };
}