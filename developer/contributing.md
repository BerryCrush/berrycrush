# Contributing to LemonCheck

Thank you for your interest in contributing to LemonCheck! This guide will help you get started.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/lemon-check.git
   cd lemon-check
   ```
3. **Set up the development environment:**
   ```bash
   # Verify JDK 21
   java -version
   
   # Build the project
   ./gradlew build
   ```

## Development Workflow

### 1. Create a Branch

```bash
git checkout -b feature/my-new-feature
```

### 2. Make Changes

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Write tests for new functionality
- Update documentation as needed

### 3. Format Code

```bash
./gradlew ktlintFormat
```

### 4. Run Tests

```bash
./gradlew test
```

### 5. Commit Changes

```bash
git commit -m "feat(scope): add new feature"
```

### 6. Push and Create PR

```bash
git push origin feature/my-new-feature
```

Then create a Pull Request on GitHub.

## Code Quality Standards

### Kotlin Style

- Use ktlint for formatting
- Follow official Kotlin conventions
- Prefer immutability (`val` over `var`)
- Use meaningful names
- Add KDoc comments for public APIs

### Testing

- Write unit tests for new code
- Aim for high test coverage
- Use descriptive test names
- Include edge cases

### Documentation

- Update README if adding features
- Add KDoc to public classes/methods
- Update Sphinx docs for user-facing changes

## Pull Request Guidelines

1. **One feature per PR** - Keep PRs focused
2. **Clear description** - Explain what and why
3. **Reference issues** - Link to related issues
4. **Pass CI** - All checks must pass
5. **Respond to feedback** - Address review comments

## Reporting Issues

When reporting bugs:

1. Search existing issues first
2. Include reproduction steps
3. Provide version information
4. Attach relevant logs/output

## License

By contributing, you agree that your contributions will be licensed under the project's Apache 2.0 License.
