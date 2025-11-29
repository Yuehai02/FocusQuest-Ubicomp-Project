package com.example.lehighstudymate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

// A BroadcastReceiver triggered by the AlarmManager to display a reminder notification for a scheduled habit.
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Get the NotificationManager service
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_channel"
        // Retrieve the habit name passed through the Intent extras
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Time to Focus!"

        // Create a notification channel for Android O (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to launch SuperFocusActivity when the notification is tapped
        val tapIntent = Intent(context, SuperFocusActivity::class.java)
        // Create a PendingIntent to be executed when the user taps the notification
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE // Required flag for security
        )

        // Build the notification content
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Set a generic alarm icon
            .setContentTitle("Habit Reminder")
            .setContentText("It's time for: $habitName")
            .setContentIntent(pendingIntent) // Set the intent that fires when the user taps the notification
            .setAutoCancel(true) // Automatically dismiss the notification when tapped
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set high priority for urgent reminders
            .build()

        // Display the notification. Using System.currentTimeMillis().toInt() as a unique ID.
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}