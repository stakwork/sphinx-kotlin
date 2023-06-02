package chat.sphinx.delete_chat_media.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.delete_chat_media.navigation.DeleteChatMediaNavigator
import chat.sphinx.delete_chat_media.viewstate.DeleteChatMediaViewState
import chat.sphinx.delete_chat_media.viewstate.DeleteChatNotificationViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_message_media.MessageMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class DeleteChatMediaViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteChatMediaNavigator,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteNotifySideEffect,
        DeleteChatMediaViewState
        >(dispatchers, DeleteChatMediaViewState.Loading)
{
     val deleteAllFeedsNotificationViewStateContainer: ViewStateContainer<DeleteChatNotificationViewState> by lazy {
        ViewStateContainer(DeleteChatNotificationViewState.Closed)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getAllDownloadedMedia().collect { chatItems ->
                val chatIdAndFileList =  getLocalFilesGroupedByChatId(chatItems)

            }
        }
    }

    private fun getLocalFilesGroupedByChatId(chatItems: List<MessageMedia>): Map<ChatId?, List<File>> {
        return chatItems.groupBy({ it.chatId }, { it.localFile as File })
    }

}
