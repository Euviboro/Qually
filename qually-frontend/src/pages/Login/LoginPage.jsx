/**
 * @module pages/Login/LoginPage
 *
 * Login page — Microsoft Entra ID edition.
 *
 * The only action is navigating to /oauth2/authorization/azure.
 * Spring Security handles the redirect to Microsoft, the token exchange,
 * the DB lookup, and setting the access_token cookie. When the browser
 * returns to the frontend root, AuthContext re-fetches /api/auth/me.
 *
 * The ?error=true query param is set by Spring when OAuth2 fails
 * (e.g. email not in Qually, account inactive). We surface a message in that case.
 */

import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";

const BACKEND = import.meta.env.VITE_API_BASE.replace("/api", "");

const ERROR_MESSAGES = {
  user_not_registered:
    "Your Microsoft account is not registered in Qually. Contact your QA Director to be added.",
  user_inactive:
    "Your Qually account is inactive. Contact your QA Director.",
  email_not_found:
    "Your Microsoft account did not return an email address. Contact your IT team.",
  true: "Authentication failed. Please try again or contact support.",
};

export default function LoginPage() {
  const [searchParams] = useSearchParams();
  const [error, setError] = useState(null);

  useEffect(() => {
    const errorParam = searchParams.get("error");
    if (errorParam) {
      setError(ERROR_MESSAGES[errorParam] ?? ERROR_MESSAGES.true);
    }
  }, [searchParams]);

  const handleSignIn = () => {
    // Navigate the full browser window — not a fetch call.
    // Spring Security initiates the OAuth2 redirect from here.
    window.location.href = `${BACKEND}/oauth2/authorization/azure`;
  };

  return (
    <div className="min-h-screen bg-bg-secondary flex items-center justify-center p-6">
      <div className="w-full max-w-[420px] bg-bg-primary rounded-xl border border-border-sec shadow-lg p-8">

        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-lsg-blue rounded-xl mb-4">
            <svg width="24" height="24" viewBox="0 0 16 16" fill="none">
              <path d="M3 13L3 6L9 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M7 13L13 10L13 3" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" opacity="0.7" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-text-pri tracking-tight">Qually</h1>
          <p className="text-sm text-text-ter mt-1">by Lean Solutions Group</p>
        </div>

        {error && (
          <div className="mb-5 px-4 py-3 bg-error-surface border border-[rgba(226,75,74,0.2)] text-error-on text-sm rounded-xl">
            {error}
          </div>
        )}

        <button
          onClick={handleSignIn}
          className="w-full flex items-center justify-center gap-3 py-2.5 px-4 text-sm font-bold text-text-pri bg-bg-primary hover:bg-bg-secondary border border-border-sec rounded-md transition-all shadow-sm"
        >
          {/* Microsoft logo */}
          <svg width="20" height="20" viewBox="0 0 21 21" fill="none">
            <rect x="1"  y="1"  width="9" height="9" fill="#F25022"/>
            <rect x="11" y="1"  width="9" height="9" fill="#7FBA00"/>
            <rect x="1"  y="11" width="9" height="9" fill="#00A4EF"/>
            <rect x="11" y="11" width="9" height="9" fill="#FFB900"/>
          </svg>
          Sign in with Microsoft
        </button>

        <p className="text-center text-xs text-text-ter mt-6">
          Use your Lean Solutions Group Microsoft account.
        </p>
      </div>
    </div>
  );
}