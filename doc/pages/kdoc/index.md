# API Reference (KDoc)

[:material-home: BerryCrush Home](https://berrycrush.org){ .md-button }

The BerryCrush API documentation is generated from Kotlin source code using [Dokka](https://github.com/Kotlin/dokka).

## Modules

Select a module to view its API documentation:

| Module | Description | Link |
|--------|-------------|------|
| :material-cube-outline: **Core** | The core BDD execution engine with OpenAPI integration. Contains: Lexer, Parser, Executor, Step Definitions, Assertions | [:octicons-arrow-right-24: Core API](https://doc.berrycrush.org/kdoc/core/) |
| :material-test-tube: **JUnit** | JUnit 5 Platform integration for running BerryCrush tests. Contains: Test Engine, Annotations, Extensions | [:octicons-arrow-right-24: JUnit API](https://doc.berrycrush.org/kdoc/junit/) |
| :material-leaf: **Spring** | Spring Boot auto-configuration and integration. Contains: Auto-Configuration, Context Helpers | [:octicons-arrow-right-24: Spring API](https://doc.berrycrush.org/kdoc/spring/) |

## Using the API

The API documentation provides detailed information about:

- **Classes and Interfaces** - All public types with their methods and properties
- **Functions** - Standalone functions and extension functions
- **Annotations** - Available annotations for tests and configuration
- **Exceptions** - Exception types thrown by the library

## Package Structure

| Package | Description |
|---------|-------------|
| `org.berrycrush.config` | Configuration classes and builders |
| `org.berrycrush.dsl` | Kotlin DSL for building scenarios |
| `org.berrycrush.executor` | Scenario execution engine |
| `org.berrycrush.junit` | JUnit 5 Platform integration |
| `org.berrycrush.model` | Data models (Scenario, Step, etc.) |
| `org.berrycrush.plugin` | Plugin system interfaces |
| `org.berrycrush.spring` | Spring Boot integration |
| `org.berrycrush.step` | Step definition support |
