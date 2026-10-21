package com.pokedex.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * security.md §4.8: migrations run as the schema owner ({@code FLYWAY_USER}), the app as {@code DB_USER}. Only when
 * both owner variables are set does Flyway switch credentials, on the same JDBC URL as the app; otherwise it keeps
 * the datasource (which also keeps Testcontainers' {@code @ServiceConnection} working).
 */
class FlywayOwnerCredentialsTest {

    private static final String URL = "jdbc:postgresql://db.example:5432/pokedex";

    @Test
    void should_connect_as_the_owner_on_the_app_url_when_both_variables_are_set() {
        FluentConfiguration configuration = new FluentConfiguration();

        new FlywayOwnerCredentials("schema_owner", "owner-credential", () -> URL).customize(configuration);

        assertThat(configuration.getUrl()).isEqualTo(URL);
        assertThat(configuration.getUser()).isEqualTo("schema_owner");
        assertThat(configuration.getPassword()).isEqualTo("owner-credential");
    }

    @ParameterizedTest(name = "user=''{0}'' password=''{1}'' -> datasource credentials")
    @CsvSource(
            value = {"NULL, NULL", "schema_owner, NULL", "NULL, owner-credential", "'', ''", "'  ', x"},
            nullValues = "NULL")
    void should_keep_the_datasource_unless_both_variables_are_set(String user, String password) {
        FluentConfiguration configuration = new FluentConfiguration();

        new FlywayOwnerCredentials(user, password, () -> {
                    throw new AssertionError("the URL must not be resolved");
                })
                .customize(configuration);

        assertThat(configuration.getUser()).isNull();
        assertThat(configuration.getDataSource()).isNull();
    }
}
