package io.github.pedrovvitor.talentintelligence.adapter.web

import io.github.pedrovvitor.talentintelligence.application.CreateJobCommand
import io.github.pedrovvitor.talentintelligence.application.JobCatalogService
import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

data class CreateJobRequest(
    @field:NotBlank @field:Size(max = 160) val title: String,
    @field:NotBlank @field:Size(max = 160) val company: String,
    @field:NotBlank @field:Size(max = 10_000) val description: String,
    @field:NotEmpty val requiredSkills: Set<@NotBlank @Size(max = 80) @Pattern(regexp = "^[^|]+$") String>,
    val seniority: Seniority,
    val workMode: WorkMode,
    @field:Size(max = 160) val location: String?,
    @field:DecimalMin("0.00") val salaryMin: BigDecimal?,
    @field:DecimalMin("0.00") val salaryMax: BigDecimal?,
)

data class JobResponse(
    val id: UUID,
    val title: String,
    val company: String,
    val description: String,
    val requiredSkills: Set<String>,
    val seniority: Seniority,
    val workMode: WorkMode,
    val location: String?,
    val salaryMin: BigDecimal?,
    val salaryMax: BigDecimal?,
) {
    companion object {
        fun from(job: JobPosting): JobResponse = JobResponse(
            id = job.id,
            title = job.title,
            company = job.company,
            description = job.description,
            requiredSkills = job.requiredSkills,
            seniority = job.seniority,
            workMode = job.workMode,
            location = job.location,
            salaryMin = job.salaryMin,
            salaryMax = job.salaryMax,
        )
    }
}

@RestController
@RequestMapping("/api/jobs")
class JobController(
    private val jobCatalogService: JobCatalogService,
    private val identityResolver: RequestIdentityResolver,
) {
    @GetMapping
    fun list(authentication: JwtAuthenticationToken): List<JobResponse> =
        jobCatalogService.list(identityResolver.resolve(authentication).tenantId).map(JobResponse::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateJobRequest,
        authentication: JwtAuthenticationToken,
    ): JobResponse = JobResponse.from(
        jobCatalogService.create(
            identityResolver.resolve(authentication).tenantId,
            CreateJobCommand(
                title = request.title,
                company = request.company,
                description = request.description,
                requiredSkills = request.requiredSkills,
                seniority = request.seniority,
                workMode = request.workMode,
                location = request.location,
                salaryMin = request.salaryMin,
                salaryMax = request.salaryMax,
            ),
        ),
    )
}
