package com.example.lehighstudymate.network


data class ChatRequest(
    val model: String = "gpt-4",
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)