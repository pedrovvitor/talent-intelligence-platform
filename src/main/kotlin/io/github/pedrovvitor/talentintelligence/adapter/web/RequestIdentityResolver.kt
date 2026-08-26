package io.github.pedrovvitor.talentintelligence.adapter.web

import io.github.pedrovvitor.talentintelligence.application.RequestIdentity
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class RequestIdentityResolver {
    fun resolve(authentication: JwtAuthenticationToken): RequestIdentity {
        val tenantClaim = authentication.token.getClaimAsString(TENANT_CLAIM)
            ?: throw AccessDeniedException("Tenant identity is required")
        val tenantId = try {
            TenantId.parse(tenantClaim)
        } catch (exception: IllegalArgumentException) {
            throw AccessDeniedException("Tenant identity is invalid", exception)
        }
        return RequestIdentity(
            actorId = authentication.token.subject ?: throw AccessDeniedException("Actor identity is required"),
            tenantId = tenantId,
        )
    }

    companion object {
        const val TENANT_CLAIM = "tenant_id"
    }
}
