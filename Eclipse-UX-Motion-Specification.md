# ECLIPSE BROWSER — UX, Motion & Feel Specification

This document defines exactly how Eclipse Browser must look, move, and feel.
Every developer or AI working on this app must read this before writing one line of UI code.
"It looks good" is not acceptable. Every pixel, every animation, every interaction is defined here.

---

## THE FEELING WE ARE BUILDING

Eclipse must feel like this:
- Open the app → feels like stepping into space
- Scroll the home screen → content floats, not snaps
- Tap anything → instant response, never feels stuck
- Panels open → slide up like they have weight
- Switches toggle → satisfying, springy
- Search bar focus → the whole screen breathes

Reference: Nothing Phone UI, Linear App, Apple iOS 17 springboard.
Not reference: Old Android apps, Material 2, anything that feels "webview-ish"

---

## 1. MOTION SYSTEM

### The One Rule
Every single animation in Eclipse uses springs, not linear or ease curves.
Springs feel alive. Linear feels like a loading bar. Never use linear.

### Spring Presets — use these exact values everywhere

```kotlin
object EclipseSpring {

  // Standard — most UI elements
  val standard = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,  // 0.5f
    stiffness    = Spring.StiffnessMedium             // 400f
  )

  // Gentle — large panels, bottom sheets
  val gentle = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,  // 0.75f
    stiffness    = Spring.StiffnessLow            // 200f
  )

  // Snappy — toggles, chips, small buttons
  val snappy = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,  // 1.0f
    stiffness    = Spring.StiffnessHigh          // 1000f
  )

  // Float — orb, background elements
  val float = spring<Float>(
    dampingRatio = 0.6f,
    stiffness    = 120f
  )
}
```

### Entry Animations — every screen element animates in

```kotlin
// Every item that enters the screen uses this
@Composable
fun EclipseEnterAnimation(
  index: Int = 0,         // stagger index — item 0 enters first
  content: @Composable () -> Unit
) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay(index * 55L)    // 55ms stagger between items
    visible = true
  }
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(400)) + slideInVertically(
      animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
      initialOffsetY = { (it * 0.28f).toInt() }
    )
  ) { content() }
}
```

Usage — wrap every home screen section:
```kotlin
EclipseEnterAnimation(index = 0) { TopBar() }
EclipseEnterAnimation(index = 1) { HeroBlock() }
EclipseEnterAnimation(index = 2) { SearchBar() }
EclipseEnterAnimation(index = 3) { FilterTabs() }
EclipseEnterAnimation(index = 4) { QuickSites() }
```

### Bottom Sheet Entry
```kotlin
// All panels (Customize, History, Bookmarks, Tabs, Menu) open with this
val sheetEnter = slideInVertically(
  animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
  initialOffsetY = { it }
) + fadeIn(tween(280))

val sheetExit = slideOutVertically(
  animationSpec = tween(220, easing = FastOutLinearInEasing),
  targetOffsetY = { it }
) + fadeOut(tween(180))
```

### Scale Press — every tappable element
```kotlin
// Apply to every Button, Card, SiteIcon, Chip
@Composable
fun Modifier.eclipseClickable(onClick: () -> Unit): Modifier {
  val scale = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()
  return this
    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
    .pointerInput(Unit) {
      detectTapGestures(
        onPress = {
          scope.launch { scale.animateTo(0.88f, EclipseSpring.snappy) }
          tryAwaitRelease()
          scope.launch { scale.animateTo(1f, EclipseSpring.standard) }
          onClick()
        }
      )
    }
}
```

---

## 2. SCROLLING

### Home Screen Scroll Rules
- Use `LazyColumn` — never `Column` in a `ScrollState`
- `flingBehavior = rememberSnapFlingBehavior(...)` — NO. Free scroll, no snapping
- `overscrollEffect` — disable the glow, use custom subtle elastic overscroll

### Elastic Overscroll (custom, replaces Android glow)
```kotlin
// When user pulls past top or bottom, content stretches slightly then snaps back
// Use this modifier on the LazyColumn
fun Modifier.elasticOverscroll(): Modifier = composed {
  val overscrollY = remember { Animatable(0f) }
  // On overscroll drag: move content by drag * 0.3 (resistance)
  // On release: spring back to 0 with gentle spring
  graphicsLayer { translationY = overscrollY.value }
}
```

### Scroll Parallax on Hero
As user scrolls down, the hero content (orb + greeting) moves up at 0.5x speed:
```kotlin
val scrollState = rememberLazyListState()
val heroOffset by remember {
  derivedStateOf { scrollState.firstVisibleItemScrollOffset * 0.5f }
}
// Apply to hero block:
.graphicsLayer { translationY = -heroOffset }
```

### Section Fade on Scroll
Each section fades slightly as it leaves the top of the screen:
```kotlin
val alpha by remember {
  derivedStateOf {
    val offset = scrollState.firstVisibleItemScrollOffset
    (1f - (offset / 300f)).coerceIn(0f, 1f)
  }
}
```

---

## 3. HOME SCREEN LAYOUT

### Exact Spacing (no guessing)
```
Top padding:     statusBarHeight + 12dp
TopBar height:   52dp
Hero block:      padding 18dp horizontal, 14dp top, 18dp bottom
Search wrap:     padding 0 18dp 10dp
Filter tabs:     padding 0 18dp 12dp
Sites section:   padding 4dp 18dp 14dp
Widget zone:     padding 0 18dp 20dp, gap 12dp between widgets
```

### Hero Block Layout
```
Row (horizontally space-between):
  Left column:
    - Date label        — Space Mono, 10sp, #FFFFFF38, letter-spacing 2.5sp, uppercase
    - Greeting          — Outfit 300, 26sp, white
      "Good [ACCENT]Evening[/ACCENT], Explorer"
    - Clock             — Space Mono, 12sp, #FFFFFF66, letter-spacing 2sp
    - Night tagline     — only visible 21:00–04:59 (see Night Mode below)
    - Weather inline    — only visible if weather widget enabled
  Right side:
    - Eclipse Orb       — 110dp × 110dp, floating animation
```

### Eclipse Orb — floating animation
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val floatY by infiniteTransition.animateFloat(
  initialValue = 0f,
  targetValue  = -7f,
  animationSpec = infiniteRepeatable(
    animation = tween(3500, easing = FastOutSlowInEasing),
    repeatMode = RepeatMode.Reverse
  )
)
// Apply: .graphicsLayer { translationY = floatY.dp.toPx() }
```

---

## 4. NIGHT MODE — "Find the Unseen"

Between 21:00 and 04:59 the home screen subtly transforms:

### Night Tagline
Appears below the clock. Small, mysterious, glowing.

```kotlin
if (isNight) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text("·", color = accentColor.copy(alpha = 0.5f), fontSize = 18.sp)
    Spacer(Modifier.width(8.dp))
    // Animated text — alpha and letter spacing pulse slowly
    val alpha by infiniteTransition.animateFloat(
      initialValue = 0.4f, targetValue = 0.85f,
      animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    Text(
      text = "FIND THE UNSEEN",
      fontFamily = SpaceMono,
      fontSize = 10.sp,
      letterSpacing = 3.sp,
      color = Color.White.copy(alpha = alpha),
      // Subtle glow via shadow
      style = TextStyle(
        shadow = Shadow(color = accentColor.copy(alpha = 0.4f), blurRadius = 12f)
      )
    )
    Spacer(Modifier.width(8.dp))
    Text("·", color = accentColor.copy(alpha = 0.5f), fontSize = 18.sp)
  }
}
```

### Night star density
At night, spawn 180 stars instead of 120. Slightly brighter.

---

## 5. SEARCH BAR BEHAVIOR

### Focus state — the whole bar breathes
```kotlin
val borderColor by animateColorAsState(
  targetValue = if (focused) accentColor else Color.White.copy(alpha = 0.09f),
  animationSpec = tween(300)
)
val glowAlpha by animateFloatAsState(
  targetValue = if (focused) 0.12f else 0f,
  animationSpec = tween(300)
)
// Box shadow equivalent:
Modifier.drawBehind {
  drawRoundRect(
    color = accentColor.copy(alpha = glowAlpha),
    cornerRadius = CornerRadius(18.dp.toPx()),
    style = Stroke(width = 12.dp.toPx())
  )
}
```

### Search button press
Scale 1.0 → 0.82 → 1.0 with snappy spring.
Color briefly brightens then returns.

---

## 6. FILTER TABS

Horizontal scrollable row. No scrollbar.

Active tab state:
```kotlin
val backgroundAlpha by animateFloatAsState(if (active) 1f else 0f, tween(250))
val textColor by animateColorAsState(if (active) accentColor else mutedColor, tween(250))
```

When tab switches:
- Previous tab: background fades out
- New tab: background fades in + scale 0.92 → 1.0

---

## 7. QUICK SITES GRID

4-column grid. Each site icon:

### Press animation
```kotlin
.eclipseClickable { openUrl(site.url) }
// Background glow appears on press
Box(
  Modifier
    .size(60.dp)
    .clip(RoundedCornerShape(18.dp))
    .background(surfaceColor)
    .border(1.dp, borderColor, RoundedCornerShape(18.dp))
) {
  // favicon image
}
```

### Long press — remove dialog
On long press (650ms): show a confirmation dialog with scaleIn animation.
Dialog has two buttons: Cancel (ghost) and Remove (danger red).

### Add site — "+" icon at end of grid
Tapping: Modal slides up with two fields — URL and Name.
URL field auto-formats: adds `https://` if missing.
Name field auto-fills from URL hostname if left empty.

---

## 8. BOTTOM SHEETS — general rules

All sheets (Customize, History, Tabs, Menu, About):

### Structure
```
Overlay (semi-transparent scrim, blur behind)
  └── Sheet (slides up from bottom)
        ├── Handle bar (40dp wide, 4dp tall, centered)
        ├── Header (title + close button)
        └── Content (scrollable)
```

### Drag to dismiss
User can drag sheet down to dismiss it. Use `AnchoredDraggable`:
- Dragging: sheet follows finger
- Release at < 40% height: snap back up with spring
- Release at > 40% height: dismiss with slide down

### Scrim
```kotlin
val scrimAlpha by animateFloatAsState(
  targetValue = if (visible) 0.6f else 0f,
  animationSpec = tween(300)
)
Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
```
Scrim also blurs the content behind it using `BlurMaskFilter` or `RenderEffect`.

---

## 9. CUSTOMIZE PANEL

### Accent color swatches
7 color swatches in a horizontal wrap row.
Active swatch:
```kotlin
val scale by animateFloatAsState(if (active) 1.2f else 1f, EclipseSpring.standard)
val borderWidth by animateDpAsState(if (active) 2.dp else 0.dp, EclipseSpring.snappy)
// White border on active, no border on inactive
```

When color changes:
- All accent-colored elements in the app animate to new color simultaneously
- Use `animateColorAsState` on all accent usages

### Theme selection
Each theme option row:
- Background fades in/out: `animateColorAsState`
- Checkmark animates in with `scaleIn + fadeIn`

### Toggle switches
```kotlin
val thumbOffset by animateFloatAsState(
  targetValue = if (on) 20f else 0f,
  animationSpec = EclipseSpring.snappy
)
val trackColor by animateColorAsState(
  targetValue = if (on) accentColor else surfaceColor,
  animationSpec = tween(250)
)
```

---

## 10. WEBVIEW SCREEN (browsing)

When user navigates to a website:
- Home screen does NOT reload — it stays in memory
- WebView slides in from right: `slideInHorizontally { it }`
- Back button: WebView slides out to right, home screen is already there

### URL bar at top (shown while browsing)
```
[← Back] [Site favicon + domain              ] [✕ Close]
```
- Slides down from top when page loads: `slideInVertically { -it }`
- Domain text fades in after page title loads
- Progress indicator: thin accent-colored line at very top, 2dp tall

### Page load progress
```kotlin
// Thin line at very top of screen
Box(
  Modifier
    .fillMaxWidth(progress)  // animates from 0f to 1f
    .height(2.dp)
    .background(
      Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.4f)))
    )
)
```

---

## 11. VOID MODE (Incognito)

When entering Void Mode:
- Screen fades to black first (200ms)
- Then Void UI fades in from center (300ms)
- Color scheme switches to purple (#7C00FF) for all accents

The Void icon (SVG orb equivalent):
- Drawn on Canvas in Compose
- Soft purple radial glow behind it
- Floating animation same as main orb but slower (10 second cycle)
- Purple ring orbiting it at slight angle (animated rotation, 20 second full cycle)

Exiting Void Mode:
- Fade to black (150ms)
- Clear all incognito WebView data
- Home screen fades back in (300ms)

---

## 12. GLASSMORPHISM MODE

When user selects "Glassy" in Customize:

For Android API 31+:
```kotlin
Modifier.graphicsLayer {
  renderEffect = RenderEffect
    .createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
    .asComposeRenderEffect()
}
.background(Color(0x8C0C0C18))
.border(1.dp, Color(0x1FFFFFFF), shape)
```

For Android API < 31 (fallback):
```kotlin
.background(Color(0xCC0C0C18))
.border(1.dp, Color(0x1AFFFFFF), shape)
```

Detection:
```kotlin
val isGlassSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
```

---

## 13. TYPOGRAPHY RULES

```kotlin
// Space Mono — data, labels, times, codes, small caps
// Outfit — everything else

// Hierarchy:
// 28sp Outfit 300  → greeting main
// 22sp Outfit 500  → section titles
// 18sp Outfit 400  → body text
// 15sp Outfit 400  → secondary body
// 13sp Outfit 400  → captions
// 12sp Space Mono  → clock, dates, version, labels
// 11sp Space Mono  → uppercase section headers (+ letter-spacing 2sp)
// 10sp Space Mono  → tiny metadata

// NEVER use:
// font-weight 700 (bold) for anything except the ECLIPSE brand label
// font-size below 10sp
// all-caps on body text (only on Space Mono labels)
```

---

## 14. COLOR RULES

```kotlin
// Backgrounds (darkest to lightest)
val bg          = Color(0xFF000000)  // pure black
val surface     = Color(0x0EFFFFFF)  // cards, inputs
val surface2    = Color(0x17FFFFFF)  // hover, pressed states
val border      = Color(0x1AFFFFFF)  // subtle borders
val border2     = Color(0x0FFFFFFF)  // very subtle dividers

// Text
val textPrimary = Color(0xFFF0F0F0)  // main text
val textMuted   = Color(0x66FFFFFF)  // secondary text
val textMuted2  = Color(0x38FFFFFF)  // placeholders, hints

// Never use pure white (#FFFFFF) for text — always use textPrimary
// Never use pure black for surfaces — use bg
// Accent color comes from user settings, default #FF6B1A
```

---

## 15. TOAST / FEEDBACK

Every user action that changes data must show a toast.
Examples:
- Bookmark added → "★ Bookmarked"
- Site removed → "Removed"
- Theme changed → (no toast — visual change is enough)
- History cleared → "History cleared"
- Copy URL → "URL copied"

```kotlin
// Toast appears 82dp above bottom nav
// Slides up from 20dp below its final position
// Stays for 2200ms
// Fades + slides down on exit

// Custom composable, not Android Toast API
@Composable
fun EclipseToast(message: String, visible: Boolean) {
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically { 20 } + fadeIn(tween(280)),
    exit  = slideOutVertically { 20 } + fadeOut(tween(200))
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xF5141423),
      border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
      tonalElevation = 8.dp
    ) {
      Text(message, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), fontSize = 13.sp)
    }
  }
}
```

---

## 16. LOADING STATES

Every async operation that takes > 200ms must show a loading state.
Never show a blank screen or leave the user wondering.

```kotlin
// Shimmer effect for loading cards
@Composable
fun ShimmerBox(modifier: Modifier) {
  val shimmerColors = listOf(
    Color.White.copy(alpha = 0.04f),
    Color.White.copy(alpha = 0.10f),
    Color.White.copy(alpha = 0.04f)
  )
  val transition = rememberInfiniteTransition()
  val translateX by transition.animateFloat(
    initialValue = -300f, targetValue = 300f,
    animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing))
  )
  Box(modifier.background(
    Brush.horizontalGradient(shimmerColors, startX = translateX, endX = translateX + 300f)
  ))
}
```

Use shimmer for:
- Weather widget loading
- NASA image loading
- Crypto prices loading
- Quick sites favicon loading

---

## 17. EMPTY STATES

When a list has no items, never show blank space. Show:
```
[Icon]
Title
Subtitle
```

Examples:
- No bookmarks: 🔖 "No bookmarks yet" "Start exploring and save your favorites"
- No history: ⏱ "Fresh start" "Your browsing history will appear here"
- Search results loading: spinner with accent color

---

## FINAL CHECKLIST BEFORE SHIPPING ANY SCREEN

- [ ] Every element animates in (no instant appearance)
- [ ] Every tap gives visual feedback (scale press)
- [ ] Every async operation has loading state
- [ ] Every empty list has empty state
- [ ] Every action has toast feedback
- [ ] Scrolling feels smooth (no jank, LazyColumn used)
- [ ] Springs used everywhere (no linear/ease animations)
- [ ] Night tagline shows only at night
- [ ] Glass mode works without crashing on API < 31
- [ ] Accent color change reflects everywhere instantly
- [ ] No hardcoded colors (use Color tokens)
- [ ] No hardcoded strings (use constants or resources)
- [ ] Back navigation works correctly on every screen
