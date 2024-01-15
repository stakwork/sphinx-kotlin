package chat.sphinx.video_screen.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_feed_status.NetworkQueryFeedStatus
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.navigation.VideoScreenNavigator
import chat.sphinx.video_screen.ui.viewstate.BoostAnimationViewState
import chat.sphinx.video_screen.ui.viewstate.SelectedVideoViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedItemDetailsViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.video_screen.ui.viewstate.VideoPlayerViewState
import chat.sphinx.wrapper_action_track.action_wrappers.VideoRecordConsumed
import chat.sphinx.wrapper_action_track.action_wrappers.VideoStreamSatsTimer
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.FeedBoost
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal open class VideoFeedScreenViewModel(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    private val chatRepository: ChatRepository,
    private val repositoryMedia: RepositoryMedia,
    private val feedRepository: FeedRepository,
    private val actionsRepository: ActionsRepository,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val lightningRepository: LightningRepository,
    private val networkQueryFeedStatus: NetworkQueryFeedStatus,
    val navigator: VideoScreenNavigator
): SideEffectViewModel<
    FragmentActivity,
    VideoFeedScreenSideEffect,
    VideoFeedScreenViewState
    >(dispatchers, VideoFeedScreenViewState.Idle)
{
    private suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        lightningRepository.getAccountBalance()


    private val videoFeedSharedFlow: SharedFlow<Feed?> = flow {
        getArgFeedId()?.let { feedId ->
            emitAll(feedRepository.getFeedById(feedId))
        } ?: run {
            emitAll(feedRepository.getFeedByChatId(getArgChatId()))
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private var videoRecordConsumed: VideoRecordConsumed? = null
    private var videoStreamSatsTimer: VideoStreamSatsTimer? = null

    suspend fun getOwner(): Contact {
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

    private suspend fun getVideoFeed(): Feed? {
        videoFeedSharedFlow.replayCache.firstOrNull()?.let { feed ->
            return feed
        }

        videoFeedSharedFlow.firstOrNull()?.let { feed ->
            return feed
        }

        var feed: Feed? = null

        try {
            videoFeedSharedFlow.collect {
                if (it != null) {
                    feed = it
                    throw Exception()
                }
            }
        } catch (e: Exception) {}
        delay(25L)
        return feed
    }

    open val selectedVideoStateContainer: ViewStateContainer<SelectedVideoViewState> by lazy {
        ViewStateContainer(SelectedVideoViewState.Idle)
    }

    open val videoPlayerStateContainer: ViewStateContainer<VideoPlayerViewState> by lazy {
        ViewStateContainer(VideoPlayerViewState.Idle)
    }

    open val boostAnimationViewStateContainer: ViewStateContainer<BoostAnimationViewState> by lazy {
        ViewStateContainer(BoostAnimationViewState.Idle)
    }

    private val _satsPerMinuteStateFlow: MutableStateFlow<Sat?> by lazy {
        MutableStateFlow(Sat(0))
    }

    private val satsPerMinuteStateFlow: StateFlow<Sat?>
        get() = _satsPerMinuteStateFlow.asStateFlow()

    private val _feedItemDetailStateFlow: MutableStateFlow<FeedItemDetail?> by lazy {
        MutableStateFlow(null)
    }

    private val feedItemDetailStateFlow: StateFlow<FeedItemDetail?>
        get() = _feedItemDetailStateFlow.asStateFlow()


    val videoFeedItemDetailsViewState: ViewStateContainer<VideoFeedItemDetailsViewState> by lazy {
        ViewStateContainer(VideoFeedItemDetailsViewState.Closed)
    }


    protected fun subscribeToViewStateFlow() {
        viewModelScope.launch(mainImmediate) {
            videoFeedSharedFlow.collect { feed ->
                feed?.let { nnFeed ->

                    val satsPerMinute = feed.contentFeedStatus?.satsPerMinute?.value ?: feed.model?.suggestedSats
                    _satsPerMinuteStateFlow.value = satsPerMinute?.let { Sat(it) }

                    updateViewState(
                        VideoFeedScreenViewState.FeedLoaded(
                            nnFeed.title,
                            nnFeed.imageUrlToShow,
                            nnFeed.chatId,
                            nnFeed.subscribed,
                            nnFeed.items,
                            nnFeed.hasDestinations,
                            satsPerMinuteStateFlow.value,
                        )
                    )

                    if (selectedVideoStateContainer.value is SelectedVideoViewState.Idle) {
                        nnFeed.items.firstOrNull()?.let { video ->
                            selectedVideoStateContainer.updateViewState(
                                SelectedVideoViewState.VideoSelected(
                                    video.id,
                                    nnFeed.id,
                                    video.title,
                                    video.description,
                                    video.enclosureUrl,
                                    video.localFile,
                                    video.dateUpdated,
                                    video.duration
                                )
                            )
                        }
                    }
                }
            }
        }

        updateFeedContentInBackground()
    }

    private fun updateFeedContentInBackground() {
        viewModelScope.launch(io) {
            val chat = chatRepository.getChatById(getArgChatId()).firstOrNull()
            val videoFeed = getVideoFeed()
            val chatHost = chat?.host ?: ChatHost(Feed.TRIBES_DEFAULT_SERVER_URL)
            val subscribed = videoFeed?.subscribed?.isTrue() == true

            getArgFeedUrl()?.let { feedUrl ->
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

    fun videoItemSelected(video: FeedItem) {
        viewModelScope.launch(mainImmediate) {
            video.feed?.let { feed ->
                feedRepository.updateContentFeedStatus(
                    feedId = feed.id,
                    feedUrl = feed.feedUrl,
                    subscriptionStatus = feed.subscribed,
                    chatId = feed.chatId,
                    itemId = video.id,
                    satsPerMinute = satsPerMinuteStateFlow.value,
                    playerSpeed = null
                )
            }

            selectedVideoStateContainer.updateViewState(
                SelectedVideoViewState.VideoSelected(
                    video.id,
                    video.feed?.id,
                    video.title,
                    video.description,
                    video.enclosureUrl,
                    video.localFile,
                    video.dateUpdated,
                    video.duration
                )
            )
        }
    }

    fun updateVideoLastPlayed() {
        (selectedVideoStateContainer.value as? SelectedVideoViewState.VideoSelected)?.let {
            it.feedId?.let { feedId ->
                feedRepository.updateLastPlayed(feedId)
            }
        }
    }

    fun toggleSubscribeState() {
        viewModelScope.launch(mainImmediate) {
            getVideoFeed()?.let { feed ->
                feedRepository.toggleFeedSubscribeState(
                    feed.id,
                    feed.subscribed
                )
            }
        }
    }

    fun updateSatsPerMinute(sats: Long) {
        viewModelScope.launch(mainImmediate) {
            (selectedVideoStateContainer.value as? SelectedVideoViewState.VideoSelected)?.let { video ->
                getVideoFeed()?.let { feed ->
                    feedRepository.updateContentFeedStatus(
                        feed.id,
                        feed.feedUrl,
                        feed.subscribed,
                        feed.chatId,
                        video.id,
                        Sat(sats),
                        null,
                        true
                    )
                }
            }
            _satsPerMinuteStateFlow.value = Sat(sats)
        }
    }

    fun sendBoost(
        amount: Sat,
        fireworksCallback: () -> Unit
    ) {
        viewModelScope.launch(mainImmediate) {
            getVideoFeed()?.let { videoFeed ->
                videoFeed.lastItem?.let { currentItem ->
                    getAccountBalance().firstOrNull()?.let { balance ->
                        when {
                            (amount.value > balance.balance.value) -> {
                                submitSideEffect(
                                    VideoFeedScreenSideEffect.Notify(
                                        app.getString(R.string.balance_too_low)
                                    )
                                )
                            }
                            (amount.value <= 0) -> {
                                submitSideEffect(
                                    VideoFeedScreenSideEffect.Notify(
                                        app.getString(R.string.boost_amount_too_low)
                                    )
                                )
                            }
                            else -> {
                                fireworksCallback()

                                val chatId = getArgChatId()

                                messageRepository.sendBoost(
                                    chatId,
                                    FeedBoost(
                                        feedId = videoFeed.id,
                                        itemId = currentItem.id,
                                        timeSeconds = 0,
                                        amount = amount
                                    )
                                )

                                actionsRepository.trackFeedBoostAction(
                                    amount.value,
                                    currentItem.id,
                                    arrayListOf("")
                                )

                                videoFeed.destinations.let { destinations ->

                                    feedRepository.streamFeedPayments(
                                        chatId,
                                        videoFeed.id.value,
                                        currentItem.id.value,
                                        0,
                                        amount,
                                        FeedPlayerSpeed(1.0),
                                        destinations
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun showOptionsFor(video: FeedItem) {
        viewModelScope.launch(mainImmediate) {
            _feedItemDetailStateFlow.value = FeedItemDetail(
                video.id,
                video.titleToShow,
                video.thumbnailUrlToShow?.value ?: "",
                R.drawable.ic_youtube_type,
                "Youtube",
                video.datePublished?.hhmmElseDate() ?: "",
                video.duration?.value?.toInt().toString(),
                null,
                false,
                video.link?.value,
                false,
                null

            )

            videoFeedItemDetailsViewState.updateViewState(
                VideoFeedItemDetailsViewState.Open(feedItemDetailStateFlow.value)
            )
        }
    }

    fun share(link: String, context: Context) {
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

    fun copyCodeToClipboard(link: String) {
        (app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
            val clipData = ClipData.newPlainText("text", link)
            manager.setPrimaryClip(clipData)

            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    VideoFeedScreenSideEffect.Notify((app.getString(R.string.episode_detail_clipboard))
                    )
                )
            }
        }
    }

    open fun getArgChatId(): ChatId {
        return ChatId(ChatId.NULL_CHAT_ID.toLong())
    }

    open fun getArgFeedUrl(): FeedUrl? {
        return null
    }

    open fun getArgFeedId(): FeedId? {
        return null
    }

    private fun streamSatsPayments() {
        viewModelScope.launch(mainImmediate) {
            videoFeedSharedFlow.firstOrNull()?.let { feed ->
                (selectedVideoStateContainer.value as? SelectedVideoViewState.VideoSelected)?.let { videoState ->
                    feedRepository.streamFeedPayments(
                        chatId = ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        feedId = videoState.feedId?.value ?: "",
                        feedItemId = videoState.id.value,
                        currentTime = 0,
                        satsPerMinute = satsPerMinuteStateFlow.value,
                        playerSpeed = null,
                        destinations = feed.destinations
                    )
                }
            }
        }
    }

    fun createVideoRecordConsumed(feedItemId: FeedId){
        if (videoRecordConsumed?.feedItemId == feedItemId){
            return
        }
        videoRecordConsumed = VideoRecordConsumed(feedItemId)
        videoStreamSatsTimer = VideoStreamSatsTimer {
            streamSatsPayments()
        }
    }

    fun trackVideoConsumed(){
        videoRecordConsumed?.let { record ->
            if (record.history.isNotEmpty()) {
                actionsRepository.trackMediaContentConsumed(
                    record.feedItemId,
                    record.history
                )
            }
        }
    }

    fun setNewHistoryItem(videoPosition: Long){
        videoRecordConsumed?.setNewHistoryItem(videoPosition)
    }

    fun startTimer(isSeeking: Boolean) {
        videoRecordConsumed?.startTimer()
        if (!isSeeking) {
            videoStreamSatsTimer?.startTimer()
        }
    }

    fun createHistoryItem() {
        videoRecordConsumed?.createHistoryItem()
    }

    fun stopTimer(){
        videoRecordConsumed?.stopTimer()
        videoStreamSatsTimer?.stopTimer()
    }

    fun checkYoutubeVideoAvailable(videoId: FeedId){
        viewModelScope.launch(mainImmediate) {
            val url = networkQueryFeedStatus.checkYoutubeVideoAvailable(videoId.youtubeVideoId())

            if (url != null) {
                videoPlayerStateContainer.updateViewState(VideoPlayerViewState.WebViewPlayer(url.toUri(), null))
            } else {
                videoPlayerStateContainer.updateViewState(VideoPlayerViewState.YoutubeVideoIframe(videoId))
            }
        }
    }
}