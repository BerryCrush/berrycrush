Quick Start
===========

This guide will help you get LemonCheck up and running quickly.

Prerequisites
-------------

* Java 21 or later
* Kotlin 2.0+ (for Kotlin DSL)
* Gradle 8.0+ or Maven 3.9+
* An OpenAPI specification for your API

Installation
------------

Gradle (Kotlin DSL)
^^^^^^^^^^^^^^^^^^^

Add the following to your ``build.gradle.kts``:

.. code-block:: kotlin

    dependencies {
        testImplementation("io.github.ktakashi.lemoncheck:core:0.1.0")
        testImplementation("io.github.ktakashi.lemoncheck:junit:0.1.0")
        
        // For Spring Boot projects
        testImplementation("io.github.ktakashi.lemoncheck:spring:0.1.0")
    }

Maven
^^^^^

Add the following to your ``pom.xml``:

.. code-block:: xml

    <dependencies>
        <dependency>
            <groupId>io.github.ktakashi.lemoncheck</groupId>
            <artifactId>core</artifactId>
            <version>0.1.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.github.ktakashi.lemoncheck</groupId>
            <artifactId>junit</artifactId>
            <version>0.1.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

Creating Your First Test
------------------------

1. Create an OpenAPI specification
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Place your OpenAPI spec in ``src/test/resources/petstore.yaml``:

.. code-block:: yaml

    openapi: 3.0.3
    info:
      title: Pet Store API
      version: 1.0.0
    paths:
      /api/pets:
        get:
          operationId: listPets
          responses:
            '200':
              description: List of pets
              content:
                application/json:
                  schema:
                    type: array
                    items:
                      $ref: '#/components/schemas/Pet'
    components:
      schemas:
        Pet:
          type: object
          properties:
            id:
              type: integer
            name:
              type: string

2. Create a scenario file
^^^^^^^^^^^^^^^^^^^^^^^^^

Create ``src/test/resources/scenarios/pet-api.scenario``:

.. code-block:: gherkin

    Feature: Pet API
      
      Scenario: List all pets
        Given the API is available at "{baseUrl}"
        When I request GET /api/pets
        Then the response status should be 200
        And the response content-type should be "application/json"

3. Create bindings
^^^^^^^^^^^^^^^^^^

Create a bindings class to provide configuration:

.. code-block:: kotlin

    class PetStoreBindings : LemonCheckBindings {
        override fun getBindings(): Map<String, Any> {
            return mapOf(
                "baseUrl" to "http://localhost:8080"
            )
        }

        override fun getOpenApiSpec(): String? {
            return "petstore.yaml"
        }
    }

4. Create a test class
^^^^^^^^^^^^^^^^^^^^^^

Create your JUnit 5 test class:

.. code-block:: kotlin

    import io.github.ktakashi.lemoncheck.junit.*
    import org.junit.platform.suite.api.IncludeEngines

    @IncludeEngines("lemoncheck")
    @LemonCheckScenarios(locations = "scenarios/pet-api.scenario")
    @LemonCheckConfiguration(bindings = PetStoreBindings::class)
    class PetApiTest

5. Run the test
^^^^^^^^^^^^^^^

Run your test using Gradle:

.. code-block:: bash

    ./gradlew test

You should see output indicating which scenarios passed or failed.

Next Steps
----------

* Learn about :doc:`features/custom-steps` to create reusable step definitions
* Explore :doc:`features/plugins` to extend functionality
* Check out :doc:`features/reporting` for test report generation
* See :doc:`tutorial` for a complete walkthrough
