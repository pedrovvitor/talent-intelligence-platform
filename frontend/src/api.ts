import type { ApiError, MatchRequest, MatchResponse } from "./types";

export class TalentApiError extends Error {
  constructor(
    readonly code: string,
    message: string
  ) {
    super(message);
  }
}

export async function findMatches(request: MatchRequest, accessToken: string): Promise<MatchResponse> {
  const response = await fetch("/api/matches", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request),
    signal: AbortSignal.timeout(10_000)
  });

  if (!response.ok) {
    const error = await readApiError(response);
    throw new TalentApiError(error.code, error.message);
  }

  const payload: unknown = await response.json();
  if (!isMatchResponse(payload)) {
    throw new TalentApiError("UNEXPECTED_RESPONSE", "The matching service returned an invalid response.");
  }
  return payload;
}

async function readApiError(response: Response): Promise<ApiError> {
  try {
    const payload: unknown = await response.json();
    return isApiError(payload)
      ? payload
      : { code: "UNEXPECTED_RESPONSE", message: "The matching service returned an invalid error response." };
  } catch {
    return { code: "UNEXPECTED_RESPONSE", message: "The matching service returned an unexpected response." };
  }
}

function isMatchResponse(value: unknown): value is MatchResponse {
  return isRecord(value) && Array.isArray(value.matches) && value.matches.every(isJobMatch);
}

function isJobMatch(value: unknown): boolean {
  return isRecord(value)
    && typeof value.jobId === "string"
    && typeof value.title === "string"
    && typeof value.company === "string"
    && typeof value.semanticScore === "number"
    && typeof value.skillCoverage === "number"
    && typeof value.finalScore === "number"
    && Array.isArray(value.evidence)
    && value.evidence.every(isEvidence);
}

function isEvidence(value: unknown): boolean {
  return isRecord(value)
    && typeof value.type === "string"
    && typeof value.label === "string"
    && typeof value.value === "string";
}

function isApiError(value: unknown): value is ApiError {
  return isRecord(value) && typeof value.code === "string" && typeof value.message === "string";
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
