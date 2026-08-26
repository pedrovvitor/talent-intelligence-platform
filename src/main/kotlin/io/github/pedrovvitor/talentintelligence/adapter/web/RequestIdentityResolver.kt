package io.github.pedrovvitor.talentintelligence.adapter.web

import io.github.pedrovvitor.talentintelligence.application.RequestIdentity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class RequestIdentityResolver(
    private val tenantIdentityPolicy: TenantIdentityPolicy,
) {
    fun resolve(authentication: JwtAuthenticationToken): RequestIdentity {
        val tenantId = try {
            tenantIdentityPolicy.resolve(
                authentication.token,
                authentication.authorities.mapNotNull { authority -> authority.authority },
            )
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
