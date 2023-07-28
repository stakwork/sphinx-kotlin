package chat.sphinx.threads.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.threads.R
import chat.sphinx.threads.model.ThreadItem
import chat.sphinx.threads.navigation.ThreadsNavigator
import chat.sphinx.threads.viewstate.ThreadsViewState
import chat.sphinx.wrapper_common.chatTimeFormat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_common.timeAgo
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.getColorKey
import chat.sphinx.wrapper_contact.toContactAlias
import chat.sphinx.wrapper_message.ThreadUUID
import chat.sphinx.wrapper_message.getColorKey
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ThreadsViewModel @Inject constructor(
    private val app: Application,
    val navigator: ThreadsNavigator,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        ThreadsSideEffect,
        ThreadsViewState
        >(dispatchers, ThreadsViewState.Idle)
{
    private val args: ThreadsFragmentArgs by savedStateHandle.navArgs()


    private val _ownerStateFlow: MutableStateFlow<Contact?> by lazy {
        MutableStateFlow(null)
    }
    val ownerStateFlow: StateFlow<Contact?>?
        get() = _ownerStateFlow.asStateFlow()


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


    init {
        viewModelScope.launch(mainImmediate) {
            _ownerStateFlow.value = getOwner()
        }

        viewModelScope.launch(mainImmediate) {
            messageRepository.getThreadUUIDMessagesByChatId(ChatId(args.argChatId)).collect { threadUUIDMessages ->

                val messagesGrouped = threadUUIDMessages.groupBy { it.threadUUID }

                messagesGrouped.mapNotNull { it.key?.value?.toMessageUUID() }.let { messageUUID ->
                    val headerMessage = messageRepository.getAllMessagesByUUID(messageUUID)
                    val headerMessageMappedByUUID = headerMessage.associateBy { it.uuid?.value }

                    val completeThreads = messagesGrouped.mapValues { groupEntry ->
                        val threadUUID = groupEntry.key
                        val threadMessages = groupEntry.value

                        val threadHeaderMessage = headerMessageMappedByUUID[threadUUID?.value]

                        if (threadHeaderMessage != null) {
                            listOf(threadHeaderMessage) + threadMessages
                        } else {
                            threadMessages
                        }
                    }

                    val threadItems = completeThreads.keys.map { uuid ->

                        val owner = ownerStateFlow?.value
                        val threadItem = completeThreads[uuid]
                        val isOwner: Boolean = threadItem?.get(0)?.sender == owner?.id

                        val repliesList = threadItem?.drop(1)
                        val repliesExcess: Int? = if ((repliesList?.size ?: 0) > 6) repliesList?.size?.minus(6) else null

                        val aliasAndColor = if (isOwner) {
                            Pair(owner?.alias, owner?.getColorKey())
                        } else {
                            Pair(
                                threadItem?.get(0)?.senderAlias?.value?.toContactAlias(),
                                threadItem?.get(0)?.getColorKey()
                            )
                        }

                        val itemPhotoUrl = if (isOwner) owner?.photoUrl else threadItem?.get(0)?.senderPic
                        val repliesCountMap = repliesList?.groupingBy { it.sender }?.eachCount()

                        ThreadItem(
                            aliasAndColorKey = aliasAndColor,
                            photoUrl = itemPhotoUrl,
                            date = threadItem?.get(0)?.date?.chatTimeFormat() ?: "",
                            message = threadItem?.get(0)?.messageContentDecrypted?.value ?: "",
                            usersReplies = repliesList?.take(6)?.map {
                                val sender = if (isOwner) owner?.id else it.sender
                                val repliesCount = repliesCountMap?.get(sender)?.minus(1)

                                ReplyUserHolder(
                                    if (isOwner) owner?.photoUrl else it.senderPic,
                                    if (isOwner) owner?.alias else it.senderAlias?.value?.toContactAlias(),
                                    if (isOwner) owner?.getColorKey() ?: "" else it.getColorKey(),
                                    repliesCount?.takeIf { it > 1 }?.let {
                                        String.format(app.getString(R.string.threads_plus), it.toString())
                                    }
                                )
                            }?.distinct(),
                            repliesAmount = String.format(app.getString(R.string.replies_amount) ,repliesList?.size?.toString() ?: "0"),
                            lastReplyDate = threadItem?.last()?.date?.timeAgo(),
                            uuid = uuid?.value ?: ""
                        )
                    }

                    updateViewState(ThreadsViewState.ThreadList(threadItems))
                }
            }
        }
    }

    fun navigateToThreadDetail(uuid: String) {
        viewModelScope.launch(mainImmediate) {
            navigator.toChatTribeThread(ChatId(args.argChatId), ThreadUUID(uuid))
        }
    }

}
