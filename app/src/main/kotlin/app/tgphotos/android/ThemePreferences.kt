package app.tgphotos.android

import android.content.Context
import app.tgphotos.core.model.ThemeMode

/** Only non-sensitive appearance preferences. Never write credentials or media metadata here. */
class ThemePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun mode(): ThemeMode = runCatching { ThemeMode.valueOf(preferences.getString("mode", "SYSTEM") ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    fun dynamicColors(): Boolean = preferences.getBoolean("dynamic_colors", true)
    fun setMode(value: ThemeMode) { preferences.edit().putString("mode", value.name).apply() }
    fun setDynamicColors(value: Boolean) { preferences.edit().putBoolean("dynamic_colors", value).apply() }
}
