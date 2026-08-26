package io.github.pedrovvitor.talentintelligence.adapter.web

import io.github.pedrovvitor.talentintelligence.domain.TenantId
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class TenantIdentityPolicy(
    @Value("\${app.identity.candidate-marketplace-tenant-id}") candidateMarketplaceTenantId: String,
) {
    private val candidateMarketplaceTenantId = TenantId.parse(candidateMarketplaceTenantId)

    fun resolve(jwt: Jwt, authorities: Collection<String>): TenantId {
        val tenantClaim = jwt.getClaimAsString(RequestIdentityResolver.TENANT_CLAIM)
        if (tenantClaim != null) {
            return try {
                TenantId.parse(tenantClaim)
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException("Tenant claim is invalid", exception)
            }
        }
        if (authorities.isCandidateOnly()) {
            return candidateMarketplaceTenantId
        }
        throw IllegalArgumentException("Tenant claim is required")
    }

    private fun Collection<String>.isCandidateOnly(): Boolean =
        contains(CANDIDATE_AUTHORITY) && none(PRIVILEGED_AUTHORITIES::contains)

    companion object {
        const val CANDIDATE_AUTHORITY = "ROLE_CANDIDATE"
        private val PRIVILEGED_AUTHORITIES = setOf("ROLE_RECRUITER", "ROLE_ADMIN")
    }
}
