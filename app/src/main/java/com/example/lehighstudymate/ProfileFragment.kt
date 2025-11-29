package com.example.lehighstudymate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.lehighstudymate.databinding.FragmentProfileBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Fragment displaying the user profile, settings, data access, and cloud synchronization options.
class ProfileFragment : Fragment() {
    // Backing property for View Binding
    private var _binding: FragmentProfileBinding? = null
    // Non-nullable accessor property for the binding object
    private val binding get() = _binding!!
    private val gson = Gson()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Navigation to Focus Result Activity (to view historical data/Boss stats)
        binding.btnViewData.setOnClickListener {
            val intent = Intent(context, FocusResultActivity::class.java)
            intent.putExtra("TASK_NAME", "Overall History") // Pass extra to ensure data loading
            startActivity(intent)
        }
        // Button to trigger the cloud synchronization dialog
        binding.btnCloudSync.setOnClickListener { showSyncDialog() }
        // Navigation to FAQ Activity
        binding.btnFaq.setOnClickListener { startActivity(Intent(context, FaqActivity::class.java)) }
        // Navigation to Settings Activity
        binding.btnSettings.setOnClickListener { startActivity(Intent(context, SettingsActivity::class.java)) }

        // New: Navigation to Quiz Statistics/Vocabulary Book
        binding.btnVocabBook.setOnClickListener { startActivity(Intent(context, QuizStatsActivity::class.java)) }

        return binding.root
    }

    // Crucial: Refresh user name and school every time the page becomes visible
    override fun onResume() {
        super.onResume()
        val name = SettingsStorage.getUserName(requireContext())
        val school = SettingsStorage.getUserSchool(requireContext())
        // Assuming the layout has IDs: tv_name and tv_school
        binding.tvName.text = name
        binding.tvSchool.text = school
    }

    // --- Cloud Synchronization Logic ---
    /** Shows a dialog allowing the user to choose between backup or restore operations. */
    private fun showSyncDialog() {
        val options = arrayOf("Backup Local to Cloud", "Restore Cloud to Local")
        AlertDialog.Builder(requireContext())
            .setTitle("Cloud Sync")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> uploadLocalData() // Option 0: Upload/Backup
                    1 -> restoreCloudData() // Option 1: Download/Restore
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Retrieves local Habit and Focus Session data and uploads it to Firebase. */
    private fun uploadLocalData() {
        val habits = HabitStorage.getHabits(requireContext())
        val sessions = FocusStorage.getSessions(requireContext())
        Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()

        FirebaseHelper.uploadBackup(habits, sessions,
            onSuccess = { Toast.makeText(context, "Backup Successful!", Toast.LENGTH_SHORT).show() },
            onFailure = { err -> Toast.makeText(context, "Failed: $err", Toast.LENGTH_SHORT).show() }
        )
    }

    /** Downloads Habit and Focus Session data from Firebase and overwrites local storage. */
    private fun restoreCloudData() {
        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()

        FirebaseHelper.downloadBackup(
            onSuccess = { habitsJson, sessionsJson ->
                // Type definitions for Gson deserialization
                val habitType = object : TypeToken<ArrayList<Habit>>() {}.type
                val sessionType = object : TypeToken<ArrayList<FocusSession>>() {}.type
                val habits: ArrayList<Habit> = gson.fromJson(habitsJson, habitType)
                val sessions: ArrayList<FocusSession> = gson.fromJson(sessionsJson, sessionType)

                // 1. Restore Habits and save them
                HabitStorage.saveHabits(requireContext(), habits)
                // 2. Overwrite Focus Sessions
                overwriteFocusStorage(sessions)
                // 3. Reschedule all restored alarms
                rescheduleAlarms(habits)

                Toast.makeText(context, "Data Restored!", Toast.LENGTH_LONG).show()
            },
            onFailure = { err -> Toast.makeText(context, "Failed: $err", Toast.LENGTH_SHORT).show() }
        )
    }

    /** Directly overwrites the SharedPreferences file for FocusStorage. */
    private fun overwriteFocusStorage(sessions: List<FocusSession>) {
        val json = gson.toJson(sessions)
        // Access FocusStorage's private storage ("focus_data") to replace the session list
        requireContext().getSharedPreferences("focus_data", Context.MODE_PRIVATE)
            .edit().putString("sessions_list", json).apply()
    }

    /** Iterates through the restored habits and schedules alarms for enabled ones. */
    private fun rescheduleAlarms(habits: List<Habit>) {
        for (habit in habits) {
            if (habit.isEnabled) AlarmScheduler.scheduleHabit(requireContext(), habit)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding reference
        _binding = null
    }
}