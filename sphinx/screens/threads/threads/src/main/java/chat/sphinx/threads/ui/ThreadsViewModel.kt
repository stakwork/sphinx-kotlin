package chat.sphinx.threads.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.threads.R
import chat.sphinx.threads.model.ThreadItem
import chat.sphinx.threads.navigation.ThreadsNavigator
import chat.sphinx.threads.viewstate.ThreadsViewState
import chat.sphinx.wrapper_chat.Chat
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ThreadsViewModel @Inject constructor(
    private val app: Application,
    val navigator: ThreadsNavigator,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
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
    private val ownerStateFlow: StateFlow<Contact?>?
        get() = _ownerStateFlow.asStateFlow()

    private val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(ChatId(args.argChatId)))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )


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

    suspend fun getChat(): Chat {
        chatSharedFlow.replayCache.firstOrNull()?.let { chat ->
            return chat
        }

        chatSharedFlow.firstOrNull()?.let { chat ->
            return chat
        }

        var chat: Chat? = null

        try {
            chatSharedFlow.collect {
                if (it != null) {
                    chat = it
                    throw Exception()
                }
            }
        } catch (e: Exception) {
        }
        delay(25L)

        return chat!!
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
                        val threadItemList = completeThreads[uuid]

                        val originalMessage = threadItemList?.get(0)
                        val chat = getChat()
                        val isSenderOwner: Boolean = originalMessage?.sender == chat?.contactIds.firstOrNull()

                        val aliasAndColor = if (isSenderOwner) {
                            Pair(owner?.alias, owner?.getColorKey())
                        } else {
                            Pair(
                                threadItemList?.get(0)?.senderAlias?.value?.toContactAlias(),
                                threadItemList?.get(0)?.getColorKey()
                            )
                        }
                        val itemPhotoUrl = if (isSenderOwner) owner?.photoUrl else threadItemList?.get(0)?.senderPic

                        val repliesList = threadItemList?.drop(1)?.distinctBy { it.senderAlias }
                        val repliesCountMap = threadItemList?.drop(1)?.groupingBy { (it.senderAlias?.value ?: "Unknown") }?.eachCount()

                        ThreadItem(
                            aliasAndColorKey = aliasAndColor,
                            photoUrl = itemPhotoUrl,
                            date = threadItemList?.get(0)?.date?.chatTimeFormat() ?: "",
                            message = threadItemList?.get(0)?.messageContentDecrypted?.value ?: "",
                            usersReplies = repliesList?.take(6)?.map {
                                val isSenderOwner: Boolean = it?.sender == chat?.contactIds.firstOrNull()
                                val repliesCount = repliesCountMap?.get((it.senderAlias?.value ?: "Unknown"))?.minus(1)

                                ReplyUserHolder(
                                    if (isSenderOwner) owner?.photoUrl else it.senderPic,
                                    if (isSenderOwner) owner?.alias else it.senderAlias?.value?.toContactAlias(),
                                    if (isSenderOwner) owner?.getColorKey() ?: "" else it.getColorKey(),
                                    repliesCount?.takeIf { it > 0 }?.let {
                                        String.format(app.getString(R.string.threads_plus), it.toString())
                                    }
                                )
                            },
                            repliesAmount = String.format(app.getString(R.string.replies_amount) , threadItemList?.drop(1)?.size?.toString() ?: "0"),
                            lastReplyDate = threadItemList?.last()?.date?.timeAgo(),
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
