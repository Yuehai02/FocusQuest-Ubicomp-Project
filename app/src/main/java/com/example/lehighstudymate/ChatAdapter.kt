package com.example.lehighstudymate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// This constructor accepts only a List, which is the simplest form usable by any Activity.
class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // ViewHolder class to hold the views for a single chat message item.
    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // TextView for displaying AI messages (left bubble)
        val leftBubble: TextView = view.findViewById(R.id.tv_ai_msg)
        // TextView for displaying User messages (right bubble)
        val rightBubble: TextView = view.findViewById(R.id.tv_user_msg)
    }

    // Called when the RecyclerView needs a new ViewHolder instance to represent an item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // Inflate the layout for a single chat message item.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    // Called by RecyclerView to display the data at the specified position.
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position] // Get the ChatMessage object at the current position

        // Check if the message was sent by the user
        if (msg.isUser) {
            holder.rightBubble.text = msg.content // Set text on the right bubble
            holder.rightBubble.visibility = View.VISIBLE // Show the user's message bubble
            holder.leftBubble.visibility = View.GONE // Hide the AI's message bubble
        } else {
            // Message is from the AI
            holder.leftBubble.text = msg.content // Set text on the left bubble
            holder.leftBubble.visibility = View.VISIBLE // Show the AI's message bubble
            holder.rightBubble.visibility = View.GONE // Hide the user's message bubble
        }
    }

    // Returns the total number of items in the data set held by the adapter.
    override fun getItemCount() = messages.size
}