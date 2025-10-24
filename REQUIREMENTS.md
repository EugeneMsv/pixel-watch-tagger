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
    display_order INTEGER NOT NULL,  -- Order in which buttons are displayed (0-8)
    created_at INTEGER NOT NULL,  -- Unix timestamp
    last_used_at INTEGER  -- Unix timestamp of last event
);
```

**Fields:**
- `id`: Auto-incrementing primary key
- `label`: User-defined text label (e.g., "Coffee", "Medicine")
- `emoji`: Selected emoji character for visual identification
- `color`: Hex color code for button appearance
- `display_order`: Order in which buttons appear on circle (0-8), used for consistent positioning
- `created_at`: Timestamp when button was created
- `last_used_at`: Timestamp of most recent event (updated on each tap)

### 2.2 Table: Event

Stores individual timestamp records for button presses.

```sql
CREATE TABLE event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    button_id INTEGER NOT NULL,
    timestamp LONG NOT NULL,  -- Unix timestamp
    FOREIGN KEY (button_id) REFERENCES button(id) ON DELETE CASCADE
);

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
    val displayOrder: Int,
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

### 3.1 Main Interface - Dynamic Circular Button Layout

**Layout Overview:**

The interface uses a **single adaptive circle** that adjusts based on the number of buttons. The watch screen is optimized for circular displays (Google Pixel Watch).

```
┌─────────────────────┐
│                     │
│                     │
│        ○            │  ← 1-9 buttons arranged on
│    ○       ○        │     a single circle
│   ○    ◯    ○       │     Center: Empty or logo
│    ○       ○        │     Buttons: Evenly spaced
│        ○            │
│                     │
│                     │
└─────────────────────┘
```

**Dynamic Sizing:**
Buttons automatically resize based on button count to maintain optimal usability:
- **1 button**: Large (80-100px diameter)
- **2-3 buttons**: Medium-large (70-85px diameter)
- **4-6 buttons**: Medium (55-70px diameter)
- **7-9 buttons**: Compact (45-55px diameter)

**Circle Radius:**
Calculated dynamically to position buttons optimally:
- Base radius: ~60-70% of screen width
- Adjusted based on button size to prevent edge clipping
- Formula: `radius = (screenWidth / 2) * 0.65 - (buttonDiameter / 2)`

**Button Specifications:**
- **Shape**: Circular
- **Appearance per button**:
  - Background: User-selected color
  - Icon: User-selected emoji (centered, large, scales with button)
  - Overlay (View Mode): Small countdown timer text showing time until next predicted event
    - Example: "2h 15m"
    - Position: Bottom of button or below emoji
    - Font size: Scales with button size
- **Spacing**: Evenly distributed around the circle
- **Visual feedback**:
  - **View Mode - Single tap**: Pulse animation + haptic vibration
  - **View Mode - Long press**: Gentle vibration, opens stats menu
  - **Edit Mode - Single tap**: Highlight + open settings menu

**Position Calculation:**
For button at index `i` with total `n` buttons:
```kotlin
val anglePerButton = 360f / n
val angle = (i * anglePerButton - 90f).toRadians()  // Start at top (12 o'clock)
val x = centerX + radius * cos(angle)
val y = centerY + radius * sin(angle)
```

**Button Order:**
- Buttons positioned clockwise starting from top (12 o'clock)
- Order determined by `display_order` field in database (0-8)
- First button (display_order=0) at top, subsequent buttons clockwise

**Maximum Capacity:**
- **Total maximum: 9 buttons**
- No inner/outer circle distinction
- All buttons on single circle with adaptive sizing

---

### 3.2 Application Modes: View and Edit

The application operates in two distinct modes, optimized for watch interaction:

#### 3.2.1 View Mode (Default)

**Purpose**: Quick event recording and data viewing

**Visual Appearance:**
- Only user-created buttons visible on the circle
- Clean, minimal interface
- Countdown timers visible on each button (if predictions available)
- No additional UI elements or icons

**Interactions:**

1. **Single Tap on Button**
   - Records new event with current timestamp
   - Visual feedback: Pulse animation
   - Haptic feedback: Short vibration
   - UI update: Countdown timer refreshes (if affected)
   - No navigation away from main screen

2. **Long Press on Button**
   - Opens **Stats Menu** for that specific button
   - Haptic feedback: Longer vibration pattern
   - Navigation: Transitions to Stats screen
   - Shows:
     - Button-specific statistics
     - Scatter plot chart
     - Detected clusters
     - Next predicted event details
     - Export CSV option

3. **Swipe from Edge Gesture**
   - **Swipe from right edge** → Toggle to Edit Mode
   - Smooth transition animation
   - Mode indicator briefly appears

**First-Time Experience:**
- If no buttons exist: Show centered "+" button only
- Tap "+" → Enter Edit Mode → Add first button

---

#### 3.2.2 Edit Mode

**Purpose**: Button management (add, edit, delete, reorder)

**Visual Appearance:**
- All existing buttons visible (same positions as View Mode)
- Additional **"+" button** appears on the circle
  - Positioned at the next available display_order slot
  - Visually distinct (e.g., dashed border, lighter color)
  - Icon: Plus symbol (➕)
- Optional: Subtle visual indicator showing Edit Mode is active (e.g., slight glow around buttons)

**Interactions:**

1. **Single Tap on Existing Button**
   - Opens **Button Settings Menu** for that button
   - Navigation: Transitions to Button Settings screen
   - Options available:
     - **Edit** section:
       - Rename label
       - Change emoji
       - Change color
     - **Actions** section:
       - Delete button (with confirmation dialog)

2. **Single Tap on "+" Button**
   - Opens **Add New Button** form
   - Navigation: Transitions to Add Button screen
   - Form includes:
     - Label input field
     - Emoji picker
     - Color picker
     - Save/Cancel buttons
   - On save: New button added at next display_order

3. **Swipe from Edge Gesture**
   - **Swipe from right edge** → Return to View Mode
   - Smooth transition animation
   - Mode indicator briefly disappears

**Maximum Buttons Handling:**
- If 9 buttons exist: "+" button disabled or hidden
- Attempting to add shows error: "Maximum 9 buttons reached. Delete a button first."

**Button Reordering (Optional for V1):**
- Long-press and drag to reorder (future enhancement)
- For V1: Buttons added in chronological order (by display_order)

---

### 3.3 Button Settings Menu (Edit Mode)

**Accessed via**: Edit Mode → Tap existing button

**Layout:**

```
┌─────────────────────┐
│  ← Button Settings  │
├─────────────────────┤
│                     │
│  ☕ Coffee           │  ← Current button preview
│  [Color indicator]  │
│                     │
│  ━━━ Edit ━━━━━━━━  │
│  [Rename Label]     │
│  [Change Emoji]     │
│  [Change Color]     │
│                     │
│  ━━━ Actions ━━━━━  │
│  [Delete Button]    │  ← Red/destructive style
│                     │
│  [← Back]           │
└─────────────────────┘
```

**Form Options:**

1. **Rename Label**
   - Opens text input dialog
   - Current label pre-filled
   - Max 20 characters
   - Validation: Non-empty
   - Save/Cancel buttons

2. **Change Emoji**
   - Opens system emoji picker
   - Current emoji pre-selected
   - Single emoji selection
   - Immediately updates on selection

3. **Change Color**
   - Opens color picker dialog
   - Predefined palette: 12-16 colors
   - Current color highlighted
   - Hex color stored (e.g., "#FF5722")

4. **Delete Button**
   - Red destructive button
   - Shows confirmation dialog:
     ```
     Delete "Coffee"?
     This will also delete all 87 associated events.
     This action cannot be undone.

     [Cancel] [Delete]
     ```
   - On confirm:
     - Delete button record
     - Cascade delete all events (foreign key)
     - Recalculate display_order for remaining buttons
     - Return to Edit Mode

**Navigation:**
- Back button: Return to Edit Mode without changes
- After save: Return to Edit Mode with updates applied

---

### 3.4 Add New Button Form (Edit Mode)

**Accessed via**: Edit Mode → Tap "+" button

**Layout:**

```
┌─────────────────────┐
│  ← Add Button       │
├─────────────────────┤
│  Label              │
│  [____________]     │  ← Text input field
│                     │
│  Emoji              │
│  [⭐  Select...]    │  ← Opens emoji picker
│                     │
│  Color              │
│  [●  Select...]     │  ← Opens color picker
│                     │
│  [Save]  [Cancel]   │
└─────────────────────┘
```

**Form Fields:**

1. **Label**
   - Text input, max 20 characters
   - Required field
   - Validation: Non-empty
   - Placeholder: "Button name"

2. **Emoji**
   - Opens system emoji picker dialog
   - Default: ⭐ (star)
   - Single emoji selection
   - Required field

3. **Color**
   - Opens color picker dialog
   - Predefined palette: 12-16 common colors (see Appendix A)
   - Default: Random color or first available
   - Hex color stored (e.g., "#FF5722")

**Save Behavior:**
- Validation: Ensure label is non-empty
- Assign `display_order`: Next available index (0-8)
- If < 9 buttons: Save successfully
- If = 9 buttons: Should not reach this state (+ button hidden)
- On save:
  - Create new Button record in database
  - Return to Edit Mode
  - New button appears on circle

**Cancel Behavior:**
- Discard all changes
- Return to Edit Mode
- No database modifications

---

### 3.5 Statistics Chart Screen (View Mode - Long Press)

**Accessed via**: View Mode → Long press on any button

**Purpose**: View detailed statistics, predictions, and event history for a specific button

**Layout:**

```
┌─────────────────────┐
│  ← ☕ Coffee Stats  │  ← Button name in title
├─────────────────────┤
│                     │
│  Next: 2h 15m       │  ← Next predicted event
│  Confidence: High   │
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

**Context:**
- Automatically loaded for the specific button that was long-pressed
- No need for button dropdown selector (single-button focus)
- Title shows button emoji and label

**Prediction Section:**
- **Next predicted event**: Time until next predicted occurrence
  - Format: "2h 15m" or "Tomorrow 8:15 AM"
- **Confidence level**: HIGH, MEDIUM, LOW
  - Color-coded (Green, Yellow, Gray)
- Only shown if enough data exists (7+ events)

**Controls:**

1. **Time Range Selector**
   - Three chip buttons: 7, 14, 30 days
   - Currently selected chip highlighted
   - Default: 30 days
   - Changes chart data range and cluster calculations

2. **Export CSV Button**
   - Exports current button's events to CSV
   - Format: `timestamp,unix_timestamp,button_label,button_emoji`
   - Saved to watch storage or shared to phone
   - Shows success toast: "Exported 87 events"

**Navigation:**
- Back button (← or hardware back): Return to View Mode
- Stays on this screen until user navigates away

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

1. App launches → Main interface displayed (View Mode, empty)
2. Only a centered **"+" button** is visible
3. User taps "+" button
4. App enters **Edit Mode** (or directly opens Add Button form)
5. Add Button form opens
6. User fills in:
   - Label: "Coffee"
   - Emoji: ☕ (from emoji picker)
   - Color: Brown (from color palette)
7. User taps "Save"
8. App returns to View Mode
9. First button now visible on the circle at top position (12 o'clock)
10. User can tap button to record first event

**Alternative first launch:**
- User can swipe from right edge to enter Edit Mode explicitly
- Then tap "+" button to add first button

---

### 5.2 Recording an Event (View Mode)

**Steps:**

1. User in **View Mode** (default state)
2. User taps a button (e.g., "Coffee" button)
3. **Immediate feedback**:
   - Haptic vibration (short pulse, ~50ms)
   - Button pulse animation (brief scale: 1.0 → 1.2 → 1.0)
   - Optional: Subtle visual flash (button border/glow)
4. **Backend processing** (asynchronous):
   - Create new Event record with current timestamp
   - Update button's `last_used_at` field
   - If enough data (≥7 events), recalculate predictions for this button
5. **UI update**:
   - Countdown timer updates on button (if prediction changed)
   - No navigation away from View Mode
6. User can immediately tap same button again or tap other buttons

**Expected frequency:** ~10 taps/button/day

**User stays in View Mode** - no screen transitions for quick logging

---

### 5.3 Viewing Statistics (View Mode - Long Press)

**Steps:**

1. User in **View Mode**
2. User **long presses** a button (e.g., "Coffee" button)
   - Long press duration: ~500-700ms
   - Haptic feedback: Gentle vibration pattern (different from single tap)
3. **Statistics screen opens** for that specific button
4. **Default view**:
   - Button emoji and label in title: "← ☕ Coffee Stats"
   - Prediction section shows next event and confidence
   - 30-day range selected by default
   - Scatter plot chart renders with all events
   - Detected clusters listed below chart
5. **Interactions**:
   - Change time range (7/14/30 days) → Chart re-queries and updates
   - Pinch/zoom on chart → Zoom into time range
   - Tap data point → Show tooltip with exact timestamp
   - Scroll down to view cluster legend
   - Review detected patterns and confidence levels
6. **Export** (optional):
   - Tap "Export CSV" button
   - File saved to watch storage or shared to phone
   - Success toast: "Exported 87 events to coffee-20250123.csv"
7. **Navigation back**:
   - Tap back arrow (←) or hardware back button
   - Returns to **View Mode**

---

### 5.4 Switching to Edit Mode

**Steps:**

1. User in **View Mode**
2. User swipes from **right edge** of screen
   - Swipe gesture: Short horizontal swipe inward (→ ←)
   - Visual feedback: Transition animation
   - Optional: Brief mode indicator badge ("Edit Mode")
3. App enters **Edit Mode**
4. **Visual changes**:
   - All existing buttons remain visible at same positions
   - Additional **"+" button** appears on circle (next available position)
   - Subtle visual indicator (e.g., slight glow on buttons, or "Edit" badge)
5. User can now:
   - Tap existing button → Edit button settings
   - Tap "+" button → Add new button

---

### 5.5 Adding a New Button (Edit Mode)

**Steps:**

1. User in **Edit Mode** (via swipe from edge)
2. User taps **"+" button**
3. **Add Button form** opens
4. User fills in form:
   - **Label**: "Meditation" (max 20 chars)
   - **Emoji**: 🧘 (opens emoji picker)
   - **Color**: Purple (opens color picker with 12-16 palette options)
5. User taps **"Save"**
6. **Validation**:
   - Check label is non-empty
   - If valid: Assign next available `display_order` (0-8)
   - Create Button record in database
7. Form closes, returns to **Edit Mode**
8. New button appears on circle at next position (clockwise from last button)
9. User can:
   - Add more buttons (if < 9 total)
   - Swipe from edge → Return to View Mode

**Edge case - 9 buttons exist:**
- "+" button is **hidden** or **disabled**
- User cannot add more buttons until one is deleted

---

### 5.6 Editing an Existing Button (Edit Mode)

**Steps:**

1. User in **Edit Mode**
2. User taps an **existing button** (e.g., "Coffee")
3. **Button Settings Menu** opens
4. Menu shows:
   - Current button preview (emoji, label, color)
   - **Edit** section:
     - [Rename Label]
     - [Change Emoji]
     - [Change Color]
   - **Actions** section:
     - [Delete Button] (red/destructive style)
5. User selects an option:

   **Option A: Rename Label**
   - Text input dialog opens
   - Current label ("Coffee") pre-filled
   - User changes to "Espresso"
   - Taps Save → Returns to Button Settings Menu

   **Option B: Change Emoji**
   - Emoji picker opens
   - Current emoji (☕) pre-selected
   - User selects new emoji (🍵)
   - Immediately returns to Button Settings Menu with new emoji

   **Option C: Change Color**
   - Color picker opens
   - Current color highlighted
   - User selects new color (e.g., Green)
   - Returns to Button Settings Menu with new color

6. User taps **back arrow (←)** to return to Edit Mode
7. **UI updates**:
   - Button on main circle reflects changes
   - Database updated with new values
   - Events remain unchanged (same button_id)

---

### 5.7 Deleting a Button (Edit Mode)

**Steps:**

1. User in **Edit Mode**
2. User taps an **existing button** to open Button Settings Menu
3. User scrolls to **"Delete Button"** (red button at bottom)
4. User taps **"Delete Button"**
5. **Confirmation dialog** appears:
   ```
   Delete "Coffee"?
   This will also delete all 87 associated events.
   This action cannot be undone.

   [Cancel] [Delete]
   ```
6. User taps **"Delete"**
7. **Backend processing**:
   - Delete Button record from database
   - Cascade delete all associated Event records (via foreign key)
   - Recalculate `display_order` for remaining buttons:
     - If button with display_order=3 deleted, buttons 4-8 shift down to 3-7
   - Recalculate predictions for remaining buttons (if affected)
8. Dialog closes, returns to **Edit Mode**
9. **UI updates**:
   - Deleted button removed from circle
   - Remaining buttons reposition to maintain even spacing
   - Button count decreases (e.g., 9 → 8)
   - If was at 9 buttons: "+" button now appears/enabled

---

### 5.8 Returning to View Mode (from Edit Mode)

**Steps:**

1. User in **Edit Mode**
2. User swipes from **right edge** of screen again
   - Same gesture as entering Edit Mode (toggle behavior)
   - Alternative: Hardware back button
3. **Transition animation** plays
4. App returns to **View Mode**
5. **Visual changes**:
   - "+" button disappears
   - Edit mode indicator disappears
   - Button interactions return to View Mode behavior:
     - Single tap = Record event
     - Long press = View stats

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
│   │   │   │   │   ├── ButtonLayoutCalculator.kt  // NEW: Dynamic layout calculations
│   │   │   │   │   ├── MainViewModel.kt
│   │   │   │   │   └── AppMode.kt  // NEW: Enum for View/Edit modes
│   │   │   │   ├── edit/
│   │   │   │   │   ├── EditModeScreen.kt  // NEW: Edit mode overlay
│   │   │   │   │   ├── ButtonSettingsScreen.kt  // Renamed from ButtonFormScreen
│   │   │   │   │   ├── AddButtonScreen.kt  // NEW: Separate add form
│   │   │   │   │   └── EditViewModel.kt
│   │   │   │   ├── stats/
│   │   │   │   │   ├── StatisticsScreen.kt
│   │   │   │   │   └── StatsViewModel.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── ScatterPlotChart.kt
│   │   │   │   │   ├── EmojiPicker.kt
│   │   │   │   │   ├── ColorPicker.kt
│   │   │   │   │   └── EdgeSwipeDetector.kt  // NEW: Gesture detection
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
    private val predictionService: PredictionService,
    private val layoutCalculator: ButtonLayoutCalculator
) : ViewModel() {

    // App mode state (View/Edit)
    private val _appMode = MutableStateFlow(AppMode.VIEW)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    // Button data with dynamic layout
    val buttons: StateFlow<List<Button>> = buttonRepository.getAllButtons()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Button positions calculated dynamically
    val buttonPositions: StateFlow<List<ButtonPosition>> = buttons.map { buttonList ->
        layoutCalculator.calculatePositions(buttonList, screenSize)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val predictions: StateFlow<Map<Int, Prediction>> = /* ... */

    fun toggleMode() {
        _appMode.value = when (_appMode.value) {
            AppMode.VIEW -> AppMode.EDIT
            AppMode.EDIT -> AppMode.VIEW
        }
    }

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

// App mode enum
enum class AppMode {
    VIEW,  // Default: single tap = record, long press = stats
    EDIT   // Single tap = edit button, + button visible
}

// Button position data class
data class ButtonPosition(
    val button: Button,
    val x: Float,
    val y: Float,
    val size: Float  // Diameter in pixels
)
```

### 8.4 Dynamic Layout Calculator

**Purpose**: Calculates optimal button sizes and positions for the circular layout based on button count, ensuring buttons fit perfectly inside the screen without intersection.

**Mathematical Approach:**

The layout algorithm calculates button size mathematically to ensure:
1. All buttons fit inside the circular screen
2. No buttons intersect/overlap
3. Maximum use of available space
4. Buttons touch each other at exactly one point when possible

**Implementation:**

```kotlin
class ButtonLayoutCalculator @Inject constructor() {

    /**
     * Calculates button positions and sizes for circular layout
     * @param buttons List of buttons ordered by display_order
     * @param screenSize Screen dimensions (width x height)
     * @return List of ButtonPosition with x, y coordinates and size
     */
    fun calculatePositions(
        buttons: List<Button>,
        screenSize: Size
    ): List<ButtonPosition> {
        if (buttons.isEmpty()) return emptyList()

        val centerX = screenSize.width / 2f
        val centerY = screenSize.height / 2f
        val screenRadius = min(screenSize.width, screenSize.height) / 2f

        // Special case: 1 button - fills entire screen
        if (buttons.size == 1) {
            return listOf(
                ButtonPosition(
                    button = buttons[0],
                    x = centerX,
                    y = centerY,
                    size = screenRadius * 2f * 0.9f  // 90% of screen to leave margin
                )
            )
        }

        // Calculate optimal button size based on geometry
        val buttonRadius = calculateOptimalButtonRadius(buttons.size, screenRadius)
        val buttonDiameter = buttonRadius * 2f

        // Calculate circle radius (distance from center to button centers)
        val layoutRadius = screenRadius - buttonRadius  // Ensures buttons fit inside screen

        // Calculate positions around circle
        return buttons.mapIndexed { index, button ->
            val angle = calculateAngle(index, buttons.size)
            val x = centerX + layoutRadius * cos(angle)
            val y = centerY + layoutRadius * sin(angle)

            ButtonPosition(
                button = button,
                x = x,
                y = y,
                size = buttonDiameter
            )
        }
    }

    /**
     * Calculate optimal button radius based on geometry
     *
     * For n buttons arranged on a circle of radius R:
     * - Buttons are placed at distance R from center
     * - Each button has radius r
     * - For buttons to touch at exactly one point, the distance between adjacent button centers equals 2r
     *
     * Using geometry:
     * - Angle between adjacent buttons: θ = 2π/n
     * - Chord length between adjacent centers: c = 2R·sin(θ/2)
     * - For buttons to touch: c = 2r
     * - Therefore: r = R·sin(π/n)
     *
     * Since R = screenRadius - r (to fit inside screen):
     * - r = (screenRadius - r)·sin(π/n)
     * - r = screenRadius·sin(π/n) - r·sin(π/n)
     * - r·(1 + sin(π/n)) = screenRadius·sin(π/n)
     * - r = screenRadius·sin(π/n) / (1 + sin(π/n))
     */
    private fun calculateOptimalButtonRadius(count: Int, screenRadius: Float): Float {
        val angleRad = PI.toFloat() / count
        val sinAngle = sin(angleRad)

        // Mathematical formula for optimal button radius
        val optimalRadius = screenRadius * sinAngle / (1f + sinAngle)

        // Apply practical constraints
        val minRadius = 22.5f  // Minimum 45px diameter for usability
        val maxRadius = 50f    // Maximum 100px diameter

        return optimalRadius.coerceIn(minRadius, maxRadius)
    }

    /**
     * Calculate angle for button at given index
     * Starts at top (12 o'clock = -90°) and proceeds clockwise
     */
    private fun calculateAngle(index: Int, totalCount: Int): Float {
        val anglePerButton = (2f * PI / totalCount).toFloat()
        return (index * anglePerButton - PI / 2f).toFloat()  // Start at top (-90°)
    }
}

// Button position data class
data class ButtonPosition(
    val button: Button,
    val x: Float,      // Center X coordinate
    val y: Float,      // Center Y coordinate
    val size: Float    // Diameter in pixels
)
```

**Geometric Properties:**

1. **Single Button (n=1)**:
   - Takes 90% of screen diameter
   - Centered on screen
   - Example: 360px screen → 324px button

2. **Two Buttons (n=2)**:
   - Formula: r = screenRadius × sin(90°) / (1 + sin(90°))
   - r = R × 1 / 2 ≈ 0.414R
   - Each button ≈ 41.4% of screen radius
   - Positioned at opposite sides (180° apart)
   - Touch at one point in the middle

3. **Three Buttons (n=3)**:
   - Formula: r = screenRadius × sin(60°) / (1 + sin(60°))
   - r = R × 0.866 / 1.866 ≈ 0.464R
   - Each button ≈ 46.4% of screen radius
   - Positioned at 120° intervals
   - Form equilateral triangle, touching at exactly 3 points

4. **Four or More Buttons (n≥4)**:
   - Similar geometric calculation
   - Buttons get progressively smaller as count increases
   - Always tangent to adjacent buttons (touching at one point)
   - Constrained by minimum usability size (45px diameter)

**Visual Examples:**

```
n=1: Single large button        n=2: Two buttons           n=3: Three buttons
┌─────────┐                    ┌─────────┐                ┌─────────┐
│         │                    │    ●    │                │    ●    │
│    ●    │  ← Fills screen    │  ●   ●  │  ← Touch      │  ●   ●  │  ← Form
│         │                    │         │    in center   │    ●    │    triangle
└─────────┘                    └─────────┘                └─────────┘

n=6: Six buttons               n=9: Nine buttons
┌─────────┐                    ┌─────────┐
│  ● ● ●  │                    │ ● ● ● ● │
│ ●     ● │  ← Evenly         │●       ●│  ← Maximum
│  ● ● ●  │    spaced          │ ● ● ● ● │    capacity
└─────────┘                    └─────────┘
```

**Usage in Compose UI:**

```kotlin
@Composable
fun CircularButtonLayout(
    buttonPositions: List<ButtonPosition>,
    appMode: AppMode,
    onButtonClick: (Int) -> Unit,
    onButtonLongPress: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        buttonPositions.forEach { position ->
            CircleButton(
                button = position.button,
                x = position.x,
                y = position.y,
                size = position.size,
                mode = appMode,
                onClick = { onButtonClick(position.button.id) },
                onLongPress = { onButtonLongPress(position.button.id) }
            )
        }

        // Show + button in Edit Mode if < 9 buttons
        if (appMode == AppMode.EDIT && buttonPositions.size < 9) {
            val nextPosition = calculateNextButtonPosition(buttonPositions)
            AddButton(
                x = nextPosition.x,
                y = nextPosition.y,
                size = nextPosition.size
            )
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
