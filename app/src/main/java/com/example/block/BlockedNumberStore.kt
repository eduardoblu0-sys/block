package com.example.block

import android.content.Context

class BlockedNumberStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBlockedNumbers(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_NUMBERS, emptySet()) ?: emptySet()
    }

    fun addNumber(number: String): Boolean {
        val normalizedNumber = normalize(number)
        if (normalizedNumber.isEmpty()) return false

        val current = getBlockedNumbers().toMutableSet()
        val added = current.add(normalizedNumber)
        if (added) {
            prefs.edit().putStringSet(KEY_BLOCKED_NUMBERS, current).apply()
        }
        return added
    }

    fun removeNumber(number: String) {
        val normalizedNumber = normalize(number)
        val current = getBlockedNumbers().toMutableSet()
        if (current.remove(normalizedNumber)) {
            prefs.edit().putStringSet(KEY_BLOCKED_NUMBERS, current).apply()
        }
    }

    fun isBlocked(number: String?): Boolean {
        val normalizedIncoming = normalize(number ?: return false)
        if (normalizedIncoming.isEmpty()) return false

        return getBlockedNumbers().any { blocked ->
            normalizedIncoming == blocked || normalizedIncoming.endsWith(blocked)
        }
    }

    companion object {
        private const val PREFS_NAME = "blocked_numbers"
        private const val KEY_BLOCKED_NUMBERS = "numbers"

        fun normalize(number: String): String {
            return number.filter { it.isDigit() || it == '+' }
        }
    }
}
