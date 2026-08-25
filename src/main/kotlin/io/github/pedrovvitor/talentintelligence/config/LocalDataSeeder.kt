package io.github.pedrovvitor.talentintelligence.config

import io.github.pedrovvitor.talentintelligence.application.CreateJobCommand
import io.github.pedrovvitor.talentintelligence.application.JobCatalogService
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@Profile("local")
class LocalDataSeeder(
    private val jobCatalogService: JobCatalogService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (jobCatalogService.count() > 0) {
            return
        }
        sampleJobs.forEach(jobCatalogService::create)
    }

    private val sampleJobs = listOf(
        CreateJobCommand(
            title = "Senior Backend Engineer",
            company = "Northstar Systems",
            description = "Design and operate JVM services for a high-volume talent platform using Kotlin, Spring Boot, PostgreSQL, and Kubernetes.",
            requiredSkills = setOf("Kotlin", "Spring Boot", "PostgreSQL", "Kubernetes"),
            seniority = Seniority.SENIOR,
            workMode = WorkMode.REMOTE,
            location = null,
            salaryMin = BigDecimal("120000"),
            salaryMax = BigDecimal("155000"),
        ),
        CreateJobCommand(
            title = "AI Platform Engineer",
            company = "Atlas Talent",
            description = "Build retrieval, evaluation, and model orchestration services with vector search, observability, and evidence-based outputs.",
            requiredSkills = setOf("Kotlin", "RAG", "PGVector", "OpenTelemetry"),
            seniority = Seniority.SENIOR,
            workMode = WorkMode.HYBRID,
            location = "Fortaleza",
            salaryMin = BigDecimal("130000"),
            salaryMax = BigDecimal("170000"),
        ),
        CreateJobCommand(
            title = "Frontend Engineer",
            company = "BrightPath",
            description = "Create accessible recruiter workflows with React, TypeScript, design systems, and reliable API integration.",
            requiredSkills = setOf("React", "TypeScript", "Accessibility", "Testing"),
            seniority = Seniority.MID,
            workMode = WorkMode.REMOTE,
            location = null,
            salaryMin = BigDecimal("90000"),
            salaryMax = BigDecimal("125000"),
        ),
    )
}
