package com.example.lehighstudymate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lehighstudymate.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Activity responsible for handling user login and registration using Firebase Authentication.
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth // Firebase Authentication instance
    private val gson = Gson() // Gson instance for JSON serialization/deserialization

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check if a user is already logged in, and bypass login screen if true
        if (auth.currentUser != null) {
            goToMain()
        }

        // --- Login Button Listener ---
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pwd = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && pwd.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, pwd)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Login Success! Syncing data...", Toast.LENGTH_SHORT).show()
                        // Automatically sync data after successful login
                        syncDataAndGoMain()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        // --- Register Button Listener ---
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pwd = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && pwd.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, pwd)
                    .addOnSuccessListener {
                        // Create a default profile in Firestore upon successful registration
                        FirebaseHelper.createUserProfile(email)
                        Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
                        goToMain()
                    }
                    .addOnFailureListener { e -> Toast.makeText(this, "Register Failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    /** Downloads user data backup and profile information from Firebase and restores it locally. */
    private fun syncDataAndGoMain() {
        // 1. Sync User Profile (Name and School)
        FirebaseHelper.downloadProfile { name, school ->
            if (name.isNotEmpty()) SettingsStorage.setUserName(this, name)
            if (school.isNotEmpty()) SettingsStorage.setUserSchool(this, school)
        }

        // 2. Sync Habit and Focus History Data Backup
        FirebaseHelper.downloadBackup(
            onSuccess = { habitsJson, sessionsJson ->
                // Define TypeToken for generic list deserialization
                val habitType = object : TypeToken<ArrayList<Habit>>() {}.type
                val sessionType = object : TypeToken<ArrayList<FocusSession>>() {}.type

                // Deserialize JSON strings
                val habits: ArrayList<Habit> = gson.fromJson(habitsJson, habitType)
                val sessions: ArrayList<FocusSession> = gson.fromJson(sessionsJson, sessionType)

                // Restore Habits using the dedicated storage method
                HabitStorage.saveHabits(this, habits)

                // Restore Focus Sessions by directly overwriting SharedPreferences data
                getSharedPreferences("focus_data", Context.MODE_PRIVATE)
                    .edit().putString("sessions_list", sessionsJson).apply()

                // Re-schedule all enabled alarms
                for(h in habits) if(h.isEnabled) AlarmScheduler.scheduleHabit(this, h)

                goToMain() // Navigate to main activity after successful sync
            },
            onFailure = {
                // If no backup is found, proceed to main activity with existing local data
                goToMain()
            }
        )
    }

    /** Navigates to the main activity and finishes the current login activity. */
    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}