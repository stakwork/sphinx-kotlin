package chat.sphinx.video_screen.ui

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.ui.viewstate.BoostAnimationViewState
import chat.sphinx.video_screen.ui.viewstate.SelectedVideoViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.wrapper_action_track.action_wrappers.VideoRecordConsumed
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
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
import java.io.File

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

    open val boostAnimationViewStateContainer: ViewStateContainer<BoostAnimationViewState> by lazy {
        ViewStateContainer(BoostAnimationViewState.Idle)
    }

    protected fun subscribeToViewStateFlow() {
        viewModelScope.launch(mainImmediate) {
            videoFeedSharedFlow.collect { feed ->
                feed?.let { nnFeed ->
                    updateViewState(
                        VideoFeedScreenViewState.FeedLoaded(
                            nnFeed.title,
                            nnFeed.imageUrlToShow,
                            nnFeed.chatId,
                            nnFeed.subscribed,
                            nnFeed.items,
                            nnFeed.hasDestinations
                        )
                    )

                    if (selectedVideoStateContainer.value is SelectedVideoViewState.Idle) {
                        nnFeed.items.firstOrNull()?.let { video ->
                            selectedVideoStateContainer.updateViewState(
                                SelectedVideoViewState.VideoSelected(
                                    video.id,
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
            chatRepository.getChatById(getArgChatId()).firstOrNull()?.let { chat ->
                chat.host?.let { chatHost ->
                    getArgFeedUrl()?.let { feedUrl ->
                        val subscribed = (chat != null || (getVideoFeed()?.subscribed?.isTrue() == true))

                        feedRepository.updateFeedContent(
                            chatId = chat.id,
                            host = chatHost,
                            feedUrl = feedUrl,
                            chatUUID = chat.uuid,
                            subscribed = subscribed.toSubscribed(),
                            currentEpisodeId = null
                        )
                    }
                }
            }
        }
    }

    fun videoItemSelected(video: FeedItem) {
        viewModelScope.launch(mainImmediate) {

            repositoryMedia.updateChatMetaData(
                getArgChatId(),
                video.feed?.id,
                ChatMetaData(
                    itemId = video.id,
                    itemLongId = ItemId(-1),
                    satsPerMinute = getOwner()?.tipAmount ?: Sat(0),
                    timeSeconds = 0,
                    speed = 1.0
                )
            )

            selectedVideoStateContainer.updateViewState(
                SelectedVideoViewState.VideoSelected(
                    video.id,
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

                                    repositoryMedia.streamFeedPayments(
                                        chatId,
                                        ChatMetaData(
                                            itemId = currentItem.id,
                                            itemLongId = ItemId(-1),
                                            satsPerMinute = amount,
                                            timeSeconds = 0,
                                            speed = 1.0
                                        ),
                                        videoFeed.id.value,
                                        currentItem.id.value,
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

    open fun getArgChatId(): ChatId {
        return ChatId(ChatId.NULL_CHAT_ID.toLong())
    }

    open fun getArgFeedUrl(): FeedUrl? {
        return null
    }

    open fun getArgFeedId(): FeedId? {
        return null
    }

    fun downloadMedia(
        feedItem: FeedItem,
        downloadCompleteCallback: (downloadedFile: File) -> Unit
    ) {
        repositoryMedia.downloadMediaIfApplicable(
            feedItem,
            downloadCompleteCallback
        )
    }

    suspend fun deleteDownloadedMedia(feedItem: FeedItem) {
        if (repositoryMedia.deleteDownloadedMediaIfApplicable(feedItem)) {
            feedItem.localFile = null
        }
    }

    fun isFeedItemDownloadInProgress(feedItemId: FeedId): Boolean {
        return repositoryMedia.inProgressDownloadIds().contains(feedItemId)
    }

    fun createVideoRecordConsumed(feedItemId: FeedId){
        if (videoRecordConsumed?.feedItemId == feedItemId){
            return
        }
        videoRecordConsumed = VideoRecordConsumed(feedItemId)
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

    fun startTimer() {
        videoRecordConsumed?.startTimer()
    }

    fun createHistoryItem() {
        videoRecordConsumed?.createHistoryItem()
    }

    fun stopTimer(){
        videoRecordConsumed?.stopTimer()
    }


}