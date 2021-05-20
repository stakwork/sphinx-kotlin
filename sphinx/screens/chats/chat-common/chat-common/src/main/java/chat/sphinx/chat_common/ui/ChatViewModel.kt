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
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_message.Message
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class ChatViewModel<ARGS: NavArgs>(
    protected val app: Application,
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

    protected val headerInitialsTextViewColor: Int by lazy {
        app.getRandomColor()
    }

    protected abstract val chatSharedFlow: SharedFlow<Chat?>
    abstract val headerInitialHolderSharedFlow: SharedFlow<InitialHolderViewState>

    protected abstract suspend fun getChatNameIfNull(): ChatName?

    private inner class ChatHeaderViewStateContainer: ViewStateContainer<ChatHeaderViewState>(ChatHeaderViewState.Idle) {
        override val viewStateFlow: StateFlow<ChatHeaderViewState> = flow<ChatHeaderViewState> {
            chatSharedFlow.collect { chat ->

                emit(
                    ChatHeaderViewState.Initialized(
                        chatHeaderName = chat?.name?.value ?: getChatNameIfNull()?.value ?: "",
                        showLock = chat != null,

                        // TODO: Implement for Tribes with a podcast
                        contributions = null,

                        chat?.isMuted,
                    )
                )

            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ChatHeaderViewState.Idle
        )

        @Throws(IllegalStateException::class)
        override fun updateViewState(viewState: ChatHeaderViewState) {
            throw IllegalStateException(
                """
                    ChatHeaderViewState updates automatically.
                    This method does nothing and should not be called.
                """.trimIndent()
            )
        }
    }

    override val viewStateContainer: ViewStateContainer<ChatHeaderViewState> by lazy {
        ChatHeaderViewStateContainer()
    }

    private suspend fun getChat(): Chat {
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
        } catch (e: Exception) {}
        delay(25L)

        return chat!!
    }

    abstract suspend fun getInitialHolderViewStateForReceivedMessage(
        message: Message
    ): InitialHolderViewState

    internal val messageHolderViewStateFlow: StateFlow<List<MessageHolderViewState>> = flow {
        val chat = getChat()

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
                                getInitialHolderViewStateForReceivedMessage(message)
                            )
                        )
                    }
                }
            }

            emit(newList.toList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun init() {
        // Prime our states immediately so they're already
        // updated by the time the Fragment's onStart is called
        // where they're collected.
        viewModelScope.launch(dispatchers.mainImmediate) {
            chatSharedFlow.firstOrNull()
        }
        viewModelScope.launch(dispatchers.mainImmediate) {
            headerInitialHolderSharedFlow.firstOrNull()
        }
        viewModelScope.launch(dispatchers.mainImmediate) {
            viewStateContainer.viewStateFlow.firstOrNull()
        }
    }

    abstract val checkRoute: Flow<LoadResponse<Boolean, ResponseError>>

    abstract fun readMessages()

    private var toggleChatMutedJob: Job? = null
    fun toggleChatMuted() {
        if (toggleChatMutedJob?.isActive == true) {
            // TODO: Show notification that we're waiting for network, b/c
            //  that is the only reason it's not done and we don't want
            return
        }
        // by the time the user has the ability to click mute, chatSharedFlow
        // will be instantiated. If there is no chat for the given
        // conversation (such as with a new contact), the mute button is
        // invisible, so...
        chatSharedFlow.replayCache.firstOrNull()?.let { chat ->
            toggleChatMutedJob = viewModelScope.launch(mainImmediate) {
                val response = chatRepository.toggleChatMuted(chat)
                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        // TODO: Do something
                    }
                    is Response.Success -> {
                        if (response.value) {
                            val i = 0
                            // TODO: Submit notification side effect
                        }
                    }
                }
            }
        }
    }
}
