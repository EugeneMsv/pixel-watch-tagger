# Code Formatting Guide

This document describes the code formatting tools and workflows for the Pixel Watch Tagger project.

## Table of Contents

- [Overview](#overview)
- [Spotless Setup](#spotless-setup)
- [Gradle Tasks](#gradle-tasks)
- [IDE Integration](#ide-integration)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)

---

## Overview

The project uses **Spotless** for automated code formatting with the following tools:
- **ktlint 1.0.1**: Kotlin code formatting
- **EditorConfig**: IDE-agnostic formatting rules
- **Android Lint**: Compose-specific checks

### Benefits

- **Consistency**: All code follows the same style
- **Automation**: No manual formatting needed
- **Quality**: Catches style issues before code review
- **Speed**: Faster code reviews focused on logic, not style

---

## Spotless Setup

### Installation

Spotless is already configured in the project. No additional installation is required.

### Configuration Files

1. **`build.gradle.kts`** (root):
   ```kotlin
   plugins {
       id("com.diffplug.spotless") version "6.25.0" apply false
   }
   ```

2. **`app/build.gradle.kts`**:
   ```kotlin
   plugins {
       id("com.diffplug.spotless")
   }

   spotless {
       kotlin {
           target("src/**/*.kt")
           ktlint("1.0.1")
           trimTrailingWhitespace()
           endWithNewline()
       }
   }
   ```

3. **`.editorconfig`**:
   - Defines formatting rules for all IDEs
   - Automatically detected by Spotless and ktlint

---

## Gradle Tasks

### Core Commands

#### Check Formatting

Checks if code is formatted correctly **without modifying** files:

```bash
./gradlew spotlessCheck
```

**Output:**
- ✅ Success: No formatting issues
- ❌ Failure: Lists files that need formatting

**Use when:**
- Running in CI/CD pipelines
- Checking code before committing
- Verifying formatting status

#### Apply Formatting

Automatically formats all files according to rules:

```bash
./gradlew spotlessApply
```

**Output:**
- Modifies files in place
- Reports which files were changed

**Use when:**
- Before committing changes
- After writing new code
- Fixing formatting issues

### Selective Formatting

Format only specific file types:

```bash
# Format only Kotlin files
./gradlew spotlessKotlinApply

# Format only Kotlin Gradle files
./gradlew spotlessKotlinGradleApply

# Format only XML files
./gradlew spotlessXmlApply
```

Check specific file types:

```bash
./gradlew spotlessKotlinCheck
./gradlew spotlessKotlinGradleCheck
./gradlew spotlessXmlCheck
```

### Combined Workflows

#### Pre-Commit Workflow

Run before every commit:

```bash
./gradlew spotlessApply && ./gradlew build
```

This ensures:
1. Code is properly formatted
2. Project builds successfully
3. All tests pass

#### Full Quality Check

Comprehensive quality check:

```bash
./gradlew clean spotlessApply build lint test
```

Runs:
1. Clean build
2. Format code
3. Build project
4. Run lint checks
5. Run all tests

---

## IDE Integration

### Android Studio / IntelliJ IDEA

#### EditorConfig Support

Android Studio automatically detects `.editorconfig`:
- No configuration needed
- Format on save can be enabled

#### Manual Formatting

To format manually in IDE:
1. Open Terminal in IDE
2. Run: `./gradlew spotlessApply`
3. Refresh project (File → Synchronize)

#### Format on Save (Optional)

Enable automatic formatting:

1. Go to **Settings** → **Tools** → **Actions on Save**
2. Enable **"Reformat code"**
3. Enable **"Optimize imports"**

**Note:** This uses IDE formatter, not Spotless. For Spotless consistency, use Gradle tasks.

#### Gradle Integration

Run Spotless from Gradle tool window:
1. Open **Gradle** tool window (View → Tool Windows → Gradle)
2. Navigate to **Tasks** → **formatting**
3. Double-click **spotlessApply** or **spotlessCheck**

### VS Code

Install extensions:
1. **EditorConfig for VS Code**: Respects `.editorconfig`
2. **Gradle Tasks**: Run Gradle tasks from IDE

Run Spotless:
- Terminal: `./gradlew spotlessApply`
- Command Palette: "Tasks: Run Task" → "spotlessApply"

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Code Quality

on: [push, pull_request]

jobs:
  format-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Check code formatting
        run: ./gradlew spotlessCheck

      - name: Build and test
        run: ./gradlew build

      - name: Run lint
        run: ./gradlew lint
```

### Pre-Commit Hook

Create `.githooks/pre-commit`:

```bash
#!/bin/bash

echo "Running Spotless check..."

./gradlew spotlessCheck

if [ $? -ne 0 ]; then
    echo "❌ Code formatting check failed!"
    echo "Run './gradlew spotlessApply' to fix formatting issues."
    exit 1
fi

echo "✅ Code formatting check passed!"
```

Enable the hook:
```bash
chmod +x .githooks/pre-commit
git config core.hooksPath .githooks
```

---

## Spotless Rules

### Kotlin Formatting Rules

The project enforces the following Kotlin rules:

#### Indentation
- **4 spaces** (no tabs)
- Continuation indent: 4 spaces

#### Line Length
- **Maximum 100 characters** per line
- Long lines must be wrapped

#### Whitespace
- Space after comma: `listOf(1, 2, 3)`
- Space around operators: `val sum = a + b`
- No trailing whitespace

#### Imports
- Organized alphabetically
- No wildcard imports (disabled in ktlint)
- Remove unused imports

#### Newlines
- File must end with newline
- Max 2 consecutive blank lines
- No blank lines at start of blocks

#### Naming Conventions
- Classes: PascalCase
- Functions: camelCase
- Constants: UPPER_SNAKE_CASE
- Private properties: camelCase with underscore prefix

### XML Formatting Rules

For Android resource files:

- 4-space indentation
- Trim trailing whitespace
- End with newline

### Gradle Formatting Rules

For `.gradle.kts` files:

- ktlint formatting
- 4-space indentation

---

## Troubleshooting

### Issue: Spotless Check Fails

**Symptoms:**
```
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:spotlessKotlinCheck'.
> The following files had format violations:
    src/main/java/.../MainActivity.kt
```

**Solution:**
```bash
./gradlew spotlessApply
```

### Issue: Files Keep Getting Reformatted

**Cause:** IDE formatter differs from Spotless rules

**Solution:**
1. Always run `./gradlew spotlessApply` before committing
2. Disable IDE auto-format on save
3. Use Spotless as the source of truth

### Issue: Spotless and IDE Conflict

**Symptoms:** IDE warns about formatting that Spotless accepts

**Solution:**
1. Trust Spotless rules over IDE warnings
2. Configure IDE to match EditorConfig settings
3. Run `./gradlew spotlessApply` to fix

### Issue: Slow Spotless Execution

**Symptoms:** `spotlessApply` takes a long time

**Solutions:**
```bash
# Use Gradle daemon (should be automatic)
./gradlew --daemon spotlessApply

# Format only changed files (requires git)
./gradlew spotlessApply --include-only-changed

# Increase Gradle memory
export GRADLE_OPTS="-Xmx2048m"
./gradlew spotlessApply
```

### Issue: Spotless Breaks Build

**Symptoms:** Build fails after running Spotless

**Cause:** Rare formatting changes can affect code logic

**Solution:**
1. Review changes: `git diff`
2. If incorrect, report issue with ktlint
3. Revert specific file: `git checkout -- path/to/file.kt`
4. Add to ignore: Configure in `app/build.gradle.kts`

```kotlin
spotless {
    kotlin {
        targetExclude("**/GeneratedClass.kt")
    }
}
```

---

## Best Practices

### Daily Workflow

1. **Before starting work:**
   ```bash
   git pull origin main
   ./gradlew spotlessApply
   ```

2. **During development:**
   - Write code naturally
   - Don't worry about formatting

3. **Before committing:**
   ```bash
   ./gradlew spotlessApply
   git add .
   git commit -m "Your message"
   ```

### Code Review

**Reviewers should:**
- Not comment on formatting issues (handled by Spotless)
- Focus on logic, architecture, and correctness
- Verify Spotless check passed in CI

**Authors should:**
- Run `spotlessApply` before pushing
- Ensure CI passes before requesting review
- Not commit formatting-only changes with logic changes

### Team Guidelines

1. **Never disable Spotless** without team discussion
2. **Run spotlessApply** before every commit
3. **Trust the formatter** - don't manually adjust
4. **Report issues** if Spotless produces incorrect formatting

---

## Advanced Configuration

### Custom ktlint Rules

To customize ktlint rules, edit `app/build.gradle.kts`:

```kotlin
spotless {
    kotlin {
        ktlint("1.0.1")
            .editorConfigOverride(
                mapOf(
                    "max_line_length" to "120",  // Change line length
                    "ktlint_standard_no-wildcard-imports" to "disabled"
                )
            )
    }
}
```

### Exclude Files

Exclude generated or third-party files:

```kotlin
spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude(
            "**/build/**/*.kt",
            "**/generated/**/*.kt",
            "**/third_party/**/*.kt"
        )
    }
}
```

### License Headers

Add license headers to all files:

```kotlin
spotless {
    kotlin {
        licenseHeaderFile(rootProject.file("spotless/license-header.txt"))
    }
}
```

Create `spotless/license-header.txt`:
```
/*
 * Copyright (C) 2025 Pixel Watch Tagger
 * Licensed under the Apache License, Version 2.0
 */
```

---

## Useful Links

- [Spotless Documentation](https://github.com/diffplug/spotless)
- [ktlint Documentation](https://ktlint.github.io/)
- [EditorConfig Specification](https://editorconfig.org/)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

---

## Quick Reference

### Most Common Commands

```bash
# Check formatting
./gradlew spotlessCheck

# Fix formatting
./gradlew spotlessApply

# Full build with formatting
./gradlew spotlessApply build

# Pre-commit check
./gradlew spotlessApply && ./gradlew build && git add .
```

### Command Comparison

| Command | Modifies Files | Fails on Issues | Use Case |
|---------|---------------|-----------------|----------|
| `spotlessCheck` | ❌ No | ✅ Yes | CI/CD, verification |
| `spotlessApply` | ✅ Yes | ❌ No | Local development, fixing |

---

**Document Version:** 1.0
**Last Updated:** 2025-01-30
