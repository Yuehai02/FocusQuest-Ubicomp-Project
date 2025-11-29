package com.example.lehighstudymate

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Data class representing a single vocabulary word added to the unknown list.
data class VocabWord(val word: String, val timestamp: Long)

// Object responsible for storing and managing the list of unknown words (Vocabulary Book)
// generated from the Flashcard Quiz.
object QuizStorage {
    private const val PREF_NAME = "quiz_data" // SharedPreferences file name for quiz data
    private const val KEY_UNKNOWN = "unknown_words" // Key for the stored list of unknown words
    private val gson = Gson() // Gson instance for JSON serialization/deserialization

    /**
     * Adds a word to the list of unknown words if it doesn't already exist.
     * The word is added with the current timestamp.
     * @param context Application context.
     * @param word The word to be added.
     */
    fun addUnknownWord(context: Context, word: String) {
        val list = getUnknownWords(context) // Retrieve the current list

        // Check if the word is already in the list (prevents duplicates)
        if (list.none { it.word == word }) {
            // Add the new word (most recent first)
            list.add(0, VocabWord(word, System.currentTimeMillis()))
            val json = gson.toJson(list) // Convert the updated list to JSON
            // Save the updated list back to SharedPreferences
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_UNKNOWN, json).apply()
        }
    }

    /**
     * Retrieves the entire list of unknown words (Vocabulary Book).
     * @param context Application context.
     * @return An ArrayList of VocabWord objects.
     */
    fun getUnknownWords(context: Context): ArrayList<VocabWord> {
        // Retrieve the JSON string from storage
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_UNKNOWN, null)
        return if (json != null) {
            // Deserialize the JSON string back into an ArrayList of VocabWord
            gson.fromJson(json, object : TypeToken<ArrayList<VocabWord>>() {}.type)
        } else {
            // Return an empty list if no data is found
            ArrayList()
        }
    }
}