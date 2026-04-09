# Data Model: Library Enhancement

**Phase**: 1 (Design)  
**Date**: 2026-04-09  
**Plan**: [plan.md](plan.md)

## Overview

This document defines the key entities, their attributes, relationships, and state transitions for the library enhancement feature. All entities are implementation-agnostic and focus on business logic and data structures.

---

## Core Entities

### Plugin

Represents an extension point that receives lifecycle events during scenario execution.

**Attributes**:
- `id`: Unique identifier (String)
- `priority`: Execution order (Int, default 0, lower executes first)
- `enabled`: Whether the plugin is active (Boolean, default true)
- `name`: Human-readable plugin name (String)

**Behaviors**:
- `onScenarioStart(context: ScenarioContext)`: Invoked before scenario begins
- `onScenarioEnd(context: ScenarioContext, result: ScenarioResult)`: Invoked after scenario completes
- `onStepStart(context: StepContext)`: Invoked before step begins
- `onStepEnd(context: StepContext, result: StepResult)`: Invoked after step completes

**Relationships**:
- Plugin **receives** LifecycleEvent (1:N)
- Plugin **generates** ReportArtifact (0:N) - for ReportPlugin subtypes

**Invariants**:
- Priority must be finite integer
- Lifecycle methods must not throw exceptions (built-in plugins)
- Plugins with same priority execute in registration order

---

### LifecycleEvent

Represents a moment in the test execution lifecycle with associated context.

**Attributes**:
- `type`: Event type enum (SCENARIO_START, SCENARIO_END, STEP_START, STEP_END)
- `timestamp`: When event occurred (Instant)
- `context`: Associated context object (ScenarioContext or StepContext)
- `result`: Execution result (ScenarioResult or StepResult) - null for START events

**Relationships**:
- LifecycleEvent **triggered by** TestExecution (1:1)
- LifecycleEvent **delivered to** Plugin (1:N)

**State Transitions**:
```
Created → Dispatched → Processed
```

**Invariants**:
- START events have null result
- END events have non-null result
- Timestamp must not be in the future

---

### ScenarioContext

Execution context for a scenario, providing access to scenario metadata and runtime state.

**Attributes**:
- `scenarioName`: Name from scenario file (String)
- `scenarioFile`: Path to source file (Path)
- `variables`: Mutable map of extracted variables (Map<String, Any>)
- `metadata`: Scenario-level metadata (Map<String, String>)
- `startTime`: When scenario began (Instant)
- `tags`: Scenario tags for filtering (Set<String>)

**Relationships**:
- ScenarioContext **contains** StepContext (1:N)
- ScenarioContext **part of** LifecycleEvent (1:1)

**Invariants**:
- Scenario name must not be empty
- Variables are scoped to scenario (isolated between scenarios)

---

### StepContext

Execution context for a single step within a scenario.

**Attributes**:
- `stepDescription`: Full step text (String)
- `stepType`: Type enum (CALL, ASSERT, EXTRACT, CUSTOM)
- `operationId`: OpenAPI operation ID (String, nullable)
- `request`: HTTP request details (HttpRequest, nullable)
- `response`: HTTP response details (HttpResponse, nullable)
- `scenarioContext`: Parent scenario context (ScenarioContext)
- `stepIndex`: Position within scenario (Int, 0-based)

**Relationships**:
- StepContext **belongs to** ScenarioContext (N:1)
- StepContext **part of** LifecycleEvent (1:1)

**Invariants**:
- Step index must be >= 0
- Request/response only populated for CALL steps

---

### ScenarioResult

Outcome of scenario execution with timing and status information.

**Attributes**:
- `status`: Result status enum (PASSED, FAILED, SKIPPED, ERROR)
- `duration`: Execution time (Duration)
- `failedStep`: Index of first failed step (Int, -1 if none)
- `error`: Exception if ERROR status (Throwable, nullable)
- `stepResults`: Results for each step (List<StepResult>)

**Relationships**:
- ScenarioResult **aggregates** StepResult (1:N)
- ScenarioResult **part of** LifecycleEvent (N:1)

**State Transitions**:
```
RUNNING → PASSED | FAILED | SKIPPED | ERROR
```

**Invariants**:
- FAILED status requires failedStep >= 0
- ERROR status requires non-null error
- Duration must be positive
- stepResults.size must equal number of executed steps

---

### StepResult

Outcome of individual step execution.

**Attributes**:
- `status`: Result status enum (PASSED, FAILED, SKIPPED, ERROR)
- `duration`: Execution time (Duration)
- `failure`: Failure details (AssertionFailure, nullable)
- `error`: Exception if ERROR status (Throwable, nullable)

**Relationships**:
- StepResult **part of** ScenarioResult (N:1)
- StepResult **part of** LifecycleEvent (N:1)
- StepResult **contains** AssertionFailure (0:1)

**Invariants**:
- FAILED status requires non-null failure
- ERROR status requires non-null error
- Duration must be positive

---

### AssertionFailure

Detailed information about why an assertion failed.

**Attributes**:
- `message`: Human-readable failure description (String)
- `expected`: Expected value (Any, nullable)
- `actual`: Actual value (Any, nullable)
- `diff`: Computed difference (String, nullable)
- `stepDescription`: Which step failed (String)
- `assertionType`: Type of assertion (String, e.g., "status", "jsonpath")
- `requestSnapshot`: HTTP request at failure time (HttpRequest, nullable)
- `responseSnapshot`: HTTP response at failure time (HttpResponse, nullable)

**Relationships**:
- AssertionFailure **part of** StepResult (1:1)

**Invariants**:
- Message must not be empty
- For comparison assertions, expected and actual must both be non-null

---

### CustomStep

Represents a user-defined step implementation bound to a pattern.

**Attributes**:
- `pattern`: Step pattern or regex (String or Regex)
- `executor`: Function that executes the step ((StepContext) → StepResult)
- `parameterExtractor`: Extracts parameters from step text (Function)
- `bindingType`: How step was registered (ANNOTATION, API, DSL)
- `sourceLocation`: Where step was defined (String, for debugging)

**Relationships**:
- CustomStep **matched by** StepMatcher (N:1)
- CustomStep **executes in** StepContext (N:1)

**Invariants**:
- Pattern must not be empty
- Executor must be non-null
- Pattern must be deterministic (same input → same match)

---

### StepMatcher

Engine that matches step descriptions to CustomStep implementations.

**Attributes**:
- `steps`: Registered custom steps (List<CustomStep>)
- `patternCache`: Compiled patterns for performance (Map<String, CompiledPattern>)

**Behaviors**:
- `register(step: CustomStep)`: Add a custom step
- `findMatch(stepText: String): Match?`: Find matching custom step
- `extractParameters(match: Match): Map<String, Any>`: Extract parameter values

**Relationships**:
- StepMatcher **manages** CustomStep (1:N)

**Invariants**:
- Steps registered first take precedence (first-match wins)
- Patterns must be unambiguous (no overlapping matches)

---

## Reporting Entities

### TestReport

Complete test execution report containing all scenarios and their results.

**Attributes**:
- `timestamp`: When tests ran (Instant)
- `duration`: Total execution time (Duration)
- `summary`: Aggregate statistics (TestSummary)
- `scenarios`: All scenario results (List<ScenarioReportEntry>)
- `environment`: Test environment metadata (Map<String, String>)

**Relationships**:
- TestReport **contains** ScenarioReportEntry (1:N)
- TestReport **generated by** ReportPlugin (N:1)

**Invariants**:
- Summary statistics must match scenario list
- Duration must equal sum of scenario durations

---

### ScenarioReportEntry

Report entry for a single scenario with full execution details.

**Attributes**:
- `name`: Scenario name (String)
- `status`: Overall status (ResultStatus)
- `duration`: Execution time (Duration)
- `steps`: Step execution details (List<StepReportEntry>)
- `tags`: Scenario tags (Set<String>)
- `metadata`: Additional metadata (Map<String, String>)

**Relationships**:
- ScenarioReportEntry **contains** StepReportEntry (1:N)
- ScenarioReportEntry **part of** TestReport (N:1)

---

### StepReportEntry

Report entry for a single step with request/response/assertion details.

**Attributes**:
- `description`: Step description (String)
- `status`: Step status (ResultStatus)
- `duration`: Execution time (Duration)
- `request`: HTTP request (HttpRequest, nullable)
- `response`: HTTP response (HttpResponse, nullable)
- `failure`: Failure details if failed (AssertionFailure, nullable)

**Relationships**:
- StepReportEntry **part of** ScenarioReportEntry (N:1)

---

### ReportPlugin

Specialized Plugin that generates test reports in a specific format.

**Attributes**:
- (Inherits all Plugin attributes)
- `formatName`: Report format identifier (String, e.g., "junit", "json")
- `outputPath`: Where to write report (Path)
- `options`: Format-specific options (Map<String, Any>)

**Behaviors**:
- `generateReport(report: TestReport): ReportArtifact`: Generate report file

**Relationships**:
- ReportPlugin **is a** Plugin (inheritance)
- ReportPlugin **generates** ReportArtifact (1:N)

**Invariants**:
- Format name must be unique
- Output path must be writable
- Must handle I/O errors gracefully (never throw exceptions)

---

### ReportArtifact

Generated report file or output.

**Attributes**:
- `format`: Report format (String)
- `path`: File path (Path)
- `content`: Report content (String or ByteArray)
- `generatedAt`: When created (Instant)
- `sizeBytes`: Content size (Long)

**Relationships**:
- ReportArtifact **generated by** ReportPlugin (N:1)

---

## Configuration Entities

### ReportConfiguration

User-specified configuration for which reports to generate.

**Attributes**:
- `enabledFormats`: List of format names (List<String>)
- `outputDirectory`: Base directory for reports (Path)
- `formatOptions`: Per-format options (Map<String, Map<String, Any>>)
- `source`: Where config came from (CONFIG_FILE, ANNOTATION, DEFAULT)

**Relationships**:
- ReportConfiguration **configures** ReportPlugin (1:N)

**Invariants**:
- At least one format must be enabled
- Output directory must exist or be creatable

---

### PluginRegistry

Central registry for all plugins.

**Attributes**:
- `plugins`: Registered plugins (List<Plugin>)
- `pluginsByPriority`: Sorted view (List<Plugin>)

**Behaviors**:
- `register(plugin: Plugin)`: Add plugin to registry
- `getOrderedPlugins(): List<Plugin>`: Get plugins in execution order
- `enablePlugin(id: String)`: Enable a plugin
- `disablePlugin(id: String)`: Disable a plugin

**Relationships**:
- PluginRegistry **manages** Plugin (1:N)

**Invariants**:
- Plugins sorted by priority, then registration order
- No duplicate plugin IDs

---

## Relationships Summary Diagram

```
TestExecution
  ├── LifecycleEvent (N)
  │     ├── ScenarioContext
  │     │     └── StepContext (N)
  │     │           ├── HttpRequest
  │     │           └── HttpResponse
  │     └── Result (ScenarioResult | StepResult)
  │           └── AssertionFailure
  │
  ├── PluginRegistry
  │     └── Plugin (N)
  │           └── ReportPlugin
  │                 └── ReportArtifact (N)
  │
  ├── StepMatcher
  │     └── CustomStep (N)
  │
  └── TestReport
        └── ScenarioReportEntry (N)
              └── StepReportEntry (N)
                    └── AssertionFailure
```

---

## State Transitions

### Plugin Lifecycle

```
Registered → Initialized → Active → Disabled
                            ↓
                         Executing (per event)
```

### Test Execution

```
Configured → Running → Generating Reports → Complete
                ├── Scenario 1 (PASSED)
                ├── Scenario 2 (FAILED)
                └── Scenario 3 (PASSED)
```

### Report Generation

```
TestExecution Complete → Plugin Hooks → Report Model Built → Format Rendered → File Written
```

---

## Key Invariants

1. **Plugin Isolation**: Plugin exception must not affect other plugins or test execution (fail-fast)
2. **Priority Ordering**: Plugins execute in priority order (lower first), then registration order
3. **Context Immutability**: ScenarioContext and StepContext should be read-only in most plugin hooks
4. **Report Consistency**: All report formats must represent the same underlying TestReport model
5. **Step Matching**: First registered custom step matching a pattern wins (disambiguation)
6. **Timing Accuracy**: All durations measured via monotonic clock, not wall clock
7. **Failure Transparency**: AssertionFailure must capture complete context (request, response, expected, actual)
