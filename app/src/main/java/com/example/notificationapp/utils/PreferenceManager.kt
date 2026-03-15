package com.example.notificationapp.utils

import android.content.Context

object PreferenceManager {
    fun isLoggingEnabled(context: Context): Boolean {
        return SettingsManager.isLoggingEnabled(context)
    }

    fun setLoggingEnabled(context: Context, enabled: Boolean) {
        SettingsManager.setLoggingEnabled(context, enabled)
    }
}
