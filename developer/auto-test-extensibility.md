# Auto-Test Provider Extensibility

BerryCrush's auto-test feature supports custom test providers through Java's ServiceLoader pattern. This allows users to add custom invalid request tests, security test payloads, and multi-request idempotency tests without modifying the core library.

## Overview

The auto-test system uses three types of providers:

1. **InvalidTestProvider** - Generates invalid values for schema constraint testing
2. **SecurityTestProvider** - Generates security attack payloads
3. **MultiTestProvider** - Executes multi-request idempotency tests

All types are discovered automatically via ServiceLoader, allowing you to add custom providers by simply adding classes to your project.

## Built-in Providers

### Invalid Test Providers

| Test Type | Description |
|-----------|-------------|
| minLength | Strings shorter than minLength constraint |
| maxLength | Strings longer than maxLength constraint |
| pattern | Strings not matching the pattern |
| format | Invalid format values (email, uuid, date, etc.) |
| enum | Values not in the enum list |
| minimum | Numbers below the minimum |
| maximum | Numbers above the maximum |
| type | Wrong type values (e.g., string for number field) |
| required | Missing required fields |
| minItems | Arrays with fewer items than minItems |
| maxItems | Arrays with more items than maxItems |

### Security Test Providers

| Test Type | Display Name | Description |
|-----------|--------------|-------------|
| SQLInjection | SQL Injection | SQL injection attack payloads |
| XSS | XSS | Cross-site scripting payloads |
| PathTraversal | Path Traversal | Path traversal attack patterns |
| CommandInjection | Command Injection | Shell command injection payloads |
| LDAPInjection | LDAP Injection | LDAP query injection payloads |
| XXE | XXE | XML External Entity payloads |
| HeaderInjection | Header Injection | HTTP header injection payloads |

### Multi Test Providers

| Test Type | Display Name | Description |
|-----------|--------------|-------------|
| sequential | Sequential Idempotency | Executes requests one after another |
| concurrent | Concurrent Idempotency | Executes requests in parallel using a thread pool |

## Creating Custom Providers

### Custom Invalid Test Provider

Create a class implementing `InvalidTestProvider`:

```kotlin
package com.example

import org.berrycrush.autotest.provider.InvalidTestProvider
import org.berrycrush.autotest.provider.InvalidTestValue
import io.swagger.v3.oas.models.media.Schema

class NumericOverflowProvider : InvalidTestProvider {
    // Unique identifier for test reports and excludes
    override val testType: String = "numericOverflow"
    
    // Higher priority overrides built-in providers with same testType
    override val priority: Int = 100

    override fun canHandle(schema: Schema<*>): Boolean =
        schema.type == "integer" || schema.type == "number"

    override fun generateInvalidValues(
        fieldName: String,
        schema: Schema<*>,
    ): List<InvalidTestValue> = listOf(
        InvalidTestValue(
            value = Long.MAX_VALUE,
            description = "Numeric overflow value",
        ),
        InvalidTestValue(
            value = Double.POSITIVE_INFINITY,
            description = "Infinity value",
        ),
    )
}
```

### Custom Security Test Provider

Create a class implementing `SecurityTestProvider`:

```kotlin
package com.example

import org.berrycrush.autotest.ParameterLocation
import org.berrycrush.autotest.provider.SecurityTestProvider
import org.berrycrush.autotest.provider.SecurityPayload

class NoSqlInjectionProvider : SecurityTestProvider {
    // Unique identifier for excludes
    override val testType: String = "NoSQLInjection"
    
    // Human-readable name for test reports
    override val displayName: String = "NoSQL Injection"
    
    // Higher priority overrides built-in providers with same testType
    override val priority: Int = 100

    override fun applicableLocations(): Set<ParameterLocation> =
        setOf(ParameterLocation.BODY, ParameterLocation.QUERY)

    override fun generatePayloads(): List<SecurityPayload> = listOf(
        SecurityPayload(
            name = "MongoDB $ne injection",
            payload = "{\"\$ne\": null}",
        ),
        SecurityPayload(
            name = "MongoDB $where injection",
            payload = "{\"\$where\": \"sleep(5000)\"}",
        ),
    )
}
```

### Registering via ServiceLoader

Create a service configuration file in your project:

**`src/main/resources/META-INF/services/org.berrycrush.autotest.provider.InvalidTestProvider`**:
```
com.example.NumericOverflowProvider
```

**`src/main/resources/META-INF/services/org.berrycrush.autotest.provider.SecurityTestProvider`**:
```
com.example.NoSqlInjectionProvider
```

The providers will be automatically discovered and registered when the auto-test system initializes.

## Provider Priority

Each provider has a `priority` property (default: 0 for built-in, 100 for user providers):

- Higher priority providers override lower priority ones with the same `testType`
- Equal priority: later registration wins
- Built-in providers have priority 0
- User providers should use priority >= 100 to override built-in

## Excluding Test Types

Use the `excludes` option in your scenario to skip certain test types:

```
auto-test:
  operations: [createPet]
  types: [invalid, security]
  excludes: [SQLInjection, maxLength, myCustomType]
```

The `excludes` option works with both built-in and custom provider test types.

## Programmatic Registration

You can also register providers programmatically:

```kotlin
val registry = AutoTestProviderRegistry.withDefaults()
registry.registerInvalid(MyCustomInvalidProvider())
registry.registerSecurity(MyCustomSecurityProvider())

val generator = AutoTestGenerator(openApi, registry)
```

Or create an empty registry for full control:

```kotlin
val registry = AutoTestProviderRegistry.empty()
// Only your custom providers will be used
registry.registerInvalid(MyOnlyProvider())
```

## Test Type Naming Conventions

- Use **camelCase** for `testType` (e.g., `numericOverflow`, `NoSQLInjection`)
- Use **human-readable names** for `displayName` (e.g., "Numeric Overflow", "NoSQL Injection")
- The `testType` is used for:
  - Test identification and deduplication
  - `excludes` configuration
  - Provider override matching
- The `displayName` is used for:
  - Test reports (IntelliJ, JUnit XML)
  - Scenario output logs

## Multi-Test Providers

Multi-test providers support idempotency testing by executing requests multiple times in different modes. They are used with the `auto: [multi]` directive to verify API operations produce consistent results.

### MultiTestProvider Interface

```kotlin
interface MultiTestProvider {
    /**
     * Unique identifier for this multi-test mode.
     * Used for display names in test reports: [multi:{testType}]
     * Used in excludes configuration: excludes: [{testType}]
     */
    val testType: String

    /**
     * Human-readable display name for test reports.
     * Defaults to testType if not overridden.
     */
    val displayName: String get() = testType

    /**
     * Priority for provider override. Higher values = higher priority.
     * User-provided providers default to 100, built-in default to 0.
     */
    val priority: Int get() = 0

    /**
     * Execute multi-request test.
     * @param count Number of requests to execute
     * @param executor Function that executes a single request
     * @return Results of all requests and aggregate information
     */
    fun executeMultiTest(
        count: Int,
        executor: (requestIndex: Int) -> RequestResult,
    ): MultiTestResult
}
```

### Built-in Multi-Test Providers

| Test Type | Display Name | Description |
|-----------|--------------|-------------|
| `sequential` | Sequential Idempotency | Executes requests one after another, verifying sequential idempotency |
| `concurrent` | Concurrent Idempotency | Executes requests in parallel using a thread pool (max 20 threads), verifying concurrent access safety |

### Custom Multi-Test Provider

Create a class implementing `MultiTestProvider`:

```kotlin
package com.example

import org.berrycrush.autotest.MultiMode
import org.berrycrush.autotest.MultiTestResult
import org.berrycrush.autotest.RequestResult
import org.berrycrush.autotest.provider.MultiTestProvider

class RetryMultiTestProvider : MultiTestProvider {
    // Unique identifier for this provider
    override val testType: String = "RETRY"
    
    // Human-readable name for test reports
    override val displayName: String = "Retry Test"
    
    // Higher priority overrides built-in providers
    override val priority: Int = 100

    override fun executeMultiTest(
        count: Int,
        executor: () -> RequestResult,
    ): MultiTestResult {
        val results = mutableListOf<RequestResult>()
        var lastResult: RequestResult? = null
        
        // Execute with exponential backoff
        repeat(count) { attempt ->
            if (attempt > 0) {
                Thread.sleep((100 * attempt).toLong())
            }
            lastResult = executor()
            results.add(lastResult!!)
            
            // Stop if successful
            if (lastResult!!.statusCode in 200..299) {
                return@repeat
            }
        }
        
        val totalDuration = results.sumOf { it.durationMs }
        val passed = lastResult?.statusCode in 200..299
        
        return MultiTestResult(
            mode = MultiMode.SEQUENTIAL, // or create custom mode
            requestCount = results.size,
            results = results,
            totalDurationMs = totalDuration,
            passed = passed,
            failureReason = if (!passed) "All retry attempts failed" else null,
        )
    }
}
```

### Registering Multi-Test Providers

Create a service configuration file:

**`src/main/resources/META-INF/services/org.berrycrush.autotest.provider.MultiTestProvider`**:
```
com.example.RetryMultiTestProvider
```

### Programmatic Registration

```kotlin
val registry = AutoTestProviderRegistry.withDefaults()
registry.registerMulti(RetryMultiTestProvider())
```

### Configuration Parameters

Multi-test execution can be configured via parameters:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `multiTestSequentialCount` | 3 | Number of sequential requests |
| `multiTestConcurrentCount` | 5 | Number of concurrent requests |

Set in scenario files (file-level):
```
parameters:
  multiTestSequentialCount: 5
  multiTestConcurrentCount: 10
```

At the feature level:
```
feature: Idempotency Tests
  parameters:
    multiTestSequentialCount: 10
    multiTestConcurrentCount: 20
  
  scenario: Multi test
    when: I test the API
      call ^operation
        auto: [multi]
```

Or at the step level (in the call directive):
```
when: I stress test with custom counts
  call ^operation
    auto: [multi]
    multiTestSequentialCount: 5
    multiTestConcurrentCount: 10
```

Step-level parameters take precedence over file-level and feature-level parameters.
