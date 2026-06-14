"use client";

import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import { usePathname } from "next/navigation";

import { useAuth } from "@/components/auth-provider";
import { useApi } from "@/hooks/use-api";
import type { Project } from "@/types";

type ProjectDataContextValue = {
  projects: Project[];
  loading: boolean;
  error: string | null;
  invalidateProjects: () => void;
};

const ProjectDataContext = createContext<ProjectDataContextValue | null>(null);

export function ProjectDataProvider({ children }: { children: ReactNode }) {
  const api = useApi();
  const pathname = usePathname();
  const { ready, sessionVersion } = useAuth();
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<{
    projects: Project[];
    requestKey: string | null;
    error: string | null;
  }>({
    projects: [],
    requestKey: null,
    error: null,
  });

  const invalidateProjects = useCallback(() => {
    setRevision((current) => current + 1);
  }, []);

  const projectPage =
    pathname === "/dashboard" || pathname === "/projects" ? pathname : null;
  const requestKey = projectPage
    ? `${sessionVersion}:${revision}:${projectPage}`
    : null;

  useEffect(() => {
    if (!ready || !requestKey) return;

    let active = true;

    // Dashboard and Projects consume this one request/result owned by the provider.
    api("projects")
      .then((response) => response.json())
      .then((projects: Project[]) => {
        if (active) {
          setResult({ projects, requestKey, error: null });
        }
      })
      .catch((caught: unknown) => {
        if (active) {
          setResult((current) => ({
            projects: current.projects,
            requestKey,
            error:
              caught instanceof Error
                ? caught.message
                : "Could not load projects",
          }));
        }
      });

    return () => {
      active = false;
    };
  }, [api, ready, requestKey]);

  const loading = !ready || (requestKey !== null && result.requestKey !== requestKey);
  const value = useMemo(
    () => ({
      projects: result.projects,
      loading,
      error: loading ? null : result.error,
      invalidateProjects,
    }),
    [invalidateProjects, loading, result.error, result.projects],
  );

  return (
    <ProjectDataContext.Provider value={value}>
      {children}
    </ProjectDataContext.Provider>
  );
}

export function useProjectData(): ProjectDataContextValue {
  const context = useContext(ProjectDataContext);
  if (!context) {
    throw new Error("useProjectData must be used inside ProjectDataProvider");
  }
  return context;
}
