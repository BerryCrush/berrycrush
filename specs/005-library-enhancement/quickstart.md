# Quick Start: Library Enhancement Features

**Version**: 0.1.0 (Enhanced)  
**Date**: 2026-04-09

## Overview

This guide helps you quickly get started with the new berrycrush features:
- 🔌 **Plugin System** - Extend with lifecycle hooks
- 🎯 **Custom Steps** - Define your own test steps
- 📊 **Enhanced Reporting** - Detailed failure diagnostics
- 📚 **Documentation** - Comprehensive guides and API docs

---

## Quick Start 1: Using Enhanced Reporting

### Enable Multiple Report Formats

**Option A: Configuration File**

Create `src/test/resources/berrycrush.properties`:

```properties
berrycrush.reports.enabled=text,json,junit
berrycrush.reports.outputDir=build/reports/berrycrush
```

**Option B: Annotation**

```kotlin
@Suite
@IncludeEngines("berrycrush")
@BerryCrushScenarios(locations = "scenarios/*.scenario")
@BerryCrushReports([ReportFormat.TEXT, ReportFormat.JSON, ReportFormat.JUNIT])
class PetApiTest
```

### View Detailed Failure Information

When a test fails, reports now include:
- ✅ Expected vs Actual values
- ✅ Full HTTP request/response
- ✅ Clear difference highlighting

**Example JUnit XML Output**:

```xml
<failure message="Expected status 201 but got 400" type="StatusAssertionError">
Expected: 201
Actual: 400

Request:
  POST /api/pets
  Body: {"name":"Fluffy","category":"invalid"}

Response:
  400 Bad Request
  Body: {"error":"Invalid category"}
</failure>
```

---

## Quick Start 2: Creating a Simple Plugin

### Step 1: Implement the Plugin Interface

```kotlin
class LoggingPlugin : BerryCrushPlugin {
    override val name: String = "Request Logger"
    override val priority: Int = 0
    
    override fun onStepEnd(context: StepContext, result: StepResult) {
        if (context.stepType == StepType.CALL && context.response != null) {
            println("[${context.response.statusCode}] ${context.request?.method} ${context.request?.url}")
        }
    }
}
```

### Step 2: Register the Plugin

```kotlin
@Suite
@IncludeEngines("berrycrush")
@BerryCrushScenarios(locations = "scenarios/*.scenario")
@BerryCrushConfiguration(
    plugins = [LoggingPlugin::class]
)
class PetApiTest
```

### Step 3: Run Tests

```bash
./gradlew test
```

Output:

```
[200] GET /api/pets
[201] POST /api/pets
[200] GET /api/pets/123
```

---

## Quick Start 3: Creating Custom Steps (Annotation-Based)

### Step 1: Create Step Class

```kotlin
class MathSteps {
    
    @Step("calculate sum of {int} and {int}")
    fun addNumbers(a: Int, b: Int, context: StepContext): StepResult {
        context.scenarioContext.variables["sum"] = a + b
        return StepResult.success()
    }
    
    @Step("result should be {int}")
    fun assertResult(expected: Int, context: StepContext): StepResult {
        val actual = context.scenarioContext.variables["sum"] as? Int
            ?: return StepResult.error(IllegalStateException("No sum calculated"))
        
        return if (actual == expected) {
            StepResult.success()
        } else {
            StepResult.failure(
                AssertionFailure(
                    message = "Sum doesn't match",
                    expected = expected,
                    actual = actual
                )
            )
        }
    }
}
```

### Step 2: Register Step Class

```kotlin
@BerryCrushConfiguration(
    stepClasses = [MathSteps::class]
)
class MathApiTest
```

### Step 3: Use in Scenario File

Create `src/test/resources/scenarios/math.scenario`:

```
scenario: Addition test
  when I perform calculation
    calculate sum of 5 and 3
  then I verify result
    result should be 8
```

---

## Quick Start 4: Creating Custom Steps (Kotlin DSL)

### Step 1: Define Steps with DSL

```kotlin
val databaseSteps = steps {
    
    step<String>("create user {string}") { username ->
        // Create user in database
        val user = User(username)
        database.save(user)
        variables["userId"] = user.id
        success()
    }
    
    step<String>("user {string} should exist") { username ->
        val exists = database.userExists(username)
        if (exists) {
            success()
        } else {
            failure("User not found", expected = username, actual = "not in database")
        }
    }
}
```

### Step 2: Register DSL Steps

```kotlin
@BerryCrushConfiguration(
    stepRegistry = databaseSteps
)
class DatabaseTest
```

### Step 3: Use in Scenario

```
scenario: User management
  when I create a user
    create user "alice"
  then the user exists
    user "alice" should exist
```

---

## Quick Start 5: Docker Integration Plugin

### Create Testcontainers Plugin

```kotlin
class PostgresPlugin : BerryCrushPlugin {
    override val priority: Int = -100  // Start early
    override val name: String = "PostgreSQL Testcontainer"
    
    private lateinit var postgres: PostgreSQLContainer<*>
    
    override fun onScenarioStart(context: ScenarioContext) {
        // Start container once
        if (!::postgres.isInitialized) {
            postgres = PostgreSQLContainer<Nothing>("postgres:15")
            postgres.start()
            
            // Make connection details available to scenarios
            context.variables["db.url"] = postgres.jdbcUrl
            context.variables["db.user"] = postgres.username
            context.variables["db.pass"] = postgres.password
        }
    }
}
```

### Use in Test

```kotlin
@SpringBootTest
@BerryCrushConfiguration(
    plugins = [PostgresPlugin::class]
)
class ApiWithDatabaseTest
```

Now your API tests run against a real PostgreSQL instance!

---

## Quick Start 6: Spring Integration with Custom Steps

### Step 1: Create Spring Component with Steps

```kotlin
@Component
class UserSteps(
    private val userRepository: UserRepository
) {
    
    @Step("database contains {int} users")
    fun assertUserCount(expected: Int, context: StepContext): StepResult {
        val actual = userRepository.count()
        return if (actual.toInt() == expected) {
            StepResult.success()
        } else {
            StepResult.failure(
                AssertionFailure(
                    message = "User count mismatch",
                    expected = expected,
                    actual = actual
                )
            )
        }
    }
}
```

### Step 2: Enable Auto-Discovery

```kotlin
@SpringBootTest
@BerryCrushContextConfiguration  // Enables Spring integration
@BerryCrushScenarios(locations = "scenarios/*.scenario")
class DatabaseIntegrationTest
```

Spring automatically discovers `@Step` methods in `@Component` classes!

---

## Quick Start 7: Viewing Generated Documentation

### Build Documentation

```bash
./gradlew buildSphinx    # User documentation
./gradlew dokkaHtml      # API documentation
```

### View Documentation

```bash
open doc/build/html/index.html        # User guide
open doc/build/dokka/index.html       # API reference
```

Documentation includes:
- 📖 Quick start guide
- 📚 Tutorial (step-by-step walkthrough)
- 🎯 Feature guides (plugins, custom steps, reporting)
- 🔧 Troubleshooting
- 📦 Migration guide
- 🔍 Complete API reference (Dokka)

---

## Common Scenarios

### Scenario: Test Logging + JUnit Reports for CI

```kotlin
@BerryCrushConfiguration(
    plugins = [LoggingPlugin::class],
    reports = [ReportFormat.JUNIT]
)
```

### Scenario: Docker Setup + Custom Validation Steps

```kotlin
@BerryCrushConfiguration(
    plugins = [TestcontainersPlugin::class],
    stepClasses = [DatabaseValidationSteps::class]
)
```

### Scenario: Spring + Multiple Report Formats

```kotlin
@SpringBootTest
@BerryCrushContextConfiguration
@BerryCrushReports([
    ReportFormat.TEXT,
    ReportFormat.JSON,
    ReportFormat.JUNIT
])
```

---

## Configuration Reference

### berrycrush.properties

```properties
# Report formats
berrycrush.reports.enabled=text,json,xml,junit
berrycrush.reports.outputDir=build/reports/berrycrush

# Report options
berrycrush.reports.junit.includeSystemOut=true
berrycrush.reports.json.pretty=true
berrycrush.reports.text.color=true

# Plugin configuration
berrycrush.plugins.discovery.enabled=true
```

### berrycrush.yml

```yaml
berrycrush:
  reports:
    enabled: [text, json, junit]
    outputDir: build/reports/berrycrush
    formats:
      junit:
        includeSystemOut: true
      json:
        pretty: true
  plugins:
    discovery:
      enabled: true
```

---

## Next Steps

### Learning Resources

1. **Full Documentation**: Run `./gradlew buildSphinx` and open `doc/build/html/index.html`
2. **Plugin SPI Reference**: See [contracts/plugin-spi.md](contracts/plugin-spi.md)
3. **Custom Step API**: See [contracts/custom-step-api.md](contracts/custom-step-api.md)
4. **Report Formats**: See [contracts/report-formats.md](contracts/report-formats.md)
5. **API Documentation**: Run `./gradlew dokkaHtml` and open `doc/build/dokka/index.html`

### Example Projects

- `samples/plugin-examples/` - Plugin implementation examples
- `samples/custom-steps-annotation/` - Annotation-based custom steps
- `samples/custom-steps-dsl/` - Kotlin DSL examples
- `samples/spring-integration/` - Spring Boot integration examples

### Community & Support

- **Documentation**: `doc/build/html/index.html` (after build)
- **API Reference**: `doc/build/dokka/index.html` (after build)
- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions

---

## Troubleshooting

### Plugin Not Executing

**Problem**: Plugin hooks not being called

**Solution**: Check plugin priority and registration:

```kotlin
class MyPlugin : BerryCrushPlugin {
    override val priority: Int = 0  // Ensure priority is set
    override val name: String = "MyPlugin"  // For debugging
}
```

### Custom Step Not Found

**Problem**: "No step definition found for: {step text}"

**Solution**: Verify step registration:

```kotlin
// Check step class is registered
@BerryCrushConfiguration(
    stepClasses = [MySteps::class]  // ← Ensure class is listed
)
```

### Report Not Generated

**Problem**: Expected report file not created

**Solution**: Check output directory exists and is writable:

```bash
mkdir -p build/reports/berrycrush
ls -la build/reports/berrycrush/
```

### Dependency Version Conflicts

**Problem**: Specified dependency versions don't exist

**Solution**: This specification references future versions. Use latest available:

```kotlin
// gradle/libs.versions.toml
[versions]
spring-boot = "3.4.1"    # Not 4.0.5
jackson = "2.17.0"       # Not 3.1.1
junit = "5.11.4"         # Not 6.0.3
```

See [research.md](research.md) section 7 for details.

---

## Migration from Previous Version

No breaking changes - all existing scenarios work as-is!

New features are opt-in:
- Reports: Text report enabled by default, others opt-in
- Plugins: No plugins unless explicitly registered
- Custom steps: Use new mechanisms alongside built-in steps

---

## Summary

This quick start covered:
- ✅ Enabling enhanced reporting
- ✅ Creating simple plugins
- ✅ Defining custom steps (annotation and DSL)
- ✅ Docker integration example
- ✅ Spring Boot integration
- ✅ Building and viewing documentation

For comprehensive guides, see the full documentation in `doc/`.

**Ready to start?** Pick a quick start above and try it out!
