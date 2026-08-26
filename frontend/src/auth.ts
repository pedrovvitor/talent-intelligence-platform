import Keycloak from "keycloak-js";

export type AuthSession = {
  displayName: string;
  roles: readonly PlatformRole[];
  getAccessToken: () => Promise<string>;
  logout: () => Promise<void>;
};

export type PlatformRole = "candidate" | "recruiter" | "admin";

const supportedRoles: readonly PlatformRole[] = ["candidate", "recruiter", "admin"];

const keycloak = new Keycloak({
  url: "http://localhost:8180",
  realm: "talent-intelligence",
  clientId: "talent-intelligence-web"
});
let initialization: Promise<boolean> | null = null;

export async function initializeAuthentication(): Promise<AuthSession | null> {
  const authenticated = await initializeKeycloak();
  if (!authenticated) {
    return null;
  }
  const parsedToken: unknown = keycloak.tokenParsed;

  return {
    displayName: readPreferredUsername(parsedToken) ?? keycloak.subject ?? "Authenticated user",
    roles: supportedRoles.filter((role) => keycloak.hasRealmRole(role)),
    getAccessToken: async () => {
      await keycloak.updateToken(30);
      if (keycloak.token === undefined) {
        throw new Error("OIDC access token is unavailable");
      }
      return keycloak.token;
    },
    logout: async () => {
      await keycloak.logout({ redirectUri: window.location.origin });
    }
  };
}

export async function login(): Promise<void> {
  await initializeKeycloak();
  await keycloak.login({ redirectUri: window.location.origin });
}

export async function registerCandidate(): Promise<void> {
  await initializeKeycloak();
  await keycloak.register({ redirectUri: window.location.origin });
}

export function hasAuthenticationCallback(location: Pick<Location, "hash" | "search">): boolean {
  const parameters = new URLSearchParams(location.hash.startsWith("#") ? location.hash.slice(1) : location.search);
  return parameters.has("code") || parameters.has("error");
}

function initializeKeycloak(): Promise<boolean> {
  initialization ??= keycloak.init({
    pkceMethod: "S256",
    checkLoginIframe: false
  }).catch((error: unknown) => {
    initialization = null;
    throw error;
  });
  return initialization;
}

function readPreferredUsername(token: unknown): string | undefined {
  if (typeof token !== "object" || token === null || !("preferred_username" in token)) {
    return undefined;
  }
  const value: unknown = token.preferred_username;
  return typeof value === "string" ? value : undefined;
}
