package com.example.block

import android.content.ContentResolver
import android.provider.CallLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CallEntry(
    val number: String,
    val receivedAt: Long,
    val durationSeconds: Long
)

class CallHistoryRepository(private val contentResolver: ContentResolver) {

    fun getIncomingHistory(limit: Int = 100): List<CallEntry> {
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )

        val selection = "${CallLog.Calls.TYPE} = ?"
        val selectionArgs = arrayOf(CallLog.Calls.INCOMING_TYPE.toString())
        val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT $limit"

        val history = mutableListOf<CallEntry>()

        contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex)?.takeIf { it.isNotBlank() } ?: "Número desconhecido"
                val date = cursor.getLong(dateIndex)
                val durationSeconds = cursor.getLong(durationIndex)

                history.add(
                    CallEntry(
                        number = number,
                        receivedAt = date,
                        durationSeconds = durationSeconds
                    )
                )
            }
        }

        return history
    }

    companion object {
        fun formatDateTime(timestamp: Long): String {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }
    }
}
