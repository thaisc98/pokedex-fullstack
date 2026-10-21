package com.pokedex.config;

import java.util.function.Supplier;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;

/**
 * security.md §4.8: migrations run as the schema owner ({@code FLYWAY_USER} / {@code FLYWAY_PASSWORD}) while the app
 * connects as the least-privilege {@code DB_USER}. Only when both owner variables are set does Flyway connect with
 * them, to the app's own JDBC URL; otherwise it keeps the datasource credentials (local runs, Testcontainers).
 */
final class FlywayOwnerCredentials implements FlywayConfigurationCustomizer {

    private final @Nullable String user;
    private final @Nullable String password;
    private final Supplier<String> jdbcUrl;

    FlywayOwnerCredentials(@Nullable String user, @Nullable String password, Supplier<String> jdbcUrl) {
        this.user = user;
        this.password = password;
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public void customize(FluentConfiguration configuration) {
        if (user != null && !user.isBlank() && password != null && !password.isBlank()) {
            configuration.dataSource(jdbcUrl.get(), user, password);
        }
    }
}
