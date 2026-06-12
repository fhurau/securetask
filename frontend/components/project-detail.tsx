"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChangeEvent, useCallback, useEffect, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { useAuth } from "@/components/auth-provider";
import { Message } from "@/components/message";
import { useApi } from "@/hooks/use-api";
import { downloadFilename } from "@/lib/api";
import { formatBytes, formatDate } from "@/lib/format";
import type { DocumentMetadata, Project } from "@/types";

export function ProjectDetail({ projectId }: { projectId: string }) {
  const api = useApi();
  const router = useRouter();
  const { user } = useAuth();
  const [project, setProject] = useState<Project | null>(null);
  const [documents, setDocuments] = useState<DocumentMetadata[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canManageContent = user?.roles.some((role) =>
    ["USER", "ADMIN"].includes(role),
  );

  const load = useCallback(async () => {
    setError(null);
    try {
      const [projectResponse, documentResponse] = await Promise.all([
        api(`projects/${projectId}`),
        api(`projects/${projectId}/documents`),
      ]);
      setProject((await projectResponse.json()) as Project);
      setDocuments((await documentResponse.json()) as DocumentMetadata[]);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not load project");
    } finally {
      setLoading(false);
    }
  }, [api, projectId]);

  useEffect(() => {
    let active = true;

    Promise.all([
      api(`projects/${projectId}`),
      api(`projects/${projectId}/documents`),
    ])
      .then(async ([projectResponse, documentResponse]) => {
        const projectData = (await projectResponse.json()) as Project;
        const documentData =
          (await documentResponse.json()) as DocumentMetadata[];
        if (active) {
          setProject(projectData);
          setDocuments(documentData);
        }
      })
      .catch((caught: unknown) => {
        if (active) {
          setError(
            caught instanceof Error ? caught.message : "Could not load project",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [api, projectId]);

  async function upload() {
    if (!selectedFile) return;
    setUploading(true);
    setError(null);
    const body = new FormData();
    body.append("file", selectedFile);
    try {
      await api(`projects/${projectId}/documents`, { method: "POST", body });
      setSelectedFile(null);
      const input = document.getElementById("document-file") as HTMLInputElement;
      if (input) input.value = "";
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not upload document");
    } finally {
      setUploading(false);
    }
  }

  async function download(item: DocumentMetadata) {
    setError(null);
    try {
      const response = await api(`projects/${projectId}/documents/${item.id}`);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = downloadFilename(response, item.originalFilename);
      link.click();
      URL.revokeObjectURL(url);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not download document");
    }
  }

  async function removeProject() {
    if (!window.confirm("Delete this project? This cannot be undone.")) return;
    try {
      await api(`projects/${projectId}`, { method: "DELETE" });
      router.push("/projects");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not delete project");
    }
  }

  if (loading) {
    return (
      <AppShell>
        <div className="status-card">Loading project...</div>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h1>{project?.name ?? "Project"}</h1>
          <p className="muted">{project?.description || "No description"}</p>
        </div>
        {canManageContent && project && (
          <div className="actions">
            <Link className="button secondary" href={`/projects/${projectId}/edit`}>
              Edit
            </Link>
            <button className="button danger" onClick={removeProject}>
              Delete
            </button>
          </div>
        )}
      </div>
      <Message>{error}</Message>
      {project && (
        <section className="card">
          <dl className="details">
            <dt>Owner</dt>
            <dd>{project.ownerEmail}</dd>
            <dt>Created</dt>
            <dd>{formatDate(project.createdAt)}</dd>
            <dt>Updated</dt>
            <dd>{formatDate(project.updatedAt)}</dd>
          </dl>
        </section>
      )}

      <section className="section">
        <div className="page-header">
          <div>
            <h2>Documents</h2>
            <p className="muted">PDF, TXT, PNG, or JPEG files up to 5 MB.</p>
          </div>
        </div>
        {canManageContent && (
          <div className="card form">
            <div className="field">
              <label htmlFor="document-file">Choose document</label>
              <input
                accept=".pdf,.txt,.png,.jpg,.jpeg"
                id="document-file"
                onChange={(event: ChangeEvent<HTMLInputElement>) =>
                  setSelectedFile(event.target.files?.[0] ?? null)
                }
                type="file"
              />
            </div>
            <div>
              <button
                className="button"
                disabled={!selectedFile || uploading}
                onClick={upload}
              >
                {uploading ? "Uploading..." : "Upload"}
              </button>
            </div>
          </div>
        )}

        <div className="table-wrap section">
          <table>
            <thead>
              <tr>
                <th>Filename</th>
                <th>Type</th>
                <th>Size</th>
                <th>Uploaded by</th>
                <th>Uploaded</th>
                {canManageContent && <th>Action</th>}
              </tr>
            </thead>
            <tbody>
              {documents.length === 0 ? (
                <tr>
                  <td colSpan={canManageContent ? 6 : 5}>No documents found.</td>
                </tr>
              ) : (
                documents.map((item) => (
                  <tr key={item.id}>
                    <td>{item.originalFilename}</td>
                    <td>{item.contentType}</td>
                    <td>{formatBytes(item.sizeBytes)}</td>
                    <td>{item.uploadedByEmail}</td>
                    <td>{formatDate(item.createdAt)}</td>
                    {canManageContent && (
                      <td>
                        <button
                          className="button secondary small"
                          onClick={() => download(item)}
                        >
                          Download
                        </button>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </AppShell>
  );
}
