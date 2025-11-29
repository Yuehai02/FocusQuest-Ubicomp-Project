package com.example.lehighstudymate

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.lehighstudymate.databinding.ActivityLectureSummaryBinding
// Import necessary ML Kit libraries
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import com.example.lehighstudymate.network.ChatRequest
import com.example.lehighstudymate.network.Message
import com.example.lehighstudymate.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Activity dedicated to processing lecture notes via OCR and generating smart summaries using GPT.
class LectureSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLectureSummaryBinding

    // Initialize the text recognizer (for Latin alphabet, e.g., English/numbers)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // 1. Gallery selection callback
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            binding.ivPreview.setImageURI(uri)
            binding.tvSummaryResult.text = "Recognizing text in the image..."

            // Convert the URI to the InputImage format required by ML Kit
            try {
                val image = InputImage.fromFilePath(this, uri)
                runTextRecognition(image)
            } catch (e: IOException) {
                e.printStackTrace()
                showToast("Failed to read image")
            }
        }
    }

    // 2. Take photo callback
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            binding.ivPreview.setImageBitmap(bitmap)
            binding.tvSummaryResult.text = "Recognizing text in the image..."

            // Convert the Bitmap to the InputImage format required by ML Kit
            val image = InputImage.fromBitmap(bitmap, 0)
            runTextRecognition(image)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLectureSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    /** Sets up click listeners for image selection buttons. */
    private fun setupListeners() {
        binding.btnPickGallery.setOnClickListener {
            // Launch the media picker to select images only
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnTakePhoto.setOnClickListener {
            // Launch the camera to take a picture and get a bitmap preview
            takePicture.launch(null)
        }
    }

    /** Runs ML Kit text recognition on the provided InputImage. */
    private fun runTextRecognition(image: InputImage) {
        binding.tvSummaryResult.text = "Extracting text..."
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                if (rawText.isNotEmpty()) {
                    // Trigger GPT summarization upon successful OCR
                    callGptToSummarize(rawText)
                } else {
                    binding.tvSummaryResult.text = "No text recognized."
                }
            }
            .addOnFailureListener { e ->
                binding.tvSummaryResult.text = "OCR failed: ${e.localizedMessage}"
            }
    }

    /** Calls the GPT API to generate a structured summary from the OCR text. */
    private fun callGptToSummarize(ocrText: String) {
        binding.tvSummaryResult.text = "Generating smart summary (Connecting to Brain)...\n\nRecognized Content:\n$ocrText"

        // 1. Build the API Prompt based on structured output requirements
        // Requirements: Generate summaries, key formulas, and knowledge point indexes
        val systemPrompt = """
            You are 'Lehigh StudyMate', an intelligent study assistant.
            Please analyze the following lecture notes/text. 
            Output the result in this format:
            
            【Summary】
            (A concise summary of the content)
            
            【Key Formulas】
            (Extract any math formulas or define key variables using LaTeX format if possible)
            
            【Knowledge Points】
            (A bulleted list of key concepts for quick review)
        """.trimIndent()

        val messages = listOf(
            Message(role = "system", content = systemPrompt),
            Message(role = "user", content = ocrText)
        )

        // 2. Send the API request
        RetrofitClient.instance.getSummary(ChatRequest(messages = messages))
            .enqueue(object : Callback<com.example.lehighstudymate.network.ChatResponse> {
                override fun onResponse(
                    call: Call<com.example.lehighstudymate.network.ChatResponse>,
                    response: Response<com.example.lehighstudymate.network.ChatResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        // 3. Successfully retrieved result
                        val aiReply = response.body()!!.choices.first().message.content
                        binding.tvSummaryResult.text = aiReply
                        // Save the summary result locally
                        SummaryStorage.saveNote(this@LectureSummaryActivity, aiReply)
                    } else {
                        // Failed response (e.g., bad API key, insufficient balance)
                        binding.tvSummaryResult.text = "AI request failed: Code ${response.code()}\n${response.errorBody()?.string()}"
                    }
                }

                override fun onFailure(call: Call<com.example.lehighstudymate.network.ChatResponse>, t: Throwable) {
                    // Network error (e.g., no internet, timeout)
                    binding.tvSummaryResult.text = "Network error: ${t.message}"
                    t.printStackTrace()
                }
            })
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}