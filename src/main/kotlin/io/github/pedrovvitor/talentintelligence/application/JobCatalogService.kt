package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

data class CreateJobCommand(
    val title: String,
    val company: String,
    val description: String,
    val requiredSkills: Set<String>,
    val seniority: Seniority,
    val workMode: WorkMode,
    val location: String?,
    val salaryMin: BigDecimal?,
    val salaryMax: BigDecimal?,
)

@Service
class JobCatalogService(
    private val jobCatalog: JobCatalog,
    private val semanticJobIndex: SemanticJobIndex,
    private val embeddingGateway: EmbeddingGateway,
) {
    @Transactional
    fun create(tenantId: TenantId, command: CreateJobCommand): JobPosting {
        val job = JobPosting(
            id = UUID.randomUUID(),
            title = command.title.trim(),
            company = command.company.trim(),
            description = command.description.trim(),
            requiredSkills = command.requiredSkills.map(String::trim).filter(String::isNotEmpty).toSet(),
            seniority = command.seniority,
            workMode = command.workMode,
            location = command.location?.trim()?.takeIf(String::isNotEmpty),
            salaryMin = command.salaryMin,
            salaryMax = command.salaryMax,
        )
        validateSalary(job)
        jobCatalog.save(tenantId, job)
        val searchableContent = buildSearchableContent(job)
        semanticJobIndex.index(tenantId, job.id, searchableContent, embeddingGateway.embed(searchableContent))
        return job
    }

    fun list(tenantId: TenantId): List<JobPosting> = jobCatalog.findAll(tenantId)

    fun count(tenantId: TenantId): Long = jobCatalog.count(tenantId)

    private fun validateSalary(job: JobPosting) {
        if (job.salaryMin != null && job.salaryMax != null && job.salaryMin > job.salaryMax) {
            throw InvalidJobException("salaryMin must be less than or equal to salaryMax")
        }
    }

    private fun buildSearchableContent(job: JobPosting): String = buildString {
        appendLine(job.title)
        appendLine(job.description)
        append(job.requiredSkills.joinToString(" "))
    }
}

class InvalidJobException(message: String) : RuntimeException(message)
