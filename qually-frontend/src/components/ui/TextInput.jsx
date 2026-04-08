export function TextInput({ value, onChange, placeholder, disabled, autoFocus, className = "" }) {
  return (
    <input
      type="text"
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      disabled={disabled}
      autoFocus={autoFocus}
      className={`w-full px-3 py-2 text-base rounded-md border border-gray-200
      bg-white text-gray-900 font-sans outline-none transition-all
      focus:border-blue-500 focus:ring-3 focus:ring-blue-500/10
      disabled:bg-gray-50 disabled:text-gray-400 ${className}`}
    />
  );
}