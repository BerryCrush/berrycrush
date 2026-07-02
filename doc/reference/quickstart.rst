Quick Start
===========

This guide will help you get BerryCrush up and running quickly.

Prerequisites
-------------

* Java 21 or later
* Kotlin 2.0+ (for Kotlin DSL)
* JUnit 6+ (JUnit Platform)
* Gradle 8.0+ or Maven 3.9+
* An OpenAPI specification for your API

Installation
------------

.. tabs::

   .. tab:: Gradle (Kotlin DSL)

      Add the following to your ``build.gradle.kts``:

      .. code-block:: kotlin

          dependencies {
              testImplementation("org.berrycrush.berrycrush:core:{{VERSION}}")
              testImplementation("org.berrycrush.berrycrush:junit:{{VERSION}}")
              // For Kotlin DSL
              testImplementation("org.berrycrush.berrycrush:kotlin-dsl:{{VERSION}}")
              // For Spring Boot projects
              testImplementation("org.berrycrush.berrycrush:spring:{{VERSION}}")
          }

   .. tab:: Gradle (Groovy)

      Add the following to your ``build.gradle``:

      .. code-block:: groovy

          dependencies {
              testImplementation 'org.berrycrush.berrycrush:core:{{VERSION}}'
              testImplementation 'org.berrycrush.berrycrush:junit:{{VERSION}}'
              // For Kotlin DSL
              testImplementation 'org.berrycrush.berrycrush:kotlin-dsl:{{VERSION}}'
              // For Spring Boot projects
              testImplementation 'org.berrycrush.berrycrush:spring:{{VERSION}}'
          }

   .. tab:: Maven

      Add to your ``pom.xml``:

      .. code-block:: xml

          <dependencies>
              <dependency>
                  <groupId>org.berrycrush.berrycrush</groupId>
                  <artifactId>core</artifactId>
                  <version>{{VERSION}}</version>
                  <scope>test</scope>
              </dependency>
              <dependency>
                  <groupId>org.berrycrush.berrycrush</groupId>
                  <artifactId>junit</artifactId>
                  <version>{{VERSION}}</version>
                  <scope>test</scope>
              </dependency>
              <!-- For Kotlin DSL -->
              <dependency>
                  <groupId>org.berrycrush.berrycrush</groupId>
                  <artifactId>kotlin-dsl</artifactId>
                  <version>{{VERSION}}</version>
                  <scope>test</scope>
              </dependency>
              <!-- For Spring Boot projects -->
              <dependency>
                  <groupId>org.berrycrush.berrycrush</groupId>
                  <artifactId>spring</artifactId>
                  <version>{{VERSION}}</version>
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

.. code-block:: berrycrush

    scenario: List all pets
      when: I request the pets list
        call ^listPets
      then: I receive a list of pets
        assert status 200
        assert $.pets notEmpty

3. Create bindings
^^^^^^^^^^^^^^^^^^

Create a bindings class to provide configuration:

.. code-block:: kotlin

    import org.berrycrush.junit.binding.OpenApiSpecValue
    import org.berrycrush.junit.BerryCrushBindings

    class PetStoreBindings : BerryCrushBindings {
        override fun getBindings(): Map<String, Any> {
            return mapOf(
                "default" to OpenApiSpecValue("petstore.yaml", "http://localhost:8080")
            )
        }
    }

4. Create a test class
^^^^^^^^^^^^^^^^^^^^^^

Create your JUnit 5 test class:

.. code-block:: kotlin

    import org.berrycrush.junit.*
    import org.junit.platform.suite.api.IncludeEngines

    @IncludeEngines("berrycrush")
    @BerryCrushScenarios(locations = ["scenarios/pet-api.scenario"])
    @BerryCrushSpec(paths = ["petstore.yaml"])
    @BerryCrushConfiguration(bindings = PetStoreBindings::class)
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
