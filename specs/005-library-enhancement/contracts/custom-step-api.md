# Contract: Custom Step API

**Version**: 1.0.0  
**Date**: 2026-04-09  
**Type**: Public API

## Overview

This contract defines four mechanisms for users to create custom steps that extend lemon-check's built-in step library:
1. **Annotation-based binding** - Java/Kotlin friendly, IDE autocomplete support
2. **Registration API** - Programmatic, runtime registration
3. **Kotlin DSL builder** - Idiomatic Kotlin syntax
4. **Package scanning** - Automatic discovery of @Step methods in specified packages

All four mechanisms share a common backend and provide equivalent functionality.

---

## Mechanism 1: Annotation-Based Binding

### @Step Annotation

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Step(
    /**
     * Step pattern with parameter placeholders.
     * 
     * Supported placeholders:
     * - {int}: Integer parameter
     * - {string}: String parameter (captures until next word)
     * - {word}: Single word
     * - {*}: Wildcard (captures rest of line)
     * 
     * Examples:
     * - "calculate sum of {int} and {int}"
     * - "user with name {string} exists"
     * - "send email to {*}"
     */
    val pattern: String,
    
    /**
     * Optional description for documentation.
     */
    val description: String = ""
)
```

### Java Example

```java
public class MathSteps {
    
    @Step(pattern = "calculate sum of {int} and {int}", 
          description = "Calculates the sum of two integers")
    public StepResult calculateSum(int a, int b, StepContext context) {
        int sum = a + b;
        context.getScenarioContext().getVariables().put("sum", sum);
        return StepResult.success();
    }
    
    @Step(pattern = "assert sum equals {int}")
    public StepResult assertSum(int expected, StepContext context) {
        Integer actual = (Integer) context.getScenarioContext().getVariables().get("sum");
        if (actual == null) {
            return StepResult.error(new IllegalStateException("No sum calculated"));
        }
        if (!actual.equals(expected)) {
            return StepResult.failure(
                AssertionFailure.builder()
                    .message("Sum does not match")
                    .expected(expected)
                    .actual(actual)
                    .build()
            );
        }
        return StepResult.success();
    }
}
```

### Kotlin Example

```kotlin
class MathSteps {
    
    @Step("calculate sum of {int} and {int}")
    fun calculateSum(a: Int, b: Int, context: StepContext): StepResult {
        context.scenarioContext.variables["sum"] = a + b
        return StepResult.success()
    }
    
    @Step("assert sum equals {int}")
    fun assertSum(expected: Int, context: StepContext): StepResult {
        val actual = context.scenarioContext.variables["sum"] as? Int
            ?: return StepResult.error(IllegalStateException("No sum calculated"))
        
        return if (actual == expected) {
            StepResult.success()
        } else {
            StepResult.failure(
                AssertionFailure(
                    message = "Sum does not match",
                    expected = expected,
                    actual = actual
                )
            )
        }
    }
}
```

### Registration

```kotlin
// Option 1: Explicit class registration
@LemonCheckConfiguration(
    stepClasses = [MathSteps::class]
)
class MyApiTest

// Option 2: Package scanning
@LemonCheckConfiguration(
    stepPackages = ["com.example.steps", "com.example.validators"]
)
class MyApiTest

// Option 3: Spring auto-discovery
@SpringBootTest
@LemonCheckConfiguration
class MyApiTest {
    // Spring will auto-discover @Step methods in @Component classes
}
```

---

## Mechanism 4: Package Scanning

### Automatic Discovery

Enable package-based discovery to automatically find all `@Step` annotated methods in specified packages:

```kotlin
@LemonCheckConfiguration(
    stepPackages = [
        "com.example.steps",           // Scan this package
        "com.example.validators"       // And this one
    ]
)
class MyApiTest
```

### Scanning Rules

1. **Recursive**: Scans package and all sub-packages
2. **Class Types**: Discovers `@Step` methods in any class (no need for special interfaces)
3. **Visibility**: Only public methods are discovered
4. **Static vs Instance**: Both static and instance methods supported
5. **Initialization**: Instance methods require no-arg constructor on the class

### Java Example

```java
package com.example.steps;

public class ValidationSteps {
    
    @Step(pattern = "email {string} is valid")
    public StepResult validateEmail(String email, StepContext context) {
        boolean valid = email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
        return valid ? StepResult.success() 
                     : StepResult.failure(
                           AssertionFailure.builder()
                               .message("Invalid email format")
                               .expected("valid email format")
                               .actual(email)
                               .build()
                       );
    }
}

// Another class in the same package
public class DatabaseSteps {
    
    @Step(pattern = "database contains {int} records")
    public static StepResult checkRecordCount(int expected, StepContext context) {
        // Static method - no instance needed
        int actual = Database.count();
        return actual == expected ? StepResult.success() 
                                  : StepResult.failure(
                                        AssertionFailure.builder()
                                            .message("Record count mismatch")
                                            .expected(expected)
                                            .actual(actual)
                                            .build()
                                    );
    }
}
```

### Kotlin Example

```kotlin
package com.example.steps

class ApiSteps {
    
    @Step("API is healthy")
    fun checkHealth(context: StepContext): StepResult {
        val response = httpClient.get("/health")
        return if (response.status == 200) {
            StepResult.success()
        } else {
            StepResult.failure(
                AssertionFailure(
                    message = "API health check failed",
                    expected = 200,
                    actual = response.status
                )
            )
        }
    }
}

// Another class in a sub-package
package com.example.steps.validation

object SchemaSteps {
    
    @Step("response matches schema {string}")
    @JvmStatic  // Required for object methods to be discovered
    fun validateSchema(schemaName: String, context: StepContext): StepResult {
        val schema = loadSchema(schemaName)
        val valid = schema.validate(context.response?.body)
        return if (valid) {
            StepResult.success()
        } else {
            StepResult.failure(
                AssertionFailure(
                    message = "Schema validation failed",
                    expected = "valid against $schemaName",
                    actual = "validation errors"
                )
            )
        }
    }
}
```

### Combined Configuration

Mix package scanning with explicit class registration:

```kotlin
@LemonCheckConfiguration(
    stepPackages = ["com.example.steps"],      // Auto-discover in this package
    stepClasses = [SpecialSteps::class]        // Plus this specific class (from any package)
)
```

### Performance Considerations

**Package scanning happens once at test initialization**, not per-test:
- First test in suite: ~50-100ms for typical package (10-20 classes)
- Subsequent tests: <1ms (cached)
- Recommendation: Use specific packages, not entire application packages

### Scan Filtering

Exclude classes from scanning using package naming conventions:

```kotlin
@LemonCheckConfiguration(
    stepPackages = ["com.example.steps"],
    excludePackages = ["com.example.steps.internal"]  // Don't scan internal package
)
```

### Spring Integration

When using Spring Boot, package scanning is automatic for `@Component` classes:

```kotlin
@Component  // Spring component
class SpringManagedSteps @Autowired constructor(
    private val userRepository: UserRepository
) {
    
    @Step("user {string} exists")
    fun userExists(username: String, context: StepContext): StepResult {
        val exists = userRepository.existsByUsername(username)
        return if (exists) {
            StepResult.success()
        } else {
            StepResult.failure(
                AssertionFailure(
                    message = "User not found",
                    expected = username,
                    actual = "not in database"
                )
            )
        }
    }
}
```

No `stepPackages` needed - Spring auto-discovery handles it:

```kotlin
@SpringBootTest
@LemonCheckConfiguration  // Just enable, auto-discovers Spring components
```

---

## Mechanism Comparison

| Feature | Annotation | Registration API | Kotlin DSL | Package Scanning |
|---------|-----------|------------------|------------|------------------|
| Language | Java/Kotlin | Java/Kotlin | Kotlin only | Java/Kotlin |
| Type Safety | High | Medium | High | High |
| IDE Support | Excellent | Good | Excellent | Excellent |
| Runtime Flexibility | Low | High | Medium | Low |
| Boilerplate | Low | Medium | Low | Lowest |
| Best For | Static steps | Dynamic steps | Kotlin projects | Large step libraries |

---

## Mechanism 2: Registration API

### StepRegistry Interface

```kotlin
interface StepRegistry {
    /**
     * Register a custom step with pattern matching.
     * 
     * @param pattern Regex pattern or simple pattern string
     * @param executor Function that executes the step
     */
    fun register(pattern: String, executor: (StepContext) -> StepResult)
    
    /**
     * Register a custom step with parameter extraction.
     * 
     * @param pattern Regex pattern
     * @param executor Function receiving match result and context
     */
    fun register(pattern: Regex, executor: (MatchResult, StepContext) -> StepResult)
    
    /**
     * Unregister a custom step.
     */
    fun unregister(pattern: String)
}
```

### Java Example

```java
public class MyStepBindings implements LemonCheckBindings {
    
    @Override
    public void registerSteps(StepRegistry registry) {
        // Simple pattern
        registry.register("calculate sum of (\\d+) and (\\d+)", (context) -> {
            // Manual parameter extraction from step text
            String stepText = context.getStepDescription();
            Pattern p = Pattern.compile("calculate sum of (\\d+) and (\\d+)");
            Matcher m = p.matcher(stepText);
            if (m.find()) {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                context.getScenarioContext().getVariables().put("sum", a + b);
            }
            return StepResult.success();
        });
        
        // Regex pattern
        Pattern pattern = Pattern.compile("calculate sum of (\\d+) and (\\d+)");
        registry.register(pattern, (match, context) -> {
            int a = Integer.parseInt(match.group(1));
            int b = Integer.parseInt(match.group(2));
            context.getScenarioContext().getVariables().put("sum", a + b);
            return StepResult.success();
        });
    }
}
```

### Kotlin Example

```kotlin
class MyStepBindings : LemonCheckBindings {
    
    override fun registerSteps(registry: StepRegistry) {
        // Simple pattern with automatic parameter extraction
        registry.register("calculate sum of {int} and {int}") { context ->
            val (a, b) = context.extractParameters<Int, Int>()
            context.scenarioContext.variables["sum"] = a + b
            StepResult.success()
        }
        
        // Regex pattern
        registry.register("calculate sum of (\\d+) and (\\d+)".toRegex()) { match, context ->
            val (a, b) = match.destructured
            context.scenarioContext.variables["sum"] = a.toInt() + b.toInt()
            StepResult.success()
        }
    }
}
```

---

## Mechanism 3: Kotlin DSL Builder

### Steps DSL

```kotlin
fun steps(block: StepBuilder.() -> Unit): StepRegistry
```

### StepBuilder Interface

```kotlin
interface StepBuilder {
    /**
     * Define a step with pattern.
     */
    fun step(pattern: String, executor: StepExecutor.() -> Unit)
    
    /**
     * Define a step with regex.
     */
    fun step(pattern: Regex, executor: StepExecutor.(MatchResult) -> Unit)
    
    /**
     * Define a step with parameter extraction.
     */
    inline fun <reified T1> step(
        pattern: String, 
        crossinline executor: StepExecutor.(T1) -> Unit
    )
    
    inline fun <reified T1, reified T2> step(
        pattern: String,
        crossinline executor: StepExecutor.(T1, T2) -> Unit
    )
    
    // Additional overloads for 3, 4, 5 parameters...
}
```

### StepExecutor Context

```kotlin
interface StepExecutor {
    /** Access to scenario variables */
    val variables: MutableMap<String, Any>
    
    /** Access to step context */
    val context: StepContext
    
    /** Mark step as passed */
    fun success(): StepResult
    
    /** Mark step as failed */
    fun failure(message: String, expected: Any? = null, actual: Any? = null): StepResult
    
    /** Mark step as error */
    fun error(throwable: Throwable): StepResult
}
```

### Kotlin DSL Example

```kotlin
val mySteps = steps {
    
    // Type-safe parameter extraction
    step<Int, Int>("calculate sum of {int} and {int}") { a, b ->
        variables["sum"] = a + b
        success()
    }
    
    // Single parameter
    step<Int>("assert sum equals {int}") { expected ->
        val actual = variables["sum"] as? Int
            ?: return@step error(IllegalStateException("No sum calculated"))
        
        if (actual == expected) {
            success()
        } else {
            failure(
                message = "Sum does not match",
                expected = expected,
                actual = actual
            )
        }
    }
    
    // Regex with match result
    step("""send email to (.+@.+\..+)""".toRegex()) { match ->
        val email = match.groupValues[1]
        // Email sending logic
        variables["lastEmailSent"] = email
        success()
    }
    
    // No parameters
    step("reset calculator") {
        variables.clear()
        success()
    }
}

// Register the steps
@LemonCheckConfiguration(
    stepRegistry = mySteps
)
class MyApiTest
```

---

## Common Types

### StepResult

```kotlin
sealed class StepResult {
    data class Success(val duration: Duration = Duration.ZERO) : StepResult()
    data class Failure(val failure: AssertionFailure) : StepResult()
    data class Error(val throwable: Throwable) : StepResult()
    
    companion object {
        fun success(): StepResult = Success()
        fun failure(failure: AssertionFailure): StepResult = Failure(failure)
        fun error(throwable: Throwable): StepResult = Error(throwable)
    }
}
```

### AssertionFailure

```kotlin
data class AssertionFailure(
    val message: String,
    val expected: Any? = null,
    val actual: Any? = null,
    val diff: String? = null,
    val stepDescription: String = "",
    val assertionType: String = "custom",
    val requestSnapshot: HttpRequest? = null,
    val responseSnapshot: HttpResponse? = null
) {
    class Builder {
        fun message(msg: String): Builder
        fun expected(value: Any?): Builder
        fun actual(value: Any?): Builder
        fun diff(diffText: String): Builder
        fun build(): AssertionFailure
    }
    
    companion object {
        fun builder(): Builder = Builder()
    }
}
```

---

## Parameter Extraction

### Supported Placeholders

| Placeholder | Type | Description | Example |
|-------------|------|-------------|---------|
| `{int}` | Int | Integer number | `42`, `-10` |
| `{long}` | Long | Long integer | `1234567890L` |
| `{double}` | Double | Decimal number | `3.14`, `-0.5` |
| `{boolean}` | Boolean | true/false | `true`, `false` |
| `{string}` | String | Quoted string | `"hello world"` |
| `{word}` | String | Single word | `username` |
| `{*}` | String | Rest of line | Everything after |

### Auto-Conversion

The library automatically converts captured text to the parameter type:

```kotlin
@Step("set value to {int}")
fun setValue(value: Int, context: StepContext): StepResult {
    // 'value' is already an Int, no manual parsing needed
}

@Step("user with name {string} and age {int}")
fun createUser(name: String, age: Int, context: StepContext): StepResult {
    // Both parameters are type-safe
}
```

---

## Pattern Matching Rules

1. **First Match Wins**: If multiple patterns match a step, the first registered pattern is used
2. **Exact Before Pattern**: Exact string matches take precedence over patterns
3. **Parameter Validation**: Type conversion failures result in ERROR status
4. **Ambiguity Detection**: Library warns if patterns overlap (runtime warning, not error)

### Example: Pattern Priority

```kotlin
// Registered first
@Step("create user {string}")
fun createUserSimple(name: String, ctx: StepContext): StepResult { ... }

// Registered second (will never match if step is "create user John")
@Step("create user {string} with email {string}")
fun createUserWithEmail(name: String, email: String, ctx: StepContext): StepResult { ... }

// Solution: Register more specific patterns first
```

---

## Error Handling

### Type Conversion Errors

```kotlin
@Step("set value to {int}")
fun setValue(value: Int, context: StepContext): StepResult
```

If step text is `"set value to abc"`, library automatically returns:
```kotlin
StepResult.error(NumberFormatException("Cannot parse 'abc' as Int"))
```

### Runtime Errors

```kotlin
@Step("divide {int} by {int}")
fun divide(a: Int, b: Int, context: StepContext): StepResult {
    return try {
        val result = a / b
        context.scenarioContext.variables["result"] = result
        StepResult.success()
    } catch (e: ArithmeticException) {
        StepResult.error(e)
        // OR handle gracefully:
        // StepResult.failure(AssertionFailure("Cannot divide by zero"))
    }
}
```

---

## Best Practices

### 1. Descriptive Patterns

```kotlin
// ✅ Good: Clear, reads like English
@Step("user with username {string} is created")

// ❌ Bad: Cryptic, hard to read
@Step("usr {string} crt")
```

### 2. Consistent Naming

```kotlin
// ✅ Good: Consistent verb tense
@Step("create user {string}")
@Step("create order {int}")
@Step("create payment {string}")

// ❌ Bad: Inconsistent
@Step("create user {string}")
@Step("order {int} is created")
@Step("creating payment {string}")
```

### 3. Avoid Side Effects

```kotlin
// ✅ Good: Store state in scenario variables
@Step("create user {string}")
fun createUser(name: String, context: StepContext): StepResult {
    val user = User(name)
    context.scenarioContext.variables["user"] = user
    return StepResult.success()
}

// ❌ Bad: Store in instance fields (not scenario-scoped)
private var user: User? = null

@Step("create user {string}")
fun createUser(name: String, context: StepContext): StepResult {
    this.user = User(name)  // Shared across scenarios!
    return StepResult.success()
}
```

### 4. Use StepContext for HTTP Details

```kotlin
@Step("response contains header {string}")
fun assertHeader(headerName: String, context: StepContext): StepResult {
    val response = context.response
        ?: return StepResult.error(IllegalStateException("No response available"))
    
    val headerValue = response.headers[headerName]
    return if (headerValue != null) {
        StepResult.success()
    } else {
        StepResult.failure(
            AssertionFailure(
                message = "Header not found",
                expected = "Header '$headerName' to exist",
                actual = "Headers: ${response.headers.keys}",
                responseSnapshot = response
            )
        )
    }
}
```

---

## Spring Integration

When using Spring Boot integration, custom steps can be Spring components:

```kotlin
@Component
class DatabaseSteps(
    private val userRepository: UserRepository
) {
    
    @Step("database contains user {string}")
    fun assertUserExists(username: String, context: StepContext): StepResult {
        val exists = userRepository.existsByUsername(username)
        return if (exists) {
            StepResult.success()
        } else {
            StepResult.failure(
                AssertionFailure(
                    message = "User not found in database",
                    expected = "User '$username'",
                    actual = "Not found"
                )
            )
        }
    }
}
```

Library automatically discovers `@Step` methods in Spring components.

---

## Migration from Built-in Steps

If you want to override a built-in step:

```kotlin
// Built-in: assert status {int}
// Override with custom behavior:
@Step("assert status {int}")
fun customStatusAssertion(expected: Int, context: StepContext): StepResult {
    // Custom logic
    // First-registered wins, so register this before built-in steps
}
```

---

## Versioning & Compatibility

**API Stability**: Custom Step API is **stable** after 1.0.0 release.

**Breaking Changes Policy**:
- Adding new placeholders: Non-breaking
- Adding new methods to StepBuilder: Non-breaking
- Changing parameter extraction logic: Breaking (major version bump)
- Removing placeholder support: Breaking (major version bump)

---

## Examples Repository

Full working examples available at:
- `samples/custom-steps/annotation-based/`
- `samples/custom-steps/registration-api/`
- `samples/custom-steps/kotlin-dsl/`
