"use client";

import Link from "next/link";
import { ReactNode } from "react";

import { useAuth } from "@/components/auth-provider";
import { ProtectedPage } from "@/components/protected-page";

export function AppShell({ children }: { children: ReactNode }) {
  const { logout, user } = useAuth();
  const canViewAudit = user?.roles.some((role) =>
    ["ADMIN", "AUDITOR"].includes(role),
  );

  return (
    <ProtectedPage>
      <header className="site-header">
        <Link className="brand" href="/dashboard">
          SecureTask
        </Link>
        <nav aria-label="Main navigation">
          <Link href="/dashboard">Dashboard</Link>
          <Link href="/projects">Projects</Link>
          {canViewAudit && <Link href="/audit-logs">Audit logs</Link>}
        </nav>
        <div className="account">
          <span>{user?.email ?? user?.username}</span>
          <button className="button secondary small" onClick={() => logout()}>
            Sign out
          </button>
        </div>
      </header>
      <main className="container">{children}</main>
    </ProtectedPage>
  );
}
