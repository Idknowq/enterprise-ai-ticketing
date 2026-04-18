"use client";

import "@ant-design/v5-patch-for-react-19";
import { getCurrentUser, login as loginApi } from "@/lib/services/auth";
import {
  clearStoredSession,
  getStoredSession,
  listenAuthExpired,
  setStoredSession,
  type StoredSession,
} from "@/lib/auth-storage";
import type { CurrentUserResponse, LoginRequest } from "@/types/api";
import { App as AntApp, ConfigProvider } from "antd";
import { createContext, startTransition, useContext, useEffect, useState } from "react";

interface AuthContextValue {
  user: CurrentUserResponse | null;
  session: StoredSession | null;
  authLoading: boolean;
  isAuthenticated: boolean;
  login: (payload: LoginRequest) => Promise<CurrentUserResponse>;
  logout: () => void;
  refreshUser: () => Promise<CurrentUserResponse | null>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [session, setSession] = useState<StoredSession | null>(null);
  const [authLoading, setAuthLoading] = useState(true);

  function logout() {
    clearStoredSession();
    startTransition(() => {
      setUser(null);
      setSession(null);
      setAuthLoading(false);
    });
  }

  async function refreshUser() {
    const currentSession = getStoredSession();
    if (!currentSession) {
      logout();
      return null;
    }

    const currentUser = await getCurrentUser(currentSession.accessToken);
    startTransition(() => {
      setSession(currentSession);
      setUser(currentUser);
    });
    return currentUser;
  }

  async function login(payload: LoginRequest) {
    const response = await loginApi(payload);
    const nextSession = {
      accessToken: response.accessToken,
      expiresAt: response.expiresAt,
    };

    setStoredSession(nextSession);
    startTransition(() => {
      setSession(nextSession);
      setUser(response.user);
      setAuthLoading(false);
    });
    return response.user;
  }

  useEffect(() => {
    const stopListening = listenAuthExpired(() => logout());
    const storedSession = getStoredSession();

    if (!storedSession) {
      setAuthLoading(false);
      return stopListening;
    }

    setSession(storedSession);
    getCurrentUser(storedSession.accessToken)
      .then((currentUser) => {
        startTransition(() => {
          setUser(currentUser);
        });
      })
      .catch(() => logout())
      .finally(() => setAuthLoading(false));

    return stopListening;
  }, []);

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: "#1d4ed8",
          colorSuccess: "#15803d",
          colorWarning: "#b45309",
          colorError: "#b91c1c",
          colorInfo: "#2563eb",
          colorBgLayout: "#f3f6fb",
          colorBgContainer: "#ffffff",
          colorBorderSecondary: "#dbe4f0",
          borderRadius: 12,
          fontFamily: '"Avenir Next", "PingFang SC", "Microsoft YaHei", sans-serif',
        },
      }}
    >
      <AntApp>
        <AuthContext.Provider
          value={{
            user,
            session,
            authLoading,
            isAuthenticated: Boolean(user && session),
            login,
            logout,
            refreshUser,
          }}
        >
          {children}
        </AuthContext.Provider>
      </AntApp>
    </ConfigProvider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AppProvider");
  }
  return context;
}
