package chat.sphinx.example.delete_chat_media_detail.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.example.delete_chat_media_detail.model.ChatFile
import chat.sphinx.example.delete_chat_media_detail.navigation.DeleteChatMediaDetailNavigator
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatDetailNotificationViewState
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatMediaDetailViewState
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.calculateSize
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.toFileSize
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_message_media.MediaType
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
internal class DeleteChatMediaDetailViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteChatMediaDetailNavigator,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    private val repositoryMedia: RepositoryMedia,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteNotifySideEffect,
        DeleteChatMediaDetailViewState
        >(dispatchers, DeleteChatMediaDetailViewState.Loading)
{
     val deleteChatNotificationViewStateContainer: ViewStateContainer<DeleteChatDetailNotificationViewState> by lazy {
        ViewStateContainer(DeleteChatDetailNotificationViewState.Closed)
    }
    private var currentChatIdAndFiles: Map<ChatId?, List<File>>? = null
    private var itemsTotalSize: FileSize = FileSize(0)

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getAllDownloadedMedia().collect { chatItems ->
                val chatIdAndFileList = getLocalFilesGroupedByChatId(chatItems)
                val totalSizeChats = chatItems.sumOf { it.localFile?.length() ?: 0 }.toFileSize()
                setItemTotalFile(totalSizeChats?.value ?: 0L )
                currentChatIdAndFiles = chatIdAndFileList

//                chatIdAndFileList.keys.mapNotNull { chatId ->
//                    val chat = chatId?.let { chatRepository.getChatById(it).firstOrNull() }
//                    val contact = chat?.contactIds?.lastOrNull()?.let { contactRepository.getContactById(it).firstOrNull() }
//                    val listOfFiles = chatIdAndFileList[chatId]
//
//                    if (contact != null && listOfFiles != null) {
//                        val totalSize = listOfFiles.map { FileSize(it.length()) }.calculateTotalSize()
//                        ChatToDelete(
//                            contact.alias?.value ?: "",
//                            contact.photoUrl,
//                            totalSize,
//                            chat.id,
//                            contact.id,
//                            Initials(
//                                contact.alias?.value?.getInitials(),
//                                chat.getColorKey()
//                            )
//                        )
//                    } else null
//                }.also { chatToDeletes ->
//                    viewStateContainer.updateViewState(DeleteChatMediaViewState.ChatList(chatToDeletes, totalSizeChats?.calculateSize()))
//                }
            }
        }
    }

    fun deleteAllChatFiles() {
        deleteChatNotificationViewStateContainer.updateViewState(DeleteChatDetailNotificationViewState.Deleting)
        viewModelScope.launch(mainImmediate) {
            currentChatIdAndFiles?.forEach { chatIdsAndFiles ->
                chatIdsAndFiles.key?.let {
                    if (repositoryMedia.deleteDownloadedMediaByChatId(it, chatIdsAndFiles.value)) {
//                        deleteChatNotificationViewStateContainer.updateViewState(DeleteChatNotificationViewState.SuccessfullyDeleted(itemsTotalSize.calculateSize()))
                    }
                    else {
//                        deleteChatNotificationViewStateContainer.updateViewState(DeleteChatNotificationViewState.Closed)
//                        submitSideEffect(
//                            DeleteNotifySideEffect(app.getString(R.string.manage_storage_error_delete))
//                        )
                    }
                }
                }
            }
        }



    private fun getLocalFilesGroupedByChatId(chatItems: List<MessageMedia>): Map<ChatId?, List<File>> {
        return chatItems.groupBy({ it.chatId }, { it.localFile as File })
    }

    private fun setItemTotalFile(totalSize: Long) {
        if (totalSize > 0L && totalSize >= itemsTotalSize.value) {
            itemsTotalSize = FileSize(totalSize)
        }
    }

}
