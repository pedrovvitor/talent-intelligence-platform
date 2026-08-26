package io.github.pedrovvitor.talentintelligence.adapter.security

import io.github.pedrovvitor.talentintelligence.domain.CandidateProfile
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HmacCandidateSourceFingerprinterTest {
    private val fingerprinter = HmacCandidateSourceFingerprinter(
        "synthetic-test-key-with-more-than-32-bytes",
        "synthetic-key-v1",
    )

    @Test
    fun `fingerprint is canonical without exposing candidate content`() {
        val first = candidate(setOf("Kotlin", "Spring Boot"))
        val reordered = candidate(setOf("Spring Boot", "Kotlin"))

        val firstFingerprint = fingerprinter.fingerprint(TENANT_A, first)
        val reorderedFingerprint = fingerprinter.fingerprint(TENANT_A, reordered)

        assertEquals(firstFingerprint, reorderedFingerprint)
        assertEquals(64, firstFingerprint.value.length)
        assertEquals("synthetic-key-v1", firstFingerprint.keyVersion)
        assertTrue(firstFingerprint.value.all { character -> character.isDigit() || character in 'a'..'f' })
        assertTrue("synthetic" !in firstFingerprint.value)
    }

    @Test
    fun `fingerprint is bound to tenant identity`() {
        val candidate = candidate(setOf("Kotlin"))

        assertNotEquals(
            fingerprinter.fingerprint(TENANT_A, candidate),
            fingerprinter.fingerprint(TENANT_B, candidate),
        )
    }

    private fun candidate(skills: Set<String>): CandidateProfile = CandidateProfile(
        headline = "Synthetic JVM Engineer",
        summary = "Builds reliable services",
        skills = skills,
        seniority = Seniority.SENIOR,
        preferredWorkModes = setOf(WorkMode.REMOTE, WorkMode.HYBRID),
        preferredLocations = setOf("Fortaleza"),
        minimumSalary = BigDecimal("110000.00"),
    )

    companion object {
        private val TENANT_A = TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        private val TENANT_B = TenantId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    }
}
