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

.. code-block:: text

    scenario: List all pets
      when: I request all pets
        call ^listPets
      then: I receive a list
        assert status 200
        assert $.pets notEmpty

    scenario: Create a new pet
      when: I create a pet
        call ^createPet
          body:
            name: "Fluffy"
            category: "cat"
      then: pet is created
        assert status 201
        assert $.name equals "Fluffy"
        assert $.category equals "cat"
        extract $.id => petId

Scenario Outline with Examples
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Use ``outline:`` and ``examples:`` for parameterized tests:

.. code-block:: text

    outline: Create pets of different categories
      when: I create a pet named {{name}} in category {{category}}
        call ^createPet
          body:
            name: "{{name}}"
            category: "{{category}}"
      then: pet is created
        assert status 201
        assert $.name equals "{{name}}"

      examples:
        | name   | category |
        | Fluffy | cat      |
        | Buddy  | dog      |
        | Tweety | bird     |

Advanced Scenarios
^^^^^^^^^^^^^^^^^^

.. code-block:: text

    scenario: Get pet by ID
      given: a pet exists
        call ^createPet
          body:
            name: "Max"
        assert status 201
        extract $.id => petId
      when: I request the pet
        call ^getPetById
          petId: {{petId}}
      then: I receive the pet
        assert status 200
        assert $.name equals "Max"

    scenario: Update a pet
      given: a pet exists
        call ^createPet
          body:
            name: "Max"
        assert status 201
        extract $.id => petId
      when: I update the pet
        call ^updatePet
          petId: {{petId}}
          body:
            name: "Maximum"
      then: pet is updated
        assert status 200
        assert $.name equals "Maximum"

Conditional Assertions
^^^^^^^^^^^^^^^^^^^^^^

When APIs can return different valid responses, use if/else:

.. code-block:: text

    scenario: Upsert a pet
      when: I upsert pet 999
        call ^updatePet
          petId: 999
          body:
            name: "NewPet"
        
        if status 201
          # Pet was created
          assert $.id equals 999
        else if status 200
          # Pet was updated
          assert $.name equals "NewPet"
        else
          fail "Expected 200 or 201"

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
