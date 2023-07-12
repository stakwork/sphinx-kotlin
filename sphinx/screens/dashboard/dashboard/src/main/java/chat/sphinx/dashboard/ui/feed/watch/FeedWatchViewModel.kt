package chat.sphinx.dashboard.ui.feed.watch

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.feed.FeedRecentlyPlayedViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedWatchViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FeedWatchViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedWatchSideEffect,
        FeedWatchViewState
        >(dispatchers, FeedWatchViewState.Idle), FeedFollowingViewModel, FeedRecentlyPlayedViewModel
{

    private val _feedsHolderViewStateFlow: MutableStateFlow<List<Feed>> by lazy {
        MutableStateFlow(listOf())
    }

    override val feedsHolderViewStateFlow: StateFlow<List<Feed>>
        get() = _feedsHolderViewStateFlow

    private val _lastPlayedFeedsHolderViewStateFlow: MutableStateFlow<List<Feed>> by lazy {
        MutableStateFlow(listOf())
    }

    override val lastPlayedFeedsHolderViewStateFlow: StateFlow<List<Feed>>
        get() = _lastPlayedFeedsHolderViewStateFlow

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryDashboard.getAllFeedsOfType(FeedType.Video).collect { feeds ->

                _feedsHolderViewStateFlow.value = feeds.toList()
                    .filter { it.subscribed.isTrue() || it.chatId.value.toInt() != ChatId.NULL_CHAT_ID }
                    .sortedByDescending { it.lastPublished?.datePublished?.time ?: 0 }

                _lastPlayedFeedsHolderViewStateFlow.value = feeds.toList()
                    .filter { it.lastPlayed != null }
                    .sortedWith(compareByDescending<Feed> { it.lastPlayed?.time }.thenByDescending { it.lastPublished?.datePublished?.time ?: 0 })
            }
        }
    }

    fun videoItemSelected(video: FeedItem) {
        video.feed?.let { feed ->
            goToVideoPlayer(feed)
        }
    }

    override fun recentlyPlayedSelected(feed: Feed) {
        feedSelected(feed)
    }

    override fun feedSelected(feed: Feed) {
        goToVideoPlayer(feed)
    }

    private fun goToVideoPlayer(feed: Feed) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toVideoWatchScreen(
                feed.chat?.id ?: feed.chatId,
                feed.id,
                feed.feedUrl
            )
        }
    }
}
