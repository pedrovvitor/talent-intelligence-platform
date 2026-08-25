export type Seniority = "JUNIOR" | "MID" | "SENIOR" | "STAFF" | "PRINCIPAL";
export type WorkMode = "REMOTE" | "HYBRID" | "ONSITE";

export type MatchRequest = {
  headline: string;
  summary: string;
  skills: string[];
  seniority: Seniority;
  preferredWorkModes: WorkMode[];
  preferredLocations: string[];
  minimumSalary: number | null;
  limit: number;
};

export type MatchEvidence = {
  type: string;
  label: string;
  value: string;
};

export type JobMatch = {
  jobId: string;
  title: string;
  company: string;
  semanticScore: number;
  skillCoverage: number;
  finalScore: number;
  evidence: MatchEvidence[];
};

export type MatchResponse = {
  matches: JobMatch[];
};

export type ApiError = {
  code: string;
  message: string;
};
