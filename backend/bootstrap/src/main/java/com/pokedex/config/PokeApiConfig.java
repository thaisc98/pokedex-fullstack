package com.pokedex.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.infrastructure.cache.CachingPokemonCatalog;
import com.pokedex.infrastructure.pokeapi.PokeApiCatalogAdapter;
import com.pokedex.infrastructure.pokeapi.PokeApiGateway;
import com.pokedex.infrastructure.pokeapi.PokeApiHttpClient;
import com.pokedex.infrastructure.pokeapi.PokeApiMapper;
import com.pokedex.infrastructure.pokeapi.ResponseSizeLimitInterceptor;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * PokeAPI wiring (architecture.md §1.5, ADR 0004):
 *
 * <ul>
 *   <li>{@link PokeApiHttpClient} is an HTTP service client in group {@code pokeapi}. Its base URL and
 *       timeouts come from {@code spring.http.serviceclient.pokeapi.*}, which {@code application.yml} binds
 *       to {@code pokeapi.base-url} (2 s connect, 5 s read, redirects not followed).
 *   <li>{@link PokeApiGateway} is a bean so {@code @EnableResilientMethods} can apply its {@code @Retryable}
 *       and {@code @ConcurrencyLimit}.
 *   <li>The port bean is the cache decorator around the adapter; the adapter itself is not a bean.
 *   <li>The page cache lives in a Caffeine {@link CacheManager} with a size cap and a TTL.
 *   <li>Every PokeAPI response is size-capped by {@link ResponseSizeLimitInterceptor}.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
@ImportHttpServices(group = PokeApiConfig.POKEAPI_GROUP, types = PokeApiHttpClient.class)
public class PokeApiConfig {

    static final String POKEAPI_GROUP = "pokeapi";

    /** security.md §4.5: every PokeAPI response is capped at {@code pokeapi.max-response-bytes} (1 MB). */
    @Bean
    RestClientHttpServiceGroupConfigurer pokeApiResponseSizeLimit(
            @Value("${pokeapi.max-response-bytes:1048576}") long maxResponseBytes) {
        ResponseSizeLimitInterceptor limit = new ResponseSizeLimitInterceptor(maxResponseBytes);
        return groups ->
                groups.filterByName(POKEAPI_GROUP).forEachClient((group, builder) -> builder.requestInterceptor(limit));
    }

    @Bean
    PokeApiGateway pokeApiGateway(PokeApiHttpClient pokeApiHttpClient) {
        return new PokeApiGateway(pokeApiHttpClient);
    }

    @Bean
    CacheManager cacheManager(
            @Value("${pokeapi.cache.max-pages:500}") long maxPages, @Value("${pokeapi.cache.ttl:24h}") Duration ttl) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(maxPages).expireAfterWrite(ttl));
        manager.setCacheNames(List.of(CachingPokemonCatalog.PAGES_CACHE));
        manager.setAllowNullValues(false);
        return manager;
    }

    @Bean
    PokemonCatalogPort pokemonCatalog(PokeApiGateway pokeApiGateway, CacheManager cacheManager) {
        PokeApiCatalogAdapter adapter = new PokeApiCatalogAdapter(pokeApiGateway, new PokeApiMapper());
        return new CachingPokemonCatalog(
                adapter, Objects.requireNonNull(cacheManager.getCache(CachingPokemonCatalog.PAGES_CACHE)));
    }
}
