# ECLIPSE BROWSER — DEVELOPER SPECIFICATION
## Current Build: Dawn 0.1 (Polish & Stabilization Phase)

---

## WHAT THIS DOCUMENT IS

This document defines **exactly what to build right now** for the Eclipse browser app (Android), along with the overall product vision for context. Read the full document before writing a single line of code.

---

## 1. WHAT IS ECLIPSE

Eclipse is an Android browser app built using **Android WebView**. It is not a Chrome clone or a lightweight privacy browser. It is a **fully customizable digital platform** — combining browsing, search, AI assistance, widgets, and deep UI personalization — all free, forever, with no premium tiers.

**Architecture:**
- Native Android shell written in Kotlin/Java
- UI layer rendered in HTML/CSS/JavaScript inside WebView
- External APIs for search, AI, weather, and space data

**Tech Stack (all free-tier or open source):**

| Feature | Technology |
|---|---|
| Browser engine | Android WebView |
| Search | SearXNG (self-hosted) — **currently skipped, see Section 3** |
| AI answers | Groq API (Llama models) |
| Weather | OpenWeatherMap API |
| Space data | NASA API |
| Community storage | Supabase (future) |
| Backend logic | Cloudflare Workers (future) |
| User settings | Device localStorage |

**Core Promises (non-negotiable):**
1. No user data is ever sold
2. No paid features — everything is free, forever
3. Users own their full experience

---

## 2. OVERALL ROADMAP (Context Only — Do Not Build Yet)

The full Eclipse roadmap spans 5+ years across 6 release series:

| Series | Timeline | Focus |
|---|---|---|
| **Dawn 0.1 – 0.5** | Now – 2 months | Fix, polish, stabilize |
| Umbra 1.0 – 1.5 | 2 – 6 months | Core features, first public launch |
| Penumbra 2.0 – 2.4 | 6 – 12 months | Full drag-and-drop customization |
| Corona 3.0 – 3.4 | 1 – 1.5 years | Community layer |
| Totality 4.0 – 4.3 | 1.5 – 3 years | Independent search engine |
| Sirius 5.0 – 5.2 | 3 – 5 years | Multi-platform, voice, launcher |

**Build philosophy: One clean release at a time. Slow. Precise. Stable.**

---

## 3. WHAT TO BUILD RIGHT NOW — DAWN 0.1

### 3A. Scope of This Phase

**Goal:** Polish and stabilize the existing working app. This is a refinement phase — not a feature expansion phase.

The base codebase already exists. Read it carefully before making any changes.

### 3B. What Is Explicitly OUT OF SCOPE (Do Not Build)

| Feature | Status | Reason |
|---|---|---|
| SearXNG search integration | ❌ SKIP | Hosting is not available right now. Keep the existing search engine as-is. Do not modify search behavior. |
| Music player UI/system | ❌ REMOVE | Fully remove from UI and all backend logic |
| News feed layout/system | ❌ REMOVE | Fully remove from UI and all backend logic |
| Any Umbra/Penumbra/Corona features | ❌ NOT YET | Future phases only |

### 3C. Required Changes for Dawn 0.1

These are the only things to build in this phase:

#### Fix 1 — Splash Screen Redesign
- Completely redesign the splash screen
- Theme: space-inspired, premium feel
- Must include animated Eclipse logo
- No generic gradients or flat colors — this sets the tone for the whole app

#### Fix 2 — Greeting Text
- Dynamic greeting based on time of day:
  - 5:00 AM – 11:59 AM → **"Good Morning, Explorer"**
  - 12:00 PM – 4:59 PM → **"Good Afternoon, Explorer"**
  - 5:00 PM – 8:59 PM → **"Good Evening, Explorer"**
  - 9:00 PM – 4:59 AM → **"Good Night, Explorer"**
- Text must animate in smoothly (fade + subtle slide up)

#### Fix 3 — Remove Music System
- Remove all music player UI components from home screen and settings
- Remove all music-related backend code and state management
- No trace of the music system should remain anywhere in the codebase

#### Fix 4 — Remove News Layout System
- Remove the news section layout and any associated backend/API calls
- Remove news from home screen entirely
- No trace of news feed architecture should remain

#### Fix 5 — Bottom Navigation Bar
- Bottom nav bar must remain stable and visible while browsing websites
- It must never be covered by or hidden behind website content
- Correct overlapping/z-index issues

#### Fix 6 — Home Button Alignment
- Fix the home button cutoff or misalignment in the nav bar
- All nav icons must be properly centered and fully visible

#### Fix 7 — Menu Expansion
- Expand the app menu with properly structured options
- Items must be logically grouped
- Menu must open and close with a smooth animation

#### Fix 8 — Eclipse Logo Integration
- The Eclipse logo must be properly embedded in the app
- Correct placement in splash screen, top bar, and about section
- No placeholder icons or missing asset references

---

## 4. DESIGN REQUIREMENTS — NON-NEGOTIABLE

These rules apply to every single screen, animation, and UI element.

### 4A. What the UI Must NOT Feel Like
- AI-generated or template-based
- "Vibe coded" — rushed, inconsistent, or generic
- Copy-pasted from stock UI kits without customization
- Choppy, janky, or unresponsive

### 4B. What the UI MUST Feel Like
- **Handcrafted and intentional** — every spacing decision should be deliberate
- **Premium and cohesive** — consistent visual language across all screens
- **Alive** — subtle motion everywhere, nothing is static without purpose

### 4C. Animation Standards

| Property | Requirement |
|---|---|
| Easing | Use intentional curves — `cubic-bezier(0.16, 1, 0.3, 1)` or similar spring-feel curves. Never use `linear`. |
| Timing | UI transitions: 250–400ms. Micro-animations: 150–250ms. Nothing feels instant or sluggish. |
| Scroll | Scroll-triggered animations on list items and cards (fade + translate) |
| Motion | Subtle ambient motion on home screen (particles, stars, gradients shifting) |
| Priority | Smoothness and polish are more important than file size or code brevity |

### 4D. Glassy UI System

A full glassmorphism (Glassy UI) system must be implemented and toggleable:

- Backdrop blur on all panels, cards, and overlays
- Layered transparency with depth
- Adjustable blur intensity (user-controlled slider)
- Light refraction feeling — surfaces should not look flat
- No visual artifacts or bleed on any background type
- Works correctly over both dark and animated backgrounds

### 4E. Development Rules
- **Go slow.** Write complete, thorough code. Do not abbreviate or shortcut.
- **File size does not matter.** Code clarity and quality matter.
- **Test after every major change.** Do not stack multiple untested changes.
- **No instability.** Crashes, layout breaks, and visual glitches are not acceptable in any delivered build.

---

## 5. HOME SCREEN — CURRENT EXPECTED STATE (Post Dawn 0.1)

After all Dawn 0.1 fixes are applied, the home screen should contain:

- **Dynamic greeting** (time-based, animated)
- **Clock widget** (styled, prominent)
- **Weather widget** (real data via OpenWeatherMap API)
- **Quick access sites** (user-defined shortcuts)
- **Search bar** (uses existing search engine — do not change)
- **Animated background** (star field or space particle system — smooth, not heavy)
- **Eclipse Island** (pill-shaped element at top — idle state shows animated logo)

**Not on home screen:** Music player, news feed (both removed in this phase)

---

## 6. ECLIPSE ISLAND (Top Dynamic Element)

A persistent pill-shaped interactive element at the top of the screen.

**Behavior by state:**

| State | Display |
|---|---|
| Idle | Small animated Eclipse logo |
| Download in progress | Progress bar with filename |
| Search loading | Subtle loading animation |
| Timer running | Countdown display |
| Weather alert | Expands with weather icon and message |

**Note:** Music state has been removed. Do not include it.

Implementation must feel native and smooth — inspired by the iOS Dynamic Island concept, adapted for Android/WebView.

---

## 7. EXISTING SEARCH BEHAVIOR — DO NOT CHANGE

The current search functionality uses an existing engine. **Leave it completely as-is.** The SearXNG integration planned for Dawn 0.2 is postponed. Do not modify, break, or replace the current search engine in this phase.

Fallback/backup search sources (for future reference, not this phase):
- DuckDuckGo instant API
- Wikipedia API
- Reddit JSON API
- HackerNews API

---

## 8. SETTINGS PANEL — REQUIRED TO WORK

The following settings must save correctly to localStorage and restore on app open:

- Selected theme (Eclipse Space, Nebula, Aurora, Pure Void)
- Accent color selection
- Animation toggle (on/off for battery saving)
- Glassy UI toggle
- Blur intensity (if Glassy UI is on)
- Visual effects toggles

Settings panel must open and close smoothly. All controls must feel premium — no raw checkboxes, default selects, or unstyled inputs.

---

## 9. VOID MODE (Incognito)

Void Mode is a completely separate browsing context:
- Separate WebView instance — zero crossover with normal session
- No history stored
- No cookies
- No cache
- UI switches to a **purple-tinted theme** whenever Void Mode is active — the user always knows they're in it
- Bottom nav displays a Void Mode indicator badge

---

## 10. DELIVERY REQUIREMENTS

When delivering this build:

- Submit as a complete, structured ZIP matching the format of the previously shared file
- Every file must be in its correct directory — no loose files
- The app must be fully functional from first launch
- No commented-out debug code or placeholder content
- No broken imports or missing assets
- Dawn 0.1 changes must all be implemented and verified

---

## SUMMARY — WHAT TO DO IN THIS PHASE

| Task | Action |
|---|---|
| Read the existing codebase | ✅ Do first, before any changes |
| Remove music system | ✅ Full removal, UI + backend |
| Remove news layout system | ✅ Full removal, UI + backend |
| Redesign splash screen | ✅ Space theme, premium, animated |
| Fix dynamic greeting | ✅ Time-based, smooth animation |
| Fix bottom nav stability | ✅ Always visible while browsing |
| Fix home button alignment | ✅ Properly centered in nav bar |
| Expand menu with proper structure | ✅ Grouped options, smooth animation |
| Integrate Eclipse logo correctly | ✅ Splash, top bar, about page |
| SearXNG integration | ❌ Skip — not this phase |
| Any Umbra or later features | ❌ Skip — not this phase |

---

*Eclipse — Not just a browser. Your world.*
