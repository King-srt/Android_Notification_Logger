package com.example.notificationapp.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import androidx.activity.ComponentActivity
import com.example.notificationapp.data.database.NotificationEntity
import com.example.notificationapp.utils.DateUtils
import com.google.android.material.appbar.MaterialToolbar

class NotificationDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#FFF7F0"))
        }
        val toolbar = MaterialToolbar(this).apply {
            title = "Notification Detail"
            setBackgroundColor(Color.parseColor("#FF8A3D"))
            setTitleTextColor(Color.WHITE)
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(16)
            setPadding(padding, padding, padding, padding)
        }
        scroll.addView(content)
        root.addView(scroll)

        val appName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty()
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val padding = dpToPx(8)
            setPadding(0, 0, 0, padding)
        }
        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
            setImageDrawable(getAppIcon(packageName))
        }
        val headerText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            text = appName
            val startPadding = dpToPx(12)
            setPadding(startPadding, 0, 0, 0)
            setTextColor(Color.parseColor("#6A2E00"))
        }
        headerRow.addView(iconView)
        headerRow.addView(headerText)
        content.addView(headerRow)

        content.addView(detailRow("App Name", appName))
        content.addView(detailRow("Package Name", packageName))
        content.addView(detailRow("Title", intent.getStringExtra(EXTRA_TITLE).orEmpty()))
        content.addView(detailRow("Message", intent.getStringExtra(EXTRA_TEXT).orEmpty()))
        content.addView(detailRow("Category", intent.getStringExtra(EXTRA_CATEGORY).orEmpty()))
        content.addView(
            detailRow(
                "Post Time",
                DateUtils.formatTimestamp(intent.getLongExtra(EXTRA_POST_TIME, 0L))
            )
        )
        val removedTime = intent.getLongExtra(EXTRA_REMOVED_TIME, 0L)
        content.addView(detailRow("Removed Time", DateUtils.formatTimestamp(removedTime)))
        content.addView(
            detailRow(
                "Removed",
                intent.getBooleanExtra(EXTRA_IS_REMOVED, false).toString()
            )
        )

        setContentView(root)
    }

    private fun detailRow(label: String, value: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(8)
            setPadding(0, padding, 0, padding)
        }
        val labelView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            text = label
            setTextColor(Color.parseColor("#8C3B00"))
        }
        val valueView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            text = value
            setTextColor(Color.parseColor("#5A2500"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        row.addView(labelView)
        row.addView(valueView)
        return row
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun getAppIcon(packageName: String) = try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val EXTRA_APP_NAME = "extra_app_name"
        private const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_TEXT = "extra_text"
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_POST_TIME = "extra_post_time"
        private const val EXTRA_REMOVED_TIME = "extra_removed_time"
        private const val EXTRA_IS_REMOVED = "extra_is_removed"

        fun newIntent(context: Context, entity: NotificationEntity): Intent {
            return Intent(context, NotificationDetailActivity::class.java).apply {
                putExtra(EXTRA_APP_NAME, entity.appName)
                putExtra(EXTRA_PACKAGE_NAME, entity.packageName)
                putExtra(EXTRA_TITLE, entity.title)
                putExtra(EXTRA_TEXT, entity.text)
                putExtra(EXTRA_CATEGORY, entity.category)
                putExtra(EXTRA_POST_TIME, entity.postTime)
                putExtra(EXTRA_REMOVED_TIME, entity.removedTime)
                putExtra(EXTRA_IS_REMOVED, entity.isRemoved)
            }
        }
    }
}
