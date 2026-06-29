package com.example.zzkeys

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

class KeywordAccessibilityService : AccessibilityService() {

    private val TAG = "zzkeys"

    private var windowManager: WindowManager? = null

    // The overlay is one window we add once and then show/hide. `scrollView` is the
    // root we hand to WindowManager; `bar` is the button container inside it.
    private var scrollView: HorizontalScrollView? = null
    private var bar: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var barVisible = false

    // Last matches rendered, so repeated events (esp. TYPE_WINDOW_CONTENT_CHANGED, which
    // fires a lot) don't rebuild identical buttons and cause flicker.
    private var lastMatches: List<String> = emptyList()

    // Hiding is DEFERRED. Chatty apps (e.g. Amazon's live-suggestion search) fire bursts of
    // TYPE_WINDOW_CONTENT_CHANGED events, and a single bad/transient read mid-burst used to
    // hide the bar we'd just shown. Now a non-match schedules a hide a beat later, and the
    // next matching keystroke cancels it — so momentary misreads never reach the screen.
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideBar() }
    private val hideDelayMs = 150L

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType

        // TEXT_CHANGED / SELECTION_CHANGED come straight from the edited field, so they're
        // trustworthy. WINDOW_CONTENT_CHANGED is the noisy fallback we keep only for Compose
        // fields (where TEXT_CHANGED is unreliable); chatty apps like Amazon and Google
        // search fire it in bursts with transient bad reads, so it may only SHOW, never hide.
        val authoritative = type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        if (!authoritative && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        // The bar only makes sense floating above an open keyboard. No keyboard → we're done
        // editing, so hide. This is the robust "dismiss" signal (independent of focus quirks).
        val imeTop = imeTopPx()
        if (imeTop < 0) { scheduleHide(); return }

        val node = editableNodeFor(event)
        if (node != null) {
            val full = node.text?.toString() ?: ""
            val cursor = node.textSelectionEnd.let { if (it in 0..full.length) it else full.length }
            val token = currentToken(full, cursor)
            Log.d(TAG, "type=$type imeTop=$imeTop text='$full' cursor=$cursor token='$token'")

            if (token.length >= 2 && token.startsWith("zz", ignoreCase = true)) {
                val matches = KeywordStore.load(this).filter { it.startsWith(token, ignoreCase = true) }
                if (matches.isNotEmpty()) {
                    showBar(matches, imeTop)
                    return
                }
            }
        }

        // No match shown. Only hide on a trustworthy field event — a transient miss on a
        // WINDOW_CONTENT_CHANGED (or an unreadable node) must NOT tear down the bar, which
        // was the flicker bug in Amazon/Google search.
        if (authoritative) scheduleHide()
    }

    /**
     * Resolve the editable field this event refers to, or null if none.
     *
     * For plain EditText, `event.source` is the field. For Jetpack Compose fields,
     * TYPE_VIEW_TEXT_CHANGED is unreliable and we lean on TYPE_WINDOW_CONTENT_CHANGED,
     * whose source is often a container — so we fall back to the focused input node.
     */
    private fun editableNodeFor(event: AccessibilityEvent): AccessibilityNodeInfo? {
        val src = event.source
        if (src != null && src.isEditable) return src
        val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return if (focused != null && focused.isEditable) focused else null
    }

    /** Extract the word immediately left of the cursor (back to last whitespace). */
    private fun currentToken(text: String, cursor: Int): String {
        if (text.isEmpty()) return ""
        val end = cursor.coerceIn(0, text.length)
        var start = end
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        return text.substring(start, end)
    }

    private fun showBar(matches: List<String>, imeTop: Int) {
        handler.removeCallbacks(hideRunnable) // a real match arrived; cancel any pending hide
        val wm = windowManager ?: return
        ensureBar(wm)
        val container = bar ?: return

        // Only rebuild buttons when the match set actually changed.
        if (!(barVisible && matches == lastMatches)) {
            container.removeAllViews()
            for (kw in matches) {
                val b = Button(this).apply {
                    text = kw
                    isAllCaps = false
                    setOnClickListener { applyKeyword(kw) }
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = 12 }
                container.addView(b, lp)
            }
            lastMatches = matches
        }

        positionAboveKeyboard(imeTop)
        setBarVisible(true)
    }

    /** Create the overlay window once; subsequent shows just toggle visibility. */
    private fun ensureBar(wm: WindowManager) {
        if (scrollView != null) return

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
            y = 0 // real position is set per-show in positionAboveKeyboard()
        }

        wm.addView(scroll, lp)
        scrollView = scroll
        bar = container
        layoutParams = lp
        barVisible = true
    }

    /**
     * Park the bar just above the on-screen keyboard.
     *
     * With BOTTOM gravity, `y` is the gap between the screen bottom and the bar's bottom
     * edge. We want the bar's bottom edge to sit at the keyboard's top edge, so
     * y = screenHeight - imeTop. If we can't find the IME window we fall back to the
     * screen bottom (y = 0) rather than a magic offset.
     */
    private fun positionAboveKeyboard(imeTop: Int) {
        val wm = windowManager ?: return
        val lp = layoutParams ?: return
        val screenH = wm.currentWindowMetrics.bounds.height()
        lp.y = if (imeTop in 1 until screenH) screenH - imeTop else 0
        Log.d(TAG, "position imeTop=$imeTop screenH=$screenH y=${lp.y}")
        scrollView?.let { wm.updateViewLayout(it, lp) }
    }

    /** Top edge (screen px) of the on-screen keyboard window, or -1 if not visible. */
    private fun imeTopPx(): Int {
        val wins = windows ?: return -1
        val r = Rect()
        for (w in wins) {
            if (w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                w.getBoundsInScreen(r)
                if (r.height() > 0) return r.top
            }
        }
        return -1
    }

    private fun setBarVisible(visible: Boolean) {
        val v = scrollView ?: return
        val target = if (visible) View.VISIBLE else View.GONE
        if (v.visibility != target) v.visibility = target
        barVisible = visible
    }

    /** Hide right now (used when a keyword is applied or the service stops). */
    private fun hideBar() {
        handler.removeCallbacks(hideRunnable)
        if (!barVisible) return
        setBarVisible(false)
        lastMatches = emptyList()
    }

    /** Hide shortly, unless a matching keystroke arrives first and cancels it. */
    private fun scheduleHide() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, hideDelayMs)
    }

    /**
     * Replace the in-progress token with the chosen keyword and move the cursor after it.
     *
     * We re-resolve the focused field and re-read its text here rather than trusting state
     * captured when the bar was shown — the field may have changed, and re-fetching avoids
     * holding a stale node across events.
     */
    private fun applyKeyword(keyword: String) {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (node == null || !node.isEditable) { hideBar(); return }

        val full = node.text?.toString() ?: ""
        val cursor = node.textSelectionEnd.let { if (it in 0..full.length) it else full.length }
        val token = currentToken(full, cursor)
        val start = cursor - token.length
        if (start < 0 || !token.startsWith("zz", ignoreCase = true)) { hideBar(); return }

        val newText = full.substring(0, start) + keyword + full.substring(cursor)
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
        handler.removeCallbacks(hideRunnable)
        scrollView?.let { windowManager?.removeView(it) }
        scrollView = null
        bar = null
        layoutParams = null
    }
}
