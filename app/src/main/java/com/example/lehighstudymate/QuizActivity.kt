package com.example.lehighstudymate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lehighstudymate.databinding.ActivityQuizBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.abs

class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding
    private val wordList = listOf("Algorithm", "Recursion", "Polymorphism", "Interface", "Compiler", "Database", "Heuristic")
    private var currentIndex = 0
    private var score = 0
    private var isProcessing = false

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
            .build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvScore.setOnClickListener {
            startActivity(Intent(this, QuizStatsActivity::class.java))
        }

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 400)

        showNextWord()
    }

    private fun showNextWord() {
        if (currentIndex < wordList.size) {
            binding.tvWord.text = wordList[currentIndex]
            binding.tvEmojiFeedback.visibility = View.GONE
            binding.tvWord.visibility = View.VISIBLE
            isProcessing = false
        } else {
            binding.tvWord.text = "Finished!"
            binding.tvQuestionLabel.text = "Score: $score / ${wordList.size}\nTap Score to see Vocab Book"
            stopCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewMini.surfaceProvider) }
            val imageAnalysis = ImageAnalysis.Builder().build()
            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                val image = imageProxy.image
                if (image != null) {
                    val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                    faceDetector.process(inputImage).addOnSuccessListener { faces ->
                        if (faces.isNotEmpty() && !isProcessing) {
                            val face = faces[0]
                            if (face.headEulerAngleX > 20 || face.headEulerAngleX < -20) handleAnswer(true)
                            else if (abs(face.headEulerAngleY) > 30) handleAnswer(false)
                        }
                    }.addOnCompleteListener { imageProxy.close() }
                } else { imageProxy.close() }
            }
            try { cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis) } catch (e: Exception) {}
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleAnswer(isKnown: Boolean) {
        isProcessing = true
        runOnUiThread {
            if (isKnown) {
                score++
                binding.tvScore.text = "Score: $score (Tap to see Book)"
                binding.tvEmojiFeedback.text = "CORRECT"
                binding.tvEmojiFeedback.setTextColor(Color.GREEN)
            } else {
                QuizStorage.addUnknownWord(this, wordList[currentIndex])
                binding.tvEmojiFeedback.text = "WRONG"
                binding.tvEmojiFeedback.setTextColor(Color.RED)
            }

            binding.tvEmojiFeedback.visibility = View.VISIBLE
            binding.tvWord.visibility = View.INVISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                currentIndex++
                showNextWord()
            }, 1000)
        }
    }
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}