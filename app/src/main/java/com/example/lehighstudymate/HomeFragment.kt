package com.example.lehighstudymate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lehighstudymate.databinding.FragmentHomeBinding

// Fragment representing the main home screen of the application, serving as a navigation hub.
class HomeFragment : Fragment() {
    // Backing property for View Binding
    private var _binding: FragmentHomeBinding? = null
    // Non-nullable accessor property for the binding object
    private val binding get() = _binding!!

    // Called to create the view hierarchy associated with the fragment.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment using View Binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Setup click listeners for navigation cards

        // Navigation to Smart Planner/Schedule
        binding.cardPlanner.setOnClickListener { startActivity(Intent(context, SmartPlannerActivity::class.java)) }
        // Navigation to Habit Management
        binding.cardHabits.setOnClickListener { startActivity(Intent(context, HabitManagerActivity::class.java)) }

        // New Academic Guidance Entry Points

        // Navigation to Lecture Summary feature (e.g., OCR summary)
        binding.cardLecture.setOnClickListener { startActivity(Intent(context, LectureSummaryActivity::class.java)) }
        // Navigation to Homework Tutor feature (AI guidance)
        binding.cardTutor.setOnClickListener { startActivity(Intent(context, HomeworkTutorActivity::class.java)) }

        // Navigation to Quiz feature (e.g., Flashcard Quiz)
        binding.btnQuiz.setOnClickListener { startActivity(Intent(context, QuizActivity::class.java)) }

        // Hidden entry point to Emotional Support/Psychologist Chat
        binding.btnPsychText.setOnClickListener { startActivity(Intent(context, EmotionCareActivity::class.java)) }

        return binding.root // Return the root view
    }

    // Called when the fragment is visible to the user and actively running.
    override fun onResume() {
        super.onResume()
        // Load the focus session history
        val sessions = FocusStorage.getSessions(requireContext())
        // Update the focus trend chart with the loaded data using the dedicated method
        binding.chartFocusTrend.setRealData(sessions)
    }

    // Called when the view previously created by onCreateView has been detached from the fragment.
    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding object to avoid memory leaks
        _binding = null
    }
}