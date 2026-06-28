# HANDOFF — zzkeys

A handoff for a fresh Claude Code session. This document is the source of truth for
what this project is, why it's built the way it is, and what to do next.

## Goal

A personal Android tool that autocompletes custom keywords beginning with `zz`
(e.g. `zzjosh`, `zzgood`). The user types a prefix like `zzj` into ANY app's text
field; a bar above the keyboard shows matching keywords; tapping one replaces the
in-progress word with the full keyword.

Target device: Pixel 10 XL, latest Android. No backward-compatibility concerns.
Distribution: personal sideload only (not Play Store).

## Why this architecture (decisions already made — don't relitigate without reason)

The user and Claude worked through the options before building:

- **Full/forked IME (custom keyboard):** rejected — too big a scope; owning an entire
  keyboard (layouts, languages, gestures) just to add a suggestion bar.
- **Personal Dictionary shortcuts (`UserDictionary`), zero code:** rejected — shortcuts
  fire on the full shortcut and are NOT prefix-matched, so typing `zzj` does not surface
  `zzjosh`. Does not meet the prefix-autocomplete requirement.
- **`SpellCheckerService`:** rejected — only feeds the red-underline typo-correction path,
  not the live prediction strip; and Gboard blends/re-ranks your output with its own
  neural model, so you don't control ordering. Wrong mechanism for deterministic
  keyword expansion.
- **Overlay + AccessibilityService:** CHOSEN. It's the only approach that gives
  deterministic "prefix `zzj` → exactly these keywords, in my order, every time"
  behavior over arbitrary apps without writing a keyboard.

Key simplification: an `AccessibilityService` can BOTH read field text AND draw its own
bar via a `TYPE_ACCESSIBILITY_OVERLAY` window — so we do NOT need the separate
"draw over other apps" (`TYPE_APPLICATION_OVERLAY`) permission. One service, one
permission toggle.

## Build/distribution constraint

The user is on the phone with NO dev machine for now. The chosen build path is
**GitHub Actions**: push the repo, the workflow builds a debug APK, the user downloads
the artifact on the phone and sideloads it. The workflow deliberately avoids needing the
Gradle wrapper jar (which is hard to create from a phone) by using
`gradle/actions/setup-gradle` and running `gradle assembleDebug` directly.

If THIS session is running on a real machine with `gh` authenticated, you can instead
create the repo and push directly (`gh repo create ... --source=. --push`), then let
Actions build — or even build locally if the Android SDK is present.

## What exists (current state — working prototype, not yet device-tested by user)

```
zzkeys/
  settings.gradle
  build.gradle                         # AGP 8.5.2, Kotlin 1.9.24
  gradle.properties
  .gitignore
  README.md
  .github/workflows/build.yml          # builds debug APK, uploads as artifact
  app/
    build.gradle                       # namespace com.example.zzkeys, minSdk 30, target/compileSdk 35
    src/main/
      AndroidManifest.xml              # MainActivity + the accessibility service
      res/xml/accessibility_service_config.xml
      res/values/strings.xml
      java/com/example/zzkeys/
        Keywords.kt                    # THE keyword list — user edits this
        KeywordAccessibilityService.kt # core logic
        MainActivity.kt                # launcher: enable service + test EditText
```

### How the core logic works (KeywordAccessibilityService.kt)

1. Listens for `TYPE_VIEW_TEXT_CHANGED` and `TYPE_VIEW_TEXT_SELECTION_CHANGED`.
2. On each event, if `source.isEditable`, reads full text and cursor
   (`textSelectionEnd`, fallback to text length).
3. `currentToken()` walks back from the cursor to the last whitespace to get the
   in-progress word.
4. If the token is >=2 chars and starts with `zz`, prefix-filters `Keywords.ALL`.
5. Renders matches as buttons in a `TYPE_ACCESSIBILITY_OVERLAY` bar (horizontal,
   scrollable).
6. On tap, `applyKeyword()` rebuilds the full string with the token replaced by the
   keyword, writes it back with `ACTION_SET_TEXT`, then sets the cursor after the
   inserted keyword with `ACTION_SET_SELECTION`.
7. Logs `tag: zzkeys` on every keystroke (token + matches) for debugging.

## Known rough edges / TODO for next session

These are the things to fix/improve, roughly in priority order:

1. **Bar vertical position is hardcoded** (`y = 720` in `showBar()`). It needs to sit
   just above Gboard. Right approach: derive the keyboard top from window insets /
   the IME window bounds rather than a magic number. This is the most likely thing to
   look wrong on first run.
2. **`TYPE_VIEW_TEXT_CHANGED` is unreliable in some Jetpack Compose fields.** Plain
   `EditText` (and the app's own test box) are the reliable baseline. If broadening to
   apps like Google Tasks, verify events actually fire; may need
   `TYPE_WINDOW_CONTENT_CHANGED` as a fallback signal.
3. **No debounce.** Recompute is cheap (in-memory prefix filter) so it's fine now, but
   if keyword sourcing ever becomes async, add debounce.
4. **Bar styling is minimal** (plain buttons, dark background). Cosmetic.
5. **Keyword list is compile-time** (`Keywords.kt`). A nice enhancement: editable at
   runtime (store in SharedPreferences, edit in MainActivity) so the user doesn't have
   to rebuild to change keywords.
6. **Node recycling / lifecycle:** review `AccessibilityNodeInfo` handling for leaks;
   `lastEditable` is held across events.
7. **Password / non-editable fields** correctly produce no bar — keep that behavior.

## How to verify it works

1. Build (Actions artifact, or locally if SDK present) and install the debug APK.
2. Open the **zzkeys** app → enable **zzkeys autocomplete** in Accessibility settings.
3. Type `zzj` in the app's test EditText. Expect a bar with `zzjosh`, `zzjenny`.
4. Tap one → the token is replaced in-place, cursor lands after it.
5. `adb logcat -s zzkeys` (or on-device logcat) shows token + matches per keystroke if
   debugging detection issues.

## User context

The user is technically strong (embedded/hardware/firmware background) and on a Pixel
10 XL. Comfortable with code review; wants to understand and tweak what's built rather
than receive a black box. Keep changes legible and explain tradeoffs.
