package com.example.lehighstudymate

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HabitStorage {
    private const val PREF_NAME = "habit_data"
    private const val KEY_HABITS = "habits_list"
    private val gson = Gson()

    fun saveHabits(context: Context, habits: List<Habit>) {
        val json = gson.toJson(habits)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_HABITS, json).apply()
    }

    fun getHabits(context: Context): ArrayList<Habit> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HABITS, null)
        return if (json != null) {
            gson.fromJson(json, object : TypeToken<ArrayList<Habit>>() {}.type)
        } else {
            ArrayList()
        }
    }
}