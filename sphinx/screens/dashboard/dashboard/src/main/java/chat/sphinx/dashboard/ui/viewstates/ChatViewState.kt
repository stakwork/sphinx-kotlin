package chat.sphinx.dashboard.ui.viewstates

import app.cash.exhaustive.Exhaustive
import chat.sphinx.dashboard.ui.adapter.DashboardChat
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewState
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal sealed class ChatViewState: ViewState<ChatViewState>() {

    abstract val list: List<DashboardChat>

    class ListMode(
        override val list: List<DashboardChat>
    ): ChatViewState()

    class SearchMode(
        val filter: ChatFilter.FilterBy,
        override val list: List<DashboardChat>
    ): ChatViewState()
}

internal sealed class ChatFilter {
    object UseCurrent: ChatFilter()
    class FilterBy(val value: CharSequence): ChatFilter()
    object ClearFilter: ChatFilter()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun List<DashboardChat>.filterDashboardChats(
    filter: CharSequence
): List<DashboardChat> =
    filter {
        it.chat.name?.value?.contains(filter, ignoreCase = true) == true
    }

internal class ChatViewStateContainer(
    private val dispatchers: CoroutineDispatchers,
): ViewStateContainer<ChatViewState>(ChatViewState.ListMode(emptyList())) {

    override fun updateViewState(viewState: ChatViewState) {
        throw IllegalStateException("Must utilize updateDashboardChats method")
    }

    private val lock = Mutex()

    suspend fun updateDashboardChats(
        dashboardChats: List<DashboardChat>,
        filter: ChatFilter = ChatFilter.UseCurrent
    ) {
        lock.withLock {
            val sortedDashboardChats = withContext(dispatchers.default) {
                dashboardChats.sortedByDescending {
                    it.chat.latestMessageId?.value
                }
            }

            @Exhaustive
            when (filter) {
                is ChatFilter.UseCurrent -> {

                    @Exhaustive
                    when (val viewState = viewStateFlow.value) {
                        is ChatViewState.ListMode -> {
                            super.updateViewState(
                                ChatViewState.ListMode(sortedDashboardChats)
                            )
                        }
                        is ChatViewState.SearchMode -> {
                            super.updateViewState(
                                ChatViewState.SearchMode(
                                    viewState.filter,
                                    sortedDashboardChats.filterDashboardChats(viewState.filter.value)
                                )
                            )
                        }
                    }

                }
                is ChatFilter.ClearFilter -> {
                    super.updateViewState(
                        ChatViewState.ListMode(sortedDashboardChats)
                    )
                }
                is ChatFilter.FilterBy -> {
                    super.updateViewState(
                        ChatViewState.SearchMode(
                            filter,
                            sortedDashboardChats.filterDashboardChats(filter.value)
                        )
                    )
                }
            }
        }
    }
}
