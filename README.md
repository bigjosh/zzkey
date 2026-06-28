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

Easiest: open the **zzkeys** app, edit the **Keywords** box (one per line), and tap
**Save keywords**. Changes take effect immediately — no rebuild, no reinstall.

The built-in defaults live in `app/src/main/java/com/example/zzkeys/Keywords.kt`; the
app only falls back to them until you save your own list.

## Known rough edges (it's a prototype)

- The bar now positions itself just above the keyboard by reading the IME window's
  bounds (`AccessibilityWindowInfo.TYPE_INPUT_METHOD`). If the keyboard window can't be
  found it falls back to the screen bottom.
- `TYPE_VIEW_TEXT_CHANGED` is inconsistent in some Jetpack Compose fields, so the service
  also listens for `TYPE_WINDOW_CONTENT_CHANGED` and resolves the focused input node as a
  fallback. Plain `EditText` apps (and the built-in test box) remain the reliable baseline.
- Password fields won't expose text — by design.
