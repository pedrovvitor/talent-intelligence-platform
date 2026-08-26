package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.CandidateProfile
import io.github.pedrovvitor.talentintelligence.domain.EligibilityPolicy
import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.MatchDecision
import io.github.pedrovvitor.talentintelligence.domain.MatchDecisionRecord
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchingServiceTest {
    @Test
    fun `filters ineligible jobs and ranks eligible jobs with evidence`() {
        val eligibleJob = job(title = "Senior Kotlin Engineer", workMode = WorkMode.REMOTE)
        val ineligibleJob = job(title = "Onsite Kotlin Engineer", workMode = WorkMode.ONSITE)
        val catalog = InMemoryJobCatalog(listOf(eligibleJob, ineligibleJob))
        val index = FixedSemanticIndex(
            listOf(
                SemanticJobCandidate(ineligibleJob.id, 0.99),
                SemanticJobCandidate(eligibleJob.id, 0.82),
            ),
        )
        val audit = RecordingDecisionAudit()
        val service = MatchingService(
            catalog,
            index,
            ConstantEmbeddingGateway,
            EligibilityPolicy(),
            FixedSourceFingerprinter,
            audit,
            FIXED_CLOCK,
        )

        val decision = service.match(TEST_IDENTITY, candidate(), 5)
        val matches = decision.matches

        assertEquals(1, matches.size)
        assertEquals(eligibleJob.id, matches.single().jobId)
        assertTrue(matches.single().evidence.any { it.type == "matched-skills" })
        assertEquals(0.874, matches.single().finalScore)
        assertEquals("eligibility-policy-v1", decision.policyVersion)
        assertEquals(ConstantEmbeddingGateway.modelVersion, decision.embeddingModel)
        assertEquals("synthetic-fingerprint", audit.record?.sourceFingerprint)
        assertEquals("synthetic-key-v1", audit.record?.fingerprintKeyVersion)
        assertEquals("synthetic-actor", audit.record?.actorId)
        assertEquals("candidate-job-matching", audit.record?.purpose)
    }

    private fun candidate(): CandidateProfile = CandidateProfile(
        headline = "Senior Kotlin Engineer",
        summary = "Backend engineer with JVM experience",
        skills = setOf("Kotlin", "Spring Boot", "PostgreSQL"),
        seniority = Seniority.SENIOR,
        preferredWorkModes = setOf(WorkMode.REMOTE),
        preferredLocations = emptySet(),
        minimumSalary = BigDecimal("100000"),
    )

    private fun job(title: String, workMode: WorkMode): JobPosting = JobPosting(
        id = UUID.randomUUID(),
        title = title,
        company = "Northstar",
        description = "Build Kotlin and Spring Boot services",
        requiredSkills = setOf("Kotlin", "Spring Boot"),
        seniority = Seniority.SENIOR,
        workMode = workMode,
        location = if (workMode == WorkMode.REMOTE) null else "Sao Paulo",
        salaryMin = BigDecimal("120000"),
        salaryMax = BigDecimal("150000"),
    )
}

internal object ConstantEmbeddingGateway : EmbeddingGateway {
    override val modelVersion: String = "synthetic-embedding-v1"

    override fun embed(text: String): FloatArray = floatArrayOf(1f, 0f)
}

internal object FixedSourceFingerprinter : CandidateSourceFingerprinter {
    override fun fingerprint(tenantId: TenantId, candidate: CandidateProfile): CandidateSourceFingerprint =
        CandidateSourceFingerprint("synthetic-fingerprint", "synthetic-key-v1")
}

internal class RecordingDecisionAudit : MatchDecisionAudit {
    var record: MatchDecisionRecord? = null

    override fun append(record: MatchDecisionRecord) {
        this.record = record
    }

    override fun findById(tenantId: TenantId, decisionId: UUID): MatchDecision? =
        record?.takeIf { it.tenantId == tenantId && it.decision.id == decisionId }?.decision
}

internal class FixedSemanticIndex(
    private val candidates: List<SemanticJobCandidate>,
) : SemanticJobIndex {
    override fun index(
        tenantId: TenantId,
        jobId: UUID,
        searchableContent: String,
        embedding: FloatArray,
        embeddingModel: String,
    ) = Unit

    override fun search(
        tenantId: TenantId,
        queryEmbedding: FloatArray,
        embeddingModel: String,
        limit: Int,
    ): List<SemanticJobCandidate> =
        candidates.take(limit)
}

internal class InMemoryJobCatalog(jobs: List<JobPosting>) : JobCatalog {
    private val jobsByTenantAndId = jobs.associateBy { job -> TEST_TENANT_ID to job.id }.toMutableMap()

    override fun save(tenantId: TenantId, job: JobPosting): JobPosting =
        job.also { jobsByTenantAndId[tenantId to it.id] = it }

    override fun findById(tenantId: TenantId, id: UUID): JobPosting? = jobsByTenantAndId[tenantId to id]

    override fun findAll(tenantId: TenantId): List<JobPosting> = jobsByTenantAndId
        .filterKeys { (jobTenantId, _) -> jobTenantId == tenantId }
        .values
        .toList()

    override fun count(tenantId: TenantId): Long = findAll(tenantId).size.toLong()
}

internal val TEST_TENANT_ID = TenantId(UUID.fromString("00000000-0000-0000-0000-000000000099"))
internal val TEST_IDENTITY = RequestIdentity("synthetic-actor", TEST_TENANT_ID)
internal val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC)
