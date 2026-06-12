"use client";

import { usePathname, useRouter } from "next/navigation";
import { ReactNode, useEffect } from "react";

import { useAuth } from "@/components/auth-provider";

export function ProtectedPage({ children }: { children: ReactNode }) {
  const { authenticated, loading } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !authenticated) {
      router.replace(`/login?returnTo=${encodeURIComponent(pathname)}`);
    }
  }, [authenticated, loading, pathname, router]);

  if (loading || !authenticated) {
    return <div className="status-card">Checking your session...</div>;
  }

  return <>{children}</>;
}
