# Awaytime ⏳✨

> **Track the time you spend living your life, away from your phone screen.**

Awaytime is an Android widget & companion app inspired by the clean, minimal aesthetic of Nothing OS and modern Material 3 design. It brings phone-free time directly to your home screen with real-time tracking, 24-hour visual interval timelines, and weekly activity charts.

---

## 📸 Overview & Widget Showcases

| Small Away Widget (2x2 / 2x1) | Weekly Away Widget (4x3) | Companion App Interface |
|---|---|---|
| Displays today's hours & minutes away, a 24h timeline bar with live cursor, and theme sparkle badge. | Features weekly total away time, day average pill, and a 7-day dotted activity chart. | Dark theme ("n0widgets" style) with pastel squircle badges, live previews, and floating bottom navigation. |

---

## ✨ Features

- **Small Away Widget (2x2 / 2x1)**:
  - Real-time time away from phone (e.g. `8h 19m`).
  - **Dynamic 24h Timeline Bar**: Generated dynamically via custom Canvas bitmap rendering, showing off-screen blocks vs on-screen usage intervals with a real-time cursor indicator.
  - **8-Point Sparkle Badge**: Minimal status star with customizable accent colors (Cyber Blue, Mint Green, Coral Peach, Bubble Pink, Sun Yellow).
- **Weekly Away Widget (4x3 / 4x2)**:
  - Displays total weekly away time (e.g. `30 hr 28 min`) and date range.
  - **Day Average Pill**: Displays average hours away per day (e.g. `11 hr 19 min`).
  - **7-Day Dot Chart**: Dotted vertical guideline columns with floating data points representing daily offline hours.
  - Radiant blue gradient or pitch black OLED theme.
- **Companion Application (Image 4 Aesthetic)**:
  - Deep dark background (`#0C0E12`) with high-contrast typography.
  - Rounded configuration cards with pastel icon squircles:
    - ⏱ **Daily Away** (`#CBE5FF`): Daily statistics and timeline options.
    - 📊 **Weekly Overview** (`#FFE3C2`): Weekly totals and 7-day chart metrics.
    - ⏳ **Focus & Streaks** (`#FFD4DF`): Live offline streak and interval tracker.
    - 🎯 **Daily Goals** (`#D4F4DF`): Custom detox targets (e.g. 8h, 10h, 12h away).
    - 🎨 **Styles & Accents** (`#FFF0B3`): OLED Pitch Black, Radiant Blue, Nothing Mono, and Muted Slate themes.
    - 📉 **Timeline & Intervals** (`#D0F2ED`): Hourly screen-off distribution.
    - 🛡 **Tracking & Permissions** (`#E3D7FF`): Usage Access permission status and diagnostics.
  - **Live Home Screen Preview**: Preview changes in real time before adding to your home screen.
  - **One-Tap Pin Widget**: Add widgets directly to your home screen using `AppWidgetManager.requestPinAppWidget()`.
  - **Floating Bottom Navigation Bar**: Styled with a rounded floating pill navigation (`Settings`, `Widget`, `Walls`).

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 2.2+ / Java 21
- **UI Toolkit**: Jetpack Compose & Material 3
- **Widgets**: Android AppWidget framework with `RemoteViews` and anti-aliased dynamic Canvas bitmaps
- **Tracking Engine**: Android `UsageStatsManager` (`SCREEN_INTERACTIVE` / `SCREEN_NON_INTERACTIVE` events) + screen state broadcast receivers
- **Build System**: Android Gradle Plugin (AGP) 9.0+

---

## 📲 Installation

### Direct APK Download
You can find the ready-to-install debug APK located in the repository:
```
release/Awaytime-v1.0.0.apk
```
Transfer the APK to your Android device or install via ADB:
```bash
adb install release/Awaytime-v1.0.0.apk
```

### Build from Source
```bash
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
MIT License.
