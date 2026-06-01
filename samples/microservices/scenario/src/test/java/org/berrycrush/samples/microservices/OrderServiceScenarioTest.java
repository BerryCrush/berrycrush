package org.berrycrush.samples.microservices;

import org.berrycrush.junit.BerryCrushConfiguration;
import org.berrycrush.junit.BerryCrushScenarios;
import org.berrycrush.junit.BerryCrushSpec;
import org.berrycrush.spring.BerryCrushContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import org.berrycrush.samples.microservices.order.OrderServiceApplication;

/**
 * Integration test for Order Service API using BerryCrush scenarios.
 */
@Suite
@IncludeEngines("berrycrush")
@SpringBootTest(
    classes = OrderServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.sql.init.mode=never"}
)
@Import(OrderServiceBindings.class)
@BerryCrushContextConfiguration
@BerryCrushScenarios(locations = {"scenarios/01-order-service.scenario"})
@BerryCrushConfiguration(
    bindings = OrderServiceBindings.class, 
    plugins = {"report:text", "report:console:high-contrast"}
)
@BerryCrushSpec(paths = {"order-service.yaml"})
public class OrderServiceScenarioTest {
}
