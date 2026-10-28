package com.david.notebook.data.settings

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode { System, Light, Dark }

enum class Accent {
    /** Wallpaper colours (Material You). Only exists on Android 12+. */
    Dynamic,
    Red, Orange, Yellow, Green, Blue, Purple;

    companion object {
        val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val available = entries.filter { it != Dynamic || dynamicSupported }
        val default = if (dynamicSupported) Dynamic else Blue
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val accent: Accent = Accent.default,
    /** Notifications before tests and when tasks are due. */
    val reminders: Boolean = true,
)

/** A few small preferences, so SharedPreferences rather than pulling in DataStore. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _settings.update { it.copy(themeMode = mode) }
    }

    fun setAccent(accent: Accent) {
        prefs.edit { putString(KEY_ACCENT, accent.name) }
        _settings.update { it.copy(accent = accent) }
    }

    fun setReminders(on: Boolean) {
        prefs.edit { putBoolean(KEY_REMINDERS, on) }
        _settings.update { it.copy(reminders = on) }
    }

    /**
     * Whether Notebook already asked for the notification permission on its own (after
     * the first test or dated task was saved), so it doesn't ask again on every save.
     */
    var askedForNotifications: Boolean
        get() = prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)
        set(value) = prefs.edit { putBoolean(KEY_ASKED_NOTIFICATIONS, value) }

    private fun read() = AppSettings(
        themeMode = prefs.getString(KEY_THEME, null)
            ?.let { name -> ThemeMode.entries.find { it.name == name } }
            ?: ThemeMode.System,
        accent = prefs.getString(KEY_ACCENT, null)
            ?.let { name -> Accent.available.find { it.name == name } }
            ?: Accent.default,
        reminders = prefs.getBoolean(KEY_REMINDERS, true),
    )

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_ACCENT = "accent"
        const val KEY_REMINDERS = "reminders"
        const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
    }
}
