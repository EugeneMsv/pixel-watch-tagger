# Architecture Guidelines

This document outlines the architecture patterns, project structure, and design principles for the Pixel Watch Tagger application.

## Table of Contents

- [Architecture Pattern](#architecture-pattern)
- [Project Structure](#project-structure)
- [Layer Responsibilities](#layer-responsibilities)
- [Dependency Injection](#dependency-injection)
- [Data Flow](#data-flow)
- [Navigation](#navigation)
- [Error Handling](#error-handling)

---

## Architecture Pattern

### MVVM (Model-View-ViewModel)

The app follows the **MVVM (Model-View-ViewModel)** architecture pattern, recommended by Google for Android applications.

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  (Jetpack Compose + ViewModels + UI State)      │
└──────────────┬──────────────────────────────────┘
               │ observes state
               │ sends events
               ▼
┌─────────────────────────────────────────────────┐
│                 Domain Layer                     │
│      (Use Cases + Business Logic + Models)      │
└──────────────┬──────────────────────────────────┘
               │ calls
               ▼
┌─────────────────────────────────────────────────┐
│                  Data Layer                      │
│     (Repositories + Data Sources + DAOs)         │
└─────────────────────────────────────────────────┘
```

**Benefits:**
- Clear separation of concerns
- Easier to test each layer independently
- Reactive UI updates with StateFlow/Flow
- Configuration change survival
- Business logic decoupled from UI

---

## Project Structure

### Package Organization

```
com.example.pixelwatchtagger/
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt           # Room database
│   │   ├── ButtonDao.kt             # Button data access
│   │   ├── EventDao.kt              # Event data access
│   │   └── entities/
│   │       ├── Button.kt            # Button entity
│   │       └── Event.kt             # Event entity
│   └── repository/
│       ├── ButtonRepository.kt      # Button data repository
│       └── EventRepository.kt       # Event data repository
│
├── domain/
│   ├── clustering/
│   │   ├── ClusteringService.kt     # Clustering business logic
│   │   ├── DBSCANAlgorithm.kt       # DBSCAN implementation
│   │   └── models/
│   │       └── Cluster.kt           # Cluster model
│   └── prediction/
│       ├── PredictionService.kt     # Prediction business logic
│       └── models/
│           └── Prediction.kt        # Prediction model
│
├── ui/
│   ├── main/
│   │   ├── MainScreen.kt            # Main screen composable
│   │   ├── MainViewModel.kt         # Main screen view model
│   │   ├── MainUiState.kt           # Main screen UI state
│   │   └── components/
│   │       ├── CircularButtonLayout.kt
│   │       └── ButtonLayoutCalculator.kt
│   ├── edit/
│   │   ├── EditModeScreen.kt
│   │   ├── ButtonSettingsScreen.kt
│   │   ├── AddButtonScreen.kt
│   │   └── EditViewModel.kt
│   ├── stats/
│   │   ├── StatisticsScreen.kt
│   │   ├── StatsViewModel.kt
│   │   └── components/
│   │       └── ScatterPlotChart.kt
│   ├── components/
│   │   ├── EmojiPicker.kt
│   │   ├── ColorPicker.kt
│   │   └── LoadingIndicator.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── workers/
│   ├── DataCleanupWorker.kt         # Background cleanup
│   └── PredictionWorker.kt          # Background prediction
│
├── di/
│   ├── DatabaseModule.kt            # Database DI module
│   ├── RepositoryModule.kt          # Repository DI module
│   └── AppModule.kt                 # App-level DI module
│
├── util/
│   ├── DateUtils.kt
│   ├── Constants.kt
│   └── Extensions.kt
│
└── MainActivity.kt                  # App entry point
```

---

## Layer Responsibilities

### 1. UI Layer

**Responsibilities:**
- Display data to user
- Handle user interactions
- Observe ViewModel state
- Manage Compose UI lifecycle

**Components:**
- **Composables**: UI components (stateless preferred)
- **ViewModels**: Manage UI state and handle user events
- **UI State**: Immutable data classes representing screen state

**Example:**

```kotlin
// UI State
sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(
        val buttons: List<Button>,
        val predictions: Map<Int, Prediction>,
        val mode: AppMode
    ) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

// ViewModel
@HiltViewModel
class MainViewModel @Inject constructor(
    private val buttonRepository: ButtonRepository,
    private val predictionService: PredictionService
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onButtonClick(buttonId: Int) {
        viewModelScope.launch {
            // Handle event
        }
    }
}

// Composable
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    MainScreenContent(
        uiState = uiState,
        onButtonClick = viewModel::onButtonClick
    )
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onButtonClick: (Int) -> Unit
) {
    when (uiState) {
        is MainUiState.Loading -> LoadingIndicator()
        is MainUiState.Success -> ButtonGrid(uiState.buttons, onButtonClick)
        is MainUiState.Error -> ErrorMessage(uiState.message)
    }
}
```

### 2. Domain Layer

**Responsibilities:**
- Business logic
- Data transformation
- Algorithm implementation
- Use cases

**Components:**
- **Services**: Business logic (ClusteringService, PredictionService)
- **Models**: Domain-specific data classes
- **Use Cases**: Single-purpose business operations (optional for simple apps)

**Example:**

```kotlin
interface PredictionService {
    suspend fun calculatePrediction(
        events: List<Event>,
        currentTime: Long
    ): Prediction?
}

class PredictionServiceImpl @Inject constructor(
    private val clusteringService: ClusteringService
) : PredictionService {

    override suspend fun calculatePrediction(
        events: List<Event>,
        currentTime: Long
    ): Prediction? = withContext(Dispatchers.Default) {
        if (events.size < 7) return@withContext null

        val clusters = clusteringService.performClustering(events)
        val nextCluster = findNextCluster(clusters, currentTime)

        nextCluster?.let {
            Prediction(
                targetTime = calculateTargetTime(it, currentTime),
                confidence = it.confidence
            )
        }
    }

    private fun findNextCluster(clusters: List<Cluster>, currentTime: Long): Cluster? {
        // Implementation
    }
}
```

### 3. Data Layer

**Responsibilities:**
- Data access and storage
- Database operations
- External API calls (if any)
- Caching

**Components:**
- **Repositories**: Abstract data sources, provide clean API
- **DAOs**: Room database access objects
- **Entities**: Database models
- **Data Sources**: Local (Room) or Remote (API)

**Example:**

```kotlin
// Repository Interface
interface ButtonRepository {
    fun getAllButtons(): Flow<List<Button>>
    suspend fun getButtonById(id: Int): Button?
    suspend fun insert(button: Button): Long
    suspend fun update(button: Button)
    suspend fun delete(button: Button)
}

// Repository Implementation
class ButtonRepositoryImpl @Inject constructor(
    private val buttonDao: ButtonDao
) : ButtonRepository {

    override fun getAllButtons(): Flow<List<Button>> =
        buttonDao.getAllButtonsFlow()

    override suspend fun getButtonById(id: Int): Button? =
        buttonDao.getButtonById(id)

    override suspend fun insert(button: Button): Long =
        buttonDao.insert(button)

    override suspend fun update(button: Button) =
        buttonDao.update(button)

    override suspend fun delete(button: Button) =
        buttonDao.delete(button)
}

// DAO
@Dao
interface ButtonDao {
    @Query("SELECT * FROM button ORDER BY display_order ASC")
    fun getAllButtonsFlow(): Flow<List<Button>>

    @Query("SELECT * FROM button WHERE id = :id")
    suspend fun getButtonById(id: Int): Button?

    @Insert
    suspend fun insert(button: Button): Long

    @Update
    suspend fun update(button: Button)

    @Delete
    suspend fun delete(button: Button)
}
```

---

## Dependency Injection

### Hilt for Dependency Injection

Use **Hilt** (built on Dagger) for dependency injection throughout the app.

**Benefits:**
- Compile-time safety
- Automatic ViewModel injection
- Scoped dependencies
- Easy testing with test modules

### Module Structure

```kotlin
// Database Module
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pixel_watch_tagger.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideButtonDao(database: AppDatabase): ButtonDao {
        return database.buttonDao()
    }

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }
}

// Repository Module
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindButtonRepository(
        impl: ButtonRepositoryImpl
    ): ButtonRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        impl: EventRepositoryImpl
    ): EventRepository
}

// Application class
@HiltAndroidApp
class PixelWatchTaggerApp : Application()
```

---

## Data Flow

### Unidirectional Data Flow (UDF)

```
┌──────────────┐
│   UI Event   │  User clicks button
└──────┬───────┘
       │
       ▼
┌─────────────────┐
│   ViewModel     │  Handles event, updates state
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│   Repository    │  Fetches/updates data
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│   UI State      │  New state emitted
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│   Composable    │  Recomposes with new state
└─────────────────┘
```

**Example Flow:**

```kotlin
// 1. User clicks button
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Button(
        onClick = { viewModel.onButtonClick(buttonId = 1) }  // Event
    ) {
        Text("Record Event")
    }
}

// 2. ViewModel handles event
@HiltViewModel
class MainViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    fun onButtonClick(buttonId: Int) {
        viewModelScope.launch {
            try {
                // 3. Repository updates data
                eventRepository.recordEvent(buttonId)

                // 4. Update UI state
                _uiState.value = MainUiState.Success(...)
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// 5. UI recomposes with new state
```

### StateFlow vs Flow

**StateFlow:**
- Hot stream (always active)
- Always has a current value
- State holder
- Use for: UI state, configuration

**Flow:**
- Cold stream (activated on collection)
- No initial value
- Event stream
- Use for: Database queries, one-time operations

```kotlin
// StateFlow for UI state
val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

// Flow for database queries
fun getAllButtons(): Flow<List<Button>> = buttonDao.getAllButtonsFlow()

// Convert Flow to StateFlow in ViewModel
val buttons: StateFlow<List<Button>> = buttonRepository
    .getAllButtons()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

---

## Navigation

### Wear OS Navigation

Use **SwipeDismissableNavHost** for Wear OS navigation with swipe-to-dismiss support:

```kotlin
@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToStats = { buttonId ->
                    navController.navigate("stats/$buttonId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "stats/{buttonId}",
            arguments = listOf(navArgument("buttonId") { type = NavType.IntType })
        ) { backStackEntry ->
            val buttonId = backStackEntry.arguments?.getInt("buttonId")
            StatsScreen(buttonId = buttonId)
        }

        composable("settings") {
            SettingsScreen()
        }
    }
}
```

### Navigation Best Practices

1. **Pass only IDs, not full objects**
   ```kotlin
   // Good
   navController.navigate("stats/$buttonId")

   // Bad
   navController.navigate("stats/${button.toJson()}")
   ```

2. **Use type-safe navigation (Navigation Compose)**
   ```kotlin
   sealed class Screen(val route: String) {
       object Main : Screen("main")
       data class Stats(val buttonId: Int) : Screen("stats/{buttonId}")
   }
   ```

3. **Handle back navigation properly**
   ```kotlin
   Button(onClick = { navController.navigateUp() }) {
       Text("Back")
   }
   ```

---

## Error Handling

### Repository Layer

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}

class ButtonRepositoryImpl @Inject constructor(
    private val buttonDao: ButtonDao
) : ButtonRepository {

    override suspend fun insert(button: Button): Result<Long> {
        return try {
            val id = buttonDao.insert(button)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### ViewModel Layer

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ButtonRepository
) : ViewModel() {

    fun addButton(button: Button) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading

            when (val result = repository.insert(button)) {
                is Result.Success -> {
                    _uiState.value = MainUiState.Success(...)
                }
                is Result.Error -> {
                    _uiState.value = MainUiState.Error(
                        message = result.exception.message ?: "Failed to add button"
                    )
                }
            }
        }
    }
}
```

### UI Layer

```kotlin
@Composable
fun MainScreenContent(uiState: MainUiState) {
    when (uiState) {
        is MainUiState.Loading -> {
            LoadingIndicator()
        }
        is MainUiState.Success -> {
            ButtonGrid(uiState.buttons)
        }
        is MainUiState.Error -> {
            ErrorMessage(
                message = uiState.message,
                onRetry = { /* retry action */ }
            )
        }
    }
}
```

---

## Testing Strategy

### Unit Tests

**Test each layer independently:**

```kotlin
// Repository Test
class ButtonRepositoryTest {

    @Test
    fun `insert button returns success`() = runTest {
        val dao = FakeButtonDao()
        val repository = ButtonRepositoryImpl(dao)

        val result = repository.insert(testButton)

        assertTrue(result is Result.Success)
    }
}

// ViewModel Test
class MainViewModelTest {

    @Test
    fun `onButtonClick updates ui state to success`() = runTest {
        val repository = FakeButtonRepository()
        val viewModel = MainViewModel(repository)

        viewModel.onButtonClick(1)

        assertTrue(viewModel.uiState.value is MainUiState.Success)
    }
}
```

### Integration Tests

```kotlin
@HiltAndroidTest
class MainScreenIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun clickButton_recordsEvent() {
        composeTestRule.onNodeWithText("Coffee").performClick()

        // Verify event recorded in database
        composeTestRule.onNodeWithText("Event recorded").assertExists()
    }
}
```

---

## Performance Considerations

### Database Optimization

- Use indexed queries for frequent lookups
- Implement pagination for large datasets
- Use background threads for heavy queries

### Compose Performance

- Keep composables small and focused
- Use `remember` for expensive calculations
- Hoist state to appropriate level
- Use stable data classes

### Memory Management

- Cancel coroutines when ViewModel cleared
- Use `viewModelScope` for automatic cancellation
- Avoid memory leaks with proper lifecycle handling

---

## References

- [Guide to app architecture](https://developer.android.com/topic/architecture)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Wear OS UI Guidelines](https://developer.android.com/training/wearables/ui)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)

---

**Document Version:** 1.0
**Last Updated:** 2025-01-30
