package io.github.ktakashi.samples.petstore;

import io.github.ktakashi.lemoncheck.config.Configuration;
import io.github.ktakashi.lemoncheck.junit.LemonCheckBindings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bindings for petstore scenario tests.
 * 
 * This class provides runtime configuration for scenario execution,
 * including the dynamically allocated port from Spring Boot's test server.
 */
@Component
public class PetstoreBindings implements LemonCheckBindings {

    private int port;

    /**
     * Default constructor for JUnit engine instantiation.
     */
    public PetstoreBindings() {
        // Port will be set via setPort or defaults to 8080
        this.port = 8080;
    }

    /**
     * Sets the port for the test server.
     */
    public void setPort(int port) {
        this.port = port;
    }

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
    public void configure(Configuration config) {
        config.setBaseUrl("http://localhost:" + port + "/api/v1");
    }
}
