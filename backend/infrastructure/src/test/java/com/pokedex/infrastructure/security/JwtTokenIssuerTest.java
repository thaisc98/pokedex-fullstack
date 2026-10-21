package com.pokedex.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.identity.AccessToken;
import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.PasswordHash;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * {@code TokenIssuer} adapter and its decoder (security.md §4.2): HS256, claims {@code iss aud sub roles iat exp jti},
 * decoder with a fixed algorithm, issuer and audience validators and a 30 s clock skew.
 */
class JwtTokenIssuerTest {

    private static final SecretKey KEY = JwtSecretKey.fromSecret("test-only-jwt-secret-0123456789-abcdefghijklmnop");
    private static final SecretKey OTHER_KEY =
            JwtSecretKey.fromSecret("another-secret-of-at-least-256-bits-0123456789-abcdefghij");
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final Instant EXPIRES = NOW.plus(Duration.ofMinutes(60));

    private static final User ASH = user(Role.USER);

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(KEY, "pokedex-api", "pokedex-web");

    @Test
    void should_issue_a_token_with_the_documented_claims() {
        AccessToken token = issuer.issue(ASH, NOW, EXPIRES);

        Jwt jwt = decoderAt(NOW).decode(token.value());
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getIssuer()).hasToString("pokedex-api");
        assertThat(jwt.getAudience()).containsExactly("pokedex-web");
        assertThat(jwt.getSubject()).isEqualTo(ASH.id().value().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(EXPIRES);
        assertThat(jwt.getId()).isNotBlank();
        assertThat(token.expiresAt()).isEqualTo(EXPIRES);
    }

    @Test
    void should_put_every_implied_role_in_an_admin_token() {
        Jwt jwt = decoderAt(NOW).decode(issuer.issue(user(Role.ADMIN), NOW, EXPIRES).value());

        assertThat(jwt.getClaimAsStringList("roles")).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void should_give_every_token_its_own_id() {
        Jwt first = decoderAt(NOW).decode(issuer.issue(ASH, NOW, EXPIRES).value());
        Jwt second = decoderAt(NOW).decode(issuer.issue(ASH, NOW, EXPIRES).value());

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void should_accept_a_token_within_the_30_second_skew_after_expiry() {
        String token = issuer.issue(ASH, NOW, EXPIRES).value();

        assertThat(decoderAt(EXPIRES.plusSeconds(29)).decode(token).getSubject())
                .isEqualTo(ASH.id().value().toString());
    }

    @Test
    void should_reject_an_expired_token_beyond_the_skew() {
        String token = issuer.issue(ASH, NOW, EXPIRES).value();

        assertThatThrownBy(() -> decoderAt(EXPIRES.plusSeconds(31)).decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void should_reject_a_token_from_another_issuer_or_for_another_audience() {
        String otherIssuer = new JwtTokenIssuer(KEY, "https://evil.example", "pokedex-web")
                .issue(ASH, NOW, EXPIRES)
                .value();
        String otherAudience = new JwtTokenIssuer(KEY, "pokedex-api", "someone-else")
                .issue(ASH, NOW, EXPIRES)
                .value();

        assertThatThrownBy(() -> decoderAt(NOW).decode(otherIssuer)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoderAt(NOW).decode(otherAudience)).isInstanceOf(JwtException.class);
    }

    @Test
    void should_reject_a_token_signed_with_another_key() {
        String forged = new JwtTokenIssuer(OTHER_KEY, "pokedex-api", "pokedex-web")
                .issue(ASH, NOW, EXPIRES)
                .value();

        assertThatThrownBy(() -> decoderAt(NOW).decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void should_reject_an_unsigned_alg_none_token() {
        String signed = issuer.issue(ASH, NOW, EXPIRES).value();
        String payload = signed.split("\\.")[1];
        String header = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> decoderAt(NOW).decode(header + "." + payload + "."))
                .isInstanceOf(JwtException.class);
    }

    private static JwtDecoder decoderAt(Instant now) {
        return AccessTokenDecoderFactory.create(
                KEY, "pokedex-api", "pokedex-web", Duration.ofSeconds(30), Clock.fixed(now, ZoneOffset.UTC));
    }

    private static User user(Role role) {
        return new User(
                UserId.random(),
                new Email("ash@example.com"),
                new DisplayName("Ash"),
                new PasswordHash("{bcrypt}$2a$12$abcdefghijklmnopqrstuv"),
                role,
                NOW);
    }
}
