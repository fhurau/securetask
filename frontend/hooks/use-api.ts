"use client";

import { useRouter } from "next/navigation";
import { useCallback } from "react";

import { useAuth } from "@/components/auth-provider";
import { ApiError, apiRequest } from "@/lib/api";

export function useApi() {
  const { getToken, invalidateSession } = useAuth();
  const router = useRouter();

  return useCallback(
    async (path: string, init?: RequestInit) => {
      try {
        return await apiRequest(path, await getToken(), init);
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          invalidateSession();
          router.push("/login?expired=1");
        }
        if (error instanceof ApiError && error.status === 403) {
          router.push("/forbidden");
        }
        throw error;
      }
    },
    [getToken, invalidateSession, router],
  );
}
