package com.example.lehighstudymate

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lehighstudymate.databinding.ActivityFocusModeBinding
import kotlin.math.abs
import kotlin.math.log10

// Activity for the Focus Mode feature, monitoring noise (microphone) and movement (accelerometer).
class FocusModeActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityFocusModeBinding
    private var mediaRecorder: MediaRecorder? = null // Object for recording/monitoring microphone input
    private var isMonitoring = false // State flag for active monitoring
    private val handler = Handler(Looper.getMainLooper()) // Handler for scheduling periodic tasks

    // Sensor-related variables
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isFirstSensorData = true // Flag to ignore the first set of sensor data

    // Debounce variables
    // 1. Noise buffer: How many consecutive noise detections are needed to trigger an alarm?
    private var consecutiveLoudCount = 0
    private val NOISE_THRESHOLD_COUNT = 5 // Checking every 300ms, 5 counts is approx. 1.5 seconds

    // 2. Motion buffer: How long must motion be sustained to count as a distraction?
    private var motionStartTime: Long = 0
    private val MOTION_DURATION_THRESHOLD = 2000L // 2000 milliseconds = 2 seconds

    // Notification Manager for Do Not Disturb (DND) access
    private lateinit var notificationManager: NotificationManager

    // Noise detection periodic task (runs every 300ms)
    private val updateTask = object : Runnable {
        override fun run() {
            if (isMonitoring && mediaRecorder != null) {
                // Get the maximum amplitude since the last call
                val amplitude = mediaRecorder?.maxAmplitude ?: 0
                // Convert amplitude to decibels (dB)
                val db = if (amplitude > 0) (20 * log10(amplitude.toDouble())).toInt() else 0

                // Call the debounced noise update logic
                updateNoiseUIWithDebounce(db)

                // Reschedule the task
                handler.postDelayed(this, 300)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize sensor and notification managers
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Start the monitoring process after checking permissions
        checkPermissionsAndStart()
        // Set up the DND toggle switch logic
        setupDNDLogic()

        // Set up the stop button listener
        binding.btnStopMonitor.setOnClickListener {
            stopMonitoring()
            finish() // Close the activity
        }
    }

    /** Checks for RECORD_AUDIO permission and starts monitoring if granted. */
    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            startMonitoring()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for microphone permission result
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startMonitoring()
        }
    }

    /** Initializes and starts the MediaRecorder and registers accelerometer listener. */
    private fun startMonitoring() {
        try {
            // Create a temporary file for MediaRecorder output
            val tempFile = java.io.File(externalCacheDir, "temp_audio.3gp")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start() // Start recording to enable maxAmplitude readings
            }
            isMonitoring = true
            handler.post(updateTask) // Start the periodic noise check

            // Register the accelerometer listener
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Microphone start failed", Toast.LENGTH_SHORT).show()
        }
    }

    /** Handles the logic for toggling and requesting Do Not Disturb (DND) permission. */
    private fun setupDNDLogic() {
        binding.switchDnd.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // If permission is granted, set DND to Priority mode
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    Toast.makeText(this, "Focus Mode ON", Toast.LENGTH_SHORT).show()
                    binding.btnDndPermission.visibility = android.view.View.GONE
                } else {
                    // If permission is not granted, uncheck the switch and request permission
                    binding.switchDnd.isChecked = false
                    Toast.makeText(this, "Please grant DND permission", Toast.LENGTH_LONG).show()
                    // Open the system settings to grant access
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
            } else {
                // If unchecked, set DND back to normal (Allow All)
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
        }
    }

    // Humanized noise update logic with debounce
    /** Updates noise UI and applies a debounce mechanism for alarms. */
    private fun updateNoiseUIWithDebounce(db: Int) {
        // Real-time update of value and progress bar (for immediate feedback)
        binding.tvDecibel.text = "$db dB"
        binding.progressNoise.progress = db

        // Determine environment status (with buffer)
        if (db > 60) {
            // Increment count only when noise is detected
            consecutiveLoudCount++

            if (consecutiveLoudCount > NOISE_THRESHOLD_COUNT) {
                // Alarm: Noise has been sustained for over ~1.5 seconds
                binding.tvStatus.text = "Too Noisy! (Sustained)"
                binding.tvStatus.setTextColor(android.graphics.Color.RED)
            } else {
                // Warning: Short spike detected, but not yet an alarm
                binding.tvStatus.text = "Spike detected..."
                binding.tvStatus.setTextColor(android.graphics.Color.YELLOW)
            }
        } else {
            // Environment is quiet, reset the counter
            consecutiveLoudCount = 0
            binding.tvStatus.text = "Environment OK"
            binding.tvStatus.setTextColor(android.graphics.Color.GREEN)
        }
    }

    // Humanized distraction detection logic (accelerometer)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            if (!isFirstSensorData) {
                // Calculate movement differences from the last reading
                val deltaX = abs(lastX - x)
                val deltaY = abs(lastY - y)
                val deltaZ = abs(lastZ - z)

                // Check for significant motion (Sensitivity set to 0.5)
                if ((deltaX > 0.5 || deltaY > 0.5 || deltaZ > 0.5)) {
                    // If this is the start of a motion sequence, record the time
                    if (motionStartTime == 0L) {
                        motionStartTime = System.currentTimeMillis()
                    }

                    // Calculate how long the motion has been sustained
                    val duration = System.currentTimeMillis() - motionStartTime

                    if (duration > MOTION_DURATION_THRESHOLD) {
                        // Alarm: Only flag as distraction if movement exceeds 2 seconds
                        binding.tvDistraction.text = "Distraction Detected! (Playing w/ Phone) "
                        binding.tvDistraction.setTextColor(android.graphics.Color.RED)
                    } else {
                        // Warning: Just started moving, not yet an alarm
                        binding.tvDistraction.text = "Phone moved..."
                        binding.tvDistraction.setTextColor(android.graphics.Color.YELLOW)
                    }
                } else {
                    // Phone is stationary, immediately reset the motion timer
                    motionStartTime = 0L
                    binding.tvDistraction.text = "Phone is stationary (Focusing...)"
                    binding.tvDistraction.setTextColor(android.graphics.Color.GREEN)
                }
            }

            // Update last known position
            lastX = x
            lastY = y
            lastZ = z
            isFirstSensorData = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for accelerometer monitoring
    }

    /** Stops all monitoring tasks, releases resources, and resets DND settings. */
    private fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacks(updateTask) // Stop the noise check task
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {} // Ignore exceptions during stop/release
        mediaRecorder = null
        sensorManager.unregisterListener(this) // Unregister sensor listener

        // Reset DND filter back to normal if permission was granted
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}