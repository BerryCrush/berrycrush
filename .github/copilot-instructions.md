# berrycrush Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-09

## Active Technologies
- Kotlin 2.3.20 (JUnit engine), Java 21 (sample petstore) + JUnit Platform 6.0.3, Spring Boot 4.0.5, H2, Spring Data JPA (002-junit-engine-integration)
- H2 2.3.232 in-memory database (sample only) (002-junit-engine-integration)
- Kotlin 2.3.20, Java 21 + JUnit 6.0.3, JUnit Platform 6.0.3, Swagger Parser 2.1.39, Jackson 3.1.1, JSONPath 3.0.0 (003-operation-id-prefix)
- Kotlin 2.3.20, Java 21 + JUnit Platform 6.0.3, Spring Boot 4.0.5, spring-boot-starter-test (004-spring-context-integration)
- N/A (testing infrastructure) (004-spring-context-integration)
- Kotlin 2.3.20 / Java 21 + JUnit Platform 6.0.3 (currently), Spring Boot 4.0.5 (currently), Swagger Parser 2.1.39, Jackson 3.1.1, JSONPath 3.0.0, json-schema-validator 3.0.1 (005-library-enhancement)
- H2 2.3.232 (sample only, not production) (005-library-enhancement)

- Java 21 + Kotlin 2.3.20 (Kotlin DSL for scenarios) (001-openapi-bdd-testing)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 21 + Kotlin 2.3.20 (Kotlin DSL for scenarios)

## Code Style

Java 21 + Kotlin 2.3.20 (Kotlin DSL for scenarios): Follow standard conventions

## Recent Changes
- 005-library-enhancement: Added Kotlin 2.3.20 / Java 21 + JUnit Platform 6.0.3 (currently), Spring Boot 4.0.5 (currently), Swagger Parser 2.1.39, Jackson 3.1.1, JSONPath 3.0.0, json-schema-validator 3.0.1
- 004-spring-context-integration: Added Kotlin 2.3.20, Java 21 + JUnit Platform 6.0.3, Spring Boot 4.0.5, spring-boot-starter-test
- 003-operation-id-prefix: Added Kotlin 2.3.20, Java 21 + JUnit 6.0.3, JUnit Platform 6.0.3, Swagger Parser 2.1.39, Jackson 3.1.1, JSONPath 3.0.0


<!-- MANUAL ADDITIONS START -->
# Approval and feedback

When all the user stories are finished, then the agent MUST ask the approval as well as the
feedback via vscode_askQuestions tools for further improvements. The agent MUST ask the user to review the generated code and provide feedback on any issues or improvements that can be made. The agent MUST also ask for approval to merge the generated code into the main branch of the repository.
<!-- MANUAL ADDITIONS END -->
