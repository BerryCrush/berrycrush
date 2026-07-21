package org.berrycrush.samples.microservices;

import org.berrycrush.junit.BerryCrushConfiguration;
import org.berrycrush.junit.BerryCrushScenarios;
import org.berrycrush.junit.BerryCrushSpec;
import org.berrycrush.spring.BerryCrushContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

import org.berrycrush.samples.microservices.inventory.InventoryServiceApplication;

/**
 * Integration test for Inventory Service API using BerryCrush scenarios.
 * 
 * Includes chaos testing scenarios for resilience testing with 503 error simulation.
 */
@Suite
@IncludeEngines("berrycrush")
@SpringBootTest(
    classes = {InventoryServiceApplication.class, InventoryServiceBindings.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@BerryCrushContextConfiguration
@BerryCrushScenarios(locations = {
    "scenarios/02-inventory-service.scenario",
    "scenarios/03-retry-resilience.scenario"
})
@BerryCrushConfiguration(
    bindings = InventoryServiceBindings.class, 
    plugins = {"report:text", "report:console:high-contrast"}
)
@BerryCrushSpec(paths = {"inventory-service.yaml"})
public class InventoryServiceScenarioTest {
}
