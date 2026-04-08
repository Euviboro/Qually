export function SectionCard({ children, className = "" }) {
  return (
    <div className={`bg-white border border-gray-100 rounded-xl p-6 shadow-sm ${className}`}>
      {children}
    </div>
  );
}