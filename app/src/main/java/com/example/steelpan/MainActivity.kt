package com.example.steelpan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var steelpanView: SteelpanView
    private lateinit var configManager: ConfigurationManager
    private lateinit var modeButton: Button
    private lateinit var saveButton: Button
    private lateinit var loadButton: Button
    private lateinit var configSpinner: Spinner
    private lateinit var controlsLayout: LinearLayout
    private var isConfigMode = false

    companion object {
        init {
            System.loadLibrary("steelpan")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and set to landscape
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set audio mode for low latency
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        // Initialize configuration manager
        configManager = ConfigurationManager(this)

        // Initialize native audio engine first
        initializeAudio()

        setupUI()

        // Request audio permissions if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun setupUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(240, 235, 220))
        }

        // Create controls layout
        controlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
        }

        // Mode toggle button
        modeButton = Button(this).apply {
            text = "Config Mode"
            setOnClickListener { toggleMode() }
        }

        // Save button
        saveButton = Button(this).apply {
            text = "Save"
            visibility = View.GONE
            setOnClickListener { showSaveDialog() }
        }

        // Load button
        loadButton = Button(this).apply {
            text = "Load"
            setOnClickListener { showLoadDialog() }
        }

        // Configuration spinner
        configSpinner = Spinner(this).apply {
            visibility = View.GONE
        }

        controlsLayout.addView(modeButton)
        controlsLayout.addView(saveButton)
        controlsLayout.addView(loadButton)
        controlsLayout.addView(configSpinner)

        // Create steelpan view
        steelpanView = SteelpanView(this, configManager)

        mainLayout.addView(controlsLayout)
        mainLayout.addView(steelpanView)

        setContentView(mainLayout)

        // Load saved configurations
        updateConfigSpinner()
    }

    private fun toggleMode() {
        isConfigMode = !isConfigMode
        steelpanView.setConfigMode(isConfigMode)

        if (isConfigMode) {
            modeButton.text = "Play Mode"
            saveButton.visibility = View.VISIBLE
            configSpinner.visibility = View.VISIBLE
        } else {
            modeButton.text = "Config Mode"
            saveButton.visibility = View.GONE
            configSpinner.visibility = View.GONE
        }
    }

    private fun showSaveDialog() {
        val editText = EditText(this).apply {
            hint = "Configuration name"
        }

        AlertDialog.Builder(this)
            .setTitle("Save Configuration")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val config = steelpanView.getCurrentConfiguration()
                    configManager.saveConfiguration(name, config)
                    updateConfigSpinner()
                    Toast.makeText(this, "Configuration saved as '$name'", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoadDialog() {
        val configurations = configManager.getAllConfigurations()
        if (configurations.isEmpty()) {
            Toast.makeText(this, "No saved configurations", Toast.LENGTH_SHORT).show()
            return
        }

        val names = configurations.keys.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Load Configuration")
            .setItems(names) { _, which ->
                val selectedName = names[which]
                val config = configurations[selectedName]
                if (config != null) {
                    steelpanView.loadConfiguration(config)
                    Toast.makeText(this, "Loaded '$selectedName'", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateConfigSpinner() {
        val configurations = configManager.getAllConfigurations()
        val names = listOf("Default") + configurations.keys.toList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        configSpinner.adapter = adapter

        configSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    steelpanView.loadDefaultConfiguration()
                } else {
                    val selectedName = names[position]
                    val config = configurations[selectedName]
                    if (config != null) {
                        steelpanView.loadConfiguration(config)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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