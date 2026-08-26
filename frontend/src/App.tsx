import { type SubmitEvent, useState } from "react";
import { findMatches, TalentApiError } from "./api";
import type { AuthSession } from "./auth";
import type { JobMatch, MatchRequest, MatchResponse, Seniority, WorkMode } from "./types";

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

type AppProps = {
  auth: AuthSession | null;
  authError?: string;
  onLogin: () => Promise<void>;
  onRegisterCandidate: () => Promise<void>;
};

export default function App({ auth, authError, onLogin, onRegisterCandidate }: AppProps) {
  if (auth === null) {
    return <PublicSite authError={authError} onLogin={onLogin} onRegisterCandidate={onRegisterCandidate} />;
  }
  if (auth.roles.length === 0) {
    return <AccessPending auth={auth} />;
  }
  return <MatchingWorkspace auth={auth} />;
}

function MatchingWorkspace({ auth }: { auth: AuthSession }) {
  const [request, setRequest] = useState<MatchRequest>(initialRequest);
  const [matches, setMatches] = useState<JobMatch[]>([]);
  const [decision, setDecision] = useState<MatchResponse | null>(null);
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus("loading");
    setError(null);
    try {
      const accessToken = await auth.getAccessToken();
      const response = await findMatches(request, accessToken);
      setMatches(response.matches);
      setDecision(response);
      setStatus("success");
    } catch (caughtError: unknown) {
      const message = caughtError instanceof TalentApiError ? caughtError.message : "Matching failed. Try again.";
      setError(message);
      setStatus("error");
    }
  }

  const isCandidateOnly = auth.roles.includes("candidate")
    && !auth.roles.includes("recruiter")
    && !auth.roles.includes("admin");

  return (
    <main className="min-h-screen bg-[#f4f1ea] text-[#17201f]">
      <header className="border-b border-[#17201f]/15 bg-[#132d2a] text-white">
        <div className="mx-auto flex max-w-[1440px] items-center justify-between px-6 py-5 lg:px-10">
          <div className="flex items-center gap-3">
            <BrandMark compact />
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[#b8f34a]">Talent Intelligence</p>
              <h1 className="mt-1 text-xl font-semibold tracking-tight md:text-2xl">
                {isCandidateOnly ? "Your evidence-led match workspace" : "Evidence-based matching workspace"}
              </h1>
            </div>
          </div>
          <div className="flex items-center gap-4 text-sm text-[#d7e6dd]">
            <div className="hidden text-right md:block">
              <p className="font-semibold text-white">{auth.displayName}</p>
              <p className="text-xs uppercase tracking-wide">{auth.roles.join(" · ") || "no capability"}</p>
            </div>
            <button
              className="border border-white/30 px-3 py-2 text-xs font-semibold uppercase tracking-wide hover:bg-white/10"
              onClick={() => { void auth.logout(); }}
              type="button"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto grid max-w-[1440px] gap-8 px-6 py-8 lg:grid-cols-[390px_1fr] lg:px-10">
        <section aria-labelledby="candidate-profile-heading" className="self-start border border-[#17201f]/15 bg-white p-6 shadow-[4px_4px_0_#17201f]">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[#66716f]">{isCandidateOnly ? "Your profile" : "Candidate input"}</p>
          <h2 id="candidate-profile-heading" className="mt-2 text-xl font-semibold">{isCandidateOnly ? "Describe the work you do best" : "Build the retrieval query"}</h2>
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
              {status === "loading" ? "Evaluating matches…" : isCandidateOnly ? "Discover my matches" : "Find eligible matches"}
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

          {decision && (
            <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2 border border-[#174a42]/20 bg-[#edf4ef] px-4 py-3 text-xs text-[#405c54]">
              <span><strong>Decision</strong> {decision.decisionId.slice(0, 8)}</span>
              <span><strong>Policy</strong> {decision.policyVersion}</span>
              <span><strong>Embedding</strong> {decision.embeddingModel}</span>
              <span><strong>Recorded</strong> {new Date(decision.decidedAt).toLocaleString()}</span>
            </div>
          )}

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

function PublicSite({
  authError,
  onLogin,
  onRegisterCandidate
}: {
  authError?: string;
  onLogin: () => Promise<void>;
  onRegisterCandidate: () => Promise<void>;
}) {
  return (
    <main className="overflow-hidden bg-[#081512] text-[#eef4ef]">
      <header className="relative z-20 border-b border-white/10">
        <nav aria-label="Primary navigation" className="mx-auto flex max-w-[1440px] items-center justify-between px-5 py-5 lg:px-10">
          <a className="flex items-center gap-3 text-white no-underline" href="#top">
            <BrandMark />
            <span className="text-sm font-bold tracking-[-0.02em]">Talent Intelligence</span>
          </a>
          <div className="hidden items-center gap-8 text-sm text-[#b7c6c0] md:flex">
            <a className="transition hover:text-white" href="#product">Product</a>
            <a className="transition hover:text-white" href="#trust">Trust</a>
            <a className="transition hover:text-white" href="#pricing">Pricing</a>
          </div>
          <div className="flex items-center gap-2">
            <button className="rounded-full px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10" onClick={() => { void onLogin(); }} type="button">
              Sign in
            </button>
            <button className="hidden rounded-full bg-[#b8f34a] px-4 py-2.5 text-sm font-bold text-[#081512] transition hover:bg-[#ccff68] sm:block" onClick={() => { void onRegisterCandidate(); }} type="button">
              Join free
            </button>
          </div>
        </nav>
      </header>

      <section id="top" className="relative">
        <div aria-hidden="true" className="absolute left-[55%] top-[-12rem] h-[42rem] w-[42rem] rounded-full bg-[#b8f34a]/10 blur-3xl" />
        <div className="mx-auto grid min-h-[760px] max-w-[1440px] items-center gap-14 px-5 py-20 lg:grid-cols-[1.08fr_0.92fr] lg:px-10 lg:py-28">
          <div className="relative z-10">
            <div className="inline-flex items-center gap-2 rounded-full border border-[#b8f34a]/30 bg-[#b8f34a]/10 px-3 py-1.5 text-xs font-bold uppercase tracking-[0.16em] text-[#d9ff91]">
              <span className="h-1.5 w-1.5 rounded-full bg-[#b8f34a]" />
              AI decisions with a paper trail
            </div>
            <h1 className="mt-8 max-w-4xl text-[clamp(3.4rem,8vw,7.6rem)] font-[780] leading-[0.86] tracking-[-0.07em] text-white">
              Talent signals,<br />without the <span className="text-[#b8f34a]">black box.</span>
            </h1>
            <p className="mt-8 max-w-2xl text-lg leading-8 text-[#b7c6c0] md:text-xl">
              Semantic discovery expands who gets seen. Deterministic policy decides who is eligible. Every recommendation arrives with evidence your team can inspect.
            </p>
            <div className="mt-10 flex flex-col gap-3 sm:flex-row">
              <button className="rounded-full bg-[#b8f34a] px-7 py-4 text-sm font-bold text-[#081512] transition hover:-translate-y-0.5 hover:bg-[#ccff68]" onClick={() => { void onRegisterCandidate(); }} type="button">
                Create candidate account
              </button>
              <button className="rounded-full border border-white/25 px-7 py-4 text-sm font-bold text-white transition hover:border-white/50 hover:bg-white/5" onClick={() => { void onLogin(); }} type="button">
                Recruiter sign in
              </button>
            </div>
            <p className="mt-5 text-xs leading-5 text-[#81928c]">Candidate accounts are free forever. Recruiter roles are organization-provisioned to protect tenant data.</p>
            {authError && (
              <div className="mt-6 max-w-2xl rounded-xl border border-[#ff6b4a]/40 bg-[#ff6b4a]/10 p-4 text-sm text-[#ffd2c8]" role="alert">
                Identity service is currently unavailable. The public product and pricing remain accessible. Detail: {authError}
              </div>
            )}
          </div>

          <div className="relative mx-auto w-full max-w-[620px]">
            <div className="absolute -inset-8 rotate-3 rounded-[2.5rem] border border-[#b8f34a]/20 bg-[#b8f34a]/5" />
            <div className="relative rounded-[1.8rem] border border-white/15 bg-[#10241f] p-3 shadow-[0_40px_120px_rgba(0,0,0,0.45)]">
              <div className="flex items-center justify-between border-b border-white/10 px-4 py-3">
                <div className="flex gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-[#ff6b4a]" /><span className="h-2.5 w-2.5 rounded-full bg-[#ffd166]" /><span className="h-2.5 w-2.5 rounded-full bg-[#b8f34a]" /></div>
                <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-[#789087]">Decision 8F21 · audited</span>
              </div>
              <div className="p-4 sm:p-6">
                <div className="flex items-start justify-between gap-4 rounded-2xl bg-[#f2f2ea] p-5 text-[#0a1a17]">
                  <div>
                    <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#61706b]">Top evidence-led match</p>
                    <h2 className="mt-2 text-2xl font-bold tracking-[-0.04em]">AI Platform Engineer</h2>
                    <p className="mt-1 text-sm text-[#61706b]">Atlas Talent · Hybrid</p>
                  </div>
                  <div className="rounded-xl bg-[#b8f34a] px-4 py-3 text-center"><p className="text-[10px] font-bold uppercase">Score</p><p className="text-3xl font-extrabold">92</p></div>
                </div>
                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  <EvidencePreview label="Policy" value="All hard constraints passed" accent="lime" />
                  <EvidencePreview label="Skill coverage" value="Kotlin · RAG · PGVector" accent="coral" />
                  <EvidencePreview label="Semantic relevance" value="0.94 verified retrieval" accent="lime" />
                  <EvidencePreview label="Governance" value="Policy + model versioned" accent="coral" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="product" className="bg-[#f1f1e9] px-5 py-24 text-[#0a1a17] lg:px-10 lg:py-32">
        <div className="mx-auto max-w-[1440px]">
          <div className="max-w-3xl">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#356355]">Handcrafted intelligence</p>
            <h2 className="mt-5 text-[clamp(2.6rem,6vw,5.5rem)] font-[770] leading-[0.95] tracking-[-0.06em]">AI is inside the workflow.<br />Never above the rules.</h2>
          </div>
          <div className="mt-16 grid border-y border-[#0a1a17]/15 md:grid-cols-3">
            <ProductStep number="01" title="Retrieve broadly" body="Local embeddings and PGVector discover transferable experience beyond exact keywords." />
            <ProductStep number="02" title="Decide explicitly" body="Salary, seniority, location, and work mode remain deterministic business policy." />
            <ProductStep number="03" title="Prove every result" body="Scores, evidence, actor, policy, and model versions become an immutable decision snapshot." />
          </div>
        </div>
      </section>

      <section id="trust" className="border-y border-white/10 bg-[#0d211c] px-5 py-24 lg:px-10 lg:py-32">
        <div className="mx-auto grid max-w-[1440px] gap-14 lg:grid-cols-[0.85fr_1.15fr]">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#b8f34a]">Built for scrutiny</p>
            <h2 className="mt-5 text-5xl font-bold leading-[0.96] tracking-[-0.055em] md:text-7xl">Useful AI.<br />Bounded blast radius.</h2>
          </div>
          <div className="grid gap-px overflow-hidden rounded-3xl bg-white/10 sm:grid-cols-2">
            <TrustCell title="Tenant-owned context" body="Organization identity comes from signed claims, never browser-selected headers." />
            <TrustCell title="Transient candidate input" body="Raw profile text is not stored in the current decision pipeline." />
            <TrustCell title="Fail-closed access" body="Candidate, recruiter, and admin capabilities are explicitly allow-listed per route." />
            <TrustCell title="Reproducible locally" body="Keycloak, PostgreSQL, PGVector, API, model, and React run from one Compose stack." />
          </div>
        </div>
      </section>

      <section id="pricing" className="bg-[#f1f1e9] px-5 py-24 text-[#0a1a17] lg:px-10 lg:py-32">
        <div className="mx-auto max-w-[1440px]">
          <div className="flex flex-col justify-between gap-8 lg:flex-row lg:items-end">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#356355]">Launch pricing</p>
              <h2 className="mt-5 text-5xl font-bold tracking-[-0.06em] md:text-7xl">Free for talent.<br />Predictable for teams.</h2>
            </div>
            <p className="max-w-xl text-base leading-7 text-[#5b6864]">Organizations pay for governed evaluations, not opaque token counters. Billing and entitlement enforcement are intentionally outside this preview.</p>
          </div>
          <div className="mt-14 grid gap-4 lg:grid-cols-4">
            <PricingCard name="Candidate" price="$0" cadence="forever" items={["Public role discovery", "Evidence-led matching", "Transient profile input"]} cta="Create free account" onClick={onRegisterCandidate} />
            <PricingCard name="Recruiter Sandbox" price="$0" cadence="per workspace" items={["1 recruiter seat", "1 active role", "50 evaluations / month"]} cta="Recruiter sign in" onClick={onLogin} />
            <PricingCard featured name="Recruiter Team" price="$49" cadence="per recruiter / month, annual" items={["15 active roles", "2,000 evaluations / month", "Audit, collaboration, exports"]} cta="Request workspace" />
            <PricingCard name="Enterprise" price="Custom" cadence="security + scale" items={["SSO and SCIM", "Regional data controls", "Private connectivity and SLA"]} cta="Talk to us" />
          </div>
          <p className="mt-6 text-xs text-[#6c7974]">Pricing is a version 0.3 market hypothesis. Taxes, overages, metering, and payment collection are not yet implemented.</p>
        </div>
      </section>

      <section id="access" className="bg-[#b8f34a] px-5 py-20 text-[#081512] lg:px-10 lg:py-24">
        <div className="mx-auto flex max-w-[1440px] flex-col justify-between gap-10 lg:flex-row lg:items-end">
          <div className="max-w-4xl">
            <p className="text-xs font-bold uppercase tracking-[0.2em]">Organization access</p>
            <h2 className="mt-5 text-[clamp(2.8rem,6vw,5.8rem)] font-[780] leading-[0.92] tracking-[-0.06em]">No role roulette.<br />Every recruiter belongs to a verified workspace.</h2>
          </div>
          <div className="max-w-md">
            <p className="text-sm leading-6 text-[#29443b]">Recruiter signup provisions a tenant, plan, and invitation through an administrative boundary. That workflow is documented but not automated in this preview; existing local reviewers can use the synthetic recruiter account.</p>
            <button className="mt-6 rounded-full bg-[#081512] px-6 py-3.5 text-sm font-bold text-white transition hover:-translate-y-0.5 hover:bg-[#15362d]" onClick={() => { void onLogin(); }} type="button">Sign in to a recruiter workspace</button>
          </div>
        </div>
      </section>

      <footer className="border-t border-white/10 px-5 py-10 lg:px-10">
        <div className="mx-auto flex max-w-[1440px] flex-col justify-between gap-5 text-sm text-[#81928c] sm:flex-row sm:items-center">
          <div className="flex items-center gap-3"><BrandMark compact /><span>Talent Intelligence · Decisions with evidence.</span></div>
          <span>Candidate access is free. Recruiter access is organization-managed.</span>
        </div>
      </footer>
    </main>
  );
}

function BrandMark({ compact = false }: { compact?: boolean }) {
  return (
    <span aria-hidden="true" className={`${compact ? "h-9 w-9 rounded-xl" : "h-11 w-11 rounded-[0.9rem]"} relative inline-flex shrink-0 items-center justify-center bg-[#b8f34a] text-lg font-black text-[#081512]`}>
      T<span className="absolute bottom-1.5 right-1.5 h-2 w-2 rounded-full bg-[#ff6b4a]" />
    </span>
  );
}

function EvidencePreview({ label, value, accent }: { label: string; value: string; accent: "lime" | "coral" }) {
  return (
    <div className="rounded-xl border border-white/10 bg-[#18312a] p-4">
      <div className={`mb-3 h-1 w-8 rounded-full ${accent === "lime" ? "bg-[#b8f34a]" : "bg-[#ff6b4a]"}`} />
      <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#789087]">{label}</p>
      <p className="mt-1 text-sm font-semibold text-white">{value}</p>
    </div>
  );
}

function ProductStep({ number, title, body }: { number: string; title: string; body: string }) {
  return (
    <article className="border-[#0a1a17]/15 py-9 md:border-r md:px-8 md:first:pl-0 md:last:border-r-0">
      <span className="font-mono text-xs text-[#62726c]">{number}</span>
      <h3 className="mt-10 text-2xl font-bold tracking-[-0.035em]">{title}</h3>
      <p className="mt-3 max-w-sm text-sm leading-6 text-[#5b6864]">{body}</p>
    </article>
  );
}

function TrustCell({ title, body }: { title: string; body: string }) {
  return (
    <article className="bg-[#0d211c] p-7 sm:p-9">
      <div className="mb-8 h-2.5 w-2.5 rounded-full bg-[#b8f34a]" />
      <h3 className="text-xl font-bold tracking-[-0.03em] text-white">{title}</h3>
      <p className="mt-3 text-sm leading-6 text-[#9aada6]">{body}</p>
    </article>
  );
}

function PricingCard({
  name,
  price,
  cadence,
  items,
  cta,
  featured = false,
  onClick
}: {
  name: string;
  price: string;
  cadence: string;
  items: string[];
  cta: string;
  featured?: boolean;
  onClick?: () => Promise<void>;
}) {
  return (
    <article className={`flex min-h-[440px] flex-col rounded-3xl border p-6 ${featured ? "border-[#0a1a17] bg-[#0a1a17] text-white" : "border-[#0a1a17]/15 bg-[#fafaf5]"}`}>
      <h3 className={`text-xs font-bold uppercase tracking-[0.16em] ${featured ? "text-[#b8f34a]" : "text-[#557168]"}`}>{name}</h3>
      <p className="mt-8 text-5xl font-extrabold tracking-[-0.06em]">{price}</p>
      <p className={`mt-2 min-h-10 text-xs ${featured ? "text-[#9aada6]" : "text-[#6c7974]"}`}>{cadence}</p>
      <ul className={`mt-8 space-y-3 border-t pt-6 text-sm ${featured ? "border-white/15 text-[#ced9d5]" : "border-[#0a1a17]/15 text-[#45534f]"}`}>
        {items.map((item) => <li key={item} className="flex gap-2"><span className="font-bold text-[#58a98f]">✓</span><span>{item}</span></li>)}
      </ul>
      {onClick ? (
        <button className={`mt-auto rounded-full px-5 py-3 text-sm font-bold ${featured ? "bg-[#b8f34a] text-[#0a1a17]" : "border border-[#0a1a17]/20 hover:bg-[#0a1a17] hover:text-white"}`} onClick={() => { void onClick(); }} type="button">{cta}</button>
      ) : (
        <a className={`mt-auto rounded-full px-5 py-3 text-center text-sm font-bold ${featured ? "bg-[#b8f34a] text-[#0a1a17]" : "border border-[#0a1a17]/20 hover:bg-[#0a1a17] hover:text-white"}`} href="#access">{cta}</a>
      )}
    </article>
  );
}

function AccessPending({ auth }: { auth: AuthSession }) {
  return (
    <main className="grid min-h-screen place-items-center bg-[#081512] p-6 text-white">
      <section className="w-full max-w-xl rounded-3xl border border-white/15 bg-[#10241f] p-8 shadow-2xl">
        <BrandMark />
        <p className="mt-8 text-xs font-bold uppercase tracking-[0.18em] text-[#b8f34a]">Access pending</p>
        <h1 className="mt-3 text-4xl font-bold tracking-[-0.05em]">Your identity is valid, but no product capability is assigned.</h1>
        <p className="mt-5 text-sm leading-7 text-[#a9bbb4]">Candidate accounts receive access automatically. Recruiter and administrator roles are assigned through an organization workspace to protect tenant data.</p>
        <button className="mt-8 rounded-full border border-white/20 px-5 py-3 text-sm font-bold hover:bg-white/10" onClick={() => { void auth.logout(); }} type="button">Sign out</button>
      </section>
    </main>
  );
}

function parseList(value: string): string[] {
  return value.split(",").map((item) => item.trim()).filter((item) => item.length > 0);
}

function toggle<T>(values: T[], value: T): T[] {
  return values.includes(value) ? values.filter((item) => item !== value) : [...values, value];
}
