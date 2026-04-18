const SESSION_STORAGE_KEY = "enterprise-ai-ticketing.session";
const AUTH_EXPIRED_EVENT = "enterprise-ai-ticketing:auth-expired";

export interface StoredSession {
  accessToken: string;
  expiresAt: string;
}

function canUseStorage() {
  return typeof window !== "undefined";
}

export function getStoredSession(): StoredSession | null {
  if (!canUseStorage()) {
    return null;
  }

  const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const session = JSON.parse(raw) as StoredSession;
    if (!session.accessToken || !session.expiresAt) {
      clearStoredSession();
      return null;
    }

    if (new Date(session.expiresAt).getTime() <= Date.now()) {
      clearStoredSession();
      return null;
    }

    return session;
  } catch {
    clearStoredSession();
    return null;
  }
}

export function setStoredSession(session: StoredSession) {
  if (!canUseStorage()) {
    return;
  }

  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

export function clearStoredSession() {
  if (!canUseStorage()) {
    return;
  }

  window.localStorage.removeItem(SESSION_STORAGE_KEY);
}

export function emitAuthExpired() {
  if (!canUseStorage()) {
    return;
  }

  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
}

export function listenAuthExpired(handler: () => void) {
  if (!canUseStorage()) {
    return () => undefined;
  }

  window.addEventListener(AUTH_EXPIRED_EVENT, handler);
  return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handler);
}

