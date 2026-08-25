package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.CandidateProfile
import io.github.pedrovvitor.talentintelligence.domain.EligibilityPolicy
import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
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
        val service = MatchingService(catalog, index, ConstantEmbeddingGateway, EligibilityPolicy())

        val matches = service.match(candidate(), 5)

        assertEquals(1, matches.size)
        assertEquals(eligibleJob.id, matches.single().jobId)
        assertTrue(matches.single().evidence.any { it.type == "matched-skills" })
        assertEquals(0.874, matches.single().finalScore)
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
    override fun embed(text: String): FloatArray = floatArrayOf(1f, 0f)
}

internal class FixedSemanticIndex(
    private val candidates: List<SemanticJobCandidate>,
) : SemanticJobIndex {
    override fun index(jobId: UUID, searchableContent: String, embedding: FloatArray) = Unit

    override fun search(queryEmbedding: FloatArray, limit: Int): List<SemanticJobCandidate> = candidates.take(limit)
}

internal class InMemoryJobCatalog(jobs: List<JobPosting>) : JobCatalog {
    private val jobsById = jobs.associateBy(JobPosting::id).toMutableMap()

    override fun save(job: JobPosting): JobPosting = job.also { jobsById[it.id] = it }

    override fun findById(id: UUID): JobPosting? = jobsById[id]

    override fun findAll(): List<JobPosting> = jobsById.values.toList()

    override fun count(): Long = jobsById.size.toLong()
}
