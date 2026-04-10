package io.github.ktakashi.samples.petstore;

import io.github.ktakashi.lemoncheck.junit.LemonCheckConfiguration;
import io.github.ktakashi.lemoncheck.junit.LemonCheckScenarios;
import io.github.ktakashi.lemoncheck.junit.LemonCheckTags;
import io.github.ktakashi.lemoncheck.spring.LemonCheckContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test demonstrating combined include/exclude tag filtering.
 * 
 * <p>This test class demonstrates tag precedence when both include and exclude
 * are specified. Exclude takes precedence over include.
 * 
 * <h2>Test Case</h2>
 * <ul>
 *   <li>{@code include = {"api"}} - Only run scenarios with @api tag</li>
 *   <li>{@code exclude = {"ignore"}} - Skip scenarios with @ignore tag</li>
 * </ul>
 * 
 * <h2>Expected Behavior</h2>
 * <p>Given a scenario with both @api and @ignore tags:
 * <ul>
 *   <li>The @api tag matches the include filter</li>
 *   <li>BUT the @ignore tag matches the exclude filter</li>
 *   <li>Therefore, the scenario should be EXCLUDED (exclude wins)</li>
 * </ul>
 * 
 * <h2>Scenarios from 85-feature-and-tags.scenario</h2>
 * <ul>
 *   <li>Feature scenarios (inherit @api from feature) - INCLUDED</li>
 *   <li>"@ignore scenario" - EXCLUDED (has @ignore)</li>
 *   <li>"@api @ignore scenario" - EXCLUDED (exclude wins even with @api)</li>
 *   <li>"@standalone scenario" - EXCLUDED (no @api tag)</li>
 *   <li>"@api scenario" - INCLUDED (has @api, no @ignore)</li>
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
@LemonCheckTags(include = {"api"}, exclude = {"ignore"})
public class CombinedTagFilteringTest {
    // Test class body is empty - scenarios are discovered and executed by the lemoncheck engine
}
