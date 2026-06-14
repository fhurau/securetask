"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { useAuth } from "@/components/auth-provider";
import { Message } from "@/components/message";
import { useApi } from "@/hooks/use-api";
import { formatDate } from "@/lib/format";
import type { AuditLog } from "@/types";

export default function AuditLogsPage() {
  const api = useApi();
  const router = useRouter();
  const { ready, user } = useAuth();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!ready || !user) return;

    if (user && !user.roles.some((role) => ["ADMIN", "AUDITOR"].includes(role))) {
      router.replace("/forbidden");
      return;
    }

    let active = true;
    api("audit-logs")
      .then((response) => response.json())
      .then((data: AuditLog[]) => {
        if (active) setLogs(data);
      })
      .catch((caught: unknown) => {
        if (active) {
          setError(
            caught instanceof Error ? caught.message : "Could not load audit logs",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [api, ready, router, user]);

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h1>Audit logs</h1>
          <p className="muted">Security-relevant events, newest first.</p>
        </div>
      </div>
      <Message>{error}</Message>
      {loading ? (
        <div className="status-card">Loading audit logs...</div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Actor</th>
                <th>Action</th>
                <th>Resource</th>
                <th>Result</th>
                <th>Correlation ID</th>
              </tr>
            </thead>
            <tbody>
              {logs.length === 0 ? (
                <tr>
                  <td colSpan={6}>No audit events found.</td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDate(log.timestamp)}</td>
                    <td>{log.actorEmail}</td>
                    <td>{log.action}</td>
                    <td>
                      {log.resourceType}
                      {log.resourceId ? `: ${log.resourceId}` : ""}
                    </td>
                    <td>{log.result}</td>
                    <td>{log.correlationId}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </AppShell>
  );
}
