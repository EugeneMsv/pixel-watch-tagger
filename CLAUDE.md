# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

```bash
# Build the project
./gradlew build

# Install debug APK to connected watch
./gradlew installDebug

# Clean build artifacts
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

### Code Quality Tools

The IntelliJ configuration includes:
- **CheckStyle**: Configured with custom rules
- **Google Java Format**: Enabled for code formatting

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
