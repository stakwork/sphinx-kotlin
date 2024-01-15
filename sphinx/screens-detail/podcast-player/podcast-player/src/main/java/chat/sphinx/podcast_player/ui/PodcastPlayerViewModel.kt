package chat.sphinx.podcast_player.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.concept_view_model_coordinator.ResponseHolder
import chat.sphinx.feature_view_model_coordinator.RequestCatcher
import chat.sphinx.kotlin_response.Response
import chat.sphinx.podcast_player.R
import chat.sphinx.podcast_player.coordinator.PodcastPlayerViewModelCoordinator
import chat.sphinx.podcast_player.navigation.BackType
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.podcast_player.ui.viewstates.BoostAnimationViewState
import chat.sphinx.podcast_player.ui.viewstates.FeedItemDetailsViewState
import chat.sphinx.podcast_player.ui.viewstates.PodcastPlayerViewState
import chat.sphinx.podcast_player_view_model_coordinator.response.PodcastPlayerResponse
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.StorageData
import chat.sphinx.wrapper_common.StorageLimit.DEFAULT_STORAGE_LIMIT
import chat.sphinx.wrapper_common.StorageLimit.STORAGE_LIMIT_KEY
import chat.sphinx.wrapper_common.calculateUserStorageLimit
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.toFileSize
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItemDetail
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.FeedBoost
import chat.sphinx.wrapper_message.PodcastClip
import chat.sphinx.wrapper_message.toJson
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import chat.sphinx.wrapper_podcast.toHrAndMin
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

internal inline val PodcastPlayerFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

internal inline val PodcastPlayerFragmentArgs.feedId: FeedId
    get() = FeedId(argFeedId)

internal inline val PodcastPlayerFragmentArgs.fromFeed: Boolean
    get() = argFromFeed

internal inline val PodcastPlayerFragmentArgs.fromDownloaded: Boolean
    get() = argFromDownloaded

@HiltViewModel
internal class PodcastPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PodcastPlayerNavigator,
    private val app: Application,
    private val moshi: Moshi,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val repositoryMedia: RepositoryMedia,
    private val feedRepository: FeedRepository,
    private val actionsRepository: ActionsRepository,
    private val lightningRepository: LightningRepository,
    savedStateHandle: SavedStateHandle,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    private val podcastPlayerViewModelCoordinator: PodcastPlayerViewModelCoordinator,
) : SideEffectViewModel<
        FragmentActivity,
        PodcastPlayerSideEffect,
        PodcastPlayerViewState,
        >(dispatchers, PodcastPlayerViewState.Idle)
, MediaPlayerServiceController.MediaServiceListener {

    private val args: PodcastPlayerFragmentArgs by savedStateHandle.navArgs()
    private var storageData: StorageData? = null

    private suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        lightningRepository.getAccountBalance()

    private val podcastFlow: Flow<Podcast?> =
        if (args.argChatId != ChatId.NULL_CHAT_ID.toLong()) {
            feedRepository.getPodcastByChatId(args.chatId)
        } else {
            feedRepository.getPodcastById(args.feedId)
        }


    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)

                resolvedOwner!!
            }
        }
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

    val boostAnimationViewStateContainer: ViewStateContainer<BoostAnimationViewState> by lazy {
        ViewStateContainer(BoostAnimationViewState.Idle)
    }

    val feedItemDetailsViewStateContainer: ViewStateContainer<FeedItemDetailsViewState> by lazy {
        ViewStateContainer(FeedItemDetailsViewState.Closed)
    }

    private val _feedItemDetailStateFlow: MutableStateFlow<FeedItemDetail?> by lazy {
        MutableStateFlow(null)
    }

    private val feedItemDetailStateFlow: StateFlow<FeedItemDetail?>
        get() = _feedItemDetailStateFlow.asStateFlow()

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId != args.chatId) {
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
                        viewStateContainer.updateViewState(
                            PodcastPlayerViewState.MediaStateUpdate(
                                podcast,
                                args.fromDownloaded,
                                serviceState
                            )
                        )
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                        podcast.pauseEpisodeUpdate()

                        viewStateContainer.updateViewState(
                            PodcastPlayerViewState.MediaStateUpdate(
                                podcast,
                                args.fromDownloaded,
                                serviceState
                            )
                        )
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                        podcast.endEpisodeUpdate(
                            serviceState.episodeId,
                            ::retrieveEpisodeDuration
                        )
                        viewStateContainer.updateViewState(
                            PodcastPlayerViewState.MediaStateUpdate(
                                podcast,
                                args.fromDownloaded,
                                serviceState
                            )
                        )
                        feedRepository.updatePlayedMark(podcast.id, true)
                    }
                    is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {
                        setPaymentsDestinations()
                    }
                    is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                        viewStateContainer.updateViewState(PodcastPlayerViewState.ServiceLoading)
                    }
                    is MediaPlayerServiceState.ServiceInactive -> {
                        podcast.pauseEpisodeUpdate()
                        viewStateContainer.updateViewState(PodcastPlayerViewState.ServiceInactive)
                    }
                    else -> {}
                }
            }
        }
    }

    init {
        mediaPlayerServiceController.addListener(this)

        viewModelScope.launch(mainImmediate) {
            podcastFlow.collect { podcast ->
                podcast?.let { nnPodcast ->
                    setPodcastFeed(podcast)
                    podcastLoaded(nnPodcast)
                }
            }
        }

        viewModelScope.launch(mainImmediate) {
            val owner = getOwner()

            boostAnimationViewStateContainer.updateViewState(
                BoostAnimationViewState.BoosAnimationInfo(
                    owner.photoUrl,
                    owner.tipAmount
                )
            )
        }

        updateFeedContentInBackground()
        getStorageData()
    }

    private val requestCatcher = RequestCatcher(
        viewModelScope,
        podcastPlayerViewModelCoordinator,
        mainImmediate
    )

    private var responseJob: Job? = null
    fun shouldShareClip() {
        if (responseJob?.isActive == true) {
            return
        }

        responseJob = viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                getOwner().nodePubKey?.let { ownerPubKey ->

                    val podcastClip = PodcastClip(
                        text = null,
                        title = podcast.getCurrentEpisode().title.value,
                        pubkey = ownerPubKey,
                        url = podcast.getCurrentEpisode().episodeUrl,
                        feedID = podcast.id,
                        itemID = podcast.getCurrentEpisode().id,
                        ts = (podcast.timeMilliSeconds / 1000).toInt()
                    )

                    actionsRepository.trackPodcastClipComments(
                        podcast.getCurrentEpisode().id,
                        podcast.timeMilliSeconds / 1000,
                        arrayListOf("")
                    )

                    try {
                        requestCatcher.getCaughtRequestStateFlow().collect { list ->
                            list.firstOrNull()?.let { requestHolder ->

                                podcastPlayerViewModelCoordinator.submitResponse(
                                    response = Response.Success(
                                        ResponseHolder(
                                            requestHolder,
                                            PodcastPlayerResponse(
                                                podcastClip.toJson(moshi)
                                            )
                                        )
                                    ),
                                    navigateBack = BackType.CloseDetailScreen
                                )
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        }
    }

    private fun updateFeedContentInBackground() {
        viewModelScope.launch(io) {
            val chat = chatRepository.getChatById(args.chatId).firstOrNull()
            val podcast = getPodcastFeed()
            val chatHost = chat?.host ?: ChatHost(Feed.TRIBES_DEFAULT_SERVER_URL)
            val subscribed = podcast?.subscribed?.isTrue() == true

            args.argFeedUrl.toFeedUrl()?.let { feedUrl ->
                feedRepository.updateFeedContent(
                    chatId = chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    host = chatHost,
                    feedUrl = feedUrl,
                    chatUUID = chat?.uuid,
                    subscribed = subscribed.toSubscribed(),
                    currentItemId = null
                )
            }
        }
    }

    private fun podcastLoaded(podcast: Podcast) {
        viewModelScope.launch(mainImmediate) {

            val playingContent = mediaPlayerServiceController.getPlayingContent()
            podcast.applyPlayingContentState(playingContent)

            viewStateContainer.updateViewState(
                PodcastPlayerViewState.PodcastLoaded(
                    podcast,
                    args.fromDownloaded
                )
            )

            val contentFeedStatus = podcast.getUpdatedContentFeedStatus()

            mediaPlayerServiceController.submitAction(
                UserAction.AdjustSatsPerMinute(
                    args.chatId,
                    contentFeedStatus
                )
            )
        }
    }

    fun updateSatsPerMinute(sats: Long) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { nnPodcast ->

                nnPodcast.didChangeSatsPerMinute(sats)

                viewModelScope.launch(mainImmediate) {
                    mediaPlayerServiceController.submitAction(
                        UserAction.AdjustSatsPerMinute(
                            args.chatId,
                            nnPodcast.getUpdatedContentFeedStatus(
                                Sat(sats)
                            )
                        )
                    )
                }

            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerServiceController.removeListener(this)
    }

    private var toggleSubscriptionJob: Job? = null
    fun toggleSubscribeState() {
        if (toggleSubscriptionJob?.isActive == true) {
            return
        }
        toggleSubscriptionJob = viewModelScope.launch(mainImmediate) {
            podcastFlow.firstOrNull()?.let { podcast ->
                feedRepository.toggleFeedSubscribeState(
                    podcast.id,
                    podcast.subscribed
                )
            }
        }
    }

    fun playEpisodeFromList(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                podcast.getEpisodeWithId(episode.id.value)?.let { episode ->
                    if (mediaPlayerServiceController.getPlayingContent()?.second == episode.id.value) {
                        pauseEpisode(episode)
                    } else {
                        viewStateContainer.updateViewState(PodcastPlayerViewState.LoadingEpisode(episode))
                        delay(50L)
                        playEpisode(episode)
                    }
                }
            }
        }
    }

    fun togglePlayState() {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                val episode = podcast.getCurrentEpisode()

                if (episode.playing) {
                    pauseEpisode(episode)
                } else {
                    playEpisode(episode)
                }
            }
        }
    }

    private fun playEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                podcast.willStartPlayingEpisode(
                    episode,
                    episode.currentTimeMilliseconds ?: 0,
                    ::retrieveEpisodeDuration
                )

                viewStateContainer.updateViewState(
                    PodcastPlayerViewState.EpisodePlayed(
                        podcast,
                        args.fromDownloaded
                    )
                )

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Play(
                        args.chatId,
                        episode.episodeUrl,
                        podcast.getUpdatedContentFeedStatus(),
                        podcast.getUpdatedContentEpisodeStatus()
                    )
                )
            }
        }
    }

    private fun pauseEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                podcast.didPausePlayingEpisode(episode)

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Pause(
                        args.chatId,
                        episode.id.value
                    )
                )
            }
        }
    }

    fun seekTo(timeMilliseconds: Long) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                podcast.didSeekTo(timeMilliseconds)

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Seek(
                        args.chatId,
                        podcast.getUpdatedContentEpisodeStatus()
                    )
                )
            }
        }
    }

    fun adjustSpeed(speed: Double) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                podcast.speed = speed

                val contentFeedStatus = podcast.getUpdatedContentFeedStatus()

                mediaPlayerServiceController.submitAction(
                    UserAction.AdjustSpeed(
                        args.chatId,
                        contentFeedStatus
                    )
                )
            }
        }
    }

    private fun setPaymentsDestinations() {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                val destinations = podcast.getFeedDestinations()

                if (destinations.isNotEmpty()) {
                    mediaPlayerServiceController.submitAction(
                        UserAction.SetPaymentsDestinations(
                            args.chatId,
                            destinations
                        )
                    )
                }
            }
        }
    }

    fun sendPodcastBoost(
        amount: Sat,
        fireworksCallback: () -> Unit
    ) {
        viewModelScope.launch(mainImmediate) {
            getPodcastFeed()?.let { podcast ->
                getAccountBalance().firstOrNull()?.let { balance ->
                    when {
                        (amount.value > balance.balance.value) -> {
                            submitSideEffect(
                                PodcastPlayerSideEffect.Notify(
                                    app.getString(R.string.balance_too_low)
                                )
                            )
                        }
                        (amount.value <= 0) -> {
                            submitSideEffect(
                                PodcastPlayerSideEffect.Notify(
                                    app.getString(R.string.boost_amount_too_low)
                                )
                            )
                        }
                        else -> {
                            fireworksCallback()

                            messageRepository.sendBoost(
                                args.chatId,
                                FeedBoost(
                                    feedId = podcast.id,
                                    itemId = podcast.getUpdatedContentFeedStatus().itemId ?: podcast.getCurrentEpisode().id,
                                    timeSeconds = podcast.getUpdatedContentEpisodeStatus().currentTime.value.toInt(),
                                    amount = amount
                                )
                            )

                            actionsRepository.trackFeedBoostAction(
                                amount.value,
                                podcast.getCurrentEpisode().id,
                                arrayListOf("")
                            )

                            if (podcast.hasDestinations) {
                                mediaPlayerServiceController.submitAction(
                                    UserAction.SendBoost(
                                        args.chatId,
                                        podcast.id.value,
                                        podcast.getUpdatedContentFeedStatus(amount),
                                        podcast.getUpdatedContentEpisodeStatus(),
                                        podcast.getFeedDestinations()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun trackPodcastConsumed() {
        viewModelScope.launch(mainImmediate) {
            mediaPlayerServiceController.submitAction(
                UserAction.TrackPodcastConsumed(
                    args.chatId
                )
            )
        }
    }

    fun retrieveEpisodeDuration(
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

    fun downloadMedia(
        podcastEpisode: PodcastEpisode,
        downloadCompleteCallback: (downloadedFile: File) -> Unit
    ) {
        repositoryMedia.downloadMediaIfApplicable(
            podcastEpisode,
            downloadCompleteCallback
        )
        getDeleteExcessFileIfApplicable()
    }

    fun downloadMediaByItemId(feedId: FeedId) {
        viewModelScope.launch(mainImmediate) {
            feedRepository.getFeedItemById(feedId).firstOrNull()?.let { feedItem ->
                repositoryMedia.downloadMediaIfApplicable(feedItem) { localFile ->
                    
                    _feedItemDetailStateFlow.value = _feedItemDetailStateFlow.value?.copy(
                        downloaded = true,
                        isDownloadInProgress = false
                    )

                    getPodcastFeed()?.let { nnPodcast ->
                        nnPodcast.getEpisodeWithId(feedId.value)?.let { episode ->
                            episode.localFile = localFile
                        }
                    }

                    if (feedItemDetailsViewStateContainer.value is FeedItemDetailsViewState.Open) {
                        feedItemDetailsViewStateContainer.updateViewState(
                            FeedItemDetailsViewState.Open(feedItemDetailStateFlow.value)
                        )
                    }

                    forceListReload()
                }
            }

            val isFeedItemDownloadInProgress = repositoryMedia.inProgressDownloadIds()
                .contains(feedId)

            _feedItemDetailStateFlow.value = _feedItemDetailStateFlow.value?.copy(
                isDownloadInProgress = isFeedItemDownloadInProgress
            )

            feedItemDetailsViewStateContainer.updateViewState(
                FeedItemDetailsViewState.Open(feedItemDetailStateFlow.value)
            )

            forceListReload()
            getDeleteExcessFileIfApplicable()
        }
    }

    fun deleteDownloadedMediaByItemId(feedId: FeedId) {
        viewModelScope.launch(mainImmediate) {
            feedRepository.getFeedItemById(feedId).firstOrNull()?.let { feedItem ->

                if (repositoryMedia.deleteDownloadedMediaIfApplicable(feedItem)) {
                    _feedItemDetailStateFlow.value = _feedItemDetailStateFlow.value?.copy(downloaded = false)

                    getPodcastFeed()?.let { nnPodcast ->
                        nnPodcast.getEpisodeWithId(feedId.value)?.let { episode ->
                            episode.localFile = null
                        }
                    }
                }

                feedItemDetailsViewStateContainer.updateViewState(
                    FeedItemDetailsViewState.Open(feedItemDetailStateFlow.value)
                )

                forceListReload()
            }
        }
    }

    private val storageLimitSharedPreferences: SharedPreferences = app.applicationContext.getSharedPreferences(STORAGE_LIMIT_KEY, Context.MODE_PRIVATE)

    private fun getDeleteExcessFileIfApplicable(){
        viewModelScope.launch(mainImmediate) {
            storageData?.let { nnStorageData ->
                val storageLimitProgress = storageLimitSharedPreferences.getInt(STORAGE_LIMIT_KEY, DEFAULT_STORAGE_LIMIT)
                val userLimit = nnStorageData.freeStorage?.value?.let { calculateUserStorageLimit(freeStorage = it, seekBarValue = storageLimitProgress ) } ?: 0L
                val usageStorage = nnStorageData.usedStorage.value
                val excessSize = (usageStorage - userLimit)
                repositoryMedia.deleteExcessFilesOnBackground(excessSize)
            }
        }
    }

    private fun getStorageData(){
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getStorageDataInfo().collect { storageDataInfo ->
                val totalStorage = getTotalStorage()
                val usedStorage = storageDataInfo.usedStorage
                val freeStorage = (totalStorage - usedStorage.value).toFileSize()
                val modifiedStorageDataInfo = storageDataInfo.copy(freeStorage = freeStorage)
                storageData = modifiedStorageDataInfo
            }
        }
    }

    private fun getTotalStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }


    fun showOptionsFor(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            val duration = episode.getUpdatedContentEpisodeStatus().duration.value.toInt().toHrAndMin()
            val played = getPlayedMark(episode.id)

            _feedItemDetailStateFlow.value = FeedItemDetail(
                episode.id,
                episode.titleToShow,
                episode.image?.value ?: "",
                R.drawable.ic_podcast_type,
                "Podcast",
                episode.dateString,
                duration,
                episode.downloaded,
                isFeedItemDownloadInProgress(episode.id),
                episode.episodeUrl,
                played,
                podcast?.title?.value
            )

            feedItemDetailsViewStateContainer.updateViewState(
                FeedItemDetailsViewState.Open(feedItemDetailStateFlow.value)
            )
        }
    }

    fun share(itemId: FeedId, context: Context) {
        viewModelScope.launch(mainImmediate) {
            val link = generateSphinxFeedItemLink(itemId) ?: ""
            val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, link)
            }

            context.startActivity(
                Intent.createChooser(
                    sharingIntent,
                    app.getString(R.string.episode_detail_share_link)
                )
            )
        }
    }

    fun copyCodeToClipboard(itemId: FeedId) {
        (app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
            viewModelScope.launch(mainImmediate) {
                val link = generateSphinxFeedItemLink(itemId) ?: ""
                val clipData = ClipData.newPlainText("text", link)
                manager.setPrimaryClip(clipData)

                submitSideEffect(
                    PodcastPlayerSideEffect.Notify(
                        app.getString(R.string.episode_detail_clipboard)
                    )
                )
            }
        }
    }

    private suspend fun generateSphinxFeedItemLink(itemId: FeedId): String? {
        val shareAtTime = suspendCoroutine<Boolean> { continuation ->
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(PodcastPlayerSideEffect.CopyLinkSelection(viewModelScope) { result ->
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

    private suspend fun getPlayedMark(feedItemId: FeedId): Boolean {
       return feedRepository.getPlayedMark(feedItemId).firstOrNull() ?: false
    }

    fun updatePlayedMark() {
        feedItemDetailStateFlow.value?.feedId?.let{ itemId ->

            val played = feedItemDetailStateFlow.value?.played ?: false
            feedRepository.updatePlayedMark(itemId, !played)

            _feedItemDetailStateFlow.value = _feedItemDetailStateFlow.value?.copy(
                played = !played,
            )

            getPodcastFeed()?.let { nnPodcast ->
                nnPodcast.getEpisodeWithId(itemId.value)?.let { episode ->
                    episode.played = !played
                }
            }

            forceListReload()

            feedItemDetailsViewStateContainer.updateViewState(
                FeedItemDetailsViewState.Open(feedItemDetailStateFlow.value)
            )
        }
    }

    fun isFeedItemDownloadInProgress(feedItemId: FeedId): Boolean {
        return repositoryMedia.inProgressDownloadIds().contains(feedItemId)
    }

    fun isEpisodeSoundPlaying(episode: PodcastEpisode): Boolean {
        return episode.playing && mediaPlayerServiceController.getPlayingContent()?.third == true
    }

    fun forceFeedReload() {
        viewModelScope.launch(io) {
            getPodcastFeed()?.let { podcast ->
                feedRepository.toggleFeedSubscribeState(
                    podcast.id,
                    if (podcast.subscribed.isTrue()) Subscribed.False else Subscribed.True,
                )
            }
        }
    }

    private fun forceListReload() {
        when (val viewState = viewStateContainer.viewStateFlow.value) {
            is PodcastPlayerViewState.PodcastLoaded -> {
                viewState.podcast.forceUpdate = !viewState.podcast.forceUpdate

                viewStateContainer.updateViewState(
                    PodcastPlayerViewState.PodcastLoaded(
                        viewState.podcast,
                        args.fromDownloaded
                    )
                )
                Log.d("isAudioComingOut", "UpdateState! PodcastLoaded")
            }
            is PodcastPlayerViewState.EpisodePlayed -> {
                viewState.podcast.forceUpdate = !viewState.podcast.forceUpdate

                viewStateContainer.updateViewState(
                    PodcastPlayerViewState.EpisodePlayed(
                        viewState.podcast,
                        args.fromDownloaded
                    )
                )
                Log.d("isAudioComingOut", "UpdateState! EpisodePlayed ")
            }
            is PodcastPlayerViewState.MediaStateUpdate -> {
                viewState.podcast.forceUpdate = !viewState.podcast.forceUpdate

                viewStateContainer.updateViewState(
                    PodcastPlayerViewState.MediaStateUpdate(
                        viewState.podcast,
                        args.fromDownloaded,
                        viewState.state
                    )
                )
                Log.d("isAudioComingOut", "UpdateState! MediaStateUpdate")
            }
            else -> {}
        }
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
