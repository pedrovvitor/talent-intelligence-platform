package io.github.pedrovvitor.talentintelligence.domain

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EligibilityPolicyTest {
    private val policy = EligibilityPolicy()

    @Test
    fun `accepts a candidate who satisfies every hard constraint`() {
        val decision = policy.evaluate(candidate(), job())

        assertTrue(decision.eligible)
        assertTrue(decision.reasons.isEmpty())
    }

    @Test
    fun `rejects a candidate before semantic ranking when hard constraints fail`() {
        val decision = policy.evaluate(
            candidate().copy(
                seniority = Seniority.MID,
                preferredWorkModes = setOf(WorkMode.ONSITE),
                minimumSalary = BigDecimal("180000"),
            ),
            job(),
        )

        assertFalse(decision.eligible)
        assertTrue(decision.reasons.size == 3)
    }

    private fun candidate(): CandidateProfile = CandidateProfile(
        headline = "Senior JVM Engineer",
        summary = "Kotlin backend engineer",
        skills = setOf("Kotlin", "Spring Boot"),
        seniority = Seniority.SENIOR,
        preferredWorkModes = setOf(WorkMode.REMOTE),
        preferredLocations = emptySet(),
        minimumSalary = BigDecimal("100000"),
    )

    private fun job(): JobPosting = JobPosting(
        id = UUID.randomUUID(),
        title = "Senior Backend Engineer",
        company = "Northstar",
        description = "Build Kotlin services",
        requiredSkills = setOf("Kotlin", "Spring Boot"),
        seniority = Seniority.SENIOR,
        workMode = WorkMode.REMOTE,
        location = null,
        salaryMin = BigDecimal("120000"),
        salaryMax = BigDecimal("150000"),
    )
}
