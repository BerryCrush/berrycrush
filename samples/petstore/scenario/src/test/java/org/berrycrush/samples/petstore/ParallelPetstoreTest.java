package org.berrycrush.samples.petstore;

import org.berrycrush.junit.BerryCrushConfiguration;
import org.berrycrush.junit.BerryCrushScenarios;
import org.berrycrush.junit.BerryCrushSpec;
import org.berrycrush.junit.BerryCrushTags;
import org.berrycrush.junit.ParallelExecutionMode;
import org.berrycrush.spring.BerryCrushContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Demonstrates BerryCrush parallel execution capabilities.
 *
 * <p>This test class runs scenarios in parallel using JUnit 5's parallel
 * execution support. Each scenario gets its own isolated ExecutionContext,
 * allowing safe concurrent execution.
 *
 * <h2>Parallel Execution Setup</h2>
 * <ol>
 *   <li>Set {@code parallelExecution = ParallelExecutionMode.CONCURRENT} in configuration</li>
 *   <li>Create {@code junit-platform.properties} with parallel execution settings</li>
 *   <li>Ensure scenarios don't share mutable state</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>BerryCrush guarantees thread safety through:
 * <ul>
 *   <li>Isolated ExecutionContext per scenario (default behavior)</li>
 *   <li>Stateless executor components</li>
 *   <li>Thread-safe HTTP client (Java HttpClient)</li>
 *   <li>ConcurrentHashMap for variable storage</li>
 * </ul>
 *
 * <h2>Performance</h2>
 * <p>Parallel execution can significantly reduce test suite execution time.
 * The parallelism level is configured in {@code junit-platform.properties}.
 *
 * @see ParallelExecutionMode
 * @see PetstoreBindings
 */
@Suite
@IncludeEngines("berrycrush")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@BerryCrushContextConfiguration
@BerryCrushScenarios(
    locations = {"scenarios/parallel/*.scenario"},
    fragments = {"fragments/*.fragment"}
)
@BerryCrushConfiguration(
    bindings = PetstoreBindings.class,
    parallelExecution = ParallelExecutionMode.CONCURRENT,
    stepPackages = {"org.berrycrush.samples.petstore"},
    assertionPackages = {"org.berrycrush.samples.petstore.assertions"}
)
@BerryCrushSpec(paths = {"petstore.yaml"})
@BerryCrushTags(exclude = {"ignore"})
public class ParallelPetstoreTest {
}
