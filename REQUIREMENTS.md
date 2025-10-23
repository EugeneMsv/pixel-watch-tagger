# Pixel Watch Tagger - Requirements Document

## 1. Application Overview

**Name**: Pixel Watch Tagger

**Purpose**: A Wear OS application for Google Pixel Watch 4 that tracks timestamped button presses across multiple configurable categories, analyzes patterns using clustering algorithms, and predicts future occurrences.

**Target Platform**: Wear OS 4+ (Google Pixel Watch 4)

**Technology Stack**:
- Kotlin
- Jetpack Compose for Wear OS
- Room Persistence Library
- DBSCAN/K-means clustering

---

## 2. Database Schema

### 2.1 Table: Button

Stores user-configured tracking categories.

```sql
CREATE TABLE button (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    label TEXT NOT NULL,
    emoji TEXT NOT NULL,
    color TEXT NOT NULL,  -- Hex color code (e.g., "#FF5722")
    position INTEGER NOT NULL,  -- 0-2 for inner circle, 3-8 for outer circle
    created_at INTEGER NOT NULL,  -- Unix timestamp
    last_used_at INTEGER  -- Unix timestamp of last event
);
```

**Fields:**
- `id`: Auto-incrementing primary key
- `label`: User-defined text label (e.g., "Coffee", "Medicine")
- `emoji`: Selected emoji character for visual identification
- `color`: Hex color code for button appearance
- `position`: Button position (0-2 inner circle, 3-8 outer circle)
- `created_at`: Timestamp when button was created
- `last_used_at`: Timestamp of most recent event (updated on each tap)

### 2.2 Table: Event

Stores individual timestamp records for button presses.

```sql
CREATE TABLE event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    button_id INTEGER NOT NULL,
    timestamp INTEGER NOT NULL,  -- Unix timestamp
    FOREIGN KEY (button_id) REFERENCES button(id) ON DELETE CASCADE
);

CREATE INDEX idx_event_button_id ON event(button_id);
CREATE INDEX idx_event_timestamp ON event(timestamp);
CREATE INDEX idx_event_button_timestamp ON event(button_id, timestamp);
```

**Fields:**
- `id`: Auto-incrementing primary key
- `button_id`: Foreign key reference to button table
- `timestamp`: Unix timestamp when button was pressed

**Indexes:**
- `idx_event_button_id`: Fast queries by button
- `idx_event_timestamp`: Fast queries by time range
- `idx_event_button_timestamp`: Composite index for button-specific time queries

**Cascade Delete:** When a button is deleted, all associated events are automatically deleted.

### 2.3 Room Database Entities (Kotlin)

```kotlin
@Entity(tableName = "button")
data class Button(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val emoji: String,
    val color: String,
    val position: Int,
    val createdAt: Long,
    val lastUsedAt: Long?
)

@Entity(
    tableName = "event",
    foreignKeys = [ForeignKey(
        entity = Button::class,
        parentColumns = ["id"],
        childColumns = ["button_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("button_id"), Index("timestamp")]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "button_id") val buttonId: Int,
    val timestamp: Long
)
```

### 2.4 Data Retention & Cleanup

**Retention Policy:**
- Events older than 30 days are automatically deleted
- Rolling 30-day window maintained

**Cleanup Implementation:**
- Background WorkManager task runs daily at midnight
- Query: `DELETE FROM event WHERE timestamp < (current_unix_time - 2592000)`
  - 2592000 seconds = 30 days
- No user intervention required

**Storage Estimates:**
- Per event record: ~20 bytes (after indexing overhead)
- Expected volume: 9 buttons × 10 events/day × 30 days = 2,700 events
- Total storage: ~54 KB for events + minimal overhead for buttons
- Very lightweight for watch storage

---

## 3. User Interface Design

### 3.1 Main Interface - Circular Button Layout

**Layout Overview:**

```
┌─────────────────────┐
│         ⚙️          │  ← Settings icon (top-right corner)
│                     │
│        ○            │  ← Inner circle: max 3 buttons
│      ○   ○          │     Positions: 0, 1, 2
│    ○       ○        │  ← Outer circle: max 6 buttons
│      ○   ○          │     Positions: 3, 4, 5, 6, 7, 8
│        ○            │
│                     │
│   Next: Coffee 2h   │  ← Optional: Shows nearest prediction
└─────────────────────┘
```

**Button Specifications:**
- **Equal size**: All buttons same diameter regardless of circle
- **Shape**: Circular
- **Appearance per button**:
  - Background: User-selected color
  - Icon: User-selected emoji (centered, large)
  - Overlay: Small countdown timer text showing time until next predicted event
    - Example: "2h 15m"
- **Spacing**: Evenly distributed on their respective circles
- **Visual feedback on tap**:
  - Pulse animation
  - Haptic vibration feedback
  - Brief color flash

**Circle Dimensions:**
- Inner circle radius: ~30% of screen width
- Outer circle radius: ~65% of screen width
- Button diameter: ~50-60px

**Position Calculation:**
- For position `N` in circle with `M` total positions:
  - Angle = `(N × 360° / M) - 90°`
  - X coordinate = `centerX + radius × cos(angle)`
  - Y coordinate = `centerY + radius × sin(angle)`

**Maximum Capacity:**
- Inner circle: 3 buttons (positions 0, 1, 2)
- Outer circle: 6 buttons (positions 3, 4, 5, 6, 7, 8)
- **Total maximum: 9 buttons**

**Interactions:**
- **Tap button**: Record timestamp event for that button
- **Tap ⚙️ icon**: Navigate to Settings mode

---

### 3.2 Settings Mode

**Navigation:**
- **Enter**: Tap ⚙️ icon on main screen
- **Exit**: Hardware back button or back gesture

#### 3.2.1 Button Management Screen

**Layout:**

```
┌─────────────────────┐
│  ← Button Manager   │
├─────────────────────┤
│  ☕ Coffee      [✏️] │  ← Tap row or ✏️ to edit
│  💊 Medicine    [✏️] │
│  🏃 Exercise    [✏️] │
│  🍕 Meal        [✏️] │
│                     │
│  [+ Add Button]     │  ← Shows if < 9 buttons
│                     │
│  [Statistics]       │  ← Navigate to stats screen
└─────────────────────┘
```

**Button List:**
- Shows all configured buttons
- Each row displays: emoji, label, edit icon
- Tap row or edit icon → Open edit form
- Scrollable list if many buttons

**Add Button Behavior:**
- If < 9 buttons: Button enabled, opens add form
- If = 9 buttons: Button disabled OR shows error dialog
  - Error message: "Maximum 9 buttons reached. Please delete an existing button first."

---

#### 3.2.2 Add/Edit Button Form

**Layout:**

```
┌─────────────────────┐
│  ← Edit Button      │
├─────────────────────┤
│  Label              │
│  [Coffee_________]  │  ← Text input field
│                     │
│  Emoji              │
│  [☕  Select...]    │  ← Opens emoji picker
│                     │
│  Color              │
│  [🟤  Select...]    │  ← Opens color picker
│                     │
│  Circle Preference  │
│  ⚪ Inner (3 max)   │  ← Radio buttons
│  ⚫ Outer (6 max)   │     (auto-assign position)
│                     │
│  [Delete Button]    │  ← Only shown when editing
│  [Save]             │
└─────────────────────┘
```

**Form Fields:**

1. **Label**
   - Text input, max 20 characters
   - Required field
   - Validation: Non-empty

2. **Emoji**
   - Opens system emoji picker
   - Default: ⭐ (if adding new button)
   - Single emoji selection

3. **Color**
   - Color picker dialog
   - Predefined palette: 12-16 common colors
   - Hex color stored (e.g., "#FF5722")

4. **Circle Preference**
   - Radio button selection: Inner or Outer
   - System auto-assigns first available position in selected circle
   - If selected circle is full:
     - Show warning: "Inner/Outer circle full. Choose other circle."
     - Disable Save until valid selection

5. **Delete Button** (Edit mode only)
   - Red destructive button
   - Shows confirmation dialog before deletion
   - Confirmation message:
     ```
     Delete "Coffee"?
     This will also delete all X associated events.
     [Cancel] [Delete]
     ```

**Position Auto-Assignment Logic:**
```
IF user selects "Inner":
    Find first available position in [0, 1, 2]
    IF all full:
        Show error, disable Save
ELSE IF user selects "Outer":
    Find first available position in [3, 4, 5, 6, 7, 8]
    IF all full:
        Show error, disable Save
```

---

#### 3.2.3 Statistics Chart Screen

**Layout:**

```
┌─────────────────────┐
│  ← Statistics       │
├─────────────────────┤
│  Button             │
│  [☕ Coffee     ▼]  │  ← Dropdown selector
│                     │
│  Time Range         │
│  [7] [14] [30]      │  ← Chip buttons (30 selected)
│                     │
│ ┌─────────────────┐ │
│ │   Scatter Plot  │ │
│ │                 │ │
│ │  • • •  • • •   │ │  ← Chart area
│ │    • •  •  •    │ │
│ │  • • •  • •     │ │
│ └─────────────────┘ │
│                     │
│  Detected Clusters  │
│  🔵 Morning (23)    │  ← Cluster info
│  🟢 Afternoon (18)  │
│  🟡 Evening (19)    │
│                     │
│  [Export CSV]       │
└─────────────────────┘
```

**Controls:**

1. **Button Selector**
   - Dropdown showing all configured buttons
   - Selected button's data is displayed

2. **Time Range Selector**
   - Three chip buttons: 7, 14, 30 days
   - Currently selected chip highlighted
   - Changes chart data range

3. **Export CSV Button**
   - Exports current button's events to CSV
   - Format: `timestamp,button_label,button_emoji`
   - Saved to paired phone or watch storage

**Chart Specifications:**

**Axes:**
- **X-axis**: Time of day (00:00 to 23:59)
  - Labels every 3-6 hours (00:00, 06:00, 12:00, 18:00, 24:00)
- **Y-axis**: Date
  - Most recent date at top
  - Oldest date at bottom
  - Show date labels on left side

**Data Visualization:**

1. **Event Points**
   - Small circles (4-6px diameter)
   - Color: Button's assigned color
   - Position: (time_of_day, date)
   - Semi-transparent to show density

2. **Cluster Overlays**
   - Each cluster gets unique color: 🔵🟢🟡🟠🔴
   - Semi-transparent filled region showing cluster boundaries
   - Cluster centroid: Larger filled circle
   - Visual distinction from raw event points

3. **Prediction Overlay**
   - Predicted next events shown as outlined circles
   - Dashed line connecting to cluster centroid
   - Label: "Predicted: 14:30"

**Chart Interactions:**
- **Pinch to zoom**: Zoom into time range or date range
- **Pan**: Swipe to scroll through dates or times
- **Tap data point**: Show tooltip with exact timestamp
- **Tap cluster**: Highlight cluster events, show stats

**Cluster Legend:**
```
Detected Clusters: 3

🔵 Cluster 1: Morning
   - Centroid: 08:15
   - Events: 23
   - Confidence: High

🟢 Cluster 2: Afternoon
   - Centroid: 14:45
   - Events: 18
   - Confidence: Medium

🟡 Cluster 3: Evening
   - Centroid: 20:30
   - Events: 19
   - Confidence: High
```

---

## 4. Prediction & Clustering System

### 4.1 Clustering Algorithm

**Objective:** Identify temporal patterns in button press events to predict future occurrences.

**Algorithm Choice:** DBSCAN (Density-Based Spatial Clustering) or K-means
- **DBSCAN**: Better for variable cluster counts
- **K-means**: Faster, requires predetermined K

**Input Data Preparation:**

1. Query events for specific button from last 30 days:
   ```sql
   SELECT timestamp FROM event
   WHERE button_id = ?
   AND timestamp >= (current_time - 30 days)
   ORDER BY timestamp
   ```

2. Convert timestamps to "minutes since midnight" (0-1439):
   ```
   time_of_day = (timestamp % 86400) / 60
   ```
   - Ignores date, focuses on time patterns

3. Create data points array: `[245, 248, 850, 852, ...]`

**Clustering Process:**

1. **Minimum data requirement**: At least 7 days × 1 event = 7 events
   - If insufficient data: No predictions generated

2. **Apply clustering algorithm**:
   - DBSCAN parameters:
     - `eps` (epsilon): 60 minutes (events within 1 hour considered same cluster)
     - `min_samples`: 3 (minimum events to form cluster)
   - K-means:
     - `K`: 2-5 (try multiple values, use silhouette score)

3. **Identify clusters**:
   - Label each event with cluster ID
   - Calculate cluster centroids (average time)
   - Compute cluster statistics (size, std deviation)

**Cluster Metadata:**

```kotlin
data class Cluster(
    val id: Int,
    val buttonId: Int,
    val centroidMinutes: Int,  // 0-1439 (minutes since midnight)
    val eventIds: List<Int>,
    val color: String,  // 🔵🟢🟡🟠🔴
    val standardDeviation: Float,
    val confidence: ConfidenceLevel  // HIGH, MEDIUM, LOW
)

enum class ConfidenceLevel {
    HIGH,    // Tight cluster, many events
    MEDIUM,  // Moderate spread
    LOW      // Sparse or few events
}
```

**Confidence Scoring:**

```kotlin
fun calculateConfidence(cluster: Cluster): ConfidenceLevel {
    val density = cluster.eventIds.size
    val spread = cluster.standardDeviation

    return when {
        density >= 10 && spread < 30 -> HIGH
        density >= 5 && spread < 60 -> MEDIUM
        else -> LOW
    }
}
```

### 4.2 Prediction Logic

**Objective:** Predict the next likely time a button will be pressed.

**Algorithm:**

1. **Get current time**: Current minutes since midnight

2. **Find next cluster**:
   ```kotlin
   fun getNextPrediction(currentMinutes: Int, clusters: List<Cluster>): Prediction? {
       // Find clusters after current time today
       val nextToday = clusters
           .filter { it.centroidMinutes > currentMinutes }
           .minByOrNull { it.centroidMinutes }

       if (nextToday != null) {
           return Prediction(
               targetTime = todayDate + nextToday.centroidMinutes,
               confidence = nextToday.confidence
           )
       }

       // No clusters left today, use first cluster tomorrow
       val firstCluster = clusters.minByOrNull { it.centroidMinutes }
       return firstCluster?.let {
           Prediction(
               targetTime = tomorrowDate + it.centroidMinutes,
               confidence = it.confidence
           )
       }
   }
   ```

3. **Calculate countdown**:
   ```kotlin
   val countdown = targetTime - currentTime
   val hours = countdown / 3600
   val minutes = (countdown % 3600) / 60
   return "${hours}h ${minutes}m"
   ```

**Display on Main Interface:**

Each button shows countdown timer:
- Text overlay on button: "2h 15m"
- Updates every minute
- Color-coded by confidence:
  - Green: High confidence
  - Yellow: Medium confidence
  - Gray: Low confidence or no prediction

**Prediction Recalculation Triggers:**

1. **On app launch**: Recalculate all predictions
2. **After button press**: Recalculate for that specific button
3. **Daily at midnight**: Background task recalculates all clusters and predictions
4. **On settings change**: If button deleted, remove predictions

### 4.3 Cluster Visualization on Chart

**Rendering Process:**

1. **Draw base scatter plot**: All events as small dots

2. **For each cluster**:
   - Assign color from palette: 🔵🟢🟡🟠🔴
   - Calculate cluster boundary (centroid ± 2 × std deviation)
   - Draw semi-transparent filled region
   - Draw cluster centroid as larger circle

3. **Draw prediction markers**:
   - For each predicted time (based on clusters)
   - Draw outlined circle at predicted position
   - Add label with time

**Example visualization:**

```
Time →
00:00  06:00  12:00  18:00  24:00
  │      │      │      │      │
  │      │  🔵  │      │      │  Day 1
  │      │ •••  │      │  🟡  │
  │      │  •   │      │ •••• │  Day 2
  │      │ •••  │      │ •••  │
  │      │  ••  │      │  •   │  Day 3
  ...

🔵 = Morning cluster (07:00-09:00)
🟡 = Evening cluster (19:00-21:00)
• = Individual events
```

---

## 5. User Flows

### 5.1 First Launch Experience

**Steps:**

1. App launches → Main interface displayed (empty)
2. Message overlay: "Tap ⚙️ to add your first button"
3. User taps settings icon
4. Settings screen opens → Button Management
5. User taps "+ Add Button"
6. Add button form opens
7. User fills in label, emoji, color, circle preference
8. User taps "Save"
9. Return to main interface → Button now visible on circle
10. User can tap button to record first event

### 5.2 Recording an Event

**Steps:**

1. User on main interface
2. User taps a button (e.g., "Coffee" button)
3. **Immediate feedback**:
   - Haptic vibration (short pulse)
   - Button pulse animation (brief scale up/down)
   - Optional: Brief success toast "Event recorded"
4. **Backend processing**:
   - Create new Event record with current timestamp
   - Update button's `last_used_at` field
   - If enough data (>7 events), recalculate predictions
5. **UI update**:
   - Countdown timer updates (if prediction changed)
6. User can immediately tap again or tap other buttons

**Expected frequency:** ~10 taps/button/day

### 5.3 Viewing Statistics

**Steps:**

1. User taps ⚙️ icon → Settings mode
2. Tap "Statistics" option
3. Statistics screen opens
4. **Default view**:
   - First button selected in dropdown
   - 30-day range selected
   - Chart renders with all events
5. **Interactions**:
   - Change button via dropdown → Chart updates
   - Change time range (7/14/30 days) → Chart re-queries and updates
   - Pinch/zoom on chart → Zoom into time range
   - Tap data point → Show tooltip with exact timestamp
6. **View clusters**:
   - Scroll down to cluster legend
   - Review detected patterns and confidence levels
7. **Export** (optional):
   - Tap "Export CSV" button
   - File saved to watch storage or sent to phone
   - Success message displayed

### 5.4 Managing Buttons

#### 5.4.1 Adding a New Button

**Steps:**

1. Settings → Button Management
2. Tap "+ Add Button"
3. Fill out form:
   - Label: "Meditation"
   - Emoji: 🧘 (from picker)
   - Color: Purple (from palette)
   - Circle: Inner (if space available)
4. Tap "Save"
5. **Validation**:
   - Check if selected circle has space
   - If full: Show error, prevent save
   - If valid: Assign position, save to database
6. Return to button list → New button visible
7. Navigate to main interface → Button visible on circle

**Edge case - 9 buttons exist:**

1. User taps "+ Add Button"
2. Error dialog appears:
   > "Maximum 9 buttons reached. Please delete an existing button first."
3. User must delete a button before adding new one

#### 5.4.2 Editing an Existing Button

**Steps:**

1. Settings → Button Management
2. Tap edit icon ✏️ on desired button
3. Edit form opens with current values pre-filled
4. User modifies label, emoji, or color
5. Tap "Save"
6. Changes saved to database
7. UI updates on main interface (button appearance changes)
8. Events remain unchanged (same button_id)

#### 5.4.3 Deleting a Button

**Steps:**

1. Settings → Button Management
2. Tap edit icon ✏️ on desired button
3. Scroll to "Delete Button" (red button)
4. Tap "Delete Button"
5. **Confirmation dialog**:
   ```
   Delete "Coffee"?
   This will also delete all 87 associated events.

   [Cancel] [Delete]
   ```
6. User taps "Delete"
7. **Backend processing**:
   - Delete button record
   - Cascade delete all events (via foreign key)
   - Recalculate predictions for remaining buttons
8. Return to button list → Button removed
9. Main interface updates → Button removed from circle

---

## 6. Data Export Functionality

### 6.1 CSV Export Format

**File naming:** `button-{label}-{date}.csv`
- Example: `button-coffee-20250122.csv`

**CSV structure:**

```csv
timestamp,unix_timestamp,button_label,button_emoji
2025-01-22 08:15:34,1737534934,Coffee,☕
2025-01-22 14:23:12,1737557192,Coffee,☕
2025-01-22 20:45:01,1737580501,Coffee,☕
...
```

**Fields:**
- `timestamp`: Human-readable datetime (ISO 8601)
- `unix_timestamp`: Unix epoch timestamp (for programmatic use)
- `button_label`: Button's label
- `button_emoji`: Button's emoji character

### 6.2 Export Process

**Trigger:** User taps "Export CSV" on Statistics screen

**Steps:**

1. Query events for selected button and time range:
   ```sql
   SELECT e.timestamp, b.label, b.emoji
   FROM event e
   JOIN button b ON e.button_id = b.id
   WHERE b.id = ?
   AND e.timestamp >= ?
   ORDER BY e.timestamp ASC
   ```

2. Generate CSV file in memory

3. **Save options**:
   - **Option A**: Save to watch internal storage
     - Path: `/sdcard/PixelWatchTagger/exports/`
   - **Option B**: Send to paired phone via Wear OS Data Layer API
   - **Option C**: Share via standard Android share sheet

4. Show success message: "Exported 87 events to coffee-20250122.csv"

---

## 7. Background Tasks & Services

### 7.1 Data Cleanup Worker

**Purpose:** Automatically delete events older than 30 days.

**Implementation:** WorkManager periodic task

**Schedule:** Daily at 00:00 (midnight)

**Logic:**

```kotlin
class DataCleanupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)

        val deletedCount = database.eventDao().deleteOlderThan(thirtyDaysAgo)

        Log.d("DataCleanup", "Deleted $deletedCount old events")

        return Result.success()
    }
}

// In DAO:
@Query("DELETE FROM event WHERE timestamp < :cutoffTime")
suspend fun deleteOlderThan(cutoffTime: Long): Int
```

**Constraints:**
- Device idle (not in use)
- Battery not low
- No network required

### 7.2 Prediction Recalculation Worker

**Purpose:** Recalculate clusters and predictions daily.

**Schedule:** Daily at 00:05 (shortly after cleanup)

**Logic:**

```kotlin
class PredictionWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val buttons = database.buttonDao().getAllButtons()

        buttons.forEach { button ->
            val events = database.eventDao().getEventsForButton(button.id, last30Days)

            if (events.size >= 7) {
                val clusters = clusteringService.performClustering(events)
                val prediction = predictionService.calculateNext(clusters)

                predictionCache.update(button.id, prediction)
            }
        }

        return Result.success()
    }
}
```

---

## 8. Technical Architecture

### 8.1 Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/pixelwatchtagger/
│   │   │   ├── data/
│   │   │   │   ├── database/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── ButtonDao.kt
│   │   │   │   │   ├── EventDao.kt
│   │   │   │   │   ├── entities/
│   │   │   │   │   │   ├── Button.kt
│   │   │   │   │   │   └── Event.kt
│   │   │   │   ├── repository/
│   │   │   │   │   ├── ButtonRepository.kt
│   │   │   │   │   └── EventRepository.kt
│   │   │   ├── domain/
│   │   │   │   ├── clustering/
│   │   │   │   │   ├── ClusteringService.kt
│   │   │   │   │   ├── DBSCANAlgorithm.kt
│   │   │   │   │   └── Cluster.kt
│   │   │   │   ├── prediction/
│   │   │   │   │   ├── PredictionService.kt
│   │   │   │   │   └── Prediction.kt
│   │   │   ├── ui/
│   │   │   │   ├── main/
│   │   │   │   │   ├── MainScreen.kt
│   │   │   │   │   ├── CircularButtonLayout.kt
│   │   │   │   │   └── MainViewModel.kt
│   │   │   │   ├── settings/
│   │   │   │   │   ├── ButtonManagementScreen.kt
│   │   │   │   │   ├── ButtonFormScreen.kt
│   │   │   │   │   ├── StatisticsScreen.kt
│   │   │   │   │   └── SettingsViewModel.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── ScatterPlotChart.kt
│   │   │   │   │   ├── EmojiPicker.kt
│   │   │   │   │   └── ColorPicker.kt
│   │   │   ├── workers/
│   │   │   │   ├── DataCleanupWorker.kt
│   │   │   │   └── PredictionWorker.kt
│   │   │   ├── di/
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   └── RepositoryModule.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   └── strings.xml
│   │   └── AndroidManifest.xml
│   └── test/
│       └── java/com/example/pixelwatchtagger/
│           ├── ClusteringServiceTest.kt
│           ├── PredictionServiceTest.kt
│           └── DatabaseTest.kt
└── build.gradle
```

### 8.2 Key Dependencies

**build.gradle (app level):**

```gradle
dependencies {
    // Wear OS
    implementation "androidx.wear.compose:compose-material:1.2.0"
    implementation "androidx.wear.compose:compose-foundation:1.2.0"

    // Compose
    implementation "androidx.compose.ui:ui:1.5.4"
    implementation "androidx.compose.runtime:runtime:1.5.4"

    // Room Database
    implementation "androidx.room:room-runtime:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"

    // WorkManager
    implementation "androidx.work:work-runtime-ktx:2.9.0"

    // Hilt (Dependency Injection)
    implementation "com.google.dagger:hilt-android:2.48"
    kapt "com.google.dagger:hilt-compiler:2.48"

    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

    // Clustering (Apache Commons Math or custom)
    implementation "org.apache.commons:commons-math3:3.6.1"

    // Testing
    testImplementation "junit:junit:4.13.2"
    testImplementation "androidx.room:room-testing:2.6.1"
    androidTestImplementation "androidx.test.ext:junit:1.1.5"
}
```

### 8.3 Architecture Pattern

**MVVM (Model-View-ViewModel):**

- **Model**: Room entities, repositories
- **View**: Jetpack Compose screens
- **ViewModel**: Business logic, state management

**Data Flow:**

```
UI (Compose) → ViewModel → Repository → DAO → Database
     ↑              ↓
     └── StateFlow ──┘
```

**Example ViewModel:**

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val buttonRepository: ButtonRepository,
    private val eventRepository: EventRepository,
    private val predictionService: PredictionService
) : ViewModel() {

    val buttons: StateFlow<List<Button>> = buttonRepository.getAllButtons()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val predictions: StateFlow<Map<Int, Prediction>> = /* ... */

    fun recordEvent(buttonId: Int) {
        viewModelScope.launch {
            val event = Event(
                buttonId = buttonId,
                timestamp = System.currentTimeMillis()
            )
            eventRepository.insert(event)

            buttonRepository.updateLastUsed(buttonId, event.timestamp)

            // Recalculate prediction for this button
            updatePrediction(buttonId)
        }
    }
}
```

---

## 9. Performance & Optimization

### 9.1 Database Optimization

**Indexing strategy:**
- `idx_event_button_id`: Fast lookup of events by button
- `idx_event_timestamp`: Fast time-range queries
- `idx_event_button_timestamp`: Composite index for button-specific time queries

**Query optimization:**
- Use `LIMIT` for paginated queries
- Avoid `SELECT *`, specify columns
- Use compiled statements for frequent queries

**Example optimized query:**

```sql
-- Good: Uses composite index
SELECT timestamp FROM event
WHERE button_id = ? AND timestamp >= ?
ORDER BY timestamp DESC
LIMIT 100;

-- Bad: Full table scan
SELECT * FROM event
WHERE timestamp >= ?;
```

### 9.2 Memory Management

**Clustering optimization:**
- Perform clustering in background thread
- Cache cluster results for 24 hours
- Only recalculate when new events added or daily

**Chart rendering:**
- Limit rendered data points (e.g., max 1000 points)
- Use canvas drawing for efficiency
- Implement virtualization for large datasets

### 9.3 Battery Optimization

**Background work constraints:**
- Run cleanup/prediction workers only when device idle
- Use `setRequiresBatteryNotLow(true)` for WorkManager
- Avoid frequent recalculations

**UI optimizations:**
- Debounce rapid button taps (max 1 event per second)
- Update countdown timers at 1-minute intervals (not every second)
- Pause updates when app in background

---

## 10. Testing Strategy

### 10.1 Unit Tests

**Database tests:**
```kotlin
@Test
fun insertButtonAndRetrieve() {
    val button = Button(label = "Test", emoji = "⭐", color = "#FF0000", position = 0, ...)
    dao.insert(button)

    val retrieved = dao.getButtonById(button.id)
    assertEquals("Test", retrieved.label)
}

@Test
fun cascadeDeleteEvents() {
    val button = dao.insert(Button(...))
    dao.insertEvent(Event(buttonId = button.id, timestamp = now))

    dao.deleteButton(button.id)

    val events = dao.getEventsForButton(button.id)
    assertTrue(events.isEmpty())
}
```

**Clustering tests:**
```kotlin
@Test
fun clusteringIdentifiesMorningPattern() {
    val events = listOf(
        Event(timestamp = toMinutes(8, 15)),
        Event(timestamp = toMinutes(8, 20)),
        Event(timestamp = toMinutes(8, 10)),
        // ... more morning events
    )

    val clusters = clusteringService.performClustering(events)

    assertEquals(1, clusters.size)
    assertTrue(clusters[0].centroidMinutes in 480..510) // ~8:00-8:30
}
```

### 10.2 Integration Tests

**End-to-end flow:**
```kotlin
@Test
fun recordEventAndVerifyPrediction() {
    // Add button
    val button = viewModel.addButton("Coffee", "☕", "#8B4513", 0)

    // Record multiple events
    repeat(10) {
        viewModel.recordEvent(button.id)
        delay(1000)
    }

    // Verify prediction exists
    val prediction = viewModel.predictions.value[button.id]
    assertNotNull(prediction)
}
```

### 10.3 UI Tests

**Compose UI testing:**
```kotlin
@Test
fun tapButtonRecordsEvent() {
    composeTestRule.setContent {
        MainScreen(viewModel)
    }

    composeTestRule.onNodeWithText("Coffee").performClick()

    // Verify event recorded
    val events = runBlocking { eventRepository.getEventsForButton(coffeeButton.id) }
    assertEquals(1, events.size)
}
```

---

## 11. Future Enhancements (Out of Scope for V1)

### 11.1 Advanced Features

1. **Smart Watch Face Complication**
   - Display nearest prediction on watch face
   - Show multiple upcoming predictions
   - Color-coded by confidence

2. **Predictive Notifications**
   - Alert 5-10 minutes before predicted event
   - "Time for Coffee?" notification at 14:25 (if cluster at 14:30)
   - User can snooze or dismiss

3. **Pattern Anomaly Detection**
   - Detect missed expected events
   - Alert: "You usually have coffee by now. Everything okay?"
   - Health monitoring applications

4. **Multi-Device Sync**
   - Sync buttons and events across multiple watches
   - Cloud backup (Firebase/Google Drive)
   - Restore data on new device

5. **Voice Commands**
   - "Hey Google, log coffee"
   - "Hey Google, when's my next medication?"
   - Hands-free event recording

6. **Gamification & Streaks**
   - Track consecutive days of specific button presses
   - Achievements: "7-day coffee streak!"
   - Visual badges and rewards

7. **Integration with Health APIs**
   - Export to Google Fit
   - Correlate with heart rate, activity data
   - Health insights: "You drink coffee after exercise"

8. **Advanced Analytics**
   - Weekly/monthly summary reports
   - Trend analysis (increasing/decreasing frequency)
   - Correlation between buttons (e.g., coffee → bathroom)

### 11.2 UI/UX Enhancements

1. **Customizable Layouts**
   - User can drag buttons to reposition
   - Save preferred layout
   - Multiple layout presets

2. **Themes**
   - Dark mode / Light mode toggle
   - Custom color schemes
   - High contrast accessibility mode

3. **Haptic Patterns**
   - Different vibration patterns per button
   - Customizable feedback intensity

4. **Animated Transitions**
   - Smooth animations between screens
   - Button press animations
   - Chart loading animations

---

## 12. Development Roadmap

### Phase 1: Foundation (Week 1-2)
- [x] Project setup (Wear OS + Kotlin + Compose)
- [ ] Room database schema implementation
- [ ] Basic CRUD repositories and DAOs
- [ ] Unit tests for database operations

### Phase 2: Main Interface (Week 3-4)
- [ ] Circular button layout composable
- [ ] Position calculation logic
- [ ] Button tap handling and event recording
- [ ] Settings navigation

### Phase 3: Settings & Management (Week 5-6)
- [ ] Button management screen
- [ ] Add/edit/delete button forms
- [ ] Emoji and color pickers
- [ ] Position auto-assignment logic
- [ ] 9-button limit enforcement

### Phase 4: Statistics & Visualization (Week 7-8)
- [ ] Scatter plot chart component
- [ ] Time range selector
- [ ] Data querying and rendering
- [ ] CSV export functionality

### Phase 5: Clustering & Predictions (Week 9-10)
- [ ] DBSCAN/K-means clustering implementation
- [ ] Cluster detection and metadata
- [ ] Prediction algorithm
- [ ] Countdown timer calculation
- [ ] Integration with main interface

### Phase 6: Background Tasks (Week 11)
- [ ] Data cleanup WorkManager task
- [ ] Prediction recalculation worker
- [ ] Task scheduling and constraints

### Phase 7: Polish & Testing (Week 12)
- [ ] UI/UX refinements
- [ ] Performance optimization
- [ ] End-to-end testing
- [ ] Bug fixes
- [ ] Documentation updates

### Phase 8: Launch Preparation (Week 13-14)
- [ ] User acceptance testing
- [ ] Play Store assets (screenshots, description)
- [ ] Privacy policy and terms
- [ ] Release build and deployment

---

## 13. Success Criteria

### 13.1 Functional Requirements

✅ **Core Functionality:**
- [ ] User can add up to 9 configurable buttons (label, emoji, color)
- [ ] Buttons arranged in circular layout (3 inner, 6 outer)
- [ ] Tap button records timestamp event
- [ ] Events stored in database with 30-day retention
- [ ] Settings mode allows button management
- [ ] Statistics screen shows scatter plot of events
- [ ] Export events to CSV

✅ **Prediction System:**
- [ ] Clustering algorithm identifies temporal patterns
- [ ] Predictions calculated for each button
- [ ] Countdown timers displayed on main interface
- [ ] Predictions update after new events
- [ ] Cluster visualization on statistics chart

### 13.2 Non-Functional Requirements

✅ **Performance:**
- [ ] Button tap response < 100ms
- [ ] Chart rendering < 2 seconds (for 1000 points)
- [ ] Database queries < 50ms (average)
- [ ] App launch time < 3 seconds

✅ **Usability:**
- [ ] Intuitive circular button interface
- [ ] Clear visual feedback on interactions
- [ ] Easy navigation between main and settings modes
- [ ] Accessible on Pixel Watch 4 screen size

✅ **Reliability:**
- [ ] No data loss on app crashes
- [ ] Accurate event timestamps
- [ ] Consistent predictions across app restarts
- [ ] Proper cascade deletion of events

✅ **Battery Efficiency:**
- [ ] Background tasks run only when device idle
- [ ] No significant battery drain from app
- [ ] Efficient database indexing

---

## 14. Open Questions & Decisions Needed

1. **Clustering Algorithm Selection:**
   - **Decision needed**: DBSCAN vs. K-means?
   - **Recommendation**: Start with DBSCAN (better for variable clusters)

2. **Prediction Confidence Threshold:**
   - **Decision needed**: Minimum confidence to show prediction?
   - **Recommendation**: Show all predictions, color-code by confidence

3. **Export Destination:**
   - **Decision needed**: Save to watch or send to phone?
   - **Recommendation**: Offer both options via share sheet

4. **Button Deletion:**
   - **Decision needed**: Allow undo after deletion?
   - **Recommendation**: No undo (confirmation dialog sufficient)

5. **Chart Interaction:**
   - **Decision needed**: Support zoom/pan or keep simple?
   - **Recommendation**: Add zoom/pan for better data exploration

6. **Notification Preferences:**
   - **Decision needed**: Enable predictive notifications in V1?
   - **Recommendation**: Defer to future enhancement (Phase 2)

7. **Data Privacy:**
   - **Decision needed**: Local-only or cloud sync?
   - **Recommendation**: V1 local-only, cloud sync in future

---

## 15. Appendices

### Appendix A: Color Palette Recommendations

Suggested button colors (hex codes):

- 🔴 Red: `#F44336`
- 🟠 Orange: `#FF9800`
- 🟡 Yellow: `#FFEB3B`
- 🟢 Green: `#4CAF50`
- 🔵 Blue: `#2196F3`
- 🟣 Purple: `#9C27B0`
- 🟤 Brown: `#795548`
- ⚫ Black: `#212121`
- ⚪ White: `#FAFAFA`
- 🩷 Pink: `#E91E63`
- 🩵 Cyan: `#00BCD4`
- 🟫 Lime: `#CDDC39`

### Appendix B: Sample Data Schema

**Sample button configuration:**

```json
{
  "id": 1,
  "label": "Coffee",
  "emoji": "☕",
  "color": "#795548",
  "position": 3,
  "created_at": 1737534000,
  "last_used_at": 1737620400
}
```

**Sample event records:**

```json
[
  {
    "id": 1,
    "button_id": 1,
    "timestamp": 1737534934
  },
  {
    "id": 2,
    "button_id": 1,
    "timestamp": 1737557192
  }
]
```

### Appendix C: Clustering Example

**Input events (Coffee button, minutes since midnight):**

```
[495, 500, 505, 850, 855, 860, 1230, 1235]
```

**DBSCAN clustering (eps=60, min_samples=2):**

```
Cluster 1 (Morning): [495, 500, 505]
  Centroid: 500 (08:20)

Cluster 2 (Afternoon): [850, 855, 860]
  Centroid: 855 (14:15)

Cluster 3 (Evening): [1230, 1235]
  Centroid: 1232.5 (20:32)
```

**Predictions:**
- Current time: 10:00 (600 minutes)
- Next prediction: Cluster 2 at 14:15 (855 minutes)
- Countdown: 4h 15m

---

## Document Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-01-22 | Initial | Complete requirements document |

---

**End of Requirements Document**
