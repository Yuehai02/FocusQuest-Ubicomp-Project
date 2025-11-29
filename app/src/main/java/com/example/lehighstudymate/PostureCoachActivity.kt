package com.example.lehighstudymate

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lehighstudymate.databinding.ActivityPostureCoachBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors

// Activity dedicated to real-time posture monitoring using the camera and ML Kit Pose Detection.
class PostureCoachActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostureCoachBinding

    // ML Kit Pose Detector initialization (using stream mode for faster processing).
    private val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE) // Stream mode is optimized for speed
            .build()
    )

    // Timer variables
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    // Runnable for the session timer
    private val timerRunnable = object : Runnable {
        override fun run() {
            seconds++
            val min = seconds / 60
            val sec = seconds % 60
            binding.tvTimer.text = String.format("Session: %02d:%02d", min, sec)

            // Simulating the long-sitting reminder (e.g., 45-60 minutes in a proposal).
            // For testing, set to 60 seconds (1 minute).
            if (seconds > 60) {
                binding.tvTimer.setTextColor(Color.RED)
                binding.tvTimer.text = "Time to Stretch! (Session > 1min)"
            }
            handler.postDelayed(this, 1000) // Repeat every second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostureCoachBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 200)
        } else {
            startCamera() // Start the camera if permission is granted
        }

        // Start the session timer immediately
        handler.post(timerRunnable)
    }

    /** Sets up and starts the camera preview and image analysis pipeline. */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 1. Preview configuration
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // 2. Image Analysis configuration (Core: Pose detection happens here)
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Use the most recent frame
                .build()

            // Set the analyzer to process frames using a single thread executor
            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val mediaImage = imageProxy.image

                if (mediaImage != null) {
                    // Create InputImage for ML Kit
                    val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                    // Call ML Kit Pose Detector
                    poseDetector.process(image)
                        .addOnSuccessListener { pose ->
                            // Get the position of the Nose landmark
                            val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

                            if (nose != null) {
                                // Simple logic: Check the nose position (Proxy for head/face position)
                                val yPos = nose.position.y
                                // Warning line: Lower 30% of the image height
                                val limit = imageProxy.height * 0.7

                                runOnUiThread {
                                    if (yPos > limit) {
                                        // Nose dropped low -> Bad posture (likely slouching/leaning in)
                                        showBadPosture()
                                    } else {
                                        // Nose position is normal -> Good posture
                                        showGoodPosture()
                                    }
                                    binding.tvCameraStatus.text = "Tracking Face..."
                                }
                            } else {
                                runOnUiThread {
                                    binding.tvCameraStatus.text = "No Face Detected"
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Pose", "Detection failed", e)
                        }
                        .addOnCompleteListener {
                            // MUST close the imageProxy to process the next frame
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            try {
                // Unbind any previous use cases
                cameraProvider.unbindAll()
                // Bind the selected camera, preview, and image analysis to the activity's lifecycle
                // Using the back camera by default for posture monitoring
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /** Updates the UI to indicate good posture. */
    private fun showGoodPosture() {
        binding.tvPostureFeedback.text = "Good Posture"
        binding.tvPostureFeedback.setTextColor(Color.parseColor("#4CAF50")) // Green
    }

    /** Updates the UI to indicate bad posture (slouching). */
    private fun showBadPosture() {
        binding.tvPostureFeedback.text = "Slouching Detected! "
        binding.tvPostureFeedback.setTextColor(Color.RED)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera() // Restart camera setup if permission is granted
        } else {
            Toast.makeText(this, "Camera permission required for posture detection", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the session timer when the activity is destroyed
        handler.removeCallbacks(timerRunnable)
    }
}