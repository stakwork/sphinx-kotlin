package chat.sphinx.concept_repository_dashboard_android

import chat.sphinx.concept_paging.PageSourceWrapper
import chat.sphinx.concept_repository_dashboard.DashboardItem
import chat.sphinx.concept_repository_dashboard.RepositoryDashboard

interface RepositoryDashboardAndroid<T: Any>: RepositoryDashboard {
    suspend fun getDashboardItemPagingSource(): PageSourceWrapper<Long, DashboardItem, T>
}
