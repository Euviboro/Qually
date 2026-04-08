export default function LogSessionPage() {
  return (
    <div style={{ padding: "2.5rem 2rem", maxWidth: "860px" }}>
      <p style={{ fontSize: "24px", fontWeight: "700", color: "var(--color-text-primary)", marginBottom: "4px" }}>
        Log Audit Session
      </p>
      <p style={{ color: "var(--color-text-secondary)", marginBottom: "2rem" }}>
        Select a client, choose a finalized protocol, set logic type, and answer each question.
      </p>
      {/* TODO: Multi-step session form */}
    </div>
  );
}