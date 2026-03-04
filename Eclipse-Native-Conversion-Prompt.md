# ECLIPSE BROWSER — Native Android Conversion Prompt

## CONTEXT
Eclipse Browser is currently an Android app where the home screen is built
in HTML/CSS/JavaScript loaded inside a WebView. The WebView is only supposed
to be used for browsing external websites — NOT for rendering the app's own UI.

The goal of this task is to convert the home screen UI from HTML/CSS/JS
into real native Android code so the app feels and performs like a
professional Android app.

---

## CURRENT ARCHITECTURE (what exists now)

```
MainActivity.kt
  └── WebView (loads index.html as home screen)
        ├── index.html       ← home screen UI
        ├── css/style.css    ← all styling
        └── js/main.js       ← all logic (clock, search, settings, widgets)
        └── js/storage.js    ← localStorage wrapper
```

The WebView also handles:
- Greeting + clock
- Search bar
- Quick access sites
- Customize panel (themes, accent colors, toggles)
- Bookmarks, History overlays
- Tabs management
- Incognito / Void mode
- About page
- Widgets zone (weather, NASA, Pomodoro etc.)

---

## WHAT YOU MUST BUILD

Convert the above into **Jetpack Compose** (preferred) or **XML + ViewBinding**.

### Tech Stack to use:
- Language: Kotlin only
- UI: Jetpack Compose (Material3 + custom theming)
- State: ViewModel + StateFlow
- Storage: DataStore (replaces localStorage)
- Navigation: No Jetpack Nav — single Activity, show/hide composables
- WebView: Keep for actual web browsing only
- Fonts: Use Google Fonts via Compose (Outfit, Space Mono)
- Animations: Compose animate* APIs — no XML animators
- Min SDK: 24, Target SDK: 34

### Do NOT use:
- Fragments
- XML layouts (if using Compose)
- Any third party UI library
- Room database (DataStore is enough)

---

## COMPONENT BREAKDOWN
Build each component independently in this exact order:

### 1. AppTheme.kt
- Define color tokens matching the HTML CSS variables:
  - `accentOrange = #FF6B1A`
  - `accentGold = #E8C840`
  - `surface = rgba(255,255,255,0.055) equivalent`
  - `background = #000000`
- Define typography using Outfit (weights 200,300,400,500,600) and Space Mono
- Support dynamic accent color (user can change it — store in DataStore)

### 2. StorageManager.kt
- Replace localStorage/Store JS object with DataStore<Preferences>
- Must support:
  - `searchEngine: String` (default: "duckduckgo")
  - `bgTheme: String` (default: "eclipse")
  - `accentColor: String` (default: "#FF6B1A")
  - `particlesOn: Boolean` (default: true)
  - `starsOn: Boolean` (default: true)
  - `orbOn: Boolean` (default: true)
  - `weatherOn: Boolean` (default: true)
  - `uiStyle: String` (default: "normal") — "normal" or "glass"
  - `quickSites: String` (JSON string of site list)
  - `bookmarks: String` (JSON string)
  - `history: String` (JSON string)
- Expose all as StateFlow
- All reads/writes are suspend functions

### 3. HomeViewModel.kt
- Holds all UI state
- Clock state: updates every 30 seconds
- Greeting: Morning/Afternoon/Evening/Night based on hour
- Night tagline: show "Find the Unseen" only between 21:00–04:59
- Weather state: loaded from OpenWeatherMap API
- Quick sites state: loaded from StorageManager
- All state exposed as StateFlow<T>

### 4. StarFieldCanvas.kt
- Compose Canvas that draws 120 animated stars
- Each star has random size (0.4–2.6dp), position, opacity, twinkle speed
- Twinkle animation: alpha oscillates between lo and hi values
- Background gradient based on selected theme:
  - Eclipse: pure black
  - Nebula: radial purple/blue overlay
  - Aurora: radial green/teal overlay
  - Void: pure black

### 5. ParticleSystem.kt
- Compose Canvas that spawns floating particles
- Particles drift upward with random horizontal drift
- Color matches current accent color
- Maximum 10 particles on screen at once
- Toggle on/off from settings

### 6. EclipseOrb.kt
- Recreate the SVG eclipse orb as a Compose Canvas drawing
- Outer ring: stroke circle with golden color
- Shadow disc: offset filled circle in near-black
- Glow: radial gradient on left side
- Floating animation: translateY oscillates ±7dp over 7 seconds

### 7. HomeScreen.kt
- Main scrollable column
- Contains in order:
  1. TopBar (brand label "ECLIPSE" + Shield badge)
  2. HeroBlock (greeting, date, clock, night tagline, weather inline, orb)
  3. SearchBar + search button
  4. FilterTabs (All, News, Images, Videos, AI)
  5. QuickSites grid (4 columns)
  6. WidgetsZone (vertical list of active widgets)
- All sections animate in with staggered fadeUp + translateY on first compose

### 8. SearchBar.kt
- Rounded container with search icon
- Focus state: accent color border + glow shadow
- On submit: build search URL based on selected engine and active tab
- Engines: DuckDuckGo, Google, Bing, Brave
- Tab modifiers: add correct URL params per engine per tab type
- On search: call `onNavigate(url)` lambda — opens in WebView

### 9. QuickSites.kt
- 4-column grid
- Each site: rounded icon container + favicon from Google S2 API + label
- Long press → confirm remove dialog
- Plus button → add site dialog (URL + optional name)
- Sites saved to DataStore via StorageManager

### 10. WebViewScreen.kt
- Full screen WebView for browsing
- Intercept ad domains (use the BLOCKED set from original MainActivity.kt)
- Bottom nav stays visible (Back, Forward, Home, Tabs, Menu)
- Home button returns to HomeScreen composable
- Title + URL updated in parent ViewModel on page load
- Download handling via DownloadManager

### 11. CustomizeSheet.kt
- Bottom sheet composable
- Sections: Accent Color, Background Theme, UI Style, Search Engine, Visual Effects
- All changes immediately apply + save to DataStore
- UI Style toggle: "Normal" vs "Glassy"
  - Glassy: surfaces get blur effect using RenderEffect (API 31+) with fallback for older

### 12. TabsSheet.kt
- Bottom sheet showing open tabs in 2-column grid
- Each tab: title, domain, close button
- Active tab highlighted with accent border
- New tab + New Void tab buttons

### 13. IncognitoScreen.kt
- Full replacement for home when in void mode
- Purple color scheme (--incog: #7C00FF)
- Void mode icon (SVG equivalent in Canvas)
- "No history. No traces. You are invisible." message
- Incognito pills: No History, No Tracking, Blackhole Session
- Search bar with purple focus state
- Exit Void Mode button

### 14. HistoryScreen.kt + BookmarksScreen.kt
- Bottom sheet with scrollable list
- Each item: favicon, title, domain/time, delete button
- Clear all button in header
- Tapping item navigates to that URL

### 15. AboutSheet.kt
- Eclipse logo, version, tagline
- Stats row: users, rating
- Upcoming roadmap items
- Share Eclipse button (Android share intent)

---

## ANIMATION RULES
Every screen entry must animate. Use these exact specs:

```kotlin
// Standard entry animation
val enterTransition = fadeIn(tween(350)) + slideInVertically(
    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
    initialOffsetY = { it / 3 }
)

// Scale in for cards/sheets
val scaleIn = scaleIn(
    animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
    initialScale = 0.88f
) + fadeIn(tween(300))

// Stagger: add delay per item index
delay = index * 60 // milliseconds
```

All springs use `dampingRatio = 0.75f` — this gives the premium bouncy feel.
No linear animations anywhere in the UI.

---

## GLASS MODE
When `uiStyle == "glass"`:
- Use `BlurMaskFilter` or `RenderEffect.createBlurEffect` on surface backgrounds
- Surface background: `Color(0x8C0C0C18)` (semi-transparent dark)
- Add a subtle white border: `Color(0x1FFFFFFF)`
- For API < 31: use semi-transparent surface without blur as fallback

---

## ERROR HANDLING RULES
- Every network call: try/catch with user-visible snackbar or toast
- Every DataStore read: provide default value, never crash on missing key
- Favicon loading: placeholder icon if load fails
- Weather API: show "--°" if unavailable, never blank
- All coroutines: use `viewModelScope`, catch exceptions

---

## STORAGE MIGRATION
The existing app uses `localStorage` in WebView. When user first opens the
native version, attempt to read from the old WebView localStorage via
JavaScript bridge and import into DataStore. If not found, use defaults.

---

## WHAT SUCCESS LOOKS LIKE
- App opens in under 300ms (no HTML parse time)
- Home screen feels native — same smoothness as Google Chrome or Brave
- All features from the HTML version work identically
- Searching opens website in WebView correctly
- Back button from website returns to home
- All settings save and restore on app restart
- Glassy mode looks premium
- Night tagline "Find the Unseen" glows subtly at night

---

## WHAT TO DELIVER
One PR or zip with these files added/modified:
- `ui/theme/AppTheme.kt`
- `ui/theme/Type.kt`
- `ui/theme/Color.kt`
- `data/StorageManager.kt`
- `ui/viewmodel/HomeViewModel.kt`
- `ui/screens/HomeScreen.kt`
- `ui/screens/WebViewScreen.kt`
- `ui/screens/IncognitoScreen.kt`
- `ui/components/StarFieldCanvas.kt`
- `ui/components/ParticleSystem.kt`
- `ui/components/EclipseOrb.kt`
- `ui/components/SearchBar.kt`
- `ui/components/QuickSites.kt`
- `ui/sheets/CustomizeSheet.kt`
- `ui/sheets/TabsSheet.kt`
- `ui/sheets/HistoryScreen.kt`
- `ui/sheets/BookmarksScreen.kt`
- `ui/sheets/AboutSheet.kt`
- `MainActivity.kt` (updated to host Compose)
- `build.gradle` (updated dependencies)

Build must compile with zero errors and zero warnings.
