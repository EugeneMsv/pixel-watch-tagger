# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**Note**: Each entry should be concise (maximum 3 sentences).

## [Unreleased]

### Added
- Initial project setup with IntelliJ IDEA configuration
- Git hooks system for mandatory CHANGELOG.md and CLAUDE.md updates
- Claude Code integration for automatic documentation updates
- Gradle 8.9 build system with Kotlin DSL and Java 21 support
- Basic Wear OS 4+ project structure with Jetpack Compose
- Minimal launcher app displaying "Pixel Watch Tagger" text
- Centralized version management in gradle.properties for SDK versions, dependencies, and app metadata
- Mandatory unit test coverage requirement with 60% minimum threshold enforced by pre-commit hook. JaCoCo integration provides automated test coverage reporting and verification for all source code changes.
- Simplified Gradle tasks: styleCheck (formatting + linting) and test (unit tests + coverage), reducing common workflows to two main commands

### Changed
- Enhanced pre-commit hook with headless mode and auto-approve for seamless documentation updates
- Refactored app/build.gradle.kts to use gradle.properties for version configuration, improving maintainability
- Disabled Gradle configuration cache temporarily to resolve build issues
- Improved pre-commit hook to properly stage CHANGELOG.md after automatic updates. Added verification to ensure documentation modifications are captured before commit proceeds.
- Updated pre-commit hook manual invocation command to use proper headless mode flags for better user experience
- Completely rewrote all style guides (KOTLIN_STYLE_GUIDE.md, COMPOSE_STYLE_GUIDE.md, ARCHITECTURE.md) to use text-based rule descriptions instead of code examples, making them more concise and easier to maintain while preserving all guidance
- Integrated spotlessApply into pre-commit hook to automatically format code before changelog validation, ensuring consistent code style with automatic staging of formatted files
- Simplified coverage check script to run gradle task and extract overall coverage percentage, reducing code complexity from 246 to 84 lines

[Unreleased]: https://github.com/username/pixel-watch-tagger/compare/v0.1.0...HEAD
