export const API_BASE = import.meta.env.VITE_API_BASE;
 
export const COPC_CATEGORY_META = {
  CUSTOMER:   { label: "Customer",   bgVar: "--color-copc-customer-bg",   textVar: "--color-copc-customer-text",   dotVar: "--color-copc-customer-dot"   },
  BUSINESS:   { label: "Business",   bgVar: "--color-copc-business-bg",   textVar: "--color-copc-business-text",   dotVar: "--color-copc-business-dot"   },
  COMPLIANCE: { label: "Compliance", bgVar: "--color-copc-compliance-bg", textVar: "--color-copc-compliance-text", dotVar: "--color-copc-compliance-dot" },
};
 
export const AUDIT_STATUS_META = {
  IN_PROGRESS: { label: "In Progress", bg: "#E6F4FF", text: "#003D84" },
  SUBMITTED:   { label: "Submitted",   bg: "#FFF0E6", text: "#7A3200" },
  COMPLETED:   { label: "Completed",   bg: "#E1F5EE", text: "#085041" },
  REVIEWED:    { label: "Reviewed",    bg: "#EDF2FA", text: "#001E4B" },
  DISPUTED:    { label: "Disputed",    bg: "#FDECEA", text: "#7A1010" },
  RESOLVED:    { label: "Resolved",    bg: "#E1F5EE", text: "#085041" },
};
 
export const AUDIT_LOGIC_TYPE_META = {
  STANDARD:       { label: "Standard",       description: "Any NO marks the category as 0" },
  ACCOUNTABILITY: { label: "Accountability", description: "Only company-accountable NOs affect the score" },
};
 
export const CLIENT_ACCENT_COLORS = [
  { bg: "#E6F4FF", text: "#002F65", dot: "#0096FF" },
  { bg: "#EBF4FF", text: "#003D84", dot: "#006EF4" },
  { bg: "#FFF0E6", text: "#7A3200", dot: "#FF8021" },
  { bg: "#E1F5EE", text: "#085041", dot: "#1D9E75" },
  { bg: "#EDF2FA", text: "#001E4B", dot: "#3A5272" },
  { bg: "#E6EEFF", text: "#003D84", dot: "#0065B7" },
];