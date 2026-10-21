package com.pokedex.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.pokedex.support.AbstractIntegrationIT;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code GET /api/v1/auth/me} (UC-08, openapi {@code getCurrentUser}) and JWT validation over the wire.
 *
 * <p>Traceability: security.md §3.2 (me: anonymous 401, USER 200), §4.2 (HS256 with {@code JWT_SECRET}, claims
 * {@code iss aud sub roles iat exp jti}, fixed algorithm so {@code alg:none} and algorithm switching are rejected,
 * issuer/audience validated, 30 s clock skew, token read from the {@code access_token} cookie); testing-strategy.md
 * §5 Identity ("missing, expired, tampered or alg:none token → 401").
 *
 * <p>Forged tokens are built here with the JDK only. They copy the claims of a real token issued by the app (so
 * {@code iss}/{@code aud}/{@code sub}/{@code roles} are the app's own values) and change one thing. The key is
 * {@link AbstractIntegrationIT#TEST_JWT_SECRET} as raw UTF-8 bytes. A control test proves that a token signed this
 * way with valid claims is accepted, so every rejection below is caused by the one thing that was changed.
 */
@DisplayName("Identity - current user and JWT validation")
class CurrentUserIT extends AbstractIntegrationIT {

    static final String PASSWORD = "Bulbasaur-Vine-Garden-31";
    static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void should_return_current_user_when_access_token_cookie_is_present() {
        Browser browser = new Browser();
        String email = uniqueEmail("me");
        Response registered = browser.register(email, PASSWORD, "Me Myself");
        browser.login(email, PASSWORD).then().statusCode(200);

        Response me = browser.get(Browser.ME_PATH);

        me.then().statusCode(200).contentType(startsWith("application/json")).body("email", equalTo(email));
        assertThat(me.jsonPath().getMap("$")).isEqualTo(registered.jsonPath().getMap("$"));
    }

    @Test
    void should_return_401_problem_without_access_token_cookie() {
        new Browser()
                .get(Browser.ME_PATH)
                .then()
                .statusCode(401)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/unauthorized"));
    }

    @Test
    void should_accept_a_token_signed_with_the_secret_and_valid_claims() {
        // Control for the forged-token tests: same claims, fresh iat/exp/jti, signed by the test.
        Map<String, Object> claims = claimsOfRealToken();
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(600).getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        meWithToken(hs256(claims, TEST_JWT_SECRET)).then().statusCode(200);
    }

    @Test
    void should_reject_tampered_token() {
        Browser browser = loggedInBrowser();
        String token = browser.cookie("access_token");
        String[] parts = token.split("\\.");
        Map<String, Object> claims = decode(parts[1]);
        claims.put("roles", java.util.List.of("ADMIN"));
        String tampered = parts[0] + "." + encode(claims) + "." + parts[2]; // original signature kept

        assertUnauthorized(meWithToken(tampered));
    }

    @Test
    void should_reject_token_with_garbage_signature() {
        String token = loggedInBrowser().cookie("access_token");
        String broken = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA" + token.substring(token.length() - 4);

        assertUnauthorized(meWithToken(broken));
    }

    @Test
    void should_reject_expired_token() {
        Map<String, Object> claims = claimsOfRealToken();
        claims.put("iat", Instant.now().minusSeconds(7200).getEpochSecond());
        claims.put("exp", Instant.now().minusSeconds(600).getEpochSecond()); // well past the 30 s skew

        assertUnauthorized(meWithToken(hs256(claims, TEST_JWT_SECRET)));
    }

    @Test
    void should_reject_alg_none_token() {
        Map<String, Object> claims = claimsOfRealToken();
        claims.put("exp", Instant.now().plusSeconds(600).getEpochSecond());
        String header = encode(Map.of("alg", "none", "typ", "JWT"));

        assertUnauthorized(meWithToken(header + "." + encode(claims) + "."));
    }

    @Test
    void should_reject_token_signed_with_another_secret() {
        Map<String, Object> claims = claimsOfRealToken();
        claims.put("exp", Instant.now().plusSeconds(600).getEpochSecond());

        assertUnauthorized(meWithToken(hs256(claims, "another-secret-of-at-least-256-bits-0123456789-abcdefghij")));
    }

    @Test
    void should_reject_token_with_wrong_audience() {
        Map<String, Object> claims = claimsOfRealToken();
        claims.put("exp", Instant.now().plusSeconds(600).getEpochSecond());
        claims.put("aud", "someone-else");

        assertUnauthorized(meWithToken(hs256(claims, TEST_JWT_SECRET)));
    }

    @Test
    void should_reject_token_with_wrong_issuer() {
        Map<String, Object> claims = claimsOfRealToken();
        claims.put("exp", Instant.now().plusSeconds(600).getEpochSecond());
        claims.put("iss", "https://evil.example");

        assertUnauthorized(meWithToken(hs256(claims, TEST_JWT_SECRET)));
    }

    @Test
    void should_issue_token_with_the_documented_claims() {
        Map<String, Object> claims = claimsOfRealToken();

        assertThat(claims).containsKeys("iss", "aud", "sub", "roles", "iat", "exp", "jti");
        long lifetime = ((Number) claims.get("exp")).longValue() - ((Number) claims.get("iat")).longValue();
        assertThat(lifetime).as("60-minute token").isEqualTo(3600);
        assertThat(decode(loggedInBrowser().cookie("access_token").split("\\.")[0]))
                .containsEntry("alg", "HS256");
    }

    // ------------------------------------------------------------ helpers

    private Browser loggedInBrowser() {
        Browser browser = new Browser();
        browser.registerAndLogin(uniqueEmail("jwt"), PASSWORD);
        return browser;
    }

    /** Claims of a token the app itself issued to a fresh user. */
    private Map<String, Object> claimsOfRealToken() {
        return decode(loggedInBrowser().cookie("access_token").split("\\.")[1]);
    }

    private static Response meWithToken(String token) {
        Browser browser = new Browser();
        browser.setCookie("access_token", token);
        return browser.get(Browser.ME_PATH);
    }

    private static void assertUnauthorized(Response response) {
        response.then()
                .statusCode(401)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("/problems/unauthorized"));
    }

    static String hs256(Map<String, Object> claims, String secret) {
        String signingInput = encode(Map.of("alg", "HS256", "typ", "JWT")) + "." + encode(claims);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static String encode(Map<String, Object> json) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(JSON.writeValueAsString(json).getBytes(StandardCharsets.UTF_8));
    }

    static Map<String, Object> decode(String base64Url) {
        String json = new String(Base64.getUrlDecoder().decode(base64Url), StandardCharsets.UTF_8);
        return new LinkedHashMap<>(JSON.readValue(json, new TypeReference<Map<String, Object>>() {}));
    }
}
