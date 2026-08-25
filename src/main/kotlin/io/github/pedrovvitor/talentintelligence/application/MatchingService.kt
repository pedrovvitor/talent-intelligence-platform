package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.CandidateProfile
import io.github.pedrovvitor.talentintelligence.domain.EligibilityPolicy
import io.github.pedrovvitor.talentintelligence.domain.JobMatch
import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.MatchEvidence
import io.github.pedrovvitor.talentintelligence.domain.normalized
import org.springframework.stereotype.Service
import kotlin.math.round

@Service
class MatchingService(
    private val jobCatalog: JobCatalog,
    private val semanticJobIndex: SemanticJobIndex,
    private val embeddingGateway: EmbeddingGateway,
    private val eligibilityPolicy: EligibilityPolicy,
) {
    fun match(candidate: CandidateProfile, limit: Int): List<JobMatch> {
        val safeLimit = limit.coerceIn(1, MAX_RESULTS)
        val queryEmbedding = embeddingGateway.embed(buildCandidateQuery(candidate))
        return semanticJobIndex.search(queryEmbedding, RETRIEVAL_WINDOW)
            .mapNotNull { semanticCandidate ->
                val job = jobCatalog.findById(semanticCandidate.jobId) ?: return@mapNotNull null
                if (!eligibilityPolicy.evaluate(candidate, job).eligible) {
                    return@mapNotNull null
                }
                toMatch(candidate, job, semanticCandidate.score)
            }
            .sortedByDescending(JobMatch::finalScore)
            .take(safeLimit)
    }

    private fun buildCandidateQuery(candidate: CandidateProfile): String = buildString {
        appendLine(candidate.headline)
        appendLine(candidate.summary)
        append(candidate.skills.joinToString(" "))
    }

    private fun toMatch(candidate: CandidateProfile, job: JobPosting, semanticScore: Double): JobMatch {
        val candidateSkills = candidate.skills.associateBy(String::normalized)
        val matchedSkills = job.requiredSkills.filter { it.normalized() in candidateSkills }
        val skillCoverage = if (job.requiredSkills.isEmpty()) 1.0 else matchedSkills.size.toDouble() / job.requiredSkills.size
        val normalizedSemanticScore = semanticScore.coerceIn(0.0, 1.0)
        val finalScore = normalizedSemanticScore * SEMANTIC_WEIGHT + skillCoverage * SKILL_WEIGHT
        val evidence = buildList {
            add(MatchEvidence("semantic", "Semantic relevance", percentage(normalizedSemanticScore)))
            add(MatchEvidence("skills", "Required skill coverage", percentage(skillCoverage)))
            if (matchedSkills.isNotEmpty()) {
                add(MatchEvidence("matched-skills", "Matched skills", matchedSkills.sorted().joinToString(", ")))
            }
            add(MatchEvidence("eligibility", "Hard constraints", "Passed"))
        }
        return JobMatch(
            jobId = job.id,
            title = job.title,
            company = job.company,
            semanticScore = normalizedSemanticScore.rounded(),
            skillCoverage = skillCoverage.rounded(),
            finalScore = finalScore.rounded(),
            evidence = evidence,
        )
    }

    private fun percentage(value: Double): String = "${round(value * 100).toInt()}%"

    private fun Double.rounded(): Double = round(this * 10_000) / 10_000

    companion object {
        private const val MAX_RESULTS = 20
        private const val RETRIEVAL_WINDOW = 100
        private const val SEMANTIC_WEIGHT = 0.7
        private const val SKILL_WEIGHT = 0.3
    }
}
