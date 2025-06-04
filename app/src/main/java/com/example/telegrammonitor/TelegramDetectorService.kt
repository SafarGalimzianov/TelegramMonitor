package com.example.telegrammonitor

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.Toast

class TelegramDetectorService : AccessibilityService() {

    private var wm: WindowManager? = null
    private var popup: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        hidePopup()
        Toast.makeText(this, "Сервис запущен", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return
        if (containsText(root, "Meduza — LIVE")) showPopup() else hidePopup()
    }

    override fun onInterrupt() {}

    private fun containsText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString() == text) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsText(child, text)) return true
        }
        return false
    }

    private fun showPopup() {
        if (popup != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        popup = LayoutInflater.from(this).inflate(R.layout.overlay_popup, null).also { view ->
            view.findViewById<Button>(R.id.btnClose).setOnClickListener {
                hidePopup()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            wm?.addView(view, params)
        }
    }

    private fun hidePopup() {
        popup?.let { wm?.removeView(it); popup = null }
    }
}
