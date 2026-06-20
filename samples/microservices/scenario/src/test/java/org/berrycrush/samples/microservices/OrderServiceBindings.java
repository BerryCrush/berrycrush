package org.berrycrush.samples.microservices;

import org.berrycrush.config.BerryCrushConfiguration;
import org.berrycrush.junit.binding.OpenApiSpecValue;
import org.berrycrush.junit.BerryCrushBindings;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bindings for Order Service scenario tests.
 */
@Component
@Lazy
public class OrderServiceBindings implements BerryCrushBindings {

    @LocalServerPort
    private int port;

    @Override
    public Map<String, Object> getBindings() {
        String host = "http://localhost:" + port;
        return Map.of(
            "default", new OpenApiSpecValue("order-service.yaml", host)
        );
    }

    @Override
    public void configure(BerryCrushConfiguration config) {
        config.setLogRequests(true);
        config.setLogResponses(true);
    }
}
