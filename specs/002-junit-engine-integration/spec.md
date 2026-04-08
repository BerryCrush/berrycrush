# Feature Specification: JUnit Engine Integration

**Feature Branch**: `002-junit-engine-integration`  
**Created**: 2026-04-07  
**Status**: Draft  
**Input**: User description: "The library must provide custom JUnit engine, called `lemoncheck`. The engine must resolve the `*.scenario` files by checking the `LemonCheckScenarios` annotation. And custom configuration can be taken from the `LemonCheckConfiguration` annotation. To maximize the interoperability to Java, the `sample` project must be rewritten in Java 21. `sample/petstore` project must implement API of the `petstore.yaml` with H2 database. Then its test module must test with the newly created JUnit engine, with `*.scenario` files. If needed, add custom binding to test."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Run Scenario Tests via JUnit Engine (Priority: P1)

As a developer using lemon-check, I want to run my BDD scenario files (`.scenario`) through JUnit 5 by annotating my test class with `@LemonCheckScenarios`, so that my scenario tests integrate seamlessly with my existing test infrastructure and IDE.

**Why this priority**: This is the core functionality - without the JUnit engine, users cannot run scenario tests through standard test frameworks. This enables IDE integration, CI/CD pipelines, and build tool support.

**Independent Test**: Can be fully tested by creating a test class annotated with `@LemonCheckScenarios(locations = "scenarios/")`, running it via JUnit Platform, and verifying that scenario files are discovered and executed with pass/fail results reported correctly.

**Acceptance Scenarios**:

1. **Given** a test class annotated with `@LemonCheckScenarios(locations = "scenarios/")`, **When** JUnit 5 discovers tests, **Then** all `.scenario` files in the specified location are discovered as test cases
2. **Given** a discovered scenario test, **When** the scenario executes successfully, **Then** JUnit reports it as passed
3. **Given** a discovered scenario test, **When** the scenario fails, **Then** JUnit reports it as failed with clear error details
4. **Given** a test class without `@LemonCheckScenarios`, **When** JUnit 5 runs, **Then** the lemoncheck engine does not interfere with other tests

---

### User Story 2 - Configure Scenario Execution (Priority: P2)

As a developer, I want to configure the scenario execution behavior via `@LemonCheckConfiguration` annotation, so that I can customize bindings, timeouts, and other execution parameters per test class.

**Why this priority**: Configuration flexibility is essential for real-world usage where tests need different settings, but basic execution (P1) must work first.

**Independent Test**: Can be tested by creating a test class with `@LemonCheckConfiguration` specifying custom bindings, running scenarios, and verifying the bindings are applied correctly.

**Acceptance Scenarios**:

1. **Given** a test class with `@LemonCheckConfiguration(bindings = MyBindings.class)`, **When** scenarios execute, **Then** custom bindings from `MyBindings` are used for scenario execution
2. **Given** a test class without `@LemonCheckConfiguration`, **When** scenarios execute, **Then** default configuration is applied
3. **Given** a test class with `@LemonCheckConfiguration`, **When** the configuration class is invalid or missing, **Then** a clear error message is reported at test discovery time

---

### User Story 3 - Integrate with Spring Boot Tests (Priority: P3)

As a developer using Spring Boot, I want to run lemon-check scenarios within a Spring Boot test context using `@SpringBootTest` and `@IncludeEngines("lemoncheck")`, so that my API scenarios can run against a live Spring application context with full dependency injection.

**Why this priority**: Spring Boot integration demonstrates Java interoperability and enables testing real applications, but requires the core engine (P1) and configuration (P2) to be working first.

**Independent Test**: Can be tested by creating a Spring Boot test class with `@SpringBootTest`, `@IncludeEngines("lemoncheck")`, and `@LemonCheckScenarios`, running petstore API scenarios, and verifying HTTP calls reach the running Spring application.

**Acceptance Scenarios**:

1. **Given** a Spring Boot test class with `@SpringBootTest` and `@IncludeEngines("lemoncheck")`, **When** tests run, **Then** the Spring application context is started before scenarios execute
2. **Given** a running Spring Boot test context, **When** scenarios make HTTP requests to the application, **Then** responses are received and validated correctly
3. **Given** a Spring Boot test with `@LocalServerPort`, **When** scenarios reference the base URL, **Then** scenarios can dynamically use the allocated port

---

### User Story 4 - Petstore Sample Implementation (Priority: P3)

As a developer evaluating lemon-check, I want a complete sample implementation of the petstore API in Java 21 with Spring Boot and H2, so that I can see how to integrate scenario testing into a real application.

**Why this priority**: The sample serves as documentation and validation of the JUnit engine, but is dependent on the engine being complete.

**Independent Test**: Can be tested by running the sample project's tests and verifying all petstore scenarios pass against the H2-backed API.

**Acceptance Scenarios**:

1. **Given** the sample petstore project, **When** I run `./gradlew :samples:petstore:test`, **Then** all scenario tests pass
2. **Given** the petstore API implementation, **When** scenarios exercise CRUD operations for pets, **Then** data is persisted to and retrieved from the H2 database
3. **Given** the petstore sample, **When** I review the test classes, **Then** I can understand how to configure and use `@LemonCheckScenarios` and `@LemonCheckConfiguration`

---

### Edge Cases

- What happens when a scenario file has syntax errors? The engine must report a clear parsing error at test discovery time.
- What happens when the specified scenario location does not exist? The engine must report a clear error rather than silently passing.
- What happens when multiple test classes specify overlapping scenario locations? Each test class must independently discover and execute its scenarios.
- How does the engine handle scenario files with no executable steps? The scenario should be reported as skipped or empty.
- What happens if a custom binding class cannot be instantiated? A clear error must be reported identifying the binding class and cause.

## Requirements *(mandatory)*

### Functional Requirements

**JUnit Engine (lemon-check/junit module)**:

- **FR-001**: System MUST provide a JUnit 5 TestEngine implementation registered with engine ID `lemoncheck`
- **FR-002**: System MUST discover scenario files based on `@LemonCheckScenarios` annotation on test classes
- **FR-003**: System MUST support `locations` attribute in `@LemonCheckScenarios` to specify scenario file paths (relative to classpath)
- **FR-004**: System MUST support glob patterns in scenario locations (e.g., `scenarios/**/*.scenario`)
- **FR-005**: System MUST create individual test descriptors for each scenario file discovered
- **FR-006**: System MUST execute scenarios and report results through JUnit's test execution lifecycle
- **FR-007**: System MUST provide `@LemonCheckConfiguration` annotation for custom configuration per test class
- **FR-008**: System MUST support `bindings` attribute in `@LemonCheckConfiguration` to specify custom binding classes
- **FR-009**: System MUST be compatible with JUnit Platform's `@IncludeEngines` annotation for selective engine execution
- **FR-010**: System MUST report clear error messages for invalid configurations, missing files, or parsing errors

**Sample Petstore Implementation (samples/petstore module)**:

- **FR-011**: Sample project MUST be implemented in Java 21
- **FR-012**: Sample project MUST implement all endpoints defined in `petstore.yaml` using Spring Boot
- **FR-013**: Sample project MUST use H2 in-memory database for data persistence
- **FR-014**: Sample project MUST include test classes using `@SpringBootTest` with `@IncludeEngines("lemoncheck")`
- **FR-015**: Sample project MUST include scenario files testing the petstore API CRUD operations
- **FR-016**: Sample project MUST demonstrate custom bindings if needed for HTTP client configuration

### Key Entities

- **TestEngine**: The JUnit 5 engine implementation that discovers and executes scenario tests
- **LemonCheckScenarios**: Annotation marking a test class for scenario discovery with `locations` attribute
- **LemonCheckConfiguration**: Annotation providing configuration options including `bindings` for custom binding classes
- **ScenarioDescriptor**: JUnit test descriptor representing a single scenario file to execute
- **Pet**: Domain entity with id, name, status, category, tags, and price attributes (as defined in petstore.yaml)
- **Bindings**: Custom classes that provide variable bindings and HTTP client configuration for scenario execution

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All scenario files in specified locations are discovered within 2 seconds for up to 100 scenarios
- **SC-002**: Scenario test results are correctly reported in IDE test runners (IntelliJ, VS Code, Eclipse)
- **SC-003**: 100% of petstore API endpoints defined in `petstore.yaml` are implemented and covered by scenario tests
- **SC-004**: Sample project tests complete successfully in under 30 seconds including Spring context startup
- **SC-005**: Users can run scenario tests without any additional configuration beyond adding the annotation
- **SC-006**: Error messages for configuration issues clearly identify the problem and suggest resolution
- **SC-007**: The JUnit engine integrates with Gradle test task and Maven Surefire/Failsafe without special configuration

## Assumptions

- JUnit 5 (Jupiter) is already a dependency in the project or will be added as a test dependency
- Scenario files follow the `.scenario` extension and the format defined by lemon-check core module
- Spring Boot 3.x will be used for the sample project, compatible with Java 21
- H2 database is suitable for sample/testing purposes; production usage would use a different database
- The lemon-check core module provides the scenario parsing and execution capabilities that the JUnit engine will utilize
- Test resources are available on the classpath for scenario file discovery
- Users are familiar with JUnit 5 annotation-based test discovery patterns
