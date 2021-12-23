package chat.sphinx.dashboard.ui.feed

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.DeepLinkPopupViewState
import chat.sphinx.dashboard.ui.viewstates.FeedChipsViewState
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.feed.toFeedUrl
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.FeedSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val feedRepository: FeedRepository,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedSideEffect,
        FeedViewState
        >(dispatchers, FeedViewState.Idle)
{

    val feedChipsViewStateContainer: ViewStateContainer<FeedChipsViewState> by lazy {
        ViewStateContainer(FeedChipsViewState.All)
    }

    private var searchFeedsJob: Job? = null
    suspend fun searchFeedsBy(
        searchTerm: String,
        feedType: FeedType?,
        searchFieldActive: Boolean
    ) {
        searchFeedsJob?.cancel()

        if (searchTerm.isEmpty()) {
            updateViewState(
                if (searchFieldActive) {
                    when (feedChipsViewStateContainer.value) {
                        is FeedChipsViewState.Listen -> {
                            FeedViewState.SearchPodcastPlaceHolder
                        }
                        is FeedChipsViewState.Watch -> {
                            FeedViewState.SearchVideoPlaceHolder
                        }
                        else -> {
                            FeedViewState.SearchPlaceHolder
                        }
                    }
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
            delay(500)

            feedRepository.searchFeedsBy(
                searchTerm,
                feedType
            ).collect { searchResults ->
                if (searchResults.isEmpty()) {
                    updateViewState(
                        when (feedChipsViewStateContainer.value) {
                            is FeedChipsViewState.Listen -> {
                                FeedViewState.SearchPodcastPlaceHolder
                            }
                            is FeedChipsViewState.Watch -> {
                                FeedViewState.SearchVideoPlaceHolder
                            }
                            else -> {
                                FeedViewState.SearchPlaceHolder
                            }
                        }
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
            searchFeedsJob = it
        }
    }

    fun toggleSearchState(searchFieldActive: Boolean) {
        val viewState = currentViewState

        if (viewState is FeedViewState.Idle && searchFieldActive) {
            updateViewState(
                when (feedChipsViewStateContainer.value) {
                    is FeedChipsViewState.Listen -> {
                        FeedViewState.SearchPodcastPlaceHolder
                    }
                    is FeedChipsViewState.Watch -> {
                        FeedViewState.SearchVideoPlaceHolder
                    }
                    else -> {
                        FeedViewState.SearchPlaceHolder
                    }
                }
            )
        } else if (
            viewState is FeedViewState.SearchPlaceHolder ||
            viewState is FeedViewState.SearchPodcastPlaceHolder ||
            viewState is FeedViewState.SearchVideoPlaceHolder
        ) {
            updateViewState(FeedViewState.Idle)
        }
    }

    private var searchResultSelectedJob: Job? = null
    fun feedSearchResultSelected(
        searchResult: FeedSearchResult,
        callback: () -> Unit
    ) {
        if (searchResultSelectedJob?.isActive == true) {
            callback()
            return
        }

        searchResultSelectedJob = viewModelScope.launch(mainImmediate) {
            searchResult.id.toFeedId()?.let { feedId ->
                feedRepository.getFeedById(feedId).collect { feed ->
                    feed?.let { nnFeed ->
                        goToFeedDetailView(nnFeed)
                        callback()
                    }
                }
            }
        }

        viewModelScope.launch(mainImmediate) {
            searchResult.url.toFeedUrl()?.let { feedUrl ->
                feedRepository.updateFeedContent(
                    chatId = ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    host = ChatHost(Feed.TRIBES_DEFAULT_SERVER_URL),
                    feedUrl = feedUrl,
                    searchResultDescription = searchResult.description?.toFeedDescription(),
                    searchResultImageUrl = searchResult.imageUrl?.toPhotoUrl(),
                    chatUUID = null,
                    subscribed = false.toSubscribed(),
                    currentEpisodeId = null
                )
            }
        }
    }

    private suspend fun goToFeedDetailView(feed: Feed) {
        when {
            feed.isPodcast -> {
                dashboardNavigator.toPodcastPlayerScreen(
                    feed.chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    feed.id,
                    feed.feedUrl,
                    0
                )
            }
            feed.isVideo -> {
                dashboardNavigator.toVideoWatchScreen(
                    feed.chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    feed.id,
                    feed.feedUrl
                )
            }
            feed.isNewsletter -> {
                dashboardNavigator.toNewsletterDetail(
                    feed.chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    feed.feedUrl
                )
            }
        }
        searchResultSelectedJob?.cancel()
    }
}
