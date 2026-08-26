import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import type { AuthSession } from "./auth";

const auth: AuthSession = {
  displayName: "Synthetic Recruiter",
  roles: ["recruiter"],
  getAccessToken: () => Promise.resolve("synthetic-token"),
  logout: () => Promise.resolve()
};

const appActions = {
  onLogin: () => Promise.resolve(),
  onRegisterCandidate: () => Promise.resolve()
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("App", () => {
  it("renders the matching workspace", () => {
    render(<App auth={auth} {...appActions} />);

    expect(screen.getByRole("heading", { name: "Evidence-based matching workspace" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Find eligible matches" })).toBeInTheDocument();
    expect(screen.getByText("Synthetic Recruiter")).toBeInTheDocument();
  });

  it("sends a refreshed bearer token with the match request", async () => {
    const getAccessToken = vi.fn(() => Promise.resolve("synthetic-token"));
    const fetchMock = vi.fn(() => Promise.resolve(new Response(JSON.stringify({
      decisionId: "00000000-0000-0000-0000-000000000100",
      decidedAt: "2026-08-25T12:00:00Z",
      policyVersion: "eligibility-policy-v1",
      embeddingModel: "bge-small-en-v1.5-q",
      generativeModel: null,
      promptVersion: null,
      matches: []
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    })));
    vi.stubGlobal("fetch", fetchMock);
    render(<App auth={{ ...auth, getAccessToken }} {...appActions} />);

    fireEvent.click(screen.getByRole("button", { name: "Find eligible matches" }));

    await waitFor(() => { expect(getAccessToken).toHaveBeenCalledOnce(); });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/matches",
      expect.objectContaining({
        headers: {
          Authorization: "Bearer synthetic-token",
          "Content-Type": "application/json"
        }
      })
    );
  });

  it("renders the public product and pricing without forcing authentication", () => {
    render(<App auth={null} {...appActions} />);

    expect(screen.getByRole("heading", { name: /Talent signals/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Recruiter Team" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create candidate account" })).toBeInTheDocument();
  });

  it("starts candidate self-registration from the public site", () => {
    const onRegisterCandidate = vi.fn(() => Promise.resolve());
    render(<App auth={null} onLogin={appActions.onLogin} onRegisterCandidate={onRegisterCandidate} />);

    fireEvent.click(screen.getByRole("button", { name: "Create candidate account" }));

    expect(onRegisterCandidate).toHaveBeenCalledOnce();
  });

  it("uses candidate-specific workspace language", () => {
    render(<App auth={{ ...auth, roles: ["candidate"] }} {...appActions} />);

    expect(screen.getByRole("heading", { name: "Your evidence-led match workspace" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Discover my matches" })).toBeInTheDocument();
  });
});
