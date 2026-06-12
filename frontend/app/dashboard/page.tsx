"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { useAuth } from "@/components/auth-provider";
import { Message } from "@/components/message";
import { useApi } from "@/hooks/use-api";
import type { Project } from "@/types";

export default function DashboardPage() {
  const api = useApi();
  const { user } = useAuth();
  const [projects, setProjects] = useState<Project[]>([]);
  const [error, setError] = useState<string | null>(null);
  const canCreate = user?.roles.some((role) => ["USER", "ADMIN"].includes(role));

  useEffect(() => {
    api("projects")
      .then((response) => response.json())
      .then((data: Project[]) => setProjects(data))
      .catch((caught: unknown) =>
        setError(caught instanceof Error ? caught.message : "Could not load projects"),
      );
  }, [api]);

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
          <p>{projects.length}</p>
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
