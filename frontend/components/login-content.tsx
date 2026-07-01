"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect } from "react";

import { useAuth } from "@/components/auth-provider";

export function LoginContent() {
  const { authenticated, loading, login } = useAuth();
  const searchParams = useSearchParams();
  const returnTo = searchParams.get("returnTo") ?? "/dashboard";
  const expired = searchParams.get("expired") === "1";

  useEffect(() => {
    if (!loading && authenticated) {
      window.location.replace(returnTo.startsWith("/") ? returnTo : "/dashboard");
    }
  }, [authenticated, loading, returnTo]);

  return (
    <main className="auth-layout">
      <section className="auth-card">
        <h1>distributed-auth-platform</h1>
        <p className="muted">
          Sign in through Keycloak to manage projects and documents.
        </p>
        {expired && (
          <p className="error">Your session expired. Please sign in again.</p>
        )}
        {loading ? (
          <p>Checking your session...</p>
        ) : authenticated ? (
          <p>
            Signed in. <Link href={returnTo}>Continue</Link>
          </p>
        ) : (
          <button className="button" onClick={() => login(returnTo)}>
            Sign in with Keycloak
          </button>
        )}
      </section>
    </main>
  );
}
