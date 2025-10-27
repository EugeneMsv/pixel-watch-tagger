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

### Changed
- Enhanced pre-commit hook with headless mode and auto-approve for seamless documentation updates
- Refactored app/build.gradle.kts to use gradle.properties for version configuration, improving maintainability
- Disabled Gradle configuration cache temporarily to resolve build issues

[Unreleased]: https://github.com/username/pixel-watch-tagger/compare/v0.1.0...HEAD
