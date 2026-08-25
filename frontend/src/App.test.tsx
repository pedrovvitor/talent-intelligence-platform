import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "./App";

describe("App", () => {
  it("renders the matching workspace", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "Evidence-based matching workspace" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Find eligible matches" })).toBeInTheDocument();
  });
});
