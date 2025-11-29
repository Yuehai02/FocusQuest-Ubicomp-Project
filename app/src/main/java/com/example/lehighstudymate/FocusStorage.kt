package com.example.lehighstudymate

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar // Import Calendar for time calculations

// Data class representing a single completed focus session.
data class FocusSession(
    val taskName: String, // Name of the task focused on
    val durationMinutes: Int, // Duration of the session in minutes
    val timestamp: Long // Unix timestamp when the session was recorded
)

// Object responsible for storing and retrieving focus session data using SharedPreferences.
object FocusStorage {
    private const val PREF_NAME = "focus_data" // SharedPreferences file name
    private const val KEY_SESSIONS = "sessions_list" // Key for the stored list of sessions
    private val gson = Gson() // Gson instance for JSON serialization

    /** Saves a new FocusSession to the local storage. */
    fun saveSession(context: Context, taskName: String, durationMinutes: Int) {
        val sessions = getSessions(context) // Retrieve existing sessions
        // Create the new session record with the current timestamp
        val newSession = FocusSession(taskName, durationMinutes, System.currentTimeMillis())
        sessions.add(0, newSession) // Add the new session to the beginning of the list (most recent first)

        // Convert the updated list to JSON and save it
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SESSIONS, gson.toJson(sessions)).apply()
    }

    /** Retrieves all saved FocusSessions from local storage. */
    fun getSessions(context: Context): ArrayList<FocusSession> {
        // Retrieve the JSON string
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_SESSIONS, null)
        return if (json != null) {
            // Deserialize the JSON string back into an ArrayList of FocusSession
            gson.fromJson(json, object : TypeToken<ArrayList<FocusSession>>() {}.type)
        } else {
            // Return an empty list if no data is found
            ArrayList()
        }
    }

    /** Calculates and returns the total focus minutes for each task completed today. */
    fun getTodayStats(context: Context): Map<String, Int> {
        val allSessions = getSessions(context)
        val todayStart = getTodayStartTime() // Get the timestamp for the start of today (midnight)

        return allSessions
            // Filter sessions to include only those recorded today
            .filter { it.timestamp >= todayStart }
            // Group the filtered sessions by task name
            .groupBy { it.taskName }
            // Map the grouped entries to the sum of their durations
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
    }

    /** Calculates the Unix timestamp for the start of the current day (00:00:00). */
    private fun getTodayStartTime(): Long {
        val cal = Calendar.getInstance()
        // Set time fields to midnight
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // NEW: Clears all focus data from local storage.
    /** Clears all data stored in the focus data SharedPreferences file. */
    fun clearAll(context: Context) {
        // Get the SharedPreferences editor and call clear()
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}