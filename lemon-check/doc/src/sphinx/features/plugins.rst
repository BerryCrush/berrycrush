Plugins
=======

LemonCheck provides a plugin system to extend its functionality. Plugins can hook into
scenario and step lifecycle events to perform custom actions like logging, reporting,
or modifying behavior.

Plugin Architecture
-------------------

All plugins implement the ``LemonCheckPlugin`` interface:

.. code-block:: kotlin

    interface LemonCheckPlugin {
        val name: String
        val priority: Int get() = 0

        fun onScenarioStart(context: ScenarioContext) {}
        fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {}
        fun onStepStart(context: StepContext) {}
        fun onStepEnd(context: StepContext, result: StepResult) {}
    }

Lifecycle Events
^^^^^^^^^^^^^^^^

Plugins receive callbacks at specific points during test execution:

* ``onScenarioStart``: Called before each scenario begins
* ``onScenarioEnd``: Called after each scenario completes (with results)
* ``onStepStart``: Called before each step executes
* ``onStepEnd``: Called after each step completes (with results)

Priority
^^^^^^^^

Plugins are executed in priority order (higher priority first). The default priority is 0.
Use priority to ensure proper ordering when plugins depend on each other.

Creating a Custom Plugin
------------------------

Basic Plugin
^^^^^^^^^^^^

.. code-block:: kotlin

    class LoggingPlugin : LemonCheckPlugin {
        override val name = "logging"

        override fun onScenarioStart(context: ScenarioContext) {
            println("Starting scenario: ${context.name}")
        }

        override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
            println("Scenario ${context.name}: ${result.status}")
        }

        override fun onStepStart(context: StepContext) {
            println("  Step: ${context.description}")
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
        override val priority = 100  // Run first

        private val metrics = mutableMapOf<String, Long>()

        override fun onScenarioStart(context: ScenarioContext) {
            context.variables["_startTime"] = System.currentTimeMillis()
        }

        override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
            val startTime = context.variables["_startTime"] as Long
            metrics[context.name] = System.currentTimeMillis() - startTime
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
