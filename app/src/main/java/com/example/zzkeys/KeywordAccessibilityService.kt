package com.example.zzkeys

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

class KeywordAccessibilityService : AccessibilityService() {

    private val TAG = "zzkeys"

    private var windowManager: WindowManager? = null
    private var bar: LinearLayout? = null
    private var barShown = false

    // The node we last saw being edited, so the tap handler knows where to write.
    private var lastEditable: AccessibilityNodeInfo? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        ) return

        val source = event.source ?: return
        if (!source.isEditable) return

        val full = source.text?.toString() ?: ""
        // Where is the cursor? Fall back to end of text.
        val cursor = source.textSelectionEnd.let { if (it in 0..full.length) it else full.length }

        val token = currentToken(full, cursor)
        Log.d(TAG, "text='$full' cursor=$cursor token='$token'")

        if (token.length >= 2 && token.startsWith("zz", ignoreCase = true)) {
            val matches = Keywords.ALL.filter { it.startsWith(token, ignoreCase = true) }
            Log.d(TAG, "matches=$matches")
            if (matches.isNotEmpty()) {
                lastEditable = source
                showBar(matches, full, token, cursor)
                return
            }
        }
        hideBar()
    }

    /** Extract the word immediately left of the cursor (back to last whitespace). */
    private fun currentToken(text: String, cursor: Int): String {
        if (text.isEmpty()) return ""
        val end = cursor.coerceIn(0, text.length)
        var start = end
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        return text.substring(start, end)
    }

    private fun showBar(
        matches: List<String>,
        fullText: String,
        token: String,
        cursor: Int
    ) {
        val wm = windowManager ?: return

        if (bar == null) {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(0xFF202124.toInt())
                setPadding(16, 12, 16, 12)
            }
            val scroll = HorizontalScrollView(this).apply { addView(container) }

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM
                // Sits above the keyboard. Tune this offset if it overlaps Gboard.
                y = 720
            }

            wm.addView(scroll, lp)
            bar = container
            barShown = true
        }

        val container = bar ?: return
        container.removeAllViews()
        for (kw in matches) {
            val b = Button(this).apply {
                text = kw
                isAllCaps = false
                setOnClickListener { applyKeyword(kw, fullText, token, cursor) }
            }
            container.addView(b)
        }
        if (!barShown) {
            (container.parent as? android.view.View)?.visibility = android.view.View.VISIBLE
            barShown = true
        }
    }

    private fun hideBar() {
        val container = bar ?: return
        if (barShown) {
            (container.parent as? android.view.View)?.visibility = android.view.View.GONE
            barShown = false
        }
    }

    /** Replace the in-progress token with the chosen keyword and move the cursor after it. */
    private fun applyKeyword(keyword: String, fullText: String, token: String, cursor: Int) {
        val node = lastEditable ?: return
        val end = cursor.coerceIn(0, fullText.length)
        val start = end - token.length
        if (start < 0) return

        val newText = fullText.substring(0, start) + keyword + fullText.substring(end)
        val newCursor = start + keyword.length

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        val sel = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursor)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursor)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel)

        hideBar()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        bar?.let { (it.parent as? android.view.View)?.let { v -> windowManager?.removeView(v) } }
        bar = null
    }
}
