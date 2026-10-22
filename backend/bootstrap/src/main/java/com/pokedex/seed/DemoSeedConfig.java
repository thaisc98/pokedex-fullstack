package com.pokedex.seed;

import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.application.identity.port.out.CommonPasswordList;
import com.pokedex.application.identity.port.out.PasswordHasher;
import com.pokedex.application.identity.port.out.UserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Demo data, {@code demo} profile only. Seeding never stops the application from starting. */
@Configuration(proxyBeanMethods = false)
@Profile("demo")
public class DemoSeedConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DemoSeedConfig.class);

    @Bean
    ApplicationRunner demoDataSeeder(
            UserRepository users,
            PasswordHasher hasher,
            CommonPasswordList commonPasswords,
            Clock clock,
            Environment environment,
            CollectedPokemonRepository collection,
            SyncPokemonUseCase sync,
            UpdateCollectedPokemonUseCase update) {
        DemoAccountSeeder accounts =
                new DemoAccountSeeder(users, hasher, commonPasswords, clock, environment::getProperty);
        DemoCollectionSeeder pokemon = new DemoCollectionSeeder(collection, sync, update, DemoCollectionSeeder.GEN_ONE);
        return args -> {
            if (!DemoAccountSeeder.isConfigured(environment::getProperty)) {
                LOG.info("Demo seed: no DEMO_* variables set; nothing seeded");
                return;
            }
            runSafely("accounts", accounts::seed);
            runSafely("collection", pokemon::seed);
        };
    }

    private static void runSafely(String part, Runnable seed) {
        try {
            seed.run();
        } catch (RuntimeException e) {
            // The exception type only: messages could carry input values.
            LOG.warn(
                    "Demo seed: the {} step failed ({}); continuing without it",
                    part,
                    e.getClass().getSimpleName());
        }
    }
}
