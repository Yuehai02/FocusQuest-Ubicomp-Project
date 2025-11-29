package com.example.lehighstudymate

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lehighstudymate.databinding.ActivityHabitManagerBinding
import java.util.Calendar
import java.util.Collections

// Activity for managing and scheduling daily habits.
class HabitManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHabitManagerBinding
    private val habitList = ArrayList<Habit>()
    private lateinit var adapter: HabitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHabitManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Initialize the Adapter (passing in edit and delete logic)
        adapter = HabitAdapter(habitList,
            onClick = { habit -> showEditDialog(habit) }, // Click card -> Edit
            onDelete = { habit -> deleteHabit(habit) }    // Click trash can -> Delete
        )

        binding.rvHabits.layoutManager = LinearLayoutManager(this)
        binding.rvHabits.adapter = adapter

        // 2. Load data from storage
        loadHabits()

        // 3. Core Feature: Drag and Drop Sorting (ItemTouchHelper)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // Allow vertical dragging
            0 // Do not handle swipe-to-dismiss (using a button for deletion)
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Swap the positions in the underlying list
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                Collections.swap(habitList, fromPos, toPos)
                adapter.notifyItemMoved(fromPos, toPos)

                // Save the new order after every move
                HabitStorage.saveHabits(this@HabitManagerActivity, habitList)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Left empty as we use a button for deletion
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvHabits)

        // 4. Add Button (FAB)
        binding.fabAdd.setOnClickListener {
            showEditDialog(null) // Pass null to indicate "Add New Habit"
        }

        // 5. Sort Button (Sort by time)
        binding.btnSort.setOnClickListener {
            // Sort the list based on time (converted to minutes for comparison)
            habitList.sortBy { it.hour * 60 + it.minute }
            adapter.notifyDataSetChanged()
            // Save the newly sorted list
            HabitStorage.saveHabits(this, habitList)
            Toast.makeText(this, "Sorted by time!", Toast.LENGTH_SHORT).show()
        }
    }

    /** Clears the list and reloads habits from storage. */
    private fun loadHabits() {
        habitList.clear()
        habitList.addAll(HabitStorage.getHabits(this))
        adapter.notifyDataSetChanged()
    }

    // --- Core Feature: Unified Add/Edit Dialog ---
    /**
     * Shows a dialog to input the habit name and handles transition to the time picker.
     * @param habitToEdit The Habit object to edit, or null if adding a new habit.
     */
    private fun showEditDialog(habitToEdit: Habit?) {
        val input = EditText(this)
        input.setPadding(50, 30, 50, 30) // Add padding for better looks

        val title = if (habitToEdit == null) "New Habit" else "Edit Habit"

        // If editing, pre-fill the old name
        if (habitToEdit != null) {
            input.setText(habitToEdit.name)
        } else {
            input.hint = "Enter Habit Name (e.g., Read)"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Next") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    // If editing, use the old time; if adding, use the current time
                    val initialHour = habitToEdit?.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val initialMinute = habitToEdit?.minute ?: Calendar.getInstance().get(Calendar.MINUTE)

                    showTimePicker(name, initialHour, initialMinute, habitToEdit)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Shows the TimePickerDialog to select the scheduled time for the habit.
     * @param name The habit name confirmed in the previous step.
     * @param hour The initial hour for the picker.
     * @param minute The initial minute for the picker.
     * @param oldHabit The original Habit object if editing, or null if adding.
     */
    private fun showTimePicker(name: String, hour: Int, minute: Int, oldHabit: Habit?) {
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->

            if (oldHabit == null) {
                // --- Logic A: Add New Habit Mode ---
                val newHabit = Habit(name = name, hour = selectedHour, minute = selectedMinute)
                habitList.add(newHabit)
                adapter.notifyItemInserted(habitList.size - 1) // Notify adapter for smooth update

                // Schedule alarm & Save to storage
                AlarmScheduler.scheduleHabit(this, newHabit)
                HabitStorage.saveHabits(this, habitList)
                Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show()

            } else {
                // --- Logic B: Edit Existing Habit Mode ---
                // 1. Cancel the old alarm first (Crucial!)
                AlarmScheduler.cancelHabit(this, oldHabit)

                // 2. Update the existing data object
                oldHabit.name = name
                oldHabit.hour = selectedHour
                oldHabit.minute = selectedMinute
                adapter.notifyDataSetChanged() // Notify adapter for full update

                // 3. Set the new alarm & Save to storage
                AlarmScheduler.scheduleHabit(this, oldHabit)
                HabitStorage.saveHabits(this, habitList)
                Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show()
            }

        }, hour, minute, true).show()
    }

    /** Deletes a habit, cancels its alarm, and updates storage. */
    private fun deleteHabit(habit: Habit) {
        // Cancel the scheduled alarm
        AlarmScheduler.cancelHabit(this, habit)
        // Remove from the local list
        habitList.remove(habit)
        adapter.notifyDataSetChanged()
        // Save the updated list to storage
        HabitStorage.saveHabits(this, habitList)
        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
    }
}