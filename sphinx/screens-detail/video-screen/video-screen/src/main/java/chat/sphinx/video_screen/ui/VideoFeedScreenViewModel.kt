package chat.sphinx.video_screen.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.video_screen.ui.viewstate.BoostAnimationViewState
import chat.sphinx.video_screen.ui.viewstate.SelectedVideoViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.DashboardItemType
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_message.FeedBoost
import chat.sphinx.wrapper_podcast.Podcast
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
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
    private val chatRepository: ChatRepository,
    private val repositoryMedia: RepositoryMedia,
    private val feedRepository: FeedRepository,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
): BaseViewModel<VideoFeedScreenViewState>(dispatchers, VideoFeedScreenViewState.Idle)
{
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
        viewModelScope.launch(mainImmediate) {
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

    fun sendBoost(customAmount: Sat?) {
        viewModelScope.launch(mainImmediate) {
            (customAmount ?: getOwner().tipAmount)?.let { amount ->
                getVideoFeed()?.let { videoFeed ->
                    videoFeed.lastItem?.let { currentItem ->
                        if (amount.value > 0) {

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
}