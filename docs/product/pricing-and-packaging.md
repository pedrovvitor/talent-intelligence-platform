# Pricing and Packaging

## Commercial principle

Talent Intelligence charges organizations for governed recruiting operations. Candidates never pay to discover work. Model-token accounting remains an internal cost signal; customers buy understandable product capacity measured in active roles, recruiter seats, and governed evaluations.

## Launch hypothesis

| Plan | Price | Included | Intended customer |
|---|---:|---|---|
| Candidate | USD 0 forever | Public role discovery, evidence-led matches, transient profile input | Individual job seekers |
| Recruiter Sandbox | USD 0 | 1 recruiter, 1 active role, 50 governed evaluations/month | Evaluation and small hiring experiments |
| Recruiter Team | USD 49/recruiter/month, annual billing | 15 active roles, 2,000 governed evaluations/workspace/month, collaboration, decision audit, exports | Growing recruiting teams |
| Enterprise | Custom | SSO/SCIM, regional controls, private connectivity, custom retention, procurement support, SLA | Regulated and multi-business organizations |

The Team price is a testable hypothesis, not a billing commitment. Version 0.3 contains no checkout, metering enforcement, automatic upgrade, or paid-service dependency.

## Why this packaging

- Free candidate access removes a trust and marketplace-adoption barrier.
- A constrained recruiter sandbox makes evaluation possible without a sales call while protecting inference and support costs.
- Per-recruiter Team pricing matches how recruiting software is budgeted, while the workspace evaluation allowance makes AI cost predictable.
- Enterprise features correspond to real security, compliance, support, and deployment costs rather than artificial feature withholding.
- Evaluation bundles are easier to forecast and explain than variable token charges.

## Entitlement boundary for a later release

Billing must not be enforced in React. A future entitlement service will resolve the organization plan from the authenticated tenant, reserve an evaluation atomically before a costly operation, record idempotent usage, and return a typed denial when an allowance is exhausted. Authorization and deterministic eligibility remain independent of commercial entitlements.

## Pricing validation

Before implementing billing:

1. Interview at least five recruiting leads about active-role volume and evaluation frequency.
2. Measure local and remote model cost per governed evaluation.
3. Test Sandbox-to-Team conversion at 50, 100, and 250 monthly evaluations.
4. Validate whether buyers prefer per-seat pricing or a workspace base fee.
5. Define taxes, refunds, cancellation, data export, and retention before collecting payment.
