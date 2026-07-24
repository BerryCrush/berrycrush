File-Level Parameters
=====================

The parameters section provides a way to override configuration settings at the
scenario file level. This allows you to customize behavior for specific scenario
files without changing the global configuration.

Syntax
------

Place the ``parameters:`` block at the top of your scenario file, before any
scenarios or fragments:

.. code-block:: berrycrush

   parameters:
     baseUrl: "http://localhost:8080"
     timeout: 60
     shareVariablesAcrossScenarios: true

   scenario: My test scenario
     when I call the API
       call ^listPets

Scenario-Level Parameters
=========================

Parameters can also be defined at the scenario level to override settings for
individual scenarios. This is useful when different scenarios require different
configurations.

Syntax
------

Place the ``parameters:`` block immediately after the scenario declaration, before
the steps:

.. code-block:: berrycrush

   scenario: Create pet with extended timeout
     parameters:
       timeout: 120
       retries: 3
     when I create a pet
       call ^createPet
         body: {"name": "Fluffy"}
       assert status 201

This also works with scenario outlines:

.. code-block:: berrycrush

   outline: Test with various configurations
     parameters:
       timeout: 90
     when I get pet "<name>"
       call ^getPetById
         petId: "<id>"
       assert status 200
     examples:
       | name   | id |
       | Fluffy | 1  |
       | Buddy  | 2  |

Feature-Level Parameters
========================

When using features to group scenarios, you can define parameters at the feature
level. These parameters are inherited by all scenarios in the feature.

Syntax
------

Place the ``parameters:`` block immediately after the feature declaration:

.. code-block:: berrycrush

   feature: Pet Management API
     parameters:
       environment: staging
       timeout: 60

     scenario: Create a pet
       when I create a pet
         call ^createPet
       assert status 201

     scenario: List all pets
       when I list pets
         call ^listPets
       assert status 200

Parameter Inheritance
=====================

Parameters follow a hierarchical inheritance model:

1. **File-level parameters** - Apply to all scenarios in the file
2. **Feature-level parameters** - Apply to all scenarios in the feature
3. **Scenario-level parameters** - Apply to a specific scenario

Lower levels override higher levels. For example:

.. code-block:: berrycrush

   parameters:
     timeout: 30
     environment: development

   feature: Pet Management
     parameters:
       timeout: 60
       # environment inherited from file

     scenario: Quick operation
       # timeout = 60 (from feature)
       # environment = development (from file)
       when I list pets
         call ^listPets

     scenario: Slow operation with override
       parameters:
         timeout: 120
       # timeout = 120 (from scenario, overrides feature)
       # environment = development (from file)
       when I create multiple pets
         call ^batchCreate

Variable References in Parameters
=================================

Parameter values can include variable references using the ``${...}`` syntax:

.. code-block:: berrycrush

   scenario: Use environment variables
     parameters:
       apiKey: "{{env.API_KEY}}"
       baseUrl: "https://{{env.HOST}}/api"
     when I authenticate
       call ^authenticate
         header.X-Api-Key: {{apiKey}}

Supported variable reference patterns:

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Pattern
     - Description
   * - ``{{env.VAR_NAME}}``
     - Environment variable
   * - ``{{param.paramName}}``
     - Reference to another parameter
   * - ``{{variableName}}``
     - Shorthand for context variable

Supported Parameters
--------------------

Configuration Overrides
^^^^^^^^^^^^^^^^^^^^^^^

.. list-table::
   :header-rows: 1
   :widths: 25 50 25

   * - Parameter
     - Description
     - Example Value
   * - ``baseUrl``
     - Override the base URL for API requests
     - ``"http://localhost:8080"``
   * - ``timeout``
     - Request timeout in seconds
     - ``60``
   * - ``environment``
     - Environment name for reporting
     - ``"staging"``
   * - ``strictSchemaValidation``
     - Fail on schema validation warnings
     - ``true`` / ``false``
   * - ``followRedirects``
     - Follow HTTP redirects
     - ``true`` / ``false``
   * - ``logRequests``
     - Log HTTP requests
     - ``true`` / ``false``
   * - ``logResponses``
     - Log HTTP responses
     - ``true`` / ``false``
   * - ``shareVariablesAcrossScenarios``
     - Share extracted variables across scenarios
     - ``true`` / ``false``

Header Overrides
^^^^^^^^^^^^^^^^

Use the ``header.`` prefix to add or override default headers:

.. code-block:: berrycrush

   parameters:
     header.Authorization: "Bearer test-token"
     header.X-Api-Key: "my-api-key"
     header.Accept: "application/json"

Auto-Assertion Overrides
^^^^^^^^^^^^^^^^^^^^^^^^

Control automatic assertion generation:

.. list-table::
   :header-rows: 1
   :widths: 35 65

   * - Parameter
     - Description
   * - ``autoAssertions.enabled``
     - Enable/disable all auto-assertions
   * - ``autoAssertions.statusCode``
     - Auto-assert correct status code
   * - ``autoAssertions.contentType``
     - Auto-assert Content-Type header
   * - ``autoAssertions.schema``
     - Auto-assert response matches schema

Example:

.. code-block:: berrycrush

   parameters:
     autoAssertions.enabled: false

Use Cases
---------

Environment-Specific Settings
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Configure different base URLs or authentication for different environments:

.. code-block:: berrycrush

   # production-tests.scenario
   parameters:
     baseUrl: "https://api.production.example.com"
     header.Authorization: "Bearer prod-readonly-token"
     logRequests: false

   scenario: Production health check
     when I check the API health
       call ^healthCheck
     then the API is healthy
       assert status 200

Cross-Scenario Variable Sharing
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Enable variable sharing for integration test flows:

.. code-block:: berrycrush

   # crud-workflow.scenario
   parameters:
     shareVariablesAcrossScenarios: true
     logRequests: true

   scenario: Create a resource
     when I create a pet
       call ^createPet
         body: {"name": "Fluffy", "tag": "dog"}
       extract $.id => petId

   scenario: Verify the resource
     when I retrieve the pet
       call ^getPetById
         petId: {{petId}}
     then the pet exists
       assert $.name equals "Fluffy"

   scenario: Clean up
     when I delete the pet
       call ^deletePet
         petId: {{petId}}
     then deletion succeeded
       assert status 200

Testing with Debug Logging
^^^^^^^^^^^^^^^^^^^^^^^^^^

Enable verbose logging for troubleshooting:

.. code-block:: berrycrush

   parameters:
     logRequests: true
     logResponses: true
     timeout: 120

   scenario: Debug complex flow
     when I perform complex operation
       call ^complexOperation
         body: {"debug": true}

Programmatic Usage
------------------

When using the ``ScenarioLoader`` programmatically, you can access parameters
through the ``ScenarioFileContent`` class:

.. code-block:: kotlin

   val loader = ScenarioLoader()
   val content = loader.loadFileContent(path)

  // Access ordered top-level entries (ScenarioEntry / FeatureGroup)
  val stories = content.stories

  // Extract standalone scenarios from ordered top-level entries
  val standaloneScenarios = content.scenarios

   // Access parameters
   val parameters = content.parameters
   val baseUrl = parameters["baseUrl"] as? String
   val timeout = parameters["timeout"] as? Long

To run scenarios with file-level parameters, use the ``runWithParameters`` method:

.. code-block:: kotlin

   val runner = ScenarioRunner(specRegistry, configuration, pluginRegistry)

   // Run with file-level parameter overrides
  val result = runner.runWithParameters(standaloneScenarios, content.parameters)

   // Or apply parameters to configuration manually
   val modifiedConfig = configuration.withParameters(content.parameters)
   // Execute with modifiedConfig...

JUnit Engine Integration
------------------------

File-level parameters work with both JUnit tests and standalone ``ScenarioRunner``.
When using JUnit tests, parameters in the ``.scenario`` file are applied and take
precedence over the bindings configuration for that specific file.

For JUnit tests, you can also configure settings in your bindings class:

.. code-block:: kotlin

   class MyBindings : BerryCrushBindings() {
       override fun configure(configuration: Configuration) {
           // These can be overridden by file-level parameters
           configuration.logRequests = true
       }
   }

Parameters defined in the scenario file will override the bindings configuration
for the scenarios in that specific file. This allows you to have default settings
in your bindings while customizing behavior for specific test files.

Configuration Priority
----------------------

Parameters are applied in the following order (later values override earlier):

1. Default ``Configuration`` values
2. Programmatic configuration changes
3. File-level ``parameters:`` section (highest priority for that file)

Notes
-----

- Parameters only affect scenarios in the same file
- Empty parameter values are ignored
- Unknown parameter names are silently ignored
- Boolean values can be ``true``/``false`` or ``"true"``/``"false"`` strings
- Timeout values must be integers (seconds)

See Also
--------

- :doc:`standalone-runner` - Running scenarios without JUnit
- :doc:`kotlin-dsl` - Programmatic scenario configuration
