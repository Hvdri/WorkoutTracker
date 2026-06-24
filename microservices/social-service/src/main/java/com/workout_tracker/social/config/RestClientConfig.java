package com.workout_tracker.social.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${app.main-app.uri}")
    private String mainAppUri;

    // Single RestClient instance pre-configured with the monolith's base URL. Used by
    // MainAppClient to call /internal/ projections. Spring Boot 4 favors RestClient over
    // the older RestTemplate — same pattern as awbd2026/product-hub/product-api-app's
    // DiscountClient. Not using Feign because the prof's reference doesn't use Feign
    // either; she goes straight to RestClient.
    //
    // We use the static RestClient.builder() factory rather than injecting
    // RestClient.Builder: in Spring Boot 4 the auto-configured Builder bean isn't always
    // surfaced for non-actuator slices, and we don't need the auto-config customizations
    // (logging, observation, etc.) for an /internal/ projection call.
    @Bean
    public RestClient mainAppRestClient() {
        return RestClient.builder().baseUrl(mainAppUri).build();
    }
}
