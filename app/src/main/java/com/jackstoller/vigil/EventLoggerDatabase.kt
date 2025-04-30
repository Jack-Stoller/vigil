package com.jackstoller.vigil

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.view.accessibility.AccessibilityEvent

class EventLoggerDatabase(context: Context) : SQLiteOpenHelper(context, "events.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER,
                eventType TEXT,
                packageName TEXT,
                className TEXT,
                text TEXT,
                contentDescription TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS events")
        onCreate(db)
    }

    fun insertEvent(event: AccessibilityEvent) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("timestamp", event.eventTime)
            put("eventType", AccessibilityEvent.eventTypeToString(event.eventType))
            put("packageName", event.packageName?.toString())
            put("className", event.className?.toString())
            put("text", event.text.joinToString())
            put("contentDescription", event.contentDescription?.toString())
        }
        db.insert("events", null, values)
    }

    fun clearEvents() {
        val db = writableDatabase
        db.delete("events", null, null)
    }

    fun getAllEvents(): List<EventEntry> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM events ORDER BY timestamp DESC", null)
        val events = mutableListOf<EventEntry>()

        while (cursor.moveToNext()) {
            events.add(
                EventEntry(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    eventType = cursor.getString(cursor.getColumnIndexOrThrow("eventType")),
                    packageName = cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                    className = cursor.getString(cursor.getColumnIndexOrThrow("className")),
                    text = cursor.getString(cursor.getColumnIndexOrThrow("text")),
                    contentDescription = cursor.getString(cursor.getColumnIndexOrThrow("contentDescription"))
                )
            )
        }

        cursor.close()
        return events
    }
}

data class EventEntry(
    val id: Int,
    val timestamp: Long,
    val eventType: String,
    val packageName: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?
)
