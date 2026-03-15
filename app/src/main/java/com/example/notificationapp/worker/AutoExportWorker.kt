package com.example.notificationapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notificationapp.data.database.AppDatabase
import com.example.notificationapp.utils.ExportUtils
import com.example.notificationapp.utils.SettingsManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AutoExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val isManual = inputData.getBoolean(KEY_MANUAL_EXPORT, false)
        if (!isManual && !SettingsManager.isAutoExportEnabled(applicationContext)) {
            return Result.success()
        }

        val dao = AppDatabase.getInstance(applicationContext).notificationDao()
        val notifications = dao.getAllNotifications()
        val dateStamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"))

        when (SettingsManager.getExportFormat(applicationContext)) {
            SettingsManager.EXPORT_FORMAT_JSON -> {
                ExportUtils.exportToJSONInDocuments(applicationContext, notifications, dateStamp)
            }
            SettingsManager.EXPORT_FORMAT_BOTH -> {
                ExportUtils.exportToCSVInDocuments(applicationContext, notifications, dateStamp)
                ExportUtils.exportToJSONInDocuments(applicationContext, notifications, dateStamp)
            }
            else -> {
                ExportUtils.exportToCSVInDocuments(applicationContext, notifications, dateStamp)
            }
        }

        val deleteAfter = SettingsManager.isDeleteAfterExportEnabled(applicationContext)
        if (!isManual && deleteAfter) {
            dao.clearAllNotifications()
        }

        return Result.success()
    }

    companion object {
        const val KEY_MANUAL_EXPORT = "manual_export"
    }
}
