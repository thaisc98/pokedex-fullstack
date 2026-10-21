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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    /** Exists-check and insert in one transaction (the unique index still guards concurrent registrations). */
    @Bean
    RegisterUserUseCase registerUserUseCase(
            UserRepository users,
            PasswordHasher hasher,
            CommonPasswordList commonPasswords,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        return TransactionalUseCase.wrap(
                RegisterUserUseCase.class,
                new RegisterUserService(users, hasher, commonPasswords, clock),
                new TransactionTemplate(transactionManager));
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository users,
            PasswordHasher hasher,
            TokenIssuer tokens,
            Clock clock,
            @Value("${pokedex.security.jwt.lifetime:60m}") Duration lifetime,
            PlatformTransactionManager transactionManager) {
        return TransactionalUseCase.wrap(
                AuthenticateUserUseCase.class,
                new AuthenticateUserService(users, hasher, tokens, clock, lifetime),
                readOnly(transactionManager));
    }

    @Bean
    GetCurrentUserUseCase getCurrentUserUseCase(UserRepository users, PlatformTransactionManager transactionManager) {
        return TransactionalUseCase.wrap(
                GetCurrentUserUseCase.class, new GetCurrentUserService(users), readOnly(transactionManager));
    }

    /** security.md §4.8: Flyway migrates as FLYWAY_USER when it is set; the app always connects as DB_USER. */
    @Bean
    FlywayConfigurationCustomizer flywayOwnerCredentials(
            @Value("${FLYWAY_USER:}") String user,
            @Value("${FLYWAY_PASSWORD:}") String password,
            ObjectProvider<JdbcConnectionDetails> connectionDetails) {
        return new FlywayOwnerCredentials(
                user, password, () -> connectionDetails.getObject().getJdbcUrl());
    }

    private static TransactionTemplate readOnly(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        return template;
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
