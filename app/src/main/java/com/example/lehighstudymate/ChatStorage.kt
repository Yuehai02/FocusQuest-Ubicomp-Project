package com.example.lehighstudymate

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.min

// Object responsible for storing and retrieving chat history using SharedPreferences.
object ChatStorage {
    // Upgraded database name to prevent interference from old data (version 2).
    private const val PREF_NAME = "chat_history_v2"
    // Key for storing tutoring-related chat messages.
    private const val KEY_TUTOR = "tutor_messages"
    // Key for storing psychology-related chat messages.
    private const val KEY_PSYCH = "psych_messages"
    private val gson = Gson()

    // --- 1. Tutor Specific Methods ---
    /** Saves the list of ChatMessage for the tutor session. */
    fun saveTutorMessages(context: Context, messages: List<ChatMessage>) {
        saveToPrefs(context, KEY_TUTOR, messages)
    }

    /** Retrieves the list of ChatMessage for the tutor session. */
    fun getTutorMessages(context: Context): ArrayList<ChatMessage> {
        return getFromPrefs(context, KEY_TUTOR)
    }

    /** Clears all stored messages for the tutor session. */
    fun clearTutorMessages(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_TUTOR).apply()
    }

    // --- 2. Psychologist Specific Methods ---
    /** Saves the list of ChatMessage for the psychologist session. */
    fun savePsychMessages(context: Context, messages: List<ChatMessage>) {
        saveToPrefs(context, KEY_PSYCH, messages)
    }

    /** Retrieves the list of ChatMessage for the psychologist session. */
    fun getPsychMessages(context: Context): ArrayList<ChatMessage> {
        return getFromPrefs(context, KEY_PSYCH)
    }

    /** Clears all stored messages for the psychologist session. */
    fun clearPsychMessages(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_PSYCH).apply()
    }

    // --- Underlying General Logic ---
    /** Converts message list to JSON and saves it to SharedPreferences. */
    private fun saveToPrefs(context: Context, key: String, messages: List<ChatMessage>) {
        val jsonString = gson.toJson(messages)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, jsonString).apply()
    }

    /** Retrieves JSON string from SharedPreferences and converts it back to a message list. */
    private fun getFromPrefs(context: Context, key: String): ArrayList<ChatMessage> {
        val jsonString = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
        return if (jsonString != null) {
            // Deserialize the JSON string back into an ArrayList of ChatMessage
            gson.fromJson(jsonString, object : TypeToken<ArrayList<ChatMessage>>() {}.type)
        } else {
            // Return an empty list if no data is found
            ArrayList()
        }
    }

    // --- Duplicate Check Algorithm (Only checks Tutor history) ---
    /**
     * Checks the Tutor chat history for a question similar to the new question.
     * @param newQuestion The user's new question to check.
     * @return The content of a similar historical question if found, otherwise null.
     */
    fun findSimilarQuestion(context: Context, newQuestion: String): String? {
        val history = getTutorMessages(context) // Only check the tutoring history
        // Filter for user questions longer than 5 characters to avoid noise
        val userQuestions = history.filter { it.isUser && it.content.length > 5 }

        for (oldMsg in userQuestions) {
            // Check for similarity above a threshold of 0.7
            if (calculateSimilarity(newQuestion, oldMsg.content) > 0.7) {
                return oldMsg.content
            }
        }
        return null
    }

    /** Calculates the similarity between two strings based on Levenshtein distance. */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        if (longer.isEmpty()) return 1.0 // If both are empty, similarity is 1.0

        // Calculate the edit distance between the lowercase versions of the strings
        val editDistance = levenshtein(longer.lowercase(), shorter.lowercase())
        // Similarity is calculated as (Length - EditDistance) / Length
        return (longer.length - editDistance).toDouble() / longer.length.toDouble()
    }

    /** Calculates the Levenshtein edit distance between two CharSequences. */
    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        // Initialize cost array for the first row (insertions from empty string)
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i // Cost of transforming the first i chars of rhs from empty string
            for (j in 1..lhsLength) {
                // Cost is 0 if characters match, 1 otherwise
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                // Find the minimum cost among insert, delete, and replace
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            // Swap cost arrays for the next iteration
            val swap = cost
            cost = newCost
            newCost = swap
        }
        // The final cost is the Levenshtein distance
        return cost[lhsLength]
    }
}