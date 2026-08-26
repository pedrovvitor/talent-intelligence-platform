package io.github.pedrovvitor.talentintelligence.config

import jakarta.servlet.http.HttpServletResponse
import io.github.pedrovvitor.talentintelligence.adapter.web.RequestIdentityResolver
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import java.nio.charset.StandardCharsets

@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authoritiesConverter: Converter<Jwt, AbstractAuthenticationToken>,
    ): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole(PlatformRole.ADMIN.authoritySuffix)
                    .requestMatchers("/api/jobs/**", "/api/matches/**")
                    .hasAnyRole(PlatformRole.RECRUITER.authoritySuffix, PlatformRole.ADMIN.authoritySuffix)
                    .requestMatchers("/actuator/**").hasRole(PlatformRole.ADMIN.authoritySuffix)
                    .anyRequest().denyAll()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer
                    .jwt { jwt -> jwt.jwtAuthenticationConverter(authoritiesConverter) }
                    .authenticationEntryPoint { _, response, _ ->
                        writeSecurityError(response, 401, "AUTHENTICATION_REQUIRED", "Authentication is required")
                    }
                    .accessDeniedHandler { _, response, _ ->
                        writeSecurityError(response, 403, "ACCESS_DENIED", "Required capability is missing")
                    }
            }
        return http.build()
    }

    @Bean
    fun keycloakAuthoritiesConverter(): Converter<Jwt, AbstractAuthenticationToken> = Converter { jwt ->
        requireTenantId(jwt)
        val authorities = realmRoles(jwt)
            .map(PlatformRole::fromClaim)
            .filterNotNull()
            .map { role -> SimpleGrantedAuthority("ROLE_${role.authoritySuffix}") }
        JwtAuthenticationToken(jwt, authorities, principalName(jwt))
    }

    private fun realmRoles(jwt: Jwt): List<String> {
        val realmAccess = jwt.getClaimAsMap("realm_access") ?: return emptyList()
        val roles = realmAccess["roles"] as? Collection<*> ?: return emptyList()
        return roles.filterIsInstance<String>()
    }

    private fun principalName(jwt: Jwt): String =
        jwt.getClaimAsString("preferred_username")
            ?: jwt.subject?.takeIf(String::isNotBlank)
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_token", "Subject claim is required", null))

    private fun requireTenantId(jwt: Jwt) {
        val tenantClaim = jwt.getClaimAsString(RequestIdentityResolver.TENANT_CLAIM)
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_token", "Tenant claim is required", null))
        try {
            TenantId.parse(tenantClaim)
        } catch (exception: IllegalArgumentException) {
            throw OAuth2AuthenticationException(
                OAuth2Error("invalid_token", "Tenant claim is invalid", null),
                exception,
            )
        }
    }

    private fun writeSecurityError(
        response: HttpServletResponse,
        status: Int,
        code: String,
        message: String,
    ) {
        response.status = status
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("""{"code":"$code","message":"$message"}""")
    }
}

private enum class PlatformRole(
    val claim: String,
    val authoritySuffix: String,
) {
    RECRUITER("recruiter", "RECRUITER"),
    ADMIN("admin", "ADMIN"),
    ;

    companion object {
        fun fromClaim(claim: String): PlatformRole? = entries.firstOrNull { role -> role.claim == claim }
    }
}
