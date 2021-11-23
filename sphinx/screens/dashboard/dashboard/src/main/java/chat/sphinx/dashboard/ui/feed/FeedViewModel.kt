package chat.sphinx.dashboard.ui.feed

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_podcast_search.NetworkQueryPodcastSearch
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val networkQueryPodcastSearch: NetworkQueryPodcastSearch,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedSideEffect,
        FeedViewState
        >(dispatchers, FeedViewState.Idle)
{


    private var searchPodcastsJob: Job? = null
    suspend fun searchPodcastBy(
        searchTerm: String,
        searchFieldActive: Boolean
    ) {
        if (searchPodcastsJob?.isActive == true) {
            searchPodcastsJob?.cancel()
        }

        if (searchTerm.isEmpty()) {
            updateViewState(
                if (searchFieldActive) {
                    FeedViewState.SearchPlaceHolder
                } else {
                    FeedViewState.Idle
                }
            )
            return
        }

        delay(500L)
        
        searchPodcastsJob = viewModelScope.launch(mainImmediate) {
            networkQueryPodcastSearch.searchPodcasts(searchTerm).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                        updateViewState(
                            FeedViewState.LoadingSearchResults
                        )
                    }
                    is Response.Error -> {
                        updateViewState(
                            FeedViewState.SearchPlaceHolder
                        )
                    }
                    is Response.Success -> {
                        if (loadResponse.value.isNotEmpty()) {
                            updateViewState(
                                FeedViewState.SearchResults(
                                    loadResponse.value
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleSearchState(searchFieldActive: Boolean) {
        val viewState = currentViewState

        if (viewState is FeedViewState.Idle && searchFieldActive) {
            updateViewState(FeedViewState.SearchPlaceHolder)
        } else if (viewState is FeedViewState.SearchPlaceHolder) {
            updateViewState(FeedViewState.Idle)
        }
    }
}
