package com.example.notificationapp

import com.google.android.material.datepicker.MaterialDatePicker
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.paging.LoadState
import com.example.notificationapp.ui.detail.NotificationDetailActivity
import com.example.notificationapp.ui.main.NotificationAdapter
import com.example.notificationapp.ui.main.NotificationListItem
import com.example.notificationapp.utils.DateUtils
import com.example.notificationapp.utils.ExportUtils
import com.example.notificationapp.utils.SettingsManager
import com.example.notificationapp.viewmodel.NotificationViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.notificationapp.settings.SettingsActivity
import com.example.notificationapp.utils.ExportScheduler
import com.example.notificationapp.service.NotificationListener

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: NotificationViewModel
    private lateinit var adapter: NotificationAdapter
    private var selectedDateDay: Int? = null
    private var selectedPackage: String? = null
    private lateinit var statsTodayView: TextView
    private lateinit var statsTopAppView: TextView
    private lateinit var chipsContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var hideEmptyCheck: CheckBox
    private var pendingScrollToTop: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(12)
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.parseColor("#FFF7F0"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Notification Logger"
            setBackgroundColor(Color.parseColor("#FF8A3D"))
            setTitleTextColor(Color.WHITE)
            setupMenu()
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_SETTINGS -> {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        true
                    }
                    MENU_ACCESS -> {
                        openNotificationAccessSettings()
                        true
                    }
                    MENU_EXPORT_NOW -> {
                        triggerExportNow()
                        true
                    }
                    else -> false
                }
            }
        }
        root.addView(toolbar)

        val statsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val padding = dpToPx(12)
            setPadding(padding, padding, padding, padding)
            background = roundedBackground("#FFE8D6")
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = dpToPx(6)
                setMargins(0, margin, 0, margin)
            }
        }
        statsTodayView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        statsTopAppView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        statsPanel.addView(statsTodayView)
        statsPanel.addView(statsTopAppView)
        root.addView(statsPanel)

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = dpToPx(6)
                setMargins(0, margin, 0, margin)
            }
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            background = roundedBackground("#FFF1E6")
        }
        root.addView(headerRow)

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundedBackground("#FFFFFF")
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = dpToPx(6)
                setMargins(0, margin, 0, margin)
            }
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
        }
        val searchIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            val size = dpToPx(18)
            layoutParams = LinearLayout.LayoutParams(size, size)
        }
        val searchInput = EditText(this).apply {
            hint = "Search notifications"
            inputType = InputType.TYPE_CLASS_TEXT
            background = null
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { leftMargin = dpToPx(8) }
            addTextChangedListener { text ->
                viewModel.setQuery(text?.toString().orEmpty())
            }
        }
        searchRow.addView(searchIcon)
        searchRow.addView(searchInput)
        root.addView(searchRow)

        val chipsScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        chipsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val padding = dpToPx(6)
            setPadding(padding, padding, padding, padding)
        }
        chipsScroll.addView(
            chipsContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(chipsScroll)

        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = dpToPx(6)
                setMargins(0, margin, 0, margin)
            }
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
        val dateButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Pick Date"
            setBackgroundColor(Color.parseColor("#FFE0C7"))
            setTextColor(Color.parseColor("#6A2E00"))
            isAllCaps = false
            textSize = 13f
            setOnClickListener { showDatePicker { dateDay ->
                selectedDateDay = dateDay
                viewModel.setDateDay(dateDay)
            } }
        }
        val clearDateButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Clear"
            setBackgroundColor(Color.parseColor("#FFE0C7"))
            setTextColor(Color.parseColor("#6A2E00"))
            isAllCaps = false
            textSize = 13f
            setOnClickListener {
                selectedDateDay = null
                viewModel.setDateDay(null)
                pendingScrollToTop = true
                adapter.refresh()
            }
        }
        hideEmptyCheck = CheckBox(this).apply {
            text = "Hide empty"
            setTextColor(Color.parseColor("#6A2E00"))
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setHideEmpty(isChecked)
                adapter.refresh()
            }
        }
        viewModel.setHideEmpty(hideEmptyCheck.isChecked)
        dateRow.addView(dateButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = dpToPx(8) })
        dateRow.addView(clearDateButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = dpToPx(8) })
        dateRow.addView(hideEmptyCheck, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { leftMargin = dpToPx(4) })
        root.addView(dateRow)

        adapter = NotificationAdapter { entity ->
            startActivity(NotificationDetailActivity.newIntent(this, entity))
        }
        adapter.addLoadStateListener { loadState ->
            if (pendingScrollToTop && loadState.refresh is LoadState.NotLoading) {
                pendingScrollToTop = false
                recyclerView.scrollToPosition(0)
            }
        }

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
            setBackgroundColor(Color.parseColor("#FFF7F0"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        root.addView(recyclerView)

        setContentView(root)

        attachSwipeActions(recyclerView)
        refreshStatsAndChips()
        if (SettingsManager.isAutoExportEnabled(this)) {
            ExportScheduler.scheduleWeeklyExport(this)
        }

        lifecycleScope.launch {
            viewModel.notificationsPagingFlow
                .map { pagingData ->
                    pagingData.map { NotificationListItem.Item(it) }
                        .insertSeparators { before, after ->
                            val beforeDate = (before as? NotificationListItem.Item)?.notification?.dateDay
                            val afterDate = (after as? NotificationListItem.Item)?.notification?.dateDay
                            if (afterDate != null && beforeDate != afterDate) {
                                NotificationListItem.Header(afterDate)
                            } else {
                                null
                            }
                        }
                }
                .collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setHideEmpty(hideEmptyCheck.isChecked)
        if (!isNotificationListenerEnabled()) {
            Toast.makeText(
                this,
                "Notification access is disabled. Please enable it.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun attachSwipeActions(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.getNotificationAt(position)
                return if (item == null) 0 else makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.getNotificationAt(position)
                if (item == null) {
                    adapter.notifyItemChanged(position)
                    return
                }
                if (direction == ItemTouchHelper.RIGHT) {
                    copyToClipboard(formatShareText(item))
                    Toast.makeText(this@MainActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                } else if (direction == ItemTouchHelper.LEFT) {
                    shareText(formatShareText(item))
                }
                adapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun showDatePicker(onSelected: (Int) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setTheme(R.style.ThemeOverlay_NotificationApp_DatePicker)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val dateDay = DateUtils.toDateDay(selection)
            onSelected(dateDay)
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun triggerExportNow() {
        lifecycleScope.launch {
            val result = runCatching {
                val notifications = withContext(Dispatchers.IO) { viewModel.getAllNotifications() }
                val dateStamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"))
                when (SettingsManager.getExportFormat(this@MainActivity)) {
                    SettingsManager.EXPORT_FORMAT_JSON -> {
                        ExportUtils.exportToJSONInDocuments(this@MainActivity, notifications, dateStamp)
                    }
                    SettingsManager.EXPORT_FORMAT_BOTH -> {
                        ExportUtils.exportToCSVInDocuments(this@MainActivity, notifications, dateStamp)
                        ExportUtils.exportToJSONInDocuments(this@MainActivity, notifications, dateStamp)
                        null
                    }
                    else -> {
                        ExportUtils.exportToCSVInDocuments(this@MainActivity, notifications, dateStamp)
                    }
                }
            }
            result.onSuccess {
                Toast.makeText(this@MainActivity, "Exported to Documents/NotificationLogger", Toast.LENGTH_LONG)
                    .show()
            }.onFailure {
                Toast.makeText(this@MainActivity, "Export failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }


    private fun isNotificationListenerEnabled(): Boolean {
        val component = ComponentName(this, NotificationListener::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabled.split(":").any { it.equals(component.flattenToString(), ignoreCase = true) }
    }

    private fun MaterialToolbar.setupMenu() {
        menu.clear()
        menu.add(0, MENU_EXPORT_NOW, 0, "Export Now")
        menu.add(0, MENU_ACCESS, 1, "Access")
        menu.add(0, MENU_SETTINGS, 2, "Settings")
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("notification", text))
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share notification"))
    }

    private fun formatShareText(item: com.example.notificationapp.data.database.NotificationEntity): String {
        val title = if (item.title.isBlank()) "(no title)" else item.title
        val message = if (item.text.isBlank()) "(content hidden)" else item.text
        return buildString {
            append("App Name: ").append(item.appName).append('\n')
            append("Title: ").append(title).append('\n')
            append("Message: ").append(message).append('\n')
            append("Post Time: ").append(DateUtils.formatTimestamp(item.postTime))
        }
    }

    private fun refreshStatsAndChips() {
        lifecycleScope.launch {
            val notifications = withContext(Dispatchers.IO) { viewModel.getAllNotifications() }
            val today = DateUtils.toDateDay(System.currentTimeMillis())
            val todayCount = notifications.count { it.dateDay == today }
            val topApp = notifications
                .groupBy { it.packageName }
                .maxByOrNull { it.value.size }
                ?.value
                ?.firstOrNull()
                ?.appName ?: "-"

            statsTodayView.text = "Notifications Today: $todayCount"
            statsTopAppView.text = "Most Apps: $topApp"

            val appMap = notifications.associateBy({ it.packageName }, { it.appName })
            renderAppChips(appMap.toList().sortedBy { it.second })
        }
    }

    private fun renderAppChips(apps: List<Pair<String, String>>) {
        chipsContainer.removeAllViews()
        val allChip = createChip("All", null)
        chipsContainer.addView(allChip)
        apps.forEach { (packageName, appName) ->
            chipsContainer.addView(createChip(appName, packageName))
        }
        highlightSelectedChip()
    }

    private fun createChip(label: String, packageName: String?): MaterialButton {
        val chip = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            val margin = dpToPx(6)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(margin, 0, margin, 0)
            }
            isAllCaps = false
            setBackgroundColor(Color.parseColor("#FFE0C7"))
            setTextColor(Color.parseColor("#6A2E00"))
            textSize = 12f
            minHeight = dpToPx(36)
            setOnClickListener {
                selectedPackage = packageName
                viewModel.setPackageFilter(packageName)
                highlightSelectedChip()
            }
        }
        chip.tag = packageName ?: "ALL"
        return chip
    }

    private fun highlightSelectedChip() {
        val selectedTag = selectedPackage ?: "ALL"
        for (i in 0 until chipsContainer.childCount) {
            val child = chipsContainer.getChildAt(i) as? MaterialButton ?: continue
            val isSelected = child.tag == selectedTag
            child.alpha = if (isSelected) 1f else 0.6f
            if (isSelected) {
                child.setBackgroundColor(Color.parseColor("#FF8A3D"))
                child.setTextColor(Color.WHITE)
            } else {
                child.setBackgroundColor(Color.parseColor("#FFE0C7"))
                child.setTextColor(Color.parseColor("#6A2E00"))
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun roundedBackground(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = dpToPx(12).toFloat()
        }
    }

    companion object {
        private const val MENU_EXPORT_NOW = 1
        private const val MENU_ACCESS = 2
        private const val MENU_SETTINGS = 3
    }
}
