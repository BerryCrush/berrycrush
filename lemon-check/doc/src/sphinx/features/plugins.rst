Plugins
=======

LemonCheck provides a plugin system to extend its functionality. Plugins can hook into
test execution, scenario, and step lifecycle events to perform custom actions like logging, 
reporting, or modifying behavior.

Plugin Architecture
-------------------

All plugins implement the ``LemonCheckPlugin`` interface:

.. code-block:: kotlin

    interface LemonCheckPlugin {
        // Identity
        val id: String get() = this::class.qualifiedName ?: "unknown"
        val name: String get() = this::class.simpleName ?: "Unknown Plugin"
        val priority: Int get() = 0

        // Test execution lifecycle (called once per test run)
        fun onTestExecutionStart() {}
        fun onTestExecutionEnd() {}

        // Scenario lifecycle
        fun onScenarioStart(context: ScenarioContext) {}
        fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {}

        // Step lifecycle
        fun onStepStart(context: StepContext) {}
        fun onStepEnd(context: StepContext, result: StepResult) {}
    }

Lifecycle Events
^^^^^^^^^^^^^^^^

Plugins receive callbacks at specific points during test execution:

**Test Execution Level:**

* ``onTestExecutionStart()``: Called once before the first scenario starts
* ``onTestExecutionEnd()``: Called once after all scenarios complete

**Scenario Level:**

* ``onScenarioStart(context)``: Called before each scenario begins
* ``onScenarioEnd(context, result)``: Called after each scenario completes (with results)

**Step Level:**

* ``onStepStart(context)``: Called before each step executes
* ``onStepEnd(context, result)``: Called after each step completes (with results)

Context Objects
^^^^^^^^^^^^^^^

**ScenarioContext** provides access to scenario execution state:

.. code-block:: kotlin

    interface ScenarioContext {
        val scenarioName: String           // Name from scenario file
        val scenarioFile: Path             // Path to scenario file
        val variables: MutableMap<String, Any>  // Runtime variables
        val metadata: Map<String, String>  // Scenario metadata
        val startTime: Instant             // Execution start time
        val tags: Set<String>              // Scenario tags for filtering
    }

**StepContext** provides access to step execution state:

.. code-block:: kotlin

    interface StepContext {
        val stepDescription: String        // Full step description
        val stepType: StepType             // CALL, ASSERT, EXTRACT, CUSTOM
        val stepIndex: Int                 // Zero-based index in scenario
        val scenarioContext: ScenarioContext  // Parent scenario
        val request: HttpRequest?          // Request details (CALL steps)
        val response: HttpResponse?        // Response details (after call)
        val operationId: String?           // OpenAPI operation ID
    }

Result Objects
^^^^^^^^^^^^^^

**ScenarioResult** contains the outcome of a scenario:

.. code-block:: kotlin

    interface ScenarioResult {
        val status: ResultStatus           // PASSED, FAILED, SKIPPED, ERROR
        val duration: Duration             // Total execution time
        val failedStep: Int                // First failed step index (-1 if none)
        val error: Throwable?              // Exception if ERROR status
        val stepResults: List<StepResult>  // All step results
    }

**StepResult** contains the outcome of a step:

.. code-block:: kotlin

    interface StepResult {
        val status: ResultStatus           // PASSED, FAILED, SKIPPED, ERROR
        val duration: Duration             // Execution time
        val failure: AssertionFailure?     // Failure details if FAILED
        val error: Throwable?              // Exception if ERROR
    }

Priority
^^^^^^^^

Plugins execute in priority order (lower values execute first):

* Negative priorities (e.g., -100): Setup/infrastructure plugins
* Zero (default): Standard plugins (reporting, logging)
* Positive priorities (e.g., 100): Cleanup/finalization plugins

Plugins with the same priority execute in registration order.

Creating a Custom Plugin
------------------------

Basic Plugin
^^^^^^^^^^^^

.. code-block:: kotlin

    class LoggingPlugin : LemonCheckPlugin {
        override val name = "logging"

        override fun onScenarioStart(context: ScenarioContext) {
            println("Starting scenario: ${context.scenarioName}")
        }

        override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
            println("Scenario ${context.scenarioName}: ${result.status}")
        }

        override fun onStepStart(context: StepContext) {
            println("  Step: ${context.stepDescription}")
        }

        override fun onStepEnd(context: StepContext, result: StepResult) {
            println("  Result: ${result.status}")
        }
    }

Stateful Plugin
^^^^^^^^^^^^^^^

Plugins can maintain state to collect data across scenarios:

.. code-block:: kotlin

    class MetricsPlugin : LemonCheckPlugin {
        override val name = "metrics"
        override val priority = -100  // Run early (lower = earlier)

        private val metrics = mutableMapOf<String, Long>()

        override fun onScenarioStart(context: ScenarioContext) {
            context.variables["_startTime"] = System.currentTimeMillis()
        }

        override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
            val startTime = context.variables["_startTime"] as Long
            metrics[context.scenarioName] = System.currentTimeMillis() - startTime
        }

        fun getMetrics(): Map<String, Long> = metrics.toMap()
    }

Registering Plugins
-------------------

Via Annotation
^^^^^^^^^^^^^^

The recommended way to register plugins is via the ``@LemonCheckConfiguration`` annotation:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        pluginClasses = [LoggingPlugin::class, MetricsPlugin::class]
    )
    class MyApiTest

By Name
^^^^^^^

Some built-in plugins can be registered by name:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        plugins = [
            "report:json:output.json",
            "report:junit:test-results.xml"
        ]
    )

Programmatic Registration
^^^^^^^^^^^^^^^^^^^^^^^^^

For dynamic plugin configuration:

.. code-block:: kotlin

    class MyBindings : LemonCheckBindings {
        override fun getPlugins(): List<LemonCheckPlugin> {
            return listOf(
                LoggingPlugin(),
                JsonReportPlugin("lemon-check-report.json")
            )
        }
    }

Built-in Plugins
----------------

Report Plugins
^^^^^^^^^^^^^^

LemonCheck includes several report plugins:

* ``TextReportPlugin`` - Human-readable console output
* ``JsonReportPlugin`` - Machine-parseable JSON format
* ``XmlReportPlugin`` - Generic XML structure
* ``JunitReportPlugin`` - JUnit XML for CI/CD integration

See :doc:`reporting` for detailed configuration.

Best Practices
--------------

1. **Keep plugins focused**: Each plugin should do one thing well
2. **Use priority wisely**: Set explicit priorities when order matters
3. **Handle exceptions**: Plugins should not crash test execution
4. **Be thread-safe**: Plugins may be called from multiple threads
5. **Clean up resources**: Use ``onScenarioEnd`` to release resources

Example: Retry Plugin
---------------------

Here's a more complex example - a plugin that retries failed steps:

.. code-block:: kotlin

    class RetryPlugin(
        private val maxRetries: Int = 3,
        private val retryDelay: Duration = Duration.ofSeconds(1)
    ) : LemonCheckPlugin {
        override val name = "retry"
        override val priority = -100  // Run last

        private val retryCount = ThreadLocal.withInitial { 0 }

        override fun onStepEnd(context: StepContext, result: StepResult) {
            if (result.status == ResultStatus.FAILED && retryCount.get() < maxRetries) {
                retryCount.set(retryCount.get() + 1)
                Thread.sleep(retryDelay.toMillis())
                throw RetryStepException()
            }
            retryCount.set(0)
        }
    }
