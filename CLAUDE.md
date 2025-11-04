# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# NON Escaping rules
- None of the document must have the real code examples or snippets or real code examples from repository. Rules must described by words not by examples
- Always prefer a concise wording to make thing simple

## Project Overview

**pixel-watch-tagger** is a Kotlin application designed for Google Pixel Watch. The app tracks timestamped button presses across configurable categories and uses clustering algorithms to analyze patterns and predict future occurrences.

## Project Configuration

- **Platform**: Google Pixel Watch (Wear OS 4+)
- **Language**: Kotlin
- **Java Version**: Java 21
- **Min SDK**: 33 (Wear OS 4+)
- **Target SDK**: 34
- **IDE**: IntelliJ IDEA or Android Studio
- **Build System**: Gradle 8.9 with Kotlin DSL
- **UI Framework**: Jetpack Compose for Wear OS

### Version Management

All version numbers and SDK configurations are centralized in `gradle.properties`:
- SDK versions (compileSdk, minSdk, targetSdk)
- Plugin versions (Android Gradle Plugin, Kotlin)
- Dependency versions (Wear Compose, Compose UI, Activity Compose)
- App metadata (versionCode, versionName)
- Java/Kotlin configuration (javaVersion, jvmTargetVersion)

To update versions, modify `gradle.properties` instead of individual build files.

## Development Setup

### Initial Setup

**IMPORTANT**: On first interaction with this repository, run the git hooks setup:

```bash
./setup-hooks.sh
```

This configures the repository to enforce documentation updates on every commit. The setup script will:
- Configure git to use custom hooks from `.githooks/`
- Check for Claude Code installation
- Optionally enable automatic documentation updates via Claude Code

### Environment Requirements

- **Java JDK**: Java 21 or higher
- **Android SDK**: API level 34 (Android 14)
- **Wear OS SDK**: Included in Android SDK
- **IDE**: Android Studio (recommended) or IntelliJ IDEA with Android plugin
- **Gradle**: 8.9+ (included via wrapper)

### Build Commands

**Main Commands:**

```bash
# Style check - runs code formatting and linting
./gradlew styleCheck

# Test - runs unit tests with coverage report
./gradlew test
```

**Additional Commands:**

```bash
# Build the project
./gradlew build

# Run full check (includes tests, coverage verification, lint)
./gradlew check

# Install debug APK to connected watch
./gradlew installDebug

# Clean build artifacts
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

### Code Quality and Style Guidelines

This project follows strict code quality standards with automated formatting and linting. All contributors must adhere to the following guidelines:

#### Style Guides

Comprehensive style guides are available in the repository:

- **[STYLE_GUIDE.md](STYLE_GUIDE.md)**: Project coding conventions and best practices
  - Naming conventions, formatting, null safety
  - Coroutines, data classes, and modern language features
  - Based on official Kotlin conventions and Android guidelines

#### Automated Code Formatting with Spotless

The project uses **Spotless** with **ktlint** for automated code formatting.

**Key Commands:**

```bash
# Run style check (formatting + linting)
./gradlew styleCheck

# Or run formatting only
./gradlew spotlessApply
```

**Spotless Configuration:**
- Enforces 4-space indentation
- 100-character line limit
- ktlint 1.0.1 for Kotlin formatting
- Automatic trailing whitespace removal
- Ensures files end with newline

**Integration:**
- Spotless runs automatically in CI/CD
- Pre-commit hooks can trigger spotlessCheck
- Configuration in `app/build.gradle.kts`

#### Lint Checks

The project includes comprehensive lint checks, including Compose-specific rules:

```bash
# Run lint checks
./gradlew lint

# Generate lint report
./gradlew lintDebug
```

**Enabled Compose Lint Checks:**
- `ComposeUnstableCollections`: Detects unstable collections in Composables
- `ComposableNaming`: Enforces PascalCase for Composable functions
- `ComposeModifierMissing`: Ensures Composables accept Modifier parameters
- `ComposeRememberMissing`: Detects missing `remember` calls
- `ComposeParameterOrder`: Enforces correct parameter ordering
- `ComposeViewModelInjection`: Validates ViewModel injection patterns

Lint reports are generated in `app/build/reports/lint-results.html`

#### EditorConfig

The project includes `.editorconfig` for consistent formatting across all IDEs:
- 4-space indentation for Kotlin, XML, and Gradle files
- UTF-8 encoding
- LF line endings
- Automatic trailing whitespace trimming

Most modern IDEs (Android Studio, IntelliJ IDEA, VS Code) automatically respect these settings.

#### Unit Test Coverage Requirements

**MANDATORY**: All source code changes MUST include unit tests with minimum 60% code coverage.

This requirement is enforced automatically by the pre-commit hook.

**Key Requirements:**
- Every source file must have a corresponding test file
- Test file naming: `FooBar.kt` → `FooBarTest.kt`
- Test location: `app/src/main/java/...` → `app/src/test/java/...`
- Minimum coverage: 60% line coverage per changed file

**Coverage Commands:**

```bash
# Run tests with coverage report
./gradlew test

# View coverage report
open app/build/reports/jacoco/jacocoTestReport/html/index.html

# Run full check including coverage verification
./gradlew check
```

**Coverage Configuration:**
- Tool: JaCoCo 0.8.12
- Reports: XML, HTML, and CSV formats
- Excludes: Generated code, Android framework classes, test files
- Configuration: `app/build.gradle.kts` and `gradle.properties`
- Minimum threshold: Configurable via `minimumCoverageRequired` property

**Pre-commit Hook Behavior:**

The pre-commit hook automatically:
1. Detects changed source files
2. Verifies each has a corresponding test file
3. Runs all tests
4. Checks coverage for changed files
5. Blocks commit if coverage < 60%

**Emergency Bypass:**

Only use when absolutely necessary:
```bash
git commit --no-verify
```

#### Code Review Checklist

Before submitting code, ensure:
- [ ] `./gradlew styleCheck` has been run (formatting and linting)
- [ ] Unit tests added for all source code changes
- [ ] `./gradlew test` passes without errors (tests and coverage)
- [ ] Test coverage meets 60% minimum requirement
- [ ] `./gradlew build` passes without errors
- [ ] Code follows Kotlin style guide
- [ ] Composables follow Compose best practices
- [ ] Architecture patterns are maintained
- [ ] Documentation is updated

## Project Structure

```
pixel-watch-tagger/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/pixelwatchtagger/
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── colors.xml
│   │   │   │   └── mipmap-*/  (launcher icons)
│   │   │   └── AndroidManifest.xml
│   │   └── test/  (unit tests - to be added)
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

### Key Files

- **gradle.properties**: Centralized version management for all SDK versions, plugins, and dependencies
- **app/build.gradle.kts**: App module configuration with dependencies (reads from gradle.properties)
- **build.gradle.kts**: Root project configuration
- **settings.gradle.kts**: Project settings and module inclusion
- **AndroidManifest.xml**: Wear OS app configuration and permissions
- **MainActivity.kt**: Main entry point with Compose UI

## Development Workflow

### Git Hooks - Automated Documentation Enforcement

This repository uses git hooks to enforce documentation updates. A pre-commit hook automatically validates (and optionally updates) documentation before every commit.

#### How It Works

**Automatic Mode** (Claude Code installed and enabled):
1. Pre-commit hook detects changes
2. Invokes Claude Code in headless mode with auto-approve for edits
3. Updates CHANGELOG.md and CLAUDE.md automatically
4. Stages the updated documentation
5. Commit proceeds automatically

**Manual Mode** (fallback):
1. Pre-commit hook validates changes
2. Blocks commit if documentation missing
3. Provides clear instructions on what to update
4. Allows commit after manual updates

#### Enable/Disable Auto-Update

```bash
# Enable automatic updates with Claude Code
git config --local hooks.enableClaude true

# Disable automatic updates (manual mode)
git config --local hooks.enableClaude false

# Emergency bypass (use sparingly)
git commit --no-verify
```

### Changelog Management

**MANDATORY**: `CHANGELOG.md` MUST be updated with EVERY substantive commit:
- Update CHANGELOG.md BEFORE making the commit (or let Claude Code do it automatically)
- Keep entries concise: maximum 3 sentences per change
- Add entry under "Unreleased" section
- Follow Keep a Changelog format (https://keepachangelog.com/)
- Categories: Added, Changed, Deprecated, Removed, Fixed, Security
- Include brief description of what changed and why

**Excluded from requirement**:
- IDE configuration files (.idea/, *.iml)
- Git configuration (.gitignore, .gitattributes)
- Documentation-only changes (*.md files)
- CHANGELOG.md itself

### CLAUDE.md Updates

Update CLAUDE.md when making **structural or architectural changes**:
- New build files (build.gradle, pom.xml, etc.)
- Project structure changes
- New development workflows or commands
- Architecture or design pattern changes

The git hook will detect these changes and either:
- Automatically update via Claude Code (if enabled)
- Remind you to update manually

## Current Development Status

### Completed
- ✅ Basic Gradle build system with Wear OS support
- ✅ Minimal Kotlin app with Jetpack Compose
- ✅ Java 21 configuration
- ✅ Project structure and AndroidManifest

### Next Steps
1. Implement Room database schema (Button and Event entities)
2. Create circular button layout UI with dynamic positioning
3. Implement View/Edit mode with gesture navigation
4. Add statistics screen with scatter plot visualization
5. Implement clustering algorithm (DBSCAN) for pattern detection
6. Add prediction system with countdown timers
7. Implement background workers for data cleanup and prediction updates

See REQUIREMENTS.md for detailed feature specifications.
