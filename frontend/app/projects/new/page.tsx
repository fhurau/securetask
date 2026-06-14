"use client";

import { useRouter } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { ProjectForm } from "@/components/project-form";
import { useProjectData } from "@/components/project-data-provider";
import { useApi } from "@/hooks/use-api";
import type { Project } from "@/types";

export default function CreateProjectPage() {
  const api = useApi();
  const router = useRouter();
  const { invalidateProjects } = useProjectData();

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h1>Create project</h1>
          <p className="muted">Add a project owned by your signed-in account.</p>
        </div>
      </div>
      <ProjectForm
        submitLabel="Create project"
        onSubmit={async (value) => {
          const response = await api("projects", {
            method: "POST",
            body: JSON.stringify(value),
          });
          const project = (await response.json()) as Project;
          invalidateProjects();
          router.push(`/projects/${project.id}`);
        }}
      />
    </AppShell>
  );
}
