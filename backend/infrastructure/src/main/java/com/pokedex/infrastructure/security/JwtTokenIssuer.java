package com.pokedex.infrastructure.security;

import com.pokedex.application.identity.port.out.TokenIssuer;
import com.pokedex.domain.identity.AccessToken;
import com.pokedex.domain.identity.User;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** HS256 JWT with claims {@code iss aud sub roles iat exp jti} (security.md §4.2). */
public final class JwtTokenIssuer implements TokenIssuer {

    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;

    public JwtTokenIssuer(SecretKey key, String issuer, String audience) {
        this.encoder = NimbusJwtEncoder.withSecretKey(Objects.requireNonNull(key, "key"))
                .algorithm(MacAlgorithm.HS256)
                .build();
        this.issuer = Objects.requireNonNull(issuer, "issuer");
        this.audience = Objects.requireNonNull(audience, "audience");
    }

    @Override
    public AccessToken issue(User user, Instant issuedAt, Instant expiresAt) {
        List<String> roles =
                user.role().impliedRoles().stream().map(Enum::name).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(user.id().value().toString())
                .claim(ROLES_CLAIM, roles)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}
