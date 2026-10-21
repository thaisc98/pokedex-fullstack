package com.pokedex.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decoder for our access tokens (security.md §4.2): HS256 only (rejects {@code alg:none} and algorithm switching),
 * issuer and audience validators, {@code exp} required, and a fixed clock skew.
 */
public final class AccessTokenDecoderFactory {

    private AccessTokenDecoderFactory() {}

    public static JwtDecoder create(SecretKey key, String issuer, String audience, Duration clockSkew, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(clockSkew);
        timestamps.setClock(clock);
        timestamps.setAllowEmptyExpiryClaim(false);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps, new JwtIssuerValidator(issuer), new JwtAudienceValidator(audience)));
        return decoder;
    }
}
