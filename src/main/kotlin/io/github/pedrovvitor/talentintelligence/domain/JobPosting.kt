package io.github.pedrovvitor.talentintelligence.domain

import java.math.BigDecimal
import java.util.UUID

enum class Seniority(val level: Int) {
    JUNIOR(1),
    MID(2),
    SENIOR(3),
    STAFF(4),
    PRINCIPAL(5),
}

enum class WorkMode {
    REMOTE,
    HYBRID,
    ONSITE,
}

data class JobPosting(
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
)
