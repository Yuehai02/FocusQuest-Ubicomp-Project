package com.example.lehighstudymate

import android.content.Context
import kotlin.math.min

// Data class to hold the current state of the boss for display
data class BossState(
    val level: Int, // The current level of the boss
    val name: String, // The name of the current boss
    val currentHp: Int, // The boss's current Hit Points (HP)
    val maxHp: Int, // The boss's maximum Hit Points (HP)
    val iconRes: Int, // Resource ID for the boss icon
    val statusMessage: String // Message summarizing the last action or current status
)

object BossGameManager {
    // SharedPreferences file name for saving game state
    private const val PREF_NAME = "focus_rpg_save"
    // Key for saving the boss level
    private const val KEY_LEVEL = "boss_level"
    // Key for saving the boss's current HP
    private const val KEY_HP = "boss_current_hp"
    // Key for saving the time of the last focus session
    private const val KEY_LAST_TIME = "last_fight_time"

    // Configuration list for initial bosses: (Level, Name, Max HP)
    private val BOSS_CONFIG = listOf(
        Triple(1, "Procrastination Slime", 300),
        Triple(2, "Distraction Goblin", 1200),
        Triple(3, "Social Media Demon", 3000),
        Triple(4, "Burnout Dragon", 10000)
    )

    /**
     * Processes a focus session, calculates damage to the boss, handles level-ups,
     * and saves the new game state.
     * @param context Application context for accessing SharedPreferences.
     * @param focusMinutes The duration of the focus session in minutes.
     * @return The updated BossState.
     */
    fun processFocusSession(context: Context, focusMinutes: Int): BossState {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        var level = prefs.getInt(KEY_LEVEL, 1) // Load current boss level, default to 1
        var currentHp = prefs.getInt(KEY_HP, -1) // Load current HP, -1 means new level or first start
        val lastTime = prefs.getLong(KEY_LAST_TIME, 0L) // Load time of last focus session
        val currentTime = System.currentTimeMillis()

        // Helper function to get boss configuration based on level
        fun getBossInfo(lvl: Int): Triple<Int, String, Int> {
            val index = min(lvl - 1, BOSS_CONFIG.lastIndex) // Use max available config for high levels
            val config = BOSS_CONFIG[index]
            // Scale HP for levels beyond the predefined config list
            val hp = if (lvl > BOSS_CONFIG.size) config.third + (lvl * 2000) else config.third
            return Triple(lvl, config.second, hp)
        }

        var (currentLevel, bossName, maxHp) = getBossInfo(level)
        // If currentHp is -1 (first start or level up without saving HP), initialize it to maxHp
        if (currentHp == -1) currentHp = maxHp

        var healMsg = ""
        // Check if enough time has passed for the boss to heal
        if (lastTime > 0) {
            val hoursPassed = (currentTime - lastTime) / (1000 * 60 * 60)
            if (hoursPassed >= 1) {
                // Boss heals 5% of max HP per hour passed
                val healAmount = (maxHp * 0.05 * hoursPassed).toInt()
                if (healAmount > 0 && currentHp < maxHp) {
                    currentHp = min(maxHp, currentHp + healAmount) // Apply healing, capped at maxHp
                    healMsg = " (Boss healed $healAmount!)"
                }
            }
        }

        var damageToDeal = focusMinutes * 10 // Calculate total damage based on focus minutes
        val totalDamageDealt = damageToDeal // Store the initial damage dealt
        var isLevelUp = false // Flag to check if a boss was defeated
        var defeatedBossName = ""

        // Loop to handle damage and potential boss defeats (level-ups)
        while (damageToDeal > 0) {
            if (currentHp > damageToDeal) {
                currentHp -= damageToDeal // Boss absorbs all remaining damage
                damageToDeal = 0
            } else {
                // Boss is defeated
                damageToDeal -= currentHp // Carry over excess damage
                currentHp = 0
                isLevelUp = true
                defeatedBossName = bossName
                level++ // Advance to the next level
                // Get info for the new boss
                val nextBoss = getBossInfo(level)
                bossName = nextBoss.second
                maxHp = nextBoss.third
                currentHp = maxHp // New boss starts with full HP
            }
        }

        // Determine the status message based on the outcome
        val statusMsg = if (isLevelUp) {
            "Overkill! Defeated $defeatedBossName! Remaining damage hit $bossName!"
        } else {
            "You dealt $totalDamageDealt damage!$healMsg"
        }

        // Save the new game state
        prefs.edit()
            .putInt(KEY_LEVEL, level)
            .putInt(KEY_HP, currentHp)
            .putLong(KEY_LAST_TIME, currentTime)
            .apply()

        // Return the final state
        return BossState(level, bossName, currentHp, maxHp, android.R.drawable.ic_lock_idle_low_battery, statusMsg)
    }

    /**
     * Retrieves the current BossState without applying any damage (focusMinutes = 0).
     * Used typically for initial load or display when no session has ended.
     * @param context Application context.
     * @return The current BossState with a generic message.
     */
    fun getCurrentState(context: Context): BossState {
        // Use processFocusSession with 0 minutes to load current stats without changing HP
        return processFocusSession(context, 0).copy(statusMessage = "Boss is waiting...")
    }
}