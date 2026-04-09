package io.github.ktakashi.samples.petstore;

import io.github.ktakashi.lemoncheck.plugin.LemonCheckPlugin;
import io.github.ktakashi.lemoncheck.plugin.ResultStatus;
import io.github.ktakashi.lemoncheck.plugin.ScenarioContext;
import io.github.ktakashi.lemoncheck.plugin.ScenarioResult;
import io.github.ktakashi.lemoncheck.plugin.StepContext;
import io.github.ktakashi.lemoncheck.plugin.StepResult;

import java.time.Duration;
import java.time.Instant;

/**
 * Sample plugin demonstrating LemonCheck plugin system.
 *
 * <p>This plugin logs scenario and step lifecycle events to stdout.
 * It shows how to:
 * <ul>
 *   <li>Implement the LemonCheckPlugin interface</li>
 *   <li>Use lifecycle hooks (onScenarioStart, onScenarioEnd, onStepStart, onStepEnd)</li>
 *   <li>Access context information (scenario name, step description, etc.)</li>
 *   <li>Track timing information</li>
 * </ul>
 */
public class SampleLoggingPlugin implements LemonCheckPlugin {
    private Instant scenarioStartTime = null;

    @Override
    public String getId() {
        return "sample:logging";
    }

    @Override
    public String getName() {
        return "Sample Logging Plugin";
    }

    @Override
    public int getPriority() {
        return -10; // Execute early to capture all events
    }

    @Override
    public void onScenarioStart(ScenarioContext context) {
        scenarioStartTime = Instant.now();
        System.out.println("\n╔══════════════════════════════════════════════════════════════");
        System.out.println("║ [" + getName() + "] Scenario Starting: " + context.getScenarioName());
        String tags = context.getTags().isEmpty()
            ? "none"
            : String.join(", ", context.getTags());
        System.out.println("║ Tags: " + tags);
        System.out.println("╚══════════════════════════════════════════════════════════════\n");
    }

    @Override
    public void onScenarioEnd(ScenarioContext context, ScenarioResult result) {
        long duration = scenarioStartTime != null
            ? Duration.between(scenarioStartTime, Instant.now()).toMillis()
            : 0;
        boolean passed = result.getStatus() == ResultStatus.PASSED;
        String statusText = passed ? "✅ PASSED" : "❌ FAILED";

        System.out.println("\n╔══════════════════════════════════════════════════════════════");
        System.out.println("║ [" + getName() + "] Scenario Completed: " + context.getScenarioName());
        System.out.println("║ Status: " + statusText);
        System.out.println("║ Duration: " + duration + "ms");
        if (!passed && result.getError() != null) {
            System.out.println("║ Error: " + result.getError().getMessage());
        }
        System.out.println("╚══════════════════════════════════════════════════════════════\n");
    }

    @Override
    public void onStepStart(StepContext context) {
        System.out.println("  ▶ [" + getName() + "] Step Starting: " + context.getStepDescription());
    }

    @Override
    public void onStepEnd(StepContext context, StepResult result) {
        boolean passed = result.getStatus() == ResultStatus.PASSED;
        String statusSymbol = passed ? "✓" : "✗";
        long duration = result.getDuration().toMillis();
        System.out.println("  " + statusSymbol + " [" + getName()
            + "] Step Completed: " + context.getStepDescription()
            + " (" + duration + "ms)");

        if (!passed && result.getFailure() != null) {
            System.out.println("    └─ Failure: " + result.getFailure().getMessage());
        }
    }
}
