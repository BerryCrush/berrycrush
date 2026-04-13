# Feature Specification: Library Enhancement

**Feature Branch**: `005-library-enhancement`  
**Created**: 2026-04-09  
**Status**: Draft  
**Input**: User description: "As a product owner, I want to improve the library feature, dependencies and other things so that the library can be up to date on the market."

## Clarifications

### Session 2026-04-09

- Q: How should users bind custom step implementations to scenario step references? → A: Support multiple approaches - annotation-based binding (@Step), registration API with programmatic mapping, DSL builder pattern, and package scanning
- Q: When multiple plugins are registered, how should the system determine their execution order? → A: Explicit priority/order values, with fallback to registration order if no explicit value is assigned
- Q: How should users specify which report formats to generate? → A: Configuration file/properties for project-wide defaults, plus annotation support for test-specific overrides
- Q: When a plugin throws an exception during a lifecycle hook, what should happen to the test execution? → A: Fail the entire test run immediately; built-in library plugins must be designed to never throw exceptions
- Q: What content sections should the comprehensive user documentation include? → A: Quick start, tutorial, feature guides, API reference, migration guide, troubleshooting
- Q: How should plugins be registered? → A: Support both string-based names ("report:json:output.json") and class-based registration. Plugin name comes from the plugin itself via name property
- Q: What JSON Schema version should be used? → A: JSON Schema 2020-12 (latest stable)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Plugin System with Lifecycle Hooks (Priority: P1)

As a library user, I want to create plugins that hook into the test lifecycle so that I can set up external dependencies (like Docker containers) or perform custom actions when scenarios and steps start and finish.

**Why this priority**: Plugins are foundational infrastructure that other features (like reporting) depend on. This enables extensibility and customization that differentiates the library in the market.

**Independent Test**: Can be fully tested by creating a simple plugin that logs lifecycle events (scenario start/end, step start/end) and verifying the logs appear in correct order.

**Acceptance Scenarios**:

1. **Given** a user creates a plugin implementing the lifecycle interface, **When** a scenario is invoked, **Then** the plugin's onScenarioStart hook is called before the scenario executes
2. **Given** a user creates a plugin implementing the lifecycle interface, **When** a scenario completes (success or failure), **Then** the plugin's onScenarioEnd hook is called with the result
3. **Given** a user creates a plugin implementing the lifecycle interface, **When** a step is invoked, **Then** the plugin's onStepStart hook is called before the step executes
4. **Given** a user creates a plugin implementing the lifecycle interface, **When** a step completes (success or failure), **Then** the plugin's onStepEnd hook is called with the result
5. **Given** multiple plugins are registered with explicit priority values, **When** lifecycle events occur, **Then** all plugins receive the events in priority order (lower values first)
6. **Given** multiple plugins are registered without explicit priority values, **When** lifecycle events occur, **Then** all plugins receive the events in registration order

---

### User Story 2 - Enhanced Test Reporting (Priority: P1)

As a library user, I want detailed test reports that show input values, expected values, and actual values when assertions fail, so that I can quickly understand why tests are failing without debugging.

**Why this priority**: Clear failure diagnostics directly impact user productivity and adoption. Without this, users struggle to diagnose failures.

**Independent Test**: Can be tested by deliberately failing a validation step and verifying the report contains the actual input value, expected value, and clear diff.

**Acceptance Scenarios**:

1. **Given** a validation step fails, **When** the report is generated, **Then** the report shows the actual input value that was validated
2. **Given** a validation step fails, **When** the report is generated, **Then** the report shows the expected value from the assertion
3. **Given** a validation step fails, **When** the report is generated, **Then** the report clearly indicates the difference between expected and actual
4. **Given** tests have completed, **When** the user requests a text report, **Then** a human-readable text report is generated
5. **Given** tests have completed, **When** the user requests a JSON report, **Then** a machine-parseable JSON report is generated
6. **Given** tests have completed, **When** the user requests an XML report, **Then** an XML report is generated
7. **Given** tests have completed, **When** the user requests a JUnit report, **Then** a JUnit-compatible XML report is generated for CI/CD integration
8. **Given** the reporting system, **When** it is implemented, **Then** it is built as a plugin using the plugin mechanism
9. **Given** a configuration file specifies enabled report formats, **When** tests complete, **Then** only the configured formats are generated
10. **Given** a test is annotated with specific report formats, **When** that test completes, **Then** the annotation overrides the configuration for that test

---

### User Story 3 - Custom Step Definition (Priority: P2)

As a library user, I want to define custom steps that execute my own Java/Kotlin code, so that I can extend the testing capabilities for domain-specific validations or setup operations.

**Why this priority**: Flexibility is critical for adoption across different use cases. Users need to extend beyond built-in steps.

**Independent Test**: Can be tested by creating a custom step that performs a calculation, using it in a scenario, and verifying the step executes correctly.

**Acceptance Scenarios**:

1. **Given** a user defines a custom step using annotation-based binding, **When** a scenario references that step, **Then** the custom code is executed
2. **Given** a user defines a custom step using the registration API, **When** a scenario references that step, **Then** the custom code is executed
3. **Given** a user defines a custom step using Kotlin DSL builder, **When** a scenario references that step, **Then** the custom code is executed
4. **Given** a custom step is defined, **When** the step completes, **Then** it integrates with the reporting system to show results
5. **Given** a custom step needs access to test context, **When** it executes, **Then** it can access request/response data and variables

---

### User Story 4 - Dependency Updates for Security and Compatibility (Priority: P2)

As a library maintainer, I want to update all dependencies to specified versions so that the library remains secure and compatible with modern ecosystems.

**Why this priority**: Security vulnerabilities and outdated dependencies prevent enterprise adoption and pose compliance risks.

**Independent Test**: Can be tested by running dependency analysis tools to confirm all specified versions are in use and no known vulnerabilities exist.

**Acceptance Scenarios**:

1. **Given** the library build configuration, **When** dependencies are resolved, **Then** Spring Boot version is 4.0.5
2. **Given** the library build configuration, **When** dependencies are resolved, **Then** Jackson version is 3.1.1
3. **Given** the library build configuration, **When** dependencies are resolved, **Then** com.networknt:json-schema-validator version is 3.0.1
4. **Given** the library build configuration, **When** dependencies are resolved, **Then** com.jayway.jsonpath:json-path version is 3.0.0
5. **Given** the library build configuration, **When** dependencies are resolved, **Then** H2 database version is 2.4.240
6. **Given** the library build configuration, **When** dependencies are resolved, **Then** JUnit and JUnit Platform version is 6.0.3
7. **Given** the updated dependencies, **When** the library is built, **Then** all existing tests pass

---

### User Story 5 - Maven Publishing with Javadoc and Sources (Priority: P2)

As a library consumer, I want the published Maven artifacts to include javadoc and source jars so that I can navigate API documentation and source code directly in my IDE.

**Why this priority**: Standard Maven publishing expectations for professional libraries. Essential for developer experience and adoption.

**Independent Test**: Can be tested by publishing to local Maven repository and verifying -javadoc.jar and -sources.jar artifacts are present alongside the main jar.

**Acceptance Scenarios**:

1. **Given** the library is published to Maven, **When** artifacts are inspected, **Then** a -javadoc.jar artifact is present
2. **Given** the library is published to Maven, **When** artifacts are inspected, **Then** a -sources.jar artifact is present
3. **Given** the javadoc jar, **When** it is generated, **Then** it is produced by Dokka plugin version 2.2.0
4. **Given** the javadoc jar, **When** it is inspected, **Then** it contains documentation for all public APIs

---

### User Story 6 - Comprehensive User Documentation (Priority: P3)

As a library user, I want comprehensive documentation with guides, tutorials, and examples so that I can learn how to use the library effectively without reading source code.

**Why this priority**: Documentation is critical for adoption but can be developed in parallel with core features.

**Independent Test**: Can be tested by building the documentation and verifying HTML output is generated with all expected sections.

**Acceptance Scenarios**:

1. **Given** the doc module, **When** the Sphinx build task is executed, **Then** HTML documentation is generated
2. **Given** the documentation source, **When** it is inspected, **Then** it is written in reStructuredText format
3. **Given** the documentation, **When** it is reviewed, **Then** it includes a quick start guide
4. **Given** the documentation, **When** it is reviewed, **Then** it includes a tutorial section
5. **Given** the documentation, **When** it is reviewed, **Then** it includes feature guides
6. **Given** the documentation, **When** it is reviewed, **Then** it includes a migration guide
7. **Given** the documentation, **When** it is reviewed, **Then** it includes a troubleshooting section
8. **Given** the Dokka API documentation, **When** it is generated, **Then** it is output to the doc module's output directory and linked from the main documentation

---

### User Story 7 - API Documentation via Dokka (Priority: P3)

As a library user, I want auto-generated API documentation from KDoc comments so that I can reference complete API details without reading source code.

**Why this priority**: API docs complement user documentation and are generated automatically from code.

**Independent Test**: Can be tested by running Dokka and verifying HTML API documentation is generated for all public classes.

**Acceptance Scenarios**:

1. **Given** Dokka plugin version 2.2.0 is configured, **When** the documentation task runs, **Then** API documentation is generated from KDoc
2. **Given** API documentation is generated, **When** the output is inspected, **Then** it is located in the doc module's output directory
3. **Given** the generated API documentation, **When** it is reviewed, **Then** all public classes and methods are documented

---

### User Story 8 - Apache License 2.0 (Priority: P3)

As a potential library user, I want clear licensing information so that I can verify the library is compatible with my project's licensing requirements.

**Why this priority**: Clear licensing removes legal barriers to adoption but is a simple one-time addition.

**Independent Test**: Can be tested by verifying LICENSE file exists at repository root with correct Apache License 2.0 content.

**Acceptance Scenarios**:

1. **Given** the repository root, **When** the LICENSE file is inspected, **Then** it contains the full Apache License 2.0 text
2. **Given** the LICENSE file, **When** it is reviewed, **Then** it includes the correct copyright year and holder information

---

### Edge Cases

- What happens when a plugin throws an exception during a lifecycle hook? The test run fails immediately with a clear error message indicating which plugin and lifecycle event caused the failure.
- What happens when multiple reporting plugins are registered? All should generate their respective outputs independently.
- What happens when a custom step definition is invalid or missing required annotations? Clear error messages should guide the user.
- What happens when dependencies have transitive conflicts after updates? Build should fail fast with clear conflict resolution guidance.
- What happens when Dokka cannot parse a KDoc comment? Documentation should be generated with warnings, not failures.
- What happens when a built-in reporting plugin encounters an I/O error (e.g., disk full)? The plugin must handle it gracefully and log the error without throwing an exception.

## Requirements *(mandatory)*

### Functional Requirements

**Plugin System**

- **FR-001**: System MUST provide a plugin interface with lifecycle hooks for scenario start, scenario end, step start, and step end events
- **FR-002**: System MUST pass relevant context (scenario name, step details, execution results) to plugin hooks
- **FR-003**: System MUST support registration of multiple plugins with optional explicit priority/order values
- **FR-003a**: System MUST execute plugins with explicit priority values in priority order (lower values first)
- **FR-003b**: System MUST fall back to registration order for plugins without explicit priority values
- **FR-003c**: System MUST support name-based plugin registration with string identifiers (e.g., "report:json:output.json")
- **FR-003d**: System MUST support class-based plugin registration for custom plugin classes
- **FR-004**: System MUST fail the entire test run immediately when any plugin throws an exception during lifecycle hook execution
- **FR-004a**: All built-in plugins (text, JSON, XML, JUnit reporters) MUST be designed to handle all error conditions without throwing exceptions

**Custom Steps**

- **FR-005**: System MUST provide annotation-based binding for custom steps (e.g., @Step("step name")) in both Java and Kotlin
- **FR-006**: System MUST provide a registration API allowing programmatic mapping of step names to function implementations
- **FR-006a**: System MUST provide a Kotlin DSL builder pattern for defining custom steps
- **FR-006b**: System MUST provide package scanning to auto-discover @Step annotated methods in specified packages
- **FR-007**: System MUST allow custom steps to access test context including request data, response data, and scenario variables
- **FR-008**: System MUST integrate custom step results into the standard reporting flow

**Reporting**

- **FR-009**: System MUST capture and include actual input values when a validation fails
- **FR-010**: System MUST capture and include expected values when a validation fails
- **FR-011**: System MUST implement reporting as a plugin using the plugin mechanism
- **FR-011a**: System MUST support configuration file/properties for specifying enabled report formats (e.g., `berrycrush.reports=text,junit,json`)
- **FR-011b**: System MUST support annotation-based override of report formats for specific tests
- **FR-012**: System MUST provide a text report format plugin (enabled by default)
- **FR-013**: System MUST provide a JSON report format plugin
- **FR-014**: System MUST provide an XML report format plugin
- **FR-015**: System MUST provide a JUnit-compatible XML report format plugin for CI/CD integration

**Dependency Updates**

- **FR-016**: Build MUST use Spring Boot version 4.0.5
- **FR-017**: Build MUST use Jackson version 3.1.1
- **FR-018**: Build MUST use com.networknt:json-schema-validator version 3.0.1
- **FR-019**: Build MUST use com.jayway.jsonpath:json-path version 3.0.0
- **FR-020**: Build MUST use H2 database version 2.4.240
- **FR-021**: Build MUST use JUnit and JUnit Platform version 6.0.3

**Documentation**

- **FR-022**: Project MUST include a `doc` module for documentation
- **FR-023**: Documentation MUST be written in reStructuredText format
- **FR-024**: Build MUST include a Sphinx task to generate HTML documentation
- **FR-024a**: Documentation MUST include a quick start guide
- **FR-024b**: Documentation MUST include a tutorial section
- **FR-024c**: Documentation MUST include feature guides
- **FR-024d**: Documentation MUST include a migration guide
- **FR-024e**: Documentation MUST include a troubleshooting section
- **FR-025**: API documentation MUST be generated from KDoc using Dokka plugin version 2.2.0
- **FR-026**: Dokka-generated API documentation MUST be output to the doc module's output directory

**Maven Publishing**

- **FR-027**: Maven publication MUST include a -javadoc.jar artifact generated by Dokka
- **FR-028**: Maven publication MUST include a -sources.jar artifact

**Licensing**

- **FR-029**: Repository MUST include a LICENSE file containing Apache License 2.0

### Key Entities

- **Plugin**: Represents an extension point that receives lifecycle events and can perform custom actions. Has registration order and enabled state.
- **LifecycleEvent**: Represents a test lifecycle moment (scenario start/end, step start/end) with associated context data.
- **CustomStep**: Represents a user-defined step implementation with binding mechanism to scenario DSL.
- **TestReport**: Represents the output of a test run with details about each scenario, step, assertions, and their pass/fail status with diagnostic data.
- **ReportPlugin**: A specific type of plugin that generates test reports in a particular format.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can create and register a plugin in under 15 minutes following documentation
- **SC-002**: Failed assertions display actual value, expected value, and clear difference in all report formats
- **SC-003**: JUnit XML reports are parseable by standard CI/CD tools (Jenkins, GitHub Actions, GitLab CI)
- **SC-004**: Custom step definitions can be created and used in scenarios with no more than 3 lines of boilerplate code
- **SC-005**: All dependency versions match specified requirements with no known critical or high severity CVEs
- **SC-006**: HTML documentation builds successfully and covers all major features
- **SC-007**: Published Maven artifacts include main jar, javadoc jar, and sources jar
- **SC-008**: Library users can confirm licensing compatibility by inspecting the LICENSE file

## Assumptions

- Users have basic Java/Kotlin development experience and can implement interfaces
- Docker setup plugins are user-implemented; the library provides hooks but not Docker integration itself
- Spring Boot 4.0.5 and other specified dependency versions are released and stable
- Existing tests will be updated as needed to work with new dependency versions
- The doc module will be a separate Gradle subproject
- Default report plugins (text, json, xml, junit) are bundled with the library and enabled by configuration
- Copyright holder for the Apache License 2.0 will be the project maintainer(s)
- KDoc comments exist or will be added to public APIs before Dokka generation
