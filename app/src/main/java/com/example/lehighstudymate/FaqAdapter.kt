package com.example.lehighstudymate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter for the RecyclerView to display Frequently Asked Questions (FAQ) with expandable answers.
class FaqAdapter(private val faqList: List<FaqItem>) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    // ViewHolder class to cache view components for a single FAQ item.
    class FaqViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // TextView for the question
        val tvQuestion: TextView = view.findViewById(R.id.tv_question)
        // TextView for the answer (content that expands/collapses)
        val tvAnswer: TextView = view.findViewById(R.id.tv_answer)
        // ImageView for the expansion/collapse indicator (arrow)
        val ivArrow: ImageView = view.findViewById(R.id.iv_arrow)
    }

    // Creates and returns a new ViewHolder instance.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        // Inflate the layout for a single FAQ item.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_faq, parent, false)
        return FaqViewHolder(view)
    }

    // Binds the data from the FaqItem list to the views in the ViewHolder.
    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val item = faqList[position] // Get the current FaqItem

        holder.tvQuestion.text = item.question // Set the question text
        holder.tvAnswer.text = item.answer // Set the answer text

        // Show or hide the answer based on the expansion state (isExpanded in FaqItem data class)
        val isVisible = item.isExpanded
        holder.tvAnswer.visibility = if (isVisible) View.VISIBLE else View.GONE

        // Rotate the arrow indicator: 180 degrees when expanded, 0 degrees when collapsed
        holder.ivArrow.rotation = if (isVisible) 180f else 0f

        // Click listener to toggle collapse/expand state
        holder.itemView.setOnClickListener {
            item.isExpanded = !item.isExpanded // Toggle the expansion state
            notifyItemChanged(position) // Refresh this specific item view to update visibility and rotation
        }
    }

    // Returns the total number of items in the FAQ list.
    override fun getItemCount() = faqList.size
}