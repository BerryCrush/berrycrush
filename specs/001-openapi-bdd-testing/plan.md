# Implementation Plan: OpenAPI BDD Testing Library

**Branch**: `001-openapi-bdd-testing` | **Date**: 2026-04-07 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/001-openapi-bdd-testing/spec.md`

## Summary

Build a functional test library that enables QA engineers to write and execute human-readable BDD scenarios against APIs defined by OpenAPI 3.x specifications. The library uses Kotlin DSL for scenario authoring, Swagger Parser for OpenAPI parsing, and vanilla Java HttpClient for HTTP execution. Scenarios support data flow between steps, reusable fragments, parameterization, and schema validation.

## Technical Context

**Language/Version**: Java 21 + Kotlin 2.3.20 (Kotlin DSL for scenarios)  
**Build System**: Gradle with Kotlin DSL (build.gradle.kts)  
**Primary Dependencies**:
- Swagger Parser 2.1.x (OpenAPI 3.x parsing)
- java.net.http.HttpClient (HTTP execution - vanilla Java)
- Jackson (JSON parsing, bundled with Swagger Parser)
- JUnit 5 (test framework integration)

**Storage**: N/A (stateless library, scenarios stored as files)  
**Testing**: JUnit 5 + kotlin.test  
**Target Platform**: JVM 21+ (library consumed via Gradle/Maven dependency)  
**Project Type**: Library  
**Performance Goals**: Execute 100 scenario steps/second, <50ms overhead per HTTP call  
**Constraints**: No external HTTP client dependencies, minimal dependency footprint  
**Scale/Scope**: Support test suites with 1000+ scenarios, OpenAPI specs with 500+ operations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| **I. Code Quality** | ✅ PASS | Kotlin provides null safety, static typing; Gradle enforces lint via ktlint |
| **II. User Experience** | ✅ PASS | Kotlin DSL provides readable, IDE-supported scenario authoring; clear error messages specified in FR-010 |
| **III. Maintainability** | ✅ PASS | Modular design with parser/executor/reporter separation; single responsibility per module |
| **IV. Testing Standards** | ✅ PASS | Library itself tested via JUnit 5; test pyramid with unit/integration/contract tests |
| **V. Flexibility** | ✅ PASS | Plugin architecture for custom step definitions; configuration for environments; extensible reporters |

**Gate Result**: ✅ PASSED - Proceed to Phase 0

## Project Structure

### Documentation (this feature)

```text
specs/001-openapi-bdd-testing/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (DSL API)
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
lemon-check/
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Multi-module settings
├── gradle/
│   └── libs.versions.toml        # Version catalog
│
├── lemon-check-core/             # Core library module
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/
│       │   │   └── io/github/ktakashi/lemoncheck/
│       │   │       ├── dsl/          # Kotlin DSL for scenarios
│       │   │       │   ├── ScenarioDsl.kt
│       │   │       │   ├── StepDsl.kt
│       │   │       │   └── FragmentDsl.kt
│       │   │       ├── scenario/     # Scenario file parser
│       │   │       │   ├── Lexer.kt
│       │   │       │   ├── Parser.kt
│       │   │       │   ├── Ast.kt
│       │   │       │   ├── ScenarioLoader.kt
│       │   │       │   └── SemanticAnalyzer.kt
│       │   │       ├── openapi/      # OpenAPI parsing
│       │   │       │   ├── OpenApiLoader.kt
│       │   │       │   └── OperationResolver.kt
│       │   │       ├── executor/     # HTTP execution
│       │   │       │   ├── ScenarioExecutor.kt
│       │   │       │   ├── HttpRequestBuilder.kt
│       │   │       │   └── ResponseHandler.kt
│       │   │       ├── context/      # Runtime context
│       │   │       │   ├── ExecutionContext.kt
│       │   │       │   └── ValueExtractor.kt
│       │   │       ├── assertion/    # Assertions & validation
│       │   │       │   ├── Assertions.kt
│       │   │       │   ├── AssertionParser.kt
│       │   │       │   └── SchemaValidator.kt
│       │   │       └── report/       # Result reporting
│       │   │           ├── TestReporter.kt
│       │   │           └── ConsoleReporter.kt
│       │   └── resources/
│       └── test/
│           ├── kotlin/
│           │   └── io/github/ktakashi/lemoncheck/
│           │       ├── dsl/
│           │       ├── scenario/     # Parser tests
│           │       ├── openapi/
│           │       ├── executor/
│           │       └── integration/
│           └── resources/
│               ├── petstore.yaml     # OpenAPI test fixture
│               └── scenarios/        # .scenario test files
│                   ├── valid/
│                   └── invalid/      # Error case tests
│
├── lemon-check-junit/            # JUnit 5 integration module
│   ├── build.gradle.kts
│   └── src/
│       └── main/kotlin/
│           └── io/github/ktakashi/lemoncheck/junit/
│               ├── LemonCheckExtension.kt
│               └── ScenarioTest.kt
│
└── samples/                      # Example scenarios
    └── petstore/
        ├── build.gradle.kts
        └── src/test/kotlin/
            └── PetstoreScenarios.kt
```

**Structure Decision**: Multi-module Gradle project with core library separated from JUnit integration. This enables:
- Core library usable standalone or with other test frameworks
- Clean dependency boundaries
- Independent versioning of integration modules

## Complexity Tracking

No constitution violations requiring justification.

---

## Constitution Re-Check (Post Phase 1 Design)

*Re-evaluated after design artifacts completed.*

| Principle | Status | Post-Design Evidence |
|-----------|--------|---------------------|
| **I. Code Quality** | ✅ PASS | Kotlin null safety, explicit error types (see contracts/kotlin-dsl-api.md), ktlint configured |
| **II. User Experience** | ✅ PASS | DSL API designed for readability (see quickstart.md examples), IDE autocomplete support |
| **III. Maintainability** | ✅ PASS | Clear module boundaries (core/junit/samples), single responsibility per package (see plan structure) |
| **IV. Testing Standards** | ✅ PASS | Test pyramid defined: unit tests per package, integration tests for HTTP, contract tests for DSL |
| **V. Flexibility** | ✅ PASS | Pluggable reporters (research.md R8), configuration DSL, fragment composition |

**Post-Design Gate Result**: ✅ PASSED

---

## Generated Artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Implementation Plan | [plan.md](plan.md) | ✅ Complete |
| Research | [research.md](research.md) | ✅ Complete (10 topics) |
| Data Model | [data-model.md](data-model.md) | ✅ Complete |
| Kotlin DSL API Contract | [contracts/kotlin-dsl-api.md](contracts/kotlin-dsl-api.md) | ✅ Complete |
| Scenario File Format | [contracts/scenario-file-format.md](contracts/scenario-file-format.md) | ✅ Complete |
| Quickstart Guide | [quickstart.md](quickstart.md) | ✅ Complete |
| Tasks | tasks.md | ⏳ Next: `/speckit.tasks` |
