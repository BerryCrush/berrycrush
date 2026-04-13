# Module Structure

BerryCrush is organized as a multi-module Gradle project with clear separation of concerns.

## Project Layout

```
berrycrush/
├── build.gradle.kts          # Root build configuration
├── settings.gradle.kts       # Module declarations
├── gradle/
│   └── libs.versions.toml    # Version catalog
├── berrycrush/              # Library modules
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

### berrycrush/core

**Purpose:** Core library providing standalone scenario execution without framework dependencies.

**Key Packages:**

| Package | Description |
|---------|-------------|
| `org.berrycrush.dsl` | Kotlin DSL for scenario definition |
| `org.berrycrush.model` | Domain model classes |
| `org.berrycrush.scenario` | Scenario file parsing (Lexer, Parser, Loader) |
| `org.berrycrush.openapi` | OpenAPI integration |
| `org.berrycrush.executor` | Scenario execution engine |
| `org.berrycrush.step` | Custom step definitions |
| `org.berrycrush.plugin` | Plugin system |
| `org.berrycrush.report` | Report generation |
| `org.berrycrush.context` | Execution context and variables |
| `org.berrycrush.config` | Configuration classes |
| `org.berrycrush.logging` | HTTP logging |
| `org.berrycrush.exception` | Exception types |
| `org.berrycrush.runner` | Standalone runner |
| `org.berrycrush.assertion` | Assertion generation and schema validation |

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

### berrycrush/junit

**Purpose:** JUnit 5 TestEngine implementation for IDE and build tool integration.

**Key Packages:**

| Package | Description |
|---------|-------------|
| `org.berrycrush.junit` | Annotations and bindings |
| `org.berrycrush.junit.engine` | JUnit TestEngine implementation |
| `org.berrycrush.junit.discovery` | Scenario and fragment discovery |
| `org.berrycrush.junit.spi` | Service provider interfaces |
| `org.berrycrush.junit.plugin` | JUnit-specific plugins |

**Key Classes:**

| Class | Description |
|-------|-------------|
| `BerryCrushTestEngine` | JUnit 5 TestEngine (ENGINE_ID = "berrycrush") |
| `BerryCrushScenarios` | Annotation for scenario locations |
| `BerryCrushConfiguration` | Annotation for test configuration |
| `BerryCrushBindings` | Interface for runtime bindings |
| `BindingsProvider` | SPI for custom bindings creation |

**Dependencies:**
- berrycrush/core
- JUnit Platform Engine
- JUnit Platform Commons

**build.gradle.kts:**
```kotlin
dependencies {
    api(project(":berrycrush:core"))
    api(libs.junit.platform.engine)
    api(libs.junit.platform.commons)
    
    testImplementation(libs.bundles.junit)
    testImplementation(libs.junit.platform.launcher)
}
```

### berrycrush/spring

**Purpose:** Spring Boot integration with dependency injection support.

**Key Packages:**

| Package | Description |
|---------|-------------|
| `org.berrycrush.spring` | Spring integration classes |

**Key Classes:**

| Class | Description |
|-------|-------------|
| `SpringBindingsProvider` | BindingsProvider that retrieves beans from Spring context |
| `SpringContextAdapter` | Bridges BerryCrush and Spring TestContext |
| `BerryCrushContextConfiguration` | Annotation for Spring context integration |
| `SpringStepDiscovery` | Auto-discovers step definitions from Spring beans |

**Dependencies:**
- berrycrush/junit
- Spring Boot Starter Test

**build.gradle.kts:**
```kotlin
dependencies {
    api(project(":berrycrush:junit"))
    api(libs.spring.boot.starter.test)
    
    testImplementation(libs.bundles.junit)
}
```

### samples/petstore

**Purpose:** Demonstrates BerryCrush usage with a complete Spring Boot application.

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
- `PetstoreScenarioTest.kt` - JUnit test with BerryCrush
- `PetstoreBindings.kt` - Runtime bindings configuration
- Various `.scenario` files demonstrating features

### berrycrush/doc

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
./gradlew :berrycrush:core:build
./gradlew :berrycrush:junit:build
./gradlew :berrycrush:spring:build
```

### Run Tests
```bash
./gradlew test                           # All tests
./gradlew :samples:petstore:test         # Sample tests only
```

### Generate Documentation
```bash
./gradlew dokkaHtml                      # API documentation
cd berrycrush/doc && make html          # Sphinx docs
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
| Core | `org.berrycrush:core:VERSION` |
| JUnit | `org.berrycrush:junit:VERSION` |
| Spring | `org.berrycrush:spring:VERSION` |
