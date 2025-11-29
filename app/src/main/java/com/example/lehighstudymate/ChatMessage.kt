package com.example.lehighstudymate

data class ChatMessage(
    val content: String,
    val isUser: Boolean // true = user; false = AI
)