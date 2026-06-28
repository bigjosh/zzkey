package com.example.zzkeys

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var keywordsBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Left/right padding only; top/bottom come from window insets below so the
            // content clears the status bar, navigation bar and (when open) the keyboard.
            setPadding(48, 24, 48, 24)
        }

        root.addView(TextView(this).apply {
            text = "zzkeys"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        })

        root.addView(TextView(this).apply {
            text = "1. Tap \"Open Accessibility settings\" and enable \"zzkeys autocomplete\".\n" +
                "2. Come back here and type in the test box. Try \"zzj\".\n" +
                "3. Tap a suggestion in the bar above the keyboard.\n\n" +
                "Edit your keywords below (one per line) and tap Save — no rebuild needed."
            textSize = 16f
        })

        root.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(TextView(this).apply {
            text = "Keywords (one per line):"
            textSize = 14f
            setPadding(0, 40, 0, 8)
        })

        keywordsBox = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            gravity = Gravity.TOP or Gravity.START
            minLines = 4
            setText(KeywordStore.asText(this@MainActivity))
            setPadding(24, 24, 24, 24)
        }
        root.addView(keywordsBox)

        root.addView(Button(this).apply {
            text = "Save keywords"
            setOnClickListener {
                KeywordStore.save(this@MainActivity, keywordsBox.text.toString())
                // Re-normalise the box so it shows exactly what got stored.
                keywordsBox.setText(KeywordStore.asText(this@MainActivity))
                Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(TextView(this).apply {
            text = "Test box — type \"zzj\" here:"
            textSize = 14f
            setPadding(0, 40, 0, 8)
        })

        root.addView(EditText(this).apply {
            hint = "Type here to test (e.g. zzj)"
            setPadding(24, 48, 24, 48)
        })

        // Android 15 (targetSdk 35) forces edge-to-edge, so the activity draws behind the
        // status and navigation bars. Wrap in a ScrollView and pad it by the system-bar +
        // IME insets so nothing is hidden under the bars and everything stays reachable.
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, bars.bottom)
            insets
        }

        setContentView(scroll)
    }
}
