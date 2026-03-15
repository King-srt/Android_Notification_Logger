package com.example.notificationapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.notificationapp.data.database.AppDatabase
import com.example.notificationapp.data.database.NotificationEntity
import com.example.notificationapp.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository =
        NotificationRepository(AppDatabase.getInstance(application).notificationDao())

    private val queryFlow = MutableStateFlow("")
    private val dateDayFlow = MutableStateFlow<Int?>(null)
    private val packageFilterFlow = MutableStateFlow<String?>(null)
    private val hideEmptyFlow = MutableStateFlow(false)

    val notificationsPagingFlow = combine(queryFlow, dateDayFlow, packageFilterFlow, hideEmptyFlow) {
            query,
            dateDay,
            packageName,
            hideEmpty ->
            Quadruple(query, dateDay, packageName, hideEmpty)
        }.flatMapLatest { (query, dateDay, packageName, hideEmpty) ->
            repository.getPagedNotifications(
                query.takeIf { it.isNotBlank() },
                dateDay,
                packageName,
                hideEmpty
            ).flow
        }.cachedIn(viewModelScope)

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun setDateDay(dateDay: Int?) {
        dateDayFlow.value = dateDay
    }

    fun setPackageFilter(packageName: String?) {
        packageFilterFlow.value = packageName
    }

    fun setHideEmpty(enabled: Boolean) {
        hideEmptyFlow.value = enabled
    }

    suspend fun getAllNotifications(): List<NotificationEntity> {
        return repository.getAllNotifications()
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
