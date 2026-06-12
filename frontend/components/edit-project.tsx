"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { Message } from "@/components/message";
import { ProjectForm } from "@/components/project-form";
import { useApi } from "@/hooks/use-api";
import type { Project } from "@/types";

export function EditProject({ projectId }: { projectId: string }) {
  const api = useApi();
  const router = useRouter();
  const [project, setProject] = useState<Project | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api(`projects/${projectId}`)
      .then((response) => response.json())
      .then((data: Project) => setProject(data))
      .catch((caught: unknown) =>
        setError(caught instanceof Error ? caught.message : "Could not load project"),
      );
  }, [api, projectId]);

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h1>Edit project</h1>
          <p className="muted">Update the project name or description.</p>
        </div>
      </div>
      <Message>{error}</Message>
      {!project && !error && <div className="status-card">Loading project...</div>}
      {project && (
        <ProjectForm
          initialDescription={project.description ?? ""}
          initialName={project.name}
          submitLabel="Save changes"
          onSubmit={async (value) => {
            await api(`projects/${projectId}`, {
              method: "PUT",
              body: JSON.stringify(value),
            });
            router.push(`/projects/${projectId}`);
          }}
        />
      )}
    </AppShell>
  );
}
