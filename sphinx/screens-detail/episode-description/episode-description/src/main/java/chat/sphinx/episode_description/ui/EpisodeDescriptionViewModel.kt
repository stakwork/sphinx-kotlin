package chat.sphinx.episode_description.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.create_description.R
import chat.sphinx.episode_description.model.FeedItemDescription
import chat.sphinx.episode_description.navigation.EpisodeDescriptionNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.ContentEpisodeStatus
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import chat.sphinx.wrapper_podcast.toHrAndMin
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EpisodeDescriptionViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val app: Application,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    val navigator: EpisodeDescriptionNavigator,
): SideEffectViewModel<
        FragmentActivity,
        EpisodeDescriptionSideEffect,
        EpisodeDescriptionViewState,
        >(dispatchers, EpisodeDescriptionViewState.Idle),
    MediaPlayerServiceController.MediaServiceListener {

    private val args: EpisodeDescriptionFragmentArgs by savedStateHandle.navArgs()

    private val _feedItemStateFlow: MutableStateFlow<FeedItem?> by lazy {
        MutableStateFlow(null)
    }

    private val feedItemStateFlow: StateFlow<FeedItem?>
        get() = _feedItemStateFlow.asStateFlow()

    val feedItemDetailsMenuViewStateContainer: ViewStateContainer<FeedItemDetailsMenuViewState> by lazy {
        ViewStateContainer(FeedItemDetailsMenuViewState.Closed)
    }

    var podcast: Podcast? = null

    private fun getPodcastFeed(): Podcast? {
        return podcast
    }

    private fun setPodcastFeed(podcast: Podcast) {
        val episodeId = this.podcast?.episodeId
        val playing = this.podcast?.playingEpisode?.playing == true

        val timeMilliSeconds = if (podcast.timeMilliSeconds > 0) {
            podcast.timeMilliSeconds
        } else {
            this.podcast?.timeMilliSeconds ?: 0
        }

        this.podcast = podcast
        this.podcast?.episodeId = episodeId
        this.podcast?.playingEpisode?.playing = playing
        this.podcast?.timeMilliSeconds = timeMilliSeconds
    }

    init {
        mediaPlayerServiceController.addListener(this)

        viewModelScope.launch(mainImmediate) {
            args.argFeedId.toFeedId()?.let { nnFeedId ->
                feedRepository.getFeedItemById(nnFeedId).collect { feedItem ->
                    feedItem?.let { nnFeedItem ->

                        _feedItemStateFlow.value = nnFeedItem

                        feedRepository.getPodcastById(nnFeedItem.feedId).firstOrNull()?.let {
                            setPodcastFeed(it)
                        }

                        val itemDuration = nnFeedItem.contentEpisodeStatus?.duration?.value?.toInt()

                        val feedItemDescription = FeedItemDescription(
                            feedItemTitle = nnFeedItem.titleToShow,
                            feedTitle = nnFeedItem.feed?.titleToShow,
                            feedType = nnFeedItem.feed?.feedType,
                            itemDate = nnFeedItem.datePublished?.hhmmElseDate() ?: "",
                            itemDuration = if ((itemDuration ?: 0) > 0) (itemDuration ?: 0).toHrAndMin() else "",
                            downloaded = nnFeedItem.localFile != null,
                            downloading = isFeedItemDownloadInProgress(),
                            played = nnFeedItem.contentEpisodeStatus?.played == true,
                            playing = isFeedItemPlaying(),
                            link = nnFeedItem.link?.value ?: "",
                            description = nnFeedItem.descriptionToShow,
                            descriptionExpanded = false,
                            image = nnFeedItem.imageUrlToShow?.value ?: ""
                        )

                        updateViewState(
                            EpisodeDescriptionViewState.ItemDescription(feedItemDescription)
                        )
                    }
                }
            }
        }
    }

    fun openDetailScreen(){
        feedItemDetailsMenuViewStateContainer.updateViewState(
            FeedItemDetailsMenuViewState.Open
        )
    }

    fun closeScreen() {
        viewModelScope.launch(mainImmediate) {
            if (feedItemDetailsMenuViewStateContainer.value is FeedItemDetailsMenuViewState.Open) {
                feedItemDetailsMenuViewStateContainer.updateViewState(
                    FeedItemDetailsMenuViewState.Closed
                )
            } else {
                navigator.popBackStack()
            }
        }
    }

    private fun isFeedItemDownloadInProgress(): Boolean {
        feedItemStateFlow.value?.let {
            return repositoryMedia.inProgressDownloadIds().contains(it.id)
        }
        return false
    }

    private fun isFeedItemPlaying(): Boolean {
        feedItemStateFlow.value?.let {
            val playingContent = mediaPlayerServiceController.getPlayingContent()
            return playingContent?.second == it.id.value
        }
        return false
    }

    private suspend fun getPlayedMark(feedItemId: FeedId): Boolean {
        return feedRepository.getPlayedMark(feedItemId).firstOrNull() ?: false
    }

    fun updatePlayedMark() {
        _feedItemStateFlow.value?.let { feedItem ->
            val played = feedItem.contentEpisodeStatus?.played ?: false
            feedRepository.updatePlayedMark(feedItem.id, !played)

            feedItem.contentEpisodeStatus = ContentEpisodeStatus(
                feedItem.feedId,
                feedItem.id,
                feedItem.contentEpisodeStatus?.duration ?: FeedItemDuration(0),
                feedItem.contentEpisodeStatus?.currentTime ?: FeedItemDuration(0),
                !played
            )

            _feedItemStateFlow.value = feedItem

            (currentViewState as? EpisodeDescriptionViewState.ItemDescription)?.feedItemDescription?.copy(
                played = !played
            )?.let {
                updateViewState(
                    EpisodeDescriptionViewState.ItemDescription(it)
                )
            }
        }
    }

    fun copyCodeToClipboard() {
        _feedItemStateFlow.value?.link?.value.let { feedItemLink ->
            (app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
                val clipData = ClipData.newPlainText("text", feedItemLink)
                manager.setPrimaryClip(clipData)

                viewModelScope.launch(mainImmediate) {
                    submitSideEffect(
                        EpisodeDescriptionSideEffect.Notify(
                            app.getString(R.string.episode_detail_clipboard)
                        )
                    )
                }
            }
        }
    }

    fun togglePlayState() {
        viewModelScope.launch(mainImmediate) {
            _feedItemStateFlow.value?.let { feedItem ->
                if (mediaPlayerServiceController.getPlayingContent()?.second == feedItem.id.value) {
                    pauseEpisode(feedItem.id)
                } else {
                    delay(50L)
                    playEpisode(feedItem.id)
                }
            }
        }
    }

    private fun playEpisode(feedItemId: FeedId) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                podcast.getEpisodeWithId(feedItemId.value)?.let { episode ->

                    podcast.willStartPlayingEpisode(
                        episode,
                        episode.currentTimeMilliseconds ?: 0,
                        ::retrieveEpisodeDuration
                    )

                    mediaPlayerServiceController.submitAction(
                        UserAction.ServiceAction.Play(
                            ChatId(ChatId.NULL_CHAT_ID.toLong()),
                            episode.episodeUrl,
                            podcast.getUpdatedContentFeedStatus(),
                            podcast.getUpdatedContentEpisodeStatus()
                        )
                    )
                }
            }
        }
    }

    private fun pauseEpisode(feedItemId: FeedId) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                podcast.getEpisodeWithId(feedItemId.value)?.let { episode ->
                    podcast.didPausePlayingEpisode(episode)

                    mediaPlayerServiceController.submitAction(
                        UserAction.ServiceAction.Pause(
                            ChatId(ChatId.NULL_CHAT_ID.toLong()),
                            episode.id.value
                        )
                    )
                }
            }
        }
    }

    fun share(
        context: Context,
        label: String
    ) {
        _feedItemStateFlow.value?.link?.value.let { feedItemLink ->
            val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, feedItemLink)
            }

            context.startActivity(
                Intent.createChooser(
                    sharingIntent,
                    label
                )
            )
        }
    }

    fun toggleDownloadState() {
        (currentViewState as? EpisodeDescriptionViewState.ItemDescription)?.feedItemDescription?.let { itemDescription ->
            if (itemDescription.downloaded) {
                deleteDownloadedMediaByItemId()
            } else {
                downloadMedia()
            }
        }
    }

    fun downloadMedia() {
        _feedItemStateFlow.value?.let { feedItem ->
            getPodcastFeed()?.let { podcast ->
                podcast.getEpisodeWithId(feedItem.id.value)?.let { episode ->
                    repositoryMedia.downloadMediaIfApplicable(
                        episode
                    ) { downloadedFile ->
                        episode.localFile = downloadedFile
                    }

                    (currentViewState as? EpisodeDescriptionViewState.ItemDescription)?.feedItemDescription?.copy(
                        downloading = isFeedItemDownloadInProgress()
                    )?.let {
                        updateViewState(
                            EpisodeDescriptionViewState.ItemDescription(it)
                        )
                    }
                }
            }
        }
    }

    private fun deleteDownloadedMediaByItemId() {
        viewModelScope.launch(mainImmediate) {
            _feedItemStateFlow.value?.let { feedItem ->
                repositoryMedia.deleteDownloadedMediaIfApplicable(feedItem)
            }
        }
    }

    fun toggleDescriptionExpanded() {
        (currentViewState as? EpisodeDescriptionViewState.ItemDescription)?.feedItemDescription?.let {
            it.copy(
                descriptionExpanded = !it.descriptionExpanded
            )?.let {
                updateViewState(
                    EpisodeDescriptionViewState.ItemDescription(it)
                )
            }
        }
    }

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.episodeId != _feedItemStateFlow.value?.id?.value) {
                return
            }
        }
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                when (serviceState) {
                    is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                        podcast.playingEpisodeUpdate(
                            serviceState.episodeId,
                            serviceState.currentTime,
                            serviceState.episodeDuration.toLong(),
                            serviceState.speed
                        )
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                        podcast.pauseEpisodeUpdate()
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                        podcast.endEpisodeUpdate(
                            serviceState.episodeId,
                            ::retrieveEpisodeDuration
                        )

                        feedRepository.updatePlayedMark(podcast.id, true)
                    }
                    else -> {}
                }

                (currentViewState as? EpisodeDescriptionViewState.ItemDescription)?.feedItemDescription?.copy(
                    playing = podcast.isPlaying
                )?.let {
                    updateViewState(
                        EpisodeDescriptionViewState.ItemDescription(it)
                    )
                }
            }
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
}

fun Uri.getMediaDuration(
    isLocalFile: Boolean
): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        if (Build.VERSION.SDK_INT >= 14 && !isLocalFile) {
            retriever.setDataSource(this.toString(), HashMap<String, String>())
        } else {
            retriever.setDataSource(this.toString())
        }
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        duration?.toLongOrNull() ?: 0
    } catch (exception: Exception) {
        0
    }
}
