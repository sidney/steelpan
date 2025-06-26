package com.example.steelpan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import kotlin.math.*

class SteelpanView(context: Context, private val configManager: ConfigurationManager, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dragPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val notes = mutableListOf<NoteRegion>()

    private var isConfigMode = false
    private var draggedNote: NoteRegion? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var leftDrumBounds = RectF()
    private var rightDrumBounds = RectF()

    // Available notes for configuration
    private val availableNotes = listOf(
        "C3", "C#3", "D3", "D#3", "E3", "F3", "F#3", "G3", "G#3", "A3", "A#3", "B3",
        "C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4",
        "C5"
    )

    // Note frequencies (Hz) - extended range
    private val noteFrequencies = mapOf(
        "C3" to 130.81f, "C#3" to 138.59f, "D3" to 146.83f, "D#3" to 155.56f, "E3" to 164.81f,
        "F3" to 174.61f, "F#3" to 185.00f, "G3" to 196.00f, "G#3" to 207.65f, "A3" to 220.00f,
        "A#3" to 233.08f, "B3" to 246.94f, "C4" to 261.63f, "C#4" to 277.18f, "D4" to 293.66f,
        "D#4" to 311.13f, "E4" to 329.63f, "F4" to 349.23f, "F#4" to 369.99f, "G4" to 392.00f,
        "G#4" to 415.30f, "A4" to 440.00f, "A#4" to 466.16f, "B4" to 493.88f, "C5" to 523.25f
    )

    init {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 28f
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD

        dragPaint.style = Paint.Style.FILL
        dragPaint.color = Color.argb(150, 255, 0, 0)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupDrumBounds()
        loadDefaultConfiguration()
    }

    private fun setupDrumBounds() {
        val centerY = height / 2f
        val drumRadius = min(width, height) * 0.35f
        val leftCenterX = width * 0.3f
        val rightCenterX = width * 0.7f

        leftDrumBounds.set(
            leftCenterX - drumRadius,
            centerY - drumRadius,
            leftCenterX + drumRadius,
            centerY + drumRadius
        )

        rightDrumBounds.set(
            rightCenterX - drumRadius,
            centerY - drumRadius,
            rightCenterX + drumRadius,
            centerY + drumRadius
        )
    }

    fun setConfigMode(configMode: Boolean) {
        isConfigMode = configMode
        invalidate()
    }

    fun loadDefaultConfiguration() {
        notes.clear()
        val centerY = height / 2f
        val drumRadius = min(width, height) * 0.35f
        val leftCenterX = width * 0.3f
        val rightCenterX = width * 0.7f

        // Left drum layout (default configuration)
        setupDefaultLeftDrum(leftCenterX, centerY, drumRadius)
        setupDefaultRightDrum(rightCenterX, centerY, drumRadius)

        invalidate()
    }

    private fun setupDefaultLeftDrum(centerX: Float, centerY: Float, radius: Float) {
        // Outer ring
        addNote("E3", centerX - radius * 0.7f, centerY - radius * 0.4f, radius * 0.25f, 0)
        addNote("E4", centerX - radius * 0.2f, centerY - radius * 0.8f, radius * 0.25f, 0)
        addNote("C4", centerX + radius * 0.3f, centerY - radius * 0.8f, radius * 0.25f, 0)
        addNote("F#3", centerX + radius * 0.8f, centerY - radius * 0.3f, radius * 0.25f, 0)
        addNote("B3", centerX + radius * 0.8f, centerY + radius * 0.3f, radius * 0.25f, 0)
        addNote("D3", centerX + radius * 0.2f, centerY + radius * 0.8f, radius * 0.25f, 0)
        addNote("G#3", centerX - radius * 0.3f, centerY + radius * 0.8f, radius * 0.25f, 0)

        // Inner ring
        addNote("G#4", centerX - radius * 0.35f, centerY - radius * 0.1f, radius * 0.15f, 0)
        addNote("F#4", centerX - radius * 0.1f, centerY - radius * 0.35f, radius * 0.15f, 0)
        addNote("D4", centerX + radius * 0.1f, centerY - radius * 0.1f, radius * 0.15f, 0)
        addNote("F4", centerX + radius * 0.35f, centerY + radius * 0.1f, radius * 0.15f, 0)
        addNote("A3", centerX, centerY + radius * 0.35f, radius * 0.15f, 0)
    }

    private fun setupDefaultRightDrum(centerX: Float, centerY: Float, radius: Float) {
        // Outer ring
        addNote("A3", centerX - radius * 0.7f, centerY - radius * 0.4f, radius * 0.25f, 1)
        addNote("D#4", centerX - radius * 0.2f, centerY - radius * 0.8f, radius * 0.25f, 1)
        addNote("B3", centerX + radius * 0.3f, centerY - radius * 0.8f, radius * 0.25f, 1)
        addNote("F3", centerX + radius * 0.8f, centerY - radius * 0.3f, radius * 0.25f, 1)
        addNote("A#3", centerX + radius * 0.8f, centerY + radius * 0.3f, radius * 0.25f, 1)
        addNote("C#3", centerX + radius * 0.2f, centerY + radius * 0.8f, radius * 0.25f, 1)
        addNote("G3", centerX - radius * 0.3f, centerY + radius * 0.8f, radius * 0.25f, 1)

        // Inner ring
        addNote("G4", centerX - radius * 0.35f, centerY - radius * 0.1f, radius * 0.15f, 1)
        addNote("F4", centerX - radius * 0.1f, centerY - radius * 0.35f, radius * 0.15f, 1)
        addNote("C#4", centerX + radius * 0.1f, centerY - radius * 0.1f, radius * 0.15f, 1)
    }

    private fun addNote(note: String, x: Float, y: Float, radius: Float, drumIndex: Int) {
        notes.add(NoteRegion(note, x, y, radius, drumIndex))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(Color.rgb(240, 235, 220))

        // Draw drum outlines
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = if (isConfigMode) Color.rgb(150, 100, 100) else Color.rgb(100, 100, 100)

        val drumRadius = min(width, height) * 0.35f
        canvas.drawCircle(width * 0.3f, height / 2f, drumRadius, paint)
        canvas.drawCircle(width * 0.7f, height / 2f, drumRadius, paint)

        // Draw notes
        for (note in notes) {
            if (note == draggedNote) continue // Skip dragged note, draw it separately

            // Draw note area
            paint.style = Paint.Style.FILL
            paint.color = if (isConfigMode) Color.rgb(180, 160, 120) else Color.rgb(200, 180, 140)
            canvas.drawCircle(note.x, note.y, note.radius, paint)

            // Draw note border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = if (isConfigMode) Color.rgb(100, 80, 60) else Color.rgb(120, 100, 80)
            canvas.drawCircle(note.x, note.y, note.radius, paint)

            // Draw note text
            val textSize = if (note.radius < 50) 24f else 32f
            textPaint.textSize = textSize
            canvas.drawText(note.note, note.x, note.y + textSize/3, textPaint)
        }

        // Draw dragged note if any
        draggedNote?.let { note ->
            canvas.drawCircle(note.x, note.y, note.radius, dragPaint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.RED
            canvas.drawCircle(note.x, note.y, note.radius, paint)

            val textSize = if (note.radius < 50) 24f else 32f
            textPaint.textSize = textSize
            textPaint.color = Color.RED
            canvas.drawText(note.note, note.x, note.y + textSize/3, textPaint)
            textPaint.color = Color.BLACK
        }

        // Draw title
        textPaint.textSize = 48f
        textPaint.color = Color.rgb(80, 60, 40)
        val title = if (isConfigMode) "CONFIGURATION MODE" else "STEELPAN DOUBLE GUITAR"
        canvas.drawText(title, width/2f, height * 0.1f, textPaint)

        // Draw instructions in config mode
        if (isConfigMode) {
            textPaint.textSize = 24f
            textPaint.color = Color.rgb(100, 80, 60)
            canvas.drawText("Drag notes to reposition • Long press to change note", width/2f, height * 0.15f, textPaint)
        }

        textPaint.textSize = 32f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isConfigMode) {
            return handlePlayMode(event)
        } else {
            return handleConfigMode(event)
        }
    }

    private fun handlePlayMode(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex

        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                handleTouch(x, y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleConfigMode(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Find touched note
                for (note in notes) {
                    val distance = sqrt((x - note.x).pow(2) + (y - note.y).pow(2))
                    if (distance <= note.radius) {
                        draggedNote = note
                        dragOffsetX = x - note.x
                        dragOffsetY = y - note.y
                        invalidate()
                        return true
                    }
                }

                // If no note touched, check if touch is inside drum bounds to add new note
                if (isInsideDrum(x, y) != -1) {
                    showAddNoteDialog(x, y, isInsideDrum(x, y))
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                draggedNote?.let { note ->
                    note.x = x - dragOffsetX
                    note.y = y - dragOffsetY

                    // Update drum association based on position
                    note.drumIndex = isInsideDrum(note.x, note.y).coerceAtLeast(note.drumIndex)

                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                draggedNote?.let { note ->
                    // Snap to drum boundaries if outside
                    val drumIndex = isInsideDrum(note.x, note.y)
                    if (drumIndex == -1) {
                        // Move back to original drum or remove
                        showRemoveNoteDialog(note)
                    } else {
                        note.drumIndex = drumIndex
                    }

                    draggedNote = null
                    invalidate()
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    // Long press to change note
    private var longPressRunnable: Runnable? = null

    private fun startLongPressTimer(note: NoteRegion) {
        longPressRunnable = Runnable {
            showChangeNoteDialog(note)
        }
        postDelayed(longPressRunnable, 1000) // 1 second long press
    }

    private fun cancelLongPressTimer() {
        longPressRunnable?.let {
            removeCallbacks(it)
            longPressRunnable = null
        }
    }

    private fun isInsideDrum(x: Float, y: Float): Int {
        return when {
            leftDrumBounds.contains(x, y) -> 0
            rightDrumBounds.contains(x, y) -> 1
            else -> -1
        }
    }

    private fun showAddNoteDialog(x: Float, y: Float, drumIndex: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Add Note")

        val availableNotesList = availableNotes.toTypedArray()
        builder.setItems(availableNotesList) { _, which ->
            val selectedNote = availableNotesList[which]
            val radius = min(width, height) * 0.08f // Default radius
            addNote(selectedNote, x, y, radius, drumIndex)
            invalidate()
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showChangeNoteDialog(note: NoteRegion) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Change Note")

        val availableNotesList = availableNotes.toTypedArray()
        builder.setItems(availableNotesList) { _, which ->
            val selectedNote = availableNotesList[which]
            note.note = selectedNote
            invalidate()
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showRemoveNoteDialog(note: NoteRegion) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Note Outside Drum")
        builder.setMessage("Remove this note or move it back?")

        builder.setPositiveButton("Remove") { _, _ ->
            notes.remove(note)
            invalidate()
        }

        builder.setNegativeButton("Move Back") { _, _ ->
            // Move to center of original drum
            val drumBounds = if (note.drumIndex == 0) leftDrumBounds else rightDrumBounds
            note.x = drumBounds.centerX()
            note.y = drumBounds.centerY()
            invalidate()
        }

        builder.show()
    }

    private fun handleTouch(x: Float, y: Float) {
        for (note in notes) {
            val distance = sqrt((x - note.x).pow(2) + (y - note.y).pow(2))
            if (distance <= note.radius) {
                playNote(note.note)
                break
            }
        }
    }

    private fun playNote(note: String) {
        noteFrequencies[note]?.let { frequency ->
            (context as MainActivity).playNote(frequency)
        }
    }

    fun getCurrentConfiguration(): DrumConfiguration {
        val noteConfigs = notes.map { note ->
            NoteConfiguration(note.note, note.x, note.y, note.radius, note.drumIndex)
        }
        return DrumConfiguration(noteConfigs)
    }

    fun loadConfiguration(config: DrumConfiguration) {
        notes.clear()
        for (noteConfig in config.notes) {
            notes.add(NoteRegion(
                noteConfig.note,
                noteConfig.x,
                noteConfig.y,
                noteConfig.radius,
                noteConfig.drumIndex
            ))
        }
        invalidate()
    }
}

data class NoteRegion(
    var note: String,
    var x: Float,
    var y: Float,
    var radius: Float,
    var drumIndex: Int = 0 // 0 for left drum, 1 for right drum
)