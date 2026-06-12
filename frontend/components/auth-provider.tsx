"use client";

import Keycloak from "keycloak-js";
import { useRouter } from "next/navigation";
import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import { apiRequest } from "@/lib/api";
import type { CurrentUser } from "@/types";

type AuthContextValue = {
  authenticated: boolean;
  loading: boolean;
  user: CurrentUser | null;
  getToken: () => Promise<string>;
  invalidateSession: () => void;
  login: (returnTo?: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

let keycloak: Keycloak | null = null;
let initialization: Promise<boolean> | null = null;

function getKeycloak(): Keycloak {
  if (!keycloak) {
    keycloak = new Keycloak({
      url: process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? "http://localhost:8081",
      realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? "securetask",
      clientId:
        process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? "securetask-frontend",
    });
  }
  return keycloak;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState<CurrentUser | null>(null);

  const getToken = useCallback(async () => {
    const client = getKeycloak();
    if (!client.authenticated) throw new Error("Authentication required");

    try {
      await client.updateToken(30);
    } catch {
      client.clearToken();
      setAuthenticated(false);
      setUser(null);
      throw new Error("Your session expired. Please sign in again.");
    }

    if (!client.token) throw new Error("Authentication required");
    return client.token;
  }, []);

  const invalidateSession = useCallback(() => {
    getKeycloak().clearToken();
    setAuthenticated(false);
    setUser(null);
  }, []);

  const login = useCallback(async (returnTo = "/dashboard") => {
    await getKeycloak().login({
      redirectUri: `${window.location.origin}${returnTo}`,
    });
  }, []);

  const logout = useCallback(async () => {
    setAuthenticated(false);
    setUser(null);
    await getKeycloak().logout({
      redirectUri: `${window.location.origin}/login`,
    });
  }, []);

  useEffect(() => {
    let active = true;
    const client = getKeycloak();
    initialization ??= client.init({
      onLoad: "check-sso",
      pkceMethod: "S256",
      checkLoginIframe: false,
    });

    initialization
      .then(async (isAuthenticated) => {
        if (!active) return;
        setAuthenticated(isAuthenticated);

        if (isAuthenticated && client.token) {
          try {
            const response = await apiRequest("me", client.token);
            if (active) setUser((await response.json()) as CurrentUser);
          } catch {
            if (active) {
              setAuthenticated(false);
              setUser(null);
            }
          }
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    client.onTokenExpired = () => {
      client.updateToken(30).catch(() => {
        invalidateSession();
        router.push("/login?expired=1");
      });
    };

    return () => {
      active = false;
    };
  }, [invalidateSession, router]);

  const value = useMemo(
    () => ({
      authenticated,
      loading,
      user,
      getToken,
      invalidateSession,
      login,
      logout,
    }),
    [
      authenticated,
      getToken,
      invalidateSession,
      loading,
      login,
      logout,
      user,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
