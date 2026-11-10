# T-UI Launcher: Changelog & Bug Fixes (v6.15.1 – v6.15.10)

This document provides a comprehensive log of architectural enhancements, new features, and bug fixes introduced across releases **v6.15.1** through **v6.15.10** for the T-UI Launcher Android platform.

---

## 📌 Release Summary

| Version | Focus Area | Highlights |
|---|---|---|
| **v6.15.10** | **Inbuilt Native Macro Engine** | Native command recording, persistent `macros.json`, argument substitution (`$1`, `$*`), parameter passing, removal of external MacroDroid dependencies. |
| **v6.15.9** | **Speech & Tactile Haptic System** | Hands-free SpeechRecognizer input (`voice`, toolbar microphone), physical `HapticFeedbackConstants` haptic feedback on typing, navigation, and execution. |
| **v6.15.8** | **System Telemetry & ASCII Visualizer** | ASCII bar visualization (`sys -graph`), memory breakdown, battery percentage & charging status telemetry. |
| **v6.15.7** | **Unix Piping & Filter Chain Engine** | Command piping (`| grep`, `head`, `tail`, `sort`, `wc`, `upper`, `lower`), multi-stage CLI data transforms. |
| **v6.15.6** | **Architecture-Aware App Updater** | GitHub release asset checking (`tui -update_app`), device ABI detection (arm64-v8a, armeabi-v7a, x86_64), SHA-256 integrity verification. |
| **v6.15.5** | **Theme Presets & Palette Switcher** | Modern developer themes (`matrix`, `dracula`, `catppuccin`, `nord`, `gruvbox`) with hot-reloading. |
| **v6.15.4** | **Media Session Telemetry** | Active playback tracking (`music -now`), variables `{{media_track}}` & `{{media_playback_state}}`, notification listener integration. |
| **v6.15.3** | **Jetpack Compose MAD UI Overhaul** | Redesign of `PluginStudioActivity`, `AiChatActivity`, and `GridAppLauncherActivity` conforming to Material Design 3 (8dp grid, smooth 16dp rounded corners, accessible 48dp touch targets). |
| **v6.15.2** | **Plugin Architecture & Web Templates** | Plugin installer, manager, and GUI code studio; dynamic template fetching from GitHub Pages repository. |
| **v6.15.1** | **Modern Android Baseline** | Initial migration to Android 14 (API 34), edge-to-edge support, and build system stabilization. |

---

## 🚀 Version-by-Version Detailed Breakdown

### 🔹 v6.15.10 — Inbuilt Native Macro Engine & CLI Automation
- **Native Automation Engine (`MacroEngine.kt`)**:
  - Replaced dependence on third-party MacroDroid broadcasts with an internal high-performance automation runner.
  - Automates sequences of terminal commands with customizable delays (`sleep <ms>`) and diagnostic feedback (`echo <msg>`).
  - Stored in clean, human-readable JSON format at `macros.json` within the launcher directory.
- **Dynamic Recording & Management (`macro`, `macros`)**:
  - `-rec <name> [desc]`: Intercepts and records live interactive terminal commands into a recording buffer.
  - `-stop`: Finalizes and persists the recorded command sequence into `macros.json`.
  - `-cancel`: Discards the current recording buffer without modifying saved definitions.
  - `-status`: Displays active recording state, target macro name, and count of captured steps.
  - `-ls`: Formatted table showing all installed macros, execution count, and last run timestamp.
  - `-info <name>`: Inspects macro metadata, author, and each numbered command step.
  - `-run <name> [args...]`: Executes macro with positional parameters (`$1`, `$2`, `$*`, `$@`) and named keyword arguments (`key="val"`).
  - `-rm <name>`: Removes macro definition from persistent storage.
  - `-file`: Prints absolute path to the active `macros.json` file.
- **Preloaded Starter Macros**:
  - `night`: Dims screen brightness to 15%, posts notification, and shows toast confirmation.
  - `syscheck`: Visualizes hardware telemetry (`sys -graph`) and outputs battery status and charge level.
  - `clean`: Clears console output and alerts terminal readiness.
- **Execution History (`history`)**:
  - SQLite-backed audit log capturing macro name, timestamps, parameter bindings, and success/failure status.

---

### 🔹 v6.15.9 — Speech Recognition & Tactile Haptic Feedback
- **Hands-Free Speech-to-Text (`VoiceRecognitionManager.java` & `voice.java`)**:
  - Integrated Android `SpeechRecognizer` API for voice transcription directly into the terminal input buffer.
  - Added vector microphone icon (`ic_mic.xml`) and interactive toolbar button (`mic_view`).
  - Implemented long-press trigger on input prefix and Enter button for immediate voice capture.
- **Physical Haptic Feedback System**:
  - Tactile physical feedback powered by `HapticFeedbackConstants`:
    - `KEYBOARD_TAP`: Virtual keyboard keypresses, suggestions selection, and toolbar actions.
    - `VIRTUAL_KEY`: D-Pad arrow navigation and Enter key triggers.
    - `CONFIRM`: Command execution acknowledgment.
    - `LONG_PRESS`: Mic view and special shortcut activation.

---

### 🔹 v6.15.8 — System Telemetry & ASCII Data Visualizer
- **Hardware Telemetry Engine (`sys.java`)**:
  - Real-time CPU, RAM, Battery, Storage, and Uptime monitors.
  - Flag `sys -graph`: Generates ANSI/ASCII progress bars visualizing system resource allocation:
    - RAM utilization percentage and total vs available MB.
    - Storage breakdown for Internal vs External storage.
    - Battery level bar with charging status indicator.
- **Predefined System Variables**:
  - Added `{{battery_level}}` (integer percentage 0-100).
  - Added `{{battery_status}}` ("charging", "discharging", "full", "not charging").
  - Documented in `predefined-variables.txt` and `variable.xml`.

---

### 🔹 v6.15.7 — Unix Pipeline & Stream Filters Engine
- **Command Chaining & Pipes (`|`)**:
  - Enabled multi-command pipelines in `MainManager.java` allowing command output to feed into subsequent filters.
  - Supported filters:
    - `grep [-i] [-v] <pattern>`: Filters lines matching or excluding text.
    - `head [-n count]`: Truncates output to the first N lines (default 10).
    - `tail [-n count]`: Truncates output to the last N lines (default 10).
    - `sort [-r] [-n]`: Lexicographical or numerical sorting, ascending or descending.
    - `wc [-l] [-w] [-c]`: Counts lines, words, or character bytes.
    - `upper` / `lower`: Case conversion transformations.
  - Example: `apps -ls | grep -i music | sort`

---

### 🔹 v6.15.6 — Architecture-Aware Update Engine
- **Automated Update Checker (`tui -update_app`)**:
  - Queries GitHub Releases API for the latest published T-UI Launcher APKs.
  - Detects native device ABI (`Build.SUPPORTED_ABIS`) to match the appropriate split APK (e.g. `arm64-v8a`, `armeabi-v7a`, `x86_64`, `universal`).
  - Verifies downloaded package integrity against published SHA-256 checksums before prompting system PackageInstaller.

---

### 🔹 v6.15.5 — Developer Theme Switcher & Color Palettes
- **Built-in Modern Theme Presets**:
  - Added pre-configured color palettes selectable via `theme -apply <name>`:
    - `matrix`: Classic high-contrast phosphor green on deep black.
    - `dracula`: Popular dark purple, cyan, and pastel pink theme.
    - `catppuccin`: Soothing mocha palette with soft lavender highlights.
    - `nord`: Arctic-inspired icy blue and slate grey tones.
    - `gruvbox`: Warm retro autumnal palette.
  - Dynamic palette reloading without restarting launcher activity.

---

### 🔹 v6.15.4 — Media Session Telemetry & Tracking
- **Active Music Playback Monitor (`music.java`)**:
  - Added `music -now` command to inspect active `MediaSessionManager` state.
  - Extracts track title, artist, album, duration, and playback status.
  - Dynamic variable injection:
    - `{{media_track}}`: Current track name or "None".
    - `{{media_playback_state}}`: "Playing", "Paused", or "Stopped".

---

### 🔹 v6.15.3 — Modern Android Design (MAD) Jetpack Compose Overhaul
- **Material Design 3 Polish**:
  - Refactored `PluginStudioActivity`, `AiChatActivity`, and `GridAppLauncherActivity`:
    - Strict 8dp and 16dp spacing grid elimination of cramped layouts.
    - Smooth 16dp rounded card corners replacing harsh rectangular borders.
    - Accessible touch targets complying with WCAG AA (minimum 48dp x 48dp).
    - Shimmer skeleton loaders and Material 3 Snackbars for responsive UI states.

---

### 🔹 v6.15.2 — Extensible Plugin System & Studio
- **Plugin Infrastructure**:
  - Defined standard plugin package specification: `manifest.json`, `icon.png`, `start.kt`/`start.java`/`index.html`.
  - Added Plugin Installer (`plugin -install <zip|url>`), Manager (`plugin -ls`, `plugin -rm`), and Runner (`plugin -run <id>`).
  - Created graphical in-app Code Studio (`plugin -studio`) with live preview and remote template downloading from GitHub Pages.

---

### 🔹 v6.15.1 — Modern Android Baseline
- Upgraded target SDK to Android 14 (API 34) with min SDK 26.
- Implemented edge-to-edge window insets handling for modern gesture navigation bars and display cutouts.
- Optimized D8 dexing and ProGuard rules to ensure fast build compilation and minimal APK footprint.

---

## 🛠️ Bug Fixes & Stability Improvements (v6.15.1 – v6.15.10)

1. **MacroDroid Dependency Elimination**:
   - *Issue*: Macro execution failed on devices without MacroDroid installed or configured.
   - *Fix*: Created standalone `MacroEngine.kt` and `macros.json`, routing all macro recording and playback locally while retaining external broadcast capability as an optional fallback.

2. **D8 Dexing & Java 8 Language Desugaring**:
   - *Issue*: Lambda invocations from Java into Kotlin functional types caused compilation mismatches (`bad return type in lambda expression`).
   - *Fix*: Ensured explicit return of `kotlin.Unit.INSTANCE` and added `@JvmOverloads` on Kotlin companion functions.

3. **Terminal Pipeline Variable Resolution**:
   - *Issue*: Piped commands and alias expansions failed to substitute variables nested within quotation marks.
   - *Fix*: Integrated `VariablesManager.resolveVariables(...)` before tokenization and pipeline stage parsing.

4. **SpeechRecognizer Permissions & Lifecycle**:
   - *Issue*: SpeechRecognizer crashed when invoked repeatedly without releasing previous audio focus.
   - *Fix*: Added guarded singleton lifecycle management in `VoiceRecognitionManager.java`, ensuring audio recording permissions are validated and audio sessions are cleaned up upon stop or cancel.

5. **Terminal Haptic Throttling**:
   - *Issue*: High-speed rapid typing caused haptic motor queue saturation.
   - *Fix*: Filtered `HapticFeedbackConstants.KEYBOARD_TAP` triggers to text mutation events and key releases, ensuring crisp, responsive physical feedback without latency.

---
*Generated for T-UI Launcher v6.15.10 release engineering.*
