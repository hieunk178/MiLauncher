package com.milauncher.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    // ── Visibility ──────────────────────────────────────────────────────────
    var showHdmi: Boolean
        get() = prefs.getBoolean("show_hdmi", true)
        set(value) = prefs.edit().putBoolean("show_hdmi", value).apply()

    var showApps: Boolean
        get() = prefs.getBoolean("show_apps", true)
        set(value) = prefs.edit().putBoolean("show_apps", value).apply()

    var showPhoneApps: Boolean
        get() = prefs.getBoolean("show_phone_apps", true)
        set(value) = prefs.edit().putBoolean("show_phone_apps", value).apply()

    // ── HDMI Settings ────────────────────────────────────────────────────────
    var hdmiLayout: Int
        get() = prefs.getInt("hdmi_layout", 0) // 0 = Row, 1 = Grid
        set(value) = prefs.edit().putInt("hdmi_layout", value).apply()

    var hdmiSize: Int
        get() = prefs.getInt("hdmi_size", 1) // 0 = Small, 1 = Medium, 2 = Large
        set(value) = prefs.edit().putInt("hdmi_size", value).apply()

    var hdmiColumns: Int
        get() = prefs.getInt("hdmi_columns", 0)
        set(value) = prefs.edit().putInt("hdmi_columns", value).apply()

    // ── TV Apps Settings ─────────────────────────────────────────────────────
    var appsLayout: Int
        get() = prefs.getInt("apps_layout", 0) // 0 = Row, 1 = Grid
        set(value) = prefs.edit().putInt("apps_layout", value).apply()

    var appsSize: Int
        get() = prefs.getInt("apps_size", 1)
        set(value) = prefs.edit().putInt("apps_size", value).apply()

    var appsColumns: Int
        get() = prefs.getInt("apps_columns", 0)
        set(value) = prefs.edit().putInt("apps_columns", value).apply()

    // ── Phone Apps Settings ──────────────────────────────────────────────────
    var phoneAppsLayout: Int
        get() = prefs.getInt("phone_apps_layout", 1) // Default Grid
        set(value) = prefs.edit().putInt("phone_apps_layout", value).apply()

    var phoneAppsSize: Int
        get() = prefs.getInt("phone_apps_size", 0) // Small
        set(value) = prefs.edit().putInt("phone_apps_size", value).apply()

    var phoneAppsColumns: Int
        get() = prefs.getInt("phone_apps_columns", 0)
        set(value) = prefs.edit().putInt("phone_apps_columns", value).apply()

    // ── Focus Animation ──────────────────────────────────────────────────────
    var focusAnimationStyle: Int
        get() = prefs.getInt("focus_animation_style", 0) // 0=Default, 1=Heartbeat, 2=Breathing
        set(value) = prefs.edit().putInt("focus_animation_style", value).apply()

    // ── Wallpaper ────────────────────────────────────────────────────────────
    // 0=Gradient dynamic, 1=Static image, 2=Unsplash auto
    var wallpaperType: Int
        get() = prefs.getInt("wallpaper_type", 2) // Default: Unsplash auto
        set(value) = prefs.edit().putInt("wallpaper_type", value).apply()

    var wallpaperPath: String
        get() = prefs.getString("wallpaper_path", "") ?: ""
        set(value) = prefs.edit().putString("wallpaper_path", value).apply()

    var wallpaperCategory: String
        get() = prefs.getString("wallpaper_category", "nature") ?: "nature"
        set(value) = prefs.edit().putString("wallpaper_category", value).apply()

    // ── Featured Banner ───────────────────────────────────────────────────────
    // JSON array of package names to feature on banner
    var featuredPackages: List<String>
        get() {
            val json = prefs.getString("featured_packages", "") ?: ""
            if (json.isEmpty()) return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) { emptyList() }
        }
        set(value) {
            val arr = JSONArray(value)
            prefs.edit().putString("featured_packages", arr.toString()).apply()
        }

    // ── Theme ─────────────────────────────────────────────────────────────────
    // Accent color as hex string e.g. "#FFCC00"
    var accentColor: String
        get() = prefs.getString("accent_color", "#FFCC00") ?: "#FFCC00"
        set(value) = prefs.edit().putString("accent_color", value).apply()

    // ── Clock / Weather ──────────────────────────────────────────────────────
    var showClock: Boolean
        get() = prefs.getBoolean("show_clock", true)
        set(value) = prefs.edit().putBoolean("show_clock", value).apply()

    var showWeather: Boolean
        get() = prefs.getBoolean("show_weather", true)
        set(value) = prefs.edit().putBoolean("show_weather", value).apply()

    var showGreeting: Boolean
        get() = prefs.getBoolean("show_greeting", true)
        set(value) = prefs.edit().putBoolean("show_greeting", value).apply()

    var ownerName: String
        get() = prefs.getString("owner_name", "") ?: ""
        set(value) = prefs.edit().putString("owner_name", value).apply()
}
