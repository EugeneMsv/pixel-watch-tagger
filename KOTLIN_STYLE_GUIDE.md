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

**Example:**
```kotlin
// ButtonPosition.kt
data class ButtonPosition(
    val button: Button,
    val x: Float,
    val y: Float,
    val size: Float
)

interface ButtonPositionCalculator {
    fun calculatePositions(buttons: List<Button>): List<ButtonPosition>
}
```

---

## Naming Conventions

### Classes and Objects

- **Classes**: Use PascalCase
- **Objects**: Use PascalCase
- **Companion objects**: Use PascalCase or omit name

```kotlin
class ButtonRepository { }
object DatabaseConstants { }

class MyClass {
    companion object Factory { // or just companion object
        fun create(): MyClass = MyClass()
    }
}
```

### Functions and Properties

- **Functions**: Use camelCase, prefer verb phrases
- **Properties**: Use camelCase, prefer noun phrases
- **Constants**: Use UPPER_SNAKE_CASE

```kotlin
// Good
fun calculateButtonPosition(): ButtonPosition { }
val buttonCount: Int = 9
const val MAX_BUTTON_COUNT = 9

// Bad
fun ButtonPosition(): ButtonPosition { }
fun get_button_count(): Int { }
```

### Boolean Properties

Prefix with `is`, `has`, `can`, or `should`:

```kotlin
val isEditMode: Boolean
val hasEvents: Boolean
val canAddButton: Boolean
val shouldShowPrediction: Boolean
```

---

## Formatting

### Column Limit

**100 characters per line**. Lines that exceed this limit must be line-wrapped.

### Indentation

**4 spaces** for indentation. Never use tabs.

### Whitespace

**Around operators:**
```kotlin
val sum = a + b  // Good
val sum=a+b      // Bad
```

**After commas:**
```kotlin
listOf(1, 2, 3)  // Good
listOf(1,2,3)    // Bad
```

**Around colons in type annotations:**
```kotlin
val name: String  // Good - space after colon
val name:String   // Bad
```

**Function parameters:**
```kotlin
fun calculatePosition(x: Float, y: Float): Position {
    // ...
}
```

### Line Breaks

**Chaining calls:**
```kotlin
// Good
database.buttonDao()
    .getAllButtons()
    .filter { it.isActive }
    .sortedBy { it.displayOrder }

// Bad
database.buttonDao().getAllButtons()
    .filter { it.isActive }.sortedBy { it.displayOrder }
```

**Long parameter lists:**
```kotlin
// Good
fun createButton(
    label: String,
    emoji: String,
    color: String,
    displayOrder: Int
): Button {
    // ...
}

// Bad
fun createButton(label: String, emoji: String, color: String, displayOrder: Int): Button {
    // ...
}
```

---

## Language Features

### Immutability First

**Always prefer `val` over `var`:**
```kotlin
// Good
val name = "Coffee"
val buttons = listOf(button1, button2)

// Bad
var name = "Coffee"  // Not reassigned later
```

**Use immutable collections:**
```kotlin
// Good
val buttons: List<Button> = listOf()
val buttonMap: Map<Int, Button> = mapOf()

// Bad
val buttons: MutableList<Button> = mutableListOf()  // Unless mutation needed
```

### When Expressions

**Prefer `when` over multiple `if-else`:**
```kotlin
// Good
when (buttonCount) {
    0 -> showEmptyState()
    in 1..3 -> showSmallLayout()
    in 4..6 -> showMediumLayout()
    in 7..9 -> showLargeLayout()
    else -> error("Too many buttons")
}

// Bad
if (buttonCount == 0) {
    showEmptyState()
} else if (buttonCount <= 3) {
    showSmallLayout()
} else if (buttonCount <= 6) {
    showMediumLayout()
} else if (buttonCount <= 9) {
    showLargeLayout()
} else {
    error("Too many buttons")
}
```

**Exhaustive `when` for sealed classes:**
```kotlin
sealed class AppMode {
    object View : AppMode()
    object Edit : AppMode()
}

// Good - exhaustive, no else needed
fun handleMode(mode: AppMode) = when (mode) {
    is AppMode.View -> handleViewMode()
    is AppMode.Edit -> handleEditMode()
}
```

### Scope Functions

**Use appropriate scope functions:**

- **`let`**: Non-null checks and transformations
  ```kotlin
  button?.let { btn ->
      recordEvent(btn.id)
  }
  ```

- **`apply`**: Object configuration
  ```kotlin
  val button = Button().apply {
      label = "Coffee"
      emoji = "☕"
      color = "#795548"
  }
  ```

- **`run`**: Execute block and return result
  ```kotlin
  val result = run {
      val x = calculateX()
      val y = calculateY()
      Position(x, y)
  }
  ```

- **`with`**: Call multiple methods on object
  ```kotlin
  with(canvas) {
      drawCircle(centerX, centerY, radius, paint)
      drawText(label, x, y, textPaint)
  }
  ```

- **`also`**: Additional operations (side effects)
  ```kotlin
  val button = createButton(...)
      .also { log("Created button: ${it.label}") }
  ```

### String Templates

**Use string templates over concatenation:**
```kotlin
// Good
val message = "Button ${button.label} pressed at $timestamp"

// Bad
val message = "Button " + button.label + " pressed at " + timestamp
```

**Use curly braces for expressions:**
```kotlin
val info = "Button count: ${buttons.size}"
val formatted = "Time: ${formatTime(timestamp)}"
```

---

## Null Safety

### Avoid `!!` (Not-Null Assertion)

The `!!` operator should be avoided. Use safer alternatives:

```kotlin
// Bad
val length = name!!.length

// Good - use safe call with elvis
val length = name?.length ?: 0

// Good - use let
name?.let { safeName ->
    processName(safeName)
}

// Good - use checkNotNull with message
val safeName = checkNotNull(name) { "Name must not be null" }
```

### Safe Calls and Elvis Operator

**Safe call operator (`?.`):**
```kotlin
val lastUsed = button?.lastUsedAt
```

**Elvis operator (`?:`) for defaults:**
```kotlin
val displayOrder = button?.displayOrder ?: 0
val label = button?.label ?: "Unknown"
```

**Chaining:**
```kotlin
val prediction = database
    ?.buttonDao()
    ?.getButton(id)
    ?.let { calculatePrediction(it) }
    ?: Prediction.NONE
```

### Early Returns

**Use early returns for null checks:**
```kotlin
// Good
fun processButton(button: Button?) {
    val btn = button ?: return
    // Process btn (smart cast to non-null)
}

// Alternative with guard clause
fun processButton(button: Button?) {
    if (button == null) return
    // Process button (smart cast to non-null)
}
```

### Nullable Types

**Make objects non-nullable whenever possible:**
```kotlin
// Good - non-nullable with default
data class Button(
    val label: String,
    val emoji: String,
    val displayOrder: Int = 0,  // Default instead of nullable
    val lastUsedAt: Long? = null  // Truly optional
)

// Bad - unnecessary nullable
data class Button(
    val label: String?,  // Should always have a label
    val emoji: String?   // Should always have an emoji
)
```

---

## Coroutines and Concurrency

### Structured Concurrency

**Always use structured concurrency with appropriate scopes:**
```kotlin
class MainViewModel @Inject constructor(
    private val repository: ButtonRepository
) : ViewModel() {

    fun loadButtons() {
        viewModelScope.launch {
            try {
                val buttons = repository.getAllButtons()
                _buttons.value = buttons
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
```

### Async for Parallel Tasks

**Use `async` for independent parallel operations:**
```kotlin
suspend fun loadDashboardData(): DashboardData = coroutineScope {
    val buttonsDeferred = async { repository.getButtons() }
    val eventsDeferred = async { repository.getRecentEvents() }
    val predictionsDeferred = async { predictionService.calculateAll() }

    DashboardData(
        buttons = buttonsDeferred.await(),
        events = eventsDeferred.await(),
        predictions = predictionsDeferred.await()
    )
}
```

### Dispatchers

**Use appropriate dispatchers:**
```kotlin
// IO operations
viewModelScope.launch(Dispatchers.IO) {
    val data = database.query()
    withContext(Dispatchers.Main) {
        updateUI(data)
    }
}

// CPU-intensive work
viewModelScope.launch(Dispatchers.Default) {
    val clusters = performClustering(events)
    withContext(Dispatchers.Main) {
        displayClusters(clusters)
    }
}
```

### Flow for Streams

**Use Flow for reactive streams:**
```kotlin
class ButtonRepository @Inject constructor(
    private val dao: ButtonDao
) {
    fun getAllButtons(): Flow<List<Button>> = dao.getAllButtonsFlow()

    fun getButtonById(id: Int): Flow<Button?> = dao.getButtonFlow(id)
}

// In ViewModel
val buttons: StateFlow<List<Button>> = repository.getAllButtons()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

---

## Data Classes and Objects

### Data Classes

**Use data classes for data models:**
```kotlin
data class Button(
    val id: Int = 0,
    val label: String,
    val emoji: String,
    val color: String,
    val displayOrder: Int,
    val createdAt: Long,
    val lastUsedAt: Long?
)
```

**Benefits:**
- Automatic `equals()`, `hashCode()`, `toString()`
- `copy()` method for immutable updates
- Destructuring declarations

**Using `copy()`:**
```kotlin
val updatedButton = button.copy(
    label = "Espresso",
    lastUsedAt = System.currentTimeMillis()
)
```

### Sealed Classes

**Use sealed classes for restricted hierarchies:**
```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<Button>) : UiState()
    data class Error(val message: String) : UiState()
}

// Usage with exhaustive when
fun render(state: UiState) = when (state) {
    is UiState.Loading -> showLoading()
    is UiState.Success -> showData(state.data)
    is UiState.Error -> showError(state.message)
}
```

### Object Declarations

**Use objects for singletons:**
```kotlin
object DatabaseConstants {
    const val DATABASE_NAME = "pixel_watch_tagger.db"
    const val DATABASE_VERSION = 1
}
```

---

## Extensions

### Extension Functions

**Use extensions to add functionality to existing classes:**
```kotlin
// Good - extends functionality
fun Long.toFormattedDate(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(this))
}

// Usage
val timestamp = System.currentTimeMillis()
val formatted = timestamp.toFormattedDate()
```

**Keep extensions focused and specific:**
```kotlin
// Good - specific purpose
fun List<Event>.groupByDay(): Map<LocalDate, List<Event>> {
    return groupBy { event ->
        Instant.ofEpochMilli(event.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
}

// Bad - too generic
fun List<Event>.process() { }  // What does this do?
```

### Extension Properties

```kotlin
val Button.isRecent: Boolean
    get() = lastUsedAt?.let {
        System.currentTimeMillis() - it < 24 * 60 * 60 * 1000
    } ?: false

// Usage
if (button.isRecent) {
    // ...
}
```

---

## Testing

### Test Naming

**Use descriptive test names in backticks:**
```kotlin
@Test
fun `button with valid data should be inserted successfully`() {
    // Arrange
    val button = Button(label = "Test", emoji = "⭐", color = "#FF0000", displayOrder = 0)

    // Act
    dao.insert(button)

    // Assert
    val retrieved = dao.getButtonById(button.id)
    assertEquals("Test", retrieved.label)
}
```

### Test Structure

**Follow Arrange-Act-Assert pattern:**
```kotlin
@Test
fun `clustering should identify morning pattern`() {
    // Arrange
    val events = listOf(
        Event(timestamp = toMinutes(8, 15)),
        Event(timestamp = toMinutes(8, 20)),
        Event(timestamp = toMinutes(8, 10))
    )

    // Act
    val clusters = clusteringService.performClustering(events)

    // Assert
    assertEquals(1, clusters.size)
    assertTrue(clusters[0].centroidMinutes in 480..510)
}
```

### Mock and Stub

**Use dependency injection for testability:**
```kotlin
class MainViewModel @Inject constructor(
    private val repository: ButtonRepository,  // Can be mocked
    private val predictionService: PredictionService  // Can be mocked
) : ViewModel() {
    // ...
}

// In test
@Test
fun `loading buttons should update state`() = runTest {
    // Arrange
    val mockRepository = mock<ButtonRepository> {
        on { getAllButtons() } doReturn flowOf(listOf(testButton))
    }
    val viewModel = MainViewModel(mockRepository, mockPredictionService)

    // Act
    viewModel.loadButtons()

    // Assert
    assertEquals(listOf(testButton), viewModel.buttons.value)
}
```

---

## Common Patterns

### Repository Pattern

```kotlin
interface ButtonRepository {
    fun getAllButtons(): Flow<List<Button>>
    suspend fun getButtonById(id: Int): Button?
    suspend fun insert(button: Button): Long
    suspend fun update(button: Button)
    suspend fun delete(button: Button)
}

class ButtonRepositoryImpl @Inject constructor(
    private val dao: ButtonDao
) : ButtonRepository {
    override fun getAllButtons(): Flow<List<Button>> = dao.getAllButtonsFlow()
    override suspend fun getButtonById(id: Int): Button? = dao.getButtonById(id)
    override suspend fun insert(button: Button): Long = dao.insert(button)
    override suspend fun update(button: Button) = dao.update(button)
    override suspend fun delete(button: Button) = dao.delete(button)
}
```

### ViewModel with State

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ButtonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadButtons()
    }

    private fun loadButtons() {
        viewModelScope.launch {
            repository.getAllButtons()
                .catch { e ->
                    _uiState.value = UiState.Error(e.message ?: "Unknown error")
                }
                .collect { buttons ->
                    _uiState.value = UiState.Success(buttons)
                }
        }
    }
}
```

---

## Documentation

### KDoc Comments

**Document public APIs:**
```kotlin
/**
 * Calculates button positions and sizes for circular layout.
 *
 * @param buttons List of buttons ordered by display_order
 * @param screenSize Screen dimensions (width x height)
 * @return List of ButtonPosition with x, y coordinates and size
 * @throws IllegalArgumentException if buttons list exceeds maximum count
 */
fun calculatePositions(
    buttons: List<Button>,
    screenSize: Size
): List<ButtonPosition> {
    // ...
}
```

**Link to related functions:**
```kotlin
/**
 * Records a new event for the specified button.
 *
 * @see Button
 * @see Event
 */
suspend fun recordEvent(buttonId: Int, timestamp: Long)
```

---

## References

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Kotlin Best Practices 2025](https://medium.com/@hiren6997/kotlin-best-practices-every-android-developer-should-know-in-2025-0888ba82a416)

---

**Document Version:** 1.0
**Last Updated:** 2025-01-30
