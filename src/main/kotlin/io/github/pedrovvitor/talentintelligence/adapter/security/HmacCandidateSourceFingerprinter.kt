package io.github.pedrovvitor.talentintelligence.adapter.security

import io.github.pedrovvitor.talentintelligence.application.CandidateSourceFingerprinter
import io.github.pedrovvitor.talentintelligence.application.CandidateSourceFingerprint
import io.github.pedrovvitor.talentintelligence.domain.CandidateProfile
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class HmacCandidateSourceFingerprinter(
    @Value("\${app.audit.fingerprint-key}") fingerprintKey: String,
    @Value("\${app.audit.fingerprint-key-version}") private val fingerprintKeyVersion: String,
) : CandidateSourceFingerprinter {
    private val key = fingerprintKey.toByteArray(StandardCharsets.UTF_8).also { keyBytes ->
        require(keyBytes.size >= MINIMUM_KEY_BYTES) { "Source fingerprint key must contain at least 32 bytes" }
    }

    init {
        require(fingerprintKeyVersion.isNotBlank()) { "Source fingerprint key version is required" }
    }

    override fun fingerprint(tenantId: TenantId, candidate: CandidateProfile): CandidateSourceFingerprint {
        val canonicalSource = listOf(
            tenantId.value.toString(),
            candidate.headline.normalizedFingerprintValue(),
            candidate.summary.normalizedFingerprintValue(),
            candidate.skills.canonicalSet(),
            candidate.seniority.name,
            candidate.preferredWorkModes.map(Enum<*>::name).sorted().joinToString(","),
            candidate.preferredLocations.canonicalSet(),
            candidate.minimumSalary?.stripTrailingZeros()?.toPlainString().orEmpty(),
        ).joinToString("|") { value -> "${value.toByteArray(StandardCharsets.UTF_8).size}:$value" }
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return CandidateSourceFingerprint(
            value = HexFormat.of().formatHex(mac.doFinal(canonicalSource.toByteArray(StandardCharsets.UTF_8))),
            keyVersion = fingerprintKeyVersion,
        )
    }

    private fun Set<String>.canonicalSet(): String = map { value -> value.normalizedFingerprintValue() }
        .sorted()
        .joinToString(",") { value -> "${value.toByteArray(StandardCharsets.UTF_8).size}:$value" }

    private fun String.normalizedFingerprintValue(): String = Normalizer.normalize(trim(), Normalizer.Form.NFKC).lowercase()

    companion object {
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val MINIMUM_KEY_BYTES = 32
    }
}
