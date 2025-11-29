package com.example.lehighstudymate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

// Custom View class to draw a Donut (Pie) Chart displaying focus session statistics for the day.
class PieChartView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG) // Paint object with anti-aliasing enabled
    private val rectF = RectF() // Rectangle to define the bounds of the oval shape for the arc
    private var data: Map<String, Int> = emptyMap() // Data: Map of Task Name to Duration (minutes)

    private val colors = listOf(
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#607D8B")  // Blue Grey
    )

    /** Sets the data for the chart and triggers a redraw. */
    fun setData(stats: Map<String, Int>) {
        this.data = stats
        invalidate() // Request the view to be redrawn
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            // If there is no data, draw the empty state circle
            drawEmptyState(canvas)
            return
        }

        val total = data.values.sum().toFloat()
        var startAngle = -90f // Start drawing from the top center
        val width = width.toFloat()
        val height = height.toFloat()
        val radius = min(width, height) / 2 * 0.8f // Calculate radius, leaving a margin

        // Define the rectangle bounding the circular arcs
        rectF.set(width / 2 - radius, height / 2 - radius, width / 2 + radius, height / 2 + radius)

        var colorIndex = 0
        for ((_, value) in data) {
            // Calculate the sweep angle (portion of the circle) for the current data slice
            val sweepAngle = (value / total) * 360f
            paint.color = colors[colorIndex % colors.size] // Cycle through defined colors
            paint.style = Paint.Style.FILL

            // Draw the arc (pie slice)
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)

            startAngle += sweepAngle // Move the starting point for the next slice
            colorIndex++
        }

        // Draw the white circle in the center to create a Donut Chart appearance
        paint.color = Color.WHITE
        // The white hole radius is 50% of the overall chart radius
        canvas.drawCircle(width / 2, height / 2, radius * 0.5f, paint)
    }

    /** Draws a grey outline circle and a "No Data Today" text when the data map is empty. */
    private fun drawEmptyState(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val radius = min(width, height) / 2 * 0.8f

        // Draw the grey circle outline
        paint.color = Color.LTGRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(width / 2, height / 2, radius, paint)

        // Draw the centered text
        paint.style = Paint.Style.FILL
        paint.color = Color.GRAY
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        // Adjust Y position slightly to center the text vertically
        canvas.drawText("No Data Today", width / 2, height / 2 + 15, paint)
    }
}