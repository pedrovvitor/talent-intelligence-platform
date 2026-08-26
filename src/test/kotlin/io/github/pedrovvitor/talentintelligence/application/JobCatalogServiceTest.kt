package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertFailsWith

class JobCatalogServiceTest {
    @Test
    fun `rejects an inverted salary range before persistence`() {
        val catalog = InMemoryJobCatalog(emptyList())
        val service = JobCatalogService(catalog, FixedSemanticIndex(emptyList()), ConstantEmbeddingGateway)

        assertFailsWith<InvalidJobException> {
            service.create(
                TEST_TENANT_ID,
                CreateJobCommand(
                    title = "Backend Engineer",
                    company = "Northstar",
                    description = "Build backend services",
                    requiredSkills = setOf("Kotlin"),
                    seniority = Seniority.SENIOR,
                    workMode = WorkMode.REMOTE,
                    location = null,
                    salaryMin = BigDecimal("160000"),
                    salaryMax = BigDecimal("120000"),
                ),
            )
        }
    }
}
