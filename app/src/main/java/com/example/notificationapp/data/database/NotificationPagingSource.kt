package com.example.notificationapp.data.database

import androidx.paging.PagingSource
import androidx.paging.PagingState

class NotificationPagingSource(
    private val dao: NotificationDao,
    private val query: String?,
    private val dateDay: Int?,
    private val packageName: String?
) : PagingSource<Int, NotificationEntity>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationEntity> {
        return try {
            val page = params.key ?: 0
            val limit = params.loadSize
            val offset = page * limit
            val data = when {
                !query.isNullOrBlank() && dateDay != null && !packageName.isNullOrBlank() ->
                    dao.searchNotificationsPageByAppAndDate(
                        packageName,
                        "%$query%",
                        dateDay,
                        limit,
                        offset
                    )
                !query.isNullOrBlank() && !packageName.isNullOrBlank() ->
                    dao.searchNotificationsPageByApp(packageName, "%$query%", limit, offset)
                dateDay != null && !packageName.isNullOrBlank() ->
                    dao.getNotificationsPageByAppAndDate(packageName, dateDay, limit, offset)
                !packageName.isNullOrBlank() ->
                    dao.getNotificationsPageByApp(packageName, limit, offset)
                !query.isNullOrBlank() && dateDay != null ->
                    dao.searchNotificationsPageByDate("%$query%", dateDay, limit, offset)
                !query.isNullOrBlank() ->
                    dao.searchNotificationsPage("%$query%", limit, offset)
                dateDay != null ->
                    dao.getNotificationsPageByDate(dateDay, limit, offset)
                else ->
                    dao.getNotificationsPage(limit, offset)
            }

            val nextKey = if (data.size < limit) null else page + 1
            LoadResult.Page(
                data = data,
                prevKey = if (page == 0) null else page - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, NotificationEntity>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}
