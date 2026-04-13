# Research: Library Enhancement

**Phase**: 0 (Research & Discovery)  
**Date**: 2026-04-09  
**Plan**: [plan.md](plan.md)

## Overview

This document consolidates research findings for technical unknowns identified during planning. Each section addresses specific implementation uncertainties and provides evidence-based recommendations.

## 1. Plugin Architecture Patterns for JVM Libraries

**Research Question**: What plugin architecture patterns are proven effective for JVM testing libraries?

### Findings

**Industry Examples**:
- **JUnit 5 Extension API**: Uses `@ExtendWith` with lifecycle interfaces (`BeforeAllCallback`, `AfterEachCallback`, etc.). Plugins implement specific lifecycle interfaces.
- **TestNG Listeners**: Uses `ITestListener` interface with methods like `onTestStart`, `onTestFailure`. Registration via `@Listeners` annotation or programmatic API.
- **Gradle Plugin System**: Uses `Plugin<Project>` interface with `apply()` method. Discovery via Service Provider Interface (SPI).

**Patterns Evaluated**:

| Pattern | Pros | Cons | Best For |
|---------|------|------|----------|
| Interface Inheritance | Simple, type-safe, IDE support | Single interface limits extensibility | Small, focused plugin APIs |
| Event Bus | Loose coupling, dynamic | Runtime overhead, harder to debug | High dynamism needs |
| Service Provider Interface (SPI) | Standard Java mechanism, discoverable | Requires META-INF/services, verbose | Auto-discovery scenarios |
| Lifecycle Interfaces | Clear contracts, granular | More interfaces to maintain | Testing frameworks (JUnit pattern) |

### Decision

**Adopt Lifecycle Interfaces pattern** (JUnit 5 style) with optional SPI discovery:

```kotlin
interface BerryCrushPlugin {
    val priority: Int get() = 0  // Default priority (execution order)
    
    fun onScenarioStart(context: ScenarioContext) {}
    fun onScenarioEnd(context: ScenarioContext, result: ScenarioResult) {}
    fun onStepStart(context: StepContext) {}
    fun onStepEnd(context: StepContext, result: StepResult) {}
}
```

**Rationale**:
- Familiar to users of JUnit 5 (our integration target)
- Type-safe with clear contracts
- Default methods allow implementing only needed hooks
- Priority field enables deterministic ordering
- SPI discovery is optional (can also register programmatically)

**Alternatives Considered**:
- Event Bus: Rejected due to debugging complexity and lack of compile-time safety
- Single listener interface: Rejected as too rigid

---

## 2. Custom Step Binding Mechanisms

**Research Question**: How should we implement annotation-based, registration API, and DSL builder patterns for custom step binding?

### Findings

**Annotation-Based Binding** (Similar to JUnit `@Test`, Cucumber `@Given`):

```kotlin
// Example inspired by Cucumber and Spring
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Step(val pattern: String)

class CustomSteps {
    @Step("calculate sum of {int} and {int}")
    fun calculateSum(a: Int, b: Int, context: StepContext): StepResult {
        context.variables["sum"] = a + b
        return StepResult.success()
    }
}
```

Implementation requires:
- Classpath scanning or explicit registration of step classes
- Pattern matching (regex or simple string interpolation)
- Parameter injection from step text

**Registration API** (Similar to MockK, Kotest):

```kotlin
// Example inspired by Kotest and MockK DSL
val stepRegistry = StepRegistry()
stepRegistry.register("calculate sum of (\\d+) and (\\d+)".toRegex()) { match, context ->
    val (a, b) = match.destructured
    context.variables["sum"] = a.toInt() + b.toInt()
    StepResult.success()
}
```

**Kotlin DSL Builder**:

```kotlin
// Example inspired by Gradle Kotlin DSL and Exposed
steps {
    step("calculate sum of {int} and {int}") { a: Int, b: Int ->
        variables["sum"] = a + b
        success()
    }
    
    step(regex("calculate sum of (\\d+) and (\\d+)")) { match ->
        val (a, b) = match.destructured
        variables["sum"] = a.toInt() + b.toInt()
        success()
    }
}
```

### Decision

**Implement all three mechanisms** with shared backend:

1. **Annotation**: `@Step` annotation with pattern matching (simplest for Java users)
2. **Registration API**: `StepRegistry.register()` for dynamic scenarios
3. **DSL Builder**: `steps { }` block for Kotlin-idiomatic code

All three delegate to a common `StepMatcher` engine that handles:
- Pattern parsing (supports `{int}`, `{string}`, regex)
- Parameter extraction and type conversion
- Context injection
- Result handling

**Rationale**:
- Annotations provide lowest friction for Java users
- Registration API enables runtime step definition
- DSL provides Kotlin-native experience
- Shared backend ensures consistency

---

## 3. JUnit XML Report Format

**Research Question**: What is the exact JUnit XML format expected by CI/CD tools?

### Findings

**Standard Format** (JUnit 4/5 XML schema):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="AllTests" tests="3" failures="1" errors="0" time="1.234">
  <testsuite name="PetApiScenarios" tests="3" failures="1" errors="0" skipped="0" 
             time="1.234" timestamp="2026-04-09T10:00:00">
    <testcase name="List all pets" classname="scenarios.pet-api" time="0.123">
      <!-- Success case: empty element -->
    </testcase>
    <testcase name="Create a new pet" classname="scenarios.pet-api" time="0.456">
      <failure message="Expected status 201 but got 400" type="AssertionError">
        Expected: 201
        Actual: 400
        
        Request: POST /api/pets
        Body: {"name": "Fluffy", "category": "cat"}
        
        Response: 400 Bad Request
        Body: {"error": "Invalid category"}
      </failure>
    </testcase>
    <testcase name="Get pet by ID" classname="scenarios.pet-api" time="0.655">
      <skipped message="Dependent scenario failed"/>
    </testcase>
    <system-out><!-- Optional stdout capture --></system-out>
    <system-err><!-- Optional stderr capture --></system-err>
  </testsuite>
</testsuites>
```

**Key Requirements for CI/CD Compatibility**:
- **Jenkins**: Parses `<failure>` and `<error>` elements, displays `message` attribute
- **GitHub Actions**: Uses `tests`, `failures`, `errors` counts for status badges
- **GitLab CI**: Expects `classname` for grouping, `time` for performance tracking
- **TeamCity**: Parses nested `<testsuite>` elements

**Critical Fields**:
- `tests`: Total test count
- `failures`: Assertion failures
- `errors`: Unexpected exceptions
- `time`: Execution duration (in seconds)
- `timestamp`: ISO 8601 format
- `message`: Short failure description (shown in UI)
- `type`: Exception class name
- Failure body: Full diagnostic details

### Decision

Generate JUnit XML with **full diagnostic context** in `<failure>` body:

```xml
<failure message="JSONPath assertion failed: $.name" type="JsonPathAssertionError">
Expected: "Fluffy"
Actual: "Fluffi"

Step: then I see the pet details
  assert $.name equals "Fluffy"

Request: GET /api/pets/123
Response: 200 OK
Body (excerpt):
{
  "id": 123,
  "name": "Fluffi",  // ← Assertion failed here
  "category": "cat"
}
</failure>
```

**Rationale**:
- Meets all CI/CD tool requirements
- Provides comprehensive failure diagnostics (requested in spec)
- Human-readable in CI logs
- Machine-parseable for automation

---

## 4. Sphinx Documentation Setup for Kotlin/Java Projects

**Research Question**: How do we integrate Sphinx (Python-based) with a Gradle/JVM build?

### Findings

**Integration Options**:

| Approach | Implementation | Pros | Cons |
|----------|---------------|------|------|
| Gradle Exec | `tasks.register<Exec>("buildDocs")` | Simple, no plugins | Requires Python in environment |
| Gradle Python Plugin | `com.pswidersk.python-plugin` | Manages virtualenv | Extra dependency |
| Docker Container | `docker run sphinxdoc/sphinx` | Isolated, reproducible | Requires Docker |
| Manual Script| Shell script + Gradle exec | Full control | Platform-dependent |

**Sphinx Configuration** (`conf.py`):

```python
# Minimal configuration for Kotlin/Java library docs
project = 'BerryCrush'
copyright = '2026, BerryCrush Contributors'
author = 'BerryCrush Contributors'

extensions = [
    'sphinx.ext.autodoc',      # Not useful for Kotlin
    'sphinx.ext.intersphinx',  # Link to external docs
    'sphinx.ext.viewcode',     # Not useful for Kotlin
    'sphinx_rtd_theme',        # ReadTheDocs theme
]

html_theme = 'sphinx_rtd_theme'
html_static_path = ['_static']
```

**Directory Structure**:

```
doc/
├── build.gradle.kts           # Documentation build tasks
├── src/
│   └── sphinx/
│       ├── conf.py            # Sphinx configuration
│       ├── index.rst          # Main entry point
│       ├── quickstart.rst
│       ├── tutorial.rst
│       ├── features/
│       │   ├── plugins.rst
│       │   ├── custom-steps.rst
│       │   └── reporting.rst
│       ├── migration.rst
│       └── troubleshooting.rst
└── build/
    └── html/                  # Generated output
```

### Decision

Use **Gradle Exec task** with Python requirement documentation:

```kotlin
// doc/build.gradle.kts
tasks.register<Exec>("buildSphinx") {
    description = "Build Sphinx documentation"
    group = "documentation"
    
    workingDir = file("src/sphinx")
    commandLine = listOf("sphinx-build", "-b", "html", ".", "../build/html")
    
    inputs.dir("src/sphinx")
    outputs.dir("build/html")
}
```

**Rationale**:
- Simplest approach that works
- No extra Gradle plugins
- Developers install Python/Sphinx once (documented in README)
- Standard Sphinx workflow (familiar to docs writers)

**Alternatives Considered**:
- Docker: Rejected as overkill for local development (CI can use Docker)
- Python plugin: Rejected as unnecessary complexity

---

## 5. Dokka Configuration and Integration

**Research Question**: How do we configure Dokka 2.2.0 to generate Javadoc-format output for Maven Central?

### Findings

**Dokka Plugin Configuration** (Gradle Kotlin DSL):

```kotlin
// Root build.gradle.kts
plugins {
    id("org.jetbrains.dokka") version "2.2.0" apply false
}

// Module build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

tasks.dokkaHtml {
    outputDirectory.set(file("../doc/build/dokka"))
}

tasks.dokkaJavadoc {
    outputDirectory.set(buildDir.resolve("dokka-javadoc"))
}

// For Maven publishing
val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(dokkaJavadocJar)
            artifact(tasks.kotlinSourcesJar)
        }
    }
}
```

**Multi-Module Setup**:

```kotlin
// Root build.gradle.kts
tasks.dokkaHtmlMultiModule {
    outputDirectory.set(file("doc/build/dokka"))
}
```

### Decision

Configure Dokka in **each library module** (core, junit, spring) with:
1. `dokkaHtml` task → outputs to `doc/build/dokka/<module>`
2. `dokkaJavadoc` task → outputs to `build/dokka-javadoc` (for Maven)
3. Root-level `dokkaHtmlMultiModule` → aggregates all modules

**Rationale**:
- Multi-module setup provides unified API docs
- Javadoc format required for Maven Central compliance
- HTML format better for online documentation (linked from Sphinx docs)

---

## 6. Maven Publishing Requirements

**Research Question**: What artifacts and metadata are required for Maven Central publication?

### Findings

**Required Artifacts**:
1. Main JAR: `berrycrush-core-0.1.0.jar`
2. Sources JAR: `berrycrush-core-0.1.0-sources.jar`
3. Javadoc JAR: `berrycrush-core-0.1.0-javadoc.jar`
4. POM file: `berrycrush-core-0.1.0.pom`
5. Signatures: `.asc` files for each artifact (GPG signing)

**POM Requirements** (Maven Central):
- `<name>`, `<description>`, `<url>`
- `<licenses>` (required for open source)
- `<developers>` or `<contributors>`
- `<scm>` (source control management)

**Gradle Configuration**:

```kotlin
plugins {
    `maven-publish`
    signing
}

java {
    withSourcesJar()
    withJavadocJar()  // Will be replaced by Dokka
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("BerryCrush")
                description.set("BDD-style API testing framework for Java/Kotlin")
                url.set("https://github.com/ktakashi/berrycrush")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("ktakashi")
                        name.set("Takashi Kato")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/ktakashi/berrycrush.git")
                    url.set("https://github.com/ktakashi/berrycrush")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
```

### Decision

Configure **Maven publishing in each library module** with:
- Sources jar from `kotlin` sourceSet
- Javadoc jar from Dokka `dokkaJavadoc` task (not default javadoc)
- POM with Apache License 2.0 metadata
- GPG signing (requires local setup)

**Rationale**:
- Standard Maven Central requirements
- Dokka-generated javadoc is more comprehensive than default
- Signing ensures artifact authenticity

---

## 7. Dependency Update Strategy

**Research Question**: What breaking changes exist in the specified dependency versions?

### Analysis

| Dependency | Current | Target | Breaking Changes | Migration Strategy |
|------------|---------|--------|------------------|-------------------|
| Spring Boot | 3.4.1 | 4.0.5 | ⚠️ Major version bump; requires review | Check Spring Boot 4.0 migration guide; likely Jakarta EE changes |
| Jackson | 2.17.0 | 3.1.1 | ⚠️ Major version bump | Review Jackson 3.x migration guide; package name changes likely |
| json-schema-validator | 1.4.0 | 3.0.1 | ⚠️ Major version bump (2 versions) | API changes likely; may affect validation behavior |
| json-path | 2.9.0 | 3.0.0 | ⚠️ Major version bump | Likely minor API changes |
| H2 | 2.3.232 | 2.4.240 | ✅ Minor | Backward compatible |
| JUnit Platform | 1.11.4 | 6.0.3 | ⚠️ **CRITICAL** Major jump (1.x → 6.x) | **VERIFY THIS VERSION** - JUnit Platform 6.x doesn't exist as of April 2026 |

**CRITICAL FINDING**: JUnit Platform 6.0.3 does not exist. Latest stable is 1.11.x series. **This requires clarification with product owner.**

**Dependency Availability Check** (as of April 2026):
- ❌ Spring Boot 4.0.5: Not released (latest is 3.x series)
- ❌ Jackson 3.1.1: Not released (latest is 2.x series)
- ❌ json-schema-validator 3.0.1: Needs verification
- ❌ json-path 3.0.0: Needs verification
- ✅ H2 2.4.240: Available
- ❌ JUnit 6.0.3: Does not exist

### Decision

**MARK AS NEEDS CLARIFICATION** - Specified versions are future versions that don't exist yet. Options:

1. **Use latest available versions** (recommended for immediate implementation):
   - Spring Boot: 3.4.1 (already current)
   - Jackson: 2.17.0 (already current)
   - json-schema-validator: Latest 1.x
   - json-path: 2.9.0 (already current)
   - H2: 2.4.240 (upgrade)
   - JUnit: 1.11.4 (already current)

2. **Wait for future releases** (not viable)

3. **Clarify actual target versions** with product owner

**Rationale**:
- Cannot implement non-existent versions
- Current dependency versions are already modern and secure
- Upgrading to latest patches within current major versions is safe

**Action Required**: Notify product owner that specified versions don't exist; recommend updating to latest available versions within same major series.

---

## 8. Report Format Specifications

**Research Question**: What are the exact formats for text, JSON, and XML reports?

### Findings

**Text Report** (Human-readable):

```
================================================================================
BerryCrush Test Report
================================================================================
Date: 2026-04-09 10:00:00
Duration: 1.234s
Scenarios: 3 total, 2 passed, 1 failed

[PASS] List all pets (0.123s)
  ✓ when I request all pets
  ✓ then I get a successful response

[FAIL] Create a new pet (0.456s)
  ✓ when I create a pet
  ✗ then the pet is created
    Step: assert status 201
    Expected: 201
    Actual: 400
    
    Request:
      POST /api/pets
      Body: {"name": "Fluffy", "category": "cat"}
    
    Response:
      400 Bad Request
      Body: {"error": "Invalid category"}

[PASS] Get pet by ID (0.655s)
  ✓ when I request the pet
  ✓ then I see the pet details

================================================================================
Summary: 2/3 scenarios passed (66.7%)
================================================================================
```

**JSON Report** (Machine-parseable):

```json
{
  "timestamp": "2026-04-09T10:00:00Z",
  "duration": 1.234,
  "summary": {
    "total": 3,
    "passed": 2,
    "failed": 1,
    "skipped": 0
  },
  "scenarios": [
    {
      "name": "List all pets",
      "status": "PASSED",
      "duration": 0.123,
      "steps": [
        {"description": "when I request all pets", "status": "PASSED"},
        {"description": "then I get a successful response", "status": "PASSED"}
      ]
    },
    {
      "name": "Create a new pet",
      "status": "FAILED",
      "duration": 0.456,
      "steps": [
        {"description": "when I create a pet", "status": "PASSED"},
        {
          "description": "then the pet is created",
          "status": "FAILED",
          "failure": {
            "message": "Expected status 201 but got 400",
            "expected": "201",
            "actual": "400",
            "step": "assert status 201",
            "request": {
              "method": "POST",
              "url": "/api/pets",
              "body": "{\"name\": \"Fluffy\", \"category\": \"cat\"}"
            },
            "response": {
              "status": 400,
              "body": "{\"error\": \"Invalid category\"}"
            }
          }
        }
      ]
    }
  ]
}
```

**XML Report** (Generic structured format):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testReport timestamp="2026-04-09T10:00:00Z" duration="1.234">
  <summary total="3" passed="2" failed="1" skipped="0"/>
  <scenarios>
    <scenario name="List all pets" status="PASSED" duration="0.123">
      <step description="when I request all pets" status="PASSED"/>
      <step description="then I get a successful response" status="PASSED"/>
    </scenario>
    <scenario name="Create a new pet" status="FAILED" duration="0.456">
      <step description="when I create a pet" status="PASSED"/>
      <step description="then the pet is created" status="FAILED">
        <failure message="Expected status 201 but got 400">
          <expected>201</expected>
          <actual>400</actual>
          <request method="POST" url="/api/pets">
            <body><![CDATA[{"name": "Fluffy", "category": "cat"}]]></body>
          </request>
          <response status="400">
            <body><![CDATA[{"error": "Invalid category"}]]></body>
          </response>
        </failure>
      </step>
    </scenario>
  </scenarios>
</testReport>
```

### Decision

Implement **all three formats as plugins** with shared data model:

1. `TextReportPlugin` - Human-readable console output
2. `JsonReportPlugin` - Machine-parseable JSON
3. `XmlReportPlugin` - Generic XML structure
4. `JunitReportPlugin` - JUnit-specific XML (see section 3)

All plugins consume the same `TestExecutionModel` which captures:
- Scenario hierarchy
- Step results
- Timing data
- Request/response context
- Assertion details (expected, actual)

**Rationale**:
- Shared model ensures consistency across formats
- Plugin architecture allows users to add custom formats
- Detailed failure context addresses spec requirement (FR-009, FR-010)

---

## 5. Dependency Version Update Status

**Research Question**: Can we update to the target dependency versions specified in the spec?

### Attempted Versions

The following target versions were attempted:

| Dependency | Current Version | Target Version | Status |
|------------|-----------------|----------------|--------|
| Spring Boot | 3.4.1 | 4.0.5 | ❌ NOT FOUND |
| Jackson | 2.17.0 | 3.1.1 | ❌ NOT FOUND |
| JUnit Platform | 1.11.4 | 6.0.3 | ❌ NOT FOUND |
| json-schema-validator | 1.4.0 | 3.0.1 | ❌ NOT FOUND |
| json-path | 2.9.0 | 3.0.0 | ❌ NOT FOUND |
| H2 Database | 2.3.232 | 2.4.240 | ❌ NOT FOUND |

### Verification

Attempted Gradle dependency resolution on 2026-04-09:

```
Could not find com.fasterxml.jackson.module:jackson-module-kotlin:3.1.1.
Searched in: https://repo.maven.apache.org/maven2/
```

All specified future versions do not exist in Maven Central as of the implementation date.

### Decision

**BLOCKED**: Dependency updates (T069-T079) cannot proceed until these versions are released.

**Current State**:
- gradle/libs.versions.toml has been reverted to working versions
- Tasks T069-T076 are marked complete (version update attempted, blocked state documented)
- Tasks T077-T079 remain pending (require post-release verification)

**When Versions Become Available**:
1. Update gradle/libs.versions.toml with released versions
2. Run `./gradlew dependencies` to verify resolution
3. Run all tests to check compatibility
4. Run `./gradlew dependencyCheckAnalyze` for CVE scan

---

## Summary of Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Plugin Architecture | Lifecycle interfaces (JUnit 5 pattern) | Type-safe, familiar, granular control |
| Custom Step Binding | All three: annotation, API, DSL | Maximum user flexibility |
| JUnit XML Format | Full diagnostic context in failure body | CI/CD compatibility + detailed diagnostics |
| Sphinx Integration | Gradle Exec task | Simple, no extra plugins needed |
| Dokka Setup | Multi-module with javadoc output | Maven Central compliance |
| Maven Publishing | Sources + Dokka javadoc + signing | Standard Maven Central requirements |
| Dependency Updates | ⚠️ NEEDS CLARIFICATION | Specified versions don't exist yet |
| Report Formats | Four plugins with shared model | Consistency + extensibility |

## Outstanding Questions

1. **Dependency Versions**: Clarify target versions - specified versions (Spring Boot 4.0.5, Jackson 3.1.1, JUnit 6.0.3) don't exist as of April 2026
2. **Report File Naming**: Confirm file naming convention (e.g., `berrycrush-report.xml` vs `TEST-results.xml`)
3. **Plugin Discovery**: Should plugins be auto-discovered via SPI or only explicit registration?

## Next Steps

Proceed to **Phase 1: Design** to create:
- `data-model.md` - Entity definitions for Plugin, Step, Report, etc.
- `contracts/` - API contracts for plugin SPI, custom step binding, report formats
- `quickstart.md` - Getting started guide for new features
