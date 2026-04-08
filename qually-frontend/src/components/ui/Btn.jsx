export function Btn({ children, onClick, variant = "secondary", disabled, size = "md", className = "" }) {
  const variants = {
    primary:   "bg-blue-600 text-white hover:bg-blue-700 shadow-sm",
    secondary: "bg-gray-50 text-gray-900 border border-gray-200 hover:bg-gray-100",
    ghost:     "bg-transparent text-gray-500 border border-gray-200 hover:bg-gray-50",
    danger:    "bg-red-50 text-red-600 hover:bg-red-100",
    success:   "bg-green-50 text-green-600 hover:bg-green-100",
  };
  const sizes = {
    sm: "text-xs px-3 py-1.5",
    md: "text-base px-4 py-2",
  };

  return (
    <button
      onClick={disabled ? undefined : onClick}
      className={`inline-flex items-center gap-2 rounded-md font-medium transition-all whitespace-nowrap
      ${variants[variant]} ${sizes[size]} ${disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer"} ${className}`}
    >
      {children}
    </button>
  );
}