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
  ready: boolean;
  sessionVersion: number;
  user: CurrentUser | null;
  getToken: (forceRefresh?: boolean) => Promise<string>;
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

function initializeKeycloak(): Promise<boolean> {
  const client = getKeycloak();
  initialization ??= client.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    checkLoginIframe: false,
  });
  return initialization;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [tokenAvailable, setTokenAvailable] = useState(false);
  const [sessionVersion, setSessionVersion] = useState(0);
  const [user, setUser] = useState<CurrentUser | null>(null);

  const getToken = useCallback(async (forceRefresh = false) => {
    const client = getKeycloak();
    await initializeKeycloak();
    if (!client.authenticated) throw new Error("Authentication required");

    try {
      await client.updateToken(forceRefresh ? -1 : 30);
    } catch {
      client.clearToken();
      setAuthenticated(false);
      setTokenAvailable(false);
      setUser(null);
      throw new Error("Your session expired. Please sign in again.");
    }

    if (!client.token) {
      setTokenAvailable(false);
      throw new Error("Authentication required");
    }
    setTokenAvailable(true);
    return client.token;
  }, []);

  const invalidateSession = useCallback(() => {
    getKeycloak().clearToken();
    setAuthenticated(false);
    setTokenAvailable(false);
    setUser(null);
  }, []);

  const login = useCallback(async (returnTo = "/dashboard") => {
    await getKeycloak().login({
      redirectUri: `${window.location.origin}${returnTo}`,
    });
  }, []);

  const logout = useCallback(async () => {
    setAuthenticated(false);
    setTokenAvailable(false);
    setUser(null);
    await getKeycloak().logout({
      redirectUri: `${window.location.origin}/login`,
    });
  }, []);

  useEffect(() => {
    let active = true;
    const client = getKeycloak();

    initializeKeycloak()
      .then(async (isAuthenticated) => {
        if (!active) return;
        setAuthenticated(isAuthenticated);
        setTokenAvailable(false);

        if (isAuthenticated) {
          try {
            await client.updateToken(30);
            if (!client.token) throw new Error("Authentication required");

            const response = await apiRequest("me", client.token);
            const currentUser = (await response.json()) as CurrentUser;
            if (active) {
              // Ready means identity and a refreshed token are both usable.
              setUser(currentUser);
              setTokenAvailable(true);
              setSessionVersion((current) => current + 1);
            }
          } catch {
            if (active) {
              setAuthenticated(false);
              setTokenAvailable(false);
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
      ready: !loading && authenticated && tokenAvailable && user !== null,
      sessionVersion,
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
      sessionVersion,
      tokenAvailable,
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
