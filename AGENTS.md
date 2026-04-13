# AGENTS.md - BerryCrush Development Guide

This document provides guidance for AI agents and developers working on the BerryCrush codebase.

## Project Overview

BerryCrush is an OpenAPI-driven BDD-style API testing library for Kotlin and Java. It enables writing human-readable test scenarios that automatically validate APIs against their OpenAPI specifications.

### Key Features
- BDD-style test scenarios with Gherkin-like syntax
- Automatic request/response validation against OpenAPI schemas
- Variable extraction and interpolation
- Reusable test fragments
- JUnit 5 TestEngine integration
- Spring Boot integration

## Module Structure

```
berrycrush/
├── core/                    # Standalone execution engine
│   ├── src/main/kotlin/
│   │   └── org.berrycrush.berrycrush/
│   │       ├── config/      # Configuration classes
│   │       ├── context/     # Execution context
│   │       ├── executor/    # Scenario execution
│   │       ├── model/       # Domain models
│   │       ├── openapi/     # OpenAPI integration
│   │       ├── plugin/      # Plugin system
│   │       └── scenario/    # Lexer, Parser, AST
│   └── src/test/kotlin/     # Unit tests
├── junit/                   # JUnit 5 TestEngine integration
│   ├── src/main/kotlin/     # TestEngine, TestDescriptor
│   └── src/test/kotlin/     # Integration tests
├── spring/                  # Spring Boot integration
│   ├── src/main/kotlin/     # Auto-configuration
│   └── src/test/kotlin/     # Spring tests
└── doc/                     # Sphinx documentation
```

## Technology Stack

| Technology | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.3.20 | Primary language |
| Java | 21 | Target JVM |
| JUnit Platform | 6.0.3 | Test engine integration |
| Spring Boot | 4.0.5 | Optional integration |
| Gradle | 9.x | Build system (Kotlin DSL) |
| ktlint | 14.0.1 | Code formatting |
| Swagger Parser | 2.1.39 | OpenAPI parsing |
| Jackson | 3.1.1 | JSON processing |
| JSONPath | 3.0.0 | JSON querying |

## Sample Project

```
samples/petstore/
├── src/main/kotlin/         # Sample Pet Store API (Spring Boot)
├── src/test/resources/
│   ├── scenarios/           # Test scenario files (.scenario)
│   └── openapi.yaml         # OpenAPI specification
└── berrycrush/
    └── openapi/             # OpenAPI specs for tests
```

## Core Components

### Scenario Execution Flow

1. **Lexer** (`Lexer.kt`) - Tokenizes scenario source
2. **Parser** (`Parser.kt`) - Builds AST from tokens
3. **ScenarioLoader** (`ScenarioLoader.kt`) - Transforms AST to runtime model
4. **ScenarioExecutor** (`ScenarioExecutor.kt`) - Executes against HTTP endpoints
5. **SpecRegistry** (`SpecRegistry.kt`) - Manages OpenAPI specifications

### Key Classes

| Class | Purpose |
|-------|---------|
| `Scenario` | Runtime model of a test scenario |
| `Step` | Individual test step with actions |
| `CallNode` | API call action (AST) |
| `AssertNode` | Assertion action (AST) |
| `ExecutionContext` | Variables and state during execution |
| `Configuration` | Runtime configuration |

## Build Commands

```bash
# Build all modules
./gradlew build

# Run all tests
./gradlew test

# Run specific module tests
./gradlew :berrycrush:core:test

# Format code
./gradlew ktlintFormat

# Check code style
./gradlew ktlintCheck

# Generate documentation
./gradlew :berrycrush:doc:sphinx
```

## Test Commands

```bash
# Run all tests
./gradlew test

# Run petstore sample tests
./gradlew :samples:petstore:test

# Run specific test
./gradlew :samples:petstore:test --tests "*CreatePet*"

# Run with verbose output
./gradlew test --info
```

## Commit Message Format

Use conventional commits:

```
type(scope): description

[optional body]
```

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`

**Scopes:** `core`, `junit`, `spring`, `docs`, `build`

**Examples:**
```
feat(core): add support for parameterized scenarios
fix(junit): resolve scenario discovery race condition
docs: update developer guide with architecture diagram
```

## Debugging Tips

1. **Enable HTTP logging:**
   ```
   parameters:
     logRequests: true
     logResponses: true
   ```

2. **Verbose test output:**
   ```bash
   ./gradlew test --info
   ```

3. **Debug single test:**
   ```bash
   ./gradlew test --tests "*.MyTest" --debug-jvm
   ```

## Security Best Practices

1. **Mask sensitive headers in logs** - Authorization, X-Api-Key, Cookie
2. **No credentials in code** - Use environment variables or secure vaults
3. **Dependency scanning:**
   ```bash
   ./gradlew dependencyCheckAnalyze
   ```

## Related Documentation

- **Kotlin Coding Guidelines**: See `.github/instructions/kotlin.instructions.md`
- **Scenario Syntax**: See `.github/skills/berrycrush/SKILL.md`
- **Developer Docs**: See `developer/` directory
- **Architecture**: See `developer/architecture.md`
