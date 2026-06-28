# zzkeys

A minimal Android AccessibilityService that watches text fields and, when you type a
keyword starting with `zz` (e.g. `zzj`), shows matching keywords in a bar above the
keyboard. Tap one to autocomplete the in-progress word.

This is a personal prototype, meant to be sideloaded on your own device.

## Build it with no PC

1. Create a new GitHub repo and add all these files (use **Add file → Upload files**;
   you can select multiple at once from your phone).
2. GitHub Actions runs automatically on push. Or go to the **Actions** tab →
   **Build APK** → **Run workflow**.
3. When it finishes (green check), open the run → **Artifacts** → download
   `zzkeys-debug-apk`. It downloads as a zip; your phone's file manager can extract
   `app-debug.apk`.
4. Tap the APK to install (allow installing from this source if prompted).

## Turn it on

1. Open the **zzkeys** app → **Open Accessibility settings**.
2. Enable **zzkeys autocomplete**. Android will warn it can read screen content —
   that's expected; it needs the field text to do prefix matching.
3. Back in the app, type in the test box. Try `zzj`.

## Change the keywords

Edit `app/src/main/java/com/example/zzkeys/Keywords.kt`, commit, and the Action
rebuilds a fresh APK.

## Known rough edges (it's a prototype)

- The bar's vertical position is a fixed offset (`y = 720` in the service). If it
  overlaps or floats away from Gboard on your screen, tune that number.
- `TYPE_VIEW_TEXT_CHANGED` is inconsistent in some Jetpack Compose fields. Plain
  `EditText` apps (and the built-in test box) are the reliable place to start.
- Password fields won't expose text — by design.
