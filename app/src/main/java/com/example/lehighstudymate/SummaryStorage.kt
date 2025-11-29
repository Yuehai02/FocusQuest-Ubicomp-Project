package com.example.lehighstudymate

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class representing a single lecture summary note.
data class SummaryNote(val time: String, val content: String)

// Object responsible for local storage and retrieval of AI-generated lecture summaries.
object SummaryStorage {
    private const val PREF_NAME = "lecture_summaries" // SharedPreferences file name
    private const val KEY_NOTES = "notes_list" // Key for the stored list of summaries
    private val gson = Gson() // Gson instance for JSON serialization/deserialization

    /**
     * Creates a new SummaryNote with the current timestamp and saves it to local storage.
     * @param context Application context.
     * @param content The text content of the AI-generated summary.
     */
    fun saveNote(context: Context, content: String) {
        val notes = getNotes(context) // Retrieve the existing list of notes
        // Format the current time for the timestamp (e.g., "11-29 11:55")
        val time = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
        // Create the new note and add it to the beginning of the list (most recent first)
        notes.add(0, SummaryNote(time, content))

        // Get SharedPreferences editor and save the updated list as a JSON string
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTES, gson.toJson(notes)).apply()
    }

    /**
     * Retrieves all saved SummaryNotes from local storage.
     * @param context Application context.
     * @return An ArrayList of SummaryNote objects.
     */
    fun getNotes(context: Context): ArrayList<SummaryNote> {
        // Retrieve the JSON string from storage
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_NOTES, null)
        return if (json != null) {
            // Deserialize the JSON string back into an ArrayList of SummaryNote
            gson.fromJson(json, object : TypeToken<ArrayList<SummaryNote>>(){}.type)
        } else {
            // Return an empty list if no data is found
            ArrayList()
        }
    }
}