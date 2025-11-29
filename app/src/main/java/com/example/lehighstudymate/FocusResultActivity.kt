package com.example.lehighstudymate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lehighstudymate.databinding.ActivityFocusResultBinding
import java.text.SimpleDateFormat
import java.util.Locale

// Activity displayed after a focus session ends, showing results, Boss fight outcome, and leaderboard.
class FocusResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFocusResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadRealData() // Load and process all session data, boss, and leaderboard

        // Set up the button to return to the main screen
        binding.btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // Clear all activities on top of MainActivity for a clean exit
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    /** Loads, processes, and displays all focus session data, boss state, and leaderboard. */
    private fun loadRealData() {
        // 1. Get all local focus session records
        val allSessions = FocusStorage.getSessions(this)

        // 2. Calculate damage (minutes) for the Boss battle
        // Check if the activity was started from a completed focus session
        val isFromSession = intent.hasExtra("TASK_NAME")
        val damageMinutes = if (isFromSession && allSessions.isNotEmpty()) {
            // Assume the most recent session (index 0) is the one just completed
            allSessions[0].durationMinutes
        } else {
            0 // No damage if not started from a session or if history is empty
        }

        // 3.  Run the RPG Game Engine to process damage and update state
        val bossState = BossGameManager.processFocusSession(this, damageMinutes)
        updateBossUI(bossState) // Update the Boss UI based on the new state

        // 4. Pie Chart logic
        // Get today's aggregated stats (e.g., total time, tasks)
        val todayStats = FocusStorage.getTodayStats(this)
        binding.pieChart.setData(todayStats) // Update the custom Pie Chart view

        // 5. --- Handle List Display (Local History + Leaderboard) ---

        // (A) Build local history string
        val sbLocal = StringBuilder()
        sbLocal.append("📅 Your Recent Sessions:\n")
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        if (allSessions.isEmpty()) {
            sbLocal.append("No local records yet.\n")
        } else {
            // Show only the 5 most recent sessions
            for (session in allSessions.take(5)) {
                val timeStr = dateFormat.format(java.util.Date(session.timestamp))
                sbLocal.append("[$timeStr] ${session.taskName} - ${session.durationMinutes} mins\n")
            }
        }
        // Display local history first
        binding.tvHistoryList.text = sbLocal.toString()

        // (B)  Fetch Global Leaderboard (Asynchronous loading from Firebase)
        FirebaseHelper.getLeaderboard { users ->
            val sbLeaderboard = StringBuilder()
            sbLeaderboard.append("\nGlobal Leaderboard:\n") // Add newline separator

            users.forEachIndexed { index, user ->
                // Mask the middle part of the email for privacy (e.g., t***@lehigh.edu)
                val rawName = user.email.substringBefore("@")
                val maskedName = if (rawName.length > 2) rawName.take(2) + "***" else rawName

                sbLeaderboard.append("${index + 1}. $maskedName - ${user.totalMinutes} mins\n")
            }

            // KEY: Use append to add the leaderboard data to the existing text
            binding.tvHistoryList.append(sbLeaderboard.toString())
        }

        // (C) Update total time (prioritize displaying the cloud-synced total)
        FirebaseHelper.getMyTotalTime { total ->
            binding.tvTotalTime.text = "Total Focus (Cloud): $total mins"
        }
    }

    /** Updates the UI elements related to the Boss's status. */
    private fun updateBossUI(state: BossState) {
        binding.tvBossName.text = "Lv.${state.level} ${state.name}" // Display level and name

        binding.progressHp.max = state.maxHp // Set max HP for progress bar
        binding.progressHp.progress = state.currentHp // Set current HP
        binding.tvBossHp.text = "${state.currentHp} / ${state.maxHp} HP" // Display HP numbers

        binding.tvBattleLog.text = state.statusMessage // Display the battle summary/log
        // Color-code the battle log message based on content
        if (state.statusMessage.contains("Defeated")) { // Changed from VICTORY to Defeated (based on BossGameManager)
            binding.tvBattleLog.setTextColor(android.graphics.Color.YELLOW)
        } else if (state.statusMessage.contains("healed")) {
            binding.tvBattleLog.setTextColor(android.graphics.Color.RED)
        } else {
            // Default color for damage dealt
            binding.tvBattleLog.setTextColor(android.graphics.Color.parseColor("#81C784"))
        }
    }
}