import { describe, expect, it } from "vitest";
import { hasAuthenticationCallback } from "./auth";

describe("hasAuthenticationCallback", () => {
  it("detects an authorization code in the URL fragment", () => {
    expect(hasAuthenticationCallback({ hash: "#state=synthetic&code=synthetic-code", search: "" })).toBe(true);
  });

  it("detects an OIDC error callback", () => {
    expect(hasAuthenticationCallback({ hash: "", search: "?error=access_denied" })).toBe(true);
  });

  it("keeps ordinary public URLs anonymous", () => {
    expect(hasAuthenticationCallback({ hash: "#pricing", search: "?campaign=portfolio" })).toBe(false);
  });
});
