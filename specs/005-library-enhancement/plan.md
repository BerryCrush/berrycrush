# Implementation Plan: Library Enhancement

**Branch**: `005-library-enhancement` | **Date**: 2026-04-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-library-enhancement/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Modernize and enhance the berrycrush BDD API testing library by adding:
1. Plugin system with lifecycle hooks (scenario/step start/end)
2. Enhanced reporting with detailed failure diagnostics (text, JSON, XML, JUnit formats)
3. Multiple custom step binding mechanisms (annotations, registration API, Kotlin DSL)
4. Dependency updates to latest stable versions (Spring Boot 4.0.5, Jackson 3.1.1, etc.)
5. Comprehensive documentation (reStructuredText + Sphinx, KDoc + Dokka)
6. Maven publishing enhancements (javadoc and sources jars)
7. Apache License 2.0

## Technical Context

**Language/Version**: Kotlin 2.3.20 / Java 21  
**Primary Dependencies**: JUnit Platform 5.11.4 (currently), Spring Boot 3.4.1 (currently), Swagger Parser 2.1.22, Jackson 2.17.0, JSONPath 2.9.0, json-schema-validator 1.4.0  
**Storage**: H2 2.3.232 (sample only, not production)  
**Testing**: JUnit Platform 1.11.4, JUnit Jupiter 5.11.4  
**Target Platform**: JVM 21 (library for Java/Kotlin projects)  
**Project Type**: Testing library (BDD-style API testing framework)  
**Performance Goals**: Minimal overhead on test execution (<5ms per lifecycle hook), report generation <1s for 1000 tests  
**Constraints**: Must maintain backward compatibility with existing scenario files and test classes, zero breaking changes to public API  
**Scale/Scope**: Library with 4 modules (core, junit, spring, doc), ~50 public APIs, targeting 10k+ library users

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Initial Check (Before Research)

| Principle | Assessment | Notes |
|-----------|-----------|-------|
| **Code Quality** | ✅ PASS | Enhanced reporting improves error visibility; plugin isolation prevents cascade failures |
| **User Experience** | ✅ PASS | Multiple binding mechanisms (annotation/API/DSL) provide flexibility; detailed failure reports improve debugging UX |
| **Maintainability** | ✅ PASS | Plugin architecture provides clear separation of concerns; modular design (4 report formats as plugins) maintains cohesion |
| **Testing Standards** | ✅ PASS | Enhanced reporting directly improves test quality; JUnit integration maintains test pyramid |
| **Flexibility** | ✅ PASS | Plugin SPI enables extensibility without modification; configuration + annotation override pattern accommodates diverse needs |

**Quality Gates Compliance**:
- Pre-commit: ktlint already enforced, will extend to new modules
- PR Review: Standard process maintained
- Integration: Will add integration tests for plugin lifecycle and custom step binding
- Release: Acceptance criteria clearly defined in spec

**Constitution Compliance**: ✅ **APPROVED** - No violations. Feature enhances library capabilities while maintaining architectural principles.

**Re-evaluation Trigger**: After Phase 1 design completion

---

### Post-Design Re-evaluation (After Phase 1)

**Design Review**:
- ✅ Plugin SPI follows single-responsibility principle (each plugin has one purpose)
- ✅ Custom step binding mechanisms are orthogonal (annotation, API, DSL) - users choose what fits
- ✅ Report formats share common data model (maintains consistency)
- ✅ All designs emphasize type safety and compile-time checking where possible
- ✅ Error handling is explicit (fail-fast for user plugins, resilient for built-in)

| Principle | Post-Design Assessment | Notes |
|-----------|----------------------|-------|
| **Code Quality** | ✅ PASS | Contracts define clear APIs with validation; built-in error resilience prevents silent failures |
| **User Experience** | ✅ PASS | Multiple binding options accommodate different developer preferences; detailed failure reports improve debugging UX |
| **Maintainability** | ✅ PASS | Clear separation: core module (engine + SPI), doc module (documentation); shared report data model reduces duplication |
| **Testing Standards** | ✅ PASS | Plugin SPI testable via mock contexts; custom steps testable via StepContext injection |
| **Flexibility** | ✅ PASS | SPI enables custom plugins; three binding mechanisms support diverse use cases; configuration + annotation override pattern |

**Final Verdict**: ✅ **DESIGN APPROVED** - All designs align with constitution. **User feedback incorporated** (package scanning, name-based plugin registration, JSON Schema 2020-12). Ready for Phase 2 (tasks generation).

## Project Structure

### Documentation (this feature)

```text
specs/005-library-enhancement/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification
├── research.md          # Phase 0 output (research findings)
├── data-model.md        # Phase 1 output (entities and relationships)
├── quickstart.md        # Phase 1 output (getting started guide)
├── contracts/           # Phase 1 output (plugin SPI, custom step API, report formats)
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
berrycrush/
├── core/                    # Core BDD engine (scenario parsing, execution)
│   └── src/
│       ├── main/kotlin/
│       │   └── io/github/ktakashi/berrycrush/
│       │       ├── engine/          # Scenario execution engine
│       │       ├── parser/          # Scenario file parser
│       │       ├── plugin/          # NEW: Plugin SPI
│       │       ├── step/            # NEW: Custom step binding mechanisms
│       │       └── report/          # NEW: Reporting infrastructure
│       └── test/
│           ├── kotlin/
│           └── resources/
│
├── junit/                   # JUnit 5 Platform integration
│   └── src/
│       ├── main/kotlin/
│       │   └── io/github/ktakashi/berrycrush/junit/
│       │       ├── engine/          # JUnit TestEngine implementation
│       │       └── annotations/     # Test annotations
│       └── test/
│
├── spring/                  # Spring Boot integration
│   └── src/
│       ├── main/kotlin/
│       │   └── io/github/ktakashi/berrycrush/spring/
│       │       ├── config/          # Spring configuration
│       │       └── bindings/        # Spring-aware bindings
│       └── test/
│
├── doc/                     # NEW: Documentation module
│   ├── src/
│   │   ├── sphinx/          # reStructuredText source files
│   │   │   ├── index.rst    # Main documentation entry
│   │   │   ├── quickstart.rst
│   │   │   ├── tutorial.rst
│   │   │   ├── features/    # Feature guides
│   │   │   ├── migration.rst
│   │   │   └── troubleshooting.rst
│   │   └── examples/        # Code examples
│   └── build/               # Generated HTML output
│       ├── html/            # Sphinx output
│       └── dokka/           # Dokka API docs
│
├── samples/
│   └── petstore/            # Sample application for integration testing
│
├── build.gradle.kts         # Root build configuration
├── settings.gradle.kts      # Module configuration
├── gradle/
│   └── libs.versions.toml   # Dependency version catalog (UPDATE dependencies here)
└── LICENSE                  # NEW: Apache License 2.0
```

**Structure Decision**: Single Gradle multi-project build with 4 library modules (core, junit, spring, doc) plus samples. The `core` module houses the plugin SPI and reporting infrastructure. The `doc` module is new and contains both user documentation (Sphinx) and API documentation (Dokka). This structure maintains clear separation of concerns while enabling code reuse.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**No violations identified.** All design decisions align with constitution principles:
- Plugin system maintains modularity and flexibility (Principle III, V)
- Multiple binding mechanisms serve different user needs without adding coupling (Principle II, V)
- Enhanced reporting improves user experience through better diagnostics (Principle II)
- Documentation module is separate and focused (Principle III)

---

## Phase Summary

### Phase 0: Research ✅ COMPLETE

**Deliverable**: [research.md](research.md)

**Key Findings**:
1. **Plugin Architecture**: Adopt JUnit 5-style lifecycle interfaces with priority-based ordering; name-based and class-based registration
2. **Custom Step Binding**: Implement all four mechanisms (annotation, API, DSL, package scanning) with shared backend
3. **JUnit XML Format**: Full diagnostic context in failure body for CI/CD compatibility
4. **Sphinx Integration**: Use Gradle Exec task with Python requirement documentation
5. **Dokka Configuration**: Multi-module setup with javadoc and HTML output
6. **Maven Publishing**: Standard requirements (sources + javadoc jars + POM metadata)
7. **⚠️ Dependency Versions**: Use latest available within same major series (not future versions)
8. **Report Formats**: Four plugins (text, JSON with 2020-12 schema, XML, JUnit) with shared data model

**Outstanding Questions**:
- ~~Dependency version clarification~~ **RESOLVED**: Use latest available versions within current major series (Spring Boot 3.x, Jackson 2.x, JUnit 5.x)

**User Feedback Incorporated** (2026-04-09):
1. ✅ **Package Scanning**: Added fourth custom step mechanism - package-based discovery via `stepPackages` parameter
2. ✅ **Name-Based Plugin Registration**: Support string identifiers like `"report:json:file.json"` in addition to class-based registration
3. ✅ **Plugin Name Property**: Plugin name comes from plugin's `name` property, used in name-based registration
4. ✅ **JSON Schema 2020-12**: Updated JSON report to use latest schema version instead of draft-07

---

### Phase 1: Design ✅ COMPLETE

**Deliverables**:
- [data-model.md](data-model.md) - 15 core entities defined
- [contracts/plugin-spi.md](contracts/plugin-spi.md) - Plugin interface specification
- [contracts/custom-step-api.md](contracts/custom-step-api.md) - Three binding mechanisms
- [contracts/report-formats.md](contracts/report-formats.md) - Four report format specifications
- [quickstart.md](quickstart.md) - Getting started guide with 7 quick starts

**Key Designs**:
1. **Plugin System**:
   - `BerryCrushPlugin` interface with 4 lifecycle hooks
   - Priority-based execution order (lower first) with registration fallback
   - Name-based registration: `"report:json:output.json"` with plugin name from `name` property
   - Class-based registration: `pluginClasses = [MyPlugin::class]`
   - Fail-fast error handling (plugins must be reliable)
   - Built-in plugins for reporting (text, JSON with 2020-12 schema, XML, JUnit)

2. **Custom Steps**:
   - Annotation-based: `@Step` annotation with pattern matching
   - Registration API: `StepRegistry.register()` for dynamic binding
   - Kotlin DSL: `steps { }` builder with type-safe parameters
   - Package scanning: Auto-discover `@Step` methods via `stepPackages` parameter
   - Shared `StepMatcher` engine handles pattern parsing and extraction

3. **Reporting**:
   - Shared `TestReport` data model
   - Four format plugins (text, JSON, XML, JUnit)
   - Detailed `AssertionFailure` with expected/actual/diff/request/response
   - Configuration-based + annotation override for format selection

4. **Project Structure**:
   - Core module: Plugin SPI, step binding, report infrastructure
   - Doc module: Sphinx (reStructuredText) + Dokka (API docs)
   - Maintain existing modules: junit, spring, samples

**Agent Context**: Updated `.github/copilot-instructions.md` with new technologies

**Constitution Re-check**: ✅ APPROVED - All designs align with constitution principles

---

## Next Steps

### For Implementation (Phase 2)

Run `/speckit.tasks` to generate detailed implementation tasks from this plan.

Tasks will cover:
1. **Core Infrastructure**:
   - Plugin SPI implementation
   - Custom step binding (annotation, API, DSL)
   - Report data model and plugins
   
2. **Integration**:
   - JUnit engine integration with plugins
   - Spring Boot auto-discovery of custom steps
   - Configuration system (properties, YAML, annotations)

3. **Documentation**:
   - Doc module setup (Sphinx + Dokka)
   - User guides (quick start, tutorial, features, migration, troubleshooting)
   - API documentation generation

4. **Build & Publishing**:
   - Maven publishing configuration
   - Dokka javadoc generation
   - LICENSE file addition
   - Dependency updates (to latest available versions)

5. **Testing**:
   - Plugin lifecycle tests
   - Custom step binding tests
   - Report format validation
   - Integration tests

### For Product Owner Review

Please review and clarify:

**⚠️ CRITICAL: Dependency Versions**

The specified dependency versions don't exist as of April 2026:
- Spring Boot 4.0.5 (latest is 3.x series)
- Jackson 3.1.1 (latest is 2.x series)  
- JUnit Platform 6.0.3 (latest is 1.x series)
- json-schema-validator 3.0.1 (needs verification)
- json-path 3.0.0 (needs verification)

**Recommended Action**: Update spec to specify latest available versions within current major series, OR wait for these future releases (not viable for immediate implementation).

See [research.md section 7](research.md#7-dependency-update-strategy) for detailed analysis.

---

## Artifacts Generated

| Artifact | Status | Description |
|----------|--------|-------------|
| [spec.md](spec.md) | ✅ | Feature specification (8 user stories, 29 functional requirements) |
| [research.md](research.md) | ✅ | Research findings (8 topics investigated) |
| [data-model.md](data-model.md) | ✅ | Entity definitions (15 entities, relationships, invariants) |
| [contracts/plugin-spi.md](contracts/plugin-spi.md) | ✅ | Plugin interface contract with examples |
| [contracts/custom-step-api.md](contracts/custom-step-api.md) | ✅ | Custom step binding API (3 mechanisms) |
| [contracts/report-formats.md](contracts/report-formats.md) | ✅ | Report format specifications (4 formats) |
| [quickstart.md](quickstart.md) | ✅ | Quick start guide (7 common scenarios) |
| [tasks.md](tasks.md) | ⏳ | To be generated via `/speckit.tasks` |

---

## Plan Metadata

**Branch**: `005-library-enhancement`  
**Spec**: [spec.md](spec.md)  
**Status**: Planning Complete, Ready for Task Generation  
**Last Updated**: 2026-04-09  
**Next Command**: `/speckit.tasks`
