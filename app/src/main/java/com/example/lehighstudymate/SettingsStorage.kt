package com.example.lehighstudymate

import android.content.Context

// Object responsible for managing all application settings and user profile data using SharedPreferences.
object SettingsStorage {
    private const val PREF_NAME = "app_settings" // SharedPreferences file name

    // Switch Keys (Feature Toggles)
    private const val KEY_DISTANCE = "dist_guard" // Key for screen distance guard
    private const val KEY_POSTURE = "posture_coach" // Key for posture coach
    private const val KEY_NECK = "neck_reminder" // Key for neck stretch reminder
    private const val KEY_LEADERBOARD = "join_leaderboard" // Key for leaderboard privacy setting

    // Profile Data Keys
    private const val KEY_NAME = "user_name" // Key for user's display name
    private const val KEY_SCHOOL = "user_school" // Key for user's school/university

    // --- Getters (Retrieval Methods) ---

    // Feature Toggles (default: true)
    /** Checks if the distance guard feature is enabled. */
    fun isDistanceGuardEnabled(context: Context) = getBool(context, KEY_DISTANCE, true)
    /** Checks if the posture coach feature is enabled. */
    fun isPostureCoachEnabled(context: Context) = getBool(context, KEY_POSTURE, true)
    /** Checks if the neck reminder feature is enabled. */
    fun isNeckReminderEnabled(context: Context) = getBool(context, KEY_NECK, true)
    /** Checks if the user has enabled sharing data with the leaderboard. */
    fun isLeaderboardEnabled(context: Context) = getBool(context, KEY_LEADERBOARD, true)

    // Profile Data
    /** Retrieves the user's display name (default: "Student"). */
    fun getUserName(context: Context) = getString(context, KEY_NAME, "Student")
    /** Retrieves the user's school name (default: "Lehigh University"). */
    fun getUserSchool(context: Context) = getString(context, KEY_SCHOOL, "Lehigh University")

    // --- Setters (Storage Methods) ---

    // Feature Toggles
    /** Sets the state of the distance guard feature. */
    fun setDistanceGuard(context: Context, v: Boolean) = putBool(context, KEY_DISTANCE, v)
    /** Sets the state of the posture coach feature. */
    fun setPostureCoach(context: Context, v: Boolean) = putBool(context, KEY_POSTURE, v)
    /** Sets the state of the neck reminder feature. */
    fun setNeckReminder(context: Context, v: Boolean) = putBool(context, KEY_NECK, v)
    /** Sets the state of the leaderboard privacy setting. */
    fun setLeaderboardEnabled(context: Context, v: Boolean) = putBool(context, KEY_LEADERBOARD, v)

    // Profile Data
    /** Sets the user's display name. */
    fun setUserName(context: Context, v: String) = putString(context, KEY_NAME, v)
    /** Sets the user's school name. */
    fun setUserSchool(context: Context, v: String) = putString(context, KEY_SCHOOL, v)

    /** Clears all data stored in the app settings SharedPreferences file. */
    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // --- Private Utility Methods ---

    /** Retrieves a boolean value from SharedPreferences. */
    private fun getBool(c: Context, k: String, d: Boolean) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(k, d)
    /** Stores a boolean value in SharedPreferences. */
    private fun putBool(c: Context, k: String, v: Boolean) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(k, v).apply()
    /** Retrieves a string value from SharedPreferences. */
    private fun getString(c: Context, k: String, d: String) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(k, d) ?: d
    /** Stores a string value in SharedPreferences. */
    private fun putString(c: Context, k: String, v: String) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(k, v).apply()
}