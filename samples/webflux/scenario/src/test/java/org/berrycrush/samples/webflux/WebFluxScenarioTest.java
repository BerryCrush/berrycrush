package org.berrycrush.samples.webflux;

import org.berrycrush.junit.BerryCrushConfiguration;
import org.berrycrush.junit.BerryCrushScenarios;
import org.berrycrush.junit.BerryCrushSpec;
import org.berrycrush.spring.BerryCrushContextConfiguration;
import org.berrycrush.spring.SpringStepDiscovery;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;


/**
 * Integration test for WebFlux Product API using BerryCrush scenarios.
 * 
 * Demonstrates testing reactive WebFlux endpoints with R2DBC.
 */
@Suite
@IncludeEngines("berrycrush")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SpringStepDiscovery.class)
@BerryCrushContextConfiguration
@BerryCrushScenarios(locations = {"scenarios/*.scenario"})
@BerryCrushConfiguration(
        bindings = WebFluxBindings.class,
        plugins = {"report:text", "report:console:high-contrast"},
        stepPackages = {"org.berrycrush.samples.webflux"}
)
@BerryCrushSpec(paths = {"webflux-products.yaml"})
public class WebFluxScenarioTest {
}
