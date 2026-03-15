package com.example.notificationapp.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["postTime"]),
        Index(value = ["dateDay"]),
        Index(value = ["packageName"]),
        Index(value = ["notificationKey"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val sender: String?,
    val conversationTitle: String?,
    val category: String,
    val postTime: Long,
    val removedTime: Long,
    val dateDay: Int,
    val isRemoved: Boolean
)
