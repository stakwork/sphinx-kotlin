package chat.sphinx.dashboard.ui.feed.listen

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.feed.FeedRecentlyPlayedViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedListenViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedListenViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    private val feedRepository: FeedRepository,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedListenSideEffect,
        FeedListenViewState
        >(dispatchers, FeedListenViewState.Idle), FeedFollowingViewModel, FeedRecentlyPlayedViewModel
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
            repositoryDashboard.getAllFeedsOfType(FeedType.Podcast).collect { feeds ->

                _feedsHolderViewStateFlow.value = feeds.toList()
                    .filter { it.subscribed.isTrue() || it.chatId.value.toInt() != ChatId.NULL_CHAT_ID }
                    .sortedByDescending { it.lastPublished?.datePublished?.time ?: 0 }

                _lastPlayedFeedsHolderViewStateFlow.value = feeds.toList()
                    .filter { it.lastPlayed != null }
                    .sortedWith(compareByDescending<Feed> { it.lastPlayed?.time }.thenByDescending { it.lastPublished?.datePublished?.time ?: 0 })
            }
        }
    }

    override fun recentlyPlayedSelected(feed: Feed) {
        feedSelected(feed)
    }

    fun episodeItemSelected(episode: FeedItem) {
        episode.feed?.let { feed ->
            viewModelScope.launch(mainImmediate) {
                //Pause if playing
                pausePlayingIfNeeded(
                    feed,
                    episode
                )

                delay(50L)

                //Set new episode
                setEpisodeOnFeed(
                    feed,
                    episode
                )

                goToPodcastPlayer(
                    feed.chat?.id ?: feed.chatId,
                    feed.id,
                    feed.feedUrl
                )
            }
        }
    }

    private suspend fun pausePlayingIfNeeded(
        feed: Feed,
        episode: FeedItem
    ) {
        mediaPlayerServiceController.getPlayingContent()?.let { playingContent ->
            if (
                playingContent.first == feed.id.value &&
                playingContent.second != episode.id.value
            ) {
                viewModelScope.launch(mainImmediate) {
                    mediaPlayerServiceController.submitAction(
                        UserAction.ServiceAction.Pause(
                            feed.chatId,
                            playingContent.second
                        )
                    )
                }
            }
        }
    }

    private fun setEpisodeOnFeed(
        feed: Feed,
        episode: FeedItem
    ) {
        feed?.getNNContentFeedStatus()?.let { contentFeedStatus ->
            feedRepository.updateContentFeedStatus(
                feed.id,
                contentFeedStatus.feedUrl,
                contentFeedStatus.subscriptionStatus,
                contentFeedStatus.chatId,
                episode.id,
                contentFeedStatus.satsPerMinute,
                contentFeedStatus.playerSpeed
            )
        }
    }

    override fun feedSelected(feed: Feed) {
        val playingContent = mediaPlayerServiceController.getPlayingContent()

        feedRepository.restoreContentFeedStatusByFeedId(
            feed.id,
            playingContent?.first,
            playingContent?.second
        )

        goToPodcastPlayer(
            feed.chat?.id ?: feed.chatId,
            feed.id,
            feed.feedUrl
        )
    }

    private fun goToPodcastPlayer(
        chatId: ChatId,
        feedId: FeedId,
        feedUrl: FeedUrl
    ) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toPodcastPlayerScreen(
                chatId, feedId, feedUrl
            )
        }
    }
}
