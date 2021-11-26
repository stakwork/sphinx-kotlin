package chat.sphinx.dashboard.ui.feed

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_podcast.PodcastRepository
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.feed.toFeedUrl
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_podcast.PodcastSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val chatRepository: ChatRepository,
    private val podcastRepository: PodcastRepository,
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
        searchPodcastsJob?.cancel()

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

        updateViewState(
            FeedViewState.LoadingSearchResults
        )
        
        viewModelScope.launch(mainImmediate) {
            podcastRepository.searchPodcastBy(searchTerm).collect { searchResults ->
                if (searchResults.isEmpty()) {
                    updateViewState(
                        FeedViewState.SearchPlaceHolder
                    )
                } else {
                    updateViewState(
                        FeedViewState.SearchResults(
                            searchResults
                        )
                    )
                }
            }
        }.also {
            searchPodcastsJob = it
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
    fun podcastSearchResultSelected(
        searchResult: PodcastSearchResult,
        callback: () -> Unit
    ) {
        if (searchResultSelectedJob?.isActive == true) {
            return
        }

        searchResultSelectedJob = viewModelScope.launch(mainImmediate) {
            searchResult.id.toFeedId()?.let { feedId ->
                chatRepository.getFeedById(feedId).collect { feed ->
                    feed?.let { nnFeed ->
                        goToPodcastPlayer(nnFeed)
                        callback()
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
