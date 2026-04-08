import { useAsync } from "./useAsync";
import { getProtocolById } from "../api/protocols";

export function useProtocol(id) {
  // We "wrap" the generic useAsync with specific protocol logic
  const { data, loading, error, refetch } = useAsync(
    () => getProtocolById(id),
    [id]
  );

  return { protocol: data, loading, error, refetch };
}