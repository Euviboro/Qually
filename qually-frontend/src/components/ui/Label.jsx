export function Label({ children, required }) {
  return (
    <p className="mb-1.5 text-sm font-semibold text-gray-600 tracking-wide">
      {children}
      {required && <span className="text-blue-500 ml-1">*</span>}
    </p>
  );
}