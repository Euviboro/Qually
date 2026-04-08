export default function SessionResultsPage() {
  return (
    <div style={{ padding: "2.5rem 2rem", maxWidth: "960px" }}>
      <p style={{ fontSize: "24px", fontWeight: "700", color: "var(--color-text-primary)", marginBottom: "4px" }}>
        Session Results
      </p>
      <p style={{ color: "var(--color-text-secondary)", marginBottom: "2rem" }}>
        COPC category scores, individual responses, and session summary.
      </p>
      {/* TODO: Score cards + response table */}
    </div>
  );
}