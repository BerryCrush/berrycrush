# Research: JUnit Engine Integration

**Feature**: 002-junit-engine-integration  
**Date**: 2026-04-07  
**Status**: Complete

## Research Questions

### 1. JUnit 5 TestEngine Implementation Pattern

**Question**: How to implement a custom JUnit 5 TestEngine that discovers tests based on annotations rather than method scanning?

**Decision**: Implement `org.junit.platform.engine.TestEngine` interface with custom discovery logic that scans for `@LemonCheckScenarios` annotated classes and resolves scenario files from the classpath.

**Rationale**:
- JUnit Platform provides the `TestEngine` SPI for custom test frameworks
- `EngineDiscoveryRequest` provides `ClassSelector` and `PackageSelector` for finding annotated classes
- `TestDescriptor` hierarchy maps naturally to scenario files (engine → test class → scenario file)
- The engine ID (`lemoncheck`) allows use with `@IncludeEngines("lemoncheck")` for selective execution

**Alternatives Considered**:
- **JUnit Extension only**: Already exists in current codebase, but doesn't support `.scenario` file discovery from classpath; requires programmatic scenario definition
- **Cucumber-style runner**: Would require a separate runner class per test; less flexible than annotation-based approach

**Implementation Notes**:
```kotlin
class LemonCheckTestEngine : TestEngine {
    override fun getId(): String = "lemoncheck"
    
    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        // 1. Create root descriptor
        // 2. Find classes with @LemonCheckScenarios
        // 3. For each class, resolve scenario files from locations
        // 4. Create ScenarioTestDescriptor for each file
    }
    
    override fun execute(
        request: ExecutionRequest
    ) {
        // Walk test descriptors, execute scenarios, report results
    }
}
```

---

### 2. Scenario File Discovery from Classpath

**Question**: How to resolve `*.scenario` files from classpath locations specified in annotation?

**Decision**: Use `ClassLoader.getResources()` with glob pattern matching via `java.nio.file.PathMatcher` for classpath resource discovery.

**Rationale**:
- Test resources are on classpath during test execution
- Glob patterns (`scenarios/**/*.scenario`) are standard and familiar
- Works with both file system (development) and JAR resources (packaged)

**Alternatives Considered**:
- **Scanning entire classpath**: Too slow and may find unintended files
- **Explicit file listing**: Tedious and error-prone for users
- **ServiceLoader**: Not applicable for resource files

**Implementation Notes**:
```kotlin
fun discoverScenarios(classLoader: ClassLoader, locationPattern: String): List<URL> {
    // Handle glob patterns vs direct paths
    // Use PathMatcher for filtering
    // Return URLs for scenario files
}
```

---

### 3. Spring Boot Test Integration

**Question**: How to integrate with `@SpringBootTest` so scenarios can access running application context?

**Decision**: The TestEngine should not directly depend on Spring. Instead, users provide bindings via `@LemonCheckConfiguration(bindings = ...)` that can access Spring's `@LocalServerPort` or `@Value` injected fields.

**Rationale**:
- Keeps engine free of Spring dependency (optional runtime dependency)
- Users have full control over how HTTP client is configured
- Works with any DI framework or plain Java

**Alternatives Considered**:
- **Direct Spring integration**: Would couple engine to Spring; limits flexibility
- **Auto-configuration**: Too magical; harder to debug and customize

**Implementation Notes**:
```java
// User-provided bindings class (in test code)
public class PetstoreBindings implements LemonCheckBindings {
    @LocalServerPort
    private int port;
    
    @Override
    public Map<String, Object> getBindings() {
        return Map.of("baseUrl", "http://localhost:" + port);
    }
}

// Test class
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@IncludeEngines("lemoncheck")
@LemonCheckScenarios(locations = "scenarios/*.scenario")
@LemonCheckConfiguration(bindings = PetstoreBindings.class)
class PetstoreScenarioTest {
}
```

---

### 4. Configuration Annotation Design

**Question**: What attributes should `@LemonCheckConfiguration` support?

**Decision**: Support `bindings` (required for custom bindings), optional `openApiSpec` (override spec path), and optional `timeout` (per-scenario timeout).

**Rationale**:
- `bindings`: Essential for providing runtime values (base URL, auth tokens, etc.)
- `openApiSpec`: Allows per-test-class spec override (useful for multi-module projects)
- `timeout`: Prevents runaway scenarios; sensible default of 30s

**Alternatives Considered**:
- **Properties file**: Less discoverable; harder to vary per test class
- **Programmatic configuration**: Requires boilerplate; annotations are cleaner

**Final Annotation Design**:
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LemonCheckConfiguration(
    val bindings: KClass<out LemonCheckBindings> = DefaultBindings::class,
    val openApiSpec: String = "",
    val timeout: Long = 30_000L  // milliseconds
)
```

---

### 5. Spring Boot 3.x with H2 and JPA Best Practices

**Question**: How to structure the Spring Boot sample for clarity and testability?

**Decision**: Use standard layered architecture with JPA entities, Spring Data JPA repositories, and REST controllers. Use `spring-boot-starter-data-jpa` and `h2` runtime dependency.

**Rationale**:
- Familiar pattern for Spring developers
- JPA provides simple CRUD operations
- H2 in-memory database requires no external setup
- Spring Boot 3.x compatible with Java 21

**Dependencies**:
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":lemon-check:junit"))
}
```

---

### 6. Backward Compatibility with Existing Extension

**Question**: How to maintain backward compatibility with existing `LemonCheckExtension` while adding TestEngine?

**Decision**: Keep existing extension for programmatic DSL-based tests. TestEngine is a separate discovery mechanism for `.scenario` file-based tests. Both can coexist.

**Rationale**:
- Existing users can continue using `ScenarioTest` base class with DSL
- New users can use annotation-based `.scenario` file discovery
- No breaking changes to existing API

**Coexistence Strategy**:
- Extension: `@ExtendWith(LemonCheckExtension::class)` + `ScenarioTest` base class
- Engine: `@LemonCheckScenarios(locations = ...)` + `@IncludeEngines("lemoncheck")`

---

## Summary

All NEEDS CLARIFICATION items resolved:
- TestEngine implementation pattern defined
- Scenario discovery mechanism specified
- Spring Boot integration approach determined
- Configuration annotation designed
- Sample project structure and dependencies established
- Backward compatibility strategy confirmed
