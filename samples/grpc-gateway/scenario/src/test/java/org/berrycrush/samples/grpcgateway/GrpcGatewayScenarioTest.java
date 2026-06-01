package org.berrycrush.samples.grpcgateway;

import org.berrycrush.junit.BerryCrushConfiguration;
import org.berrycrush.junit.BerryCrushScenarios;
import org.berrycrush.junit.BerryCrushSpec;
import org.berrycrush.spring.BerryCrushContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * Integration test for gRPC-Gateway style REST API using BerryCrush scenarios.
 * 
 * Demonstrates testing gRPC-style custom methods exposed as REST endpoints
 * using colon-prefixed action patterns (e.g., /messages:send).
 */
@Suite
@IncludeEngines("berrycrush")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@BerryCrushContextConfiguration
@BerryCrushScenarios(locations = {"scenarios/*.scenario"})
@BerryCrushConfiguration(
    bindings = GrpcGatewayBindings.class, 
    plugins = {"report:text", "report:console:high-contrast"}
)
@BerryCrushSpec(paths = {"messaging-service.yaml"})
public class GrpcGatewayScenarioTest {
}
