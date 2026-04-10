# Development Tools

This document describes the tools and workflows used for developing LemonCheck.

## Build System

### Gradle

LemonCheck uses Gradle with Kotlin DSL for build automation.

**Gradle Version:** 8.x (via wrapper)

**Running Commands:**
```bash
# Unix/macOS
./gradlew <task>

# Windows
gradlew.bat <task>
```

### Common Tasks

| Task | Description |
|------|-------------|
| `build` | Compile and test all modules |
| `test` | Run all tests |
| `clean` | Delete build outputs |
| `check` | Run all verification tasks |
| `assemble` | Build without testing |

### Module-Specific Tasks

```bash
# Build specific module
./gradlew :lemon-check:core:build
./gradlew :lemon-check:junit:build
./gradlew :lemon-check:spring:build

# Run specific module tests
./gradlew :lemon-check:core:test
./gradlew :samples:petstore:test

# Run single test class
./gradlew :lemon-check:core:test --tests "*.StepMatcherTest"
```

## Code Formatting

### ktlint

LemonCheck uses [ktlint](https://pinterest.github.io/ktlint/) for Kotlin code formatting via the [ktlint-gradle](https://github.com/JLLeitschuh/ktlint-gradle) plugin.

**Version:** 14.0.1 (as configured in `build.gradle.kts`)

#### Check Formatting

```bash
# Check all modules
./gradlew ktlintCheck

# Check specific module
./gradlew :lemon-check:core:ktlintCheck
```

#### Auto-Format Code

```bash
# Format all modules
./gradlew ktlintFormat

# Format specific module
./gradlew :lemon-check:core:ktlintFormat
```

#### ktlint Exit Codes

| Code | Description |
|------|-------------|
| 0 | Success, no issues |
| 1 | Formatting issues found |

#### Common Formatting Rules

ktlint enforces the official Kotlin coding conventions:

1. **Indentation:** 4 spaces (no tabs)
2. **Max line length:** Flexible, but aim for 120 characters
3. **Imports:** No wildcard imports, sorted alphabetically
4. **Trailing commas:** Recommended for multi-line collections
5. **Spacing:** Consistent spacing around operators and keywords

Example of well-formatted code:

```kotlin
package io.github.ktakashi.lemoncheck.example

import io.github.ktakashi.lemoncheck.model.Scenario
import io.github.ktakashi.lemoncheck.model.Step

class MyClass(
    private val name: String,
    private val value: Int,
) {
    fun process(items: List<String>): Map<String, Int> {
        return items.associate { item ->
            item to item.length
        }
    }
}
```

#### IDE Integration

**IntelliJ IDEA:**
1. Install the "ktlint" plugin from JetBrains Marketplace
2. Enable "Format on save" in Settings → Tools → ktlint
3. Or use Kotlin Style Guide: Settings → Editor → Code Style → Kotlin → Set from → Kotlin Style Guide

**VS Code:**
1. Install the "Kotlin Language" extension
2. Run `./gradlew ktlintFormat` before committing

## Documentation

### API Documentation (Dokka)

LemonCheck uses [Dokka](https://kotlinlang.org/docs/dokka-introduction.html) for generating API documentation.

```bash
# Generate HTML documentation
./gradlew dokkaHtml

# Output: lemon-check/doc/build/dokka/
```

### User Documentation (Sphinx)

User documentation uses [Sphinx](https://www.sphinx-doc.org/).

```bash
cd lemon-check/doc

# Install dependencies (first time)
pip install sphinx sphinx-rtd-theme

# Build HTML documentation
make html

# Output: lemon-check/doc/build/html/
```

## Dependency Management

### Version Catalog

Dependencies are managed via `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.3.20"
swagger-parser = "2.1.39"
json-path = "3.0.0"

[libraries]
swagger-parser = { module = "io.swagger.parser.v3:swagger-parser", version.ref = "swagger-parser" }
```

### Viewing Dependencies

```bash
# Show dependency tree
./gradlew dependencies

# Show dependencies for specific configuration
./gradlew :lemon-check:core:dependencies --configuration runtimeClasspath
```

### OWASP Dependency Check

Security vulnerability scanning is configured:

```bash
# Run vulnerability check
./gradlew dependencyCheckAnalyze

# Output: build/reports/dependency-check/
```

## Testing

### Running Tests

```bash
# All tests
./gradlew test

# With test output
./gradlew test --info

# Specific test
./gradlew test --tests "*.LemonCheckDslTest"

# Re-run failed tests
./gradlew test --rerun
```

### Test Reports

Test reports are generated at:
- `<module>/build/reports/tests/test/index.html`

### Test Coverage (Future)

Currently not configured. To add JaCoCo:

```kotlin
plugins {
    id("jacoco")
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
```

## IDE Setup

### IntelliJ IDEA (Recommended)

1. **Import Project**
   - File → Open → Select `lemon-check` directory
   - Import as Gradle project

2. **Configure JDK**
   - File → Project Structure → Project SDK → JDK 21

3. **Enable Annotation Processing**
   - Settings → Build → Compiler → Annotation Processors
   - Enable annotation processing

4. **Kotlin Plugin**
   - Ensure Kotlin plugin is updated to latest version

### VS Code

1. **Install Extensions**
   - Kotlin Language
   - Gradle for Java
   - EditorConfig

2. **Configure Settings**
   ```json
   {
     "kotlin.java.home": "/path/to/jdk-21",
     "editor.formatOnSave": true
   }
   ```

## Git Workflow

### Branch Naming

| Pattern | Purpose |
|---------|---------|
| `main` | Stable release code |
| `develop` | Integration branch |
| `feature/xxx-description` | Feature development |
| `fix/xxx-description` | Bug fixes |
| `docs/xxx-description` | Documentation updates |

### Commit Messages

Follow conventional commits:

```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Maintenance

Example:
```
feat(junit): add support for @LemonCheckTimeout annotation

Allow per-class and per-method timeout configuration for scenarios.

Closes #123
```

### Pre-commit Checks

Before committing, run:

```bash
# Format code
./gradlew ktlintFormat

# Run tests
./gradlew test

# Check for issues
./gradlew check
```

## Continuous Integration

### GitHub Actions (Recommended Setup)

```yaml
# .github/workflows/ci.yml
name: CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build
        run: ./gradlew build
      - name: Check formatting
        run: ./gradlew ktlintCheck
```

## Debugging

### Debug Tests in IDE

1. Set breakpoint in test code
2. Right-click test → Debug
3. Or use Gradle run configuration with `--debug-jvm`

### Debug Scenario Execution

Enable verbose logging:

```kotlin
val config = Configuration().apply {
    logRequests = true
    logResponses = true
}
```

### Debug Parser

The lexer and parser include debug output:

```kotlin
val result = Parser.parse(source, fileName)
if (!result.isSuccess) {
    result.errors.forEach { println(it) }
}
```

## Useful Commands Reference

```bash
# Build
./gradlew build              # Full build with tests
./gradlew assemble           # Build without tests
./gradlew clean build        # Clean build

# Test
./gradlew test               # Run all tests
./gradlew test --rerun       # Re-run all tests
./gradlew test --tests "*.MyTest"  # Run specific test

# Format
./gradlew ktlintCheck        # Check formatting
./gradlew ktlintFormat       # Auto-format

# Documentation
./gradlew dokkaHtml          # Generate API docs

# Dependencies
./gradlew dependencies       # Show dependency tree
./gradlew dependencyCheckAnalyze  # Security scan

# Clean
./gradlew clean              # Delete build outputs
./gradlew cleanTest          # Delete test outputs only

# Info
./gradlew tasks              # List available tasks
./gradlew tasks --all        # List all tasks
./gradlew properties         # Show project properties
```

## Troubleshooting

### Common Issues

**"Could not resolve dependencies"**
- Check internet connection
- Run `./gradlew --refresh-dependencies`

**"Kotlin version mismatch"**
- Ensure Kotlin plugin matches `gradle/libs.versions.toml`
- Sync Gradle in IDE

**"ktlint format fails"**
- Run `./gradlew ktlintFormat` to auto-fix
- Some issues may require manual fixes

**"Tests pass locally but fail in CI"**
- Check for timezone/locale dependent code
- Ensure no hardcoded paths
- Check for race conditions

### Getting Help

1. Check existing [GitHub Issues](https://github.com/ktakashi/lemon-check/issues)
2. Review the [specifications](../specs/) for expected behavior
3. Enable debug logging to trace issues
