package com.example.lehighstudymate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.lehighstudymate.databinding.ActivitySmartPlannerBinding
import com.example.lehighstudymate.network.ChatRequest
import com.example.lehighstudymate.network.Message
import com.example.lehighstudymate.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

// Activity for the Smart Planner feature, which uses AI to prioritize and schedule user tasks.
class SmartPlannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySmartPlannerBinding

    // Callback for handling the result of the voice recognition Intent
    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            // Extract the list of recognized speech results
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: "" // Use the best recognized result

            // If the user spoke a specific command ("Start Focus"), navigate immediately
            if (spokenText.lowercase().contains("focus") || spokenText.lowercase().contains("start")) {
                Toast.makeText(this, "Voice Command: Starting Focus Mode!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SuperFocusActivity::class.java))
            } else {
                // Otherwise, populate the text input field with the spoken text
                binding.etTasks.setText(spokenText)
                Toast.makeText(this, "Voice recognized", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmartPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Voice input button click listener
        binding.btnVoiceInput.setOnClickListener {
            startVoiceInput()
        }

        // 2. Generate Plan button click listener
        binding.btnGeneratePlan.setOnClickListener {
            val tasks = binding.etTasks.text.toString()
            if (tasks.isNotEmpty()) {
                callAIPlanner(tasks)
            } else {
                Toast.makeText(this, "Please enter or speak your tasks first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Initiates the system's speech recognition process. */
    private fun startVoiceInput() {
        // Create the Intent for speech recognition
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your tasks or commands...")

        try {
            // Launch the speech recognition activity
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    /** Calls the AI service to generate a prioritized and structured plan from user tasks. */
    private fun callAIPlanner(tasks: String) {
        binding.tvPlanResult.text = "Analyzing priorities & Creating schedule...\n(Consulting Productivity Expert AI)"

        // --- Prompt Engineering ---
        val systemPrompt = """
            You are a Productivity Expert.
            The user has these tasks: "$tasks".
            
            Please:
            1. Prioritize them (High/Medium/Low Impact).
            2. Suggest a logical execution order (e.g., Eat the Frog first).
            3. Breakdown complex tasks into 2-3 small steps.
            4. Suggest estimated time for each.
            
            Format output clearly.
        """.trimIndent()

        // Create the messages list for the API call (System prompt + simple User message)
        val messages = listOf(
            Message("system", systemPrompt),
            Message("user", "Plan my day.")
        )

        // Send the request to the Retrofit client
        RetrofitClient.instance.getSummary(ChatRequest(messages = messages))
            .enqueue(object : Callback<com.example.lehighstudymate.network.ChatResponse> {
                override fun onResponse(call: Call<com.example.lehighstudymate.network.ChatResponse>, response: Response<com.example.lehighstudymate.network.ChatResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        // Display the AI-generated plan
                        val plan = response.body()!!.choices.first().message.content
                        binding.tvPlanResult.text = plan
                    } else {
                        binding.tvPlanResult.text = "Failed to generate plan. Response code: ${response.code()}"
                    }
                }
                override fun onFailure(call: Call<com.example.lehighstudymate.network.ChatResponse>, t: Throwable) {
                    binding.tvPlanResult.text = "Network error: ${t.message}"
                }
            })
    }
}