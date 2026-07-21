package org.berrycrush.samples.microservices;

import org.berrycrush.config.BerryCrushConfiguration;
import org.berrycrush.junit.binding.OpenApiSpecValue;
import org.berrycrush.junit.BerryCrushBindings;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bindings for Inventory Service scenario tests.
 * 
 * Includes both inventory API and chaos API for resilience testing.
 */
@Component
@Lazy
public class InventoryServiceBindings implements BerryCrushBindings {

    @LocalServerPort
    private int port;

    @Override
    public Map<String, Object> getBindings() {
        String host = "http://localhost:" + port;
        return Map.of(
            "default", new OpenApiSpecValue("inventory-service.yaml", host)
        );
    }

    @Override
    public void configure(BerryCrushConfiguration config) {
        config.setLogRequests(true);
        config.setLogResponses(true);
    }
}
