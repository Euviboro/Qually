import { useNavigate } from "react-router-dom";
 
export default function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <div style={{
      display: "flex", flexDirection: "column", alignItems: "center",
      justifyContent: "center", height: "60vh", gap: "12px",
      color: "var(--color-text-tertiary)",
    }}>
      <p style={{ fontSize: "48px", fontWeight: "700", color: "var(--color-border-primary)" }}>404</p>
      <p style={{ fontSize: "15px" }}>Page not found.</p>
      <button
        onClick={() => navigate("/")}
        style={{
          marginTop: "8px", padding: "8px 20px",
          background: "var(--color-action-primary)", color: "white",
          border: "none", borderRadius: "var(--border-radius-md)",
          fontSize: "13px", fontWeight: "500", cursor: "pointer",
        }}
      >
        Back to Dashboard
      </button>
    </div>
  );
}