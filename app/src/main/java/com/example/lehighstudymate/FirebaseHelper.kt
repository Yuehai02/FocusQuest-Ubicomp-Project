package com.example.lehighstudymate

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson

// Data class to represent a user entry on the leaderboard.
data class LeaderboardUser(
    val email: String = "",
    val totalMinutes: Int = 0, // Total accumulated focus time
    val name: String = "",
    val isPublic: Boolean = true // Flag indicating if the user's stats are visible on the leaderboard (default is public)
)

// Helper object for all Firebase Authentication and Firestore operations.
object FirebaseHelper {
    private val db = FirebaseFirestore.getInstance() // Firestore database instance
    private val auth = FirebaseAuth.getInstance() // Firebase Authentication instance
    private val gson = Gson() // Gson instance for serialization/deserialization

    /** Initializes a new user profile in Firestore upon registration. */
    fun createUserProfile(email: String) {
        val uid = auth.currentUser?.uid ?: return // Get current user's UID or exit
        // Default user data structure
        val user = hashMapOf(
            "email" to email,
            "totalMinutes" to 0,
            "name" to "Student",
            "school" to "Lehigh University",
            "isPublic" to true // 👈 Initialize the privacy field to public
        )
        // Save the profile document using the UID
        db.collection("users").document(uid).set(user)
    }

    /** 🔥 NEW: Updates the user's privacy status for the leaderboard. 🔥 */
    fun setPrivacy(isPublic: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        // Update the 'isPublic' field in the user's document
        db.collection("users").document(uid)
            .update("isPublic", isPublic)
    }

    /** 🔥 MODIFIED: Fetches the leaderboard data, filtered by privacy settings. 🔥
     * @param onSuccess Callback function receiving the filtered list of LeaderboardUser objects.
     */
    fun getLeaderboard(onSuccess: (List<LeaderboardUser>) -> Unit) {
        db.collection("users")
            .orderBy("totalMinutes", Query.Direction.DESCENDING) // Order by total time descending
            .limit(50) // Fetch a larger number to ensure enough users remain after filtering
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(LeaderboardUser::class.java)
                // CORE LOGIC: Filter the list to only include users who set 'isPublic' to true
                val filteredList = list.filter { it.isPublic }.take(10) // Take only the top 10 public users
                onSuccess(filteredList)
            }
    }

    // ... (The following methods remain functionally unchanged) ...
    /** Uploads or updates the user's name and school fields. */
    fun uploadProfile(name: String, school: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf("name" to name, "school" to school)
        // Use SetOptions.merge() to only update the specified fields
        db.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener { onSuccess() }
    }

    /** Downloads the user's profile name and school. */
    fun downloadProfile(onSuccess: (String, String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener {
            onSuccess(it.getString("name") ?: "", it.getString("school") ?: "")
        }
    }

    /** Atomically updates the user's total focus time using a Firestore transaction. */
    fun uploadFocusSession(minutes: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(uid)
        // Use a transaction to safely increment the totalMinutes field
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentTotal = snapshot.getLong("totalMinutes") ?: 0
            transaction.update(userRef, "totalMinutes", currentTotal + minutes)
        }
    }

    /** Retrieves the user's total accumulated focus time. */
    fun getMyTotalTime(onResult: (Int) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener {
            onResult(it.getLong("totalMinutes")?.toInt() ?: 0)
        }
    }

    /** Uploads user's habits and focus history backup data to a private subcollection. */
    fun uploadBackup(habits: List<Habit>, focusHistory: List<FocusSession>, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Not logged in")
        val backupData = hashMapOf("habits_backup" to gson.toJson(habits), "focus_history_backup" to gson.toJson(focusHistory), "last_backup_time" to System.currentTimeMillis())
        // Store in a 'private_data' subcollection for sensitive information
        db.collection("users").document(uid).collection("private_data").document("backup").set(backupData).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it.message ?: "Error") }
    }

    /** Downloads user's habits and focus history backup data from a private subcollection. */
    fun downloadBackup(onSuccess: (String, String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Not logged in")
        db.collection("users").document(uid).collection("private_data").document("backup").get().addOnSuccessListener { if (it.exists()) onSuccess(it.getString("habits_backup") ?: "[]", it.getString("focus_history_backup") ?: "[]") else onFailure("No backup found") }.addOnFailureListener { onFailure(it.message ?: "Error") }
    }
}