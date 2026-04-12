Troubleshooting
===============

This guide covers common issues and their solutions.

Test Discovery Issues
---------------------

Tests Not Found
^^^^^^^^^^^^^^^

**Symptom**: No tests are discovered by the JUnit runner.

**Common Causes**:

1. Missing ``@IncludeEngines("lemoncheck")`` annotation
2. Incorrect scenario file paths
3. Scenario files not on the classpath

**Solution**:

Ensure your test class has the correct annotations:

.. code-block:: kotlin

    @IncludeEngines("lemoncheck")
    @LemonCheckScenarios(locations = "scenarios/pets.scenario")
    class PetApiTest

Verify scenario files are in ``src/test/resources/``.

Scenario Files Not Loaded
^^^^^^^^^^^^^^^^^^^^^^^^^

**Symptom**: Test class is discovered but scenarios show as "0 tests".

**Solution**:

1. Check the file extension is ``.scenario``
2. Verify the path in ``locations`` is relative to the resources folder
3. Ensure the file contains valid scenario syntax

.. code-block:: kotlin

    // Correct
    @LemonCheckScenarios(locations = "scenarios/pets.scenario")
    
    // Also correct - multiple files
    @LemonCheckScenarios(locations = [
        "scenarios/pets.scenario",
        "scenarios/users.scenario"
    ])

Configuration Issues
--------------------

Bindings Not Found
^^^^^^^^^^^^^^^^^^

**Symptom**: ``IllegalStateException: Cannot instantiate bindings class``

**Solution**:

Ensure your bindings class has a public no-arg constructor:

.. code-block:: kotlin

    // ✓ Correct
    class MyBindings : LemonCheckBindings {
        override fun getBindings() = mapOf("key" to "value")
    }

    // ✗ Wrong - requires constructor argument
    class MyBindings(private val config: Config) : LemonCheckBindings {
        override fun getBindings() = mapOf("key" to config.value)
    }

For dependency injection, use the Spring module:

.. code-block:: kotlin

    @Component
    class MyBindings(
        @Autowired private val config: Config
    ) : LemonCheckBindings

OpenAPI Spec Not Found
^^^^^^^^^^^^^^^^^^^^^^

**Symptom**: ``FileNotFoundException`` when loading OpenAPI spec

**Solution**:

Ensure the spec file is on the classpath:

.. code-block:: text

    src/test/resources/
    └── petstore.yaml

And reference it correctly:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        openApiSpec = "petstore.yaml"  // Relative to resources root
    )

HTTP Request Issues
-------------------

Connection Refused
^^^^^^^^^^^^^^^^^^

**Symptom**: ``ConnectException: Connection refused``

**Solution**:

1. Ensure the API server is running
2. Verify the baseUrl in bindings is correct
3. Check for firewall/network issues

.. code-block:: kotlin

    class MyBindings : LemonCheckBindings {
        override fun getBindings() = mapOf(
            "baseUrl" to "http://localhost:8080"  // Must match server port
        )
    }

Timeout Errors
^^^^^^^^^^^^^^

**Symptom**: Requests timeout

**Solution**:

Increase timeout in configuration:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        timeout = 60_000L  // 60 seconds
    )

SSL/TLS Errors
^^^^^^^^^^^^^^

**Symptom**: ``SSLHandshakeException`` or certificate errors

**Solution**:

For development/testing with self-signed certificates:

.. code-block:: kotlin

    class MyBindings : LemonCheckBindings {
        override fun configure(config: Configuration) {
            config.sslValidation = false  // Disable for testing only
        }
    }

.. warning::

    Never disable SSL validation in production.

Assertion Failures
------------------

JSONPath Not Matching
^^^^^^^^^^^^^^^^^^^^^

**Symptom**: JSONPath assertions fail unexpectedly

**Debug Steps**:

1. Print the actual response body
2. Verify the JSONPath expression
3. Check data types (string vs number)

.. code-block:: text

    scenario: Debug response
      when: I request pets
        call ^listPets
      then: I check the response
        assert status 200
        assert $.name equals "Fluffy"

Schema Validation Fails
^^^^^^^^^^^^^^^^^^^^^^^

**Symptom**: ``SchemaValidationException``

**Solution**:

1. Compare the actual response against the OpenAPI schema
2. Check for optional vs required fields
3. Verify data types match

.. code-block:: kotlin

    // Get detailed validation errors
    @LemonCheckConfiguration(
        pluginClasses = [SchemaValidationPlugin::class]
    )

Spring Integration Issues
-------------------------

Bindings Not Injected
^^^^^^^^^^^^^^^^^^^^^

**Symptom**: Spring dependencies are null

**Solution**:

Ensure you're using the Spring module and annotations:

.. code-block:: kotlin

    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @IncludeEngines("lemoncheck")
    @LemonCheckScenarios(locations = "scenarios/pets.scenario")
    class PetApiTest  // Make sure Spring context is initialized

Context Not Available
^^^^^^^^^^^^^^^^^^^^^

**Symptom**: ``NoSuchBeanDefinitionException``

**Solution**:

Check that the Spring module is included:

.. code-block:: kotlin

    dependencies {
        testImplementation("io.github.ktakashi.lemoncheck:spring:0.1.0")
    }

Plugin Issues
-------------

Plugin Not Executing
^^^^^^^^^^^^^^^^^^^^

**Symptom**: Plugin callbacks are not called

**Causes**:

1. Plugin not registered
2. Exception during plugin initialization
3. Priority conflicts

**Solution**:

Add logging to debug:

.. code-block:: kotlin

    class DebugPlugin : LemonCheckPlugin {
        override val name = "debug"
        override val priority = 1000  // High priority, runs first

        override fun onScenarioStart(context: ScenarioContext) {
            println("Debug: Starting ${context.name}")
        }
    }

Report Not Generated
^^^^^^^^^^^^^^^^^^^^

**Symptom**: Report file is empty or missing

**Solution**:

1. Check the output path is writable
2. Verify the plugin is registered
3. Ensure tests actually ran

.. code-block:: kotlin

    @LemonCheckConfiguration(
        plugins = ["report:json:build/reports/test.json"]
    )

Getting Help
------------

If your issue isn't listed here:

1. Check the GitHub Issues for similar problems
2. Enable debug logging: ``-Dlemoncheck.debug=true``
3. Create a minimal reproducing example
4. Open a new issue with:
   - LemonCheck version
   - Kotlin/Java version
   - Error message and stack trace
   - Sample code to reproduce
