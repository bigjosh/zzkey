package com.example.zzkeys

import android.content.Context

/**
 * Runtime source of the keyword list.
 *
 * Defaults come from [Keywords.ALL] (compiled in). The user can override them in
 * [MainActivity], which persists the list to SharedPreferences. The accessibility
 * service reads from here on every keystroke, so edits take effect immediately without
 * rebuilding the APK.
 *
 * Storage format is dead simple: one keyword per line. We parse on read so the stored
 * blob is exactly what the user typed in the editor.
 */
object KeywordStore {
    private const val PREFS = "zzkeys_prefs"
    private const val KEY = "keywords"

    /** Current keyword list — the user's saved list, or the built-in defaults. */
    fun load(context: Context): List<String> {
        val raw = prefs(context).getString(KEY, null) ?: return Keywords.ALL
        val parsed = parse(raw)
        return if (parsed.isEmpty()) Keywords.ALL else parsed
    }

    /** Persist the raw editor text. Parsing happens on the next [load]. */
    fun save(context: Context, text: String) {
        prefs(context).edit().putString(KEY, text).apply()
    }

    /** The current list rendered back as editor text, one keyword per line. */
    fun asText(context: Context): String = load(context).joinToString("\n")

    /** Split editor text into a clean keyword list: trimmed, blank lines dropped. */
    fun parse(text: String): List<String> =
        text.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
