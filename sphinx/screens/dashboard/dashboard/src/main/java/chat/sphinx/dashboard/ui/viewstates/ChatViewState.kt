package chat.sphinx.dashboard.ui.viewstates

import chat.sphinx.dashboard.ui.adapter.DashboardChat
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewState
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal sealed class ChatViewState: ViewState<ChatViewState>() {

    abstract val list: List<DashboardChat>

    class ListMode(override val list: List<DashboardChat>): ChatViewState()

    class SearchMode(override val list: List<DashboardChat>): ChatViewState()
}


internal class ChatViewStateContainer(
    private val dispatchers: CoroutineDispatchers,
): ViewStateContainer<ChatViewState>(ChatViewState.ListMode(emptyList())) {

    override fun updateViewState(viewState: ChatViewState) {
        throw IllegalStateException("Must utilize updateDashboardChats method")
    }

    private val lock = Mutex()

    suspend fun updateDashboardChats(dashboardChats: List<DashboardChat>, filter: String? = null) {
        lock.withLock {
            val sortedDashboardChats = withContext(dispatchers.default) {
                dashboardChats.sortedByDescending {
                    it.chat.latestMessageId?.value
                }
            }

            if (filter.isNullOrEmpty()) {
                super.updateViewState(
                    ChatViewState.ListMode(sortedDashboardChats)
                )
            } else {
                super.updateViewState(
                    ChatViewState.SearchMode(
                        sortedDashboardChats.filter {
                            it.chat.name?.value?.contains(filter, ignoreCase = true) == true
                        }
                    )
                )
            }
        }
    }
}
