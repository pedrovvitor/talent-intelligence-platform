import Keycloak from "keycloak-js";

export type AuthSession = {
  displayName: string;
  roles: readonly PlatformRole[];
  getAccessToken: () => Promise<string>;
  logout: () => Promise<void>;
};

export type PlatformRole = "recruiter" | "admin";

const supportedRoles: readonly PlatformRole[] = ["recruiter", "admin"];

const keycloak = new Keycloak({
  url: "http://localhost:8180",
  realm: "talent-intelligence",
  clientId: "talent-intelligence-web"
});

export async function initializeAuthentication(): Promise<AuthSession> {
  const authenticated = await keycloak.init({
    onLoad: "login-required",
    pkceMethod: "S256",
    checkLoginIframe: false
  });
  if (!authenticated) {
    throw new Error("OIDC authentication did not establish a session");
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

function readPreferredUsername(token: unknown): string | undefined {
  if (typeof token !== "object" || token === null || !("preferred_username" in token)) {
    return undefined;
  }
  const value: unknown = token.preferred_username;
  return typeof value === "string" ? value : undefined;
}
