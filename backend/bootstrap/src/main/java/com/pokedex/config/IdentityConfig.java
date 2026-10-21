package com.pokedex.config;

import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.application.identity.port.out.CommonPasswordList;
import com.pokedex.application.identity.port.out.PasswordHasher;
import com.pokedex.application.identity.port.out.TokenIssuer;
import com.pokedex.application.identity.port.out.UserRepository;
import com.pokedex.application.identity.service.AuthenticateUserService;
import com.pokedex.application.identity.service.GetCurrentUserService;
import com.pokedex.application.identity.service.RegisterUserService;
import com.pokedex.infrastructure.persistence.JpaUserRepositoryAdapter;
import com.pokedex.infrastructure.persistence.SpringDataUserRepository;
import com.pokedex.infrastructure.security.AccessTokenDecoderFactory;
import com.pokedex.infrastructure.security.BCryptPasswordHasher;
import com.pokedex.infrastructure.security.ClasspathCommonPasswordList;
import com.pokedex.infrastructure.security.JwtSecretKey;
import com.pokedex.infrastructure.security.JwtTokenIssuer;
import com.pokedex.presentation.security.AccessTokenCookies;
import com.pokedex.presentation.security.ratelimit.AuthRateLimits;
import com.pokedex.presentation.security.ratelimit.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Identity wiring (UC-06/07/08, security.md §4.1–4.2): plain use cases with their adapters, the HS256 key from
 * {@code JWT_SECRET} (the app refuses to start without a 256-bit secret), the cookie decoder, and the rate limits.
 */
@Configuration(proxyBeanMethods = false)
public class IdentityConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SecretKey jwtSigningKey(@Value("${pokedex.security.jwt.secret:}") String secret) {
        return JwtSecretKey.fromSecret(secret);
    }

    @Bean
    TokenIssuer tokenIssuer(
            SecretKey jwtSigningKey,
            @Value("${pokedex.security.jwt.issuer}") String issuer,
            @Value("${pokedex.security.jwt.audience}") String audience) {
        return new JwtTokenIssuer(jwtSigningKey, issuer, audience);
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtSigningKey,
            @Value("${pokedex.security.jwt.issuer}") String issuer,
            @Value("${pokedex.security.jwt.audience}") String audience,
            @Value("${pokedex.security.jwt.clock-skew:30s}") Duration clockSkew,
            Clock clock) {
        return AccessTokenDecoderFactory.create(jwtSigningKey, issuer, audience, clockSkew, clock);
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    CommonPasswordList commonPasswordList() {
        return ClasspathCommonPasswordList.load();
    }

    @Bean
    UserRepository userRepository(SpringDataUserRepository springDataUserRepository) {
        return new JpaUserRepositoryAdapter(springDataUserRepository);
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(
            UserRepository users, PasswordHasher hasher, CommonPasswordList commonPasswords, Clock clock) {
        return new RegisterUserService(users, hasher, commonPasswords, clock);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository users,
            PasswordHasher hasher,
            TokenIssuer tokens,
            Clock clock,
            @Value("${pokedex.security.jwt.lifetime:60m}") Duration lifetime) {
        return new AuthenticateUserService(users, hasher, tokens, clock, lifetime);
    }

    @Bean
    GetCurrentUserUseCase getCurrentUserUseCase(UserRepository users) {
        return new GetCurrentUserService(users);
    }

    @Bean
    AccessTokenCookies accessTokenCookies(@Value("${pokedex.security.jwt.lifetime:60m}") Duration lifetime) {
        return new AccessTokenCookies(lifetime);
    }

    @Bean
    AuthRateLimits authRateLimits(
            CacheManager cacheManager,
            @Value("${pokedex.security.rate-limit.login-per-minute:5}") long loginsPerMinute,
            @Value("${pokedex.security.rate-limit.registrations-per-minute:3}") long registrationsPerMinute) {
        var buckets = Objects.requireNonNull(cacheManager.getCache(AuthRateLimits.CACHE), AuthRateLimits.CACHE);
        return new AuthRateLimits(
                new RateLimiter(buckets, "login", loginsPerMinute, Duration.ofMinutes(1)),
                new RateLimiter(buckets, "register", registrationsPerMinute, Duration.ofMinutes(1)));
    }
}
