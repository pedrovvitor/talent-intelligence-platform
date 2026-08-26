package io.github.pedrovvitor.talentintelligence.config

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SecurityConfigurationTest {
    @Test
    fun `converter maps only supported realm roles`() {
        val jwt = jwtWithRoles(VALID_TENANT_ID, "recruiter", "admin", "offline_access")

        val authentication = SecurityConfiguration().keycloakAuthoritiesConverter().convert(jwt)

        assertEquals(setOf("ROLE_RECRUITER", "ROLE_ADMIN"), authentication.authorities.map { it.authority }.toSet())
        assertEquals("recruiter.synthetic", authentication.name)
    }

    @Test
    fun `converter fails closed when tenant claim is missing`() {
        val jwt = jwtWithRoles(null, "recruiter")

        assertFailsWith<OAuth2AuthenticationException> {
            SecurityConfiguration().keycloakAuthoritiesConverter().convert(jwt)
        }
    }

    @Test
    fun `converter fails closed when tenant claim is malformed`() {
        val jwt = jwtWithRoles("not-a-tenant-id", "recruiter")

        assertFailsWith<OAuth2AuthenticationException> {
            SecurityConfiguration().keycloakAuthoritiesConverter().convert(jwt)
        }
    }

    private fun jwtWithRoles(tenantId: String?, vararg roles: String): Jwt {
        val builder = Jwt.withTokenValue("synthetic-token")
            .header("alg", "none")
            .subject("synthetic-user-id")
            .claim("preferred_username", "recruiter.synthetic")
            .claim("realm_access", mapOf("roles" to roles.toList()))
            .issuedAt(Instant.parse("2026-08-25T12:00:00Z"))
            .expiresAt(Instant.parse("2026-08-25T12:05:00Z"))
        tenantId?.let { builder.claim("tenant_id", it) }
        return builder.build()
    }

    companion object {
        private const val VALID_TENANT_ID = "00000000-0000-0000-0000-000000000001"
    }
}
