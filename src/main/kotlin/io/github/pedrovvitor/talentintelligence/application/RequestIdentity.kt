package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.TenantId

data class RequestIdentity(
    val actorId: String,
    val tenantId: TenantId,
)
