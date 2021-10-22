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
    abstract val originalList: List<DashboardChat>

    class ListMode(
        override val list: List<DashboardChat>,
        override val originalList: List<DashboardChat>
    ): ChatViewState()

    class SearchMode(
        val filter: ChatFilter.FilterBy,
        override val list: List<DashboardChat>,
        override val originalList: List<DashboardChat>
    ): ChatViewState()
}

internal sealed class ChatFilter {

    /**
     * Will use the current filter (if any) applied to the list of [DashboardChat]s.
     * */
    object UseCurrent: ChatFilter()

    /**
     * Will filter the list of [DashboardChat]s based on the provided [value]
     * */
    class FilterBy(val value: CharSequence): ChatFilter() {
        init {
            require(value.isNotEmpty()) {
                "ChatFilter.FilterBy cannot be empty. Use ClearFilter."
            }
        }
    }

    /**
     * Clears any applied filters.
     * */
    object ClearFilter: ChatFilter()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun List<DashboardChat>.filterDashboardChats(
    filter: CharSequence
): List<DashboardChat> =
    filter {
        it.chatName?.contains(filter, ignoreCase = true) == true
    }

// TODO: Need to preserve the original list when going between list and search modes.
internal class ChatViewStateContainer(
    private val dispatchers: CoroutineDispatchers,
): ViewStateContainer<ChatViewState>(ChatViewState.ListMode(emptyList(), emptyList())) {

    override fun updateViewState(viewState: ChatViewState) {
        throw IllegalStateException("Must utilize updateDashboardChats method")
    }

    private val lock = Mutex()

    /**
     * Sorts and filters the provided list.
     *
     * @param [dashboardChats] if `null` uses the current, already sorted list.
     * @param [filter] the type of filtering to apply to the list. See [ChatFilter].
     * */
    suspend fun updateDashboardChats(
        dashboardChats: List<DashboardChat>?,
        filter: ChatFilter = ChatFilter.UseCurrent
    ) {
        lock.withLock {
            val sortedDashboardChats = if (dashboardChats != null) {
                withContext(dispatchers.default) {
                    dashboardChats.sortedByDescending { it.sortBy }
                }
            } else {
                viewStateFlow.value.originalList
            }

            @Exhaustive
            when (filter) {
                is ChatFilter.UseCurrent -> {
                    @Exhaustive
                    when (val viewState = viewStateFlow.value) {
                        is ChatViewState.ListMode -> {
                            super.updateViewState(
                                ChatViewState.ListMode(
                                    sortedDashboardChats,
                                    sortedDashboardChats
                                )
                            )
                        }
                        is ChatViewState.SearchMode -> {
                            super.updateViewState(
                                ChatViewState.SearchMode(
                                    viewState.filter,
                                    withContext(dispatchers.default) {
                                        sortedDashboardChats
                                            .filterDashboardChats(viewState.filter.value)
                                    },
                                    sortedDashboardChats
                                )
                            )
                        }
                    }

                }
                is ChatFilter.ClearFilter -> {
                    super.updateViewState(
                        ChatViewState.ListMode(sortedDashboardChats, sortedDashboardChats)
                    )
                }
                is ChatFilter.FilterBy -> {
                    super.updateViewState(
                        ChatViewState.SearchMode(
                            filter,
                            withContext(dispatchers.default) {
                                sortedDashboardChats
                                    .filterDashboardChats(filter.value)
                            },
                            sortedDashboardChats
                        )
                    )
                }
            }
        }
    }
}
