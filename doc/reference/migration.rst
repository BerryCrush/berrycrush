Migration Guide
===============

This guide helps you migrate from other API testing tools to BerryCrush.

From Cucumber
-------------

BerryCrush's scenario syntax is simpler and more API-focused than Gherkin.

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

BerryCrush:

.. code-block:: berrycrush

    # scenarios/pets.scenario
    scenario: List pets
      when: I request pets
        call ^listPets
      then: I get a response
        assert status 200

Key differences:

- File extension is ``.scenario`` (not ``.feature``)
- No ``Feature:`` block needed (use ``feature:`` for grouping if desired)
- Uses ``call ^operationId`` instead of raw HTTP requests
- Built-in assertions (``assert status``, ``assert $.path``, etc.)
- Variable binding syntax uses ``{{variableName}}``

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

BerryCrush (Kotlin):

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

BerryCrush:

.. code-block:: kotlin

    @BerryCrushScenarios(locations = "scenarios/pets.scenario")
    @BerryCrushConfiguration(
        stepPackages = ["com.example.steps"]
    )

From REST Assured
-----------------

REST Assured tests can be converted to BerryCrush scenarios.

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

BerryCrush:

.. code-block:: berrycrush

    scenario: List pets
      when: I request pets
        call ^listPets
          header_Accept: "application/json"
      then: pets are returned
        assert status 200
        assert $.length() greaterThan 0
        assert $[0].name notEmpty

Benefits:

- Readable by non-technical team members
- Scenarios serve as documentation
- OpenAPI validation included
- Operations referenced by operationId

From Karate
-----------

Karate and BerryCrush share similar goals, with different syntax approaches.

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

BerryCrush:

.. code-block:: berrycrush

    feature: Pet API
      background:
        given: API is ready
          call ^health
          assert status 200

      scenario: List pets
        when: I request pets
          call ^listPets
        then: pets are returned
          assert status 200
          assert $.pets notEmpty

Key differences:

- BerryCrush uses ``call ^operationId`` instead of raw paths
- OpenAPI integration for automatic request/response validation
- JUnit 5 native (no custom runner needed)
- Conditional assertions with if/else/fail

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

1. ☐ Set up BerryCrush dependencies
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
