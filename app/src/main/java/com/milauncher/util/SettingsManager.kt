package com.milauncher.util

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    var showHdmi: Boolean
        get() = prefs.getBoolean("show_hdmi", true)
        set(value) = prefs.edit().putBoolean("show_hdmi", value).apply()

    var showApps: Boolean
        get() = prefs.getBoolean("show_apps", true)
        set(value) = prefs.edit().putBoolean("show_apps", value).apply()

    // HDMI Settings
    var hdmiLayout: Int
        get() = prefs.getInt("hdmi_layout", 0) // 0 = Row, 1 = Grid
        set(value) = prefs.edit().putInt("hdmi_layout", value).apply()

    var hdmiSize: Int
        get() = prefs.getInt("hdmi_size", 1) // 0 = Small, 1 = Medium, 2 = Large
        set(value) = prefs.edit().putInt("hdmi_size", value).apply()

    var hdmiColumns: Int
        get() = prefs.getInt("hdmi_columns", 0) // 0 = Auto, n = Fixed
        set(value) = prefs.edit().putInt("hdmi_columns", value).apply()

    // Apps Settings
    var appsLayout: Int
        get() = prefs.getInt("apps_layout", 1) // 0 = Row, 1 = Grid (Default Grid for Apps)
        set(value) = prefs.edit().putInt("apps_layout", value).apply()

    var appsSize: Int
        get() = prefs.getInt("apps_size", 0) // 0 = Small, 1 = Medium, 2 = Large
        set(value) = prefs.edit().putInt("apps_size", value).apply()

    var appsColumns: Int
        get() = prefs.getInt("apps_columns", 0) // 0 = Auto, n = Fixed
        set(value) = prefs.edit().putInt("apps_columns", value).apply()
}
