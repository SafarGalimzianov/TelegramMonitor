package com.example.telegrammonitor

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class TelegramDetectorService : AccessibilityService() {

    private var wm: WindowManager? = null
    private var popup: View? = null
    private var notificationManager: NotificationManager? = null
    private var eventCounter = 0
    private var lastEventTime = 0L
    
    companion object {
        private const val TAG = "TelegramDetector"
        private const val CHANNEL_ID = "telegram_monitor_debug"
        private const val NOTIFICATION_ID_SERVICE = 1
        private const val NOTIFICATION_ID_EVENT = 2
        private const val NOTIFICATION_ID_TEXT_FOUND = 3
        private const val NOTIFICATION_ID_POPUP = 4
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
        
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
        hidePopup()
        
        // Debug notification: Service started
        showDebugNotification(
            NOTIFICATION_ID_SERVICE,
            "Service Started",
            "TelegramDetector service is running. Time: ${getCurrentTime()}"
        )
        
        Toast.makeText(this, "Сервис запущен с отладкой", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        eventCounter++
        lastEventTime = System.currentTimeMillis()
        
        Log.d(TAG, "Event #$eventCounter: ${event.eventType}, package: ${event.packageName}")
        
        // Debug notification: Event received (update every 10 events)
        if (eventCounter % 10 == 0) {
            showDebugNotification(
                NOTIFICATION_ID_EVENT,
                "Events: $eventCounter",
                "Last event: ${getCurrentTime()}, Package: ${event.packageName}"
            )
        }

        val root = rootInActiveWindow
        if (root == null) {
            Log.d(TAG, "Root node is null")
            return
        }

        // Check if we're in Telegram
        val packageName = event.packageName?.toString()
        if (packageName != null && packageName.contains("telegram", true)) {
            Log.d(TAG, "In Telegram app, scanning for text...")
            
            val allText = getAllText(root)
            Log.d(TAG, "Found text in Telegram: $allText")
            
            // Debug notification: Show what text we found in Telegram
            if (allText.isNotEmpty()) {
                showDebugNotification(
                    NOTIFICATION_ID_TEXT_FOUND,
                    "Telegram Text Found",
                    "Text: ${allText.take(100)}${if (allText.length > 100) "..." else ""}"
                )
            }
            
            if (containsText(root, "Meduza — LIVE")) {
                Log.d(TAG, "TARGET TEXT FOUND: Meduza — LIVE")
                showPopup()
            } else {
                Log.d(TAG, "Target text not found")
                hidePopup()
            }
        } else {
            hidePopup()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
        showDebugNotification(
            NOTIFICATION_ID_SERVICE,
            "Service Interrupted",
            "Service was interrupted at ${getCurrentTime()}"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        notificationManager?.cancelAll()
    }

    private fun getAllText(node: AccessibilityNodeInfo): String {
        val textList = mutableListOf<String>()
        collectAllText(node, textList)
        return textList.joinToString(" | ")
    }
    
    private fun collectAllText(node: AccessibilityNodeInfo, textList: MutableList<String>) {
        node.text?.toString()?.let { text ->
            if (text.trim().isNotEmpty()) {
                textList.add(text.trim())
            }
        }
        
        node.contentDescription?.toString()?.let { desc ->
            if (desc.trim().isNotEmpty()) {
                textList.add("(desc: ${desc.trim()})")
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectAllText(child, textList)
        }
    }

    private fun containsText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString()?.contains(text, true) == true) return true
        if (node.contentDescription?.toString()?.contains(text, true) == true) return true
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsText(child, text)) return true
        }
        return false
    }

    private fun showPopup() {
        if (popup != null) {
            Log.d(TAG, "Popup already showing")
            return
        }

        Log.d(TAG, "Creating popup")
        
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
                Log.d(TAG, "Popup close button clicked")
                hidePopup()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            wm?.addView(view, params)
            Log.d(TAG, "Popup added to window manager")
        }
        
        // Debug notification: Popup shown
        showDebugNotification(
            NOTIFICATION_ID_POPUP,
            "Popup Displayed",
            "Meduza — LIVE detected! Popup shown at ${getCurrentTime()}"
        )
    }

    private fun hidePopup() {
        popup?.let { 
            wm?.removeView(it)
            popup = null 
            Log.d(TAG, "Popup hidden")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telegram Monitor Debug",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Debug notifications for Telegram Monitor"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun showDebugNotification(id: Int, title: String, content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .build()
            
        notificationManager?.notify(id, notification)
    }
    
    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
