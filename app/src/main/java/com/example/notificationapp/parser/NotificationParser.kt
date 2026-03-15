package com.example.notificationapp.parser

import android.app.Notification

data class ParsedMessage(
    val text: String,
    val sender: String?,
    val timestamp: Long?
)

data class ParsedNotification(
    val title: String,
    val conversationTitle: String?,
    val messages: List<ParsedMessage>
)

object NotificationParser {
    fun parse(notification: Notification): ParsedNotification {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
            ?: ""
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        val messagingMessages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.mapNotNull { parcelable ->
                when (parcelable) {
                    is Notification.MessagingStyle.Message -> {
                        val text = parcelable.text?.toString().orEmpty()
                        if (text.isBlank()) return@mapNotNull null
                        val sender = parcelable.senderPerson?.name?.toString()
                            ?: parcelable.sender?.toString()
                        ParsedMessage(
                            text = normalize(text),
                            sender = sender?.let { normalize(it) },
                            timestamp = parcelable.timestamp
                        )
                    }
                    is android.os.Bundle -> {
                        val text = parcelable.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                        if (text.isBlank()) return@mapNotNull null
                        val sender = parcelable.getCharSequence("sender")?.toString()
                        val timeKey = "time"
                        val time = if (parcelable.containsKey(timeKey)) {
                            parcelable.getLong(timeKey)
                        } else {
                            null
                        }
                        ParsedMessage(
                            text = normalize(text),
                            sender = sender?.let { normalize(it) },
                            timestamp = time
                        )
                    }
                    else -> null
                }
            }.orEmpty()

        if (messagingMessages.isNotEmpty()) {
            return ParsedNotification(
                title = normalize(title.toString()),
                conversationTitle = conversationTitle?.let { normalize(it) },
                messages = messagingMessages
            )
        }

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        val normalText = extras.getCharSequence(Notification.EXTRA_TEXT)
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        if (textLines.size > 1) {
            return ParsedNotification(
                title = normalize(title.toString()),
                conversationTitle = conversationTitle?.let { normalize(it) },
                messages = textLines.map { line ->
                    ParsedMessage(
                        text = normalize(line),
                        sender = null,
                        timestamp = null
                    )
                }
            )
        }

        val rawText = when {
            !bigText.isNullOrBlank() -> bigText.toString()
            !normalText.isNullOrBlank() -> normalText.toString()
            textLines.size == 1 -> textLines.first()
            !subText.isNullOrBlank() -> subText.toString()
            !infoText.isNullOrBlank() -> infoText.toString()
            !summaryText.isNullOrBlank() -> summaryText.toString()
            !notification.tickerText.isNullOrBlank() -> notification.tickerText.toString()
            else -> ""
        }

        val normalizedTitle = normalize(title.toString())
        val normalizedText = normalize(rawText)
        return ParsedNotification(
            title = normalizedTitle,
            conversationTitle = conversationTitle?.let { normalize(it) },
            messages = listOf(
                ParsedMessage(
                    text = normalizedText,
                    sender = null,
                    timestamp = null
                )
            )
        )
    }

    private fun normalize(text: String): String {
        val collapsed = text.replace(Regex("\\s+"), " ").trim()
        return if (collapsed.length > 1000) collapsed.substring(0, 1000) else collapsed
    }
}
