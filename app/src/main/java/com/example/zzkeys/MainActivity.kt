package com.example.zzkeys

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var keywordsBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "zzkeys\n\n" +
                "1. Tap \"Open Accessibility settings\" and enable \"zzkeys autocomplete\".\n" +
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

        setContentView(root)
    }
}
