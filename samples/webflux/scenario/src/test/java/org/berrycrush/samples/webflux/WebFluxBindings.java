package org.berrycrush.samples.webflux;

import org.berrycrush.config.BerryCrushConfiguration;
import org.berrycrush.junit.binding.OpenApiSpecValue;
import org.berrycrush.junit.BerryCrushBindings;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bindings for WebFlux scenario tests.
 * 
 * Provides runtime configuration for scenario execution,
 * including the dynamically allocated port from Spring Boot's test server.
 */
@Component
@Lazy
public class WebFluxBindings implements BerryCrushBindings {

    @LocalServerPort
    private int port;

    @Override
    public Map<String, Object> getBindings() {
        String host = "http://localhost:" + port;
        return Map.of(
            "default", new OpenApiSpecValue("webflux-products.yaml", host)
        );
    }

    @Override
    public void configure(BerryCrushConfiguration config) {
        config.setLogRequests(true);
        config.setLogResponses(true);
    }
}
