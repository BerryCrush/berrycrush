# DSL API Contract: BerryCrush Kotlin DSL

**Feature**: 001-openapi-bdd-testing  
**Date**: 2026-04-07  
**Version**: 1.0.0

This document defines the public Kotlin DSL API that users interact with to write scenarios.

---

## Package Structure

```
org.berrycrush.berrycrush
├── dsl           # User-facing DSL functions
├── config        # Configuration classes
├── result        # Result types (read-only)
└── junit         # JUnit 5 integration (separate module)
```

---

## Core DSL Functions

### `berryCrush` - Entry Point

Creates and configures a BerryCrush test suite.

**Single Spec (Simple)**:
```kotlin
fun berryCrush(
    openApiSpec: String,                    // Path to OpenAPI spec file
    config: Configuration.() -> Unit = {}  // Optional configuration block
): BerryCrushSuite
```

**Multiple Specs (Advanced)**:
```kotlin
fun berryCrush(
    config: MultiSpecConfiguration.() -> Unit
): BerryCrushSuite
```

**Examples**:

```kotlin
// Single spec - simple usage
val suite = berryCrush("src/test/resources/petstore.yaml") {
    baseUrl = "https://petstore.example.com"
    timeout = 30.seconds
    defaultHeaders["X-Api-Key"] = System.getenv("API_KEY")
}

// Multiple specs - microservices or multi-API testing
val suite = berryCrush {
    spec("petstore", "specs/petstore.yaml") {
        baseUrl = "https://petstore.example.com/api/v1"
    }
    spec("inventory", "specs/inventory.yaml") {
        baseUrl = "https://inventory.example.com/api/v1"
    }
    spec("orders", "specs/orders.yaml") {
        baseUrl = "https://orders.example.com/api/v1"
        defaultHeaders["X-Service"] = "orders-client"
    }
    
    // Global settings apply to all specs
    timeout = 30.seconds
    environment = "staging"
}
```

---

### `spec` - Register an OpenAPI Specification

Registers a named OpenAPI specification for multi-spec scenarios.

```kotlin
fun MultiSpecConfiguration.spec(
    name: String,                           // Unique identifier for this spec
    path: String,                           // Path to OpenAPI spec file
    config: SpecConfiguration.() -> Unit = {}
)
```

---

### `using` - Switch Active Spec Context

In multi-spec scenarios, switches which spec operations are resolved against.

```kotlin
fun StepScope.using(specName: String)
```

**Example**:
```kotlin
scenario("Cross-service order flow") {
    given("pet exists") {
        using("petstore")
        call("getPetById") { pathParam("petId", 123) }
        extractTo("price", "$.price")
    }
    
    `when`("placing order") {
        using("orders")
        call("createOrder") {
            body("""{"petId": 123, "price": ${context["price"]}}""")
        }
    }
}
```

**Note**: If `using()` is not specified and operationId is unique across all specs, auto-resolution occurs. If ambiguous, an error is thrown.

---

### `scenario` - Define a Test Scenario

Defines a BDD scenario within a suite.

```kotlin
fun BerryCrushSuite.scenario(
    name: String,
    tags: Set<String> = emptySet(),
    block: ScenarioScope.() -> Unit
): Scenario
```

**Example**:
```kotlin
suite.scenario("List all pets", tags = setOf("smoke", "pets")) {
    given("the API is available") {
        // setup steps
    }
    `when`("I request all pets") {
        call("listPets")
    }
    then("I receive a list of pets") {
        statusCode(200)
        bodyContains("pets")
    }
}
```

---

### `scenarioOutline` - Parameterized Scenario

Defines a scenario that runs multiple times with different data.

```kotlin
fun BerryCrushSuite.scenarioOutline(
    name: String,
    block: ScenarioOutlineScope.() -> Unit
): List<Scenario>
```

**Example**:
```kotlin
suite.scenarioOutline("Create pet with validation") {
    examples(
        row(name = "Rex", status = 201),
        row(name = "", status = 400),
        row(name = "A".repeat(256), status = 400)
    )
    
    `when`("creating pet with name '<name>'") {
        call("createPet") {
            body("""{"name": "$name"}""")
        }
    }
    
    then("response status is <status>") {
        statusCode(status)
    }
}
```

---

### `fragment` - Reusable Step Group

Defines a reusable group of steps.

```kotlin
fun fragment(
    name: String,
    block: FragmentScope.() -> Unit
): Fragment
```

**Example**:
```kotlin
val authenticateAdmin = fragment("authenticate as admin") {
    given("admin credentials") {
        call("login") {
            body("""{"username": "admin", "password": "secret"}""")
        }
        extractTo("authToken", "$.token")
    }
}
```

---

## Step DSL Functions

### `given`, `when`, `then`, `and` - BDD Steps

```kotlin
fun ScenarioScope.given(description: String, block: StepScope.() -> Unit = {})
fun ScenarioScope.`when`(description: String, block: StepScope.() -> Unit = {})
fun ScenarioScope.then(description: String, block: StepScope.() -> Unit = {})
fun ScenarioScope.and(description: String, block: StepScope.() -> Unit = {})
```

**Note**: `when` requires backticks as it's a Kotlin keyword.

---

### `call` - Execute API Operation

Invokes an API operation defined in the OpenAPI spec.

```kotlin
fun StepScope.call(
    operationId: String,
    block: CallScope.() -> Unit = {}
)
```

**Example**:
```kotlin
`when`("I fetch the pet") {
    call("getPetById") {
        pathParam("petId", context["petId"])
        queryParam("includeDetails", true)
        header("Accept", "application/json")
    }
}
```

---

### `include` - Include Fragment

Executes a pre-defined fragment within a scenario.

```kotlin
fun ScenarioScope.include(fragment: Fragment)
```

**Example**:
```kotlin
scenario("Admin creates pet") {
    include(authenticateAdmin)  // Runs all fragment steps
    
    `when`("admin creates a pet") {
        // ...
    }
}
```

---

## Call Configuration Functions

### Request Building

```kotlin
interface CallScope {
    // Path parameters (required by operation)
    fun pathParam(name: String, value: Any)
    
    // Query parameters
    fun queryParam(name: String, value: Any)
    
    // Headers
    fun header(name: String, value: String)
    
    // Request body (JSON string or object that will be serialized)
    fun body(content: String)
    fun body(content: Any)  // Auto-serialized to JSON
    
    // Authentication shortcuts
    fun bearerToken(token: String)
    fun basicAuth(username: String, password: String)
    fun apiKey(headerName: String, key: String)
}
```

---

### Response Extraction

```kotlin
interface StepScope {
    // Extract value from response for use in subsequent steps
    fun extractTo(variableName: String, jsonPath: String)
    
    // Extract with default if not found
    fun extractTo(
        variableName: String, 
        jsonPath: String, 
        defaultValue: Any
    )
    
    // Access extracted values
    val context: ExecutionContext
}
```

**Example**:
```kotlin
given("pets exist") {
    call("listPets")
    extractTo("firstPetId", "$.pets[0].id")
    extractTo("petCount", "$.total", defaultValue = 0)
}

`when`("viewing the first pet") {
    call("getPetById") {
        pathParam("petId", context["firstPetId"])
    }
}
```

---

## Assertion Functions

### Status Code

```kotlin
fun StepScope.statusCode(expected: Int)
fun StepScope.statusCode(range: IntRange)  // e.g., 200..299
```

### Headers

```kotlin
fun StepScope.headerExists(name: String)
fun StepScope.headerEquals(name: String, expected: String)
fun StepScope.headerMatches(name: String, pattern: Regex)
```

### Body Content

```kotlin
// String contains
fun StepScope.bodyContains(substring: String)

// JSONPath value assertions
fun StepScope.bodyEquals(jsonPath: String, expected: Any)
fun StepScope.bodyMatches(jsonPath: String, pattern: Regex)
fun StepScope.bodyIsNull(jsonPath: String)
fun StepScope.bodyIsNotNull(jsonPath: String)

// Collection assertions
fun StepScope.bodyArraySize(jsonPath: String, expected: Int)
fun StepScope.bodyArrayNotEmpty(jsonPath: String)
```

### Schema Validation

```kotlin
// Validate against OpenAPI schema for this operation
fun StepScope.matchesSchema()

// Validate with explicit strictness
fun StepScope.matchesSchema(strict: Boolean)
```

### Response Time

```kotlin
fun StepScope.responseTime(maxDuration: Duration)
```

**Example**:
```kotlin
then("the pet details are correct") {
    statusCode(200)
    bodyEquals("$.name", "Rex")
    bodyEquals("$.status", "available")
    bodyArrayNotEmpty("$.tags")
    matchesSchema()
    responseTime(500.milliseconds)
}
```

---

## Auto-Assertions (Spec-Driven)

BerryCrush automatically generates assertions from OpenAPI spec responses. This enables "happy path" testing with zero boilerplate.

### How It Works

When you call an operation without explicit assertions, BerryCrush extracts expected behavior from the spec:

```kotlin
// Without explicit assertions - auto-generated from spec
`when`("listing pets") {
    call("listPets")
    // Auto-asserts based on spec:
    //   - status = 200 (from responses.200)
    //   - schema valid (from responses.200.content.application/json.schema)
    //   - Content-Type: application/json (from responses.200.content keys)
}

// Adding explicit assertions disables auto-generation of that type
`when`("listing pets") {
    call("listPets")
    statusCode(200)  // Explicit = auto status code disabled, but schema still auto-validates
}
```

### Disabling Auto-Assertions

For error cases or specific tests:

```kotlin
// Per-call disable
`when`("fetching non-existent pet") {
    call("getPetById") {
        pathParam("petId", 999999)
        autoAssert(false)  // Disable all auto-assertions
    }
    statusCode(404)  // Manual assertion for error case
}

// Selective disable
`when`("testing with partial response") {
    call("listPets") {
        autoAssertSchema(false)  // Disable only schema validation
    }
    statusCode(200)
}
```

### What Gets Auto-Asserted

| Spec Element | Auto-Assertion |
|--------------|----------------|
| `responses.2xx` (first found) | `statusCode(200)` |
| `responses.2xx.content.*.schema` | `matchesSchema()` |
| `responses.2xx.content` keys | `headerEquals("Content-Type", "application/json")` |
| `responses.2xx.headers[required=true]` | `headerExists("X-Request-Id")` |

---

## Configuration DSL

```kotlin
class Configuration {
    // Base URL (overrides OpenAPI servers)
    var baseUrl: String?
    
    // Request timeout
    var timeout: Duration
    
    // Headers applied to all requests
    val defaultHeaders: MutableMap<String, String>
    
    // Environment name for logging/reporting
    var environment: String
    
    // Schema validation strictness
    var strictValidation: Boolean
    
    // Custom reporter
    var reporter: TestReporter
    
    // Variable presets
    fun variables(block: MutableMap<String, Any>.() -> Unit)
    
    // Auto-assertion configuration (derived from OpenAPI spec)
    fun autoAssertions(block: AutoAssertionConfig.() -> Unit)
    
    // Load scenario files
    fun loadScenariosFrom(directory: String)
    fun loadFragmentsFrom(directory: String)
}

class AutoAssertionConfig {
    var enabled: Boolean = true           // Master switch
    var statusCode: Boolean = true        // Auto-assert success status codes
    var schemaValidation: Boolean = true  // Auto-validate response schemas
    var contentType: Boolean = true       // Auto-check Content-Type header
    var requiredHeaders: Boolean = true   // Auto-check required response headers
}
```

**Example**:
```kotlin
berryCrush("petstore.yaml") {
    baseUrl = System.getenv("API_BASE_URL")
    timeout = 30.seconds
    environment = "staging"
    strictValidation = true
    
    defaultHeaders["X-Request-Id"] = UUID.randomUUID().toString()
    
    variables {
        put("adminUser", "admin@example.com")
        put("testPrefix", "test_${System.currentTimeMillis()}")
    }
}
```

---

## Execution Functions

### Run Scenarios

```kotlin
// Run all scenarios in the suite
fun BerryCrushSuite.runAll(): TestReport

// Run scenarios matching tags
fun BerryCrushSuite.runTagged(vararg tags: String): TestReport

// Run specific scenario by name
fun BerryCrushSuite.run(scenarioName: String): ScenarioResult
```

---

## JUnit 5 Integration (berrycrush-junit module)

### Extension

```kotlin
@ExtendWith(BerryCrushExtension::class)
class PetstoreTests {
    
    @BerryCrushSpec("petstore.yaml")
    lateinit var suite: BerryCrushSuite
    
    @Test
    fun `list pets returns 200`() {
        suite.scenario("list pets") {
            `when`("listing all pets") {
                call("listPets")
            }
            then("success response") {
                statusCode(200)
            }
        }.run()
    }
}
```

### Dynamic Tests

```kotlin
@TestFactory
fun `pet crud scenarios`(): Stream<DynamicTest> {
    return suite.scenarios.stream().map { scenario ->
        dynamicTest(scenario.name) {
            scenario.run()
        }
    }
}
```

---

## Error Handling

### Custom Exceptions

```kotlin
// Thrown when OpenAPI spec cannot be parsed
class OpenApiParseException(message: String, cause: Throwable?)

// Thrown when operation ID not found in spec
class OperationNotFoundException(operationId: String)

// Thrown when extraction path finds no value
class ExtractionException(variableName: String, jsonPath: String)

// Thrown when assertion fails
class AssertionFailedException(
    assertion: Assertion,
    expected: Any?,
    actual: Any?,
    message: String
)

// Thrown on HTTP/network errors
class HttpExecutionException(message: String, cause: Throwable?)
```

---

## Type Aliases

```kotlin
typealias JsonPath = String
typealias OperationId = String
typealias VariableName = String
```

---

## Thread Safety

- `BerryCrushSuite` is immutable after creation
- `Scenario` instances are immutable
- `ExecutionContext` is NOT thread-safe (scenarios run sequentially)
- `Configuration` is immutable after suite creation

---

## Versioning

This API follows semantic versioning:
- **1.x**: Stable, backward-compatible changes only
- Breaking changes require major version bump

**Current Version**: 1.0.0
