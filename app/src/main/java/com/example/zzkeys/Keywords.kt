package com.example.zzkeys

/**
 * Built-in DEFAULT keyword list, compiled into the app and used on first run.
 *
 * You normally don't need to edit this anymore: keywords are editable at runtime in the
 * app (MainActivity), persisted via [KeywordStore], and picked up by the service
 * immediately with no rebuild. Editing here only changes the defaults shipped in the APK.
 */
object Keywords {
    val ALL = listOf(
        "zzjosh",
        "zzgood",
        "zzjenny",
        "zzthanks",
        "zzaddress",
        "zzemail",
    )
}
