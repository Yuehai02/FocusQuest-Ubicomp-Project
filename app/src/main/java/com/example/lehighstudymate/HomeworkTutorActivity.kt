package com.example.lehighstudymate

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lehighstudymate.databinding.ActivityHomeworkTutorBinding
import com.example.lehighstudymate.network.ChatRequest
import com.example.lehighstudymate.network.Message
import com.example.lehighstudymate.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Activity for the Homework Tutor, implementing a multi-stage, guided problem-solving chat system.
class HomeworkTutorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeworkTutorBinding
    private val messageList = ArrayList<ChatMessage>()
    private var adapter = ChatAdapter(messageList)

    // State flag to manage the recall/hint choice sequence
    private var isWaitingForRecallChoice = false
    // Stores the user's question while waiting for the recall choice
    private var pendingQuestion = ""
    // Tracks the current step-by-step guidance phase (0: Initial, 1: Cognitive, 2: Structure, 3: Verification)
    private var tutorStage = 0
    // Stores the main topic/question being addressed
    private var currentTopic = ""

    // Keywords used for basic emotion/stress analysis
    private val mildStressKeywords = listOf("tired", "exhausted", "stressed", "busy", "hard", "bored", "sleepy", "annoyed")
    private val severeDistressKeywords = listOf("frustrated", "fail", "give up", "hopeless", "anxious", "panic", "depressed", "kill", "die")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeworkTutorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        binding.rvChatList.layoutManager = LinearLayoutManager(this)
        binding.rvChatList.adapter = adapter

        loadHistory()

        // Setup send button listener
        binding.btnSend.setOnClickListener {
            val userText = binding.etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                setSendingState(true) // Disable sending while processing
                binding.etInput.text.clear()

                // Check for explicit reset command
                if (userText.lowercase() == "reset" || userText.lowercase() == "new question") {
                    tutorStage = 0
                    isWaitingForRecallChoice = false
                    addMessage("System: Context reset.", false)
                    setSendingState(false)
                    return@setOnClickListener
                }

                handleUserSend(userText)
            }
        }

        // Setup clear history button listener
        binding.btnClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }
    }

    /** Controls the UI state of the send button (enabled/disabled and appearance). */
    private fun setSendingState(isSending: Boolean) {
        binding.btnSend.isEnabled = !isSending
        binding.btnSend.alpha = if (isSending) 0.5f else 1.0f
    }

    /** Loads chat history specific to the Tutor from local storage. */
    private fun loadHistory() {
        // Load only the Tutor history
        val history = ChatStorage.getTutorMessages(this)
        messageList.clear()
        if (history.isNotEmpty()) {
            messageList.addAll(history)
            adapter.notifyDataSetChanged()
            binding.rvChatList.scrollToPosition(messageList.size - 1)
        } else {
            // Initial greeting message
            addMessage("Hello! I am your AI Tutor. I will guide you step-by-step.", false)
        }
    }

    /** Primary handler for processing user input based on current state. */
    private fun handleUserSend(text: String) {
        if (isWaitingForRecallChoice) {
            // If the system is waiting for an A/B choice (Hint/Answer)
            addMessage(text, true)
            processRecallChoice(text)
            setSendingState(false)
            return
        }

        if (tutorStage > 0) {
            // If in an ongoing multi-step sequence, just send to the AI
            addMessage(text, true)
            callStepByStepAI(text)
            return
        }

        // --- Initial Question Processing (tutorStage == 0) ---

        // Check for similar question only if the text is long enough
        val similarQuestion = if (text.length > 12) {
            ChatStorage.findSimilarQuestion(this, text)
        } else {
            null
        }

        addMessage(text, true) // Add user message to UI

        // 1. Emotion analysis (highest priority interruption)
        if (analyzeEmotion(text)) {
            setSendingState(false)
            return
        }

        // 2. Recall check
        if (similarQuestion != null) {
            // Initiate the recall choice sequence
            isWaitingForRecallChoice = true
            pendingQuestion = text
            val reply = """
                Wait! You asked something similar before: "$similarQuestion"
                Do you want:
                A. A hint? (Type 'Hint')
                B. The answer? (Type 'Answer')
            """.trimIndent()
            addMessage(reply, false)
            setSendingState(false)
        } else {
            // 3. Start a new step-by-step sequence
            currentTopic = text
            callStepByStepAI(text)
        }
    }

    /** Calls the AI with system prompts that enforce multi-stage tutoring. */
    private fun callStepByStepAI(userText: String) {
        val systemPrompt: String
        if (tutorStage == 0) {
            tutorStage = 1
            binding.tvTitle.text = "Tutor: Cognitive Phase"
            systemPrompt = "You are 'Lehigh StudyMate'. Phase 1 (Cognitive): Ask guiding questions about definitions and core concepts related to the user's initial question."
        } else if (tutorStage == 1) {
            tutorStage = 2
            binding.tvTitle.text = "Tutor: Structure Phase"
            systemPrompt = "Phase 2 (Structure): Break down the problem into logical steps or structural components."
        } else if (tutorStage == 2) {
            tutorStage = 3
            binding.tvTitle.text = "Tutor: Verification Phase"
            systemPrompt = "Phase 3 (Verification): Check the user's logic, suggest edge cases, or ask for validation of intermediate results."
        } else {
            // tutorStage == 3 (or higher, automatically reset)
            tutorStage = 0 // Reset for the next problem
            binding.tvTitle.text = "Tutor: Summary"
            systemPrompt = "Phase 4 (Summary): Provide positive reinforcement and summarize the key learning points from the discussion."
        }

        // Prepare context messages (only includes the current user text, as the AI manages the multi-stage logic via system prompt)
        val messages = listOf(Message("system", systemPrompt), Message("user", userText))

        RetrofitClient.instance.getSummary(ChatRequest(messages = messages))
            .enqueue(object : Callback<com.example.lehighstudymate.network.ChatResponse> {
                override fun onResponse(call: Call<com.example.lehighstudymate.network.ChatResponse>, response: Response<com.example.lehighstudymate.network.ChatResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        addMessage(response.body()!!.choices.first().message.content, false)
                    } else {
                        addMessage("Error connecting to the Tutor service.", false)
                    }
                    setSendingState(false) // Re-enable send button
                }
                override fun onFailure(call: Call<com.example.lehighstudymate.network.ChatResponse>, t: Throwable) {
                    addMessage("Network error. Could not reach the Tutor.", false)
                    setSendingState(false)
                }
            })
    }

    /** Processes the user's choice after a similar question is recalled. */
    private fun processRecallChoice(choice: String) {
        isWaitingForRecallChoice = false // Clear the flag
        val lowerChoice = choice.lowercase()
        if (lowerChoice.contains("hint") || lowerChoice.contains("a")) {
            // Request a simple hint from the AI without starting the full multi-stage process
            callSimpleAI("Don't give answer. Give a hint for: $pendingQuestion")
        } else {
            // User wants the full answer/explanation, restart the multi-stage sequence
            currentTopic = pendingQuestion
            tutorStage = 0
            callStepByStepAI(pendingQuestion)
        }
    }

    /**
     * Analyzes user text for stress keywords and provides an appropriate intervention.
     * @return True if an intervention was triggered (stop chat flow), false otherwise.
     */
    private fun analyzeEmotion(text: String): Boolean {
        val lowerText = text.lowercase()
        // Check for severe keywords (emergency response)
        for (word in severeDistressKeywords) {
            if (lowerText.contains(word)) {
                addMessage("I hear you're feeling $word. Please contact Lehigh UCPS: https://studentaffairs.lehigh.edu/content/counseling-psychological-services-ucps", false)
                binding.tvTitle.text = "Lehigh Care Center"
                return true
            }
        }
        // Check for mild keywords (suggest break)
        for (word in mildStressKeywords) {
            if (lowerText.contains(word)) {
                addMessage("You seem $word. Take a quick break before continuing.", false)
                binding.tvTitle.text = "Relaxation Coach"
                return true
            }
        }
        return false
    }

    /** Helper function for simple, single-turn AI requests (e.g., fetching a hint). */
    private fun callSimpleAI(prompt: String) {
        val messages = listOf(Message("system", "Helpful assistant."), Message("user", prompt))
        RetrofitClient.instance.getSummary(ChatRequest(messages = messages))
            .enqueue(object : Callback<com.example.lehighstudymate.network.ChatResponse> {
                override fun onResponse(call: Call<com.example.lehighstudymate.network.ChatResponse>, response: Response<com.example.lehighstudymate.network.ChatResponse>) {
                    response.body()?.let { addMessage(it.choices.first().message.content, false) }
                    setSendingState(false)
                }
                override fun onFailure(call: Call<com.example.lehighstudymate.network.ChatResponse>, t: Throwable) {
                    setSendingState(false)
                }
            })
    }

    /** Shows a confirmation dialog before clearing the chat history. */
    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Delete all chat records for the Homework Tutor?")
            .setPositiveButton("Delete") { _, _ ->
                // Only clear Tutor history
                ChatStorage.clearTutorMessages(this)
                messageList.clear()
                adapter.notifyDataSetChanged()
                tutorStage = 0
                addMessage("History cleared.", false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Adds a new message to the list, updates the UI, and saves the history. */
    private fun addMessage(text: String, isUser: Boolean) {
        messageList.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messageList.size - 1)
        binding.rvChatList.scrollToPosition(messageList.size - 1)

        // Save to the dedicated Tutor storage space
        ChatStorage.saveTutorMessages(this, messageList)
    }
}