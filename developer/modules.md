# Module Structure

LemonCheck is organized as a multi-module Gradle project with clear separation of concerns.

## Project Layout

```
lemon-check/
├── build.gradle.kts          # Root build configuration
├── settings.gradle.kts       # Module declarations
├── gradle/
│   └── libs.versions.toml    # Version catalog
├── lemon-check/              # Library modules
│   ├── core/                 # Core library (standalone)
│   ├── junit/                # JUnit 5 integration
│   ├── spring/               # Spring Boot integration
│   └── doc/                  # Sphinx documentation
├── samples/                  # Sample projects
│   └── petstore/             # Petstore API sample
├── specs/                    # Feature specifications
└── developer/                # Developer documentation
```

## Module Dependencies

```
                     ┌─────────────┐
                     │   spring    │
                     │             │
                     │ Spring Boot │
                     │ integration │
                     └──────┬──────┘
                            │
                            ▼
┌─────────────┐      ┌─────────────┐
│   samples   │─────▶│    junit    │
│             │      │             │
│ Sample apps │      │ JUnit 5     │
│             │      │ test engine │
└─────────────┘      └──────┬──────┘
                            │
                            ▼
                     ┌─────────────┐
                     │    core     │
                     │             │
                     │ Standalone  │
                     │ execution   │
                     └─────────────┘
```

## Module Details

### lemon-check/core

**Purpose:** Core library providing standalone scenario execution without framework dependencies.

**Key Packages:**

| Package | Description |
|---------|-------------|
| `io.github.ktakashi.lemoncheck.dsl` | Kotlin DSL for scenario definition |
| `io.github.ktakashi.lemoncheck.model` | Domain model classes |
| `io.github.ktakashi.lemoncheck.scenario` | Scenario file parsing (Lexer, Parser, Loader) |
| `io.github.ktakashi.lemoncheck.openapi` | OpenAPI integration |
| `io.github.ktakashi.lemoncheck.executor` | Scenario execution engine |
| `io.github.ktakashi.lemoncheck.step` | Custom step definitions |
| `io.github.ktakashi.lemoncheck.plugin` | Plugin system |
| `io.github.ktakashi.lemoncheck.report` | Report generation |
| `io.github.ktakashi.lemoncheck.context` | Execution context and variables |
| `io.github.ktakashi.lemoncheck.config` | Configuration classes |
| `io.github.ktakashi.lemoncheck.logging` | HTTP logging |
| `io.github.ktakashi.lemoncheck.exception` | Exception types |
| `io.github.ktakashi.lemoncheck.runner` | Standalone runner |
| `io.github.ktakashi.lemoncheck.assertion` | Assertion generation and schema validation |

**Dependencies:**
- Swagger Parser (OpenAPI parsing)
- JSONPath (response data extraction)
- Jackson (JSON processing)
- json-schema-validator (schema validation)
- JUnit Jupiter (for testing only)

**build.gradle.kts:**
```kotlin
dependencies {
    api(libs.swagger.parser)
    api(libs.json.path)
    api(libs.json.schema.validator)
    api(libs.jackson.kotlin)
    
    testImplementation(libs.bundles.junit)
    testImplementation(libs.kotlin.test.junit5)
}
```

### lemon-check/junit

**Purpose:** JUnit 5 TestEngine implementation for IDE and build tool integration.

**Key Packages:**

| Package | Description |
|---------|-------------|
| `io.github.ktakashi.lemoncheck.junit` | Annotations and bindings |
| `io.github.ktakashi.lemoncheck.junit.engine` | JUnit TestEngine implementation |
| `io.github.ktakashi.lemoncheck.junit.discovery` | Scenario and fragment discovery |
| `io.github.ktakashi.lemoncheck.junit.spi` | Service provider interfaces |
| `io.github.ktakashi.lemoncheck.junit.plugin` | JUnit-specific plugins |

**Key Classes:**

| Class | Description |
|-------|-------------|
| `LemonCheckTestEngine` | JUnit 5 TestEngine (ENGINE_ID = "lemoncheck") |
| `LemonCheckScenarios` | Annotation for scenario locations |
| `LemonCheckConfiguration` | Annotation for test configuration |
| `LemonCheckBindings` | Interface for runtime bindings |
| `BindingsProvider` | SPI for custom bindings creation |

**Dependencies:**
- lemon-check/core
- JUnit Platform Engine
- JUnit Platform Commons

**build.gradle.kts:**
```kotlin
dependencies {
    api(project(":lemon-check:core"))
    api(libs.junit.platform.engine)
    api(libs.junit.platform.commons)
    
    testImplementation(libs.bundles.junit)
    testImplementation(libs.junit.platform.launcher)
}
```

### lemon-check/spring

**Purpose:** Spring Boot integration with dependency injection support.

**Key Packages:**

| Package | Description |
|---------|-------------|
| `io.github.ktakashi.lemoncheck.spring` | Spring integration classes |

**Key Classes:**

| Class | Description |
|-------|-------------|
| `SpringBindingsProvider` | BindingsProvider that retrieves beans from Spring context |
| `SpringContextAdapter` | Bridges LemonCheck and Spring TestContext |
| `LemonCheckContextConfiguration` | Annotation for Spring context integration |
| `SpringStepDiscovery` | Auto-discovers step definitions from Spring beans |

**Dependencies:**
- lemon-check/junit
- Spring Boot Starter Test

**build.gradle.kts:**
```kotlin
dependencies {
    api(project(":lemon-check:junit"))
    api(libs.spring.boot.starter.test)
    
    testImplementation(libs.bundles.junit)
}
```

### samples/petstore

**Purpose:** Demonstrates LemonCheck usage with a complete Spring Boot application.

**Structure:**
```
samples/petstore/
├── src/
│   ├── main/
│   │   ├── kotlin/                    # Java/Kotlin source (API)
│   │   └── resources/
│   │       └── petstore.yaml          # OpenAPI specification
│   └── test/
│       ├── kotlin/                    # Test classes
│       │   └── PetstoreScenarioTest.kt
│       └── resources/
│           ├── scenarios/             # Scenario files
│           └── fragments/             # Fragment files
```

**Key Files:**
- `petstore.yaml` - OpenAPI specification for the sample API
- `PetstoreScenarioTest.kt` - JUnit test with LemonCheck
- `PetstoreBindings.kt` - Runtime bindings configuration
- Various `.scenario` files demonstrating features

### lemon-check/doc

**Purpose:** Sphinx documentation source.

**Structure:**
```
doc/src/sphinx/
├── conf.py                # Sphinx configuration
├── index.rst              # Documentation index
├── quickstart.rst         # Quick start guide
├── tutorial.rst           # Full tutorial
├── migration.rst          # Migration guide
├── troubleshooting.rst    # Troubleshooting guide
└── features/              # Feature documentation
    ├── custom-steps.rst
    ├── fragments.rst
    ├── kotlin-dsl.rst
    ├── logging.rst
    ├── multi-spec.rst
    ├── parameters.rst
    ├── plugins.rst
    ├── reporting.rst
    └── standalone-runner.rst
```

## Build Commands

### Build All Modules
```bash
./gradlew build
```

### Build Specific Module
```bash
./gradlew :lemon-check:core:build
./gradlew :lemon-check:junit:build
./gradlew :lemon-check:spring:build
```

### Run Tests
```bash
./gradlew test                           # All tests
./gradlew :samples:petstore:test         # Sample tests only
```

### Generate Documentation
```bash
./gradlew dokkaHtml                      # API documentation
cd lemon-check/doc && make html          # Sphinx docs
```

### Check Dependencies
```bash
./gradlew dependencyCheckAnalyze         # OWASP vulnerability check
```

## Version Catalog

All dependency versions are managed in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.3.20"
swagger-parser = "2.1.39"
json-path = "3.0.0"
json-schema-validator = "3.0.1"
junit = "6.0.3"
junit-platform = "6.0.3"
jackson = "3.1.1"
spring-boot = "4.0.5"
h2 = "2.3.232"
```

This centralized approach ensures:
- Consistent versions across modules
- Easy version upgrades
- Clear dependency overview

## Publishing Coordinates

When published, modules are available at:

| Module | Maven Coordinates |
|--------|-------------------|
| Core | `io.github.ktakashi.lemoncheck:core:VERSION` |
| JUnit | `io.github.ktakashi.lemoncheck:junit:VERSION` |
| Spring | `io.github.ktakashi.lemoncheck:spring:VERSION` |
