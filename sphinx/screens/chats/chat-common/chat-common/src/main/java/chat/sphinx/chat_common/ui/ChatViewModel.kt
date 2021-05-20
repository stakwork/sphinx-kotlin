package chat.sphinx.chat_common.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavArgs
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.header.ChatHeaderViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.HolderBackground
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.resources.getRandomColor
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.util.getInitials
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class ChatViewModel<ARGS: NavArgs>(
    private val app: Application,
    dispatchers: CoroutineDispatchers,
    protected val chatRepository: ChatRepository,
    protected val messageRepository: MessageRepository,
    protected val networkQueryLightning: NetworkQueryLightning,
    protected val savedStateHandle: SavedStateHandle
): BaseViewModel<ChatHeaderViewState>(dispatchers, ChatHeaderViewState.Idle)
{
    abstract val args: ARGS

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    @Suppress("RemoveExplicitTypeArguments")
    private val _chatDataStateFlow: MutableStateFlow<ChatData?> by lazy {
        MutableStateFlow<ChatData?>(null)
    }

    val chatDataStateFlow: StateFlow<ChatData?>
        get() = _chatDataStateFlow.asStateFlow()

    private val _messageHolderViewStateFlow: MutableStateFlow<List<MessageHolderViewState>> by lazy {
        MutableStateFlow(emptyList())
    }

    internal val messageHolderViewStateFlow: StateFlow<List<MessageHolderViewState>>
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
                app.getRandomColor()
            )
        } else {
            null
        }

        chatData.chat?.let { chat ->
            messagesJob = viewModelScope.launch(mainImmediate) {
                messageRepository.getAllMessagesToShowByChatId(chat.id).collect { messages ->
                    val newList = ArrayList<MessageHolderViewState>(messages.size)
                    withContext(default) {
                        for (message in messages) {
                            if (message.sender == chat.contactIds.firstOrNull()) {
                                newList.add(
                                    MessageHolderViewState.OutGoing(
                                        message,
                                        chat.type,
                                        HolderBackground.First.Isolated,
                                    )
                                )
                            } else {
                                newList.add(
                                    MessageHolderViewState.InComing(
                                        message,
                                        chat.type,
                                        HolderBackground.First.Isolated,
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

    val checkRoute: Flow<LoadResponse<Boolean, ResponseError>> by lazy {
        flow {
            chatDataStateFlow.value?.let { chatData ->
                when (chatData) {
                    is ChatData.Conversation -> {
                        chatData.contact.nodePubKey?.let { pubKey ->

                            chatData.contact.routeHint?.let { hint ->

                                networkQueryLightning.checkRoute(pubKey, hint)

                            } ?: networkQueryLightning.checkRoute(pubKey)

                        } ?: chatData.chat?.let { chat ->

                            networkQueryLightning.checkRoute(chat.id)

                        }
                    }

                    is ChatData.Group -> {
                        null
                    }

                    is ChatData.Tribe -> {
                        networkQueryLightning.checkRoute(chatData.chat!!.id)
                    }

                }?.collect { response ->
                    @Exhaustive
                    when (response) {
                        is LoadResponse.Loading -> {
                            emit(response)
                        }
                        is Response.Error -> {
                            emit(response)
                        }
                        is Response.Success -> {
                            emit(Response.Success(response.value.success_prob > 0))
                        }
                    }
                } ?: if (chatData is ChatData.Group) {

                    emit(Response.Success(true))

                } else {
                    emit(
                        Response.Error(ResponseError("ChatData was null"))
                    )
                }
            }
        }
    }

    val toggleChatMuted: Flow<Chat?> by lazy {
        flow {
            chatDataStateFlow.value?.let { chatData ->
                chatData.chat?.let { chat ->
                    emitAll(chatRepository.toggleChatMuted(chat))
                }
            }
        }
    }

    fun readMessages() {
        chatDataStateFlow.value?.let { chatData ->
            chatData.chat?.id?.let { chatId ->
                viewModelScope.launch(mainImmediate) {
                    messageRepository.readMessages(chatId)
                }
            }
        }
    }
}
