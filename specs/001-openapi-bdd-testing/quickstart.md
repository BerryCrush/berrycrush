# Quickstart: BerryCrush

Get started with BerryCrush in 5 minutes.

## Prerequisites

- Java 21+
- Gradle 8.5+ (or use included wrapper)
- An OpenAPI 3.x specification file for your API

---

## 1. Add Dependency

**build.gradle.kts**:
```kotlin
dependencies {
    testImplementation("org.berrycrush.berrycrush:berrycrush-core:1.0.0")
    testImplementation("org.berrycrush.berrycrush:berrycrush-junit:1.0.0")  // Optional: JUnit 5 integration
}
```

---

## 2. Add Your OpenAPI Spec

Place your OpenAPI specification in the test resources:

```
src/test/resources/petstore.yaml
```

Example minimal spec:
```yaml
openapi: 3.0.3
info:
  title: Petstore API
  version: 1.0.0
servers:
  - url: https://petstore.example.com/api/v1
paths:
  /pets:
    get:
      operationId: listPets
      summary: List all pets
      responses:
        '200':
          description: A list of pets
          content:
            application/json:
              schema:
                type: object
                properties:
                  pets:
                    type: array
                    items:
                      $ref: '#/components/schemas/Pet'
  /pets/{petId}:
    get:
      operationId: getPetById
      parameters:
        - name: petId
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: A pet
components:
  schemas:
    Pet:
      type: object
      properties:
        id:
          type: integer
        name:
          type: string
        status:
          type: string
```

---

## 3. Write Your First Scenario

**src/test/kotlin/PetstoreScenarios.kt**:
```kotlin
import org.berrycrush.berrycrush.dsl.*
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class PetstoreScenarios {

    private val suite = berryCrush("petstore.yaml") {
        baseUrl = System.getenv("PETSTORE_URL") ?: "https://petstore.example.com/api/v1"
        timeout = 10.seconds
    }

    @Test
    fun `list all pets`() {
        suite.scenario("List all available pets") {
            
            `when`("I request the list of pets") {
                call("listPets")
            }
            
            then("I receive a successful response with pets") {
                statusCode(200)
                bodyArrayNotEmpty("$.pets")
            }
            
        }.run()
    }
}
```

---

## 4. Run the Test

```bash
./gradlew test
```

Output:
```
✓ List all available pets
  ✓ When I request the list of pets
  ✓ Then I receive a successful response with pets

1 scenario passed (0.45s)
```

---

## 5. Chain API Calls with Data Flow

```kotlin
@Test
fun `view pet details after listing`() {
    suite.scenario("Customer views a pet") {
        
        given("pets are available") {
            call("listPets")
            extractTo("firstPetId", "$.pets[0].id")
        }
        
        `when`("I view the pet details") {
            call("getPetById") {
                pathParam("petId", context["firstPetId"])
            }
        }
        
        then("I see the pet information") {
            statusCode(200)
            bodyIsNotNull("$.name")
            matchesSchema()
        }
        
    }.run()
}
```

---

## 6. Use Reusable Fragments

```kotlin
// Define once
val withAvailablePets = fragment("pets are available") {
    given("the store has pets") {
        call("listPets")
        extractTo("petId", "$.pets[0].id")
        extractTo("petName", "$.pets[0].name")
    }
}

// Use in multiple scenarios
@Test
fun `view pet details`() {
    suite.scenario("View pet") {
        include(withAvailablePets)
        
        `when`("viewing pet details") {
            call("getPetById") {
                pathParam("petId", context["petId"])
            }
        }
        
        then("pet name matches") {
            bodyEquals("$.name", context["petName"])
        }
    }.run()
}

@Test
fun `delete a pet`() {
    suite.scenario("Delete pet") {
        include(withAvailablePets)
        
        `when`("deleting the pet") {
            call("deletePet") {
                pathParam("petId", context["petId"])
            }
        }
        
        then("pet is removed") {
            statusCode(204)
        }
    }.run()
}
```

---

## 7. Parameterized Tests

```kotlin
@Test
fun `create pet with various inputs`() {
    suite.scenarioOutline("Validate pet creation") {
        examples(
            row(name = "Buddy", tag = "dog", expectedStatus = 201),
            row(name = "", tag = "cat", expectedStatus = 400),
            row(name = "X".repeat(256), tag = "bird", expectedStatus = 400),
        )
        
        `when`("creating a pet named '<name>' with tag '<tag>'") {
            call("createPet") {
                body("""{"name": "$name", "tag": "$tag"}""")
            }
        }
        
        then("response status is <expectedStatus>") {
            statusCode(expectedStatus)
        }
    }.runAll()
}
```

---

## 8. Text-Based Scenario Files (For Non-Technical Users)

BerryCrush supports human-readable `.scenario` files that can be written by product owners, QA analysts, or business stakeholders without Kotlin knowledge.

### Create a Scenario File

**src/test/resources/scenarios/petstore.scenario**:
```gherkin
@openapi: petstore.yaml
@baseUrl: ${env:PETSTORE_URL}

Feature: Pet Store Customer Journey
  As a customer
  I want to browse and purchase pets
  So that I can find my perfect companion

  Scenario: Browse available pets
    Given the store has pets in stock
      | operation | listPets |
      | query     | status = available |
      | assert    | status = 200 |
      | assert    | $.pets is not empty |

    When I view the first pet's details
      | operation | listPets |
      | extract   | petId -> $.pets[0].id |
      | operation | getPetById |
      | path      | petId = ${petId} |

    Then I see the pet information
      | assert    | status = 200 |
      | assert    | $.name is not null |
      | assert    | schema valid |
```

### Run Scenario Files from Kotlin

```kotlin
import org.berrycrush.berrycrush.dsl.*
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest

class PetstoreScenarioFileTests {

    private val suite = berryCrush("petstore.yaml") {
        baseUrl = System.getenv("PETSTORE_URL")
        // Load all .scenario files from directory
        loadScenariosFrom("src/test/resources/scenarios/")
    }

    @TestFactory
    fun `run all scenario files`(): List<DynamicTest> {
        return suite.scenarios.map { scenario ->
            DynamicTest.dynamicTest(scenario.name) {
                scenario.run()
            }
        }
    }
}
```

### Scenario File with Parameterization

**src/test/resources/scenarios/pet-validation.scenario**:
```gherkin
@openapi: petstore.yaml

Feature: Pet Creation Validation

  Scenario Outline: Validate pet creation inputs
    When I create a pet with name "<name>" and tag "<tag>"
      | operation | createPet |
      | body      | {"name": "<name>", "tag": "<tag>"} |

    Then the response is <status>
      | assert    | status = <status> |

    Examples:
      | name      | tag  | status |
      | Buddy     | dog  | 201    |
      |           | cat  | 400    |
      | X*256     | bird | 400    |
```

### Reusable Fragments

**src/test/resources/fragments/auth.fragment**:
```gherkin
Fragment: authenticate-customer
  Given I have a customer session
    | operation | login |
    | body      | {"email": "${env:TEST_USER}", "password": "${env:TEST_PASS}"} |
    | extract   | authToken -> $.token |
```

Use in scenarios:
```gherkin
  Scenario: Authenticated purchase
    Given I am logged in
      | include | authenticate-customer |

    When I purchase a pet
      | operation | purchasePet |
      | header    | Authorization = Bearer ${authToken} |
```

### Best Practices for Scenario Files

1. **One feature per file** - Keep files focused and manageable
2. **Use meaningful names** - `customer-journey.scenario` not `test1.scenario`
3. **Extract common setups** - Use Background for shared Given steps
4. **Use fragments** - Avoid duplicating auth and setup across features
5. **Tag scenarios** - Use `@smoke`, `@regression`, `@api-name` for filtering

---

## 9. Auto-Assertions (Zero Boilerplate Happy Path)

BerryCrush automatically generates assertions from your OpenAPI spec. For happy path tests, you often need zero explicit assertions:

```kotlin
// OpenAPI spec defines: GET /pets returns 200 with Pet[] schema
// These assertions are AUTO-GENERATED from the spec:
`when`("listing pets") {
    call("listPets")
    // Auto-asserts: statusCode(200), matchesSchema(), Content-Type header
}

// Override for error cases
`when`("fetching non-existent pet") {
    call("getPetById") {
        pathParam("petId", 999999)
        autoAssert(false)  // Disable auto-assertions
    }
    statusCode(404)  // Manual assertion
}
```

**In scenario files**:
```gherkin
  # Happy path - no assertions needed
  When I list pets
    | operation | listPets |

  # Error case - disable auto-assert
  When I request non-existent pet
    | operation   | getPetById |
    | path        | petId = 999999 |
    | auto-assert | false |
    | assert      | status = 404 |
```

---

## 10. Multiple OpenAPI Specs (Microservices)

Test scenarios that span multiple services:

```kotlin
// Multi-spec configuration
val suite = berryCrush {
    spec("petstore", "specs/petstore.yaml") {
        baseUrl = "https://petstore.example.com"
    }
    spec("orders", "specs/orders.yaml") {
        baseUrl = "https://orders.example.com"
    }
    
    timeout = 30.seconds
    environment = "staging"
}

// Cross-service scenario
suite.scenario("Complete purchase flow") {
    given("pet exists") {
        using("petstore")  // Switch to petstore spec
        call("getPetById") { pathParam("petId", 123) }
        extractTo("price", "$.price")
    }
    
    `when`("creating order") {
        using("orders")    // Switch to orders spec
        call("createOrder") {
            body("""{"petId": 123, "price": ${context["price"]}}""")
        }
    }
    
    then("order is confirmed") {
        statusCode(201)
    }
}
```

**In scenario files**:
```gherkin
@openapi: petstore=specs/petstore.yaml, orders=specs/orders.yaml
@baseUrl.petstore: https://petstore.example.com
@baseUrl.orders: https://orders.example.com

Feature: Cross-Service Order Flow

  Scenario: Purchase a pet
    Given pet exists in store
      | using     | petstore |
      | operation | getPetById |
      | path      | petId = 123 |
      | extract   | price -> $.price |

    When order is placed
      | using     | orders |
      | operation | createOrder |
      | body      | {"petId": 123, "price": ${price}} |
```

---

## Configuration Options

```kotlin
val suite = berryCrush("spec.yaml") {
    // Override base URL
    baseUrl = "https://staging.api.example.com"
    
    // Request timeout
    timeout = 30.seconds
    
    // Default headers for all requests
    defaultHeaders["Authorization"] = "Bearer ${System.getenv("API_TOKEN")}"
    defaultHeaders["X-Request-Id"] = UUID.randomUUID().toString()
    
    // Environment name (for reports)
    environment = "staging"
    
    // Strict schema validation (fail on extra fields)
    strictValidation = true
    
    // Auto-assertions from OpenAPI spec
    autoAssertions {
        enabled = true              // Default: true
        statusCode = true           // Auto-assert success status
        schemaValidation = true     // Auto-validate response schema
        contentType = true          // Auto-check Content-Type header
    }
    
    // Preset variables
    variables {
        put("testUserId", "user-123")
    }
    
    // Load scenario files
    loadScenariosFrom("src/test/resources/scenarios/")
    loadFragmentsFrom("src/test/resources/fragments/")
}
```

---

## Next Steps

- See [DSL API Reference](contracts/kotlin-dsl-api.md) for Kotlin DSL documentation
- See [Scenario File Format](contracts/scenario-file-format.md) for text-based scenario syntax
- See [Data Model](data-model.md) for entity definitions
- See [Research](research.md) for design decisions

---

## Troubleshooting

### "Operation not found: xyz"

Ensure the `operationId` in your scenario matches exactly with the OpenAPI spec. Operation IDs are case-sensitive.

### "Cannot extract value at path"

Check that:
1. The JSONPath syntax is correct (use `$.` prefix)
2. The API returned the expected structure
3. The field exists in the response

### "Connection refused"

Verify:
1. The `baseUrl` is correct
2. The API server is running
3. Network/firewall allows the connection
