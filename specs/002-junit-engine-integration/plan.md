# Implementation Plan: JUnit Engine Integration

**Branch**: `002-junit-engine-integration` | **Date**: 2026-04-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-junit-engine-integration/spec.md`

## Summary

Implement a custom JUnit 5 TestEngine (`lemoncheck`) that discovers and executes `.scenario` files based on the `@LemonCheckScenarios` annotation. The engine integrates with the existing lemon-check core module for scenario parsing and execution. Additionally, rewrite the samples/petstore project in Java 21 with Spring Boot 3.x and H2/JPA to demonstrate the integration.

## Technical Context

**Language/Version**: Kotlin 2.3.20 (JUnit engine), Java 21 (sample petstore)  
**Primary Dependencies**: JUnit Platform 5.10.x, Spring Boot 3.x, H2, Spring Data JPA  
**Storage**: H2 in-memory database (sample only)  
**Testing**: JUnit Platform TestEngine, Spring Boot Test, lemon-check scenarios  
**Target Platform**: JVM 21+ (library and sample)
**Project Type**: Library (lemon-check/junit) + Sample application (samples/petstore)  
**Performance Goals**: Scenario discovery < 2s for 100 scenarios, test execution < 30s including Spring context  
**Constraints**: Must maintain backward compatibility with existing LemonCheckExtension; no external runtime dependencies beyond JUnit Platform  
**Scale/Scope**: Single-module engine, sample API with 5 endpoints (listPets, getPetById, createPet, updatePet, deletePet) + auth

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Kotlin follows project conventions; Java 21 sample uses modern idioms |
| II. User Experience | ✅ PASS | Annotation-based discovery aligns with JUnit conventions; minimal configuration |
| III. Maintainability | ✅ PASS | TestEngine separate from existing extension; clear module boundaries |
| IV. Testing Standards | ✅ PASS | Engine itself tested via integration tests; sample demonstrates e2e testing |
| V. Flexibility | ✅ PASS | Configuration via annotations; bindings extensible; no vendor lock-in |

**Gate Result**: PASS - No violations identified.

## Project Structure

### Documentation (this feature)

```text
specs/002-junit-engine-integration/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── junit-engine-api.md
│   └── petstore-api.md
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
lemon-check/
├── core/                           # Existing - scenario parsing & execution
│   └── src/main/kotlin/io/github/ktakashi/lemoncheck/
│       ├── scenario/               # Existing - Lexer, Parser, AST
│       ├── model/                  # Existing - Scenario, Step models
│       └── executor/               # Existing - ScenarioExecutor
└── junit/                          # Target module for JUnit engine
    └── src/
        ├── main/kotlin/io/github/ktakashi/lemoncheck/junit/
        │   ├── Annotations.kt          # Update: Add locations to LemonCheckScenarios
        │   ├── LemonCheckConfiguration.kt  # NEW: Configuration annotation
        │   ├── engine/                  # NEW: TestEngine implementation
        │   │   ├── LemonCheckTestEngine.kt
        │   │   ├── LemonCheckEngineDescriptor.kt
        │   │   ├── ScenarioTestDescriptor.kt
        │   │   └── ScenarioExecutionListener.kt
        │   └── discovery/               # NEW: Scenario discovery
        │       └── ScenarioDiscovery.kt
        └── test/kotlin/                 # Integration tests

samples/
└── petstore/                       # Target: Rewrite in Java 21
    ├── build.gradle.kts            # Update: Spring Boot + JPA + H2 dependencies
    └── src/
        ├── main/java/io/github/ktakashi/samples/petstore/
        │   ├── PetstoreApplication.java
        │   ├── controller/
        │   │   ├── PetController.java
        │   │   └── AuthController.java
        │   ├── entity/
        │   │   └── Pet.java
        │   ├── repository/
        │   │   └── PetRepository.java
        │   ├── service/
        │   │   └── PetService.java
        │   └── dto/
        │       ├── NewPet.java
        │       └── PetResponse.java
        └── test/
            ├── java/io/github/ktakashi/samples/petstore/
            │   └── PetstoreScenarioTest.java
            └── resources/
                ├── scenarios/
                │   ├── list-pets.scenario
                │   ├── create-pet.scenario
                │   ├── get-pet.scenario
                │   ├── update-pet.scenario
                │   └── delete-pet.scenario
                └── petstore.yaml
```

**Structure Decision**: Extends existing multi-module Gradle project structure. JUnit engine in `lemon-check/junit` module uses Kotlin. Sample petstore in `samples/petstore` uses Java 21 with Spring Boot conventions (controller/service/repository layers).

## Complexity Tracking

No constitution violations requiring justification.

## Constitution Re-Check (Post-Design)

| Principle | Status | Validation |
|-----------|--------|------------|
| I. Code Quality | ✅ PASS | Contracts define clear APIs; data model documents all entities |
| II. User Experience | ✅ PASS | Quickstart provides clear onboarding path; annotations are intuitive |
| III. Maintainability | ✅ PASS | Clear separation: engine/discovery/descriptors; backward compatible |
| IV. Testing Standards | ✅ PASS | Sample demonstrates testing patterns; engine tested via integration |
| V. Flexibility | ✅ PASS | LemonCheckBindings interface allows custom configuration |

**Post-Design Gate Result**: PASS - Design artifacts complete and compliant.

## Generated Artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Research | [research.md](research.md) | ✅ Complete |
| Data Model | [data-model.md](data-model.md) | ✅ Complete |
| JUnit Engine API | [contracts/junit-engine-api.md](contracts/junit-engine-api.md) | ✅ Complete |
| Petstore API | [contracts/petstore-api.md](contracts/petstore-api.md) | ✅ Complete |
| Quickstart | [quickstart.md](quickstart.md) | ✅ Complete |
