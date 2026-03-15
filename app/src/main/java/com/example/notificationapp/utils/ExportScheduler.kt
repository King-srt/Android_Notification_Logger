package com.example.notificationapp.utils

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notificationapp.worker.AutoExportWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ExportScheduler {
    private const val UNIQUE_WORK_NAME = "weekly_auto_export"

    fun scheduleWeeklyExport(context: Context) {
        val initialDelay = calculateInitialDelayToSundayMidnight()
        val input = Data.Builder()
            .putBoolean(AutoExportWorker.KEY_MANUAL_EXPORT, false)
            .build()
        val request = PeriodicWorkRequestBuilder<AutoExportWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    fun cancelWeeklyExport(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun calculateInitialDelayToSundayMidnight(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) {
            next.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }
}
