package com.example.lehighstudymate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

// Custom View class to draw a simple line chart displaying focus trend over the last 7 days.
class SimpleLineChart(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // Paint for the main line drawing
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3") // Blue line color
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND // Rounded line endings
    }
    // Paint for the data points (dots)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0") // Darker blue fill color
        style = Paint.Style.FILL
    }
    // Paint for the axes
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY // Light gray for axes
        strokeWidth = 3f
    }
    // Paint for the text (dates)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    // Data source: List of (Date String, Minutes) pairs
    private var dataPoints = mutableListOf<Pair<String, Int>>()
    // List to store the calculated screen coordinates for touch detection
    private val touchPoints = mutableListOf<Pair<Float, Float>>()

    /**
     * Processes focus sessions to generate 7-day trend data.
     * @param sessions The list of all historical FocusSessions.
     */
    fun setRealData(sessions: List<FocusSession>) {
        // 1. Generate dates for the last 7 days (e.g., "11/28")
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        // Use LinkedHashMap to ensure the dates are ordered
        val dateMap = linkedMapOf<String, Int>()

        // Initialize 7 days of data with 0 minutes
        for (i in 6 downTo 0) {
            val calCopy = calendar.clone() as Calendar
            calCopy.add(Calendar.DAY_OF_YEAR, -i) // Go back 'i' days
            val dateStr = dateFormat.format(calCopy.time)
            dateMap[dateStr] = 0
        }

        // Aggregate data from sessions
        for (session in sessions) {
            val dateStr = dateFormat.format(java.util.Date(session.timestamp))
            if (dateMap.containsKey(dateStr)) {
                // Sum the duration minutes for the same day
                dateMap[dateStr] = dateMap[dateStr]!! + session.durationMinutes
            }
        }

        dataPoints = dateMap.toList().toMutableList() // Convert map to a list of pairs
        invalidate() // Request redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val w = width.toFloat() // View width
        val h = height.toFloat() // View height
        val paddingBottom = 60f // Space for date labels
        val paddingLeft = 20f // Left padding for the first point
        val chartH = h - paddingBottom // Chart height (excluding bottom padding)

        // Draw the X-axis line
        canvas.drawLine(paddingLeft, chartH, w, chartH, axisPaint)

        // Determine the maximum value for Y-axis scaling, ensuring a minimum scale of 10
        val maxVal = (dataPoints.maxOfOrNull { it.second } ?: 60).toFloat().coerceAtLeast(10f)
        // Calculate the horizontal step between each data point
        val stepX = (w - paddingLeft * 2) / (dataPoints.size - 1).coerceAtLeast(1)

        val path = Path() // Path object for drawing the line
        touchPoints.clear() // Clear old touch coordinates

        // Iterate through data points to calculate coordinates and draw
        dataPoints.forEachIndexed { index, pair ->
            // X coordinate calculation
            val x = paddingLeft + index * stepX
            // Y coordinate calculation (inverting: 0 minutes is at chartH, maxVal is near the top)
            // Scale the data point value relative to maxVal, then map to 80% of chartH (leaving margin at top)
            val y = chartH - (pair.second / maxVal) * (chartH * 0.8f)

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y) // Build the line path

            // Draw the data point circle (dot)
            canvas.drawCircle(x, y, 12f, dotPaint)
            // Draw the date text below the X-axis
            canvas.drawText(pair.first, x, h - 10f, textPaint)

            // Record the point coordinates for touch detection
            touchPoints.add(x to y)
        }
        // Draw the final line connecting the points
        canvas.drawPath(path, linePaint)
    }

    // Touch Interaction: Tapping near a point displays its data
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchX = event.x
            val touchY = event.y

            // Iterate through touch points to find the closest one
            dataPoints.forEachIndexed { index, pair ->
                val (px, py) = touchPoints[index]
                // Check if the touch is within a 50-pixel radius (hit area) of a data point
                if (abs(touchX - px) < 50 && abs(touchY - py) < 50) {
                    // Display the date and minute value in a Toast
                    Toast.makeText(context, "${pair.first}: ${pair.second} mins", Toast.LENGTH_SHORT).show()
                    return true // Consume the event
                }
            }
        }
        return super.onTouchEvent(event) // Pass the event to the superclass if no hit
    }
}