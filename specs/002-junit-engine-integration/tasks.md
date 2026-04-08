# Tasks: JUnit Engine Integration

**Input**: Design documents from `/specs/002-junit-engine-integration/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependency configuration

- [X] T001 Update gradle/libs.versions.toml with JUnit Platform Engine dependencies
- [X] T002 Update lemon-check/junit/build.gradle.kts with JUnit Platform Engine API dependency
- [X] T003 [P] Update samples/petstore/build.gradle.kts with Spring Boot 3.x, H2, JPA, and lemon-check-junit dependencies
- [X] T004 [P] Create samples/petstore/src/main/resources/application.yaml with H2 and JPA configuration

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create LemonCheckBindings interface in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/LemonCheckBindings.kt
- [X] T006 Create DefaultBindings implementation in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/DefaultBindings.kt
- [X] T007 Create META-INF/services/org.junit.platform.engine.TestEngine service file in lemon-check/junit/src/main/resources/

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Run Scenario Tests via JUnit Engine (Priority: P1) 🎯 MVP

**Goal**: Implement custom JUnit 5 TestEngine that discovers and executes `.scenario` files based on `@LemonCheckScenarios` annotation

**Independent Test**: Create test class annotated with `@LemonCheckScenarios(locations = "scenarios/")`, run via JUnit Platform, verify scenario files are discovered and executed

### Implementation for User Story 1

- [X] T008 [US1] Update @LemonCheckScenarios annotation with `locations` parameter in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/Annotations.kt
- [X] T009 [US1] Create LemonCheckEngineDescriptor in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/engine/LemonCheckEngineDescriptor.kt
- [X] T010 [US1] Create ClassTestDescriptor in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/engine/ClassTestDescriptor.kt
- [X] T011 [US1] Create ScenarioTestDescriptor in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/engine/ScenarioTestDescriptor.kt
- [X] T012 [US1] Implement ScenarioDiscovery for classpath .scenario file discovery in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/discovery/ScenarioDiscovery.kt
- [X] T013 [US1] Implement LemonCheckTestEngine.discover() method in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/engine/LemonCheckTestEngine.kt
- [X] T014 [US1] Implement LemonCheckTestEngine.execute() method in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/engine/LemonCheckTestEngine.kt
- [X] T015 [US1] Add error handling for scenario parsing errors and missing files in LemonCheckTestEngine
- [X] T016 [US1] Create integration test for basic scenario discovery in lemon-check/junit/src/test/kotlin/io/github/ktakashi/lemoncheck/junit/engine/LemonCheckTestEngineTest.kt
- [X] T017 [US1] Add test scenario files for integration tests in lemon-check/junit/src/test/resources/scenarios/

**Checkpoint**: JUnit engine discovers and executes `.scenario` files via `@LemonCheckScenarios` annotation

---

## Phase 4: User Story 2 - Configure Scenario Execution (Priority: P2)

**Goal**: Enable custom configuration of scenario execution via `@LemonCheckConfiguration` annotation with bindings support

**Independent Test**: Create test class with `@LemonCheckConfiguration(bindings = MyBindings.class)`, verify custom bindings are applied during scenario execution

### Implementation for User Story 2

- [X] T018 [US2] Create @LemonCheckConfiguration annotation in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/LemonCheckConfiguration.kt
- [X] T019 [US2] Update ClassTestDescriptor to read @LemonCheckConfiguration annotation in lemon-check/junit/src/main/kotlin/io/github/ktakashi/lemoncheck/junit/engine/ClassTestDescriptor.kt
- [X] T020 [US2] Implement bindings instantiation and validation in LemonCheckTestEngine
- [X] T021 [US2] Integrate bindings with scenario execution context in LemonCheckTestEngine.execute()
- [X] T022 [US2] Add timeout support from @LemonCheckConfiguration to scenario execution
- [X] T023 [US2] Add error handling for invalid or missing bindings class
- [X] T024 [US2] Create integration test for custom bindings in lemon-check/junit/src/test/kotlin/io/github/ktakashi/lemoncheck/junit/engine/LemonCheckConfigurationTest.kt

**Checkpoint**: Custom bindings and configuration work via `@LemonCheckConfiguration` annotation

---

## Phase 5: User Story 3 & 4 - Spring Boot Integration + Petstore Sample (Priority: P3)

**Goal**: Demonstrate JUnit engine integration with Spring Boot via complete petstore sample implementation

**Independent Test**: Run `./gradlew :samples:petstore:test`, verify all scenario tests pass against the H2-backed Spring Boot API

### Petstore Application Implementation

- [X] T025 [P] [US4] Create PetstoreApplication main class in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/PetstoreApplication.java
- [X] T026 [P] [US4] Create PetStatus enum in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/entity/PetStatus.java
- [X] T027 [US4] Create Pet JPA entity in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/entity/Pet.java
- [X] T028 [P] [US4] Create NewPet DTO in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/dto/NewPet.java
- [X] T029 [P] [US4] Create PetResponse DTO in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/dto/PetResponse.java
- [X] T030 [P] [US4] Create ErrorResponse DTO in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/dto/ErrorResponse.java
- [X] T031 [US4] Create PetRepository interface in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/repository/PetRepository.java
- [X] T032 [US4] Create PetService with CRUD operations in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/service/PetService.java
- [X] T033 [US4] Create PetController with REST endpoints in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/controller/PetController.java
- [X] T034 [US4] Create AuthController for login endpoint in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/controller/AuthController.java
- [X] T035 [US4] Add validation and exception handling in samples/petstore/src/main/java/io/github/ktakashi/samples/petstore/exception/GlobalExceptionHandler.java

### Petstore Test Resources

- [X] T036 [P] [US4] Copy petstore.yaml to samples/petstore/src/test/resources/petstore.yaml
- [X] T037 [P] [US4] Create list-pets.scenario in samples/petstore/src/test/resources/scenarios/list-pets.scenario
- [X] T038 [P] [US4] Create get-pet.scenario in samples/petstore/src/test/resources/scenarios/get-pet.scenario
- [X] T039 [P] [US4] Create create-pet.scenario in samples/petstore/src/test/resources/scenarios/create-pet.scenario
- [X] T040 [P] [US4] Create update-pet.scenario in samples/petstore/src/test/resources/scenarios/update-pet.scenario
- [X] T041 [P] [US4] Create delete-pet.scenario in samples/petstore/src/test/resources/scenarios/delete-pet.scenario

### Spring Boot Test Integration

- [X] T042 [US3] Create PetstoreBindings class implementing LemonCheckBindings in samples/petstore/src/test/java/io/github/ktakashi/samples/petstore/PetstoreBindings.java
- [X] T043 [US3] Create PetstoreScenarioTest with @SpringBootTest and @IncludeEngines annotations in samples/petstore/src/test/java/io/github/ktakashi/samples/petstore/PetstoreScenarioTest.java
- [X] T044 [US3] Add test data initialization in samples/petstore/src/test/resources/data.sql (if needed)
- [X] T045 [US3] Verify all scenario tests pass with `./gradlew :samples:petstore:test`

**Checkpoint**: Complete petstore sample demonstrates JUnit engine with Spring Boot integration

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, cleanup, and final verification

- [X] T046 [P] Update quickstart.md with any adjustments from implementation
- [X] T047 [P] Add KDoc/Javadoc to public API classes (annotations, LemonCheckBindings interface)
- [X] T048 Run full test suite: `./gradlew test`
- [ ] T049 Verify IDE integration (IntelliJ scenario test discovery and execution)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) completion - BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational (Phase 2) completion
- **US2 (Phase 4)**: Depends on US1 (Phase 3) completion (builds on engine infrastructure)
- **US3/US4 (Phase 5)**: Depends on US2 (Phase 4) completion (needs configuration support for bindings)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

```text
Setup (Phase 1)
     │
     ▼
Foundational (Phase 2) ─────────────────────┐
     │                                       │
     ▼                                       │
US1: JUnit Engine (Phase 3) ◀────────────────┘
     │
     ▼
US2: Configuration (Phase 4)
     │
     ▼
US3/US4: Spring Boot + Petstore (Phase 5)
     │
     ▼
Polish (Phase 6)
```

### Parallel Opportunities

**Phase 1 (Setup)**:
- T002, T003, T004 can run in parallel after T001

**Phase 2 (Foundational)**:
- T005, T006, T007 can run in parallel

**Phase 5 (Petstore)**:
- T025, T026 can run in parallel (application + enum)
- T028, T029, T030 can run in parallel (all DTOs)
- T036, T037, T038, T039, T040, T041 can run in parallel (all test resources)

**Phase 6 (Polish)**:
- T046, T047 can run in parallel

---

## Implementation Strategy

### MVP Scope (Minimum Viable Product)

- **Phase 1**: Setup
- **Phase 2**: Foundational
- **Phase 3**: User Story 1 (JUnit Engine core)

MVP delivers: Working JUnit 5 TestEngine that discovers and executes `.scenario` files via `@LemonCheckScenarios` annotation.

### Incremental Delivery

1. **Increment 1 (MVP)**: Phases 1-3 → Basic scenario test execution
2. **Increment 2**: Phase 4 → Custom configuration and bindings
3. **Increment 3**: Phase 5 → Full Spring Boot sample demonstration
4. **Increment 4**: Phase 6 → Polish and documentation

---

## Summary

| Phase | Tasks | Parallel Tasks |
|-------|-------|----------------|
| 1: Setup | 4 | 3 |
| 2: Foundational | 3 | 3 |
| 3: US1 - JUnit Engine | 10 | 0 |
| 4: US2 - Configuration | 7 | 0 |
| 5: US3/US4 - Spring Boot + Petstore | 21 | 14 |
| 6: Polish | 4 | 2 |
| **Total** | **49** | **22** |

**Format Validation**: All 49 tasks follow the checklist format with checkbox, ID, optional [P] marker, [Story] label for user story phases, and file paths.
