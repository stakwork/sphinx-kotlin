package chat.sphinx.chat_common.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.HolderBackground
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.resources.getRandomColor
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class ChatViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val dispatchers: CoroutineDispatchers,
    private val messageRepository: MessageRepository,
    private val lightningRepository: LightningRepository,
): BaseViewModel<ChatViewState>(ChatViewState.Idle)
{
    @Suppress("RemoveExplicitTypeArguments")
    private val _chatDataStateFlow: MutableStateFlow<ChatData?> by lazy {
        MutableStateFlow<ChatData?>(null)
    }

    val chatDataStateFlow: StateFlow<ChatData?>
        get() = _chatDataStateFlow.asStateFlow()

    fun onNavigationBack() {
        messagesJob?.cancel()
    }

    private val _messageHolderViewStateFlow: MutableStateFlow<List<MessageHolderViewState>> by lazy {
        MutableStateFlow(emptyList())
    }

    val messageHolderViewStateFlow: StateFlow<List<MessageHolderViewState>>
        get() = _messageHolderViewStateFlow.asStateFlow()

    private var messagesJob: Job? = null
    fun setChatData(chatData: ChatData) {
        _chatDataStateFlow.value = chatData
        _messageHolderViewStateFlow.value = emptyList()

        val initialHolder: InitialHolderViewState? = if (chatData is ChatData.Conversation) {
            chatData.contact.photoUrl?.let { url ->
                InitialHolderViewState.Url(url)
            } ?: InitialHolderViewState.Initials(
                chatData.contact.alias?.value?.getInitials() ?: "",
                appContext.getRandomColor()
            )
        } else {
            null
        }

        chatData.chat?.let { chat ->
            messagesJob = viewModelScope.launch(dispatchers.mainImmediate) {
                messageRepository.getMessagesForChat(chat.id).collect { messages ->
                    val newList = ArrayList<MessageHolderViewState>(messages.size)
                    withContext(dispatchers.default) {
                        for (message in messages) {
                            if (message.sender == chat.contactIds.firstOrNull()) {
                                newList.add(
                                    MessageHolderViewState.OutGoing(
                                        message,
                                        HolderBackground.Out.Middle
                                    )
                                )
                            } else {
                                newList.add(
                                    MessageHolderViewState.InComing(
                                        message,
                                        HolderBackground.In.Middle,
                                        initialHolder
                                            ?: message.senderPic?.let { url ->
                                                InitialHolderViewState.Url(url)
                                            } ?: message.senderAlias?.let { alias ->
                                                InitialHolderViewState.Initials(
                                                    alias.value.getInitials()
                                                )
                                            } ?: InitialHolderViewState.None
                                    )
                                )
                            }
                        }
                    }
                    _messageHolderViewStateFlow.value = newList
                }
            }
        }
    }

    fun checkRoute(): Flow<Boolean> = flow {
        chatDataStateFlow.value?.let { chatData ->
            val contact: Contact? = if (chatData is ChatData.Conversation) { chatData.contact } else { null }
            lightningRepository.networkCheckRoute(chat = chatData.chat, contact = contact).collect { success ->
                emit(success)
            }
        }
    }
}
