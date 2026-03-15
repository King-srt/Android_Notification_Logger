package com.example.notificationapp.utils

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "notification_settings"

    const val KEY_ENABLE_LOGGING = "enable_logging"
    const val KEY_SHOW_HIDDEN = "show_hidden"
    const val KEY_EXPORT_FORMAT = "export_format"
    const val KEY_AUTO_EXPORT_ENABLED = "auto_export_enabled"
    const val KEY_DELETE_AFTER_EXPORT = "delete_after_export"

    const val EXPORT_FORMAT_CSV = "CSV"
    const val EXPORT_FORMAT_JSON = "JSON"
    const val EXPORT_FORMAT_BOTH = "BOTH"


    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLoggingEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLE_LOGGING, true)
    }

    fun setLoggingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLE_LOGGING, enabled).apply()
    }

    fun isShowHiddenEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SHOW_HIDDEN, false)
    }

    fun setShowHiddenEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_HIDDEN, enabled).apply()
    }

    fun getExportFormat(context: Context): String {
        return prefs(context).getString(KEY_EXPORT_FORMAT, EXPORT_FORMAT_CSV) ?: EXPORT_FORMAT_CSV
    }

    fun setExportFormat(context: Context, format: String) {
        prefs(context).edit().putString(KEY_EXPORT_FORMAT, format).apply()
    }

    fun isAutoExportEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_EXPORT_ENABLED, false)
    }

    fun setAutoExportEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_EXPORT_ENABLED, enabled).apply()
    }

    fun isDeleteAfterExportEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DELETE_AFTER_EXPORT, false)
    }

    fun setDeleteAfterExportEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DELETE_AFTER_EXPORT, enabled).apply()
    }

    fun getBlacklistApps(context: Context): Set<String> {
        return emptySet()
    }

    fun setBlacklistApps(context: Context, apps: Set<String>) = Unit
}
