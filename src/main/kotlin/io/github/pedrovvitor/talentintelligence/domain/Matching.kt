package io.github.pedrovvitor.talentintelligence.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CandidateProfile(
    val headline: String,
    val summary: String,
    val skills: Set<String>,
    val seniority: Seniority,
    val preferredWorkModes: Set<WorkMode>,
    val preferredLocations: Set<String>,
    val minimumSalary: BigDecimal?,
)

data class EligibilityDecision(
    val eligible: Boolean,
    val reasons: List<String>,
)

data class MatchEvidence(
    val type: String,
    val label: String,
    val value: String,
)

data class JobMatch(
    val jobId: UUID,
    val title: String,
    val company: String,
    val semanticScore: Double,
    val skillCoverage: Double,
    val finalScore: Double,
    val evidence: List<MatchEvidence>,
)

class EligibilityPolicy {
    val version: String = POLICY_VERSION

    fun evaluate(candidate: CandidateProfile, job: JobPosting): EligibilityDecision {
        val rejectionReasons = buildList {
            if (candidate.seniority.level < job.seniority.level) {
                add("Candidate seniority is below the role requirement")
            }
            if (job.workMode !in candidate.preferredWorkModes) {
                add("Work mode does not match candidate preferences")
            }
            if (!locationMatches(candidate, job)) {
                add("Location does not match candidate preferences")
            }
            if (!salaryMatches(candidate, job)) {
                add("Maximum salary is below the candidate minimum")
            }
        }
        return EligibilityDecision(rejectionReasons.isEmpty(), rejectionReasons)
    }

    private fun locationMatches(candidate: CandidateProfile, job: JobPosting): Boolean {
        if (job.workMode == WorkMode.REMOTE || job.location == null) {
            return true
        }
        val preferredLocations = candidate.preferredLocations.map(String::normalized).toSet()
        return job.location.normalized() in preferredLocations
    }

    private fun salaryMatches(candidate: CandidateProfile, job: JobPosting): Boolean {
        val minimumSalary = candidate.minimumSalary ?: return true
        val maximumSalary = job.salaryMax ?: return true
        return maximumSalary >= minimumSalary
    }

    companion object {
        const val POLICY_VERSION = "eligibility-policy-v1"
    }
}

data class MatchDecision(
    val id: UUID,
    val createdAt: Instant,
    val policyVersion: String,
    val embeddingModel: String,
    val generativeModel: String?,
    val promptVersion: String?,
    val matches: List<JobMatch>,
)

data class MatchDecisionRecord(
    val decision: MatchDecision,
    val tenantId: TenantId,
    val actorId: String,
    val purpose: String,
    val sourceFingerprint: String,
    val fingerprintKeyVersion: String,
)

internal fun String.normalized(): String = trim().lowercase()
