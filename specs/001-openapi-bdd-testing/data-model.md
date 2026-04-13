# Data Model: OpenAPI BDD Testing Library

**Feature**: 001-openapi-bdd-testing  
**Date**: 2026-04-07  
**Status**: Complete

## Entity Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           BerryCrush Library                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐                  ┌──────────────────┐                │
│  │ ScenarioFile │─────parses──────►│   FeatureFile    │                │
│  │   (*.scenario)                  │    (AST)         │                │
│  └──────────────┘                  └────────┬─────────┘                │
│                                             │                          │
│                                             │ transforms to            │
│                                             ▼                          │
│  ┌──────────────┐    references    ┌──────────────────┐                │
│  │   OpenAPI    │◄────────────────│    Scenario      │                │
│  │    Spec      │                  │  (executable)    │                │
│  └──────┬───────┘                  └────────┬─────────┘                │
│         │                                   │                          │
│         │ contains                          │ contains                 │
│         ▼                                   ▼                          │
│  ┌──────────────┐                  ┌──────────────────┐                │
│  │  Operation   │◄────────────────│      Step        │                │
│  │              │    maps to       │                  │                │
│  └──────────────┘                  └────────┬─────────┘                │
│                                             │                          │
│                                             │ uses                     │
│                                             ▼                          │
│                                    ┌──────────────────┐                │
│  ┌──────────────┐    flows to     │ ExecutionContext │                │
│  │   Fragment   │────────────────►│                  │                │
│  └──────────────┘                  └────────┬─────────┘                │
│                                             │                          │
│                                             │ produces                 │
│                                             ▼                          │
│                                    ┌──────────────────┐                │
│                                    │   TestReport     │                │
│                                    └──────────────────┘                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Parser Entities (Scenario File Format)

### FeatureFile

Parsed representation of a `.scenario` file.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| path | Path | Source file path | Required |
| metadata | Map&lt;String, String&gt; | File-level config (@key: value) | Required |
| feature | Feature | The parsed feature | Required |
| parseErrors | List&lt;ParseError&gt; | Any parse warnings | May be empty |

---

### Feature

A feature containing multiple scenarios.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| name | String | Feature title | Required, non-empty |
| description | String? | Multi-line description | Optional |
| background | Background? | Shared setup steps | Optional |
| scenarios | List&lt;ParsedScenario&gt; | Scenarios in this feature | At least 1 |
| sourceLocation | SourceLocation | Line/column in source | Required |

---

### Background

Shared steps executed before each scenario.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| name | String | Background label | Required |
| steps | List&lt;ParsedStep&gt; | Setup steps | At least 1 |
| sourceLocation | SourceLocation | Line/column in source | Required |

---

### ParsedScenario

A scenario as parsed from text (before execution).

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| name | String | Scenario title | Required |
| tags | Set&lt;String&gt; | @tag annotations | Optional |
| steps | List&lt;ParsedStep&gt; | BDD steps | At least 1 |
| examples | List&lt;ExampleTable&gt;? | For Scenario Outline | Optional |
| isOutline | Boolean | True if Scenario Outline | Required |
| sourceLocation | SourceLocation | Line/column in source | Required |

---

### ParsedStep

A step as parsed from text with its directive table.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| keyword | StepKeyword | GIVEN, WHEN, THEN, AND, BUT | Required |
| text | String | Step description | Required |
| directives | List&lt;StepDirective&gt; | operation, assert, etc. | Optional |
| sourceLocation | SourceLocation | Line/column in source | Required |

---

### StepDirective

A single directive from a step's configuration table.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| type | DirectiveType | operation, path, body, etc. | Required |
| value | String | Directive value | Required |
| sourceLocation | SourceLocation | Line/column in source | Required |

**DirectiveType** (enum):
```kotlin
enum class DirectiveType {
    OPERATION,  // API operation to call
    PATH,       // Path parameter
    QUERY,      // Query parameter
    HEADER,     // Request header
    BODY,       // Request body
    EXTRACT,    // Value extraction
    ASSERT,     // Assertion
    INCLUDE     // Fragment inclusion
}
```

---

### ExampleTable

Data table for Scenario Outline parameterization.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| headers | List&lt;String&gt; | Column names | Required |
| rows | List&lt;ExampleRow&gt; | Data rows | At least 1 |
| sourceLocation | SourceLocation | Line/column in source | Required |

---

### SourceLocation

Position in source file for error reporting.

| Attribute | Type | Description |
|-----------|------|-------------|
| file | Path | Source file path |
| line | Int | 1-based line number |
| column | Int | 1-based column number |
| length | Int | Length of the token |

---

### ParseError

Error or warning from parsing.

| Attribute | Type | Description |
|-----------|------|-------------|
| message | String | Human-readable error message |
| location | SourceLocation | Where the error occurred |
| severity | Severity | ERROR or WARNING |
| suggestion | String? | Suggested fix |

---

### StepKeyword

Enumeration of BDD step keywords.

```kotlin
enum class StepKeyword {
    GIVEN,
    WHEN,
    THEN,
    AND,
    BUT
}
```

---

## Core Entities

### Scenario

A complete BDD test case describing a user journey against an API.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| name | String | Human-readable scenario title | Required, non-empty |
| description | String? | Optional detailed description | Max 500 chars |
| tags | Set&lt;String&gt; | Labels for filtering/grouping | Optional |
| steps | List&lt;Step&gt; | Ordered sequence of BDD steps | At least 1 step required |
| fragments | List&lt;Fragment&gt; | Included reusable fragments | Optional |
| examples | List&lt;ExampleRow&gt;? | Data table for parameterized scenarios | Optional |

**Validation Rules**:
- Name must be unique within a feature file
- Must contain at least one `When` step (action)
- `Given` steps must precede `When` steps
- `Then` steps must follow `When` steps

**State Transitions**:
```
[Draft] → [Parsed] → [Executing] → [Passed|Failed|Error]
```

---

### Step

A single instruction within a scenario, representing an API call or assertion.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| type | StepType | GIVEN, WHEN, THEN, AND | Required |
| description | String | Natural language step text | Required, non-empty |
| operationId | String? | OpenAPI operation to invoke | Required for API calls |
| pathParams | Map&lt;String, Any&gt; | Path parameter values | Keys must match spec |
| queryParams | Map&lt;String, Any&gt; | Query parameter values | Keys must match spec |
| headers | Map&lt;String, String&gt; | HTTP headers to send | Optional |
| body | String? | Request body content | Optional |
| extractions | List&lt;Extraction&gt; | Values to extract from response | Optional |
| assertions | List&lt;Assertion&gt; | Conditions to verify | Optional |

**Step Types** (enum):
```kotlin
enum class StepType {
    GIVEN,  // Precondition setup
    WHEN,   // Action performed
    THEN,   // Expected outcome
    AND     // Continuation of previous type
}
```

**Validation Rules**:
- `AND` inherits type from preceding step
- API call steps require valid `operationId`
- Path params must match operation's path parameter definitions
- Body required if operation expects requestBody

---

### Fragment

A reusable group of steps that can be included in multiple scenarios.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| name | String | Unique fragment identifier | Required, unique |
| description | String? | What this fragment accomplishes | Optional |
| steps | List&lt;Step&gt; | Steps in this fragment | At least 1 step |
| parameters | List&lt;Parameter&gt; | Configurable inputs | Optional |

**Usage Pattern**:
- Defined at file or project level
- Referenced by name in scenarios
- Executed in order of inclusion
- Shares `ExecutionContext` with including scenario

---

### ExecutionContext

Runtime state that flows through scenario execution.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| variables | MutableMap&lt;String, Any&gt; | Extracted/stored values | Keys are user-defined |
| config | Configuration | Runtime configuration | Required |
| lastResponse | HttpResponse? | Most recent API response | Set after each call |
| authToken | String? | Current auth bearer token | Optional |

**Thread Safety**: Context is not thread-safe by design—scenarios execute sequentially.

**Variable Resolution**:
```kotlin
// Reference syntax in DSL
context["variableName"]     // Direct access
"${context.variableName}"   // String interpolation
```

---

### Configuration

Settings for scenario execution.

| Attribute | Type | Description | Default |
|-----------|------|-------------|---------|
| baseUrl | String | API base URL | From OpenAPI servers[0] |
| timeout | Duration | Request timeout | 10 seconds |
| defaultHeaders | Map&lt;String, String&gt; | Headers for all requests | Empty |
| environment | String | Environment name | "default" |
| strictValidation | Boolean | Fail on extra response fields | false |
| reporter | TestReporter | Result output handler | ConsoleReporter |

---

### Extraction

Specification for extracting a value from an API response.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| variableName | String | Name to store extracted value | Required, valid identifier |
| jsonPath | String | JSONPath expression | Required, valid JSONPath |
| required | Boolean | Fail if extraction fails | Default: true |
| defaultValue | Any? | Fallback if not found | Only if required=false |

**Examples**:
```kotlin
Extraction("petId", "$.id")                    // Required
Extraction("count", "$.total", required=false, defaultValue=0)
```

---

### Assertion

A condition to verify against an API response.

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| type | AssertionType | Kind of assertion | Required |
| expected | Any? | Expected value | Depends on type |
| actual | String? | JSONPath to actual value | For body assertions |
| message | String? | Custom failure message | Optional |

**Assertion Types** (enum):
```kotlin
enum class AssertionType {
    STATUS_CODE,      // HTTP status equals expected
    HEADER_EXISTS,    // Header present
    HEADER_EQUALS,    // Header value matches
    BODY_CONTAINS,    // Body contains string
    BODY_EQUALS,      // JSONPath value equals
    BODY_MATCHES,     // JSONPath matches regex
    SCHEMA_VALID,     // Response matches OpenAPI schema
    RESPONSE_TIME     // Response within time limit
}
```

---

### StepResult

Outcome of executing a single step.

| Attribute | Type | Description |
|-----------|------|-------------|
| step | Step | The executed step |
| status | ResultStatus | PASSED, FAILED, SKIPPED, ERROR |
| duration | Duration | Execution time |
| request | HttpRequest? | Sent request (for debugging) |
| response | HttpResponse? | Received response |
| error | Throwable? | Exception if ERROR status |
| assertions | List&lt;AssertionResult&gt; | Individual assertion outcomes |

---

### ScenarioResult

Aggregate outcome of a complete scenario.

| Attribute | Type | Description |
|-----------|------|-------------|
| scenario | Scenario | The executed scenario |
| status | ResultStatus | PASSED if all steps pass |
| stepResults | List&lt;StepResult&gt; | Results for each step |
| totalDuration | Duration | Sum of step durations |
| parameters | Map&lt;String, Any&gt;? | Values if parameterized |

---

### TestReport

Summary of test execution across scenarios.

| Attribute | Type | Description |
|-----------|------|-------------|
| scenarios | List&lt;ScenarioResult&gt; | All scenario outcomes |
| passedCount | Int | Scenarios that passed |
| failedCount | Int | Scenarios that failed |
| errorCount | Int | Scenarios with errors |
| totalDuration | Duration | Overall execution time |
| generatedAt | Instant | Report generation timestamp |

---

## Supporting Types

### ExampleRow

A single row in a parameterized scenario's data table.

| Attribute | Type | Description |
|-----------|------|-------------|
| values | Map&lt;String, Any&gt; | Named parameter values |

### ValidationError

Schema validation failure detail.

| Attribute | Type | Description |
|-----------|------|-------------|
| path | String | JSONPath to invalid field |
| message | String | Validation error description |
| expected | String | What was expected |
| actual | String | What was found |

### ResultStatus

Enumeration of possible outcomes.

```kotlin
enum class ResultStatus {
    PASSED,   // All assertions met
    FAILED,   // Assertion not met
    SKIPPED,  // Step not executed (prior failure)
    ERROR     // Exception during execution
}
```

---

## Relationships Summary

| From | To | Relationship | Cardinality |
|------|-----|--------------|-------------|
| Scenario | Step | contains | 1:N |
| Scenario | Fragment | includes | 0:N |
| Scenario | ExampleRow | parameterized by | 0:N |
| Step | Extraction | has | 0:N |
| Step | Assertion | has | 0:N |
| Fragment | Step | contains | 1:N |
| ExecutionContext | Configuration | configured by | 1:1 |
| ScenarioResult | StepResult | aggregates | 1:N |
| TestReport | ScenarioResult | aggregates | 1:N |
