package com.example.lehighstudymate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Activity dedicated to displaying the Frequently Asked Questions (FAQ) for the app.
class FaqActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the layout for this activity
        setContentView(R.layout.activity_faq)

        // Find the RecyclerView in the layout
        val rvFaq = findViewById<RecyclerView>(R.id.rv_faq)
        // Set the layout manager for the RecyclerView to display items vertically
        rvFaq.layoutManager = LinearLayoutManager(this)


        // Define the list of FAQ items (Question and Answer pairs)
        val faqList = listOf(
            // FAQ Item 1: Smart Lecture Summary feature
            FaqItem(
                "How do I use Smart Lecture Summary?",
                "Simply click 'Smart Lecture Summary', take a photo of your notes or handout. The AI will use OCR to read the text and generate a summary, key formulas, and knowledge points for you."
            ),
            // FAQ Item 2: Homework Tutor feature
            FaqItem(
                "What is the 'Homework Tutor'?",
                "It's an AI that guides you step-by-step instead of giving direct answers. It helps you understand definitions, break down problems, and verify your logic."
            ),
            // FAQ Item 3: Focus Mode feature
            FaqItem(
                "How does Focus Mode work?",
                "It uses your microphone to detect noise and the camera to check your posture. If you get too close to the screen or slouch, it will warn you to protect your health."
            ),
            // FAQ Item 4: Flashcard Quiz feature
            FaqItem(
                "How do I play the Flashcard Quiz?",
                "It's a hands-free game! Nod your head (up/down) if you know the word. Shake your head (left/right) if you don't. Make sure your face is visible in the camera preview."
            ),
            // FAQ Item 5: Chat history saving
            FaqItem(
                "Is my chat history saved?",
                "Yes! Your chats with the Tutor and the Psychologist are saved locally on your phone, so you can review them anytime."
            ),
            // FAQ Item 6: Privacy policy regarding photos/camera
            FaqItem(
                "Privacy: Where do my photos go?",
                "Photos for OCR and camera feeds for posture detection are processed instantly and are NOT saved to any server. Your privacy is our priority."
            )
        )

        // Create and set the adapter for the RecyclerView using the defined FAQ list
        rvFaq.adapter = FaqAdapter(faqList)
    }
}