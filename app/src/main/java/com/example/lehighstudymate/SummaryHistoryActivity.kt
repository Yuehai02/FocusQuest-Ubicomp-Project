package com.example.lehighstudymate
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class SummaryHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary_history)

        val tv = findViewById<TextView>(R.id.tv_all_notes)
        val notes = SummaryStorage.getNotes(this)

        if (notes.isEmpty()) {
            tv.text = "No saved summaries yet."
        } else {
            val sb = StringBuilder()
            for (note in notes) {
                sb.append("📅 ${note.time}\n")
                sb.append("-------------------\n")
                sb.append("${note.content}\n\n\n")
            }
            tv.text = sb.toString()
        }
    }
}
