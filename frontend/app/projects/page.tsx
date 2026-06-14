"use client";

import Link from "next/link";

import { AppShell } from "@/components/app-shell";
import { useAuth } from "@/components/auth-provider";
import { Message } from "@/components/message";
import { useProjects } from "@/hooks/use-projects";
import { formatDate } from "@/lib/format";

export default function ProjectsPage() {
  const { user } = useAuth();
  const { projects, loading, error } = useProjects();
  const canCreate = user?.roles.some((role) => ["USER", "ADMIN"].includes(role));

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h1>Projects</h1>
          <p className="muted">Projects the backend allows you to access.</p>
        </div>
        {canCreate && (
          <Link className="button" href="/projects/new">
            Create project
          </Link>
        )}
      </div>
      <Message>{error}</Message>
      {loading ? (
        <div className="status-card">Loading projects...</div>
      ) : error ? (
        <div className="status-card">Project data is unavailable.</div>
      ) : projects.length === 0 ? (
        <div className="status-card">No accessible projects found.</div>
      ) : (
        <div className="grid">
          {projects.map((project) => (
            <article className="card" key={project.id}>
              <h2>{project.name}</h2>
              <p>{project.description || "No description"}</p>
              <p className="muted">
                Owner: {project.ownerEmail}
                <br />
                Updated: {formatDate(project.updatedAt)}
              </p>
              <Link href={`/projects/${project.id}`}>Open project</Link>
            </article>
          ))}
        </div>
      )}
    </AppShell>
  );
}
