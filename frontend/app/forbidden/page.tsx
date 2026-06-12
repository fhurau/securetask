"use client";

import Link from "next/link";

import { AppShell } from "@/components/app-shell";

export default function ForbiddenPage() {
  return (
    <AppShell>
      <section className="status-card">
        <h1>Forbidden</h1>
        <p>You do not have permission to perform that action.</p>
        <Link className="button" href="/dashboard">
          Return to dashboard
        </Link>
      </section>
    </AppShell>
  );
}
