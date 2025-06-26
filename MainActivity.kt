// MainActivity.kt
package com.example.steelpan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    private lateinit var steelpanView: SteelpanView
    
    companion object {
        init {
            System.loadLibrary("steelpan")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize native audio engine first
        initializeAudio()
        
        steelpanView = SteelpanView(this)
        setContentView(steelpanView)
        
        // Request audio permissions if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        destroyAudio()
    }
    
    // Native methods
    external fun initializeAudio()
    external fun destroyAudio()
    external fun playNote(frequency: Float)
}

class SteelpanView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val notes = mutableListOf<NoteRegion>()
    
    // Note frequencies (Hz) - steelpan tuning
    private val noteFrequencies = mapOf(
        // Left drum (C#3 to G#4)
        "E4" to 329.63f, "C4" to 261.63f, "E♭4" to 311.13f, "B3" to 246.94f,
        "E3" to 164.81f, "F♯4" to 369.99f, "F♯3" to 185.00f, "F4" to 349.23f,
        "G♯4" to 415.30f, "D4" to 293.66f, "B♭3" to 233.08f, "F3" to 174.61f,
        "G♯3" to 207.65f, "D3" to 146.83f, "A3" to 220.00f,
        
        // Right drum 
        "G4" to 392.00f, "C♯4" to 277.18f, "C♯3" to 138.59f, "G3" to 196.00f
    )
    
    init {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 32f
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT_BOLD
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupNoteLayout()
    }
    
    private fun setupNoteLayout() {
        notes.clear()
        val centerY = height / 2f
        val drumRadius = min(width, height) * 0.35f
        val leftCenterX = width * 0.3f
        val rightCenterX = width * 0.7f
        
        // Left drum layout (C#3 to G#4)
        setupLeftDrum(leftCenterX, centerY, drumRadius)
        
        // Right drum layout
        setupRightDrum(rightCenterX, centerY, drumRadius)
    }
    
    private fun setupLeftDrum(centerX: Float, centerY: Float, radius: Float) {
        // Outer ring
        addNote("E3", centerX - radius * 0.7f, centerY - radius * 0.4f, radius * 0.25f)
        addNote("E4", centerX - radius * 0.2f, centerY - radius * 0.8f, radius * 0.25f)
        addNote("C4", centerX + radius * 0.3f, centerY - radius * 0.8f, radius * 0.25f)
        addNote("F♯3", centerX + radius * 0.8f, centerY - radius * 0.3f, radius * 0.25f)
        addNote("B♭3", centerX + radius * 0.8f, centerY + radius * 0.3f, radius * 0.25f)
        addNote("D3", centerX + radius * 0.2f, centerY + radius * 0.8f, radius * 0.25f)
        addNote("G♯3", centerX - radius * 0.3f, centerY + radius * 0.8f, radius * 0.25f)
        
        // Inner ring
        addNote("G♯4", centerX - radius * 0.35f, centerY - radius * 0.1f, radius * 0.15f)
        addNote("F♯4", centerX - radius * 0.1f, centerY - radius * 0.35f, radius * 0.15f)
        addNote("D4", centerX + radius * 0.1f, centerY - radius * 0.1f, radius * 0.15f)
        addNote("F4", centerX + radius * 0.35f, centerY + radius * 0.1f, radius * 0.15f)
        addNote("A3", centerX, centerY + radius * 0.35f, radius * 0.15f)
    }
    
    private fun setupRightDrum(centerX: Float, centerY: Float, radius: Float) {
        // Outer ring
        addNote("E♭3", centerX - radius * 0.7f, centerY - radius * 0.4f, radius * 0.25f)
        addNote("E♭4", centerX - radius * 0.2f, centerY - radius * 0.8f, radius * 0.25f)
        addNote("B3", centerX + radius * 0.3f, centerY - radius * 0.8f, radius * 0.25f)
        addNote("F3", centerX + radius * 0.8f, centerY - radius * 0.3f, radius * 0.25f)
        addNote("A3", centerX + radius * 0.8f, centerY + radius * 0.3f, radius * 0.25f)
        addNote("C♯3", centerX + radius * 0.2f, centerY + radius * 0.8f, radius * 0.25f)
        addNote("G3", centerX - radius * 0.3f, centerY + radius * 0.8f, radius * 0.25f)
        
        // Inner ring
        addNote("G4", centerX - radius * 0.35f, centerY - radius * 0.1f, radius * 0.15f)
        addNote("F4", centerX - radius * 0.1f, centerY - radius * 0.35f, radius * 0.15f)
        addNote("C♯4", centerX + radius * 0.1f, centerY - radius * 0.1f, radius * 0.15f)
    }
    
    private fun addNote(note: String, x: Float, y: Float, radius: Float) {
        notes.add(NoteRegion(note, x, y, radius))
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        canvas.drawColor(Color.rgb(240, 235, 220))
        
        // Draw drum outlines
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.rgb(100, 100, 100)
        
        val drumRadius = min(width, height) * 0.35f
        canvas.drawCircle(width * 0.3f, height / 2f, drumRadius, paint)
        canvas.drawCircle(width * 0.7f, height / 2f, drumRadius, paint)
        
        // Draw notes
        for (note in notes) {
            // Draw note area
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(200, 180, 140)
            canvas.drawCircle(note.x, note.y, note.radius, paint)
            
            // Draw note border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.rgb(120, 100, 80)
            canvas.drawCircle(note.x, note.y, note.radius, paint)
            
            // Draw note text
            canvas.drawText(note.note, note.x, note.y + textPaint.textSize/3, textPaint)
        }
        
        // Draw title
        textPaint.textSize = 48f
        textPaint.color = Color.rgb(80, 60, 40)
        canvas.drawText("STEELPAN DOUBLE GUITAR", width/2f, height * 0.1f, textPaint)
        textPaint.textSize = 32f
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            
            for (note in notes) {
                val distance = sqrt((x - note.x).pow(2) + (y - note.y).pow(2))
                if (distance <= note.radius) {
                    playNote(note.note)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun playNote(note: String) {
        noteFrequencies[note]?.let { frequency ->
            (context as MainActivity).playNote(frequency)
        }
    }
}

data class NoteRegion(
    val note: String,
    val x: Float,
    val y: Float,
    val radius: Float
)
