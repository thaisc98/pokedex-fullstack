package com.pokedex.config;

import com.pokedex.application.collection.port.in.DeleteCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.GetCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.ListCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.application.collection.port.out.CollectedPokemonRepository;
import com.pokedex.application.collection.port.out.UpstreamSnapshotPort;
import com.pokedex.application.collection.service.DeleteCollectedPokemonService;
import com.pokedex.application.collection.service.GetCollectedPokemonService;
import com.pokedex.application.collection.service.ListCollectedPokemonService;
import com.pokedex.application.collection.service.SyncPokemonService;
import com.pokedex.application.collection.service.UpdateCollectedPokemonService;
import com.pokedex.infrastructure.cache.CachingPokeApiResources;
import com.pokedex.infrastructure.cache.CachingPokemonCatalog;
import com.pokedex.infrastructure.cache.CatalogCacheRefresher;
import com.pokedex.infrastructure.persistence.CollectedPokemonEntityMapper;
import com.pokedex.infrastructure.persistence.JpaCollectedPokemonRepositoryAdapter;
import com.pokedex.infrastructure.persistence.SpringDataCollectedPokemonRepository;
import com.pokedex.infrastructure.pokeapi.PokeApiGateway;
import com.pokedex.infrastructure.pokeapi.PokeApiMapper;
import com.pokedex.infrastructure.pokeapi.PokeApiSnapshotAdapter;
import java.time.Clock;
import java.util.Objects;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Collection wiring (US03, US04, UC-05). The snapshot port is the raw, resilient {@link PokeApiGateway}, never the
 * caching catalog, so a sync always copies current upstream data. After a successful live fetch, the fresh
 * resources replace the cached ones and the page cache is cleared (approved), so the catalog shows what was synced.
 * The shared {@link Clock} bean is defined once, elsewhere in bootstrap.
 */
@Configuration(proxyBeanMethods = false)
public class CollectionConfig {

    @Bean
    UpstreamSnapshotPort upstreamSnapshotPort(PokeApiGateway pokeApiGateway, CacheManager cacheManager) {
        CatalogCacheRefresher refresher = new CatalogCacheRefresher(
                cache(cacheManager, CachingPokeApiResources.POKEMON_CACHE),
                cache(cacheManager, CachingPokeApiResources.SPECIES_CACHE),
                cache(cacheManager, CachingPokemonCatalog.PAGES_CACHE));
        return new PokeApiSnapshotAdapter(pokeApiGateway, new PokeApiMapper(), refresher);
    }

    private static Cache cache(CacheManager cacheManager, String name) {
        return Objects.requireNonNull(cacheManager.getCache(name), name);
    }

    @Bean
    CollectedPokemonRepository collectedPokemonRepository(
            SpringDataCollectedPokemonRepository rows, PlatformTransactionManager transactionManager) {
        return new JpaCollectedPokemonRepositoryAdapter(
                rows, new CollectedPokemonEntityMapper(), new TransactionTemplate(transactionManager));
    }

    @Bean
    SyncPokemonUseCase syncPokemonUseCase(
            UpstreamSnapshotPort upstream, CollectedPokemonRepository repository, Clock clock) {
        return new SyncPokemonService(upstream, repository, clock);
    }

    @Bean
    ListCollectedPokemonUseCase listCollectedPokemonUseCase(CollectedPokemonRepository repository) {
        return new ListCollectedPokemonService(repository);
    }

    @Bean
    GetCollectedPokemonUseCase getCollectedPokemonUseCase(CollectedPokemonRepository repository) {
        return new GetCollectedPokemonService(repository);
    }

    @Bean
    UpdateCollectedPokemonUseCase updateCollectedPokemonUseCase(CollectedPokemonRepository repository, Clock clock) {
        return new UpdateCollectedPokemonService(repository, clock);
    }

    @Bean
    DeleteCollectedPokemonUseCase deleteCollectedPokemonUseCase(CollectedPokemonRepository repository) {
        return new DeleteCollectedPokemonService(repository);
    }
}
