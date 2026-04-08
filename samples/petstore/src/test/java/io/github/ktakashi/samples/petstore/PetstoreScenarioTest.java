package io.github.ktakashi.samples.petstore;

import io.github.ktakashi.lemoncheck.junit.LemonCheckConfiguration;
import io.github.ktakashi.lemoncheck.junit.LemonCheckScenarios;
import io.github.ktakashi.lemoncheck.junit.LemonCheckSpec;
import org.junit.jupiter.api.Disabled;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Integration test for petstore API using lemon-check scenarios.
 * 
 * This test class demonstrates how to integrate lemon-check scenarios
 * with a Spring Boot test context. The scenarios are executed against
 * the running Spring Boot application with H2 database.
 * 
 * NOTE: This test is currently disabled because the LemonCheck JUnit engine
 * instantiates the test class independently, which means Spring's @LocalServerPort
 * injection doesn't work. Full Spring Boot integration requires additional
 * lifecycle callbacks or a custom JUnit extension approach.
 * 
 * For working examples, see PetstoreKotlinScenarios which uses the programmatic DSL.
 */
@Disabled("Requires Spring context integration - see PetstoreKotlinScenarios for working examples")
@Suite
@IncludeEngines("lemoncheck")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@LemonCheckScenarios(locations = {"scenarios/*.scenario"})
@LemonCheckConfiguration(bindings = PetstoreBindings.class, openApiSpec = "petstore.yaml")
@LemonCheckSpec(paths = {"petstore.yaml"})
public class PetstoreScenarioTest {
    
    @LocalServerPort
    private int port;
}
