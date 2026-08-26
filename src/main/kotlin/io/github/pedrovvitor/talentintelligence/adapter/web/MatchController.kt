package io.github.pedrovvitor.talentintelligence.adapter.web

import io.github.pedrovvitor.talentintelligence.application.MatchingService
import io.github.pedrovvitor.talentintelligence.domain.CandidateProfile
import io.github.pedrovvitor.talentintelligence.domain.JobMatch
import io.github.pedrovvitor.talentintelligence.domain.MatchEvidence
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

data class MatchRequest(
    @field:NotBlank @field:Size(max = 240) val headline: String,
    @field:NotBlank @field:Size(max = 10_000) val summary: String,
    @field:NotEmpty val skills: Set<@NotBlank @Size(max = 80) String>,
    val seniority: Seniority,
    @field:NotEmpty val preferredWorkModes: Set<WorkMode>,
    val preferredLocations: Set<@NotBlank @Size(max = 160) String> = emptySet(),
    @field:DecimalMin("0.00") val minimumSalary: BigDecimal?,
    @field:Min(1) @field:Max(20) val limit: Int = 5,
)

data class MatchResponse(
    val matches: List<JobMatchResponse>,
)

data class JobMatchResponse(
    val jobId: UUID,
    val title: String,
    val company: String,
    val semanticScore: Double,
    val skillCoverage: Double,
    val finalScore: Double,
    val evidence: List<MatchEvidence>,
) {
    companion object {
        fun from(match: JobMatch): JobMatchResponse = JobMatchResponse(
            jobId = match.jobId,
            title = match.title,
            company = match.company,
            semanticScore = match.semanticScore,
            skillCoverage = match.skillCoverage,
            finalScore = match.finalScore,
            evidence = match.evidence,
        )
    }
}

@RestController
@RequestMapping("/api/matches")
class MatchController(
    private val matchingService: MatchingService,
    private val identityResolver: RequestIdentityResolver,
) {
    @PostMapping
    fun match(
        @Valid @RequestBody request: MatchRequest,
        authentication: JwtAuthenticationToken,
    ): MatchResponse {
        val candidate = CandidateProfile(
            headline = request.headline.trim(),
            summary = request.summary.trim(),
            skills = request.skills.map(String::trim).filter(String::isNotEmpty).toSet(),
            seniority = request.seniority,
            preferredWorkModes = request.preferredWorkModes,
            preferredLocations = request.preferredLocations.map(String::trim).filter(String::isNotEmpty).toSet(),
            minimumSalary = request.minimumSalary,
        )
        val tenantId = identityResolver.resolve(authentication).tenantId
        return MatchResponse(matchingService.match(tenantId, candidate, request.limit).map(JobMatchResponse::from))
    }
}
