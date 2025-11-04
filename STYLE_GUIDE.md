# Kotlin Coding Style Guide

This document defines the Kotlin coding standards and best practices for the Pixel Watch Tagger project, based on the official Kotlin conventions and Android best practices for 2025.

## Table of Contents

- [Code Organization](#code-organization)
- [Naming Conventions](#naming-conventions)
- [Formatting](#formatting)
- [Language Features](#language-features)
- [Null Safety](#null-safety)
- [Coroutines and Concurrency](#coroutines-and-concurrency)
- [Data Classes and Objects](#data-classes-and-objects)
- [Extensions](#extensions)
- [Testing](#testing)

---

## Code Organization

### Source File Structure

Source files must follow this exact order:

1. **Copyright or license header** (optional)
2. **Package statement** (no line breaks)
3. **Import statements** (sorted alphabetically, no wildcards)
4. **Top-level declarations** (classes, functions, properties)

### Package Structure

Follow the package structure with the common root package omitted:

```
com/example/pixelwatchtagger/
├── data/
│   ├── database/
│   ├── repository/
│   └── model/
├── domain/
│   ├── clustering/
│   └── prediction/
├── ui/
│   ├── main/
│   ├── edit/
│   ├── stats/
│   └── components/
├── workers/
└── di/
```

### Multiple Declarations Per File

Placing multiple declarations in the same file is encouraged when they are:
- Closely related semantically
- File size remains reasonable (not exceeding 500 lines)

For example, a ButtonPosition data class and its related ButtonPositionCalculator interface can coexist in the same file if they are tightly coupled.

---

## Naming Conventions

### Classes and Objects

- **Classes**: Use PascalCase for class names (e.g., ButtonRepository, ClusteringService)
- **Objects**: Use PascalCase for object declarations (e.g., DatabaseConstants, AppConfig)
- **Companion objects**: Use PascalCase for named companion objects, or omit the name entirely

Companion objects can optionally be named (like Factory) or left unnamed. If used as a factory, the name Factory is acceptable.

### Functions and Properties

- **Functions**: Use camelCase and prefer verb phrases that describe the action (e.g., calculateButtonPosition, recordEvent, loadData)
- **Properties**: Use camelCase and prefer noun phrases (e.g., buttonCount, userName, isLoading)
- **Constants**: Use UPPER_SNAKE_CASE for compile-time constants (e.g., MAX_BUTTON_COUNT, DATABASE_NAME)

Avoid using PascalCase for function names, and avoid snake_case for regular properties or functions.

### Boolean Properties

Boolean properties should use meaningful prefixes:
- Prefix with `is` for state (e.g., isEditMode, isLoading, isEnabled)
- Prefix with `has` for possession (e.g., hasEvents, hasChildren, hasPermission)
- Prefix with `can` for capability (e.g., canAddButton, canDelete, canSave)
- Prefix with `should` for recommendations (e.g., shouldShowPrediction, shouldUpdate)

---

## Formatting

### Column Limit

Lines should not exceed 100 characters. If a line exceeds this limit, it must be line-wrapped using appropriate indentation.

### Indentation

Use 4 spaces for indentation at all levels. Never use tabs. This applies to Kotlin files, Gradle files, and XML resources.

### Whitespace

**Around operators:**
- Always use spaces around binary operators (e.g., assignment, arithmetic, comparison)
- Write `val sum = a + b` not `val sum=a+b`

**After commas:**
- Always place a space after commas in lists, function parameters, and generic type arguments
- Write `listOf(1, 2, 3)` not `listOf(1,2,3)`

**Around colons in type annotations:**
- Always place a space after the colon in type annotations, never before
- Write `val name: String` not `val name:String` or `val name :String`

**Function parameters:**
- Use consistent spacing in function parameter lists with spaces after commas and around colons

### Line Breaks

**Chaining calls:**
- When chaining multiple method calls, place each call on a new line with proper indentation
- The dot operator should be on the same line as the method being called
- Indent each subsequent call one level deeper than the initial receiver

**Long parameter lists:**
- When a function has multiple parameters that would exceed the line limit, place each parameter on its own line
- Indent parameters one level deeper than the function declaration
- Place the closing parenthesis and opening brace on the same line as the last parameter or on a new line

---

## Language Features

### Immutability First

**Always prefer `val` over `var`:**
- Use `val` for variables that are assigned once and never reassigned
- Only use `var` when reassignment is genuinely required
- This applies to local variables, properties, and parameters

**Use immutable collections:**
- Prefer List, Set, and Map over their mutable counterparts
- Only use MutableList, MutableSet, or MutableMap when mutation is necessary
- Immutable collections improve thread safety and make code easier to reason about

### When Expressions

**Prefer `when` over multiple `if-else` chains:**
- Use when expressions when you have more than two conditional branches
- When expressions are more readable and can be exhaustive
- Use ranges (e.g., `in 1..3`) for numeric comparisons
- Use `else` for catch-all cases, but avoid it when dealing with sealed classes

**Exhaustive `when` for sealed classes:**
- When using sealed classes, cover all possible cases without an else branch
- The compiler will enforce exhaustiveness, catching missing cases at compile time
- This ensures all subclasses are handled explicitly

### Scope Functions

Use the appropriate scope function based on the use case:

- **`let`**: Use for non-null checks and transformations. The context object is available as `it` (or renamed). Returns the lambda result.

- **`apply`**: Use for object configuration and initialization. The context object is available as `this`. Returns the context object itself.

- **`run`**: Use to execute a block of code and return a result. The context object is available as `this`. Returns the lambda result.

- **`with`**: Use to call multiple methods on the same object without repeating the object name. The context object is passed as a parameter and available as `this`.

- **`also`**: Use for additional operations or side effects while keeping the original object in the call chain. The context object is available as `it`. Returns the context object.

### String Templates

**Use string templates over concatenation:**
- Embed variables and expressions directly in strings using dollar sign syntax
- Write `"Button $label pressed"` instead of concatenating with plus operators

**Use curly braces for expressions:**
- When embedding expressions (not just simple variables), wrap them in curly braces
- Write `"Count: ${buttons.size}"` or `"Time: ${formatTime(timestamp)}"`

---

## Null Safety

### Avoid `!!` (Not-Null Assertion)

The not-null assertion operator (`!!`) should be avoided in production code. Use safer alternatives:

- Use safe call operator with elvis operator for defaults
- Use `let` with safe call for conditional execution
- Use `checkNotNull` or `requireNotNull` with descriptive error messages when null is truly unexpected

### Safe Calls and Elvis Operator

**Safe call operator (`?.`):**
- Use the safe call operator to access properties or methods on potentially null objects
- The expression returns null if the receiver is null

**Elvis operator (`?:`) for defaults:**
- Use the elvis operator to provide default values when the left-hand side is null
- This is cleaner than if-else null checks

**Chaining:**
- Safe calls and elvis operators can be chained for complex null-safe expressions
- The final elvis operator can provide an ultimate default value

### Early Returns

**Use early returns for null checks:**
- At the beginning of functions, return early if required parameters are null
- This uses smart casting to treat the variable as non-null in subsequent code
- This pattern is clearer than nested if-statements

**Guard clauses:**
- Use if-null-return or if-not-condition-return at the start of functions
- This reduces nesting and improves readability

### Nullable Types

**Make objects non-nullable whenever possible:**
- Design data classes with required fields as non-nullable
- Use default values instead of nullable types when appropriate
- Only make fields nullable when they represent truly optional or unknown data
- Avoid unnecessary nullable types that complicate usage

---

## Coroutines and Concurrency

### Structured Concurrency

**Always use structured concurrency with appropriate scopes:**
- In ViewModels, use `viewModelScope` to automatically cancel coroutines when the ViewModel is cleared
- Wrap suspend function bodies with try-catch to handle exceptions
- Never launch coroutines in GlobalScope, as they won't be automatically cancelled
- Use appropriate exception handling to update UI state on errors

### Async for Parallel Tasks

**Use `async` for independent parallel operations:**
- When multiple suspend functions can run concurrently, use async/await
- Each async builder returns a Deferred that can be awaited
- Use coroutineScope to ensure all async operations complete before proceeding
- This pattern significantly improves performance for independent operations

### Dispatchers

**Use appropriate dispatchers for different work types:**

**IO operations:**
- Use `Dispatchers.IO` for database access, network requests, and file operations
- Switch back to `Dispatchers.Main` with `withContext` before updating UI

**CPU-intensive work:**
- Use `Dispatchers.Default` for computational tasks like clustering, sorting, or data processing
- Switch back to `Dispatchers.Main` before updating UI

**Main thread:**
- UI updates must always happen on `Dispatchers.Main`
- Use `withContext(Dispatchers.Main)` to switch from background threads

### Flow for Streams

**Use Flow for reactive streams:**
- Room database queries should return Flow for automatic updates
- Repositories should expose Flow for observable data
- In ViewModels, convert Flow to StateFlow using stateIn with appropriate sharing strategy
- Use `SharingStarted.WhileSubscribed` with a timeout (e.g., 5000ms) for lifecycle-aware collection

---

## Data Classes and Objects

### Data Classes

**Use data classes for data models:**
- Data classes automatically generate equals, hashCode, toString, copy, and destructuring
- All properties in the primary constructor should be val (immutable)
- Data classes are ideal for DTOs, database entities, and UI state models

**Benefits:**
- Automatic structural equality based on properties
- Copy method enables immutable updates
- Destructuring allows extracting properties into variables
- toString provides readable string representation

**Using `copy()`:**
- Use the copy method to create modified copies of immutable objects
- Specify only the properties that need to change
- All other properties retain their original values

### Sealed Classes

**Use sealed classes for restricted hierarchies:**
- Sealed classes represent closed type hierarchies
- All subclasses must be defined in the same file (or as nested classes)
- Perfect for state representations, result types, and navigation destinations
- When expressions on sealed classes are exhaustive without else branches

### Object Declarations

**Use objects for singletons:**
- Object declarations create thread-safe singletons
- Use for constants, configuration, or stateless utilities
- Objects are initialized lazily on first access

---

## Extensions

### Extension Functions

**Use extensions to add functionality to existing classes:**
- Extension functions add methods to classes without modifying their source
- They're resolved statically based on the declared type, not runtime type
- Keep extensions focused on a single responsibility
- Place extensions in files logically grouped by the type they extend

**Keep extensions focused and specific:**
- Give extension functions descriptive names that clearly indicate their purpose
- Avoid overly generic names like `process` or `handle`
- Extensions should feel natural as if they were part of the original class

### Extension Properties

Extension properties can add computed properties to existing classes without storing state:
- They must be computed (using get) and cannot have backing fields
- Useful for derived properties based on existing class members
- Keep computation lightweight since they appear as properties, not functions

---

## Testing

### Test Naming

**Use descriptive test names in backticks:**
- Test function names should describe the scenario and expected outcome
- Use backticks to allow spaces and special characters
- Follow pattern: "scenario should expected_result"
- Write complete sentences that serve as documentation

### Test Structure

**Follow Arrange-Act-Assert pattern:**
- **Arrange**: Set up test data and dependencies
- **Act**: Execute the code under test
- **Assert**: Verify the expected outcomes

Clearly separate these three sections with blank lines or comments for readability.

### Mock and Stub

**Use dependency injection for testability:**
- Constructor inject dependencies in ViewModels, repositories, and services
- This allows easy substitution with mocks or fakes in tests
- Use mocking libraries to create test doubles
- Verify interactions and stub return values as needed
- Use runTest for testing suspend functions and coroutines

---

## Common Patterns

### Repository Pattern

Repositories abstract data sources and provide a clean API for the domain layer:
- Define repository interfaces in the domain or data layer
- Implementation depends on DAOs or network clients
- Expose Flow for observable data that updates automatically
- Use suspend functions for one-time operations
- Handle errors within the repository or propagate them to the caller

### ViewModel with State

ViewModels manage UI state and handle user events:
- Use MutableStateFlow internally, expose as StateFlow publicly
- Initialize state in init block or when first needed
- Handle user events through public functions
- Launch coroutines in viewModelScope for automatic cancellation
- Use catch operators on flows to convert exceptions into error states

---

## Documentation

### KDoc Comments

**Document public APIs:**
- Use KDoc comments for public classes, functions, and properties
- Describe what the function does, not how it does it
- Document parameters with @param tags
- Document return values with @return tags
- Document exceptions with @throws tags
- Keep documentation concise but complete

**Link to related functions:**
- Use @see tags to reference related classes or functions
- This helps users discover related functionality
- Creates navigable documentation

---

## References

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Kotlin Best Practices 2025](https://medium.com/@hiren6997/kotlin-best-practices-every-android-developer-should-know-in-2025-0888ba82a416)

---

**Document Version:** 2.0
**Last Updated:** 2025-11-03
