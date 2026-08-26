# Security Verification Matrix

All fixtures use reserved `.test` identities and synthetic candidate/job content. No resume or personal data is loaded by the automated suite.

| Boundary | Scenario | Mechanism | Expected result |
|---|---|---|---|
| HTTP authentication | Missing bearer token | Request through Spring Security filter chain | `401 AUTHENTICATION_REQUIRED` |
| JWT cryptography | Token signed by an untrusted RSA key | Real in-memory RS256 encoder; decoder trusts a different public key | `401 AUTHENTICATION_REQUIRED` |
| JWT lifetime | Correctly signed token expired beyond clock skew | Real RS256 token plus timestamp validator | `401 AUTHENTICATION_REQUIRED` |
| Capability RBAC | Correctly signed token with only `viewer` | Production role converter and route policy | `403 ACCESS_DENIED` |
| Candidate marketplace | Candidate-only token without tenant claim | Production identity policy and match route | Server-owned marketplace tenant is used |
| Candidate audit boundary | Candidate requests a decision identifier | Route policy before controller | `403 ACCESS_DENIED` |
| Privileged tenancy | Recruiter token without tenant claim | Production JWT converter | `401 AUTHENTICATION_REQUIRED` |
| Catalog mutation | Recruiter invokes admin capability | Authorized identity without mutation capability | `403 ACCESS_DENIED` before request parsing |
| Decision read | Tenant B requests tenant A decision identifier | Signed tenant B token propagated to the service | Tenant-scoped lookup and `404 MATCH_DECISION_NOT_FOUND` |
| Catalog persistence | Tenant A reads tenant B job | Actual PostgreSQL repository query | No row returned |
| Vector persistence | Tenant A searches with tenant B vectors present | Actual PGVector query | Only tenant A identifiers returned |
| Database invariant | Tenant A embedding references tenant B job | Direct synthetic SQL fixture | Composite foreign key rejects insert |
| Audit persistence | Tenant B reads tenant A decision | Actual PostgreSQL audit adapter | No decision returned |
| Audit immutability | Existing audit row is updated | Direct synthetic SQL fixture | Append-only trigger rejects update |

## Layered rationale

HTTP tests prove token decoding, error contracts, authority conversion, and tenant propagation. Docker-backed persistence tests independently prove that repository predicates and database constraints remain isolated even if a caller reaches an adapter directly. Neither layer substitutes for the other.

The RSA keys are generated per test process, remain in memory, and are not credentials. The untrusted key fixture tests signature verification without contacting an external identity provider. Local Docker smoke tests separately exercise the pinned Keycloak realm with short-lived real tokens.
