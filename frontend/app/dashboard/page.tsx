"use client";

import Link from "next/link";

import { AppShell } from "@/components/app-shell";
import { useAuth } from "@/components/auth-provider";
import { Message } from "@/components/message";
import { useProjects } from "@/hooks/use-projects";

export default function DashboardPage() {
  const { user } = useAuth();
  const { projects, loading, error } = useProjects();
  const canCreate = user?.roles.some((role) => ["USER", "ADMIN"].includes(role));

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">Welcome, {user?.email ?? user?.username}.</p>
        </div>
        {canCreate && (
          <Link className="button" href="/projects/new">
            Create project
          </Link>
        )}
      </div>
      <Message>{error}</Message>
      <div className="grid">
        <section className="card">
          <h2>Accessible projects</h2>
          <p>{loading ? "Loading..." : error ? "Unavailable" : projects.length}</p>
          <Link href="/projects">View projects</Link>
        </section>
        <section className="card">
          <h2>Your roles</h2>
          <p>{user?.roles.join(", ") || "No application roles"}</p>
        </section>
        <section className="card">
          <h2>Security</h2>
          <p className="muted">
            Your access token is kept in memory and sent only with authenticated
            API requests.
          </p>
        </section>
      </div>
    </AppShell>
  );
}
