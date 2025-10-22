# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**pixel-watch-tagger** is a Java application designed for Google Pixel Watch. The project is currently in its initial setup phase.

## Project Configuration

- **Platform**: Google Pixel Watch (Wear OS)
- **Language**: Java
- **Language Level**: Java 17
- **JDK**: 1.8 (configured)
- **IDE**: IntelliJ IDEA
- **Build System**: To be determined (likely Gradle for Android/Wear OS development)

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

### Environment

This project is in early initialization. When developing:
- Wear OS applications typically use Android Studio or IntelliJ IDEA with Android plugin
- Will require Android SDK and Wear OS SDK setup
- Standard Android/Wear OS project structure will be needed

### Code Quality Tools

The IntelliJ configuration includes:
- **CheckStyle**: Configured with custom rules
- **Google Java Format**: Enabled for code formatting

## Project Structure

Currently only IntelliJ IDEA configuration files exist. Standard Wear OS structure should include:
- `app/src/main/java` - Application source code
- `app/src/main/res` - Resources (layouts, drawables, etc.)
- `app/src/main/AndroidManifest.xml` - App manifest
- `build.gradle` - Build configuration
- Wear OS specific configurations and dependencies

## Development Workflow

### Git Hooks - Automated Documentation Enforcement

This repository uses git hooks to enforce documentation updates. A pre-commit hook automatically validates (and optionally updates) documentation before every commit.

#### How It Works

**Automatic Mode** (Claude Code installed and enabled):
1. Pre-commit hook detects changes
2. Invokes Claude Code to update CHANGELOG.md and CLAUDE.md
3. Stages the updated documentation
4. Commit proceeds automatically

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

## Future Development

When implementing the Pixel Watch application:
1. Set up Android/Wear OS project structure
2. Configure Gradle build system with Wear OS dependencies
3. Define app features and architecture
4. Update this CLAUDE.md with build commands, deployment instructions, and architecture details
