package com.example.lehighstudymate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lehighstudymate.databinding.ActivitySuperFocusBinding
import com.example.lehighstudymate.network.ChatRequest
import com.example.lehighstudymate.network.Message
import com.example.lehighstudymate.network.RetrofitClient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors
import kotlin.math.abs

// The core focus activity combining timer, posture/distance monitoring, ambient sound, and integrated chat/notes.
class SuperFocusActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuperFocusBinding

    // --- Focus State ---
    private var isFocusing = false // Flag indicating if the timer is running
    private var focusDurationMills = 25 * 60 * 1000L // Default duration (25 minutes)
    private var timeLeftInMillis = focusDurationMills // Remaining time
    private var timer: CountDownTimer? = null // Countdown timer object

    // --- Long-Sitting Reminder ---
    private val STRETCH_REMINDER_INTERVAL = 60 * 1000L // Interval for stretch reminder (60 seconds for testing)
    private var nextStretchTime = STRETCH_REMINDER_INTERVAL // Next scheduled reminder time

    // --- Chat Related ---
    private val messageList = ArrayList<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var currentChatMode = "TUTOR" // Current overlay chat mode (TUTOR or PSYCH)

    // --- Hardware & AI ---
    private var mediaRecorder: MediaRecorder? = null // For monitoring microphone input (noise)
    private var musicPlayer: MediaPlayer? = null // For ambient background music
    private var selectedMusicResId: Int = 0 // Resource ID of the selected ambient track
    private val faceDetector = FaceDetection.getClient( // ML Kit Face Detector
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
            .build()
    )

    // --- Logic Control Variables ---
    private var tooCloseStartTime: Long = 0 // Timestamp when the screen distance violation started
    private val TOO_CLOSE_TIME_LIMIT = 20000L // Time limit before blurring the screen (20 seconds)
    private var isNeckExercising = false // Flag indicating if the user is currently doing neck exercises
    private var neckExerciseStep = 0 // Current step in the guided neck exercise sequence

    // --- Power Saving Optimization ---
    private val handler = Handler(Looper.getMainLooper()) // Handler for scheduled tasks
    private var isCameraRunning = false // Current state of the camera analysis
    private val POSTURE_CHECK_INTERVAL = 60 * 1000L // Interval to turn the camera OFF (60 seconds)
    private val POSTURE_CHECK_DURATION = 10 * 1000L // Duration to turn the camera ON (10 seconds)
    private var keepCameraOnForSafety = false // Flag to override power saving (e.g., during violation or exercise)

    // Runnable to cycle the camera ON and OFF for power saving
    private val cameraCycleTask = object : Runnable {
        override fun run() {
            if (isFocusing && !isNeckExercising) {
                // Check current settings
                val isPostureOn = SettingsStorage.isPostureCoachEnabled(this@SuperFocusActivity)
                val isDistanceOn = SettingsStorage.isDistanceGuardEnabled(this@SuperFocusActivity)

                // If both monitoring features are OFF, ensure camera is stopped
                if (!isPostureOn && !isDistanceOn) {
                    if (isCameraRunning) {
                        stopFaceDetection()
                        updateMiniStatus("Monitoring Off", Color.parseColor("#E0F7FA"), Color.GRAY)
                    }
                    return
                }

                if (!isCameraRunning) {
                    // Turn camera ON and schedule the OFF phase
                    startFaceDetection()
                    updateMiniStatus("Checking Posture...", Color.GRAY, Color.WHITE)
                    handler.postDelayed(this, POSTURE_CHECK_DURATION)
                } else {
                    // Camera is ON
                    if (keepCameraOnForSafety) {
                        // If a violation is active, keep the camera ON and recheck soon
                        handler.postDelayed(this, 5000)
                    } else {
                        // Turn camera OFF and schedule the ON phase
                        stopFaceDetection()
                        updateMiniStatus("Power Saving", Color.parseColor("#E0F7FA"), Color.GRAY)
                        handler.postDelayed(this, POSTURE_CHECK_INTERVAL)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperFocusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions()
        setupChatUI()
        setupMusicSpinner()

        // Handle back button press while focusing
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFocusing) showExitConfirmationDialog() else finish()
            }
        })

        // Duration Slider Listener
        binding.sliderDuration.addOnChangeListener { _, value, _ ->
            if (!isFocusing) {
                val mins = value.toInt()
                binding.tvSliderLabel.text = "Set Duration: $mins mins"
                binding.tvTimerBig.text = String.format("%02d:00", mins)
                focusDurationMills = mins * 60 * 1000L
                timeLeftInMillis = focusDurationMills
            }
        }

        // Button Listeners
        binding.btnStartFocus.setOnClickListener { if (!isFocusing) startFocusSession() }
        binding.btnFinishEarly.setOnClickListener { finishSession() }
        binding.btnNeckExercise.setOnClickListener {
            if (!isNeckExercising) startNeckExercise() else stopNeckExercise()
        }

        // Overlay/Chat Listeners
        binding.btnOpenTutor.setOnClickListener { openChatMode("TUTOR") }
        binding.btnOpenPsych.setOnClickListener { openChatMode("PSYCH") }
        binding.btnCloseOverlay.setOnClickListener { closeAllOverlays() }
        binding.btnOpenNotes.setOnClickListener { openNotesMode() }

        // Chat Send Listener
        binding.btnChatSend.setOnClickListener {
            val text = binding.etChatInput.text.toString().trim()
            if (text.isNotEmpty()) {
                addChatMessage(text, true)
                binding.etChatInput.text.clear()
                callAI(text)
            }
        }
    }

    /** Shows a confirmation dialog when the user attempts to exit while the session is running. */
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Stop Focusing?")
            .setMessage("Do you want to give up this session? Progress will be saved.")
            .setPositiveButton("Give Up") { _, _ -> finishSession() }
            .setNegativeButton("Keep Focusing", null)
            .setNeutralButton("Minimize App") { _, _ -> moveTaskToBack(true) } // Minimize the app instead of closing
            .show()
    }

    /** Initializes and starts the focus session components. */
    private fun startFocusSession() {
        isFocusing = true
        nextStretchTime = STRETCH_REMINDER_INTERVAL

        binding.etTaskName.isEnabled = false
        binding.sliderDuration.isEnabled = false
        binding.btnStartFocus.isEnabled = false
        binding.spinnerNoise.isEnabled = true // Allow changing ambient sound

        binding.btnFinishEarly.visibility = View.VISIBLE
        binding.btnNeckExercise.visibility = View.VISIBLE
        binding.layoutTools.visibility = View.VISIBLE // Show overlay buttons

        startMicrophone() // Start noise monitoring
        handler.post(cameraCycleTask) // Start posture/distance monitoring cycle
        playMusic(selectedMusicResId) // Start ambient music
        startTimer(timeLeftInMillis) // Start the countdown timer
    }

    /** Starts the countdown timer. */
    private fun startTimer(millis: Long) {
        timer = object : CountDownTimer(millis, 1000) {
            override fun onTick(m: Long) {
                timeLeftInMillis = m
                val min = m / 60000
                val sec = (m % 60000) / 1000
                val timeStr = String.format("%02d:%02d", min, sec)

                if (!isNeckExercising) binding.tvTimerBig.text = timeStr
                binding.tvMiniTimer.text = timeStr
                // Update progress bar
                binding.progressTimer.progress = ((m.toFloat() / focusDurationMills) * 100).toInt()

                val elapsed = focusDurationMills - m
                // Check for neck stretch reminder interval
                if (SettingsStorage.isNeckReminderEnabled(this@SuperFocusActivity) && elapsed >= nextStretchTime) {
                    nextStretchTime += STRETCH_REMINDER_INTERVAL // Schedule next reminder
                    showStretchReminderDialog()
                }
            }
            override fun onFinish() { finishSession() } // End session when timer runs out
        }.start()
    }

    /** Shows a dialog prompting the user to take a neck stretch break. */
    private fun showStretchReminderDialog() {
        timer?.cancel() // Pause the timer
        AlertDialog.Builder(this)
            .setTitle("Time to Stretch!")
            .setMessage("Your neck needs a break. Start guided exercises now?")
            .setPositiveButton("Yes") { _, _ -> startNeckExercise() }
            .setNegativeButton("Later") { _, _ -> startTimer(timeLeftInMillis) } // Resume timer
            .setCancelable(false)
            .show()
    }

    /** Ends the focus session, saves progress, and navigates to the result activity. */
    private fun finishSession() {
        // Stop all running services
        timer?.cancel()
        stopMicrophone()
        stopFaceDetection()
        stopMusic()
        handler.removeCallbacks(cameraCycleTask)

        // Calculate and save focused minutes
        val taskName = binding.etTaskName.text.toString().ifEmpty { "Study Session" }
        val focusedMillis = focusDurationMills - timeLeftInMillis
        val focusedMinutes = (focusedMillis / 60000).toInt().coerceAtLeast(1) // Ensure at least 1 minute is saved
        FocusStorage.saveSession(this, taskName, focusedMinutes)
        FirebaseHelper.uploadFocusSession(focusedMinutes) // Upload total time to Firebase

        // Navigate to the result screen
        val intent = Intent(this, FocusResultActivity::class.java)
        intent.putExtra("TASK_NAME", taskName)
        startActivity(intent)
        finish()
    }

    /** Initializes and binds the front camera for face detection and analysis. */
    @OptIn(ExperimentalGetImage::class)
    private fun startFaceDetection() {
        if (isCameraRunning) return
        isCameraRunning = true
        binding.previewMini.visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewMini.surfaceProvider) }
            val imageAnalysis = ImageAnalysis.Builder().build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                val image = imageProxy.image
                if (image != null) {
                    val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                    faceDetector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            if (faces.isNotEmpty()) {
                                val face = faces[0]
                                val imageWidth = imageProxy.width.toFloat()
                                val imageHeight = imageProxy.height.toFloat()

                                if (isNeckExercising) {
                                    keepCameraOnForSafety = true
                                    // Process head movement for exercise guidance
                                    processNeckExercise(face.headEulerAngleY, face.headEulerAngleX, face.headEulerAngleZ)
                                } else {
                                    // Normal monitoring logic
                                    val isDistOn = SettingsStorage.isDistanceGuardEnabled(this@SuperFocusActivity)
                                    val isPostOn = SettingsStorage.isPostureCoachEnabled(this@SuperFocusActivity)
                                    val ratio = face.boundingBox.width().toFloat() / imageWidth // Face size relative to frame
                                    var isBadDist = false
                                    var isBadPost = false
                                    var msg = "Monitoring..."

                                    // 1. Distance Guard Logic
                                    if (isDistOn) {
                                        if (ratio > 0.35) { // Threshold for being too close
                                            keepCameraOnForSafety = true // Override power saving
                                            if (tooCloseStartTime == 0L) tooCloseStartTime = System.currentTimeMillis()
                                            val duration = System.currentTimeMillis() - tooCloseStartTime
                                            runOnUiThread {
                                                if (duration > TOO_CLOSE_TIME_LIMIT) {
                                                    // Blur the screen after the time limit is reached
                                                    binding.layoutBlurOverlay.visibility = View.VISIBLE
                                                } else {
                                                    msg = "Too Close (${(TOO_CLOSE_TIME_LIMIT - duration)/1000}s)"
                                                    isBadDist = true
                                                }
                                            }
                                        } else {
                                            keepCameraOnForSafety = false
                                            tooCloseStartTime = 0L
                                            runOnUiThread { binding.layoutBlurOverlay.visibility = View.GONE }
                                        }
                                    }

                                    // 2. Posture Coach Logic (Check only if not violating distance)
                                    if (isPostOn && !isDistOn) {
                                        val faceCenterY = face.boundingBox.centerY().toFloat()
                                        // Simple head-down detection (face center too low in the frame)
                                        if (faceCenterY > imageHeight * 0.7) {
                                            msg = "Bad Posture"
                                            isBadPost = true
                                        } else {
                                            msg = "Posture OK"
                                        }
                                    }

                                    // Update UI Feedback
                                    runOnUiThread {
                                        if (isBadDist || isBadPost) {
                                            updateMiniStatus("Warning: $msg", Color.parseColor("#FFEBEE"), Color.RED)
                                            binding.tvPostureIndicator.text = msg
                                            binding.tvPostureIndicator.setBackgroundColor(Color.RED)
                                        } else {
                                            updateMiniStatus("Monitoring OK", Color.parseColor("#E0F7FA"), Color.parseColor("#00796B"))
                                            binding.tvPostureIndicator.text = "OK"
                                            binding.tvPostureIndicator.setBackgroundColor(Color.parseColor("#804CAF50")) // Semi-transparent green
                                        }
                                    }
                                }
                            } else {
                                keepCameraOnForSafety = false
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                } else { imageProxy.close() }
            }
            try {
                // Use the Front Camera for monitoring
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) { Log.e("Camera", "Error", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    /** Stops the camera and unbinds all use cases. */
    private fun stopFaceDetection() {
        if (!isCameraRunning) return
        isCameraRunning = false
        binding.previewMini.visibility = View.GONE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(this))
    }

    /** Updates the text and colors of the small status bar at the bottom. */
    private fun updateMiniStatus(text: String, bgColor: Int, textColor: Int) {
        binding.layoutMiniStatusBar.setBackgroundColor(bgColor)
        binding.tvMiniStatus.text = text
        binding.tvMiniStatus.setTextColor(textColor)
    }

    /** Initiates the guided neck exercise sequence. */
    private fun startNeckExercise() {
        isNeckExercising = true
        keepCameraOnForSafety = true
        neckExerciseStep = 1
        timer?.cancel() // Pause the session timer
        handler.removeCallbacks(cameraCycleTask) // Stop the camera cycle
        if (!isCameraRunning) { startFaceDetection() } // Ensure camera is running for detection

        // Reset distance violation state
        tooCloseStartTime = 0L
        binding.layoutBlurOverlay.visibility = View.GONE

        // Update UI
        binding.tvMiniTimer.text = "Paused"
        updateMiniStatus("Neck Exercise", Color.parseColor("#FFF8E1"), Color.BLACK)
        binding.tvTimerBig.visibility = View.INVISIBLE
        binding.tvNeckInstruction.visibility = View.VISIBLE
        binding.tvNeckInstruction.text = "Turn Left"
        binding.btnNeckExercise.text = "Stop Exercise"
    }

    /** Ends the neck exercise sequence and resumes the focus session. */
    private fun stopNeckExercise() {
        isNeckExercising = false
        keepCameraOnForSafety = false
        startTimer(timeLeftInMillis) // Resume the session timer
        binding.tvTimerBig.visibility = View.VISIBLE
        binding.tvNeckInstruction.visibility = View.GONE
        binding.tvNeckInstruction.setTextColor(getColor(R.color.primary))
        binding.btnNeckExercise.text = "Start Neck Exercise"
        handler.post(cameraCycleTask) // Restart the camera cycle
    }

    /** Processes head movement (Yaw, Pitch, Roll) to guide the neck exercise sequence. */
    private fun processNeckExercise(yaw: Float, pitch: Float, roll: Float) {
        runOnUiThread {
            val statusText = "Action: ${binding.tvNeckInstruction.text}"
            updateMiniStatus(statusText, Color.parseColor("#FFF8E1"), Color.BLACK)

            // Sequence: Left (Yaw), Right (Yaw), Up (Pitch), Down (Pitch), Tilt Left (Roll), Tilt Right (Roll)
            when(neckExerciseStep) {
                1 -> if(yaw > 30) nextStep(2, "Turn Right") // Check Turn Left (Positive Yaw)
                2 -> if(yaw < -30) nextStep(3, "Look Up") // Check Turn Right (Negative Yaw)
                3 -> if(pitch > 20) nextStep(4, "Look Down") // Check Look Up (Positive Pitch)
                4 -> if(pitch < -10) nextStep(5, "Tilt Left") // Check Look Down (Negative Pitch)
                5 -> if(roll < -20) nextStep(6, "Tilt Right") // Check Tilt Left (Negative Roll)
                6 -> if(roll > 20) { // Check Tilt Right (Positive Roll)
                    neckExerciseStep = 7
                    binding.tvNeckInstruction.text = "Done"
                    binding.tvNeckInstruction.setTextColor(Color.GREEN)
                    updateMiniStatus("Exercise Done", Color.GREEN, Color.WHITE)
                    // Automatically stop exercise after a short delay
                    Handler(Looper.getMainLooper()).postDelayed({ stopNeckExercise() }, 2000)
                }
            }
        }
    }

    /** Moves to the next step in the neck exercise sequence. */
    private fun nextStep(next: Int, msg: String) {
        neckExerciseStep = next
        binding.tvNeckInstruction.text = msg
        binding.tvNeckInstruction.setTextColor(Color.GREEN)
        // Briefly flash green feedback
        Handler(Looper.getMainLooper()).postDelayed({ binding.tvNeckInstruction.setTextColor(getColor(R.color.primary)) }, 500)
    }

    /** Sets up the ambient sound selection spinner. */
    private fun setupMusicSpinner() {
        val sounds = listOf("Off", "Rain", "Forest", "Cafe")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sounds)
        binding.spinnerNoise.adapter = adapter

        binding.spinnerNoise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedMusicResId = when (position) {
                    1 -> R.raw.noise_rain
                    2 -> R.raw.noise_forest
                    3 -> R.raw.noise_cafe
                    else -> 0
                }
                // Only start playing if the session is active
                if (isFocusing) playMusic(selectedMusicResId)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    /** Starts playing the selected ambient background music in a loop. */
    private fun playMusic(resId: Int) {
        musicPlayer?.release()
        musicPlayer = null
        if (resId != 0) {
            try {
                musicPlayer = MediaPlayer.create(this, resId)
                musicPlayer?.isLooping = true
                musicPlayer?.start()
            } catch (e: Exception) {}
        }
    }

    /** Stops and releases the MediaPlayer resource. */
    private fun stopMusic() {
        musicPlayer?.stop()
        musicPlayer?.release()
        musicPlayer = null
    }

    /** Sets up the chat RecyclerView adapter and layout manager. */
    private fun setupChatUI() {
        chatAdapter = ChatAdapter(messageList)
        binding.rvChatList.layoutManager = LinearLayoutManager(this)
        binding.rvChatList.adapter = chatAdapter
    }

    /** Opens the chat overlay with either Tutor or Psychologist history. */
    private fun openChatMode(mode: String) {
        currentChatMode = mode
        // Hide main UI and show chat overlay
        binding.layoutMainFocusUi.visibility = View.GONE
        binding.layoutMiniStatusBar.visibility = View.VISIBLE
        binding.layoutChatOverlay.visibility = View.VISIBLE
        binding.layoutNotesOverlay.visibility = View.GONE

        messageList.clear()
        if (mode == "PSYCH") {
            binding.layoutChatOverlay.setBackgroundColor(Color.parseColor("#FFF3E0")) // Light Orange background
            val history = ChatStorage.getPsychMessages(this)
            if (history.isNotEmpty()) messageList.addAll(history)
            else addChatMessage("Hi. Feeling overwhelmed? I am here to listen.", false)
        } else {
            binding.layoutChatOverlay.setBackgroundColor(Color.WHITE)
            val history = ChatStorage.getTutorMessages(this)
            if (history.isNotEmpty()) messageList.addAll(history)
            else addChatMessage("Focus Tutor here. What is the question you need guidance on?", false)
        }
        chatAdapter.notifyDataSetChanged()
        if (messageList.isNotEmpty()) binding.rvChatList.scrollToPosition(messageList.size - 1)
    }

    /** Opens the notes overlay to display saved lecture summaries. */
    private fun openNotesMode() {
        // Hide chat and main UI, show notes overlay
        binding.layoutMainFocusUi.visibility = View.GONE
        binding.layoutMiniStatusBar.visibility = View.VISIBLE
        binding.layoutChatOverlay.visibility = View.GONE
        binding.layoutNotesOverlay.visibility = View.VISIBLE

        val notes = SummaryStorage.getNotes(this)
        if (notes.isEmpty()) binding.tvNotesContent.text = "No summary history yet. Use the Lecture Summary feature to create notes."
        else {
            val sb = StringBuilder()
            for (note in notes) sb.append("Date: ${note.time}\n----------------\n${note.content}\n\n")
            binding.tvNotesContent.text = sb.toString()
        }
    }

    /** Closes the chat/notes overlays and returns to the main focus UI. */
    private fun closeAllOverlays() {
        binding.layoutChatOverlay.visibility = View.GONE
        binding.layoutNotesOverlay.visibility = View.GONE
        binding.layoutMiniStatusBar.visibility = View.GONE
        binding.layoutMainFocusUi.visibility = View.VISIBLE
    }

    /** Adds a message to the chat list, updates the UI, and saves it to the respective storage. */
    private fun addChatMessage(text: String, isUser: Boolean) {
        messageList.add(ChatMessage(text, isUser))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        binding.rvChatList.scrollToPosition(messageList.size - 1)
        // Save to the correct storage based on the current mode
        if (currentChatMode == "TUTOR") ChatStorage.saveTutorMessages(this, messageList)
        else ChatStorage.savePsychMessages(this, messageList)
    }

    /** Calls the AI API with the appropriate system prompt based on the current chat mode. */
    private fun callAI(text: String) {
        // Define system prompt based on mode, ensuring no emojis are used as per instruction
        val systemPrompt = if (currentChatMode == "TUTOR") "You are a concise Homework Tutor, guiding the user step-by-step. Do not use emojis." else "You are a warm, empathetic Psychologist. Do not use emojis."
        val messages = listOf(Message("system", systemPrompt), Message("user", text))
        RetrofitClient.instance.getSummary(ChatRequest(messages = messages))
            .enqueue(object : Callback<com.example.lehighstudymate.network.ChatResponse> {
                override fun onResponse(call: Call<com.example.lehighstudymate.network.ChatResponse>, response: Response<com.example.lehighstudymate.network.ChatResponse>) {
                    response.body()?.let { addChatMessage(it.choices.first().message.content, false) }
                }
                override fun onFailure(call: Call<com.example.lehighstudymate.network.ChatResponse>, t: Throwable) {
                    addChatMessage("Connection failed. Check network.", false)
                }
            })
    }

    /** Initializes and starts the MediaRecorder for microphone input monitoring. */
    private fun startMicrophone() {
        try {
            val file = java.io.File(externalCacheDir, "temp.3gp")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start() // Start recording (required to get maxAmplitude)
            }
        } catch (e: Exception) {
            Log.e("SuperFocus", "Microphone start failed", e)
        }
    }

    /** Stops and releases the MediaRecorder resource. */
    private fun stopMicrophone() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("SuperFocus", "Microphone stop failed", e)
        }
    }

    /** Checks and requests necessary runtime permissions (CAMERA and RECORD_AUDIO). */
    private fun checkAllPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        // Request permissions if any are missing
        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 300)
        }
    }
}