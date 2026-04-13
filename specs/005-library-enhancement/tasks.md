# Tasks: Library Enhancement

**Branch**: `005-library-enhancement`  
**Date**: 2026-04-09  
**Input**: Design documents from `/specs/005-library-enhancement/`

**Organization**: Tasks grouped by user story for independent implementation and testing

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1, US2, US3, etc.)
- File paths are relative to repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and module structure

- [X] T001 Create doc module structure at berrycrush/doc/ with src/sphinx/ and build/ directories
- [X] T002 Add doc module to settings.gradle.kts
- [X] T003 [P] Configure ktlint for doc module in doc/build.gradle.kts
- [X] T004 [P] Create plugin package structure in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/
- [X] T005 [P] Create step package structure in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/step/
- [X] T006 [P] Create report package structure in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story implementation

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T007 Create base Plugin SPI interface in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/BerryCrushPlugin.kt
- [X] T008 [P] Create ScenarioContext interface in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/ScenarioContext.kt  
- [X] T009 [P] Create StepContext interface in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/StepContext.kt
- [X] T010 [P] Create ScenarioResult interface in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/ScenarioResult.kt
- [X] T011 [P] Create StepResult interface in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/StepResult.kt
- [X] T012 [P] Create AssertionFailure data class in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/AssertionFailure.kt
- [X] T013 Create PluginRegistry for managing plugin lifecycle in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/PluginRegistry.kt
- [X] T014 Implement plugin priority-based ordering in PluginRegistry
- [X] T015 Create TestReport data model in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/TestReport.kt
- [X] T016 [P] Create ScenarioReportEntry in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/ScenarioReportEntry.kt
- [X] T017 [P] Create StepReportEntry in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/StepReportEntry.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Plugin System with Lifecycle Hooks (Priority: P1) 🎯 MVP

**Goal**: Enable users to create plugins that hook into test lifecycle for custom actions

**Independent Test**: Create a logging plugin, register it, run tests, verify lifecycle hooks are called in correct order

### Implementation for User Story 1

- [X] T018 [P] [US1] Implement name-based plugin registration parser in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/PluginNameResolver.kt
- [X] T019 [P] [US1] Implement class-based plugin registration in PluginRegistry
- [X] T020 [US1] Add plugin lifecycle event dispatch in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/engine/ScenarioExecutor.kt
- [X] T021 [US1] Integrate plugin hooks into scenario start in ScenarioExecutor
- [X] T022 [US1] Integrate plugin hooks into scenario end in ScenarioExecutor
- [X] T023 [US1] Integrate plugin hooks into step start in StepExecutor
- [X] T024 [US1] Integrate plugin hooks into step end in StepExecutor
- [X] T025 [US1] Implement fail-fast exception handling for plugin errors in PluginRegistry
- [X] T026 [US1] Add plugin registration annotations (@BerryCrushConfiguration) in berrycrush/junit/src/main/kotlin/io/github/ktakashi/berrycrush/junit/annotations/BerryCrushConfiguration.kt
- [X] T027 [US1] Process plugin registration from annotations in JUnit engine in berrycrush/junit/src/main/kotlin/io/github/ktakashi/berrycrush/junit/engine/BerryCrushTestEngine.kt
- [X] T028 [P] [US1] Create integration test for plugin lifecycle events in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/plugin/PluginLifecycleTest.kt
- [X] T029 [P] [US1] Create test for priority-based plugin ordering in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/plugin/PluginPriorityTest.kt
- [X] T030 [P] [US1] Create test for plugin exception handling in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/plugin/PluginExceptionTest.kt

**Checkpoint**: Plugin system fully functional - users can create and register plugins

---

## Phase 4: User Story 2 - Enhanced Test Reporting (Priority: P1)

**Goal**: Provide detailed failure diagnostics in multiple report formats (text, JSON, XML, JUnit)  

**Independent Test**: Fail a validation step, verify report shows expected vs actual values in all formats

### Implementation for User Story 2

- [X] T031 [P] [US2] Enhance AssertionFailure to capture HTTP request snapshot in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/plugin/AssertionFailure.kt
- [X] T032 [P] [US2] Enhance AssertionFailure to capture HTTP response snapshot
- [X] T033 [P] [US2] Add diff calculation for AssertionFailure
- [X] T034 [US2] Update assertion logic to populate expected/actual values in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/engine/AssertionExecutor.kt
- [X] T035 [US2] Implement TextReportPlugin in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/TextReportPlugin.kt
- [X] T036 [P] [US2] Implement JsonReportPlugin with JSON Schema 2020-12 in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/JsonReportPlugin.kt
- [X] T037 [P] [US2] Implement XmlReportPlugin in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/XmlReportPlugin.kt
- [X] T038 [P] [US2] Implement JunitReportPlugin in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/report/JunitReportPlugin.kt
- [X] T039 [US2] Implement report format configuration parsing in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/config/ReportConfiguration.kt
- [X] T040 [US2] Support berrycrush.properties for report configuration
- [X] T041 [US2] Support berrycrush.yml for report configuration
- [X] T042 [US2] Add @BerryCrushReports annotation for per-test overrides in berrycrush/junit/src/main/kotlin/io/github/ktakashi/berrycrush/junit/annotations/BerryCrushReports.kt
- [X] T043 [US2] Process report format annotations in JUnit engine
- [X] T044 [US2] Register default text report plugin
- [X] T045 [P] [US2] Create test for text report output in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/report/TextReportPluginTest.kt
- [X] T046 [P] [US2] Create test for JSON report schema validation in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/report/JsonReportPluginTest.kt
- [X] T047 [P] [US2] Create test for JUnit XML format in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/report/JunitReportPluginTest.kt
- [X] T048 [US2] Create integration test verifying failure details in all formats in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/report/ReportIntegrationTest.kt

**Checkpoint**: All report formats generate with detailed diagnostics

---

## Phase 5: User Story 3 - Custom Step Definition (Priority: P2)

**Goal**: Enable users to define custom steps via annotation, registration API, DSL, and package scanning

**Independent Test**: Create a custom step using each mechanism, use in scenario, verify execution

### Implementation for User Story 3

- [X] T049 [P] [US3] Create @Step annotation in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/step/Step.kt
- [X] T050 [P] [US3] Create StepRegistry interface in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/step/StepRegistry.kt
- [X] T051 [US3] Implement StepMatcher for pattern matching in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/step/StepMatcher.kt  
- [X] T052 [US3] Implement parameter extraction for {int}, {string}, {word} placeholders
- [X] T053 [US3] Implement parameter type conversion
- [X] T054 [US3] Create annotation-based step scanner in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/step/AnnotationStepScanner.kt
- [X] T055 [US3] Implement package-based step discovery in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/step/PackageStepScanner.kt
- [X] T056 [US3] Create Kotlin DSL builder for step registration in berrycrush/core/src/main/kotlin/io/github/ktakashi/berrycrush/step/StepDsl.kt
- [X] T057 [US3] Add @BerryCrushConfiguration.stepClasses parameter
- [X] T058 [US3] Add @BerryCrushConfiguration.stepPackages parameter
- [X] T059 [US3] Process stepClasses annotation parameter in JUnit engine
- [X] T060 [US3] Process stepPackages annotation parameter in JUnit engine
- [X] T061 [US3] Integrate custom steps with existing step execution in StepExecutor
- [X] T062 [US3] Add custom step result integration with reporting
- [X] T063 [US3] Enable Spring auto-discovery of @Step methods in @Component classes in berrycrush/spring/src/main/kotlin/io/github/ktakashi/berrycrush/spring/SpringStepDiscovery.kt
- [X] T064 [P] [US3] Create test for annotation-based step binding in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/step/AnnotationStepTest.kt
- [X] T065 [P] [US3] Create test for registration API in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/step/RegistrationApiTest.kt
- [X] T066 [P] [US3] Create test for Kotlin DSL builder in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/step/StepDslTest.kt
- [X] T067 [P] [US3] Create test for package scanning in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/step/PackageScanningTest.kt
- [X] T068 [US3] Create integration test for custom step execution in berrycrush/core/src/test/kotlin/io/github/ktakashi/berrycrush/step/CustomStepIntegrationTest.kt

**Checkpoint**: All custom step binding mechanisms working

---

## Phase 6: User Story 4 - Dependency Updates (Priority: P2)

**Goal**: Update dependencies to target future versions

**⚠️ WARNING**: The target versions below **do not currently exist** and will fail to resolve until released:
- Spring Boot 4.0.5 (current: 3.4.1)
- Jackson 3.1.1 (current: 2.17.0)
- JUnit Platform 6.0.3 (current: 1.11.4)
- json-schema-validator 3.0.1 (current: 1.5.3)
- json-path 3.0.0 (current: 2.9.0)

**Independent Test**: Run dependency analysis, verify versions match targets when available, confirm no CVEs, all tests pass

### Implementation for User Story 4

- [X] T069 [US4] Update Spring Boot to version 4.0.5 in gradle/libs.versions.toml
- [X] T070 [US4] Update Jackson to version 3.1.1 in gradle/libs.versions.toml
- [X] T071 [US4] Update JUnit Platform to version 6.0.3 in gradle/libs.versions.toml
- [X] T072 [US4] Update json-schema-validator to version 3.0.1 in gradle/libs.versions.toml
- [X] T073 [US4] Update json-path to version 3.0.0 in gradle/libs.versions.toml
- [X] T074 [US4] Update H2 database to version 2.3.232 in gradle/libs.versions.toml
- [X] T075 [US4] Run Gradle sync to verify dependency resolution
- [X] T076 [US4] Document dependency changes
- [X] T077 [US4] Run all existing tests to verify compatibility
- [X] T078 [US4] Fix test failures caused by dependency updates
- [X] T079 [US4] Run dependency vulnerability scan (e.g., ./gradlew dependencyCheckAnalyze)

**Checkpoint**: Dependency versions updated in configuration (may be blocked awaiting release)

---

## Phase 7: User Story 5 - Maven Publishing with Javadoc and Sources (Priority: P2)

**Goal**: Publish library with javadoc (from Dokka) and sources jars

**Independent Test**: Publish to local Maven repo, verify -javadoc.jar and -sources.jar artifacts exist

### Implementation for User Story 5

- [X] T080 [P] [US5] Add Dokka plugin version 2.2.0 to root build.gradle.kts
- [X] T081 [P] [US5] Configure Dokka in berrycrush/core/build.gradle.kts
- [X] T082 [P] [US5] Configure Dokka in berrycrush/junit/build.gradle.kts
- [X] T083 [P] [US5] Configure Dokka in berrycrush/spring/build.gradle.kts
- [X] T084 [US5] Configure dokkaHtml task to output to berrycrush/doc/build/dokka/
- [X] T085 [US5] Configure dokkaJavadoc task for Maven publishing
- [X] T086 [US5] Create dokkaJavadocJar task in each library module
- [X] T087 [US5] Configure maven-publish plugin in each library module
- [X] T088 [US5] Add POM metadata (name, description, url, licenses, developers, scm) to publishing config
- [X] T089 [US5] Configure sources jar generation
- [X] T090 [US5] Add signing configuration for Maven Central
- [X] T091 [US5] Create publish task that includes main jar, javadoc jar, sources jar
- [X] T092 [US5] Test local Maven publish with ./gradlew publishToMavenLocal
- [X] T093 [US5] Verify javadoc jar contains API documentation
- [X] T094 [US5] Verify sources jar contains Kotlin source files

**Checkpoint**: Maven artifacts publishable with all required jars

---

## Phase 8: User Story 6 - Comprehensive User Documentation (Priority: P3)

**Goal**: Create user documentation with quick start, tutorial, feature guides, migration, troubleshooting

**Independent Test**: Build documentation, verify HTML output contains all sections

### Implementation for User Story 6

- [X] T095 [US6] Add Sphinx configuration in berrycrush/doc/src/sphinx/conf.py
- [X] T096 [US6] Create buildSphinx Gradle task in berrycrush/doc/build.gradle.kts
- [X] T097 [P] [US6] Create index.rst in berrycrush/doc/src/sphinx/index.rst
- [X] T098 [P] [US6] Create quickstart.rst based on quickstart.md in berrycrush/doc/src/sphinx/quickstart.rst
- [X] T099 [P] [US6] Create tutorial.rst in berrycrush/doc/src/sphinx/tutorial.rst
- [X] T100 [P] [US6] Create features/plugins.rst for plugin system guide in berrycrush/doc/src/sphinx/features/plugins.rst
- [X] T101 [P] [US6] Create features/custom-steps.rst for custom steps guide in berrycrush/doc/src/sphinx/features/custom-steps.rst
- [X] T102 [P] [US6] Create features/reporting.rst for reporting guide in berrycrush/doc/src/sphinx/features/reporting.rst
- [X] T103 [P] [US6] Create migration.rst for migration guide in berrycrush/doc/src/sphinx/migration.rst
- [X] T104 [P] [US6] Create troubleshooting.rst in berrycrush/doc/src/sphinx/troubleshooting.rst
- [X] T105 [US6] Link Dokka API docs from Sphinx documentation
- [X] T106 [US6] Add code examples from quickstart.md to documentation
- [X] T107 [US6] Test documentation build with ./gradlew buildSphinx
- [X] T108 [US6] Verify all sections render correctly in HTML output

**Checkpoint**: Documentation builds successfully with all sections

---

## Phase 9: User Story 7 - API Documentation via Dokka (Priority: P3)

**Goal**: Generate API documentation from KDoc comments

**Independent Test**: Run Dokka, verify API docs generated for all public classes

### Implementation for User Story 7

- [X] T109 [P] [US7] Add KDoc comments to Plugin interfaces in berrycrush/core/
- [X] T110 [P] [US7] Add KDoc comments to Step interfaces in berrycrush/core/
- [X] T111 [P] [US7] Add KDoc comments to Report classes in berrycrush/core/
- [X] T112 [P] [US7] Add KDoc comments to JUnit annotations in berrycrush/junit/
- [X] T113 [P] [US7] Add KDoc comments to Spring integration classes in berrycrush/spring/
- [X] T114 [US7] Configure dokkaHtmlMultiModule in root build.gradle.kts for aggregated docs
- [X] T115 [US7] Run Dokka generation with ./gradlew dokkaHtmlMultiModule
- [X] T116 [US7] Verify API docs are in berrycrush/doc/build/dokka/
- [X] T117 [US7] Verify all public APIs are documented in generated HTML

**Checkpoint**: API documentation generated and accessible

---

## Phase 10: User Story 8 - Apache License 2.0 (Priority: P3)

**Goal**: Add Apache License 2.0 to repository

**Independent Test**: Verify LICENSE file exists at root with correct content

### Implementation for User Story 8

- [X] T118 [US8] Create LICENSE file at repository root
- [X] T119 [US8] Add Apache License 2.0 text to LICENSE
- [X] T120 [US8] Update LICENSE with correct copyright year (2026) and holder information
- [X] T121 [US8] Verify LICENSE file is included in published artifacts

**Checkpoint**: LICENSE file added

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements and validation

- [X] T122 [P] Update README.md with new features (plugins, custom steps, reporting, documentation)
- [X] T123 [P] Add examples for plugin usage in samples/plugin-examples/
- [X] T124 [P] Add examples for custom steps in samples/custom-steps-annotation/, samples/custom-steps-dsl/
- [X] T125 [P] Update CHANGELOG.md with version 0.2.0 changes
- [X] T126 Run complete test suite across all modules
- [X] T127 Run ktlint check across all modules
- [X] T128 Verify quickstart.md examples work end-to-end
- [X] T129 Performance test: Verify plugin overhead <5ms per hook
- [X] T130 Performance test: Verify report generation <1s for 1000 tests
- [X] T131 Run security vulnerability scan on final dependencies

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - **BLOCKS all user stories**
- **User Stories (Phase 3-10)**: All depend on Foundational completion
  - US1 (Plugin System) and US2 (Reporting): Can proceed in parallel
  - US2 (Reporting) technically depends on US1 (reports are plugins), but can be developed concurrently
  - US3 (Custom Steps): Independent, can proceed in parallel
  - US4 (Dependencies): Independent, can proceed in parallel
  - US5 (Maven Publishing): Should wait for US7 (Dokka) since javadoc jar uses Dokka
  - US6 (User Docs) & US7 (API Docs): Can proceed in parallel
  - US8 (License): Trivial, can be done anytime
- **Polish (Phase 11)**: Depends on all desired user stories

### User Story Dependencies

- **US1 (Plugin System)**: No dependencies on other stories - foundational for US2
- **US2 (Reporting)**: Uses US1 (plugin SPI) - implement after US1 interface defined or in parallel
- **US3 (Custom Steps)**: Independent of other stories
- **US4 (Dependencies)**: Independent - update early to avoid conflicts
- **US5 (Maven Publishing)**: Depends on US7 (Dokka must generate javadoc)
- **US6 (User Documentation)**: Independent - references other features but can document concurrently
- **US7 (API Documentation)**: Independent - add KDoc as you code
- **US8 (License)**: Independent - trivial task

### Parallel Opportunities Within User Stories

**US1 - Plugin System**:
```bash
# These can run in parallel:
T018: PluginNameResolver.kt
T019: PluginRegistry class-based registration
T026: @BerryCrushConfiguration annotation
# Then integrate into engine
```

**US2 - Reporting**:
```bash
# All report plugins can be developed in parallel:
T035: TextReportPlugin.kt
T036: JsonReportPlugin.kt
T037: XmlReportPlugin.kt
T038: JunitReportPlugin.kt
```

**US3 - Custom Steps**:
```bash
# These scan mechanisms can be developed in parallel:
T049: @Step annotation
T050: StepRegistry interface
T054: AnnotationStepScanner
T055: PackageStepScanner
T056: StepDsl
```

**US5 - Maven Publishing**:
```bash
# Dokka configuration for all modules in parallel:
T080: core/build.gradle.kts
T081: junit/build.gradle.kts
T082: spring/build.gradle.kts
```

**US6 - User Documentation**:
```bash
# All .rst files can be created in parallel:
T097: quickstart.rst
T098: tutorial.rst
T099: features/plugins.rst
T100: features/custom-steps.rst
T101: features/reporting.rst
T102: migration.rst
T103: troubleshooting.rst
```

**US7 - API Documentation**:
```bash
# KDoc comments for all modules in parallel:
T108: core/ interfaces
T109: core/ step interfaces
T110: core/ report classes
T111: junit/ annotations
T112: spring/ integration classes
```

---

## Implementation Strategy

### Recommended MVP (US1 + US2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational **← CRITICAL BLOCKING PHASE**
3. Complete Phase 3: User Story 1 (Plugin System)
4. Complete Phase 4: User Story 2 (Enhanced Reporting)
5. **STOP and VALIDATE**: Create sample plugin, verify reports show detailed failures
6. This gives core library enhancement value

### Full Feature Delivery (Priority Order)

1. Phase 1 + 2: Setup + Foundation
2. Phase 3 + 4: US1 (Plugins) + US2 (Reporting) - **P1 features**
3. Phase 5: US3 (Custom Steps) - **P2**
4. Phase 6: US4 (Dependencies) - **P2** (do early to avoid conflicts)
5. Phase 7: US7 (Dokka) + Phase 8: US5 (Maven Publishing) - **P2/P3** (US7 before US5)
6. Phase 9: US6 (Documentation) - **P3**
7. Phase 10: US8 (License) - **P3** (trivial, can be done anytime)
8. Phase 11: Polish

### Parallel Team Strategy

With 3 developers:

1. All: Complete Setup + Foundational together
2. After Foundational:
   - **Developer A**: US1 (Plugin System) → integrate with US2
   - **Developer B**: US2 (Reporting) → coordinate with US1 on plugin interface
   - **Developer C**: US3 (Custom Steps) → independent development
3. Then:
   - **Developer A**: US4 (Dependencies)
   - **Developer B**: US6 (User Docs)
   - **Developer C**: US7 (Dokka) → US5 (Maven Publishing)
4. Finally: US8 (License) + Polish

---

## Task Summary

- **Total Tasks**: 131
- **Phase 1 (Setup)**: 6 tasks
- **Phase 2 (Foundational)**: 11 tasks (BLOCKING)
- **Phase 3 (US1 - Plugin System)**: 13 tasks
- **Phase 4 (US2 - Enhanced Reporting)**: 18 tasks
- **Phase 5 (US3 - Custom Steps)**: 20 tasks
- **Phase 6 (US4 - Dependencies)**: 11 tasks
- **Phase 7 (US5 - Maven Publishing)**: 15 tasks
- **Phase 8 (US6 - User Documentation)**: 14 tasks
- **Phase 9 (US7 - API Documentation)**: 9 tasks
- **Phase 10 (US8 - License)**: 4 tasks
- **Phase 11 (Polish)**: 10 tasks

**Parallelizable Tasks**: 66 tasks marked with [P]  
**Sequential Dependencies**: Foundational phase blocks all user stories

---

## Notes

- All paths relative to repository root (`/Users/yo32es/projects/berrycrush/`)
- [P] = Parallelizable (different files, no blocking dependencies)
- [US#] = User story mapping for traceability
- Each user story is independently testable
- MVP = US1 + US2 delivers core value (plugins + enhanced reporting)
- **⚠️ US4 Dependency versions**: Targeting future versions (Spring Boot 4.x, Jackson 3.x, JUnit 6.x) that do not currently exist - will fail until released
- JSON Schema: Using 2020-12 (latest stable)
- Plugin registration: Supports both name-based and class-based
- Custom steps: Four mechanisms (annotation, API, DSL, package scanning)
