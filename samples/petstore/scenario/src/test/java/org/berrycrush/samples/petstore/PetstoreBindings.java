package org.berrycrush.samples.petstore;

import org.berrycrush.config.BerryCrushConfiguration;
import org.berrycrush.config.OpenApiSpecValue;
import org.berrycrush.junit.BerryCrushBindings;
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
 * When used with @BerryCrushContextConfiguration, this class is retrieved
 * from Spring's ApplicationContext, enabling @LocalServerPort injection.
 * <p> <p />
 * Note: @Lazy is required because @LocalServerPort is only available
 * after the web server has started, which happens after initial bean creation.
 * <p> <p />
 * Multi-spec support: This bindings class registers multiple OpenAPI specs
 * with different base URLs, demonstrating multi-host API testing:
 * - default (petstore.yaml): Pet API at /api/v1
 * - auth (auth.yaml): Auth API at /auth/api/v1
 * <p> <p />
 * In a real microservices environment, these would be separate hosts:
 * - default: <a href="http://petstore-service:8080">...</a>
 * - auth: <a href="http://auth-service:8081">...</a>
 */
@Component
@Lazy
public class PetstoreBindings implements BerryCrushBindings {

    @LocalServerPort
    private int port;

    /**
     * Provide bindings for multi-host API testing.
     * <p>
     * The petstore API is at /api/v1 while the auth API is at /auth/api/v1.
     * This demonstrates different base URLs for different specs within
     * the same test suite - simulating a microservices architecture.
     */
    @Override
    public Map<String, Object> getBindings() {
        String host = "http://localhost:" + port;
        return Map.of(
            "default", new OpenApiSpecValue("petstore.yaml", host + "/api/v1"),
            "auth", new OpenApiSpecValue("auth.yaml", host + "/auth/api/v1")
        );
    }

    @Override
    public void configure(BerryCrushConfiguration config) {
        // Don't set a global baseUrl - use per-spec base URLs instead
        // This enables true multi-host API testing
        config.setLogRequests(true);
        config.setLogResponses(true);
    }
}
