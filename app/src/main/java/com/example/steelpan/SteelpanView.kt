package com.example.steelpan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
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

    // Extended available notes from A1 through G5 with both sharps and flats
    private val availableNotes = listOf(
        // Octave 1
        "C1", "C#1", "Db1", "D1", "D#1", "Eb1", "E1", "F1", "F#1", "Gb1", "G1", "G#1", "Ab1", "A1", "A#1", "Bb1", "B1",
        // Octave 2
        "C2", "C#2", "Db2", "D2", "D#2", "Eb2", "E2", "F2", "F#2", "Gb2", "G2", "G#2", "Ab2", "A2", "A#2", "Bb2", "B2",
        // Octave 3
        "C3", "C#3", "Db3", "D3", "D#3", "Eb3", "E3", "F3", "F#3", "Gb3", "G3", "G#3", "Ab3", "A3", "A#3", "Bb3", "B3",
        // Octave 4
        "C4", "C#4", "Db4", "D4", "D#4", "Eb4", "E4", "F4", "F#4", "Gb4", "G4", "G#4", "Ab4", "A4", "A#4", "Bb4", "B4",
        // Octave 5
        "C5", "C#5", "Db5", "D5", "D#5", "Eb5", "E5", "F5", "F#5", "Gb5", "G5"
    )

    // Extended note frequencies (Hz) from A1 through G5 with both sharps and flats
    private val noteFrequencies = mapOf(
        // Octave 1
        "C1" to 32.70f, "C#1" to 34.65f, "Db1" to 34.65f, "D1" to 36.71f, "D#1" to 38.89f, "Eb1" to 38.89f,
        "E1" to 41.20f, "F1" to 43.65f, "F#1" to 46.25f, "Gb1" to 46.25f, "G1" to 49.00f, "G#1" to 51.91f, "Ab1" to 51.91f,
        "A1" to 55.00f, "A#1" to 58.27f, "Bb1" to 58.27f, "B1" to 61.74f,
        // Octave 2
        "C2" to 65.41f, "C#2" to 69.30f, "Db2" to 69.30f, "D2" to 73.42f, "D#2" to 77.78f, "Eb2" to 77.78f,
        "E2" to 82.41f, "F2" to 87.31f, "F#2" to 92.50f, "Gb2" to 92.50f, "G2" to 98.00f, "G#2" to 103.83f, "Ab2" to 103.83f,
        "A2" to 110.00f, "A#2" to 116.54f, "Bb2" to 116.54f, "B2" to 123.47f,
        // Octave 3
        "C3" to 130.81f, "C#3" to 138.59f, "Db3" to 138.59f, "D3" to 146.83f, "D#3" to 155.56f, "Eb3" to 155.56f,
        "E3" to 164.81f, "F3" to 174.61f, "F#3" to 185.00f, "Gb3" to 185.00f, "G3" to 196.00f, "G#3" to 207.65f, "Ab3" to 207.65f,
        "A3" to 220.00f, "A#3" to 233.08f, "Bb3" to 233.08f, "B3" to 246.94f,
        // Octave 4
        "C4" to 261.63f, "C#4" to 277.18f, "Db4" to 277.18f, "D4" to 293.66f, "D#4" to 311.13f, "Eb4" to 311.13f,
        "E4" to 329.63f, "F4" to 349.23f, "F#4" to 369.99f, "Gb4" to 369.99f, "G4" to 392.00f, "G#4" to 415.30f, "Ab4" to 415.30f,
        "A4" to 440.00f, "A#4" to 466.16f, "Bb4" to 466.16f, "B4" to 493.88f,
        // Octave 5
        "C5" to 523.25f, "C#5" to 554.37f, "Db5" to 554.37f, "D5" to 587.33f, "D#5" to 622.25f, "Eb5" to 622.25f,
        "E5" to 659.25f, "F5" to 698.46f, "F#5" to 739.99f, "Gb5" to 739.99f, "G5" to 783.99f
    )

    companion object {
        private const val MIN_CIRCLE_RADIUS_DP = 20f // Smallest radius for highest octave (e.g., Octave 5)
        private const val MAX_CIRCLE_RADIUS_DP = 45f // Largest radius for lowest octave (e.g., Octave 1)
        private const val MIN_OCTAVE = 1
        private const val MAX_OCTAVE = 5
        private const val TAG = "SteelpanView" // For logging
    }

    init {
        textPaint.textAlign = Paint.Align.CENTER
        // textPaint.textSize is set dynamically in onDraw based on radius
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD

        dragPaint.style = Paint.Style.FILL
        dragPaint.color = Color.argb(150, 255, 0, 0) // Semi-transparent red for dragged note
    }

    // Helper function to get octave from note label (e.g., "C4" -> 4)
    private fun getOctaveFromNoteLabel(noteLabel: String): Int {
        if (noteLabel.isEmpty() || !noteLabel.last().isDigit()) {
            Log.w(TAG, "Could not parse octave from note label: $noteLabel. Defaulting to MIN_OCTAVE.")
            return MIN_OCTAVE
        }
        return noteLabel.last().toString().toIntOrNull()?.coerceIn(MIN_OCTAVE, MAX_OCTAVE) ?: MIN_OCTAVE
    }

    // Helper function to calculate radius in Px based on octave
    private fun calculateRadiusForOctave(octave: Int): Float {
        val clampedOctave = octave.coerceIn(MIN_OCTAVE, MAX_OCTAVE)
        // Normalize the octave value to a 0-1 range (0 for MAX_OCTAVE, 1 for MIN_OCTAVE)
        // This is because we want the size to decrease as the octave increases.
        val normalizedOctave = (MAX_OCTAVE - clampedOctave).toFloat() / (MAX_OCTAVE - MIN_OCTAVE).toFloat()

        // Linearly interpolate between min and max radius (in DP)
        val radiusDp = MIN_CIRCLE_RADIUS_DP + (MAX_CIRCLE_RADIUS_DP - MIN_CIRCLE_RADIUS_DP) * normalizedOctave

        // Convert Dp to Px for drawing
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radiusDp, resources.displayMetrics)
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupDrumBounds()
        // It's important that loadDefaultConfiguration or loadConfiguration is called
        // after the view has its dimensions, so radius calculation (which uses resources.displayMetrics)
        // and positioning can be done correctly.
        if (notes.isEmpty()) { // Load default only if no notes are loaded (e.g. by config)
            loadDefaultConfiguration()
        }
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
        // These radii are for positioning, not for the note circle's visual radius
        val drumRadius = min(width, height) * 0.35f
        val leftCenterX = width * 0.3f
        val rightCenterX = width * 0.7f

        // Left drum layout (default configuration)
        setupDefaultLeftDrum(leftCenterX, centerY, drumRadius)
        setupDefaultRightDrum(rightCenterX, centerY, drumRadius)

        invalidate()
    }

    private fun setupDefaultLeftDrum(centerX: Float, centerY: Float, positioningRadius: Float) {
        // Outer ring
        addNote("E3", centerX - positioningRadius * 0.7f, centerY - positioningRadius * 0.4f, 0)
        addNote("E4", centerX - positioningRadius * 0.2f, centerY - positioningRadius * 0.8f, 0)
        addNote("C4", centerX + positioningRadius * 0.3f, centerY - positioningRadius * 0.8f, 0)
        addNote("F#3", centerX + positioningRadius * 0.8f, centerY - positioningRadius * 0.3f, 0)
        addNote("B3", centerX + positioningRadius * 0.8f, centerY + positioningRadius * 0.3f, 0)
        addNote("D3", centerX + positioningRadius * 0.2f, centerY + positioningRadius * 0.8f, 0)
        addNote("G#3", centerX - positioningRadius * 0.3f, centerY + positioningRadius * 0.8f, 0)

        // Inner ring
        addNote("G#4", centerX - positioningRadius * 0.35f, centerY - positioningRadius * 0.1f, 0)
        addNote("F#4", centerX - positioningRadius * 0.1f, centerY - positioningRadius * 0.35f, 0)
        addNote("D4", centerX + positioningRadius * 0.1f, centerY - positioningRadius * 0.1f, 0)
        addNote("F4", centerX + positioningRadius * 0.35f, centerY + positioningRadius * 0.1f, 0)
        addNote("A3", centerX, centerY + positioningRadius * 0.35f, 0) // A3 was used here, ensure it's in availableNotes
    }

    private fun setupDefaultRightDrum(centerX: Float, centerY: Float, positioningRadius: Float) {
        // Outer ring
        addNote("A3", centerX - positioningRadius * 0.7f, centerY - positioningRadius * 0.4f, 1)
        addNote("D#4", centerX - positioningRadius * 0.2f, centerY - positioningRadius * 0.8f, 1)
        addNote("B3", centerX + positioningRadius * 0.3f, centerY - positioningRadius * 0.8f, 1)
        addNote("F3", centerX + positioningRadius * 0.8f, centerY - positioningRadius * 0.3f, 1)
        addNote("A#3", centerX + positioningRadius * 0.8f, centerY + positioningRadius * 0.3f, 1)
        addNote("C#3", centerX + positioningRadius * 0.2f, centerY + positioningRadius * 0.8f, 1)
        addNote("G3", centerX - positioningRadius * 0.3f, centerY + positioningRadius * 0.8f, 1)

        // Inner ring
        addNote("G4", centerX - positioningRadius * 0.35f, centerY - positioningRadius * 0.1f, 1)
        addNote("F4", centerX - positioningRadius * 0.1f, centerY - positioningRadius * 0.35f, 1)
        addNote("C#4", centerX + positioningRadius * 0.1f, centerY - positioningRadius * 0.1f, 1)
    }

    // Modified addNote: radius is now calculated based on noteLabel's octave
    private fun addNote(noteLabel: String, x: Float, y: Float, drumIndex: Int) {
        if (!availableNotes.contains(noteLabel) || !noteFrequencies.containsKey(noteLabel)) {
            Log.e(TAG, "Attempted to add unknown note: $noteLabel. Skipping.")
            return
        }
        val octave = getOctaveFromNoteLabel(noteLabel)
        val radius = calculateRadiusForOctave(octave)
        notes.add(NoteRegion(noteLabel, x, y, radius, drumIndex))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(Color.rgb(240, 235, 220)) // Light cream background

        // Draw drum outlines
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = if (isConfigMode) Color.rgb(150, 100, 100) else Color.rgb(100, 100, 100) // Darker outlines

        val drumVisualRadius = min(width, height) * 0.35f // This is for the drum circle itself
        canvas.drawCircle(width * 0.3f, height / 2f, drumVisualRadius, paint)
        canvas.drawCircle(width * 0.7f, height / 2f, drumVisualRadius, paint)


        // Draw notes
        for (note in notes) {
            if (note == draggedNote) continue // Skip dragged note, draw it separately

            // Draw note area
            paint.style = Paint.Style.FILL
            paint.color = if (isConfigMode) Color.rgb(180, 160, 120) else Color.rgb(200, 180, 140) // Earthy tones
            canvas.drawCircle(note.x, note.y, note.radius, paint)

            // Draw note border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = if (isConfigMode) Color.rgb(100, 80, 60) else Color.rgb(120, 100, 80)
            canvas.drawCircle(note.x, note.y, note.radius, paint)

            // Draw note text - dynamically adjust text size based on radius
            val textSize = note.radius * 0.5f // Example: text size is half the radius
            textPaint.textSize = textSize.coerceIn(18f, 40f) // Clamp text size to reasonable min/max
            canvas.drawText(note.note, note.x, note.y + textPaint.textSize / 3, textPaint)
        }

        // Draw dragged note if any
        draggedNote?.let { note ->
            canvas.drawCircle(note.x, note.y, note.radius, dragPaint) // Use the note's own radius
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.RED
            canvas.drawCircle(note.x, note.y, note.radius, paint)

            val draggedTextSize = note.radius * 0.5f
            textPaint.textSize = draggedTextSize.coerceIn(18f, 40f)
            val originalColor = textPaint.color
            textPaint.color = Color.RED
            canvas.drawText(note.note, note.x, note.y + textPaint.textSize / 3, textPaint)
            textPaint.color = originalColor // Reset to original color
        }

        // Draw title
        textPaint.textSize = 48f // Reset for title
        textPaint.color = Color.rgb(80, 60, 40)
        val title = if (isConfigMode) "CONFIGURATION MODE" else "STEELPAN DOUBLE GUITAR"
        canvas.drawText(title, width / 2f, height * 0.07f, textPaint) // Adjusted y for title

        // Draw instructions in config mode
        if (isConfigMode) {
            textPaint.textSize = 24f
            textPaint.color = Color.rgb(100, 80, 60)
            canvas.drawText("Drag notes to reposition • Long press to change note", width / 2f, height * 0.12f, textPaint) // Adjusted y
        }
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
        val pointerId = event.getPointerId(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val note = findNoteAt(x, y)
                if (note != null) {
                    val frequency = noteFrequencies[note.note] ?: return true
                    (context as? SoundPlayer)?.playSound(frequency, pointerId)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Optional: Could handle sliding between notes if desired
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    // If you want to trigger sounds on move over, add logic here
                }
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                (context as? SoundPlayer)?.stopSound(pointerId)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                for (i in 0 until event.pointerCount) {
                    (context as? SoundPlayer)?.stopSound(event.getPointerId(i))
                }
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
                // Check for long press initiation first
                val touchedNote = findNoteAt(x, y)
                if (touchedNote != null) {
                    draggedNote = touchedNote // Set for potential drag
                    dragOffsetX = x - touchedNote.x
                    dragOffsetY = y - touchedNote.y
                    startLongPressTimer(touchedNote) // Start timer for potential note change
                    invalidate()
                    return true
                }
                // If no note touched, check if touch is inside drum bounds to add new note (optional)
                // For now, focusing on dragging and changing existing notes.
                // You could add logic here to call showAddNoteDialog if needed.
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                cancelLongPressTimer() // If finger moves, it's not a long press
                draggedNote?.let { note ->
                    note.x = x - dragOffsetX
                    note.y = y - dragOffsetY

                    // Determine which drum it's currently over
                    val currentDrumIndex = isInsideDrum(note.x, note.y)
                    if (currentDrumIndex != -1) {
                        note.drumIndex = currentDrumIndex // Update drum association
                    }
                    // No snapping during move, only on ACTION_UP if it's outside all drums
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                cancelLongPressTimer() // Cancel timer on up event
                draggedNote?.let { note ->
                    val finalDrumIndex = isInsideDrum(note.x, note.y)
                    if (finalDrumIndex == -1) {
                        // Note is outside any drum, show dialog to remove or move back
                        showRemoveOrMoveBackDialog(note)
                    } else {
                        note.drumIndex = finalDrumIndex // Confirm drum index
                    }
                    draggedNote = null
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelLongPressTimer()
                draggedNote = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findNoteAt(x: Float, y: Float): NoteRegion? {
        // Iterate in reverse so top notes are selected first if overlapping
        for (note in notes.asReversed()) {
            val distance = hypot(x - note.x, y - note.y)
            if (distance <= note.radius) {
                return note
            }
        }
        return null
    }

    // Long press to change note
    private var longPressRunnable: Runnable? = null
    private val longPressHandler = android.os.Handler() // Use android.os.Handler

    private fun startLongPressTimer(note: NoteRegion) {
        cancelLongPressTimer() // Cancel any existing timer
        longPressRunnable = Runnable {
            // Ensure the finger is still on the same note and not dragging
            if (draggedNote == note) { // Check if it's still the "dragged" (i.e. initially touched) note
                showChangeNoteDialog(note)
                draggedNote = null // Clear draggedNote after initiating change dialog to prevent drag
                invalidate()
            }
        }
        longPressHandler.postDelayed(longPressRunnable!!, android.view.ViewConfiguration.getLongPressTimeout().toLong())
    }

    private fun cancelLongPressTimer() {
        longPressRunnable?.let {
            longPressHandler.removeCallbacks(it)
            longPressRunnable = null
        }
    }


    private fun isInsideDrum(x: Float, y: Float): Int {
        val drumVisualRadius = min(width, height) * 0.35f
        val leftDrumCenterX = width * 0.3f
        val rightDrumCenterX = width * 0.7f
        val drumCenterY = height / 2f

        if (hypot(x - leftDrumCenterX, y - drumCenterY) <= drumVisualRadius) return 0
        if (hypot(x - rightDrumCenterX, y - drumCenterY) <= drumVisualRadius) return 1
        return -1 // Not inside any drum
    }

    private fun showAddNoteDialog(x: Float, y: Float, drumIndex: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Add Note")

        val availableNotesList = availableNotes.toTypedArray()
        builder.setItems(availableNotesList) { _, which ->
            val selectedNote = availableNotesList[which]
            // Calculate radius based on the selected note's octave
            addNote(selectedNote, x, y, drumIndex) // addNote now calculates its own radius
            invalidate()
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showChangeNoteDialog(note: NoteRegion) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Change Note")

        val currentNoteIndex = availableNotes.indexOf(note.note)
        val availableNotesList = availableNotes.toTypedArray()

        builder.setSingleChoiceItems(availableNotesList, currentNoteIndex) { dialog, which ->
            val selectedNoteLabel = availableNotesList[which]
            note.note = selectedNoteLabel
            // Update the radius based on the new note's octave
            val newOctave = getOctaveFromNoteLabel(selectedNoteLabel)
            note.radius = calculateRadiusForOctave(newOctave)
            invalidate()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showRemoveOrMoveBackDialog(note: NoteRegion) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Note Outside Drums")
        builder.setMessage("This note is outside any drum. Remove it or move it back to its original drum?")

        builder.setPositiveButton("Remove") { _, _ ->
            notes.remove(note)
            invalidate()
        }

        builder.setNegativeButton("Move Back") { _, _ ->
            // Move to center of its original drum
            val originalDrumCenterX = if (note.drumIndex == 0) width * 0.3f else width * 0.7f
            val originalDrumCenterY = height / 2f
            note.x = originalDrumCenterX
            note.y = originalDrumCenterY
            // Ensure it's marked as inside its original drum
            // The drumIndex should still be its original one if it wasn't updated
            // to -1 before this dialog.
            invalidate()
        }
        builder.setNeutralButton("Cancel", null) // Allow user to cancel and manually reposition
        builder.setCancelable(false)
        builder.show()
    }

    // Interface for sound playing callback
    interface SoundPlayer {
        fun playSound(frequency: Float, pointerId: Int)
        fun stopSound(pointerId: Int)
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
            // When loading, the radius in NoteConfiguration might be from an old system.
            // It's better to recalculate it based on the note's octave to ensure consistency.
            val octave = getOctaveFromNoteLabel(noteConfig.note)
            val correctRadius = calculateRadiusForOctave(octave)
            notes.add(NoteRegion(
                noteConfig.note,
                noteConfig.x,
                noteConfig.y,
                correctRadius, // Use the correctly calculated radius
                noteConfig.drumIndex
            ))
        }
        invalidate()
    }

}

// Data class for representing a note region on the view
data class NoteRegion(
    var note: String,
    var x: Float,
    var y: Float,
    var radius: Float, // This radius is now calculated based on octave
    var drumIndex: Int = 0 // 0 for left drum, 1 for right drum
)
