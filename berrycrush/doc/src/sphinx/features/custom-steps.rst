Custom Steps
============

BerryCrush provides multiple ways to define custom step definitions, allowing you to
create reusable, domain-specific step implementations.

Step Binding Mechanisms
-----------------------

There are four ways to define custom steps:

1. **Annotation-based** - Using ``@Step`` annotation (recommended for Java users)
2. **Registration API** - Programmatic step registration
3. **Kotlin DSL** - Type-safe DSL builder (recommended for Kotlin)
4. **Package scanning** - Auto-discovery of step classes

Annotation-Based Steps
----------------------

The simplest way to define steps is using the ``@Step`` annotation:

.. code-block:: kotlin

    class PetSteps {
        @Step("a pet exists with name {string}")
        fun createPet(name: String) {
            // Create a pet with the given name
            PetService.create(name)
        }

        @Step("I have {int} pets")
        fun setPetCount(count: Int) {
            // Set up the specified number of pets
            repeat(count) { PetService.createRandom() }
        }

        @Step("the pet should have {int} legs")
        fun verifyLegCount(expected: Int) {
            val actual = PetService.getCurrentPet().legs
            assert(actual == expected) { "Expected $expected legs but found $actual" }
        }
    }

Pattern Placeholders
^^^^^^^^^^^^^^^^^^^^

The ``@Step`` pattern supports these placeholders:

* ``{int}`` - Matches an integer (e.g., ``42``, ``-10``)
* ``{string}`` - Matches a quoted string (e.g., ``"hello"``, ``'world'``)
* ``{word}`` - Matches a single word (e.g., ``active``, ``pending``)
* ``{float}`` - Matches a floating-point number (e.g., ``3.14``, ``-2.5``)
* ``{any}`` - Matches any text (greedy)

Registering Annotation-Based Steps
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Register step classes via the configuration annotation:

.. code-block:: kotlin

    @BerryCrushConfiguration(
        stepClasses = [PetSteps::class, UserSteps::class]
    )
    class MyApiTest

Or via package scanning:

.. code-block:: kotlin

    @BerryCrushConfiguration(
        stepPackages = ["com.example.steps"]
    )
    class MyApiTest

Registration API
----------------

For dynamic step registration, use the ``StepRegistry`` API:

.. code-block:: kotlin

    val registry = DefaultStepRegistry()
    val scanner = AnnotationStepScanner()

    // Register from a class
    val definitions = scanner.scan(PetSteps::class.java)
    registry.registerAll(definitions)

    // Or register manually
    registry.register(StepDefinition(
        pattern = "the status is {word}",
        method = MySteps::class.java.getMethod("setStatus", String::class.java),
        instance = MySteps(),
        description = "Sets the current status"
    ))

Kotlin DSL
----------

The Kotlin DSL provides a type-safe, concise way to define steps:

.. code-block:: kotlin

    val registry = DefaultStepRegistry()

    steps {
        step("I have {int} pets") { count: Int ->
            PetService.setCount(count)
        }

        step("the pet name is {string}") { name: String ->
            PetService.setName(name)
        }

        step("I add {int} and {int}") { a: Int, b: Int ->
            a + b  // Return value is stored
        }

        step("the setup is complete", description = "Verifies setup") {
            // No parameters
            verifySetup()
        }
    }.registerTo(registry)

Type-Safe Parameters
^^^^^^^^^^^^^^^^^^^^

The DSL supports up to 5 typed parameters:

.. code-block:: kotlin

    step<Int>("single param {int}") { value -> ... }
    step<Int, String>("two params {int} {string}") { a, b -> ... }
    step<Int, Int, Int>("{int} + {int} + {int}") { a, b, c -> ... }

Package Scanning
----------------

BerryCrush can automatically discover step classes in specified packages:

.. code-block:: kotlin

    @BerryCrushConfiguration(
        stepPackages = [
            "com.example.steps.pets",
            "com.example.steps.users"
        ]
    )
    class MyApiTest

All classes in these packages with ``@Step`` annotated methods will be discovered
and registered.

Spring Integration
------------------

With the Spring module, steps can be Spring-managed beans:

.. code-block:: kotlin

    @Component
    class PetSteps(
        private val petRepository: PetRepository
    ) {
        @Step("a pet exists with name {string}")
        fun createPet(name: String, context: StepContext) {
            val pet = petRepository.save(Pet(name = name))
            context.variables["petId"] = pet.id
        }
    }

Enable auto-discovery with the Spring configuration:

.. code-block:: kotlin

    @SpringBootTest
    @Import(SpringStepDiscovery::class)
    @IncludeEngines("berrycrush")
    @BerryCrushScenarios(locations = "scenarios/pets.scenario")
    class PetApiTest {
        @Autowired
        lateinit var stepRegistry: StepRegistry
    }

Step Context
------------

Steps can access the execution context for variables and configuration:

.. code-block:: kotlin

    @Step("I save the pet ID as {word}")
    fun savePetId(variableName: String, context: StepContext) {
        val response = context.lastResponse
        val petId = JsonPath.read<Int>(response.body, "$.id")
        context.variables[variableName] = petId
    }

    @Step("I request the saved pet")
    fun getSavedPet(context: StepContext) {
        val petId = context.variables["petId"]
        context.request("GET", "/api/pets/$petId")
    }

Best Practices
--------------

1. **Keep steps reusable**: Steps should be generic enough to use across scenarios
2. **Use descriptive patterns**: Make step text readable and self-documenting
3. **Handle return values**: Steps can return values for chaining or verification
4. **Use context variables**: Share data between steps via the context
5. **Document complex steps**: Use the ``description`` parameter for documentation
6. **Group related steps**: Organize step classes by domain (e.g., ``PetSteps``, ``UserSteps``)

Example: Complete Step Library
------------------------------

Here's a complete example of a step library for API testing:

.. code-block:: kotlin

    class ApiSteps {
        @Step("the API is available at {string}")
        fun setBaseUrl(url: String, context: StepContext) {
            context.configuration.baseUrl = url
        }

        @Step("the request header {string} is set to {string}")
        fun setHeader(name: String, value: String, context: StepContext) {
            context.headers[name] = value
        }

        @Step("the request body is:")
        fun setRequestBody(body: String, context: StepContext) {
            context.requestBody = body
        }

        @Step("I request {word} {any}")
        fun makeRequest(method: String, path: String, context: StepContext) {
            context.request(method, path)
        }

        @Step("the response status should be {int}")
        fun verifyStatus(expected: Int, context: StepContext) {
            val actual = context.lastResponse.statusCode
            assert(actual == expected) { 
                "Expected status $expected but got $actual" 
            }
        }

        @Step("the response body at {string} should be {string}")
        fun verifyJsonPath(path: String, expected: String, context: StepContext) {
            val actual = JsonPath.read<Any>(context.lastResponse.body, path)
            assert(actual.toString() == expected) {
                "At $path: expected $expected but got $actual"
            }
        }
    }
