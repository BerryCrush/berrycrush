# Research: OpenAPI BDD Testing Library

**Feature**: 001-openapi-bdd-testing  
**Date**: 2026-04-07  
**Status**: Complete

## Research Tasks

This document consolidates research findings for technology choices and design decisions.

---

## R1: Kotlin DSL Design Patterns for BDD Scenarios

**Task**: Research best practices for designing readable, type-safe Kotlin DSLs for BDD-style test scenarios.

### Decision: Builder Pattern with Lambda Receivers

**Rationale**: Kotlin's lambda with receiver (`T.() -> Unit`) enables fluent, readable DSLs that feel natural while maintaining full type safety. This pattern is proven in Kotlin ecosystem (Gradle, Ktor, kotlinx.html).

**Alternatives Considered**:
- **Annotation-based (like Cucumber)**: Rejected—requires string matching, loses compile-time safety, IDE support is weaker
- **Method chaining only**: Rejected—less readable for nested structures, harder to express hierarchies
- **External DSL (Gherkin files)**: Rejected—user requested Kotlin DSL; external files lose refactoring support

**Key Design Decisions**:
```kotlin
// Target DSL style
scenario("Customer purchases a pet") {
    given("the pet store has pets") {
        call("listPets")
        extractTo("firstPetId", "$.pets[0].id")
    }
    
    `when`("the customer views the pet details") {
        call("getPetById") {
            pathParam("petId", context["firstPetId"])
        }
    }
    
    then("the pet information is displayed") {
        statusCode(200)
        bodyContains("name")
    }
}
```

**Implementation Notes**:
- Use `@DslMarker` annotation to prevent scope leakage
- Leverage Kotlin's backtick escaping for `when` keyword
- Support both block and inline assertion styles

---

## R2: Swagger Parser Integration

**Task**: Evaluate Swagger Parser library capabilities for OpenAPI 3.x parsing and operation resolution.

### Decision: Use swagger-parser 2.1.x with OpenAPI4J for Schema Validation

**Rationale**: Swagger Parser (io.swagger.parser.v3) is the de-facto standard, actively maintained, and handles OpenAPI 3.0/3.1 specs. It provides direct access to parsed models without custom parsing.

**Alternatives Considered**:
- **OpenAPI Generator**: Rejected—focused on code generation, overkill for parsing
- **Manual YAML/JSON parsing**: Rejected—reinventing complex validation logic
- **KOpenAPI**: Rejected—less mature, smaller community

**Integration Pattern**:
```kotlin
// Loading and resolving operations
val openApi = OpenAPIV3Parser().read(specPath)
val operation = openApi.paths["/pets/{petId}"]?.get
val requestBody = operation?.requestBody?.content?.get("application/json")?.schema
```

**Key Capabilities Used**:
- Path parameter extraction from operation definitions
- Request/response schema access for validation
- Server URL resolution for base URL configuration
- Security scheme definitions for authentication

### Multi-Spec Support

**Requirement**: Support loading multiple OpenAPI specifications for testing microservices or APIs spanning multiple services.

**Implementation Pattern**:
```kotlin
// Named specs for multi-service scenarios
val suite = lemonCheck {
    spec("petstore", "specs/petstore.yaml") {
        baseUrl = "https://petstore.example.com"
    }
    spec("inventory", "specs/inventory.yaml") {
        baseUrl = "https://inventory.example.com"
    }
    spec("orders", "specs/orders.yaml") {
        baseUrl = "https://orders.example.com"
    }
}

// Reference specific spec in scenario
suite.scenario("Cross-service order flow") {
    given("pet exists in store") {
        using("petstore")  // Switch to petstore spec context
        call("getPetById") {
            pathParam("petId", 123)
        }
        extractTo("petPrice", "$.price")
    }
    
    `when`("checking inventory") {
        using("inventory")  // Switch to inventory spec
        call("checkStock") {
            queryParam("productId", 123)
        }
    }
    
    then("order can be placed") {
        using("orders")  // Switch to orders spec
        call("createOrder") {
            body("""{"petId": 123, "price": ${context["petPrice"]}}""")
        }
        statusCode(201)
    }
}
```

**Spec Registry Design**:
```kotlin
class SpecRegistry {
    private val specs: MutableMap<String, LoadedSpec> = mutableMapOf()
    
    fun register(name: String, path: String, config: SpecConfig)
    fun get(name: String): LoadedSpec
    fun resolve(operationId: String): Pair<String, Operation>  // Auto-find spec by operationId
}

data class LoadedSpec(
    val name: String,
    val openApi: OpenAPI,
    val baseUrl: String,
    val defaultHeaders: Map<String, String>
)
```

**Auto-Resolution**: If `using()` is not specified and operationId is unique across all specs, auto-resolve to the correct spec. If ambiguous, require explicit `using()`.

**Scenario File Support**:
```gherkin
@openapi: petstore=specs/petstore.yaml, inventory=specs/inventory.yaml

  Scenario: Cross-service flow
    Given pet exists
      | using     | petstore |
      | operation | getPetById |
```

**Dependency**:
```kotlin
implementation("io.swagger.parser.v3:swagger-parser:2.1.22")
```

---

## R3: Java HttpClient Best Practices

**Task**: Research patterns for using java.net.http.HttpClient for reliable, testable HTTP execution.

### Decision: Single HttpClient Instance with Request/Response Builders

**Rationale**: Java 21's HttpClient is production-ready, supports HTTP/2, async operations, and has no external dependencies. Creating one client instance and reusing it is the recommended pattern.

**Alternatives Considered**:
- **OkHttp**: Rejected—adds external dependency, vanilla Java preferred per requirements
- **Apache HttpClient**: Rejected—heavier dependency, more complex API
- **Ktor Client**: Rejected—adds Kotlin-specific dependency, vanilla Java preferred

**Implementation Pattern**:
```kotlin
class HttpRequestBuilder(private val client: HttpClient) {
    fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResponse<String> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it) }
                ?: HttpRequest.BodyPublishers.noBody())
        
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        
        return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
    }
}
```

**Configuration Options**:
- Connection timeout: configurable, default 30s
- Request timeout: configurable per step, default 10s
- Follow redirects: configurable, default NORMAL

---

## R4: Value Extraction from JSON Responses

**Task**: Evaluate approaches for extracting values from JSON responses for use in subsequent steps.

### Decision: JSONPath with Jackson Integration

**Rationale**: JSONPath is widely understood, expressive for nested extraction, and Jackson (already bundled with Swagger Parser) provides the JSON tree model.

**Alternatives Considered**:
- **JMESPath**: Rejected—less familiar to users, smaller ecosystem
- **Custom path syntax**: Rejected—requires documentation, learning curve
- **Direct Jackson tree navigation**: Rejected—verbose for complex paths

**Implementation Pattern**:
```kotlin
// Using Jayway JSONPath with Jackson provider
val json = """{"pets": [{"id": 123, "name": "Fluffy"}]}"""
val petId: Int = JsonPath.read(json, "$.pets[0].id")

// Integration with context
context["firstPetId"] = JsonPath.read(response.body(), extractionPath)
```

**Dependency**:
```kotlin
implementation("com.jayway.jsonpath:json-path:2.9.0")
```

**Supported Patterns**:
- `$.field` - Root field
- `$.array[0]` - Array index
- `$.array[*].field` - All elements
- `$.object.nested.field` - Nested path
- `$..field` - Recursive descent

---

## R5: Fragment and Reusability Patterns

**Task**: Research patterns for implementing reusable scenario fragments that maintain context.

### Decision: Named Fragments with Explicit Invocation

**Rationale**: Fragments defined as named blocks that can be invoked within scenarios. This is explicit, traceable, and maintains IDE navigation.

**Alternatives Considered**:
- **Inheritance-based**: Rejected—fragile, hard to compose multiple fragments
- **Mixin traits**: Rejected—Kotlin doesn't support multiple inheritance well for this use case
- **Implicit backgrounds**: Rejected—magic behavior reduces clarity

**Implementation Pattern**:
```kotlin
// Fragment definition
val authenticateAsAdmin = fragment("authenticate as admin") {
    given("admin credentials") {
        call("login") {
            body("""{"username": "admin", "password": "secret"}""")
        }
        extractTo("authToken", "$.token")
    }
}

// Fragment usage
scenario("Admin creates a pet") {
    include(authenticateAsAdmin)
    
    `when`("admin creates a new pet") {
        call("createPet") {
            header("Authorization", "Bearer ${context["authToken"]}")
            body("""{"name": "Rex", "tag": "dog"}""")
        }
    }
    // ...
}
```

**Context Handling**:
- Fragments share the same ExecutionContext as the including scenario
- Values extracted in fragments are available in subsequent steps
- Fragments can be nested (fragment includes fragment)

---

## R6: Parameterized Scenario Patterns

**Task**: Research patterns for parameterized scenarios with data tables.

### Decision: Data-Driven Scenarios with Inline Tables

**Rationale**: Kotlin's type-safe builders allow embedding data tables directly in test code with full IDE support. Each row generates a separate test execution.

**Alternatives Considered**:
- **External CSV/JSON files**: Rejected—loses IDE navigation, refactoring support
- **JUnit @ParameterizedTest only**: Rejected—doesn't integrate with DSL scenario structure
- **Annotation-based**: Rejected—less readable, loses DSL benefits

**Implementation Pattern**:
```kotlin
scenarioOutline("Create pet with various inputs") {
    examples(
        row(name = "Rex", tag = "dog", expectedStatus = 201),
        row(name = "", tag = "cat", expectedStatus = 400),
        row(name = "A".repeat(256), tag = "bird", expectedStatus = 400),
    )
    
    `when`("creating a pet with name '<name>' and tag '<tag>'") {
        call("createPet") {
            body("""{"name": "$name", "tag": "$tag"}""")
        }
    }
    
    then("the response status is '<expectedStatus>'") {
        statusCode(expectedStatus)
    }
}
```

**JUnit Integration**:
- Each example row becomes a dynamic test
- Test names include parameter values for identification
- Failures report which parameter combination failed

---

## R7: Schema Validation Approach

**Task**: Research approaches for validating API responses against OpenAPI schema definitions.

### Decision: JSON Schema Validation via networknt/json-schema-validator

**Rationale**: OpenAPI schemas are JSON Schema compatible. The networknt validator is fast, well-maintained, and provides detailed error messages.

**Alternatives Considered**:
- **Everit JSON Schema**: Rejected—less actively maintained
- **Custom validation**: Rejected—significant effort, error-prone
- **Swagger validation built-in**: Limited—doesn't provide detailed path-based errors

**Implementation Pattern**:
```kotlin
class SchemaValidator(private val openApi: OpenAPI) {
    fun validate(operationId: String, response: String): List<ValidationError> {
        val schema = resolveResponseSchema(operationId)
        val jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
            .getSchema(schema.toJsonNode())
        
        val errors = jsonSchema.validate(objectMapper.readTree(response))
        return errors.map { ValidationError(it.path, it.message) }
    }
}
```

**Dependency**:
```kotlin
implementation("com.networknt:json-schema-validator:1.4.0")
```

**Validation Modes**:
- **Strict**: Extra fields are errors
- **Lenient**: Extra fields ignored, only type/required validated

---

## R8: Test Reporting Strategy

**Task**: Research reporting formats and patterns for test results.

### Decision: Pluggable Reporter Interface with Console Default

**Rationale**: Different users need different formats (console, JUnit XML, HTML, JSON). A pluggable interface allows extension without core changes.

**Alternatives Considered**:
- **Fixed format**: Rejected—limits adoption across CI systems
- **Only JUnit integration**: Rejected—loses standalone usage

**Implementation Pattern**:
```kotlin
interface TestReporter {
    fun onScenarioStart(scenario: Scenario)
    fun onStepComplete(step: Step, result: StepResult)
    fun onScenarioComplete(scenario: Scenario, result: ScenarioResult)
    fun generateReport(): String
}

class ConsoleReporter : TestReporter {
    override fun onStepComplete(step: Step, result: StepResult) {
        val icon = if (result.success) "✓" else "✗"
        println("  $icon ${step.description}")
        if (!result.success) {
            println("    Expected: ${result.expected}")
            println("    Actual: ${result.actual}")
        }
    }
}
```

**Built-in Reporters**:
- `ConsoleReporter` - Human-readable terminal output
- `JsonReporter` - Machine-readable JSON for CI integration
- JUnit XML via `lemon-check-junit` module

---

## R9: BDD Scenario File Format and Parser

**Task**: Design a user-friendly text-based scenario format and parser for non-technical stakeholders.

### Decision: Custom Gherkin-inspired Format with API Extensions

**Rationale**: A custom format allows API-specific extensions (operation binding, JSONPath extraction) while maintaining the familiar Given/When/Then structure that business stakeholders understand. Unlike standard Gherkin, our format includes explicit API operation mapping and data flow syntax.

**Alternatives Considered**:
- **Standard Gherkin**: Rejected—requires step definition glue code, loses direct OpenAPI binding
- **YAML-based scenarios**: Rejected—less readable for non-technical users
- **Markdown-based**: Rejected—parsing ambiguity, hard to enforce structure

### Scenario File Format (`.scenario` extension)

```gherkin
# petstore-scenarios.scenario
@openapi: petstore.yaml
@baseUrl: ${env:PETSTORE_URL}

Feature: Pet Store Customer Journey
  As a customer
  I want to browse and purchase pets
  So that I can find my perfect companion

  Background: Authenticated User
    Given I am authenticated as "customer@example.com"
      | operation | login                           |
      | body      | {"email": "<email>", "pass": "<pass>"} |
      | extract   | authToken -> $.token            |

  Scenario: Browse and purchase a pet
    Given the pet store has available pets
      | operation | listPets                        |
      | extract   | firstPetId -> $.pets[0].id      |
      | assert    | status = 200                    |
      | assert    | $.pets is not empty             |

    When I view the pet details
      | operation | getPetById                      |
      | path      | petId = ${firstPetId}           |
      | assert    | status = 200                    |
      | assert    | $.status = "available"          |

    And I purchase the pet
      | operation | purchasePet                     |
      | path      | petId = ${firstPetId}           |
      | body      | {"paymentMethod": "card"}       |
      | assert    | status = 201                    |

    Then the pet is no longer available
      | operation | getPetById                      |
      | path      | petId = ${firstPetId}           |
      | assert    | status = 404                    |

  Scenario Outline: Create pet with validation
    When I create a pet with name "<name>"
      | operation | createPet                       |
      | body      | {"name": "<name>", "tag": "<tag>"} |

    Then the response is <expectedStatus>
      | assert    | status = <expectedStatus>       |

    Examples:
      | name   | tag  | expectedStatus |
      | Rex    | dog  | 201            |
      |        | cat  | 400            |
      | A*256  | bird | 400            |
```

### Grammar Definition (EBNF-like)

```ebnf
scenario_file   = metadata* feature
metadata        = "@" key ":" value NEWLINE
feature         = "Feature:" text NEWLINE description? background? scenario+
description     = indent text NEWLINE (indent text NEWLINE)*
background      = "Background:" text NEWLINE step+
scenario        = ("Scenario:" | "Scenario Outline:") text NEWLINE step+ examples?
step            = step_keyword text NEWLINE step_table?
step_keyword    = "Given" | "When" | "Then" | "And" | "But"
step_table      = ("|" cell+ "|" NEWLINE)+
examples        = "Examples:" NEWLINE table
table           = header_row data_row+
header_row      = "|" cell+ "|" NEWLINE
data_row        = "|" cell+ "|" NEWLINE
cell            = text
text            = [^|NEWLINE]+
value           = [^NEWLINE]+
key             = identifier
```

### Step Table Directives

| Directive | Purpose | Example |
|-----------|---------|---------|
| `operation` | OpenAPI operation ID to call | `operation \| listPets` |
| `path` | Path parameter binding | `path \| petId = ${id}` |
| `query` | Query parameter | `query \| limit = 10` |
| `header` | Request header | `header \| X-Api-Key = ${apiKey}` |
| `body` | Request body (JSON) | `body \| {"name": "Rex"}` |
| `extract` | Extract to variable | `extract \| petId -> $.id` |
| `assert` | Assertion | `assert \| status = 200` |

### Variable Interpolation

```text
${variableName}     - Reference extracted variable
${env:VAR_NAME}     - Environment variable
<placeholder>       - Scenario outline parameter
```

### Parser Implementation

```kotlin
class ScenarioParser {
    fun parse(input: String): FeatureFile
    fun parse(file: Path): FeatureFile
    fun parseDirectory(dir: Path): List<FeatureFile>
}

data class FeatureFile(
    val metadata: Map<String, String>,
    val feature: Feature
)

data class Feature(
    val name: String,
    val description: String?,
    val background: Background?,
    val scenarios: List<ParsedScenario>
)
```

### Parser Strategy: Recursive Descent

**Rationale**: Simple, debuggable, produces excellent error messages with line/column information. No external parser generator dependency.

**Implementation Notes**:
- Lexer tokenizes into: KEYWORD, TEXT, TABLE_CELL, NEWLINE, INDENT
- Parser builds AST with source location tracking
- Semantic analysis validates operation IDs against OpenAPI spec
- Errors include file:line:column and suggestion

**Dependency**: No external parser libraries (pure Kotlin implementation)

---

## R10: Auto-Generated Assertions from OpenAPI Spec

**Task**: Research how to automatically extract happy-path assertions from OpenAPI specifications so users don't need to manually define expected behaviors.

### Decision: Spec-Driven Assertion Generation with Explicit Override

**Rationale**: OpenAPI specs already define expected responses (status codes, schemas, headers). Auto-generating assertions from the spec reduces boilerplate, ensures tests stay in sync with the contract, and makes the happy path "just work" without manual assertion writing.

### What Can Be Auto-Generated

From an OpenAPI operation, we can extract:

| Element | Source in OpenAPI | Auto-Generated Assertion |
|---------|-------------------|--------------------------|
| Success status code | `responses.2xx` | `status = 200` (or 201, 204, etc.) |
| Response schema | `responses.200.content.*.schema` | `schema valid` |
| Required headers | `responses.200.headers` | `header exists X-Request-Id` |
| Content-Type | `responses.200.content` keys | `header Content-Type = application/json` |

### Implementation: Implicit Happy Path Mode

```kotlin
// Without explicit assertions - uses spec defaults
`when`("listing pets") {
    call("listPets")
    // Auto-asserts: status = 200, schema valid (derived from spec)
}

// With explicit assertions - overrides auto-generated
`when`("listing pets") {
    call("listPets")
    statusCode(200)  // Explicit = no auto-generation
}

// Opt-out of auto-assertions
`when`("testing error case") {
    call("listPets") {
        autoAssert(false)  // Disable auto-assertions for this call
    }
    statusCode(500)  // Manual assertion for error case
}
```

### Auto-Assertion Rules

1. **Default Success Status**: Use the first 2xx response code defined (commonly 200)
2. **Schema Validation**: If response has a schema, auto-validate against it
3. **Content-Type**: If response defines content types, validate header
4. **No Override**: If user provides explicit assertion of same type, skip auto-generation

### Spec Extraction Implementation

```kotlin
class AssertionGenerator(private val openApi: OpenAPI) {
    
    fun generateForOperation(operationId: String): List<Assertion> {
        val operation = resolveOperation(operationId)
        val assertions = mutableListOf<Assertion>()
        
        // Find success response (first 2xx)
        val successResponse = operation.responses
            .filterKeys { it.startsWith("2") }
            .entries.firstOrNull()
        
        if (successResponse != null) {
            // Status code assertion
            assertions += StatusCodeAssertion(successResponse.key.toInt())
            
            // Schema validation if defined
            val schema = successResponse.value.content
                ?.get("application/json")?.schema
            if (schema != null) {
                assertions += SchemaValidAssertion(schema)
            }
            
            // Required response headers
            successResponse.value.headers?.forEach { (name, header) ->
                if (header.required == true) {
                    assertions += HeaderExistsAssertion(name)
                }
            }
        }
        
        return assertions
    }
}
```

### Scenario File Support

```gherkin
# Auto-assertions enabled by default
  When I list pets
    | operation | listPets |
    # Auto-asserts status=200 and schema valid from spec

# Explicit override
  When I list pets (expecting empty)
    | operation   | listPets |
    | assert      | status = 200 |
    | assert      | $.pets is empty |
    | auto-assert | false |

# Error case - must disable auto-assert
  When I request non-existent pet
    | operation   | getPetById |
    | path        | petId = 999999 |
    | auto-assert | false |
    | assert      | status = 404 |
```

### Configuration Options

```kotlin
lemonCheck("spec.yaml") {
    // Global auto-assertion settings
    autoAssertions {
        enabled = true                    // Default: true
        statusCode = true                 // Auto-assert status codes
        schemaValidation = true           // Auto-validate response schemas
        contentType = true                // Auto-check Content-Type header
        requiredHeaders = true            // Auto-check required response headers
    }
}
```

### Benefits

1. **Less Boilerplate**: Happy path tests need zero assertions—just call the operation
2. **Contract Sync**: Tests automatically verify spec compliance
3. **Explicit Overrides**: Easy to test error cases by disabling auto-assertions
4. **Discoverable**: IDE shows which assertions are auto-generated

---

## Summary

| Topic | Decision | Key Dependency |
|-------|----------|----------------|
| DSL Design | Lambda with receiver pattern | Kotlin stdlib |
| OpenAPI Parsing | Swagger Parser 2.1.x + Multi-Spec Registry | io.swagger.parser.v3:swagger-parser |
| HTTP Client | java.net.http.HttpClient | JDK 21 |
| Value Extraction | JSONPath | com.jayway.jsonpath:json-path |
| Fragments | Named blocks with explicit include | Kotlin stdlib |
| Parameterization | Inline data tables | Kotlin stdlib |
| Schema Validation | networknt json-schema-validator | com.networknt:json-schema-validator |
| Reporting | Pluggable interface | Kotlin stdlib |
| Scenario File Format | Custom Gherkin-inspired with API extensions | Kotlin stdlib (parser) |
| Auto-Assertions | Spec-driven generation with override | Swagger Parser |
