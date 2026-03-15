package com.example.notificationapp.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query(
        "UPDATE notifications SET removedTime = :removedTime, isRemoved = 1 " +
            "WHERE notificationKey = :notificationKey"
    )
    suspend fun updateNotificationRemoved(notificationKey: String, removedTime: Long): Int

    @Query("SELECT * FROM notifications ORDER BY postTime DESC")
    fun getNotificationsPaging(): PagingSource<Int, NotificationEntity>

    @Query(
        "SELECT * FROM notifications " +
            "WHERE (:query IS NULL OR appName LIKE :query OR packageName LIKE :query OR title LIKE :query OR text LIKE :query) " +
            "AND (:dateDay IS NULL OR dateDay = :dateDay) " +
            "AND (:packageName IS NULL OR packageName = :packageName) " +
            "AND (:hideEmpty = 0 OR (length(title) > 0 AND length(text) > 0 AND text != '(content hidden)')) " +
            "ORDER BY postTime DESC"
    )
    fun getNotificationsPagingFiltered(
        query: String?,
        dateDay: Int?,
        packageName: String?,
        hideEmpty: Boolean
    ): PagingSource<Int, NotificationEntity>

    @Query(
        "SELECT * FROM notifications " +
            "WHERE appName LIKE :query OR packageName LIKE :query OR title LIKE :query OR text LIKE :query " +
            "ORDER BY postTime DESC"
    )
    suspend fun searchNotifications(query: String): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE dateDay = :dateDay ORDER BY postTime DESC")
    suspend fun getNotificationsByDate(dateDay: Int): List<NotificationEntity>

    @Query("SELECT * FROM notifications ORDER BY postTime DESC")
    suspend fun getAllNotifications(): List<NotificationEntity>

    @Query("SELECT * FROM notifications ORDER BY postTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getNotificationsPage(limit: Int, offset: Int): List<NotificationEntity>

    @Query(
        "SELECT * FROM notifications WHERE dateDay = :dateDay " +
            "ORDER BY postTime DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun getNotificationsPageByDate(dateDay: Int, limit: Int, offset: Int): List<NotificationEntity>

    @Query(
        "SELECT * FROM notifications " +
            "WHERE (appName LIKE :query OR packageName LIKE :query OR title LIKE :query OR text LIKE :query) " +
            "ORDER BY postTime DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun searchNotificationsPage(query: String, limit: Int, offset: Int): List<NotificationEntity>

    @Query(
        "SELECT * FROM notifications " +
            "WHERE (appName LIKE :query OR packageName LIKE :query OR title LIKE :query OR text LIKE :query) " +
            "AND dateDay = :dateDay " +
            "ORDER BY postTime DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun searchNotificationsPageByDate(
        query: String,
        dateDay: Int,
        limit: Int,
        offset: Int
    ): List<NotificationEntity>

    @Query(
        "SELECT * FROM notifications WHERE packageName = :packageName " +
            "ORDER BY postTime DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun getNotificationsPageByApp(
        packageName: String,
        limit: Int,
        offset: Int
    ): List<NotificationEntity>

    @Query(
        "SELECT * FROM notifications WHERE packageName = :packageName AND dateDay = :dateDay " +
            "ORDER BY postTime DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun getNotificationsPageByAppAndDate(
        packageName: String,
        dateDay: Int,
        limit: Int,
        offset: Int
    ): List<NotificationEntity>

    @Query(
        "SELECT * FROM notifications " +
            "WHERE packageName = :packageName AND " +
            "(appName LIKE :query OR packageName LIKE :query OR title LIKE :query OR text LIKE :query) " +
            "ORDER BY postTime DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun searchNotificationsPageByApp(
        packageName: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<NotificationEntity>

    @Query(
        "SELECT * FROM notifications " +
            "WHERE packageName = :packageName AND dateDay = :dateDay AND " +
            "(appName LIKE :query OR packageName LIKE :query OR title LIKE :query OR text LIKE :query) " +
            "ORDER BY postTime DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun searchNotificationsPageByAppAndDate(
        packageName: String,
        query: String,
        dateDay: Int,
        limit: Int,
        offset: Int
    ): List<NotificationEntity>

    @Query(
        "SELECT text FROM notifications " +
            "WHERE packageName = :packageName AND conversationTitle IS :conversationTitle " +
            "ORDER BY postTime DESC LIMIT 1"
    )
    suspend fun getLatestMessageText(
        packageName: String,
        conversationTitle: String?
    ): String?

    @Query(
        "SELECT text, postTime FROM notifications " +
            "WHERE packageName = :packageName AND conversationTitle IS :conversationTitle " +
            "ORDER BY postTime DESC LIMIT 1"
    )
    suspend fun getLatestMessage(
        packageName: String,
        conversationTitle: String?
    ): LatestMessage?

    data class LatestMessage(
        val text: String?,
        val postTime: Long
    )

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications(): Int
}
