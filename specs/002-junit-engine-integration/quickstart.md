# Quickstart: LemonCheck JUnit Engine

**Feature**: 002-junit-engine-integration  
**Date**: 2026-04-07

## Prerequisites

- JDK 21 or later
- Gradle 8.x or Maven 3.x
- lemon-check library added to your project

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    testImplementation("io.github.ktakashi:lemon-check-junit:0.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.ktakashi</groupId>
    <artifactId>lemon-check-junit</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

## Basic Usage

### Step 1: Create Scenario Files

Create `.scenario` files in `src/test/resources/scenarios/`:

```
# src/test/resources/scenarios/list-pets.scenario

scenario: List all pets
  when I request the list of pets
    call listPets
  then I get a successful response
    assert status 200
    assert $.pets notEmpty
```

### Step 2: Create Test Class

```java
import io.github.ktakashi.lemoncheck.junit.LemonCheckScenarios;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("lemoncheck")
@LemonCheckScenarios(locations = "scenarios/*.scenario")
public class PetApiTest {
    // No test methods needed - scenarios are discovered automatically
}
```

### Step 3: Run Tests

```bash
./gradlew test
```

## Kotlin DSL Approach (Recommended for Spring Boot)

For Spring Boot integration, the recommended approach is using the Kotlin DSL
instead of text-based scenario files, as it allows proper dependency injection:

```kotlin
import io.github.ktakashi.lemoncheck.junit.LemonCheckSpec
import io.github.ktakashi.lemoncheck.junit.ScenarioTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@LemonCheckSpec("petstore.yaml")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PetstoreKotlinTest : ScenarioTest() {
    
    @LocalServerPort
    private var port: Int = 0
    
    override fun configureSuite() {
        configure {
            baseUrl = "http://localhost:$port/api/v1"
        }
    }
    
    override fun defineScenarios() {
        scenario("List all pets") {
            `when`("I request the list of pets") {
                call("listPets")
            }
            then("I receive a list of pets") {
                statusCode(200)
                bodyArrayNotEmpty("$")
            }
        }
    }
}
```

## Text-Based Scenario Integration (Standalone Usage)

For standalone usage without Spring Boot dynamic port injection, you can use
text-based scenario files with static configuration:

### Step 1: Add Dependencies

```kotlin
dependencies {
    testImplementation("io.github.ktakashi:lemon-check-junit:0.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

### Step 2: Create Bindings Class

```java
import io.github.ktakashi.lemoncheck.junit.LemonCheckBindings;
import java.util.Map;

/**
 * Bindings class for scenario tests.
 * Note: The JUnit engine instantiates this via no-arg constructor,
 * so Spring injection won't work here.
 */
public class ApiBindings implements LemonCheckBindings {
    
    @Override
    public Map<String, Object> getBindings() {
        // Use static configuration, or environment variables
        String baseUrl = System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080/api/v1");
        return Map.of("baseUrl", baseUrl);
    }
}
```

### Step 3: Create Test Class

```java
import io.github.ktakashi.lemoncheck.junit.LemonCheckScenarios;
import io.github.ktakashi.lemoncheck.junit.LemonCheckConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("lemoncheck")
@LemonCheckScenarios(locations = "scenarios/*.scenario")
@LemonCheckConfiguration(bindings = ApiBindings.class, openApiSpec = "petstore.yaml")
public class ApiScenarioTest {
    // Scenarios are discovered and executed automatically
}
```

### Step 4: Run Tests

```bash
./gradlew test
```

## Configuration Options

### @LemonCheckScenarios

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| locations | String[] | required | Glob patterns for scenario files |

**Examples**:
```java
// Single location
@LemonCheckScenarios(locations = "scenarios/*.scenario")

// Multiple locations
@LemonCheckScenarios(locations = {"scenarios/pets/*.scenario", "scenarios/auth/*.scenario"})

// Recursive search
@LemonCheckScenarios(locations = "scenarios/**/*.scenario")
```

### @LemonCheckConfiguration

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| bindings | Class | DefaultBindings.class | Custom bindings class |
| openApiSpec | String | "" | OpenAPI spec path override |
| timeout | long | 30000 | Per-scenario timeout (ms) |

**Example**:
```java
@LemonCheckConfiguration(
    bindings = MyBindings.class,
    openApiSpec = "api/petstore.yaml",
    timeout = 60_000L
)
```

## Project Structure

Recommended test resource layout:

```
src/test/resources/
├── scenarios/
│   ├── pets/
│   │   ├── list-pets.scenario
│   │   ├── create-pet.scenario
│   │   └── delete-pet.scenario
│   └── auth/
│       └── login.scenario
├── petstore.yaml          # OpenAPI spec
└── application.yaml       # Test config (if using Spring Boot)
```

## Troubleshooting

### No tests discovered

1. Check that location pattern matches your file structure
2. Verify files have `.scenario` extension
3. Ensure files are in `src/test/resources` (on classpath)

### Bindings class not found

1. Ensure bindings class has a public no-arg constructor
2. For Spring Boot, make sure the class is in the component scan path
3. Check that the class implements `LemonCheckBindings` interface

### Scenario parse errors

Check scenario file syntax. Common issues:
- Missing `spec` declaration
- Invalid JSON in `with body` block
- Typo in operation name (must match OpenAPI operationId)

## Next Steps

- Read the [JUnit Engine API Contract](contracts/junit-engine-api.md) for detailed annotation reference
- See [Petstore Sample](../../samples/petstore/) for a complete working example
- Review [Scenario File Format](../../specs/001-openapi-bdd-testing/contracts/scenario-file-format.md) for scenario syntax
