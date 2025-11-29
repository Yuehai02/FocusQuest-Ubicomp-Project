package com.example.lehighstudymate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lehighstudymate.databinding.ActivityEmotionCareBinding
import com.example.lehighstudymate.network.ChatRequest
import com.example.lehighstudymate.network.Message
import com.example.lehighstudymate.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Activity dedicated to providing emotional support and serving as an AI psychologist chat interface.
class EmotionCareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmotionCareBinding
    // List to hold the messages displayed in the RecyclerView
    private val messageList = ArrayList<ChatMessage>()
    // Adapter for the RecyclerView
    private var adapter = ChatAdapter(messageList)

    // State variable to track the current language (false = English, true = Chinese)
    private var isChinese = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize View Binding
        binding = ActivityEmotionCareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the RecyclerView with a vertical layout manager
        binding.rvChatList.layoutManager = LinearLayoutManager(this)
        binding.rvChatList.adapter = adapter

        // 🔥 CORE CHANGE: Load only the history specific to the "Psychologist" role 🔥
        val history = ChatStorage.getPsychMessages(this)

        if (history.isNotEmpty()) {
            // Load history into the display list
            messageList.addAll(history)
            adapter.notifyDataSetChanged()
            // Scroll to the latest message
            binding.rvChatList.scrollToPosition(messageList.size - 1)

            // Since there is history, generate a new greeting based on context
            generateContextAwareGreeting(history)
        } else {
            // If no history exists, use the default opening line
            addMessage("Hi there. I'm here to listen. No judgment, just support.", false)
        }

        // Set up the send button click listener
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.etInput.text.clear() // Clear input field
                addMessage(text, true) // Add user message to UI
                callTherapistBrain(text) // Send message to AI
            }
        }

        // Set up the language toggle button
        binding.btnLanguage.setOnClickListener {
            isChinese = !isChinese // Toggle the language state
            updateLanguageUI() // Update UI elements based on the new language
        }
    }

    /** Updates text elements (title, button, hint) based on the current language state. */
    private fun updateLanguageUI() {
        if (isChinese) {
            binding.btnLanguage.text = "中"
            binding.etInput.hint = "告诉我你在想什么..."
            binding.tvTitle.text = "情感支持中心 ❤️"
            binding.btnSend.text = "发送"
        } else {
            binding.btnLanguage.text = "EN"
            binding.etInput.hint = "Tell me what's on your mind..."
            binding.tvTitle.text = "Emotional Support ❤️"
            binding.btnSend.text = "Send"
        }
    }

    /** Generates a personalized greeting using the AI based on recent chat history. */
    private fun generateContextAwareGreeting(history: List<ChatMessage>) {
        // Take only the last 4 messages (2 user/AI pairs) as context to keep prompt short
        val recentHistory = history.takeLast(4).map { "${if(it.isUser) "User" else "AI"}: ${it.content}" }.joinToString("\n")

        // System prompt instructing the AI to generate a warm, context-aware greeting
        val prompt = """
            Based on the chat history below, generate a short, warm, 1-sentence greeting to welcome the user back.
            Example: "Hi! Are you feeling better about [topic] today?"
            
            History:
            $recentHistory
            
            Output ONLY the greeting.
        """.trimIndent()

        // Create the API request messages
        val messages = listOf(Message("system", "You are a caring friend."), Message("user", prompt))
        RetrofitClient.instance.getSummary(ChatRequest(messages = messages))
            .enqueue(object : Callback<com.example.lehighstudymate.network.ChatResponse> {
                override fun onResponse(call: Call<com.example.lehighstudymate.network.ChatResponse>, response: Response<com.example.lehighstudymate.network.ChatResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val greeting = response.body()!!.choices.first().message.content
                        addMessage(greeting, false) // Add the generated greeting as an AI message
                    }
                }
                override fun onFailure(call: Call<com.example.lehighstudymate.network.ChatResponse>, t: Throwable) {
                    // Fail silently for greeting generation
                }
            })
    }

    /** Calls the external AI service with the 'psychologist' persona. */
    private fun callTherapistBrain(userText: String) {
        // Determine the language instruction for the AI
        val languageInstruction = if (isChinese) "Reply in Chinese." else "Reply in English."

        // Comprehensive system prompt defining the AI's role, tone, and safety instructions
        val systemPrompt = """
            You are 'StudyMate Care', a compassionate AI psychologist.
            Tone: Warm, empathetic, non-judgemental.
            Technique: Use 'Reflective Listening'. Validate feelings.
            Safety: If self-harm mentioned, provide: https://studentaffairs.lehigh.edu/content/counseling-psychological-services-ucps
            Language: $languageInstruction
        """.trimIndent()

        // Construct the messages list for the API call (System + User message)
        val messages = listOf(Message("system", systemPrompt), Message("user", userText))

        // Show typing indicator
        binding.tvTitle.text = if (isChinese) "对方正在输入..." else "Typing..."

        // Initiate API call
        RetrofitClient.instance.getSummary(ChatRequest(messages = messages))
            .enqueue(object : Callback<com.example.lehighstudymate.network.ChatResponse> {
                override fun onResponse(call: Call<com.example.lehighstudymate.network.ChatResponse>, response: Response<com.example.lehighstudymate.network.ChatResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        // Add AI response to the chat
                        response.body()?.let { addMessage(it.choices.first().message.content, false) }
                    } else {
                        // Handle unsuccessful response
                        addMessage("Connection weak, but I'm here.", false)
                    }
                    // Restore title after response
                    binding.tvTitle.text = if (isChinese) "情感支持中心 ❤️" else "Emotional Support ❤️"
                }
                override fun onFailure(call: Call<com.example.lehighstudymate.network.ChatResponse>, t: Throwable) {
                    // Handle network error
                    addMessage("Network error.", false)
                    binding.tvTitle.text = "Offline"
                }
            })
    }

    /** Adds a new message to the list, updates the UI, and saves the history. */
    private fun addMessage(text: String, isUser: Boolean) {
        messageList.add(ChatMessage(text, isUser)) // Add message to the list
        adapter.notifyItemInserted(messageList.size - 1) // Notify adapter of new item
        binding.rvChatList.scrollToPosition(messageList.size - 1) // Scroll to the bottom

        // 🔥 CORE CHANGE: Save the updated message list to the dedicated psychologist storage 🔥
        ChatStorage.savePsychMessages(this, messageList)
    }
}