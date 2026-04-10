package io.github.ktakashi.samples.petstore;

import io.github.ktakashi.lemoncheck.junit.LemonCheckConfiguration;
import io.github.ktakashi.lemoncheck.junit.LemonCheckScenarios;
import io.github.ktakashi.lemoncheck.junit.LemonCheckTags;
import io.github.ktakashi.lemoncheck.spring.LemonCheckContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test demonstrating tag filtering with @ignore tag.
 * 
 * <p>This test class demonstrates how to filter scenarios using tags.
 * Scenarios tagged with {@code @ignore} will be skipped, while all
 * other scenarios will be executed.
 * 
 * <h2>Tag Filtering</h2>
 * <ul>
 *   <li>{@code @LemonCheckTags(exclude = {"ignore"})} - skips scenarios with @ignore tag</li>
 *   <li>{@code @LemonCheckTags(include = {"smoke"})} - only runs scenarios with @smoke tag</li>
 *   <li>Both can be combined for more precise filtering</li>
 * </ul>
 * 
 * <h2>Feature and Background Support</h2>
 * <p>The scenario file {@code 85-feature-and-tags.scenario} demonstrates:
 * <ul>
 *   <li>Feature blocks that group related scenarios</li>
 *   <li>Background steps that run before each scenario in a feature</li>
 *   <li>Tags on both features and individual scenarios</li>
 *   <li>Tag inheritance from features to scenarios</li>
 * </ul>
 *
 * @see LemonCheckTags
 */
@Suite
@IncludeEngines("lemoncheck")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@LemonCheckContextConfiguration
@LemonCheckScenarios(locations = {"scenarios/85-feature-and-tags.scenario"})
@LemonCheckConfiguration(
    bindings = PetstoreBindings.class,
    openApiSpec = "petstore.yaml"
)
@LemonCheckTags(exclude = {"ignore"})
public class TagFilteringTest {
    // Test class body is empty - scenarios are discovered and executed by the lemoncheck engine
}
