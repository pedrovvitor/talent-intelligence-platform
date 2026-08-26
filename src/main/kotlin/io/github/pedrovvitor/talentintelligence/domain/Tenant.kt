package io.github.pedrovvitor.talentintelligence.domain

import java.util.UUID

@JvmInline
value class TenantId(val value: UUID) {
    companion object {
        fun parse(value: String): TenantId = TenantId(UUID.fromString(value))
    }
}
