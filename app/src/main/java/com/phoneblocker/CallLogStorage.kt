package com.phoneblocker

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class CallLogEntry(
    val rawNumber: String,
    val timestamp: Long,
    val wasBlocked: Boolean,
    val matchedPattern: String? = null,
    val action: String? = null
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

object CallLogStorage {
    private const val PREFS_NAME = "call_log_prefs"
    private const val KEY_LOGS = "call_logs"
    private const val MAX_ENTRIES = 50
    
    private val gson = Gson()
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun addEntry(context: Context, entry: CallLogEntry) {
        val logs = getLogs(context).toMutableList()
        logs.add(0, entry) // Add to front
        
        // Keep only last MAX_ENTRIES
        val trimmed = logs.take(MAX_ENTRIES)
        
        val json = gson.toJson(trimmed)
        getPrefs(context).edit().putString(KEY_LOGS, json).apply()
    }
    
    fun getLogs(context: Context): List<CallLogEntry> {
        val json = getPrefs(context).getString(KEY_LOGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CallLogEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun clearLogs(context: Context) {
        getPrefs(context).edit().remove(KEY_LOGS).apply()
    }
}
