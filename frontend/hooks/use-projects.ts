"use client";

import { useProjectData } from "@/components/project-data-provider";

export function useProjects() {
  const { projects, loading, error } = useProjectData();
  return { projects, loading, error };
}
