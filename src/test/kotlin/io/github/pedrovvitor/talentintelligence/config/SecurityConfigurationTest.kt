package io.github.pedrovvitor.talentintelligence.config

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals

class SecurityConfigurationTest {
    @Test
    fun `converter maps only supported realm roles`() {
        val jwt = jwtWithRoles("recruiter", "admin", "offline_access")

        val authentication = SecurityConfiguration().keycloakAuthoritiesConverter().convert(jwt)

        assertEquals(setOf("ROLE_RECRUITER", "ROLE_ADMIN"), authentication.authorities.map { it.authority }.toSet())
        assertEquals("recruiter.synthetic", authentication.name)
    }

    private fun jwtWithRoles(vararg roles: String): Jwt = Jwt.withTokenValue("synthetic-token")
        .header("alg", "none")
        .subject("synthetic-user-id")
        .claim("preferred_username", "recruiter.synthetic")
        .claim("realm_access", mapOf("roles" to roles.toList()))
        .issuedAt(Instant.parse("2026-08-25T12:00:00Z"))
        .expiresAt(Instant.parse("2026-08-25T12:05:00Z"))
        .build()
}
