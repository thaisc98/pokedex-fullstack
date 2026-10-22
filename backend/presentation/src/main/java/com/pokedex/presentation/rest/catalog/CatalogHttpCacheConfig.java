package com.pokedex.presentation.rest.catalog;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * ETag for the public catalog GETs only (ADR 0004, security.md §4.7), so a revalidation with {@code If-None-Match}
 * gets a {@code 304}. {@link ShallowEtagHeaderFilter} buffers the response body to hash it; catalog responses are
 * small JSON documents built in memory (a page is at most 100 summaries, the 1 MB cap applies to what we read from
 * PokeAPI, not to what we write), so buffering costs little and nothing is streamed. The filter only tags 2xx
 * GET/HEAD responses, so catalog errors (no-store) never get an ETag.
 */
@Configuration(proxyBeanMethods = false)
public class CatalogHttpCacheConfig {

    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> catalogEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/v1/pokemon", "/api/v1/pokemon/*");
        registration.setName("catalogEtagFilter");
        return registration;
    }
}
