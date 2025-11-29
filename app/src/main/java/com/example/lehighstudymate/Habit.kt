package com.example.lehighstudymate

import java.util.UUID

data class Habit(
    val id: String = UUID.randomUUID().toString(), // ID for alarm
    var name: String,
    var hour: Int,
    var minute: Int,
    var isEnabled: Boolean = true
)