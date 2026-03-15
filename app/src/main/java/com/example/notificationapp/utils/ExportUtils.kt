package com.example.notificationapp.utils

import android.content.Context
import com.example.notificationapp.data.database.NotificationEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ExportUtils {
    private val fileTimestampFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US).withZone(ZoneId.systemDefault())

    fun exportToCSV(context: Context, notifications: List<NotificationEntity>): File {
        val file = File(context.filesDir, "notifications_${fileTimestampFormatter.format(Instant.now())}.csv")
        val builder = StringBuilder()
        builder.append("appName,packageName,title,text,postTime,removedTime\n")
        notifications.forEach { notification ->
            builder.append(csvEscape(notification.appName)).append(',')
            builder.append(csvEscape(notification.packageName)).append(',')
            builder.append(csvEscape(notification.title)).append(',')
            builder.append(csvEscape(notification.text)).append(',')
            builder.append(notification.postTime).append(',')
            builder.append(notification.removedTime).append('\n')
        }
        file.writeText(builder.toString())
        return file
    }

    fun exportToJSON(context: Context, notifications: List<NotificationEntity>): File {
        val file = File(context.filesDir, "notifications_${fileTimestampFormatter.format(Instant.now())}.json")
        val array = JSONArray()
        notifications.forEach { notification ->
            val obj = JSONObject()
            obj.put("appName", notification.appName)
            obj.put("packageName", notification.packageName)
            obj.put("title", notification.title)
            obj.put("text", notification.text)
            obj.put("postTime", notification.postTime)
            obj.put("removedTime", notification.removedTime)
            array.put(obj)
        }
        file.writeText(array.toString())
        return file
    }

    fun exportToCSVInDocuments(context: Context, notifications: List<NotificationEntity>, dateStamp: String): File {
        val builder = StringBuilder()
        builder.append("appName,packageName,title,text,postTime,removedTime\n")
        notifications.forEach { notification ->
            builder.append(csvEscape(notification.appName)).append(',')
            builder.append(csvEscape(notification.packageName)).append(',')
            builder.append(csvEscape(notification.title)).append(',')
            builder.append(csvEscape(notification.text)).append(',')
            builder.append(notification.postTime).append(',')
            builder.append(notification.removedTime).append('\n')
        }
        return writeToDocuments(
            context,
            "notifications_$dateStamp.csv",
            "text/csv",
            builder.toString().toByteArray()
        )
    }

    fun exportToJSONInDocuments(context: Context, notifications: List<NotificationEntity>, dateStamp: String): File {
        val array = JSONArray()
        notifications.forEach { notification ->
            val obj = JSONObject()
            obj.put("appName", notification.appName)
            obj.put("packageName", notification.packageName)
            obj.put("title", notification.title)
            obj.put("text", notification.text)
            obj.put("postTime", notification.postTime)
            obj.put("removedTime", notification.removedTime)
            array.put(obj)
        }
        return writeToDocuments(
            context,
            "notifications_$dateStamp.json",
            "application/json",
            array.toString().toByteArray()
        )
    }

    private fun writeToDocuments(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): File {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/NotificationLogger/"
        val existing = findExistingDocument(resolver, fileName, relativePath)
        val uri = existing ?: run {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            resolver.insert(MediaStore.Files.getContentUri("external"), values)
        }

        if (uri != null) {
            resolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
            return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NotificationLogger/$fileName")
        }

        val fallbackDir = getFallbackDocumentsDir(context)
        val fallbackFile = File(fallbackDir, fileName)
        fallbackFile.writeBytes(bytes)
        return fallbackFile
    }

    private fun findExistingDocument(
        resolver: android.content.ContentResolver,
        fileName: String,
        relativePath: String
    ): Uri? {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(fileName, relativePath)
        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return Uri.withAppendedPath(collection, id.toString())
            }
        }
        return null
    }

    private fun getFallbackDocumentsDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val dir = File(base, "NotificationLogger")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('\n') || value.contains('"')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
