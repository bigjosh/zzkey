# zzkeys

A minimal Android AccessibilityService that watches text fields and, when you type a
keyword starting with `zz` (e.g. `zzj`), shows matching keywords in a bar above the
keyboard. Tap one to autocomplete the in-progress word.

This is a personal prototype, meant to be sideloaded on your own device.

## Install on the phone (no PC)

Every push to `main` builds the app and publishes the APK to a GitHub Release. The
easiest install is the permanent direct link (always serves the newest build):

> **https://github.com/bigjosh/zzkey/releases/latest/download/app-debug.apk**

Open that on the phone — it downloads the raw `.apk` (not a zip), and Android offers to
install it (allow installing from this source if prompted). You can also browse to the
repo's **Releases** page and tap `app-debug.apk` under the latest release.

If you'd rather use the build artifact (e.g. for a branch build that doesn't publish a
release): **Actions** tab → open the run → **Artifacts** → `zzkeys-debug-apk`. That one
downloads as a zip you have to extract first.

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
