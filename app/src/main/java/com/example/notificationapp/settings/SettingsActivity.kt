package com.example.notificationapp.settings

import android.os.Bundle
import android.provider.DocumentsContract
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.material.appbar.MaterialToolbar
import com.example.notificationapp.utils.ExportScheduler
import com.example.notificationapp.utils.SettingsManager

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val root = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(16)
            setPadding(padding, padding, padding, padding)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(container)

        val toolbar = MaterialToolbar(this).apply {
            title = "Settings"
            setBackgroundColor(android.graphics.Color.parseColor("#FF8A3D"))
            setTitleTextColor(android.graphics.Color.WHITE)
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener { finish() }
        }
        container.addView(toolbar)
        container.addView(content)

        content.addView(sectionTitle("Export Settings"))
        content.addView(label("Export Format"))
        val exportSpinner = Spinner(this).apply {
            adapter = ArrayAdapter<String>(
                this@SettingsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("CSV", "JSON", "CSV + JSON")
            )
            setSelection(
                when (SettingsManager.getExportFormat(this@SettingsActivity)) {
                    SettingsManager.EXPORT_FORMAT_JSON -> 1
                    SettingsManager.EXPORT_FORMAT_BOTH -> 2
                    else -> 0
                }
            )
            onItemSelectedListener = SimpleItemSelectedListener { position ->
                val value = when (position) {
                    1 -> SettingsManager.EXPORT_FORMAT_JSON
                    2 -> SettingsManager.EXPORT_FORMAT_BOTH
                    else -> SettingsManager.EXPORT_FORMAT_CSV
                }
                SettingsManager.setExportFormat(this@SettingsActivity, value)
            }
        }
        content.addView(exportSpinner)

        val loggingSwitch = Switch(this).apply {
            text = "Logging Enabled"
            isChecked = SettingsManager.isLoggingEnabled(this@SettingsActivity)
            setOnCheckedChangeListener { _, isChecked ->
                SettingsManager.setLoggingEnabled(this@SettingsActivity, isChecked)
            }
        }
        content.addView(loggingSwitch)

        val hiddenSwitch = Switch(this).apply {
            text = "Hidden Enabled"
            isChecked = SettingsManager.isShowHiddenEnabled(this@SettingsActivity)
            setOnCheckedChangeListener { _, isChecked ->
                SettingsManager.setShowHiddenEnabled(this@SettingsActivity, isChecked)
            }
        }
        content.addView(hiddenSwitch)

        val autoExportSwitch = Switch(this).apply {
            text = "Enable Auto Export"
            isChecked = SettingsManager.isAutoExportEnabled(this@SettingsActivity)
            setOnCheckedChangeListener { _, isChecked ->
                SettingsManager.setAutoExportEnabled(this@SettingsActivity, isChecked)
                if (isChecked) {
                    ExportScheduler.scheduleWeeklyExport(this@SettingsActivity)
                } else {
                    ExportScheduler.cancelWeeklyExport(this@SettingsActivity)
                }
            }
        }
        content.addView(autoExportSwitch)
        content.addView(label("Auto Export Schedule: Weekly (Sunday 00:00)"))

        val deleteAfterSwitch = Switch(this).apply {
            text = "Delete Logs After Auto Export"
            isChecked = SettingsManager.isDeleteAfterExportEnabled(this@SettingsActivity)
            setOnCheckedChangeListener { _, isChecked ->
                SettingsManager.setDeleteAfterExportEnabled(this@SettingsActivity, isChecked)
            }
        }
        content.addView(deleteAfterSwitch)

        val openFolderButton = Button(this).apply {
            text = "Open Export Folder"
            setBackgroundColor(android.graphics.Color.parseColor("#FF8A3D"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { openExportFolder() }
        }
        content.addView(openFolderButton)

        setContentView(root)
    }

    private fun openExportFolder() {
        val treeIntent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val initialUri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Documents"
        )
        treeIntent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        runCatching { startActivity(treeIntent) }.onFailure {
            val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(initialUri, "vnd.android.document/directory")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(viewIntent)
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dpToPx(12), 0, dpToPx(6))
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dpToPx(6), 0, dpToPx(4))
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}

private class SimpleItemSelectedListener(
    private val onSelected: (Int) -> Unit
) : android.widget.AdapterView.OnItemSelectedListener {
    override fun onItemSelected(
        parent: android.widget.AdapterView<*>?,
        view: android.view.View?,
        position: Int,
        id: Long
    ) {
        onSelected(position)
    }

    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
}
