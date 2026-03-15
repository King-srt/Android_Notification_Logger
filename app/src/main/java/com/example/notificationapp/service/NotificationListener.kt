package com.example.notificationapp.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.ComponentName
import android.os.Build
import com.example.notificationapp.data.database.AppDatabase
import com.example.notificationapp.data.database.NotificationEntity
import com.example.notificationapp.parser.NotificationParser
import com.example.notificationapp.utils.DateUtils
import com.example.notificationapp.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val logNormal = PreferenceManager.isLoggingEnabled(this)
        val logHidden = com.example.notificationapp.utils.SettingsManager.isShowHiddenEnabled(this)
        if (!logNormal && !logHidden) {
            return
        }
        val packageName = sbn.packageName

        val notification = sbn.notification
        val parsed = runCatching { NotificationParser.parse(notification) }
            .getOrElse {
                com.example.notificationapp.parser.ParsedNotification(
                    title = "",
                    conversationTitle = null,
                    messages = emptyList()
                )
            }
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
        val forcedAppName = when (packageName) {
            "com.tokopedia.tkpd",
            "com.tokopedia.mobile",
            "com.tokopedia.mitra" -> "Tokopedia"
            "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
            "org.telegram.messenger", "org.telegram.messenger.web" -> "Telegram"
            else -> appName
        }
        serviceScope.launch {
            val dao = AppDatabase.getInstance(this@NotificationListener).notificationDao()
            val conversationTitle = parsed.conversationTitle?.ifBlank { null }
                ?: parsed.title.ifBlank { null }
                ?: sbn.key
            val latest = dao.getLatestMessage(packageName, conversationTitle)
            var lastText = latest?.text
            var lastPostTime = latest?.postTime ?: -1L

            val safeMessages = if (parsed.messages.isEmpty()) {
                listOf(com.example.notificationapp.parser.ParsedMessage("", null, null))
            } else {
                parsed.messages
            }
            val sortedMessages = safeMessages.sortedBy { it.timestamp ?: sbn.postTime }
            for (message in sortedMessages) {
                val isHidden = message.text.isBlank()
                if (isHidden && !logHidden) continue
                if (!isHidden && !logNormal) continue
                val messageText = if (isHidden) "(content hidden)" else message.text
                val postTime = message.timestamp ?: sbn.postTime
                if (messageText == lastText && postTime == lastPostTime && messageText != "(content hidden)") continue
                val entity = NotificationEntity(
                    notificationKey = sbn.key,
                    packageName = packageName,
                    appName = forcedAppName,
                    title = parsed.title,
                    text = messageText,
                    sender = message.sender,
                    conversationTitle = conversationTitle,
                    category = notification.category ?: "",
                    postTime = postTime,
                    removedTime = 0L,
                    dateDay = DateUtils.toDateDay(postTime),
                    isRemoved = false
                )
                dao.insertNotification(entity)
                lastText = messageText
                lastPostTime = postTime
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val removedTime = System.currentTimeMillis()
        serviceScope.launch {
            AppDatabase.getInstance(this@NotificationListener)
                .notificationDao()
                .updateNotificationRemoved(sbn.key, removedTime)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, NotificationListener::class.java))
        }
    }

    private fun isAllowedByFilter(packageName: String): Boolean = true

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
