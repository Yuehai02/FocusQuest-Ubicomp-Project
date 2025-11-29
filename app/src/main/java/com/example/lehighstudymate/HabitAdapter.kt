package com.example.lehighstudymate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter for the RecyclerView to display a list of Habit items.
class HabitAdapter(
    private val habits: ArrayList<Habit>,
    private val onClick: (Habit) -> Unit, // New: Callback for clicking the card (for editing)
    private val onDelete: (Habit) -> Unit // Callback for clicking the trash can icon (for deletion)
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    // ViewHolder class to cache view components for a single habit item.
    class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_habit_name) // TextView for the habit name
        val time: TextView = view.findViewById(R.id.tv_habit_time) // TextView for the scheduled time
        val delete: ImageView = view.findViewById(R.id.btn_delete) // ImageView for the delete button
    }

    // Creates and returns a new ViewHolder instance.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        // Inflate the layout for a single habit item.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    // Binds the data from the Habit list to the views in the ViewHolder.
    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.name.text = habit.name
        // Format the hour and minute into a HH:mm string
        holder.time.text = String.format("%02d:%02d", habit.hour, habit.minute)

        // New: Set click listener for the entire item view -> Triggers editing
        holder.itemView.setOnClickListener {
            onClick(habit)
        }

        // Set click listener for the delete icon -> Triggers deletion
        holder.delete.setOnClickListener {
            onDelete(habit)
        }
    }

    // Returns the total number of items in the habits list.
    override fun getItemCount() = habits.size
}