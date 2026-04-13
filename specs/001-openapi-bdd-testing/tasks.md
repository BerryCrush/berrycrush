# Tasks: OpenAPI BDD Testing Library (BerryCrush)

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
- `berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/` - Core library
- `berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/` - Core tests
- `berrycrush/junit/src/main/kotlin/io/github/ktakashi/berrycrush/junit/` - JUnit integration
- `samples/petstore/src/test/kotlin/` - Example scenarios

---

## Phase 1: Setup (Project Infrastructure)

**Purpose**: Initialize Gradle multi-module project structure with all dependencies

- [x] T001 Create root build.gradle.kts with Kotlin 2.3.20 and Java 21 configuration
- [x] T002 Create settings.gradle.kts with multi-module structure (berrycrush/core, berrycrush/junit, samples/petstore)
- [x] T003 [P] Create gradle/libs.versions.toml with version catalog (swagger-parser 2.1.22, json-path 2.9.0, json-schema-validator 1.4.0, junit 5.10.x)
- [x] T004 [P] Create berrycrush/core/build.gradle.kts with dependencies
- [x] T005 [P] Create berrycrush/junit/build.gradle.kts with JUnit 5 dependencies
- [x] T006 [P] Create samples/petstore/build.gradle.kts with test configuration
- [x] T007 [P] Configure ktlint for code formatting in root build.gradle.kts
- [x] T008 Create berrycrush/core/src/test/resources/petstore.yaml OpenAPI test fixture

**Checkpoint**: `./gradlew build` compiles successfully with empty modules ✅

---

## Phase 2: Foundational (Core Infrastructure)

**Purpose**: Core infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Core Models

- [x] T009 [P] Create StepType enum in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/StepType.kt
- [x] T010 [P] Create ResultStatus enum in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/ResultStatus.kt
- [x] T011 [P] Create Scenario data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/Scenario.kt
- [x] T012 [P] Create Step data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/Step.kt
- [x] T013 [P] Create Extraction data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/Extraction.kt
- [x] T014 [P] Create Assertion data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/Assertion.kt
- [x] T015 [P] Create StepResult data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/StepResult.kt
- [x] T016 [P] Create ScenarioResult data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/ScenarioResult.kt
- [x] T017 [P] Create Configuration data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/config/Configuration.kt

### OpenAPI Integration

- [x] T018 Create OpenApiLoader class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/openapi/OpenApiLoader.kt (wraps Swagger Parser)
- [x] T019 Create OperationResolver class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/openapi/OperationResolver.kt (resolves operationId to path+method)
- [x] T020 Create SpecRegistry class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/openapi/SpecRegistry.kt (multi-spec support)
- [x] T021 [P] Create unit tests for OpenApiLoader in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/openapi/OpenApiLoaderTest.kt
- [x] T022 [P] Create unit tests for OperationResolver in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/openapi/OperationResolverTest.kt

### HTTP Execution Foundation

- [x] T023 Create HttpRequestBuilder class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/executor/HttpRequestBuilder.kt (uses java.net.http.HttpClient)
- [x] T024 Create ResponseHandler class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/executor/ResponseHandler.kt
- [x] T025 [P] Create unit tests for HttpRequestBuilder in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/executor/HttpRequestBuilderTest.kt

### Execution Context

- [x] T026 Create ExecutionContext class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/context/ExecutionContext.kt
- [x] T027 [P] Create unit tests for ExecutionContext in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/context/ExecutionContextTest.kt

### Error Handling

- [x] T028 [P] Create BerryCrushException base class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/exception/Exceptions.kt
- [x] T029 [P] Create OperationNotFoundException in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/exception/Exceptions.kt
- [x] T030 [P] Create ExtractionException in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/exception/Exceptions.kt
- [x] T031 [P] Create AssertionFailedException in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/exception/Exceptions.kt

**Checkpoint**: Foundation ready - all core models compiled, OpenAPI loading works, HTTP client configured ✅

---

## Phase 3: User Story 1 - Execute a Simple BDD Scenario (Priority: P1) 🎯 MVP

**Goal**: QA engineer can write and run a basic BDD scenario against an API defined in OpenAPI spec

**Independent Test**: Load petstore.yaml, write scenario with Given/When/Then calling listPets, verify pass/fail output

### DSL Implementation for US1

- [x] T032 [US1] Create @DslMarker annotation BerryCrushDsl in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/DslMarker.kt
- [x] T033 [US1] Create BerryCrushSuite class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/BerryCrushSuite.kt
- [x] T034 [US1] Create berryCrush() entry point function in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/BerryCrushDsl.kt
- [x] T035 [US1] Create ScenarioScope class with given/when/then functions in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/ScenarioScope.kt
- [x] T036 [US1] Create StepScope class with call() function in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/StepScope.kt
- [x] T037 [US1] Create CallScope class with pathParam/queryParam/header/body in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/CallScope.kt

### Basic Assertions for US1

- [x] T038 [US1] Implement statusCode() assertion in StepScope in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/StepScope.kt
- [x] T039 [US1] Implement bodyContains() assertion in StepScope
- [x] T040 [US1] Implement bodyEquals() assertion using JSONPath in StepScope
- [x] T041 [US1] Implement headerExists() and headerEquals() assertions in StepScope

### Scenario Execution for US1

- [x] T042 [US1] Create ScenarioExecutor class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/executor/ScenarioExecutor.kt
- [x] T043 [US1] Implement step-by-step execution with result tracking in ScenarioExecutor
- [x] T044 [US1] Implement Scenario.run() extension function in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/ScenarioExtensions.kt

### Reporting for US1

- [x] T045 [US1] Create TestReporter interface in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/TestReporter.kt
- [x] T046 [US1] Create ConsoleReporter implementation in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/ConsoleReporter.kt

### Tests for US1

- [x] T047 [P] [US1] Create DSL contract test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/dsl/BerryCrushDslTest.kt
- [x] T048 [P] [US1] Create ScenarioExecutor unit test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/executor/ScenarioExecutorTest.kt
- [x] T049 [US1] Create integration test with petstore.yaml in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/integration/SimpleScenarioIntegrationTest.kt

**Checkpoint**: User Story 1 complete - can execute `scenario("name") { given { call("op") } then { statusCode(200) } }.run()` ✅

---

## Phase 4: User Story 2 - Chain API Calls with Data Flow (Priority: P1)

**Goal**: QA engineer can chain multiple API calls where data from one response flows into the next request

**Independent Test**: Call listPets → extract first pet ID → call getPetById with that ID → verify response

### Value Extraction for US2

- [x] T050 [US2] Create ValueExtractor class using JSONPath in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/context/ValueExtractor.kt
- [x] T051 [US2] Implement extractTo() function in StepScope for variable extraction
- [x] T052 [US2] Implement context property in StepScope for variable access
- [x] T053 [US2] Add variable interpolation support in CallScope (body, pathParam, etc.)

### Context Flow for US2

- [x] T054 [US2] Implement context passing between steps in ScenarioExecutor
- [x] T055 [US2] Add lastResponse property to ExecutionContext for implicit access
- [x] T056 [US2] Implement variable resolution in HTTP request building

### Tests for US2

- [x] T057 [P] [US2] Create ValueExtractor unit test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/context/ValueExtractorTest.kt
- [x] T058 [US2] Create data flow integration test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/integration/DataFlowIntegrationTest.kt

**Checkpoint**: User Story 2 complete - `extractTo("id", "$.id")` then `pathParam("petId", context["id"])` works ✅

---

## Phase 5: User Story 3 - Reuse Scenario Fragments (Priority: P2)

**Goal**: QA engineer can define reusable step groups and include them in multiple scenarios

**Independent Test**: Define auth fragment, use in two scenarios, verify both get the auth token

### Fragment DSL for US3

- [x] T059 [US3] Create Fragment data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/Fragment.kt
- [x] T060 [US3] Create FragmentScope class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/FragmentScope.kt
- [x] T061 [US3] Create fragment() builder function in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/FragmentDsl.kt
- [x] T062 [US3] Implement include() function in ScenarioScope
- [x] T063 [US3] Implement Background support in BerryCrushSuite for shared setup

### Fragment Execution for US3

- [x] T064 [US3] Add fragment resolution in ScenarioExecutor
- [x] T065 [US3] Implement context sharing between included fragments and scenarios

### Tests for US3

- [x] T066 [P] [US3] Create Fragment DSL test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/dsl/FragmentDslTest.kt
- [x] T067 [US3] Create fragment reuse integration test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/integration/FragmentIntegrationTest.kt

**Checkpoint**: User Story 3 complete - `val authFragment = fragment("auth") { ... }; scenario { include(authFragment) }` works ✅

---

## Phase 6: User Story 4 - Parameterize Scenarios (Priority: P2)

**Goal**: QA engineer can run the same scenario with different input data from a table

**Independent Test**: Create scenarioOutline with 3 data rows, verify it runs 3 times with correct substitutions

### Parameterization DSL for US4

- [x] T068 [US4] Create ExampleRow data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/ExampleRow.kt
- [x] T069 [US4] Create ScenarioOutlineScope class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/dsl/ScenarioOutlineScope.kt
- [x] T070 [US4] Create scenarioOutline() builder function in BerryCrushSuite
- [x] T071 [US4] Implement examples() and row() functions in ScenarioOutlineScope
- [x] T072 [US4] Add parameter placeholder substitution (<name> syntax) in step execution

### Parameterized Execution for US4

- [x] T073 [US4] Implement scenario expansion from outline + examples in ScenarioExecutor
- [x] T074 [US4] Add per-row result tracking with parameter values in ScenarioResult
- [x] T075 [US4] Update ConsoleReporter to show parameter values in output

### Tests for US4

- [x] T076 [P] [US4] Create ScenarioOutline DSL test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/dsl/ScenarioOutlineDslTest.kt
- [x] T077 [US4] Create parameterized scenario integration test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/integration/ParameterizedScenarioIntegrationTest.kt

**Checkpoint**: User Story 4 complete - `scenarioOutline { examples(row(...), row(...)) }` generates multiple test runs ✅

---

## Phase 7: User Story 5 - Validate Response Against OpenAPI Schema (Priority: P3)

**Goal**: QA engineer can validate API responses match OpenAPI schema definitions automatically

**Independent Test**: Call API, enable schema validation, verify type mismatches are reported

### Schema Validation for US5

- [x] T078 [US5] Create SchemaValidator class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/assertion/SchemaValidator.kt (uses networknt json-schema-validator)
- [x] T079 [US5] Create ValidationError data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/ValidationError.kt
- [x] T080 [US5] Implement matchesSchema() assertion in StepScope
- [x] T081 [US5] Add strict vs lenient validation modes to Configuration

### Auto-Assertions for US5

- [x] T082 [US5] Create AssertionGenerator class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/assertion/AssertionGenerator.kt
- [x] T083 [US5] Implement auto-assertion extraction from OpenAPI responses (status code, schema, content-type)
- [x] T084 [US5] Add autoAssert(Boolean) control in CallScope
- [x] T085 [US5] Add autoAssertions configuration block in Configuration

### Tests for US5

- [x] T086 [P] [US5] Create SchemaValidator unit test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/assertion/SchemaValidatorTest.kt
- [x] T087 [P] [US5] Create AssertionGenerator unit test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/assertion/AssertionGeneratorTest.kt
- [x] T088 [US5] Create schema validation integration test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/integration/SchemaValidationIntegrationTest.kt

**Checkpoint**: User Story 5 complete - `matchesSchema()` validates responses, auto-assertions derive from spec ✅

---

## Phase 8: Scenario File Parser (Text-Based Scenarios)

**Goal**: Enable non-technical users to write .scenario files that are parsed and executed

**Purpose**: Extends accessibility of the library to QA analysts and business stakeholders

### Lexer & Parser

- [x] T089 [P] Create Token types in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/Token.kt
- [x] T090 [P] Create SourceLocation data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/SourceLocation.kt
- [x] T091 [P] Create ParseError data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/ParseError.kt
- [x] T092 Create Lexer class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/Lexer.kt
- [x] T093 Create AST data classes (FeatureFile, Feature, Background, ParsedScenario, ParsedStep) in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/AstNodes.kt
- [x] T094 Create Parser class (recursive descent) in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/Parser.kt
- [x] T095 Create DirectiveType enum in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/Token.kt (TokenType enum)
- [x] T096 Create StepDirective data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/AstNodes.kt (ActionNode classes)

### Semantic Analysis

- [x] T097 Create SemanticAnalyzer class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/ScenarioLoader.kt (integrated with loader)
- [x] T098 Create AssertionParser class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/Parser.kt (parseAssertAction method)

### Scenario Loading

- [x] T099 Create ScenarioLoader class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/scenario/ScenarioLoader.kt
- [x] T100 Implement loadScenariosFrom() function in BerryCrushSuite
- [x] T101 Implement loadFragmentsFrom() function in BerryCrushSuite
- [x] T102 Create AST to executable Scenario transformer

### Parser Tests

- [x] T103 [P] Create Lexer unit tests in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/scenario/LexerTest.kt
- [x] T104 [P] Create Parser unit tests in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/scenario/ParserTest.kt
- [x] T105 [P] Create test fixtures in berrycrush/core/src/test/resources/scenarios/valid/ and invalid/
- [x] T106 Create ScenarioLoader integration test in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/scenario/ScenarioLoaderTest.kt

**Checkpoint**: `.scenario` files can be parsed and executed alongside Kotlin DSL scenarios ✅

---

## Phase 9: JUnit 5 Integration

**Goal**: Seamless integration with JUnit 5 test framework for IDE and CI support

### JUnit Extension

- [x] T107 [P] Create BerryCrushExtension class in berrycrush/junit/src/main/kotlin/io/github/ktakashi/berrycrush/junit/BerryCrushExtension.kt
- [x] T108 [P] Create @BerryCrushSpec annotation in berrycrush/junit/src/main/kotlin/io/github/ktakashi/berrycrush/junit/Annotations.kt
- [x] T109 Create ScenarioTest base class in berrycrush/junit/src/main/kotlin/io/github/ktakashi/berrycrush/junit/ScenarioTest.kt
- [x] T110 Implement dynamic test generation from scenarios for @TestFactory

### JUnit Tests

- [x] T111 [P] Create BerryCrushExtension tests in berrycrush/junit/src/test/kotlin/io/github/ktakashi/berrycrush/junit/BerryCrushExtensionTest.kt

**Checkpoint**: Scenarios can run as JUnit 5 tests with IDE integration ✅

---

## Phase 10: Sample Project & Polish

**Goal**: Provide working examples and finalize documentation

### Sample Petstore Scenarios (Kotlin DSL)

- [x] T112 [P] Create PetstoreKotlinScenarios.kt in samples/petstore/src/test/kotlin/ demonstrating US1-US5
- [x] T113 [P] Create petstore.yaml OpenAPI spec in samples/petstore/src/test/resources/ (using existing spec)

### Sample Petstore Scenarios (Text Files)

- [x] T114 [P] Create petstore-crud.scenario in samples/petstore/src/test/resources/scenarios/
- [x] T115 [P] Create auth.fragment in samples/petstore/src/test/resources/fragments/

### Additional Assertions & Features

- [x] T116 [P] Implement bodyArraySize() assertion in StepScope
- [x] T117 [P] Implement bodyArrayNotEmpty() assertion in StepScope
- [x] T118 [P] Implement bodyMatches() regex assertion in StepScope
- [x] T119 [P] Implement responseTime() assertion in StepScope
- [x] T120 [P] Add bearerToken(), basicAuth(), apiKey() authentication shortcuts in CallScope

### JSON Reporter

- [x] T121 Create JsonReporter implementation in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/JsonReporter.kt
- [x] T122 Create TestReport aggregation class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/model/TestReport.kt

### Multi-Spec Support Enhancement

- [x] T123 Implement using() function in StepScope for spec switching
- [x] T124 Add spec-specific Configuration support
- [x] T125 Implement auto-resolution when operationId is unique across specs

### Final Validation

- [x] T126 Run full integration test suite with samples
- [x] T127 Verify all quickstart.md examples work correctly

**Checkpoint**: Library complete with samples, all user stories functional ✅

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
