# Tasks: OpenAPI BDD Testing Library (LemonCheck)

**Input**: Design documents from `/specs/001-openapi-bdd-testing/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Tests are included as the library itself is a testing framework - we need to verify it works correctly.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story this task belongs to (US1, US2, etc.)
- Exact file paths based on plan.md project structure

## Path Conventions

Multi-module Gradle project:
- `lemon-check/core/src/main/kotlin/io/lemoncheck/` - Core library
- `lemon-check/core/src/test/kotlin/io/lemoncheck/` - Core tests
- `lemon-check/junit/src/main/kotlin/io/lemoncheck/junit/` - JUnit integration
- `samples/petstore/src/test/kotlin/` - Example scenarios

---

## Phase 1: Setup (Project Infrastructure)

**Purpose**: Initialize Gradle multi-module project structure with all dependencies

- [ ] T001 Create root build.gradle.kts with Kotlin 2.3.20 and Java 21 configuration
- [ ] T002 Create settings.gradle.kts with multi-module structure (lemon-check/core, lemon-check/junit, samples/petstore)
- [ ] T003 [P] Create gradle/libs.versions.toml with version catalog (swagger-parser 2.1.22, json-path 2.9.0, json-schema-validator 1.4.0, junit 5.10.x)
- [ ] T004 [P] Create lemon-check/core/build.gradle.kts with dependencies
- [ ] T005 [P] Create lemon-check/junit/build.gradle.kts with JUnit 5 dependencies
- [ ] T006 [P] Create samples/petstore/build.gradle.kts with test configuration
- [ ] T007 [P] Configure ktlint for code formatting in root build.gradle.kts
- [ ] T008 Create lemon-check/core/src/test/resources/petstore.yaml OpenAPI test fixture

**Checkpoint**: `./gradlew build` compiles successfully with empty modules

---

## Phase 2: Foundational (Core Infrastructure)

**Purpose**: Core infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Core Models

- [ ] T009 [P] Create StepType enum in lemon-check/core/src/main/kotlin/io/lemoncheck/model/StepType.kt
- [ ] T010 [P] Create ResultStatus enum in lemon-check/core/src/main/kotlin/io/lemoncheck/model/ResultStatus.kt
- [ ] T011 [P] Create Scenario data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/Scenario.kt
- [ ] T012 [P] Create Step data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/Step.kt
- [ ] T013 [P] Create Extraction data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/Extraction.kt
- [ ] T014 [P] Create Assertion data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/Assertion.kt
- [ ] T015 [P] Create StepResult data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/StepResult.kt
- [ ] T016 [P] Create ScenarioResult data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/ScenarioResult.kt
- [ ] T017 [P] Create Configuration data class in lemon-check/core/src/main/kotlin/io/lemoncheck/config/Configuration.kt

### OpenAPI Integration

- [ ] T018 Create OpenApiLoader class in lemon-check/core/src/main/kotlin/io/lemoncheck/openapi/OpenApiLoader.kt (wraps Swagger Parser)
- [ ] T019 Create OperationResolver class in lemon-check/core/src/main/kotlin/io/lemoncheck/openapi/OperationResolver.kt (resolves operationId to path+method)
- [ ] T020 Create SpecRegistry class in lemon-check/core/src/main/kotlin/io/lemoncheck/openapi/SpecRegistry.kt (multi-spec support)
- [ ] T021 [P] Create unit tests for OpenApiLoader in lemon-check/core/src/test/kotlin/io/lemoncheck/openapi/OpenApiLoaderTest.kt
- [ ] T022 [P] Create unit tests for OperationResolver in lemon-check/core/src/test/kotlin/io/lemoncheck/openapi/OperationResolverTest.kt

### HTTP Execution Foundation

- [ ] T023 Create HttpRequestBuilder class in lemon-check/core/src/main/kotlin/io/lemoncheck/executor/HttpRequestBuilder.kt (uses java.net.http.HttpClient)
- [ ] T024 Create ResponseHandler class in lemon-check/core/src/main/kotlin/io/lemoncheck/executor/ResponseHandler.kt
- [ ] T025 [P] Create unit tests for HttpRequestBuilder in lemon-check/core/src/test/kotlin/io/lemoncheck/executor/HttpRequestBuilderTest.kt

### Execution Context

- [ ] T026 Create ExecutionContext class in lemon-check/core/src/main/kotlin/io/lemoncheck/context/ExecutionContext.kt
- [ ] T027 [P] Create unit tests for ExecutionContext in lemon-check/core/src/test/kotlin/io/lemoncheck/context/ExecutionContextTest.kt

### Error Handling

- [ ] T028 [P] Create LemonCheckException base class in lemon-check/core/src/main/kotlin/io/lemoncheck/exception/Exceptions.kt
- [ ] T029 [P] Create OperationNotFoundException in lemon-check/core/src/main/kotlin/io/lemoncheck/exception/Exceptions.kt
- [ ] T030 [P] Create ExtractionException in lemon-check/core/src/main/kotlin/io/lemoncheck/exception/Exceptions.kt
- [ ] T031 [P] Create AssertionFailedException in lemon-check/core/src/main/kotlin/io/lemoncheck/exception/Exceptions.kt

**Checkpoint**: Foundation ready - all core models compiled, OpenAPI loading works, HTTP client configured

---

## Phase 3: User Story 1 - Execute a Simple BDD Scenario (Priority: P1) 🎯 MVP

**Goal**: QA engineer can write and run a basic BDD scenario against an API defined in OpenAPI spec

**Independent Test**: Load petstore.yaml, write scenario with Given/When/Then calling listPets, verify pass/fail output

### DSL Implementation for US1

- [ ] T032 [US1] Create @DslMarker annotation LemonCheckDsl in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/DslMarker.kt
- [ ] T033 [US1] Create LemonCheckSuite class in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/LemonCheckSuite.kt
- [ ] T034 [US1] Create lemonCheck() entry point function in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/LemonCheckDsl.kt
- [ ] T035 [US1] Create ScenarioScope class with given/when/then functions in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/ScenarioScope.kt
- [ ] T036 [US1] Create StepScope class with call() function in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/StepScope.kt
- [ ] T037 [US1] Create CallScope class with pathParam/queryParam/header/body in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/CallScope.kt

### Basic Assertions for US1

- [ ] T038 [US1] Implement statusCode() assertion in StepScope in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/StepScope.kt
- [ ] T039 [US1] Implement bodyContains() assertion in StepScope
- [ ] T040 [US1] Implement bodyEquals() assertion using JSONPath in StepScope
- [ ] T041 [US1] Implement headerExists() and headerEquals() assertions in StepScope

### Scenario Execution for US1

- [ ] T042 [US1] Create ScenarioExecutor class in lemon-check/core/src/main/kotlin/io/lemoncheck/executor/ScenarioExecutor.kt
- [ ] T043 [US1] Implement step-by-step execution with result tracking in ScenarioExecutor
- [ ] T044 [US1] Implement Scenario.run() extension function in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/ScenarioExtensions.kt

### Reporting for US1

- [ ] T045 [US1] Create TestReporter interface in lemon-check/core/src/main/kotlin/io/lemoncheck/report/TestReporter.kt
- [ ] T046 [US1] Create ConsoleReporter implementation in lemon-check/core/src/main/kotlin/io/lemoncheck/report/ConsoleReporter.kt

### Tests for US1

- [ ] T047 [P] [US1] Create DSL contract test in lemon-check/core/src/test/kotlin/io/lemoncheck/dsl/LemonCheckDslTest.kt
- [ ] T048 [P] [US1] Create ScenarioExecutor unit test in lemon-check/core/src/test/kotlin/io/lemoncheck/executor/ScenarioExecutorTest.kt
- [ ] T049 [US1] Create integration test with petstore.yaml in lemon-check/core/src/test/kotlin/io/lemoncheck/integration/SimpleScenarioIntegrationTest.kt

**Checkpoint**: User Story 1 complete - can execute `scenario("name") { given { call("op") } then { statusCode(200) } }.run()`

---

## Phase 4: User Story 2 - Chain API Calls with Data Flow (Priority: P1)

**Goal**: QA engineer can chain multiple API calls where data from one response flows into the next request

**Independent Test**: Call listPets → extract first pet ID → call getPetById with that ID → verify response

### Value Extraction for US2

- [ ] T050 [US2] Create ValueExtractor class using JSONPath in lemon-check/core/src/main/kotlin/io/lemoncheck/context/ValueExtractor.kt
- [ ] T051 [US2] Implement extractTo() function in StepScope for variable extraction
- [ ] T052 [US2] Implement context property in StepScope for variable access
- [ ] T053 [US2] Add variable interpolation support in CallScope (body, pathParam, etc.)

### Context Flow for US2

- [ ] T054 [US2] Implement context passing between steps in ScenarioExecutor
- [ ] T055 [US2] Add lastResponse property to ExecutionContext for implicit access
- [ ] T056 [US2] Implement variable resolution in HTTP request building

### Tests for US2

- [ ] T057 [P] [US2] Create ValueExtractor unit test in lemon-check/core/src/test/kotlin/io/lemoncheck/context/ValueExtractorTest.kt
- [ ] T058 [US2] Create data flow integration test in lemon-check/core/src/test/kotlin/io/lemoncheck/integration/DataFlowIntegrationTest.kt

**Checkpoint**: User Story 2 complete - `extractTo("id", "$.id")` then `pathParam("petId", context["id"])` works

---

## Phase 5: User Story 3 - Reuse Scenario Fragments (Priority: P2)

**Goal**: QA engineer can define reusable step groups and include them in multiple scenarios

**Independent Test**: Define auth fragment, use in two scenarios, verify both get the auth token

### Fragment DSL for US3

- [ ] T059 [US3] Create Fragment data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/Fragment.kt
- [ ] T060 [US3] Create FragmentScope class in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/FragmentScope.kt
- [ ] T061 [US3] Create fragment() builder function in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/FragmentDsl.kt
- [ ] T062 [US3] Implement include() function in ScenarioScope
- [ ] T063 [US3] Implement Background support in LemonCheckSuite for shared setup

### Fragment Execution for US3

- [ ] T064 [US3] Add fragment resolution in ScenarioExecutor
- [ ] T065 [US3] Implement context sharing between included fragments and scenarios

### Tests for US3

- [ ] T066 [P] [US3] Create Fragment DSL test in lemon-check/core/src/test/kotlin/io/lemoncheck/dsl/FragmentDslTest.kt
- [ ] T067 [US3] Create fragment reuse integration test in lemon-check/core/src/test/kotlin/io/lemoncheck/integration/FragmentIntegrationTest.kt

**Checkpoint**: User Story 3 complete - `val authFragment = fragment("auth") { ... }; scenario { include(authFragment) }` works

---

## Phase 6: User Story 4 - Parameterize Scenarios (Priority: P2)

**Goal**: QA engineer can run the same scenario with different input data from a table

**Independent Test**: Create scenarioOutline with 3 data rows, verify it runs 3 times with correct substitutions

### Parameterization DSL for US4

- [ ] T068 [US4] Create ExampleRow data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/ExampleRow.kt
- [ ] T069 [US4] Create ScenarioOutlineScope class in lemon-check/core/src/main/kotlin/io/lemoncheck/dsl/ScenarioOutlineScope.kt
- [ ] T070 [US4] Create scenarioOutline() builder function in LemonCheckSuite
- [ ] T071 [US4] Implement examples() and row() functions in ScenarioOutlineScope
- [ ] T072 [US4] Add parameter placeholder substitution (<name> syntax) in step execution

### Parameterized Execution for US4

- [ ] T073 [US4] Implement scenario expansion from outline + examples in ScenarioExecutor
- [ ] T074 [US4] Add per-row result tracking with parameter values in ScenarioResult
- [ ] T075 [US4] Update ConsoleReporter to show parameter values in output

### Tests for US4

- [ ] T076 [P] [US4] Create ScenarioOutline DSL test in lemon-check/core/src/test/kotlin/io/lemoncheck/dsl/ScenarioOutlineDslTest.kt
- [ ] T077 [US4] Create parameterized scenario integration test in lemon-check/core/src/test/kotlin/io/lemoncheck/integration/ParameterizedScenarioIntegrationTest.kt

**Checkpoint**: User Story 4 complete - `scenarioOutline { examples(row(...), row(...)) }` generates multiple test runs

---

## Phase 7: User Story 5 - Validate Response Against OpenAPI Schema (Priority: P3)

**Goal**: QA engineer can validate API responses match OpenAPI schema definitions automatically

**Independent Test**: Call API, enable schema validation, verify type mismatches are reported

### Schema Validation for US5

- [ ] T078 [US5] Create SchemaValidator class in lemon-check/core/src/main/kotlin/io/lemoncheck/assertion/SchemaValidator.kt (uses networknt json-schema-validator)
- [ ] T079 [US5] Create ValidationError data class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/ValidationError.kt
- [ ] T080 [US5] Implement matchesSchema() assertion in StepScope
- [ ] T081 [US5] Add strict vs lenient validation modes to Configuration

### Auto-Assertions for US5

- [ ] T082 [US5] Create AssertionGenerator class in lemon-check/core/src/main/kotlin/io/lemoncheck/assertion/AssertionGenerator.kt
- [ ] T083 [US5] Implement auto-assertion extraction from OpenAPI responses (status code, schema, content-type)
- [ ] T084 [US5] Add autoAssert(Boolean) control in CallScope
- [ ] T085 [US5] Add autoAssertions configuration block in Configuration

### Tests for US5

- [ ] T086 [P] [US5] Create SchemaValidator unit test in lemon-check/core/src/test/kotlin/io/lemoncheck/assertion/SchemaValidatorTest.kt
- [ ] T087 [P] [US5] Create AssertionGenerator unit test in lemon-check/core/src/test/kotlin/io/lemoncheck/assertion/AssertionGeneratorTest.kt
- [ ] T088 [US5] Create schema validation integration test in lemon-check/core/src/test/kotlin/io/lemoncheck/integration/SchemaValidationIntegrationTest.kt

**Checkpoint**: User Story 5 complete - `matchesSchema()` validates responses, auto-assertions derive from spec

---

## Phase 8: Scenario File Parser (Text-Based Scenarios)

**Goal**: Enable non-technical users to write .scenario files that are parsed and executed

**Purpose**: Extends accessibility of the library to QA analysts and business stakeholders

### Lexer & Parser

- [ ] T089 [P] Create Token types in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/Token.kt
- [ ] T090 [P] Create SourceLocation data class in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/SourceLocation.kt
- [ ] T091 [P] Create ParseError data class in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/ParseError.kt
- [ ] T092 Create Lexer class in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/Lexer.kt
- [ ] T093 Create AST data classes (FeatureFile, Feature, Background, ParsedScenario, ParsedStep) in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/Ast.kt
- [ ] T094 Create Parser class (recursive descent) in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/Parser.kt
- [ ] T095 Create DirectiveType enum in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/DirectiveType.kt
- [ ] T096 Create StepDirective data class in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/StepDirective.kt

### Semantic Analysis

- [ ] T097 Create SemanticAnalyzer class in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/SemanticAnalyzer.kt (validates operationIds exist)
- [ ] T098 Create AssertionParser class in lemon-check/core/src/main/kotlin/io/lemoncheck/assertion/AssertionParser.kt (parses assertion expressions)

### Scenario Loading

- [ ] T099 Create ScenarioLoader class in lemon-check/core/src/main/kotlin/io/lemoncheck/scenario/ScenarioLoader.kt
- [ ] T100 Implement loadScenariosFrom() function in LemonCheckSuite
- [ ] T101 Implement loadFragmentsFrom() function in LemonCheckSuite
- [ ] T102 Create AST to executable Scenario transformer

### Parser Tests

- [ ] T103 [P] Create Lexer unit tests in lemon-check/core/src/test/kotlin/io/lemoncheck/scenario/LexerTest.kt
- [ ] T104 [P] Create Parser unit tests in lemon-check/core/src/test/kotlin/io/lemoncheck/scenario/ParserTest.kt
- [ ] T105 [P] Create test fixtures in lemon-check/core/src/test/resources/scenarios/valid/ and invalid/
- [ ] T106 Create ScenarioLoader integration test in lemon-check/core/src/test/kotlin/io/lemoncheck/scenario/ScenarioLoaderTest.kt

**Checkpoint**: `.scenario` files can be parsed and executed alongside Kotlin DSL scenarios

---

## Phase 9: JUnit 5 Integration

**Goal**: Seamless integration with JUnit 5 test framework for IDE and CI support

### JUnit Extension

- [ ] T107 [P] Create LemonCheckExtension class in lemon-check/junit/src/main/kotlin/io/lemoncheck/junit/LemonCheckExtension.kt
- [ ] T108 [P] Create @LemonCheckSpec annotation in lemon-check/junit/src/main/kotlin/io/lemoncheck/junit/Annotations.kt
- [ ] T109 Create ScenarioTest base class in lemon-check/junit/src/main/kotlin/io/lemoncheck/junit/ScenarioTest.kt
- [ ] T110 Implement dynamic test generation from scenarios for @TestFactory

### JUnit Tests

- [ ] T111 [P] Create LemonCheckExtension tests in lemon-check/junit/src/test/kotlin/io/lemoncheck/junit/LemonCheckExtensionTest.kt

**Checkpoint**: Scenarios can run as JUnit 5 tests with IDE integration

---

## Phase 10: Sample Project & Polish

**Goal**: Provide working examples and finalize documentation

### Sample Petstore Scenarios (Kotlin DSL)

- [ ] T112 [P] Create PetstoreKotlinScenarios.kt in samples/petstore/src/test/kotlin/ demonstrating US1-US5
- [ ] T113 [P] Create petstore.yaml OpenAPI spec in samples/petstore/src/test/resources/

### Sample Petstore Scenarios (Text Files)

- [ ] T114 [P] Create petstore-crud.scenario in samples/petstore/src/test/resources/scenarios/
- [ ] T115 [P] Create auth.fragment in samples/petstore/src/test/resources/fragments/

### Additional Assertions & Features

- [ ] T116 [P] Implement bodyArraySize() assertion in StepScope
- [ ] T117 [P] Implement bodyArrayNotEmpty() assertion in StepScope
- [ ] T118 [P] Implement bodyMatches() regex assertion in StepScope
- [ ] T119 [P] Implement responseTime() assertion in StepScope
- [ ] T120 [P] Add bearerToken(), basicAuth(), apiKey() authentication shortcuts in CallScope

### JSON Reporter

- [ ] T121 Create JsonReporter implementation in lemon-check/core/src/main/kotlin/io/lemoncheck/report/JsonReporter.kt
- [ ] T122 Create TestReport aggregation class in lemon-check/core/src/main/kotlin/io/lemoncheck/model/TestReport.kt

### Multi-Spec Support Enhancement

- [ ] T123 Implement using() function in StepScope for spec switching
- [ ] T124 Add spec-specific Configuration support
- [ ] T125 Implement auto-resolution when operationId is unique across specs

### Final Validation

- [ ] T126 Run full integration test suite with samples
- [ ] T127 Verify all quickstart.md examples work correctly

**Checkpoint**: Library complete with samples, all user stories functional

---

## Dependencies Summary

```
Phase 1 (Setup) ──────────────────────────────────────────────────►
                                                                   │
Phase 2 (Foundation) ─────────────────────────────────────────────►│
                                                                   │
        ┌──────────────────────────────────────────────────────────┤
        │                                                          │
        ▼                                                          │
Phase 3 (US1: Basic Execution) ───────────────────────────────────►│
        │                                                          │
        ▼                                                          │
Phase 4 (US2: Data Flow) ─────────────────────────────────────────►│
        │                                                          │
        ├───────────────────┬──────────────────────────────────────┤
        │                   │                                      │
        ▼                   ▼                                      │
Phase 5 (US3)         Phase 6 (US4)                                │
(Fragments)           (Parameterization)                           │
        │                   │                                      │
        └───────┬───────────┘                                      │
                │                                                  │
                ▼                                                  │
        Phase 7 (US5: Schema Validation) ─────────────────────────►│
                │                                                  │
        ┌───────┴───────────────────────────────────────────┐      │
        │                                                   │      │
        ▼                                                   ▼      │
Phase 8 (Parser)                                    Phase 9 (JUnit)│
        │                                                   │      │
        └───────────────────┬───────────────────────────────┘      │
                            │                                      │
                            ▼                                      │
                    Phase 10 (Samples & Polish) ──────────────────►│
```

## Parallel Execution Opportunities

**Within each phase**, tasks marked with [P] can run in parallel:

- **Phase 1**: T003-T007 (all Gradle configs)
- **Phase 2**: T009-T017 (all models), T021-T022, T025, T027-T031
- **Phase 3**: T047, T048
- **Phase 4**: T057
- **Phase 5**: T066
- **Phase 6**: T076
- **Phase 7**: T086, T087
- **Phase 8**: T089-T091, T103-T105
- **Phase 9**: T107, T108, T111
- **Phase 10**: T112-T120

## Implementation Strategy

**MVP Scope (Phases 1-4)**: Delivers User Stories 1 & 2 - can execute basic and chained BDD scenarios via Kotlin DSL.

**Recommended Delivery Order**:
1. Complete Phases 1-3 → Demo basic scenario execution
2. Complete Phase 4 → Demo data flow between steps
3. Phases 5-6 can proceed in parallel → Demo fragments and parameterization
4. Phase 7 → Demo auto-assertions and schema validation
5. Phase 8 → Demo text-based .scenario files
6. Phases 9-10 → Polish and samples

**Total Tasks**: 127 tasks
**Tasks per User Story**:
- US1: 18 tasks (T032-T049)
- US2: 9 tasks (T050-T058)
- US3: 9 tasks (T059-T067)
- US4: 10 tasks (T068-T077)
- US5: 11 tasks (T078-T088)
- Parser: 18 tasks (T089-T106)
- JUnit: 5 tasks (T107-T111)
- Polish: 16 tasks (T112-T127)
