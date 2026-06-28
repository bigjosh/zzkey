package com.example.zzkeys

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "zzkeys\n\n1. Tap the button below and enable \"zzkeys autocomplete\".\n" +
                "2. Come back here and type in the box. Try \"zzj\".\n" +
                "3. Tap a suggestion in the bar above the keyboard.\n\n" +
                "Keywords live in Keywords.kt — edit and rebuild to change them."
            textSize = 16f
        })

        root.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(EditText(this).apply {
            hint = "Type here to test (e.g. zzj)"
            setPadding(24, 48, 24, 48)
        })

        setContentView(root)
    }
}
