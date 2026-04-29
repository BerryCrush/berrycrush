# BerryCrush Development Skill

This skill provides guidance for developing and contributing to the BerryCrush project.

## Build Verification

### Required Steps After Changes

1. **Always run full build** after implementing changes:
   ```bash
   cd berrycrush
   ./gradlew clean build
   ```

2. **Always run the `check` task** to verify code quality:
   ```bash
   ./gradlew check
   ```
   
   The `check` task includes:
   - Unit tests (`test`)
   - SAST analysis (`sastFull`)
     - Detekt (Kotlin static analysis)
     - SpotBugs (Bug detection + Find Security Bugs)
     - CPD (Duplicate code detection)
   - KtLint (Code formatting)

### Error and Warning Handling

1. **Fix all errors** - Build must pass without errors
2. **Fix all warnings** - Strive for warning-free code
3. **If a warning cannot be fixed:**
   - Document the reason
   - Ask for feedback/approval before proceeding
   - Consider adding to baseline if intentional (e.g., DSL patterns)

## SAST Tools Configuration

### Available Tasks

| Task | Description |
|------|-------------|
| `./gradlew sast` | Quick SAST (Detekt + SpotBugs) |
| `./gradlew sastFull` | Full SAST (sast + CPD) |
| `./gradlew detekt` | Kotlin static analysis only |
| `./gradlew cpdCheck` | Duplicate code detection only |
| `./gradlew spotbugsMain` | SpotBugs on main sources |
| `./gradlew check` | Full check including tests + SAST |

### Configuration Files

| File | Purpose |
|------|---------|
| `config/detekt/detekt.yml` | Detekt rules configuration |
| `config/spotbugs/exclusions.xml` | SpotBugs exclusion filter |
| `*/config/detekt/baseline.xml` | Detekt baseline for existing issues |

### SAST Failure Behavior

- **Detekt**: `ignoreFailures = true` (uses baseline)
- **SpotBugs**: `ignoreFailures = true` (until issues addressed)
- **CPD**: `ignoreFailures = false` (fails on new duplications)

## Code Quality Standards

### Detekt Rules

Key rules enforced:
- **CyclomaticComplexMethod**: Max threshold 15
- **LongMethod**: Max 60 lines
- **LongParameterList**: Max 6 parameters
- **NestedBlockDepth**: Max 4 levels
- **MagicNumber**: Extract to named constants
- **ReturnCount**: Single return preferred

### Common Issues to Avoid

1. **Magic Numbers** - Use named constants
   ```kotlin
   // Bad
   if (count > 100) { }
   
   // Good
   companion object {
       private const val MAX_COUNT = 100
   }
   if (count > MAX_COUNT) { }
   ```

2. **Long Methods** - Extract to helper functions
3. **Complex Conditions** - Extract to named predicates
4. **Duplicate Code** - Extract to shared utilities

## Gradle Conventions

### Project Structure

```
berrycrush/
├── buildSrc/             # Convention plugins
├── berrycrush/
│   ├── core/             # Core library
│   ├── junit/            # JUnit 5 integration
│   ├── spring/           # Spring Boot integration
│   └── doc/              # Documentation
├── samples/              # Sample projects
└── config/               # SAST configuration
    ├── detekt/
    └── spotbugs/
```

### Common Build Commands

```bash
# Full build with tests
./gradlew build

# Full verification
./gradlew check

# Quick compile check
./gradlew compileKotlin

# Run specific module tests
./gradlew :berrycrush:core:test

# Clean build
./gradlew clean build

# SAST only
./gradlew sastFull
```

### Dependency Management

- Use version catalog at `gradle/libs.versions.toml`
- Declare versions centrally
- Keep dependencies up-to-date

## CI/CD Integration

### GitHub Actions Workflows

- **PR builds**: Run `./gradlew check` on all pull requests
- **CI builds**: Run `./gradlew check` + publish snapshots on main

### Reports

After build, check reports at:
- Test reports: `build/reports/tests/`
- Detekt reports: `build/reports/detekt/`
- SpotBugs reports: `build/reports/spotbugs/`
- CPD reports: `build/reports/cpd/`

## Troubleshooting

### Common Issues

1. **"Class not found" errors in buildSrc**
   - Check buildSrc dependencies
   - Avoid adding plugins that conflict with root project

2. **Detekt config validation errors**
   - Check rule names in `detekt.yml`
   - Refer to Detekt documentation for valid rule names

3. **SpotBugs Kotlin issues**
   - Add exclusions for Kotlin-specific patterns
   - Check `config/spotbugs/exclusions.xml`

4. **CPD duplicate warnings in tests**
   - Test code duplication is often acceptable
   - Consider excluding test sources if appropriate

### Getting Help

1. Check existing documentation in `developer/` directory
2. Review SAST reports for specific guidance
3. Consult Detekt documentation: https://detekt.dev/docs/rules/
