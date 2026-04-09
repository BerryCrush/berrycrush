package io.github.ktakashi.samples.petstore;

import io.github.ktakashi.lemoncheck.config.Configuration;
import io.github.ktakashi.lemoncheck.junit.LemonCheckBindings;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bindings for petstore scenario tests.
 * <p> <p />
 * This class provides runtime configuration for scenario execution,
 * including the dynamically allocated port from Spring Boot's test server.
 * <p> <p />
 * When used with @LemonCheckContextConfiguration, this class is retrieved
 * from Spring's ApplicationContext, enabling @LocalServerPort injection.
 * <p> <p />
 * Note: @Lazy is required because @LocalServerPort is only available
 * after the web server has started, which happens after initial bean creation.
 * <p> <p />
 * Multi-spec support: This bindings class registers multiple OpenAPI specs:
 * - default (petstore.yaml): Pet-related operations (listPets, createPet, getPet)
 * - auth (auth.yaml): Authentication operations (login, logout)
 */
@Component
@Lazy
public class PetstoreBindings implements LemonCheckBindings {

    @LocalServerPort
    private int port;

    @Override
    public Map<String, Object> getBindings() {
        return Map.of(
            "baseUrl", "http://localhost:" + port + "/api/v1"
        );
    }

    @Override
    public String getOpenApiSpec() {
        return "petstore.yaml";
    }

    @Override
    public Map<String, String> getAdditionalSpecs() {
        return Map.of("auth", "auth.yaml");
    }

    @Override
    public void configure(Configuration config) {
        config.setBaseUrl("http://localhost:" + port + "/api/v1");
    }
}
