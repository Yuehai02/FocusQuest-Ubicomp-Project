package com.example.lehighstudymate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lehighstudymate.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

// Activity managing all user-configurable settings, profile updates, privacy options, and logout.
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Load the initial state of all settings from local storage

        // Load feature toggle states
        binding.switchDistance.isChecked = SettingsStorage.isDistanceGuardEnabled(this)
        binding.switchPosture.isChecked = SettingsStorage.isPostureCoachEnabled(this)
        binding.switchNeck.isChecked = SettingsStorage.isNeckReminderEnabled(this)
        binding.switchLeaderboard.isChecked = SettingsStorage.isLeaderboardEnabled(this)

        // Load profile data
        binding.etUserName.setText(SettingsStorage.getUserName(this))
        binding.etUserSchool.setText(SettingsStorage.getUserSchool(this))

        // 2. Profile Saving Logic
        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etUserName.text.toString().trim()
            val school = binding.etUserSchool.text.toString().trim()

            // Save locally
            SettingsStorage.setUserName(this, name)
            SettingsStorage.setUserSchool(this, school)

            // Upload to cloud (Firebase) and show success toast on completion
            FirebaseHelper.uploadProfile(name, school) {
                Toast.makeText(this, "Profile Synced!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Switch Listeners (Update local storage)

        // Distance Guard setting
        binding.switchDistance.setOnCheckedChangeListener { _, isChecked -> SettingsStorage.setDistanceGuard(this, isChecked) }
        // Posture Coach setting
        binding.switchPosture.setOnCheckedChangeListener { _, isChecked -> SettingsStorage.setPostureCoach(this, isChecked) }
        // Neck Reminder setting
        binding.switchNeck.setOnCheckedChangeListener { _, isChecked -> SettingsStorage.setNeckReminder(this, isChecked) }

        // Leaderboard Privacy switch: Update both local storage and cloud database
        binding.switchLeaderboard.setOnCheckedChangeListener { _, isChecked ->
            SettingsStorage.setLeaderboardEnabled(this, isChecked)
            FirebaseHelper.setPrivacy(isChecked) // Tell the cloud whether to show the user on the leaderboard
        }

        // 4. Logout Logic
        binding.btnLogout.setOnClickListener {
            // Confirmation dialog before logging out
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out? All local data will be cleared.")
                .setPositiveButton("Log Out") { _, _ ->
                    // Perform Firebase Sign-Out
                    FirebaseAuth.getInstance().signOut()

                    // Clear all local data associated with the logged-in user for security/privacy
                    ChatStorage.clearTutorMessages(this)
                    ChatStorage.clearPsychMessages(this)
                    FocusStorage.clearAll(this)
                    HabitStorage.saveHabits(this, emptyList()) // Clear habits
                    SettingsStorage.clear(this) // Clear user settings/profile

                    // Navigate back to LoginActivity and clear the activity stack
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}