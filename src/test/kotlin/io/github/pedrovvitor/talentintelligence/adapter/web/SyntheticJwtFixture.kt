package io.github.pedrovvitor.talentintelligence.adapter.web

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant

@TestConfiguration
class SyntheticJwtConfiguration {
    @Bean
    fun syntheticJwtDecoder(): JwtDecoder = SyntheticJwtFixture.decoder()
}

internal object SyntheticJwtFixture {
    const val ISSUER = "https://identity.example.test/realms/talent-intelligence"
    const val AUDIENCE = "talent-intelligence-api"
    private val trustedKeyPair = generateKeyPair()
    private val untrustedKeyPair = generateKeyPair()

    fun validToken(
        tenantId: String,
        roles: List<String>,
        expiresAt: Instant = Instant.now().plusSeconds(300),
    ): String = encode(trustedKeyPair, tenantId, roles, expiresAt)

    fun invalidSignatureToken(tenantId: String, roles: List<String>): String =
        encode(untrustedKeyPair, tenantId, roles, Instant.now().plusSeconds(300))

    fun decoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(trustedKeyPair.public as RSAPublicKey)
        .build()
        .also { decoder -> decoder.setJwtValidator(validators()) }

    private fun encode(
        keyPair: KeyPair,
        tenantId: String,
        roles: List<String>,
        expiresAt: Instant,
    ): String {
        val rsaKey = RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private as RSAPrivateKey)
            .keyID("synthetic-test-key")
            .build()
        val encoder = NimbusJwtEncoder(ImmutableJWKSet(JWKSet(rsaKey)))
        val claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject("synthetic-runtime-user")
            .audience(listOf(AUDIENCE))
            .issuedAt(expiresAt.minusSeconds(300))
            .expiresAt(expiresAt)
            .claim("preferred_username", "runtime.synthetic")
            .claim("tenant_id", tenantId)
            .claim("realm_access", mapOf("roles" to roles))
            .build()
        val header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("synthetic-test-key").build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    private fun validators(): OAuth2TokenValidator<Jwt> {
        val issuerValidator = JwtValidators.createDefaultWithIssuer(ISSUER)
        val audienceValidator = JwtClaimValidator<List<String>>("aud") { audience -> AUDIENCE in audience }
        return DelegatingOAuth2TokenValidator(issuerValidator, audienceValidator)
    }

    private fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()
}
