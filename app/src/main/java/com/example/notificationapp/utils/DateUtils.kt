package com.example.notificationapp.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    fun toDateDay(timestamp: Long): Int {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
    }

    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(timestampFormatter)
    }

    fun formatDateDay(dateDay: Int): String {
        val year = dateDay / 10000
        val month = (dateDay / 100) % 100
        val day = dateDay % 100
        val date = LocalDate.of(year, month, day)
        return date.format(dateFormatter)
    }
}
