import { type SubmitEvent, useState } from "react";
import { findMatches, TalentApiError } from "./api";
import type { JobMatch, MatchRequest, Seniority, WorkMode } from "./types";

const initialRequest: MatchRequest = {
  headline: "Senior JVM Engineer",
  summary: "Backend engineer building reliable Kotlin and Java services, distributed systems, semantic retrieval, and Kubernetes workloads.",
  skills: ["Kotlin", "Java", "Spring Boot", "PostgreSQL", "Kubernetes", "RAG"],
  seniority: "SENIOR",
  preferredWorkModes: ["REMOTE", "HYBRID"],
  preferredLocations: ["Fortaleza"],
  minimumSalary: 110000,
  limit: 5
};

export default function App() {
  const [request, setRequest] = useState<MatchRequest>(initialRequest);
  const [matches, setMatches] = useState<JobMatch[]>([]);
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus("loading");
    setError(null);
    try {
      const response = await findMatches(request);
      setMatches(response.matches);
      setStatus("success");
    } catch (caughtError: unknown) {
      const message = caughtError instanceof TalentApiError ? caughtError.message : "Matching failed. Try again.";
      setError(message);
      setStatus("error");
    }
  }

  return (
    <main className="min-h-screen bg-[#f4f1ea] text-[#17201f]">
      <header className="border-b border-[#17201f]/15 bg-[#132d2a] text-white">
        <div className="mx-auto flex max-w-[1440px] items-center justify-between px-6 py-5 lg:px-10">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[#9fc5ad]">Talent Intelligence</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Evidence-based matching workspace</h1>
          </div>
          <div className="hidden items-center gap-3 text-sm text-[#d7e6dd] md:flex">
            <span className="h-2 w-2 rounded-full bg-[#70d89d]" />
            Local semantic model · PGVector
          </div>
        </div>
      </header>

      <div className="mx-auto grid max-w-[1440px] gap-8 px-6 py-8 lg:grid-cols-[390px_1fr] lg:px-10">
        <section aria-labelledby="candidate-profile-heading" className="self-start border border-[#17201f]/15 bg-white p-6 shadow-[4px_4px_0_#17201f]">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[#66716f]">Candidate input</p>
          <h2 id="candidate-profile-heading" className="mt-2 text-xl font-semibold">Build the retrieval query</h2>
          <form className="mt-6 space-y-5" onSubmit={(event) => void submit(event)}>
            <TextField label="Headline" value={request.headline} onChange={(headline) => { setRequest({ ...request, headline }); }} />
            <TextArea label="Professional summary" value={request.summary} onChange={(summary) => { setRequest({ ...request, summary }); }} />
            <TextField
              label="Skills"
              hint="Comma-separated"
              value={request.skills.join(", ")}
              onChange={(skills) => { setRequest({ ...request, skills: parseList(skills) }); }}
            />
            <div className="grid grid-cols-2 gap-4">
              <SelectField
                label="Seniority"
                value={request.seniority}
                options={["JUNIOR", "MID", "SENIOR", "STAFF", "PRINCIPAL"]}
                onChange={(seniority) => { setRequest({ ...request, seniority }); }}
              />
              <TextField
                label="Minimum salary"
                inputMode="numeric"
                required={false}
                value={request.minimumSalary?.toString() ?? ""}
                onChange={(value) => { setRequest({ ...request, minimumSalary: value === "" ? null : Number(value) }); }}
              />
            </div>
            <fieldset>
              <legend className="text-sm font-medium">Preferred work modes</legend>
              <div className="mt-2 flex flex-wrap gap-2">
                {(["REMOTE", "HYBRID", "ONSITE"] satisfies WorkMode[]).map((mode) => (
                  <button
                    key={mode}
                    className={`border px-3 py-2 text-xs font-semibold ${request.preferredWorkModes.includes(mode) ? "border-[#174a42] bg-[#d9eee3] text-[#174a42]" : "border-[#17201f]/20 bg-white text-[#66716f]"}`}
                    onClick={() => { setRequest({ ...request, preferredWorkModes: toggle(request.preferredWorkModes, mode) }); }}
                    type="button"
                  >
                    {mode}
                  </button>
                ))}
              </div>
            </fieldset>
            <TextField
              label="Preferred locations"
              hint="Comma-separated"
              value={request.preferredLocations.join(", ")}
              onChange={(preferredLocations) => { setRequest({ ...request, preferredLocations: parseList(preferredLocations) }); }}
            />
            <button
              className="w-full bg-[#d75d3b] px-5 py-3 font-semibold text-white transition hover:bg-[#b94327] disabled:cursor-wait disabled:bg-[#9b8d87]"
              disabled={status === "loading"}
              type="submit"
            >
              {status === "loading" ? "Evaluating matches…" : "Find eligible matches"}
            </button>
          </form>
        </section>

        <section aria-live="polite">
          <div className="flex flex-col justify-between gap-4 border-b border-[#17201f]/20 pb-5 md:flex-row md:items-end">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[#66716f]">Decision pipeline</p>
              <h2 className="mt-2 text-3xl font-semibold tracking-tight">Eligible roles, ranked with evidence</h2>
            </div>
            <p className="max-w-md text-sm leading-6 text-[#596360]">Hard constraints are evaluated before ranking. Semantic relevance contributes 70%; explicit skill coverage contributes 30%.</p>
          </div>

          {status === "idle" && <EmptyState title="Ready to evaluate" body="Submit the candidate profile to run semantic retrieval and deterministic eligibility checks." />}
          {status === "loading" && <EmptyState title="Evaluating the catalog" body="The local embedding model is creating the query vector and PGVector is ranking eligible jobs." />}
          {status === "error" && <EmptyState title="Matching could not complete" body={error ?? "Unexpected error"} tone="error" />}
          {status === "success" && matches.length === 0 && <EmptyState title="No eligible jobs" body="Semantic candidates were found, but none passed the selected hard constraints." />}

          {matches.length > 0 && (
            <ol className="mt-6 space-y-4">
              {matches.map((match, index) => <MatchCard key={match.jobId} match={match} rank={index + 1} />)}
            </ol>
          )}
        </section>
      </div>
    </main>
  );
}

type TextFieldProps = {
  label: string;
  value: string;
  hint?: string;
  inputMode?: "text" | "numeric";
  required?: boolean;
  onChange: (value: string) => void;
};

function TextField({ label, value, hint, inputMode = "text", required = true, onChange }: TextFieldProps) {
  return (
    <label className="block text-sm font-medium">
      <span className="flex justify-between gap-3"><span>{label}</span>{hint && <span className="font-normal text-[#7b8582]">{hint}</span>}</span>
      <input
        className="mt-2 w-full border border-[#17201f]/25 bg-[#fbfaf6] px-3 py-2.5 outline-none transition focus:border-[#174a42] focus:ring-2 focus:ring-[#174a42]/15"
        inputMode={inputMode}
        required={required}
        value={value}
        onChange={(event) => { onChange(event.target.value); }}
      />
    </label>
  );
}

function TextArea({ label, value, onChange }: Omit<TextFieldProps, "hint" | "inputMode">) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <textarea
        className="mt-2 min-h-28 w-full resize-y border border-[#17201f]/25 bg-[#fbfaf6] px-3 py-2.5 outline-none transition focus:border-[#174a42] focus:ring-2 focus:ring-[#174a42]/15"
        required
        value={value}
        onChange={(event) => { onChange(event.target.value); }}
      />
    </label>
  );
}

type SelectFieldProps = {
  label: string;
  value: Seniority;
  options: Seniority[];
  onChange: (value: Seniority) => void;
};

function SelectField({ label, value, options, onChange }: SelectFieldProps) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <select
        className="mt-2 w-full border border-[#17201f]/25 bg-[#fbfaf6] px-3 py-2.5"
        value={value}
        onChange={(event) => { onChange(event.target.value as Seniority); }}
      >
        {options.map((option) => <option key={option}>{option}</option>)}
      </select>
    </label>
  );
}

function MatchCard({ match, rank }: { match: JobMatch; rank: number }) {
  return (
    <li className="grid gap-5 border border-[#17201f]/15 bg-white p-5 shadow-[3px_3px_0_#c8c3b8] md:grid-cols-[64px_1fr_auto] md:items-start">
      <div className="flex h-12 w-12 items-center justify-center bg-[#132d2a] text-lg font-semibold text-white">{rank.toString().padStart(2, "0")}</div>
      <div>
        <h3 className="text-xl font-semibold">{match.title}</h3>
        <p className="mt-1 text-sm text-[#66716f]">{match.company}</p>
        <dl className="mt-4 grid gap-2 sm:grid-cols-2">
          {match.evidence.map((evidence) => (
            <div key={`${match.jobId}-${evidence.type}`} className="border-l-2 border-[#9fc5ad] pl-3">
              <dt className="text-xs uppercase tracking-wide text-[#727c79]">{evidence.label}</dt>
              <dd className="mt-1 text-sm font-medium">{evidence.value}</dd>
            </div>
          ))}
        </dl>
      </div>
      <div className="min-w-28 bg-[#edf4ef] px-4 py-3 text-right">
        <p className="text-xs font-semibold uppercase tracking-wide text-[#557067]">Final score</p>
        <p className="mt-1 text-3xl font-semibold text-[#174a42]">{Math.round(match.finalScore * 100)}</p>
      </div>
    </li>
  );
}

function EmptyState({ title, body, tone = "default" }: { title: string; body: string; tone?: "default" | "error" }) {
  return (
    <div className={`mt-6 border border-dashed p-10 text-center ${tone === "error" ? "border-[#b94327] bg-[#fff0eb]" : "border-[#17201f]/25 bg-white/50"}`}>
      <h3 className="text-lg font-semibold">{title}</h3>
      <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-[#66716f]">{body}</p>
    </div>
  );
}

function parseList(value: string): string[] {
  return value.split(",").map((item) => item.trim()).filter((item) => item.length > 0);
}

function toggle<T>(values: T[], value: T): T[] {
  return values.includes(value) ? values.filter((item) => item !== value) : [...values, value];
}
