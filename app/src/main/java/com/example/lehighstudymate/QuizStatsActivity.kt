package com.example.lehighstudymate
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class QuizStatsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)

        val title = TextView(this)
        title.text = "My Vocabulary Book"
        title.textSize = 24f
        layout.addView(title)

        val listView = ListView(this)
        layout.addView(listView)
        setContentView(layout)

        val words = QuizStorage.getUnknownWords(this)
        val displayList = words.map { "❌ ${it.word}" }

        if (displayList.isEmpty()) {
            val empty = TextView(this)
            empty.text = "\nNo unknown words yet. Great job!"
            layout.addView(empty)
        } else {
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        }
    }
}