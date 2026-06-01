package org.berrycrush.samples.graphql;

import org.berrycrush.junit.BerryCrushConfiguration;
import org.berrycrush.junit.BerryCrushScenarios;
import org.berrycrush.junit.BerryCrushSpec;
import org.berrycrush.spring.BerryCrushContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * Integration test for GraphQL API using BerryCrush scenarios.
 * 
 * Demonstrates testing GraphQL queries and mutations via HTTP POST
 * using BerryCrush's OpenAPI testing capabilities.
 */
@Suite
@IncludeEngines("berrycrush")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@BerryCrushContextConfiguration
@BerryCrushScenarios(locations = {"scenarios/*.scenario"})
@BerryCrushConfiguration(
    bindings = GraphQLBindings.class, 
    plugins = {"report:text", "report:console:high-contrast"}
)
@BerryCrushSpec(paths = {"graphql-api.yaml"})
public class GraphQLScenarioTest {
}
