package com.example.steelpan

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class NoteConfiguration(
    val note: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val drumIndex: Int // 0 for left drum, 1 for right drum
)

data class DrumConfiguration(
    val notes: List<NoteConfiguration>
)

class ConfigurationManager(private val context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("steelpan_configs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveConfiguration(name: String, configuration: DrumConfiguration) {
        val json = gson.toJson(configuration)
        sharedPrefs.edit().putString(name, json).apply()
    }

    fun loadConfiguration(name: String): DrumConfiguration? {
        val json = sharedPrefs.getString(name, null) ?: return null
        return try {
            gson.fromJson(json, DrumConfiguration::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getAllConfigurations(): Map<String, DrumConfiguration> {
        val configs = mutableMapOf<String, DrumConfiguration>()
        val allEntries = sharedPrefs.all

        for ((key, value) in allEntries) {
            if (value is String) {
                try {
                    val config = gson.fromJson(value, DrumConfiguration::class.java)
                    configs[key] = config
                } catch (e: Exception) {
                    // Skip invalid configurations
                }
            }
        }
        return configs
    }

    fun deleteConfiguration(name: String) {
        sharedPrefs.edit().remove(name).apply()
    }

    fun getConfigurationNames(): List<String> {
        return sharedPrefs.all.keys.toList()
    }
}