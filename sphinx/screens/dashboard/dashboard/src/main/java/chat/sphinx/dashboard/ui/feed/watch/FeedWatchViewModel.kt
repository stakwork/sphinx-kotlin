package chat.sphinx.dashboard.ui.feed.watch

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedWatchViewState
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FeedWatchViewModel @Inject constructor(
    handler: SavedStateHandle,
    val dashboardNavigator: DashboardNavigator,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedWatchSideEffect,
        FeedWatchViewState
        >(dispatchers, FeedWatchViewState.Idle), FeedFollowingViewModel
{

    override val feedsHolderViewStateFlow: StateFlow<List<Feed>> = flow {
        repositoryDashboard.getAllFeedsOfType(FeedType.Video).collect { podcastFeeds ->
            emit(podcastFeeds.toList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun episodeItemSelected(episode: FeedItem) {
        goToVideoPlayer(
            feedId = episode.feedId,
            feedItemId = episode.id
        )
    }

    override fun feedSelected(feed: Feed) {
        goToVideoPlayer(
            feedId = feed.id
        )
    }

    private fun goToVideoPlayer(feedId: FeedId, feedItemId: FeedId? = null) {
        viewModelScope.launch(mainImmediate) {
            // TODO: Go to video player...
        }
    }
}
