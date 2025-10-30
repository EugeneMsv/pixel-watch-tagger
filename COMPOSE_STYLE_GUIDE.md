# Jetpack Compose Best Practices Guide

This document defines the Jetpack Compose coding standards and best practices for the Pixel Watch Tagger project, based on official Android recommendations and industry standards for 2025.

## Table of Contents

- [Composition](#composition)
- [State Management](#state-management)
- [Performance](#performance)
- [Wear OS Specifics](#wear-os-specifics)
- [Architecture Patterns](#architecture-patterns)
- [Theming and Styling](#theming-and-styling)
- [Testing](#testing)
- [Common Patterns](#common-patterns)

---

## Composition

### Composable Function Naming

**Use PascalCase for Composable functions:**
```kotlin
// Good
@Composable
fun MainScreen() { }

@Composable
fun CircularButtonLayout() { }

// Bad
@Composable
fun mainScreen() { }  // Should be PascalCase
```

### Stateless vs Stateful Composables

**Separate stateless and stateful composables:**

```kotlin
// Stateful - manages its own state
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MainScreenContent(
        uiState = uiState,
        onButtonClick = viewModel::onButtonClick
    )
}

// Stateless - receives all data via parameters
@Composable
fun MainScreenContent(
    uiState: UiState,
    onButtonClick: (Int) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Success -> ButtonGrid(uiState.buttons, onButtonClick)
        is UiState.Error -> ErrorMessage(uiState.message)
    }
}
```

**Benefits:**
- Easier to test (stateless composables)
- Better reusability
- Clear separation of concerns

### State Hoisting

**Hoist state to the lowest common ancestor:**

```kotlin
// Good - state hoisted to parent
@Composable
fun ButtonSettingsScreen() {
    var label by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("⭐") }

    ButtonSettingsForm(
        label = label,
        onLabelChange = { label = it },
        emoji = emoji,
        onEmojiChange = { emoji = it }
    )
}

@Composable
fun ButtonSettingsForm(
    label: String,
    onLabelChange: (String) -> Unit,
    emoji: String,
    onEmojiChange: (String) -> Unit
) {
    Column {
        TextField(value = label, onValueChange = onLabelChange)
        EmojiPicker(selected = emoji, onSelect = onEmojiChange)
    }
}

// Bad - state trapped in child
@Composable
fun ButtonSettingsForm() {
    var label by remember { mutableStateOf("") }  // Can't be accessed by parent
    TextField(value = label, onValueChange = { label = it })
}
```

### Minimize Calculations in Composables

**Cache expensive computations with `remember`:**

```kotlin
// Good - calculation cached
@Composable
fun CircularButtonLayout(buttons: List<Button>) {
    val positions = remember(buttons) {
        calculateButtonPositions(buttons)
    }

    buttons.forEachIndexed { index, button ->
        CircleButton(
            button = button,
            position = positions[index]
        )
    }
}

// Bad - recalculated on every recomposition
@Composable
fun CircularButtonLayout(buttons: List<Button>) {
    buttons.forEachIndexed { index, button ->
        val position = calculateButtonPositions(buttons)[index]  // Recalculates every time
        CircleButton(button = button, position = position)
    }
}
```

### Avoid Backwards Writes

**Never write to state during composition:**

```kotlin
// Bad - writing during composition
@Composable
fun BadCounter() {
    var count by remember { mutableStateOf(0) }
    count++  // WRONG! Side effect during composition
    Text("Count: $count")
}

// Good - write in response to event
@Composable
fun GoodCounter() {
    var count by remember { mutableStateOf(0) }
    Column {
        Text("Count: $count")
        Button(onClick = { count++ }) {  // Correct - in lambda
            Text("Increment")
        }
    }
}

// Good - use LaunchedEffect for side effects
@Composable
fun GoodCounter() {
    var count by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Side effects here
        delay(1000)
        count++
    }

    Text("Count: $count")
}
```

---

## State Management

### Remember vs RememberSaveable

**Use `rememberSaveable` for configuration changes:**

```kotlin
// Use remember for normal state
@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }  // Lost on config change
    TextField(value = query, onValueChange = { query = it })
}

// Use rememberSaveable to survive config changes
@Composable
fun SearchScreen() {
    var query by rememberSaveable { mutableStateOf("") }  // Survives rotation
    TextField(value = query, onValueChange = { query = it })
}
```

### DerivedStateOf

**Use `derivedStateOf` for computed state:**

```kotlin
// Good - only recomputes when buttons change
@Composable
fun ButtonStats(buttons: List<Button>) {
    val activeButtonCount by remember {
        derivedStateOf {
            buttons.count { it.isActive }
        }
    }

    Text("Active buttons: $activeButtonCount")
}

// Bad - creates new state on every recomposition
@Composable
fun ButtonStats(buttons: List<Button>) {
    val activeButtonCount = buttons.count { it.isActive }  // Recalculated always
    Text("Active buttons: $activeButtonCount")
}
```

### StateFlow and State

**Collect StateFlow properly:**

```kotlin
// Good - collect as State
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val buttons by viewModel.buttons.collectAsState()

    ButtonList(buttons = buttons)
}

// Good - collect with lifecycle awareness
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val buttons by viewModel.buttons.collectAsStateWithLifecycle()

    ButtonList(buttons = buttons)
}
```

### Immutable Collections

**Use immutable collections for better performance:**

```kotlin
// Good - immutable list
data class UiState(
    val buttons: List<Button> = emptyList(),  // Immutable
    val isLoading: Boolean = false
)

// Bad - mutable list causes unnecessary recompositions
data class UiState(
    val buttons: MutableList<Button> = mutableListOf(),  // Unstable type
    val isLoading: Boolean = false
)
```

---

## Performance

### Stable Types

**Ensure types are stable to minimize recompositions:**

```kotlin
// Good - all properties are val and types are stable
@Immutable
data class ButtonData(
    val id: Int,
    val label: String,
    val emoji: String,
    val color: String
)

// Bad - var properties make it unstable
data class ButtonData(
    var id: Int,  // Mutable
    var label: String  // Mutable
)

// Good - mark as @Immutable if truly immutable
@Immutable
data class ButtonPosition(
    val x: Float,
    val y: Float,
    val size: Float
)
```

### Skip Unnecessary Recompositions

**Use keys for lists:**

```kotlin
// Good - uses stable key
@Composable
fun ButtonList(buttons: List<Button>) {
    LazyColumn {
        items(
            items = buttons,
            key = { button -> button.id }  // Stable key
        ) { button ->
            ButtonItem(button)
        }
    }
}

// Bad - no key, full recomposition on list change
@Composable
fun ButtonList(buttons: List<Button>) {
    LazyColumn {
        items(buttons) { button ->  // No key
            ButtonItem(button)
        }
    }
}
```

### Defer State Reads

**Read state as late as possible:**

```kotlin
// Good - reads state only in Text
@Composable
fun Counter(count: Int) {
    Column {
        Text("Static header")
        Text("Count: $count")  // Only this recomposes when count changes
        Text("Static footer")
    }
}

// Better - defer read with lambda
@Composable
fun Counter(count: () -> Int) {
    Column {
        Text("Static header")
        Text("Count: ${count()}")  // Read deferred
        Text("Static footer")
    }
}
```

### Avoid Expensive Operations

**Move heavy calculations out of composition:**

```kotlin
// Bad - clustering runs during composition
@Composable
fun ClusterVisualization(events: List<Event>) {
    val clusters = performClustering(events)  // Expensive!
    ClusterChart(clusters)
}

// Good - calculate in ViewModel
class StatsViewModel : ViewModel() {
    val clusters: StateFlow<List<Cluster>> = eventsFlow
        .map { events -> performClustering(events) }  // Background
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@Composable
fun ClusterVisualization(viewModel: StatsViewModel = hiltViewModel()) {
    val clusters by viewModel.clusters.collectAsState()
    ClusterChart(clusters)
}

// Good - use LaunchedEffect for async work
@Composable
fun ClusterVisualization(events: List<Event>) {
    var clusters by remember { mutableStateOf<List<Cluster>>(emptyList()) }

    LaunchedEffect(events) {
        clusters = withContext(Dispatchers.Default) {
            performClustering(events)
        }
    }

    ClusterChart(clusters)
}
```

---

## Wear OS Specifics

### Use Wear Compose Libraries

**Always use Wear-specific components:**

```kotlin
// Good - Wear OS components
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun WearButton() {
    Button(onClick = { }) {  // Wear OS Button
        Text("Tap me")  // Wear OS Text
    }
}

// Bad - regular Android components
import androidx.compose.material3.Button  // Mobile version
import androidx.compose.material3.Text   // Mobile version

@Composable
fun MobileButton() {
    Button(onClick = { }) {  // Wrong library!
        Text("Tap me")
    }
}
```

### Scrollable Lists

**Use Wear-specific lazy lists:**

```kotlin
// Good - ScalingLazyColumn for Wear OS
import androidx.wear.compose.material3.ScalingLazyColumn

@Composable
fun WearList(items: List<Item>) {
    ScalingLazyColumn {
        items(items) { item ->
            ListItem(item)
        }
    }
}

// Even better - with rotary support
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.rotaryScrollable

@Composable
fun WearListWithRotary(items: List<Item>) {
    val scrollState = rememberScalingLazyListState()
    val focusRequester = rememberActiveFocusRequester()

    ScalingLazyColumn(
        state = scrollState,
        modifier = Modifier
            .rotaryScrollable(
                behavior = ScalingLazyColumnRotaryBehavior(scrollState),
                focusRequester = focusRequester
            )
            .focusRequester(focusRequester)
            .focusable()
    ) {
        items(items) { item ->
            ListItem(item)
        }
    }
}
```

### SwipeDismissableNavHost

**Use swipe-to-dismiss navigation:**

```kotlin
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToStats = { navController.navigate("stats") }
            )
        }
        composable("stats") {
            StatsScreen()  // Swipe right to go back
        }
    }
}
```

### Round Screen Support

**Design for circular displays:**

```kotlin
@Composable
fun CircularLayout() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isRound = LocalConfiguration.current.isScreenRound

        if (isRound) {
            // Adjust layout for round screen
            CircularButtonGrid()
        } else {
            // Fallback for square screens
            StandardButtonGrid()
        }
    }
}
```

---

## Architecture Patterns

### Unidirectional Data Flow

**Follow UDF pattern:**

```kotlin
// ViewModel - single source of truth
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ButtonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Events come in
    fun onButtonClick(buttonId: Int) {
        viewModelScope.launch {
            repository.recordEvent(buttonId)
            loadButtons()  // Update state
        }
    }

    private fun loadButtons() {
        viewModelScope.launch {
            _uiState.value = UiState.Success(repository.getButtons())
        }
    }
}

// UI - observes state, sends events
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    MainScreenContent(
        uiState = uiState,
        onButtonClick = viewModel::onButtonClick  // Events up
    )
}
```

### UI State Classes

**Use sealed classes for UI state:**

```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(
        val buttons: List<Button>,
        val predictions: Map<Int, Prediction>
    ) : UiState()
    data class Error(val message: String) : UiState()
}

// In composable
@Composable
fun MainScreenContent(uiState: UiState) {
    when (uiState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Success -> {
            ButtonGrid(
                buttons = uiState.buttons,
                predictions = uiState.predictions
            )
        }
        is UiState.Error -> {
            ErrorScreen(message = uiState.message)
        }
    }
}
```

### Side Effects

**Use appropriate side effect APIs:**

```kotlin
// LaunchedEffect - triggered when key changes
@Composable
fun TimerScreen(buttonId: Int) {
    var secondsElapsed by remember { mutableStateOf(0) }

    LaunchedEffect(buttonId) {  // Restarts when buttonId changes
        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    Text("Elapsed: $secondsElapsed seconds")
}

// DisposableEffect - cleanup when leaving composition
@Composable
fun SensorScreen() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val listener = registerSensorListener(context)

        onDispose {
            unregisterSensorListener(listener)  // Cleanup
        }
    }
}

// SideEffect - sync Compose state to external state
@Composable
fun AnalyticsScreen(screen: String) {
    SideEffect {
        analytics.logScreenView(screen)  // Called on every successful composition
    }
}

// rememberCoroutineScope - launch coroutines from event handlers
@Composable
fun SaveButton(onSave: suspend () -> Unit) {
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                onSave()  // Suspend function
            }
        }
    ) {
        Text("Save")
    }
}
```

---

## Theming and Styling

### Material Theme

**Use Material Theme for consistency:**

```kotlin
@Composable
fun App() {
    MaterialTheme {
        // All content inherits theme
        MainScreen()
    }
}

// Access theme values
@Composable
fun ThemedButton() {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = "Button",
            style = MaterialTheme.typography.labelLarge
        )
    }
}
```

### Custom Theme

**Define custom theme for app:**

```kotlin
// Define colors
private val WatchColorScheme = darkColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    background = Color.Black,
    surface = Color(0xFF121212),
    error = Color(0xFFCF6679)
)

// Theme composable
@Composable
fun PixelWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WatchColorScheme,
        typography = WearTypography,
        content = content
    )
}

// Usage
@Composable
fun App() {
    PixelWatchTheme {
        MainScreen()
    }
}
```

### Modifiers

**Apply modifiers in consistent order:**

```kotlin
// Recommended order:
// 1. Size and layout
// 2. Padding/spacing
// 3. Border/background
// 4. Interaction (clickable, etc.)
// 5. Semantics

@Composable
fun StyledButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()           // 1. Size
            .height(48.dp)
            .padding(horizontal = 16.dp)  // 2. Padding
            .background(Color.Blue)   // 3. Background
            .border(2.dp, Color.White) // 3. Border
            .clickable { }            // 4. Interaction
            .semantics { role = Role.Button }  // 5. Semantics
    )
}
```

---

## Testing

### Preview Functions

**Create previews for all composables:**

```kotlin
@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun MainScreenPreview() {
    PixelWatchTheme {
        MainScreenContent(
            uiState = UiState.Success(
                buttons = listOf(
                    Button(id = 1, label = "Coffee", emoji = "☕", color = "#795548"),
                    Button(id = 2, label = "Water", emoji = "💧", color = "#2196F3")
                ),
                predictions = emptyMap()
            ),
            onButtonClick = { }
        )
    }
}

// Preview different states
@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun MainScreenLoadingPreview() {
    PixelWatchTheme {
        MainScreenContent(
            uiState = UiState.Loading,
            onButtonClick = { }
        )
    }
}
```

### Compose Testing

**Test composables with ComposeTestRule:**

```kotlin
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_showsButtons() {
        // Arrange
        val buttons = listOf(
            Button(id = 1, label = "Coffee", emoji = "☕", color = "#795548")
        )

        // Act
        composeTestRule.setContent {
            MainScreenContent(
                uiState = UiState.Success(buttons, emptyMap()),
                onButtonClick = { }
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Coffee").assertExists()
    }

    @Test
    fun buttonClick_triggersCallback() {
        var clickedButtonId: Int? = null

        composeTestRule.setContent {
            MainScreenContent(
                uiState = UiState.Success(testButtons, emptyMap()),
                onButtonClick = { clickedButtonId = it }
            )
        }

        composeTestRule.onNodeWithText("Coffee").performClick()

        assertEquals(1, clickedButtonId)
    }
}
```

---

## Common Patterns

### Slot Pattern

**Use slot pattern for flexible composables:**

```kotlin
// Composable with slots
@Composable
fun Card(
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    actions: @Composable () -> Unit
) {
    Column {
        Box(modifier = Modifier.padding(16.dp)) {
            title()
        }
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
        Row(modifier = Modifier.padding(8.dp)) {
            actions()
        }
    }
}

// Usage with different content
@Composable
fun ButtonCard(button: Button) {
    Card(
        title = {
            Text(button.label, style = MaterialTheme.typography.titleLarge)
        },
        content = {
            Text("${button.emoji} ${button.color}")
        },
        actions = {
            Button(onClick = { }) { Text("Edit") }
            Button(onClick = { }) { Text("Delete") }
        }
    )
}
```

### CompositionLocal

**Use CompositionLocal for implicit dependencies:**

```kotlin
// Define local
val LocalButtonRepository = staticCompositionLocalOf<ButtonRepository> {
    error("No ButtonRepository provided")
}

// Provide at top level
@Composable
fun App(repository: ButtonRepository) {
    CompositionLocalProvider(LocalButtonRepository provides repository) {
        MainScreen()
    }
}

// Access anywhere in tree
@Composable
fun ButtonList() {
    val repository = LocalButtonRepository.current
    val buttons by repository.getAllButtons().collectAsState(initial = emptyList())

    LazyColumn {
        items(buttons) { button ->
            ButtonItem(button)
        }
    }
}
```

### Loading and Error States

**Consistent loading/error handling:**

```kotlin
@Composable
fun <T> AsyncContent(
    state: AsyncState<T>,
    onRetry: () -> Unit = { },
    content: @Composable (T) -> Unit
) {
    when (state) {
        is AsyncState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is AsyncState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error: ${state.message}")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
        is AsyncState.Success -> {
            content(state.data)
        }
    }
}

// Usage
@Composable
fun ButtonScreen(viewModel: ButtonViewModel = hiltViewModel()) {
    val state by viewModel.buttonsState.collectAsState()

    AsyncContent(
        state = state,
        onRetry = viewModel::loadButtons
    ) { buttons ->
        ButtonList(buttons)
    }
}
```

---

## Code Quality Checklist

Before submitting code, ensure:

- [ ] Composables are properly split (stateless/stateful)
- [ ] State is hoisted appropriately
- [ ] Expensive calculations are cached with `remember`
- [ ] No backwards writes (state modified during composition)
- [ ] Lists use stable keys
- [ ] Wear OS-specific components are used
- [ ] All composables have `@Preview` functions
- [ ] UI tests cover critical paths
- [ ] Modifiers are applied in consistent order
- [ ] No side effects during composition
- [ ] Immutable data classes for UI state

---

## References

- [Jetpack Compose Best Practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [Compose for Wear OS](https://developer.android.com/training/wearables/compose)
- [Compose Performance](https://developer.android.com/develop/ui/compose/performance)
- [Compose API Guidelines](https://developer.android.com/develop/ui/compose/api-guidelines)

---

**Document Version:** 1.0
**Last Updated:** 2025-01-30
