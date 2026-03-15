package com.example.notificationapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.notificationapp.data.database.NotificationDao
import com.example.notificationapp.data.database.NotificationEntity
import com.example.notificationapp.data.database.NotificationPagingSource

class NotificationRepository(private val dao: NotificationDao) {
    fun getPagedNotifications(
        query: String?,
        dateDay: Int?,
        packageName: String?,
        hideEmpty: Boolean
    ): Pager<Int, NotificationEntity> {
        val queryLike = query?.let { "%$it%" }
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false),
            pagingSourceFactory = { dao.getNotificationsPagingFiltered(queryLike, dateDay, packageName, hideEmpty) }
        )
    }

    suspend fun insertNotification(notification: NotificationEntity): Long {
        return dao.insertNotification(notification)
    }

    suspend fun updateNotificationRemoved(notificationKey: String, removedTime: Long): Int {
        return dao.updateNotificationRemoved(notificationKey, removedTime)
    }

    suspend fun getAllNotifications(): List<NotificationEntity> {
        return dao.getAllNotifications()
    }

    suspend fun searchNotifications(query: String): List<NotificationEntity> {
        return dao.searchNotifications("%$query%")
    }

    suspend fun getNotificationsByDate(dateDay: Int): List<NotificationEntity> {
        return dao.getNotificationsByDate(dateDay)
    }

    suspend fun getLatestMessageText(packageName: String, conversationTitle: String?): String? {
        return dao.getLatestMessageText(packageName, conversationTitle)
    }

    suspend fun getLatestMessage(packageName: String, conversationTitle: String?): NotificationDao.LatestMessage? {
        return dao.getLatestMessage(packageName, conversationTitle)
    }

    suspend fun clearAllNotifications(): Int {
        return dao.clearAllNotifications()
    }
}
