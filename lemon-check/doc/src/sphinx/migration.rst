Migration Guide
===============

This guide helps you migrate from other API testing tools to LemonCheck.

From Cucumber
-------------

LemonCheck's scenario syntax is inspired by Gherkin, making migration straightforward.

Feature Files
^^^^^^^^^^^^^

Cucumber:

.. code-block:: gherkin

    # features/pets.feature
    Feature: Pet API
      Scenario: List pets
        Given the API is available
        When I send a GET request to "/pets"
        Then the response status should be 200

LemonCheck:

.. code-block:: gherkin

    # scenarios/pets.scenario
    Feature: Pet API
      Scenario: List pets
        Given the API is available at "{baseUrl}"
        When I request GET /api/pets
        Then the response status should be 200

Key differences:
- File extension is ``.scenario`` (not ``.feature``)
- Built-in HTTP steps (no custom step definitions needed for basic operations)
- Variable binding syntax uses ``{variableName}``

Step Definitions
^^^^^^^^^^^^^^^^

Cucumber (Java):

.. code-block:: java

    @Given("the API is available")
    public void theApiIsAvailable() {
        baseUrl = "http://localhost:8080";
    }

    @When("I send a GET request to {string}")
    public void sendGet(String path) {
        response = RestAssured.get(baseUrl + path);
    }

LemonCheck (Kotlin):

.. code-block:: kotlin

    @Step("the API is available at {string}")
    fun setBaseUrl(url: String, context: StepContext) {
        context.configuration.baseUrl = url
    }

    @Step("I request {word} {any}")
    fun makeRequest(method: String, path: String, context: StepContext) {
        context.request(method, path)
    }

Configuration:

Cucumber:

.. code-block:: java

    @CucumberOptions(
        features = "classpath:features",
        glue = "com.example.steps"
    )

LemonCheck:

.. code-block:: kotlin

    @LemonCheckScenarios(locations = "scenarios/pets.scenario")
    @LemonCheckConfiguration(
        stepPackages = ["com.example.steps"]
    )

From REST Assured
-----------------

REST Assured tests can be converted to LemonCheck scenarios.

REST Assured:

.. code-block:: java

    @Test
    public void testListPets() {
        given()
            .baseUri("http://localhost:8080")
            .header("Accept", "application/json")
        .when()
            .get("/api/pets")
        .then()
            .statusCode(200)
            .body("$.size()", greaterThan(0))
            .body("[0].name", notNullValue());
    }

LemonCheck:

.. code-block:: gherkin

    Scenario: List pets
      Given the API is available at "http://localhost:8080"
      And the Accept header is "application/json"
      When I request GET /api/pets
      Then the response status should be 200
      And the response body at "$.length()" should be greater than 0
      And the response body at "$[0].name" should not be null

Benefits:
- Readable by non-technical team members
- Scenarios are documentation
- OpenAPI validation included

From Karate
-----------

Karate and LemonCheck share similar goals, with some syntax differences.

Karate:

.. code-block:: gherkin

    Feature: Pet API
      Background:
        * url 'http://localhost:8080'

      Scenario: List pets
        Given path '/api/pets'
        When method get
        Then status 200
        And match response == '#array'

LemonCheck:

.. code-block:: gherkin

    Feature: Pet API
      Background:
        Given the API is available at "http://localhost:8080"

      Scenario: List pets
        When I request GET /api/pets
        Then the response status should be 200
        And the response body should be an array

Key differences:
- LemonCheck uses standard Gherkin syntax (Given/When/Then)
- OpenAPI integration for automatic validation
- JUnit 5 native (no custom runner)

Migration Checklist
-------------------

Before Migration
^^^^^^^^^^^^^^^^

1. ☐ Document existing test coverage
2. ☐ Identify custom step definitions
3. ☐ Export OpenAPI specification
4. ☐ Inventory test data and fixtures

During Migration
^^^^^^^^^^^^^^^^

1. ☐ Set up LemonCheck dependencies
2. ☐ Convert feature files to scenario files
3. ☐ Migrate step definitions
4. ☐ Configure bindings
5. ☐ Set up CI/CD reports

After Migration
^^^^^^^^^^^^^^^

1. ☐ Verify all tests pass
2. ☐ Compare coverage before/after
3. ☐ Update CI/CD configuration
4. ☐ Train team on new syntax

Getting Help
------------

If you encounter issues during migration:

1. Check :doc:`troubleshooting` for common problems
2. Review the :doc:`tutorial` for complete examples
3. Open an issue on GitHub for specific questions
