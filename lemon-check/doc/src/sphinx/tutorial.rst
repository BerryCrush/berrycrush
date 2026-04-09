Tutorial
========

This tutorial walks you through building a complete API test suite using LemonCheck.

Overview
--------

We'll create tests for a simple Pet Store API that supports:

* Listing all pets
* Creating a new pet
* Getting a pet by ID
* Updating a pet
* Deleting a pet

By the end, you'll understand how to:

* Write BDD-style scenarios
* Use data tables and examples
* Create custom step definitions
* Configure plugins for reporting
* Integrate with Spring Boot

Project Setup
-------------

1. Create a new Gradle project with Kotlin DSL:

.. code-block:: bash

    mkdir petstore-tests
    cd petstore-tests
    gradle init --type kotlin-application

2. Update ``build.gradle.kts``:

.. code-block:: kotlin

    plugins {
        kotlin("jvm") version "2.0.0"
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("io.github.ktakashi.lemoncheck:core:0.1.0")
        testImplementation("io.github.ktakashi.lemoncheck:junit:0.1.0")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }

    tasks.test {
        useJUnitPlatform()
    }

Writing Scenarios
-----------------

Basic Scenario
^^^^^^^^^^^^^^

Create ``src/test/resources/scenarios/pets.scenario``:

.. code-block:: gherkin

    Feature: Pet Store API
      As a pet store user
      I want to manage pets through the API
      So that I can keep track of available pets

      Background:
        Given the API base URL is "{baseUrl}"

      Scenario: List all pets returns empty list initially
        When I request GET /api/pets
        Then the response status should be 200
        And the response body should be an empty array

      Scenario: Create a new pet
        Given the request body is:
          """
          {
            "name": "Fluffy",
            "category": "cat"
          }
          """
        When I request POST /api/pets
        Then the response status should be 201
        And the response body should contain:
          | $.name     | "Fluffy" |
          | $.category | "cat"    |

Scenario Outline with Examples
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: gherkin

      Scenario Outline: Create pets of different categories
        Given the request body is:
          """
          {
            "name": "<name>",
            "category": "<category>"
          }
          """
        When I request POST /api/pets
        Then the response status should be 201

        Examples:
          | name     | category |
          | Fluffy   | cat      |
          | Buddy    | dog      |
          | Tweety   | bird     |

Advanced Scenarios
^^^^^^^^^^^^^^^^^^

.. code-block:: gherkin

      Scenario: Get pet by ID
        Given a pet exists with name "Max"
        When I request GET /api/pets/{petId}
        Then the response status should be 200
        And the response body at "$.name" should be "Max"

      Scenario: Update a pet
        Given a pet exists with name "Max"
        And the request body is:
          """
          {
            "name": "Maximum"
          }
          """
        When I request PUT /api/pets/{petId}
        Then the response status should be 200
        And the response body at "$.name" should be "Maximum"

Custom Step Definitions
-----------------------

For complex setup logic, create custom steps:

.. code-block:: kotlin

    @Component
    class PetSteps {
        @Autowired
        lateinit var petRepository: PetRepository

        @Step("a pet exists with name {string}")
        fun createPet(name: String, context: StepContext) {
            val pet = Pet(name = name)
            petRepository.save(pet)
            context.variables["petId"] = pet.id
        }
    }

Register steps in your test configuration:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        bindings = PetStoreBindings::class,
        stepClasses = [PetSteps::class]
    )

Running Tests
-------------

Run all tests:

.. code-block:: bash

    ./gradlew test

Run a specific scenario:

.. code-block:: bash

    ./gradlew test --tests "*PetApiTest*"

Generating Reports
------------------

Configure the JSON report plugin:

.. code-block:: kotlin

    @LemonCheckConfiguration(
        plugins = ["report:json:build/reports/lemon-check.json"]
    )

See :doc:`features/reporting` for more report formats.

Spring Boot Integration
-----------------------

For Spring Boot projects, add the Spring module:

.. code-block:: kotlin

    testImplementation("io.github.ktakashi.lemoncheck:spring:0.1.0")

Then use Spring's test annotations:

.. code-block:: kotlin

    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @IncludeEngines("lemoncheck")
    @LemonCheckScenarios(locations = "scenarios/pets.scenario")
    class PetApiTest {
        @LocalServerPort
        var port: Int = 0
    }

The Spring integration will automatically discover bindings from the Spring context.

Conclusion
----------

You've learned how to:

* Set up a LemonCheck project
* Write BDD-style scenarios
* Use scenario outlines with examples
* Create custom step definitions
* Generate reports
* Integrate with Spring Boot

For more details, explore:

* :doc:`features/plugins` - Extend LemonCheck with plugins
* :doc:`features/custom-steps` - All step binding mechanisms
* :doc:`features/reporting` - Report formats and customization
