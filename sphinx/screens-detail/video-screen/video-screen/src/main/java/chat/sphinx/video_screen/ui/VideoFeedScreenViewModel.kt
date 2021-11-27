package chat.sphinx.video_screen.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.video_screen.ui.viewstate.SelectedVideoViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal open class VideoFeedScreenViewModel(
    dispatchers: CoroutineDispatchers,
    private val chatRepository: ChatRepository,
): BaseViewModel<VideoFeedScreenViewState>(dispatchers, VideoFeedScreenViewState.Idle)
{
    private val videoFeedSharedFlow: SharedFlow<Feed?> = flow {
        emitAll(chatRepository.getFeedByChatId(getArgChatId()))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    open val selectedVideoStateContainer: ViewStateContainer<SelectedVideoViewState> by lazy {
        ViewStateContainer(SelectedVideoViewState.Idle)
    }

    protected fun subscribeToViewStateFlow() {
        viewModelScope.launch(mainImmediate) {
            videoFeedSharedFlow.collect { feed ->
                feed?.let { nnFeed ->
                    updateViewState(
                        VideoFeedScreenViewState.FeedLoaded(
                            nnFeed.title,
                            nnFeed.imageUrlToShow,
                            nnFeed.items
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
                        chatRepository.updateFeedContent(
                            chatId = chat.id,
                            host = chatHost,
                            feedUrl = feedUrl,
                            chatUUID = chat.uuid,
                            true.toSubscribed(),
                            currentEpisodeId = null
                        )
                    }
                }
            }
        }
    }

    fun videoItemSelected(video: FeedItem) {
        selectedVideoStateContainer.updateViewState(
            SelectedVideoViewState.VideoSelected(
                video.id,
                video.title,
                video.description,
                video.enclosureUrl,
                video.dateUpdated,
                video.duration
            )
        )
    }

    open fun getArgChatId(): ChatId {
        return ChatId(ChatId.NULL_CHAT_ID.toLong())
    }

    open fun getArgFeedUrl(): FeedUrl? {
        return null
    }
}