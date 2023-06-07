package chat.sphinx.example.delete_chat_media_detail.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.delete.chat.media.detail.R
import chat.sphinx.example.delete_chat_media_detail.model.ChatFile
import chat.sphinx.example.delete_chat_media_detail.navigation.DeleteChatMediaDetailNavigator
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatDetailNotificationViewState
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatMediaDetailViewState
import chat.sphinx.example.delete_chat_media_detail.viewstate.HeaderSelectionModeViewState
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.calculateLongSize
import chat.sphinx.wrapper_common.calculateSize
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.toChatId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.toFileSize
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.MessageMedia
import chat.sphinx.wrapper_message_media.isAudio
import chat.sphinx.wrapper_message_media.isImage
import chat.sphinx.wrapper_message_media.isPdf
import chat.sphinx.wrapper_message_media.isVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class DeleteChatMediaDetailViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteChatMediaDetailNavigator,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    private val repositoryMedia: RepositoryMedia,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteNotifySideEffect,
        DeleteChatMediaDetailViewState
        >(dispatchers, DeleteChatMediaDetailViewState.Loading)
{
    private val args: DeleteChatMediaDetailFragmentArgs by savedStateHandle.navArgs()
    private var itemsTotalSize: FileSize = FileSize(0)
    private var currentChatIdAndFiles: Pair<ChatId, List<File>>? = null


    val deleteChatNotificationViewStateContainer: ViewStateContainer<DeleteChatDetailNotificationViewState> by lazy {
        ViewStateContainer(DeleteChatDetailNotificationViewState.Closed)
    }

    val headerSelectionModeViewStateContainer: ViewStateContainer<HeaderSelectionModeViewState> by lazy {
        ViewStateContainer(HeaderSelectionModeViewState.Off)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getAllDownloadedMediaByChatId(ChatId(args.argChatId)).collect { chatItems ->
                val totalSizeChats = chatItems.sumOf { it.localFile?.length() ?: 0 }.toFileSize()
                setItemTotalFile(totalSizeChats?.value ?: 0L )
                currentChatIdAndFiles = Pair(ChatId(args.argChatId), chatItems.mapNotNull { it.localFile })

                val fileList = chatItems.map { ChatFile(
                    it.fileName?.value,
                    getMediaType(it.mediaType),
                    it.messageId,
                    it.chatId,
                    FileSize(it.localFile?.length() ?: 0L).calculateSize()
                )}

                viewStateContainer.updateViewState(DeleteChatMediaDetailViewState.FileList(fileList, totalSizeChats?.calculateSize()))
            }
        }
    }

    fun deleteAllChatFiles() {
        deleteChatNotificationViewStateContainer.updateViewState(DeleteChatDetailNotificationViewState.Deleting)
        viewModelScope.launch(mainImmediate) {
            currentChatIdAndFiles?.let { chatIdAndFiles ->
                if (repositoryMedia.deleteDownloadedMediaByChatId(chatIdAndFiles.first, chatIdAndFiles.second)) {
                    deleteChatNotificationViewStateContainer.updateViewState(
                        DeleteChatDetailNotificationViewState.SuccessfullyDeleted(itemsTotalSize.calculateSize())
                    )
                } else {
                    deleteChatNotificationViewStateContainer.updateViewState(
                        DeleteChatDetailNotificationViewState.Closed
                    )
                    submitSideEffect(
                        DeleteNotifySideEffect(app.getString(R.string.manage_storage_error_delete))
                    )
                }
            }
        }
    }

    private fun getMediaType(mediaType: MediaType): String {
         return when {
             mediaType.isImage -> MediaType.IMAGE
             mediaType.isAudio -> MediaType.AUDIO
             mediaType.isVideo -> MediaType.VIDEO
             mediaType.isPdf -> MediaType.PDF
             else -> { MediaType.SPHINX_TEXT }
         }
    }

    fun changeItemSelection(messageId: MessageId) {
        val updatedFiles = (currentViewState as? DeleteChatMediaDetailViewState.FileList)?.files?.map { chatFile ->
            if (chatFile.messageId.value == messageId.value) {
                chatFile.copy(isSelected = !chatFile.isSelected)
            }
            else chatFile
        }

        updatedFiles?.let { fileList ->
            if (fileList.any { it.isSelected }) {
                val itemNumber = fileList.count { it.isSelected }.toString()
                val selectedSize = fileList.filter { it.isSelected }.sumOf{ it.size.calculateLongSize()}.toFileSize()?.calculateSize() ?: ""
                headerSelectionModeViewStateContainer.updateViewState(HeaderSelectionModeViewState.On(itemNumber, selectedSize))
            } else {
                headerSelectionModeViewStateContainer.updateViewState(HeaderSelectionModeViewState.Off)
            }
            updateViewState(DeleteChatMediaDetailViewState.FileList(fileList, itemsTotalSize.calculateSize()))
        }
    }

    fun deselectAllItems(){
        val updatedFiles = (currentViewState as? DeleteChatMediaDetailViewState.FileList)?.files?.map { chatFile ->
            chatFile.copy(isSelected = false)
        }
        updatedFiles?.let { fileList ->
            headerSelectionModeViewStateContainer.updateViewState(HeaderSelectionModeViewState.Off)
            updateViewState(DeleteChatMediaDetailViewState.FileList(updatedFiles, itemsTotalSize.calculateSize()))
        }
    }

    private fun setItemTotalFile(totalSize: Long) {
        if (totalSize > 0L && totalSize >= itemsTotalSize.value) {
            itemsTotalSize = FileSize(totalSize)
        }
    }

}
