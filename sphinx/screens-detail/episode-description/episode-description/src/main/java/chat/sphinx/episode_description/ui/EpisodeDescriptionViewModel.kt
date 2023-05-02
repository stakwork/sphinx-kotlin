package chat.sphinx.episode_description.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.core.view.ContentInfoCompat
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
import chat.sphinx.wrapper_common.feed.*
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

                if (args.argIsRecommendation) {
                    feedRepository.getRecommendationFeedItemById(nnFeedId)
                } else {
                    feedRepository.getFeedItemById(nnFeedId)
                }.collect { feedItem ->

                    feedItem?.let { nnFeedItem ->

                        _feedItemStateFlow.value = nnFeedItem

                        feedRepository.getPodcastById(nnFeedItem.feedId).firstOrNull()?.let {
                            setPodcastFeed(it)
                        }

                        val itemDuration = nnFeedItem.contentEpisodeStatus?.duration?.value?.toInt()

                        val feedItemDescription = FeedItemDescription(
                            feedItemTitle = nnFeedItem.titleToShow,
                            feedTitle = nnFeedItem.feed?.titleToShow,
                            feedType = nnFeedItem.feed?.feedType ?: nnFeedItem.feedType,
                            itemDate = nnFeedItem.datePublished?.hhmmElseDate() ?: "",
                            itemDuration = if ((itemDuration ?: 0) > 0) (itemDuration ?: 0).toHrAndMin() else "",
                            downloaded = nnFeedItem.localFile != null,
                            downloading = isFeedItemDownloadInProgress(),
                            played = nnFeedItem.contentEpisodeStatus?.played == true,
                            playing = isFeedItemPlaying(),
                            link = nnFeedItem.link?.value ?: "",
                            description = nnFeedItem.descriptionToShow,
                            descriptionExpanded = false,
                            image = nnFeedItem.imageUrlToShow?.value ?: "",
                            isRecommendation =  args.argIsRecommendation,
                            headerVisible = false
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
        viewModelScope.launch(mainImmediate) {
            (app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
                (currentViewState as? EpisodeDescriptionViewState.ItemDescription)?.feedItemDescription?.let { feedItemDescription ->

                   val link = _feedItemStateFlow.value?.id?.let { feedId ->
                        if (feedItemDescription.feedType is FeedType.Podcast && !feedItemDescription.isRecommendation) {
                            generateSphinxFeedItemLink(feedId)
                        } else {
                            _feedItemStateFlow.value?.link?.value ?: ""
                        }
                    } ?: _feedItemStateFlow.value?.link?.value ?: ""

                    val clipData = ClipData.newPlainText("text", link)
                    manager.setPrimaryClip(clipData)
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

    fun toggleHeader(show: Boolean) {
        (currentViewState as? EpisodeDescriptionViewState.ItemDescription)?.feedItemDescription?.copy(
            headerVisible = show
        )?.let {
            updateViewState(
                EpisodeDescriptionViewState.ItemDescription(it)
            )
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
        viewModelScope.launch(mainImmediate) {
            (currentViewState as? EpisodeDescriptionViewState.ItemDescription)?.feedItemDescription?.let { feedItemDescription ->

                val link = _feedItemStateFlow.value?.id?.let { feedId ->
                    if (feedItemDescription.feedType is FeedType.Podcast && !feedItemDescription.isRecommendation) {
                        generateSphinxFeedItemLink(feedId)
                    } else {
                        _feedItemStateFlow.value?.link?.value ?: ""
                    }
                } ?: _feedItemStateFlow.value?.link?.value ?: ""

                val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, link)
                }
                context.startActivity(Intent.createChooser(sharingIntent, label))
            }
        }
    }

    private suspend fun generateSphinxFeedItemLink(itemId: FeedId): String? {
        val shareAtTime = suspendCoroutine<Boolean> { continuation ->
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(EpisodeDescriptionSideEffect.CopyLinkSelection(viewModelScope) { result ->
                    continuation.resume(result)
                })
            }
        }

        val nnPodcast = getPodcastFeed() ?: return null
        val feed = feedRepository.getFeedById(nnPodcast.id).firstOrNull() ?: return null
        val currentTime = nnPodcast.getEpisodeWithId(itemId.value)?.getUpdatedContentEpisodeStatus()?.currentTime?.value

        return if (shareAtTime && currentTime != null) {
            generateFeedItemLink(feed.feedUrl, feed.id, itemId, currentTime)
        } else {
            generateFeedItemLink(feed.feedUrl, feed.id, itemId, null)
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
                descriptionExpanded = !it.descriptionExpanded,
                headerVisible = false
            )?.let { updatedFeedItemDescription ->
                updateViewState(
                    EpisodeDescriptionViewState.ItemDescription(updatedFeedItemDescription)
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
