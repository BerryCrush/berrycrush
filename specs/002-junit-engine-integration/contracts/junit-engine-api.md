# JUnit Engine API Contract

**Feature**: 002-junit-engine-integration  
**Date**: 2026-04-07

## Annotations

### @BerryCrushScenarios

Marks a test class for scenario file discovery and execution.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class BerryCrushScenarios(
    /**
     * Classpath locations to search for scenario files.
     * Supports glob patterns (e.g., "scenarios/*.scenario", "**/*.scenario").
     * Paths are relative to the classpath root.
     */
    vararg val locations: String
)
```

**Example Usage**:
```java
@BerryCrushScenarios(locations = {"scenarios/pets/*.scenario", "scenarios/auth/*.scenario"})
public class PetApiTest {
}
```

---

### @BerryCrushConfiguration

Provides configuration options for scenario execution.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class BerryCrushConfiguration(
    /**
     * Class implementing BerryCrushBindings to provide runtime values.
     * Must have a no-arg constructor.
     */
    val bindings: KClass<out BerryCrushBindings> = DefaultBindings::class,
    
    /**
     * Path to OpenAPI specification file.
     * Overrides any spec discovery from @BerryCrushSpec.
     */
    val openApiSpec: String = "",
    
    /**
     * Timeout per scenario in milliseconds.
     * Default: 30000 (30 seconds).
     */
    val timeout: Long = 30_000L
)
```

**Example Usage**:
```java
@BerryCrushConfiguration(
    bindings = MyCustomBindings.class,
    openApiSpec = "api/petstore.yaml",
    timeout = 60_000L
)
@BerryCrushScenarios(locations = "scenarios/*.scenario")
public class PetApiTest {
}
```

---

## Interfaces

### BerryCrushBindings

Interface for providing runtime bindings to scenario execution.

```kotlin
interface BerryCrushBindings {
    /**
     * Returns variable bindings available to scenarios.
     * Common bindings include baseUrl, authToken, etc.
     */
    fun getBindings(): Map<String, Any>
    
    /**
     * Optional: Override the OpenAPI spec path.
     * Return null to use annotation-specified or default spec.
     */
    fun getOpenApiSpec(): String? = null
    
    /**
     * Optional: Configure the execution context.
     * Called before scenario execution begins.
     */
    fun configure(config: Configuration) {}
}
```

**Example Implementation**:
```java
public class PetstoreBindings implements BerryCrushBindings {
    @LocalServerPort
    private int port;
    
    @Override
    public Map<String, Object> getBindings() {
        return Map.of(
            "baseUrl", "http://localhost:" + port + "/api/v1",
            "authToken", "test-token-12345"
        );
    }
    
    @Override
    public String getOpenApiSpec() {
        return "petstore.yaml";
    }
}
```

---

## TestEngine SPI

### BerryCrushTestEngine

JUnit Platform TestEngine implementation.

| Method | Description |
|--------|-------------|
| `getId(): String` | Returns `"berrycrush"` |
| `discover(request, uniqueId): TestDescriptor` | Discovers scenarios from annotated classes |
| `execute(request)` | Executes discovered scenarios |

**Service Registration**:
```text
# META-INF/services/org.junit.platform.engine.TestEngine
org.berrycrush.berrycrush.junit.engine.BerryCrushTestEngine
```

---

## Test Descriptor Hierarchy

```text
BerryCrushEngineDescriptor (root)
    displayName: "BerryCrush"
    uniqueId: [engine:berrycrush]
    │
    └── ClassTestDescriptor
            displayName: "PetApiTest"
            uniqueId: [engine:berrycrush]/[class:com.example.PetApiTest]
            │
            ├── ScenarioTestDescriptor
            │       displayName: "list-pets.scenario"
            │       uniqueId: [engine:berrycrush]/[class:...]/[scenario:list-pets]
            │
            ├── ScenarioTestDescriptor
            │       displayName: "create-pet.scenario"
            │       uniqueId: [engine:berrycrush]/[class:...]/[scenario:create-pet]
            │
            └── ScenarioTestDescriptor
                    displayName: "delete-pet.scenario"
                    uniqueId: [engine:berrycrush]/[class:...]/[scenario:delete-pet]
```

---

## Error Handling

| Error Condition | Exception Type | Message Format |
|-----------------|----------------|----------------|
| Invalid locations pattern | `IllegalArgumentException` | "Invalid location pattern: {pattern}" |
| No scenarios found | `NoTestsFoundException` | "No scenario files found in locations: {locations}" |
| Bindings class not instantiable | `IllegalStateException` | "Cannot instantiate bindings class: {class}. Ensure it has a no-arg constructor." |
| Scenario parse error | `ScenarioParseException` | "{file}:{line}: {message}" |
| OpenAPI spec not found | `IllegalArgumentException` | "OpenAPI spec not found: {path}" |

---

## Integration with JUnit Platform

### Selective Engine Execution

```java
// Run only berrycrush scenarios
@IncludeEngines("berrycrush")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@BerryCrushScenarios(locations = "scenarios/*.scenario")
@BerryCrushConfiguration(bindings = PetstoreBindings.class)
public class PetstoreScenarioTest {
}
```

### Gradle Configuration

```kotlin
tasks.test {
    useJUnitPlatform {
        // Optional: include only berrycrush engine
        includeEngines("berrycrush")
    }
}
```
