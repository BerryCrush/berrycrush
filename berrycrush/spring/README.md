# BerryCrush Spring Integration

This module provides Spring TestContext integration for berrycrush scenario tests, enabling dependency injection (`@Autowired`, `@LocalServerPort`) in bindings classes.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation(project(":berrycrush:spring"))
    // or when published:
    // testImplementation("org.berrycrush:berrycrush-spring:x.y.z")
}
```

## Usage

### Without Spring Integration

For simple API tests without Spring context, use berrycrush directly:

```java
@Suite
@IncludeEngines("berrycrush")
@BerryCrushScenarios(locations = "scenarios/*.scenario")
@BerryCrushConfiguration(bindings = SimpleBindings.class, openApiSpec = "api.yaml")
public class SimpleApiTest {
}
```

```java
public class SimpleBindings implements BerryCrushBindings {
    
    @Override
    public Map<String, Object> getBindings() {
        return Map.of(
            "baseUrl", "http://localhost:8080/api"
        );
    }
    
    @Override
    public String getOpenApiSpec() {
        return "api.yaml";
    }
}
```

### With Spring Integration

For Spring Boot integration tests, add `@SpringBootTest` and `@BerryCrushContextConfiguration`:

```java
@Suite
@IncludeEngines("berrycrush")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@BerryCrushContextConfiguration
@BerryCrushScenarios(locations = "scenarios/*.scenario")
@BerryCrushConfiguration(bindings = SpringBindings.class, openApiSpec = "api.yaml")
public class SpringApiTest {
}
```

```java
@Component
@Lazy  // Required for @LocalServerPort timing
public class SpringBindings implements BerryCrushBindings {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private SomeService service;
    
    @Override
    public Map<String, Object> getBindings() {
        return Map.of(
            "baseUrl", "http://localhost:" + port + "/api"
        );
    }
    
    @Override
    public String getOpenApiSpec() {
        return "api.yaml";
    }
    
    @Override
    public void configure(Configuration config) {
        config.setBaseUrl("http://localhost:" + port + "/api");
    }
}
```

## Scenario File Format

Create `.scenario` files in your test resources:

```
# scenarios/list-items.scenario
scenario: List all items
  when I request all items
    call ^listItems
  then I get a successful response
    assert status 200
    assert $.items notEmpty
```

## Key Annotations

| Annotation | Purpose |
|------------|---------|
| `@BerryCrushContextConfiguration` | Enables Spring context integration |
| `@SpringBootTest` | Starts Spring Boot test context |
| `@BerryCrushScenarios` | Specifies scenario file locations |
| `@BerryCrushConfiguration` | Configures bindings and OpenAPI spec |

## Important Notes

### @Lazy Annotation
When using `@LocalServerPort`, add `@Lazy` to your bindings class. The port is only available after the server starts, which happens after initial bean creation.

### Test Isolation
Scenarios in the same test class share the same Spring context. Database changes persist across scenarios. Use filename prefixes to control execution order:
- `01-read-tests.scenario` (runs first)
- `99-delete-tests.scenario` (runs last)

### Error Handling
Missing `@SpringBootTest` with `@BerryCrushContextConfiguration` produces a clear error message explaining the required configuration.

## Complete Example

See [samples/petstore](../samples/petstore) for a complete working example with:
- Spring Boot application with H2 database
- Multiple scenario files
- Spring-injected bindings with `@LocalServerPort`
