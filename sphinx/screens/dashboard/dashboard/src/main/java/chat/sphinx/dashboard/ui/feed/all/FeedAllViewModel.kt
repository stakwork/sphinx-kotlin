package chat.sphinx.dashboard.ui.feed.all

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedDownloadedViewModel
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.feed.FeedRecentlyPlayedViewModel
import chat.sphinx.dashboard.ui.feed.FeedRecommendationsViewModel
import chat.sphinx.dashboard.ui.getMediaDuration
import chat.sphinx.dashboard.ui.viewstates.FeedAllViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_podcast.FeedRecommendation
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject


@HiltViewModel
internal class FeedAllViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    private val feedRepository: FeedRepository,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    val moshi: Moshi,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        FragmentActivity,
        FeedAllSideEffect,
        FeedAllViewState
        >(dispatchers, FeedAllViewState.Disabled),
    FeedFollowingViewModel,
    FeedRecommendationsViewModel,
    FeedRecentlyPlayedViewModel,
    FeedDownloadedViewModel
{

    override val feedRecommendationsHolderViewStateFlow: MutableStateFlow<List<FeedRecommendation>> = MutableStateFlow(emptyList())

    init {
        setRecommendationsVisibility()

        viewModelScope.launch(mainImmediate) {
            repositoryDashboard.getAllFeeds().collect { feeds ->

                _feedsHolderViewStateFlow.value = feeds.toList()
                    .filter { it.subscribed.isTrue() || it.chatId.value.toInt() != ChatId.NULL_CHAT_ID }
                    .sortedByDescending { it.lastPublished?.datePublished?.time ?: 0 }

                _lastPlayedFeedsHolderViewStateFlow.value = feeds.toList()
                    .filter { it.lastPlayed != null }
                    .sortedWith(compareByDescending<Feed> { it.lastPlayed?.time }.thenByDescending { it.lastPublished?.datePublished?.time ?: 0 })
            }
        }

        viewModelScope.launch(mainImmediate) {
            feedRepository.getAllDownloadedFeedItems().collect { feedItems ->
                _feedDownloadedHolderViewStateFlow.value = feedItems.sortedBy { it.feed?.lastPlayed?.value }
            }
        }
    }

    private fun setRecommendationsVisibility() {
        viewModelScope.launch(mainImmediate) {
            feedRepository.recommendationsToggleStateFlow.collect { enabled ->
                if (enabled) {
                    loadFeedRecommendations()
                } else {
                    feedRecommendationsHolderViewStateFlow.value = listOf()
                    updateViewState(FeedAllViewState.Disabled)
                }
            }
        }
    }

    override fun loadFeedRecommendations() {
        if (!feedRepository.recommendationsToggleStateFlow.value) {
            updateViewState(FeedAllViewState.Disabled)
            return
        }

        viewModelScope.launch(mainImmediate) {
            feedRepository.getPodcastById(
                FeedId(FeedRecommendation.RECOMMENDATION_PODCAST_ID)
            )?.firstOrNull()?.let { podcast ->
                if (podcast.isPlaying) {
                    submitSideEffect(FeedAllSideEffect.RefreshWhilePlaying)
                    return@launch
                }
            }

            updateViewState(FeedAllViewState.Loading)

            repositoryDashboard.getRecommendedFeeds().collect { feedRecommended ->
                feedRecommendationsHolderViewStateFlow.value = feedRecommended
                    .sortedByDescending { it.date }
                    .toList()

                if (feedRecommended.isNotEmpty()) {
                    updateViewState(FeedAllViewState.RecommendedList)
                } else updateViewState(FeedAllViewState.NoRecommendations)
            }
        }
    }

    private val _feedsHolderViewStateFlow: MutableStateFlow<List<Feed>> by lazy {
        MutableStateFlow(listOf())
    }

    override val feedsHolderViewStateFlow: StateFlow<List<Feed>>
        get() = _feedsHolderViewStateFlow.asStateFlow()

    override fun feedSelected(feed: Feed) {
        @Exhaustive
        when (feed.feedType) {
            is FeedType.Podcast -> {
                goToPodcastPlayer(feed.chatId, feed.id, feed.feedUrl)
            }
            is FeedType.Video -> {
                goToVideoPlayer(feed.chatId, feed.id, feed.feedUrl)
            }
            is FeedType.Newsletter -> {
                goToNewsletterDetail(feed.chatId, feed.feedUrl)
            }
            is FeedType.Unknown -> {}
            else -> {}
        }
    }

    private val _lastPlayedFeedsHolderViewStateFlow: MutableStateFlow<List<Feed>> by lazy {
        MutableStateFlow(listOf())
    }

    override val lastPlayedFeedsHolderViewStateFlow: StateFlow<List<Feed>>
        get() = _lastPlayedFeedsHolderViewStateFlow

    override fun recentlyPlayedSelected(feed: Feed) {
        feedSelected(feed)
    }

    private val _feedDownloadedHolderViewStateFlow: MutableStateFlow<List<FeedItem>> by lazy {
        MutableStateFlow(listOf())
    }

    override val feedDownloadedHolderViewStateFlow: StateFlow<List<FeedItem>>
        get() = _feedDownloadedHolderViewStateFlow

    override fun feedDownloadedSelected(feedItem: FeedItem) {
        feedItem.feed?.let { feed ->
            viewModelScope.launch(mainImmediate) {

                //Pause if playing
                pausePlayingIfNeeded(
                    feed,
                    feedItem
                )

                delay(50L)

                //Set new episode
                setEpisodeOnFeed(
                    feed,
                    feedItem
                )

                dashboardNavigator.toPodcastPlayerScreen(
                    feed.chat?.id ?: feed.chatId,
                    feed.id,
                    feed.feedUrl,
                    true
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

    private fun retrieveEpisodeDuration(
        episode: PodcastEpisode
    ): Long {
        val duration = episode.localFile?.let {
            Uri.fromFile(it).getMediaDuration(true)
        } ?: Uri.parse(episode.episodeUrl).getMediaDuration(false)

        viewModelScope.launch(io) {
            feedRepository.updateContentEpisodeStatus(
                feedId = episode.podcastId,
                itemId = episode.id,
                FeedItemDuration(duration / 1000),
                FeedItemDuration(episode.currentTimeSeconds)
            )
        }

        return duration
    }


    private fun goToPodcastPlayer(
        chatId: ChatId,
        feedId: FeedId,
        feedUrl: FeedUrl
    ) {
        val playingContent = mediaPlayerServiceController.getPlayingContent()

        feedRepository.restoreContentFeedStatusByFeedId(
            feedId,
            playingContent?.first,
            playingContent?.second
        )

        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toPodcastPlayerScreen(
                chatId, feedId, feedUrl
            )
        }
    }

    private fun goToNewsletterDetail(chatId: ChatId, feedUrl: FeedUrl) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toNewsletterDetail(chatId, feedUrl)
        }
    }

    private fun goToVideoPlayer(chatId: ChatId, feedId: FeedId, feedUrl: FeedUrl) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toVideoWatchScreen(chatId, feedId, feedUrl)
        }
    }

    override fun feedRecommendationSelected(feed: FeedRecommendation) {
        viewModelScope.launch(mainImmediate) {
            val recommendations = feedRecommendationsHolderViewStateFlow.value

            if (recommendations.isEmpty()) {
                return@launch
            }

            feedRepository.getPodcastById(
                FeedId(FeedRecommendation.RECOMMENDATION_PODCAST_ID)
            ).firstOrNull()?.let { podcast ->

                pauseEpisodeIfNeeded(
                    podcast,
                    FeedId(feed.id)
                )

                dashboardNavigator.toCommonPlayerScreen(
                    podcast.id,
                    FeedId(feed.id)
                )
            }
        }
    }

    private fun pauseEpisodeIfNeeded(
        podcast: Podcast,
        episodeId: FeedId
    ) {
        viewModelScope.launch(mainImmediate) {
            val currentEpisode = podcast.getCurrentEpisode()

            if (currentEpisode.playing && currentEpisode.id != episodeId) {
                podcast.didPausePlayingEpisode(currentEpisode)

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Pause(
                        ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        currentEpisode.id.value
                    )
                )
            }
        }
    }

}
