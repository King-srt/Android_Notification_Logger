package com.example.notificationapp.ui.main

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.LruCache
import android.util.TypedValue
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationapp.R
import com.example.notificationapp.data.database.NotificationEntity
import com.example.notificationapp.utils.DateUtils
import com.google.android.material.card.MaterialCardView
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class NotificationListItem {
    data class Header(val dateDay: Int) : NotificationListItem()
    data class Item(val notification: NotificationEntity) : NotificationListItem()
}

class NotificationAdapter(
    private val onItemClick: (NotificationEntity) -> Unit
) : PagingDataAdapter<NotificationListItem, RecyclerView.ViewHolder>(DIFF) {

    private val iconCache = LruCache<String, Drawable>(50)
    private val headerFormatter = DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (peek(position)) {
            is NotificationListItem.Header -> VIEW_TYPE_HEADER
            is NotificationListItem.Item -> VIEW_TYPE_ITEM
            else -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(createHeaderView(parent.context))
        } else {
            ItemViewHolder(createItemView(parent.context))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is NotificationListItem.Item -> (holder as ItemViewHolder).bind(item.notification)
            null -> Unit
        }
    }

    fun getNotificationAt(position: Int): NotificationEntity? {
        return (getItem(position) as? NotificationListItem.Item)?.notification
    }

    private fun createHeaderView(context: Context): TextView {
        return TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#6A2E00"))
            val padding = dpToPx(context, 12)
            setPadding(padding, padding, padding, padding / 2)
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = dpToPx(context, 6)
                setMargins(0, margin, 0, margin / 2)
            }
        }
    }

    private fun createItemView(context: Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val margin = dpToPx(context, 1)
            (layoutParams as RecyclerView.LayoutParams).setMargins(0, margin, 0, margin)
            radius = dpToPx(context, 12).toFloat()
            cardElevation = dpToPx(context, 2).toFloat()
            useCompatPadding = true
            setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            strokeWidth = dpToPx(context, 1)
            strokeColor = Color.parseColor("#FFD3B3")
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val padding = dpToPx(context, 12)
            setPadding(padding, padding, padding, padding)
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 40), dpToPx(context, 40))
        }

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            val startPadding = dpToPx(context, 12)
            setPadding(startPadding, 0, 0, 0)
        }

        val appNameView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#6A2E00"))
        }
        val titleView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.parseColor("#8C3B00"))
        }
        val messageView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.parseColor("#A04A00"))
        }

        val timeView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#C07234"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
                leftMargin = dpToPx(context, 8)
            }
        }

        textContainer.addView(appNameView)
        textContainer.addView(titleView)
        textContainer.addView(messageView)

        container.addView(iconView)
        container.addView(textContainer)
        container.addView(timeView)
        card.addView(container)

        card.tag = ItemViews(iconView, appNameView, titleView, messageView, timeView)
        return card
    }

    fun formatHeader(dateDay: Int): String {
        val year = dateDay / 10000
        val month = (dateDay / 100) % 100
        val day = dateDay % 100
        val date = java.time.LocalDate.of(year, month, day)
        return date.format(headerFormatter)
    }

    private fun getIcon(context: Context, packageName: String): Drawable? {
        val cached = iconCache.get(packageName)
        if (cached != null) return cached
        return try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            iconCache.put(packageName, icon)
            icon
        } catch (e: Exception) {
            val fallback = context.getDrawable(R.drawable.my_icon)
            if (fallback != null) {
                iconCache.put(packageName, fallback)
            }
            fallback
        }
    }

    inner class HeaderViewHolder(private val headerView: TextView) :
        RecyclerView.ViewHolder(headerView) {
        fun bind(header: NotificationListItem.Header) {
            headerView.text = formatHeader(header.dateDay)
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val views = itemView.tag as ItemViews

        fun bind(item: NotificationEntity) {
            val context = itemView.context
            val icon = getIcon(context, item.packageName)
            if (icon != null) {
                views.iconView.setImageDrawable(icon)
            } else {
                views.iconView.setImageResource(R.drawable.my_icon)
            }
            views.appNameView.text = item.appName
            val titleText = if (item.title.isBlank()) "(no title)" else item.title
            views.titleView.text = titleText
            views.messageView.text = if (item.text.isBlank()) "(no content)" else item.text
            views.timeView.text = DateUtils.formatTimestamp(item.postTime)
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    data class ItemViews(
        val iconView: ImageView,
        val appNameView: TextView,
        val titleView: TextView,
        val messageView: TextView,
        val timeView: TextView
    )

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1

        private val DIFF = object : DiffUtil.ItemCallback<NotificationListItem>() {
            override fun areItemsTheSame(
                oldItem: NotificationListItem,
                newItem: NotificationListItem
            ): Boolean {
                return when {
                    oldItem is NotificationListItem.Header && newItem is NotificationListItem.Header ->
                        oldItem.dateDay == newItem.dateDay
                    oldItem is NotificationListItem.Item && newItem is NotificationListItem.Item ->
                        oldItem.notification.id == newItem.notification.id
                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: NotificationListItem,
                newItem: NotificationListItem
            ): Boolean = oldItem == newItem
        }

        private fun dpToPx(context: Context, dp: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        }
    }
}
