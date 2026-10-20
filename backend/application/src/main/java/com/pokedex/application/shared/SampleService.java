package com.pokedex.application.shared;

/**
 * Placeholder use case: a plain class wired as a {@code @Bean} in bootstrap, with no framework
 * annotations. Replaced by the real use cases in P2.
 */
public final class SampleService {

    private final SampleGateway gateway;

    public SampleService(SampleGateway gateway) {
        this.gateway = gateway;
    }

    public int doubled() {
        return gateway.load() * 2;
    }
}
