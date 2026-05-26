Troubleshooting
===============

This guide covers common issues and their solutions.

Test Discovery Issues
---------------------

Tests Not Found
^^^^^^^^^^^^^^^

**Symptom**: No tests are discovered by the JUnit runner.

**Common Causes**:

1. Missing ``@IncludeEngines("berrycrush")`` annotation
2. Incorrect scenario file paths
3. Scenario files not on the classpath

**Solution**:

Ensure your test class has the correct annotations:

.. code-block:: kotlin

    @IncludeEngines("berrycrush")
    @BerryCrushScenarios(locations = "scenarios/pets.scenario")
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
    @BerryCrushScenarios(locations = "scenarios/pets.scenario")
    
    // Also correct - multiple files
    @BerryCrushScenarios(locations = [
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
    class MyBindings : BerryCrushBindings {
        override fun getBindings() = mapOf("key" to "value")
    }

    // ✗ Wrong - requires constructor argument
    class MyBindings(private val config: Config) : BerryCrushBindings {
        override fun getBindings() = mapOf("key" to config.value)
    }

For dependency injection, use the Spring module:

.. code-block:: kotlin

    @Component
    class MyBindings(
        @Autowired private val config: Config
    ) : BerryCrushBindings

OpenAPI Spec Not Found
^^^^^^^^^^^^^^^^^^^^^^

**Symptom**: ``FileNotFoundException`` when loading OpenAPI spec

**Solution**:

Ensure the spec file is on the classpath:

.. code-block:: text

    src/test/resources/
    └── petstore.yaml

And reference it correctly using ``@BerryCrushSpec``:

.. code-block:: kotlin

    @BerryCrushSpec(paths = ["petstore.yaml"])  // Relative to resources root
    @BerryCrushConfiguration(bindings = MyBindings::class)

HTTP Request Issues
-------------------

Connection Refused
^^^^^^^^^^^^^^^^^^

**Symptom**: ``ConnectException: Connection refused``

**Solution**:

1. Ensure the API server is running
2. Verify the baseUrl in getBindings() is correct
3. Check for firewall/network issues

.. code-block:: kotlin

    class MyBindings : BerryCrushBindings {
        override fun getBindings() = mapOf(
            "default" to OpenApiSpecValue("api.yaml", "http://localhost:8080")  // Must match server port
        )
    }

Timeout Errors
^^^^^^^^^^^^^^

**Symptom**: Requests timeout

**Solution**:

Increase timeout in configuration:

.. code-block:: kotlin

    @BerryCrushConfiguration(
        timeout = 60_000L  // 60 seconds
    )

SSL/TLS Errors
^^^^^^^^^^^^^^

**Symptom**: ``SSLHandshakeException`` or certificate errors

**Solution**:

For development/testing with self-signed certificates:

.. code-block:: kotlin

    class MyBindings : BerryCrushBindings {
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

.. code-block:: berrycrush

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
    @BerryCrushConfiguration(
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
    @IncludeEngines("berrycrush")
    @BerryCrushScenarios(locations = "scenarios/pets.scenario")
    class PetApiTest  // Make sure Spring context is initialized

Context Not Available
^^^^^^^^^^^^^^^^^^^^^

**Symptom**: ``NoSuchBeanDefinitionException``

**Solution**:

Check that the Spring module is included:

.. code-block:: kotlin

    dependencies {
        testImplementation("org.berrycrush.berrycrush:spring:0.1.0")
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

    class DebugPlugin : BerryCrushPlugin {
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

    @BerryCrushConfiguration(
        plugins = ["report:json:build/reports/test.json"]
    )

Understanding Error Messages
----------------------------

BerryCrush provides detailed error context to help diagnose test failures quickly.

HTTP Execution Errors
^^^^^^^^^^^^^^^^^^^^^

When HTTP requests fail, exceptions include rich context:

.. code-block:: text

    HTTP request failed: POST https://api.example.com/pets
    Cause: Connection timeout

    --- Scenario Context ---
    Scenario: Create Pet
    File: src/test/resources/pets.scenario
    Step: Create a new pet (line 15)
    Operation: createPet

    --- Request ---
    POST https://api.example.com/pets
      Content-Type: application/json
      Authorization: [MASKED]

    Request Body:
    {"name": "Fluffy", "type": "cat"}

    --- Response ---
    Status: 500 Internal Server Error
    Duration: 150ms
      Content-Type: application/json

    Response Body:
    {"error": "Database connection failed"}

Sensitive headers like ``Authorization``, ``Cookie``, and ``X-Api-Key`` are
automatically masked in error output.

Assertion Failures
^^^^^^^^^^^^^^^^^^

Assertion exceptions show expected vs actual values clearly:

.. code-block:: text

    Assertion failed for $.name [jsonpath]
      Expected: "Fluffy"
      Actual:   "Buddy"

    Scenario: Verify Pet Name
    Step: Check pet details (line 25)
    Operation: getPetById

    Response Status: 200 OK
    Response Body (preview): {"id": 123, "name": "Buddy", ...}

Schema Validation Errors
^^^^^^^^^^^^^^^^^^^^^^^^

Schema validation failures include the failing path and response context:

.. code-block:: text

    Schema validation failed at path: #/components/schemas/Pet:
      - required property 'name' is missing
      - property 'age' must be integer

    Scenario: Create Pet
    Operation: createPet

    Response Status: 200
    Response (preview): {"id": 123, "type": "cat"}

Parse Errors
^^^^^^^^^^^^

Parse exceptions show surrounding source context:

.. code-block:: text

    Parse error in test.scenario at line 3, column 8: Unknown step type 'invlid'

         1: Scenario: Test
         2:   Given a user
    >    3:   When invlid step
         4:   Then success
         5: End Scenario
                ^

Configuring Error Context
^^^^^^^^^^^^^^^^^^^^^^^^^

Control error output through configuration:

.. code-block:: kotlin

    configuration {
        // Include/exclude body content in errors
        errorContext.includeRequestBody = true
        errorContext.includeResponseBody = true

        // Limit body size (prevents huge payloads in logs)
        errorContext.maxBodySize = 4096  // bytes

        // Additional headers to mask (case-insensitive)
        // Default: authorization, cookie, set-cookie, x-api-key, x-auth-token
    }

Or via parameters:

.. code-block:: kotlin

    @BerryCrushConfiguration(
        parameters = [
            "errorContext.maxBodySize=1024",
            "errorContext.includeResponseBody=false"
        ]
    )

Getting Help
------------

If your issue isn't listed here:

1. Check the GitHub Issues for similar problems
2. Enable debug logging: ``-Dberrycrush.debug=true``
3. Create a minimal reproducing example
4. Open a new issue with:
   - BerryCrush version
   - Kotlin/Java version
   - Error message and stack trace
   - Sample code to reproduce
