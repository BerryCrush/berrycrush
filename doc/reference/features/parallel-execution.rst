Parallel Execution
==================

BerryCrush supports JUnit 5 parallel execution. All components are thread-safe
and each scenario gets its own isolated ``ExecutionContext`` by default.

This allows you to significantly reduce test suite execution time by running
multiple scenarios concurrently.

Thread Safety Guarantees
------------------------

BerryCrush's internal components are designed for thread-safe parallel execution:

.. list-table::
   :header-rows: 1
   :widths: 30 15 55

   * - Component
     - Status
     - Notes
   * - ExecutionContext
     - Thread-safe
     - Uses ConcurrentHashMap + @Volatile
   * - BerryCrushScenarioExecutor
     - Stateless
     - No shared mutable state
   * - HTTP Client
     - Thread-safe
     - Java HttpClient is thread-safe
   * - Assertion Engine
     - Stateless
     - Immutable configuration

Enabling Parallel Execution
---------------------------

JUnit Platform Properties
^^^^^^^^^^^^^^^^^^^^^^^^^

Create ``src/test/resources/junit-platform.properties``:

.. code-block:: properties

   # Enable parallel execution
   junit.jupiter.execution.parallel.enabled=true
   
   # Default execution mode for tests
   junit.jupiter.execution.parallel.mode.default=concurrent
   
   # Default mode for classes
   junit.jupiter.execution.parallel.mode.classes.default=concurrent
   
   # Parallelism strategy: fixed, dynamic, or custom
   junit.jupiter.execution.parallel.config.strategy=fixed
   
   # Number of concurrent threads
   junit.jupiter.execution.parallel.config.fixed.parallelism=4

Annotation-Based Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Control parallel execution per test class using ``@BerryCrushConfiguration``:

.. code-block:: java

   // Default: scenarios can run in parallel (thread-safe)
   @BerryCrushConfiguration(parallelExecution = ParallelExecutionMode.CONCURRENT)
   public class ParallelPetApiTest { }
   
   // Force sequential execution for this test class
   @BerryCrushConfiguration(parallelExecution = ParallelExecutionMode.SAME_THREAD)
   public class SequentialPetApiTest { }

Kotlin example:

.. code-block:: kotlin

   @BerryCrushConfiguration(parallelExecution = ParallelExecutionMode.CONCURRENT)
   class ParallelPetApiTest

Parallel Execution Modes
------------------------

CONCURRENT (Default)
^^^^^^^^^^^^^^^^^^^^

Scenarios can run in parallel. This is the default mode and is safe when:

* Each scenario uses its own ``ExecutionContext``
* No shared mutable state between scenarios
* ``shareVariablesAcrossScenarios`` is false (the default)

SAME_THREAD
^^^^^^^^^^^

Force sequential execution within the test class. Use when:

* Scenarios share variables (``shareVariablesAcrossScenarios: true``)
* External resources need exclusive access
* Scenario order matters

Best Practices
--------------

1. **Use Isolated Contexts** (Default)
   
   Each scenario gets its own variables. This is the safest approach:

   .. code-block:: berrycrush

      # No shareVariablesAcrossScenarios - each scenario is isolated
      scenario: Create a pet
        when I create a pet
          call ^createPet
            body: {"name": "Fluffy"}
          extract $.id => petId

2. **Avoid Shared State**
   
   Don't rely on execution order between scenarios:

   .. code-block:: java

      // Good: Each test is independent
      @BerryCrushConfiguration(parallelExecution = ParallelExecutionMode.CONCURRENT)
      class IndependentTests { }

3. **Mark Sequential Tests Explicitly**
   
   When scenarios must run in order:

   .. code-block:: java

      // Scenarios share state and must run sequentially
      @BerryCrushConfiguration(parallelExecution = ParallelExecutionMode.SAME_THREAD)
      class DependentTests { }

4. **Use Isolated Copy for Parallel Features**
   
   The ``ExecutionContext.createIsolatedCopy()`` method creates a fully
   independent copy of the context for parallel scenarios:

   .. code-block:: kotlin

      val parallelContext = sharedContext.createIsolatedCopy()
      // parallelContext is completely independent

Example Configuration
---------------------

Complete parallel execution setup:

**junit-platform.properties:**

.. code-block:: properties

   junit.jupiter.execution.parallel.enabled=true
   junit.jupiter.execution.parallel.mode.default=concurrent
   junit.jupiter.execution.parallel.config.strategy=fixed
   junit.jupiter.execution.parallel.config.fixed.parallelism=4

**Test Class:**

.. code-block:: java

   @Suite
   @IncludeEngines("berrycrush")
   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
   @BerryCrushContextConfiguration
   @BerryCrushScenarios(locations = {"scenarios/*.scenario"})
   @BerryCrushConfiguration(
       bindings = PetstoreBindings.class,
       parallelExecution = ParallelExecutionMode.CONCURRENT
   )
   @BerryCrushSpec(paths = {"petstore.yaml"})
   public class ParallelPetstoreTest { }

Performance Considerations
--------------------------

* **Thread Pool Size**: Match parallelism to available CPU cores
* **Resource Contention**: Monitor for database or API rate limits
* **Memory Usage**: Each parallel scenario uses its own context
* **CI/CD**: Adjust parallelism based on CI runner capabilities

Troubleshooting
---------------

**Scenarios Interfering With Each Other**

If you see unexpected variable values or test failures:

1. Verify ``shareVariablesAcrossScenarios`` is not enabled
2. Use ``SAME_THREAD`` mode for dependent scenarios
3. Check for external shared state (database, files)

**Parallel Execution Not Working**

1. Verify ``junit-platform.properties`` is in ``src/test/resources``
2. Check ``junit.jupiter.execution.parallel.enabled=true``
3. Ensure BerryCrush is using the correct configuration

See Also
--------

* :doc:`parameters` - File-level parameter configuration
* :doc:`spring-boot` - Spring Boot integration
* :doc:`error-context` - Error context and debugging
