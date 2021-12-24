package chat.sphinx.video_screen.ui.detail

import androidx.lifecycle.SavedStateHandle
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.video_screen.ui.VideoFeedScreenViewModel
import chat.sphinx.video_screen.ui.watch.VideoFeedWatchScreenFragmentArgs
import chat.sphinx.video_screen.ui.watch.chatId
import chat.sphinx.video_screen.ui.watch.feedId
import chat.sphinx.video_screen.ui.watch.feedUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

internal inline val VideoFeedDetailScreenFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

internal inline val VideoFeedDetailScreenFragmentArgs.feedUrl: FeedUrl?
    get() = FeedUrl(argFeedUrl)

internal inline val VideoFeedDetailScreenFragmentArgs.feedId: FeedId?
    get() = FeedId(argFeedId)

@HiltViewModel
internal class VideoFeedDetailScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    chatRepository: ChatRepository,
    repositoryMedia: RepositoryMedia,
    feedRepository: FeedRepository,
): VideoFeedScreenViewModel(
    dispatchers,
    chatRepository,
    repositoryMedia,
    feedRepository
)
{
    private val args: VideoFeedDetailScreenFragmentArgs by savedStateHandle.navArgs()

    init {
        subscribeToViewStateFlow()
    }

    override fun getArgChatId(): ChatId {
        return args.chatId
    }

    override fun getArgFeedUrl(): FeedUrl? {
        return args.feedUrl
    }

    override fun getArgFeedId(): FeedId? {
        return args.feedId
    }
}
