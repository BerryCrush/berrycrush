# Contract: Plugin SPI

**Version**: 1.0.0  
**Date**: 2026-04-09  
**Type**: Service Provider Interface

## Overview

This contract defines the Plugin Service Provider Interface (SPI) that allows users to extend berrycrush with custom lifecycle hooks. Plugins receive events at key points during scenario execution (scenario start/end, step start/end) and can perform custom actions like setup, teardown, logging, or reporting.

---

## Core Interface

###BerryCrushPlugin

Main plugin interface that all plugins must implement.

```kotlin
interface BerryCrushPlugin {
    /**
     * Unique identifier for this plugin.
     * Must be unique within a test run.
     */
    val id: String get() = this::class.qualifiedName ?: "unknown"
    
    /**
     * Plugin priority for execution ordering.
     * Lower priority values execute first.
     * Default: 0 (executes after negative priorities, before positive)
     * 
     * Example:
     * - priority = -100: Setup/infrastructure plugins
     * - priority = 0: Default (reporting)
     * - priority = 100: Cleanup/finalization plugins
     */
    val priority: Int get() = 0
    
    /**
     * Human-readable plugin name for logging and debugging.
     */
    val name: String get() = this::class.simpleName ?: "Unknown Plugin"
    
    /**
     * Called before each scenario starts.
     * 
     * @param context Scenario execution context with variables, metadata
     * @throws Any exception thrown will fail the entire test run
     */
    fun onScenarioStart(context: ScenarioContext) {
        // Default: no-op
    }
    
    /**
     * Called after each scenario ends.
     * 
     * @param context Scenario execution context
     * @param result Scenario result (status, duration, failures)
     * @throws Any exception thrown will fail the entire test run
     */
    fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
        // Default: no-op
    }
    
    /**
     * Called before each step starts.
     * 
     * @param context Step execution context with request details, parent scenario
     * @throws Any exception thrown will fail the entire test run
     */
    fun onStepStart(context: StepContext) {
        // Default: no-op
    }
    
    /**
     * Called after each step ends.
     * 
     * @param context Step execution context
     * @param result Step result (status, duration, failure details)
     * @throws Any exception thrown will fail the entire test run
     */
    fun onStepEnd(context: StepContext, result: StepResult) {
        // Default: no-op
    }
}
```

---

## Context Objects

### ScenarioContext

Provides access to scenario-level execution context.

```kotlin
interface ScenarioContext {
    /** Scenario name from scenario file */
    val scenarioName: String
    
    /** Path to scenario file */
    val scenarioFile: Path
    
    /** Mutable variables extracted during scenario execution */
    val variables: MutableMap<String, Any>
    
    /** Read-only scenario metadata (tags, etc.) */
    val metadata: Map<String, String>
    
    /** When scenario started */
    val startTime: Instant
    
    /** Scenario tags for filtering/organization */
    val tags: Set<String>
}
```

### StepContext

Provides access to step-level execution context.

```kotlin
interface StepContext {
    /** Full step description text */
    val stepDescription: String
    
    /** Step type (CALL, ASSERT, EXTRACT, CUSTOM) */
    val stepType: StepType
    
    /** Index of this step within scenario (0-based) */
    val stepIndex: Int
    
    /** Parent scenario context */
    val scenarioContext: ScenarioContext
    
    /** HTTP request details (null for non-CALL steps) */
    val request: HttpRequest?
    
    /** HTTP response details (null until response received) */
    val response: HttpResponse?
    
    /** OpenAPI operation ID (null if not applicable) */
    val operationId: String?
}

enum class StepType {
    CALL,     // HTTP API call
    ASSERT,   // Assertion/validation
    EXTRACT,  // Variable extraction
    CUSTOM    // User-defined custom step
}
```

### HttpRequest

Snapshot of HTTP request details.

```kotlin
interface HttpRequest {
    val method: String              // GET, POST, etc.
    val url: String                 // Full URL
    val headers: Map<String, List<String>>
    val body: String?               // Request body (if any)
    val timestamp: Instant          // When request was sent
}
```

### HttpResponse

Snapshot of HTTP response details.

```kotlin
interface HttpResponse {
    val statusCode: Int
    val statusMessage: String
    val headers: Map<String, List<String>>
    val body: String?               // Response body
    val duration: Duration          // Time to receive response
    val timestamp: Instant          // When response was received
}
```

---

## Result Objects

### ScenarioResult

Outcome of scenario execution.

```kotlin
interface ScenarioResult {
    /** Result status */
    val status: ResultStatus
    
    /** Total scenario execution time */
    val duration: Duration
    
    /** Index of first failed step (-1 if no failures) */
    val failedStep: Int
    
    /** Exception if status is ERROR */
    val error: Throwable?
    
    /** Results for all executed steps */
    val stepResults: List<StepResult>
}
```

### StepResult

Outcome of step execution.

```kotlin
interface StepResult {
    /** Result status */
    val status: ResultStatus
    
    /** Step execution time */
    val duration: Duration
    
    /** Failure details if status is FAILED */
    val failure: AssertionFailure?
    
    /** Exception if status is ERROR */
    val error: Throwable?
}
```

### ResultStatus

```kotlin
enum class ResultStatus {
    PASSED,   // Step/scenario succeeded
    FAILED,   // Assertion failure
    SKIPPED,  // Not executed (dependency failed)
    ERROR     // Unexpected exception
}
```

### AssertionFailure

Detailed failure information for debugging.

```kotlin
interface AssertionFailure {
    /** Human-readable failure message */
    val message: String
    
    /** Expected value (for comparison assertions) */
    val expected: Any?
    
    /** Actual value (for comparison assertions) */
    val actual: Any?
    
    /** Computed difference (for string/object comparisons) */
    val diff: String?
    
    /** Which step failed */
    val stepDescription: String
    
    /** Type of assertion (status, jsonpath, etc.) */
    val assertionType: String
    
    /** HTTP request snapshot at failure time */
    val requestSnapshot: HttpRequest?
    
    /** HTTP response snapshot at failure time */
    val responseSnapshot: HttpResponse?
}
```

---

## Plugin Registration

### Name-Based Registration

Use string identifiers for built-in plugins with optional configuration:

```kotlin
@BerryCrushConfiguration(
    plugins = [
        "report:json:build/reports/output.json",
        "report:junit:build/test-results/junit.xml",
        "logging"
    ],
    pluginClasses = [
        MyCustomPlugin::class  // For custom plugin classes
    ]
)
class MyApiTest
```

**Name Format**: `<plugin-name>[:<param1>[:<param2>]]`

Examples:
- `"report:json"` - JSON report with default output path
- `"report:json:custom/path.json"` - JSON report with custom path
- `"logging"` - Simple plugin with no parameters

**Plugin Name Resolution**: The plugin name is provided by the plugin's `name` property:

```kotlin
class MyCustomPlugin : BerryCrushPlugin {
    override val name: String = "my-custom"  // Used in name-based registration
}
```

### Class-Based Registration

```kotlin
// Explicit class registration
@BerryCrushConfiguration(
    pluginClasses = [
        MyCustomPlugin::class,
        AnotherPlugin::class
    ]
)
class MyApiTest
```

### Service Provider Interface (SPI) Discovery

Create file: `src/main/resources/META-INF/services/org.berrycrush.berrycrush.plugin.BerryCrushPlugin`

```text
com.example.myplugin.MyCustomPlugin
com.example.reporting.CustomReporter
```

### Registration Priority

1. Name-based plugins (from `plugins` parameter) are resolved first
2. Class-based plugins (from `pluginClasses` parameter) are registered second
3. SPI-discovered plugins are registered last

---

## Plugin Examples

### Example 1: Logging Plugin

```kotlin
class LoggingPlugin : BerryCrushPlugin {
    override val priority: Int = -50  // Run early
    override val name: String = "Logging Plugin"
    
    override fun onScenarioStart(context: ScenarioContext) {
        println("[SCENARIO] Starting: ${context.scenarioName}")
    }
    
    override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
        println("[SCENARIO] Finished: ${context.scenarioName} - ${result.status} (${result.duration})")
    }
    
    override fun onStepStart(context: StepContext) {
        println("  [STEP] ${context.stepDescription}")
    }
    
    override fun onStepEnd(context: StepContext, result: StepResult) {
        val symbol = if (result.status == ResultStatus.PASSED) "✓" else "✗"
        println("  $symbol ${context.stepDescription} (${result.duration})")
    }
}
```

### Example 2: Docker Testcontainers Plugin

```kotlin
class TestcontainersPlugin : BerryCrushPlugin {
    override val priority: Int = -100  // Run first (setup)
    override val name: String = "Testcontainers Plugin"
    
    private lateinit var postgres: PostgreSQLContainer<*>
    
    override fun onScenarioStart(context: ScenarioContext) {
        // Start container before first scenario
        if (!::postgres.isInitialized) {
            postgres = PostgreSQLContainer<Nothing>("postgres:15")
            postgres.start()
            
            // Inject connection details into scenario variables
            context.variables["db.url"] = postgres.jdbcUrl
            context.variables["db.username"] = postgres.username
            context.variables["db.password"] = postgres.password
        }
    }
    
    override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
        // Could reset database state here if needed
        if (result.status == ResultStatus.PASSED) {
            // Cleanup after each scenario
        }
    }
}
```

### Example 3: Performance Monitoring Plugin

```kotlin
class PerformanceMonitorPlugin : BerryCrushPlugin {
    override val priority: Int = 0
    override val name: String = "Performance Monitor"
    
    private val metrics = mutableMapOf<String, MutableList<Duration>>()
    
    override fun onStepEnd(context: StepContext, result: StepResult) {
        if (context.stepType == StepType.CALL) {
            val operation = context.operationId ?: "unknown"
            metrics.getOrPut(operation) { mutableListOf() }.add(result.duration)
        }
    }
    
    override fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {
        // After all scenarios, could write performance report
        if (context.scenarioName == "Last Scenario") {  // Hacky, but demonstrates concept
            File("performance-report.txt").writeText(
                metrics.entries.joinToString("\\n") { (operation, durations) ->
                    val avg = durations.map { it.toMillis() }.average()
                    val max = durations.maxOf { it.toMillis() }
                    "$operation: avg=${avg}ms, max=${max}ms"
                }
            )
        }
    }
}
```

---

## Built-in Report Plugins

These plugins are provided by the library and use the same SPI.

### Built-in Plugin Names

| Plugin Name | Parameters | Description |
|-------------|-----------|-------------|
| `report:text` | `[output-path]` | Human-readable text report |
| `report:json` | `[output-path]` | JSON format report (2020-12 schema) |
| `report:xml` | `[output-path]` | Generic XML report |
| `report:junit` | `[output-path]` | JUnit-compatible XML |

**Default Output Paths** (when not specified):
- text: `build/reports/berrycrush/report.txt`
- json: `build/reports/berrycrush/report.json`
- xml: `build/reports/berrycrush/report.xml`
- junit: `build/test-results/berrycrush/TEST-berrycrush.xml`

### TextReportPlugin

Generates human-readable text reports.

```kotlin
// Name-based registration with default path
@BerryCrushConfiguration(plugins = ["report:text"])

// Name-based with custom path
@BerryCrushConfiguration(plugins = ["report:text:custom/report.txt"])

// Class-based for advanced configuration
class TextReportPlugin(
    val outputPath: Path = Paths.get("build/reports/berrycrush/report.txt"),
    val useColor: Boolean = true
) : BerryCrushPlugin {
    override val priority: Int = 100  // Run late (after tests)
    override val name: String = "report:text"
    
    // Implementation uses onScenarioEnd to collect results
    // Generates report at the end
}
```

### JsonReportPlugin

Generates machine-parseable JSON reports using JSON Schema 2020-12.

```kotlin
// Name-based registration
@BerryCrushConfiguration(plugins = ["report:json:output/report.json"])

// Class-based
class JsonReportPlugin(
    val outputPath: Path = Paths.get("build/reports/berrycrush/report.json"),
    val prettyPrint: Boolean = true
) : BerryCrushPlugin {
    override val priority: Int = 100
    override val name: String = "report:json"
}
```

### XmlReportPlugin

Generates generic XML reports.

```kotlin
// Name-based registration
@BerryCrushConfiguration(plugins = ["report:xml:reports/test.xml"])
```

### JunitReportPlugin

Generates JUnit-compatible XML for CI/CD integration.

```kotlin
// Name-based registration
@BerryCrushConfiguration(plugins = ["report:junit"])

// Class-based for advanced configuration
class JunitReportPlugin(
    val outputPath: Path = Paths.get("build/test-results/berrycrush/TEST-berrycrush.xml"),
    val includeSystemOut: Boolean = false
) : BerryCrushPlugin {
    override val priority: Int = 100
    override val name: String = "report:junit"
}
```

---

## Error Handling Rules

1. **No Exception Tolerance**: Any exception thrown from a plugin hook will **immediately fail the entire test run**
2. **Built-in Plugin Resilience**: All built-in plugins (Text, JSON, XML, JUnit reporters) must handle errors gracefully without throwing exceptions
3. **User Plugin Responsibility**: User-written plugins must catch and handle their own exceptions if they want graceful degradation

### Example: Resilient Plugin

```kotlin
class ResilientPlugin : BerryCrushPlugin {
    override fun onStepEnd(context: StepContext, result: StepResult) {
        try {
            // Plugin logic that might fail
            sendToExternalService(context, result)
        } catch (e: Exception) {
            // Log but don't throw - prevents test run failure
            System.err.println("Plugin error: ${e.message}")
        }
    }
}
```

---

## Contract Guarantees

**Library guarantees to plugins**:
1. Lifecycle hooks called in deterministic order: onScenarioStart → onStepStart → onStepEnd → onScenarioEnd
2. Context objects remain valid for the duration of the hook invocation
3. Plugins with lower priority execute before plugins with higher priority
4. Plugins with same priority execute in registration order

**Plugins must guarantee to library**:
1. Thread-safety if plugin maintains state
2. Reasonable execution time (hooks should be fast, <10ms recommended)
3. No side effects on context objects (treat as read-only except `variables` map)

---

## Versioning & Compatibility

**SPI Stability**: This SPI is **provisional** in version 0.1.0. Breaking changes may occur before 1.0.0.

**Breaking vs. Non-Breaking Changes**:
- ✅ Non-breaking: Adding new methods with default implementations
- ✅ Non-breaking: Adding new context properties
- ❌ Breaking: Removing methods
- ❌ Breaking: Changing method signatures
- ❌ Breaking: Removing context properties

---

## Migration Path

If plugin API changes in future versions, migration guide will be provided in library documentation.
