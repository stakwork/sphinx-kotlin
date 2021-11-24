package chat.sphinx.dashboard.ui.feed

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_podcast_search.NetworkQueryPodcastSearch
import chat.sphinx.concept_network_query_podcast_search.model.PodcastSearchResultDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.feed.toFeedUrl
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_feed.Feed
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val chatRepository: ChatRepository,
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

        delay(1000L)
        
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

    private var searchResultSelectedJob: Job? = null
    fun podcastSearchResultSelected(searchResult: PodcastSearchResultDto) {
        if (searchResultSelectedJob?.isActive == true) {
            return
        }

        searchResultSelectedJob = viewModelScope.launch(mainImmediate) {
            searchResult.id.toFeedId()?.let { feedId ->
                chatRepository.getFeedById(feedId).collect { feed ->
                    feed?.let { nnFeed ->
                        goToPodcastPlayer(nnFeed)
                    }
                }
            }
        }

        viewModelScope.launch(mainImmediate) {
            searchResult.url.toFeedUrl()?.let { feedUrl ->
                chatRepository.updateFeedContent(
                    chatId = ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    host = ChatHost(Feed.TRIBES_DEFAULT_SERVER_URL),
                    feedUrl = feedUrl,
                    chatUUID = null,
                    false.toSubscribed(),
                    currentEpisodeId = null
                )
            }
        }
    }

    private suspend fun goToPodcastPlayer(feed: Feed) {
        dashboardNavigator.toPodcastPlayerScreen(
            feed.chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
            feed.id,
            feed.feedUrl,
            0
        )
        searchResultSelectedJob?.cancel()
    }
}
