package chat.sphinx.chat_common.ui

import android.app.Application
import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavArgs
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.header.ChatHeaderViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.BubbleBackground
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.SendMessage
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.message
import chat.sphinx.resources.getRandomColor
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.isDeleted
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
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
    protected val contactRepository: ContactRepository,
    protected val messageRepository: MessageRepository,
    protected val networkQueryLightning: NetworkQueryLightning,
    protected val savedStateHandle: SavedStateHandle
): SideEffectViewModel<
        Context,
        ChatSideEffect,
        ChatHeaderViewState
        >(dispatchers, ChatHeaderViewState.Idle)
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

                showUpdatedPrice(chat)

                emit(
                    ChatHeaderViewState.Initialized(
                        chatHeaderName = chat?.name?.value ?: getChatNameIfNull()?.value ?: "",
                        showLock = chat != null,
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

    private fun showUpdatedPrice(chat: Chat?) {
        viewModelScope.launch(mainImmediate) {
            val pricePerMessage = chat?.pricePerMessage?.value ?: 0
            val escrowAmount = chat?.escrowAmount?.value ?: 0

            if (pricePerMessage + escrowAmount > 0) {
                submitSideEffect(ChatSideEffect.Notify("Price per message: $pricePerMessage\n Amount to Stake: $escrowAmount"))
            }
        }
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

    private var notify200Limit: Boolean = false
    internal val messageHolderViewStateFlow: StateFlow<List<MessageHolderViewState>> = flow {
        val chat = getChat()
        val chatName = getChatNameIfNull()

        messageRepository.getAllMessagesToShowByChatId(chat.id).distinctUntilChanged().collect { messages ->
            val newList = ArrayList<MessageHolderViewState>(messages.size)

            if (messages.size > 199 && !notify200Limit) {
                viewModelScope.launch(mainImmediate) {
                    notify200Limit = true
                    delay(2_000)
                    submitSideEffect(ChatSideEffect.Notify(
                        "Messages are temporarily limited to 200 for this chat"
                    ))
                }
            }

            withContext(default) {
                for (message in messages) {
                    if (message.sender == chat.contactIds.firstOrNull()) {
                        newList.add(
                            MessageHolderViewState.Sent(
                                message,
                                chat,
                                if (message.status.isDeleted()) {
                                    BubbleBackground.Gone(setSpacingEqual = false)
                                } else {
                                    BubbleBackground.First.Isolated
                                },

                            ) { replyMessage ->
                                when {
                                    replyMessage.sender == chat.contactIds.firstOrNull() -> {
                                        contactRepository.accountOwner.value?.alias?.value ?: ""
                                    }
                                    chat.type.isConversation() -> {
                                        chatName?.value ?: ""
                                    }
                                    else -> {
                                        replyMessage.senderAlias?.value ?: ""
                                    }
                                }
                            }
                        )
                    } else {

                        val isDeleted = message.status.isDeleted()

                        newList.add(
                            MessageHolderViewState.Received(
                                message,
                                chat,

                                if (isDeleted) {
                                    BubbleBackground.Gone(setSpacingEqual = false)
                                } else {
                                    BubbleBackground.First.Isolated
                                },

                                if (isDeleted) {
                                    InitialHolderViewState.None
                                } else {
                                    getInitialHolderViewStateForReceivedMessage(message)
                                },

                            ) { replyMessage ->
                                when {
                                    replyMessage.sender == chat.contactIds.firstOrNull() -> {
                                        contactRepository.accountOwner.value?.alias?.value ?: ""
                                    }
                                    chat.type.isConversation() -> {
                                        chatName?.value ?: ""
                                    }
                                    else -> {
                                        replyMessage.senderAlias?.value ?: ""
                                    }
                                }
                            }
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
        val setupChatFlowJob = viewModelScope.launch(mainImmediate) {
            chatSharedFlow.firstOrNull()
        }
        val setupHeaderInitialHolderJob = viewModelScope.launch(mainImmediate) {
            headerInitialHolderSharedFlow.firstOrNull()
        }
        val setupViewStateContainerJob = viewModelScope.launch(mainImmediate) {
            viewStateContainer.viewStateFlow.firstOrNull()
        }
        viewModelScope.launch(mainImmediate) {
            delay(500)
            // cancel the setup jobs as the view has taken over observation
            // and we don't want to continue collecting endlessly if any of
            // them are still active. WhileSubscribed will take over.
            setupChatFlowJob.cancel()
            setupHeaderInitialHolderJob.cancel()
            setupViewStateContainerJob.cancel()
        }
    }

    abstract val checkRoute: Flow<LoadResponse<Boolean, ResponseError>>

    abstract fun readMessages()

    /**
     * Builds the [SendMessage] and returns it (or null if it was invalid),
     * then passes it off to the [MessageRepository] for processing.
     * */
    @CallSuper
    open fun sendMessage(builder: SendMessage.Builder): SendMessage? {
        val msg = builder.build()
        // TODO: if null figure out why and notify user via side effect
        messageRepository.sendMessage(msg)
        return msg
    }

    private var toggleChatMutedJob: Job? = null
    protected var notifyJob: Job? = null
    fun toggleChatMuted() {
        if (toggleChatMutedJob?.isActive == true) {
            if (notifyJob?.isActive == true) {
                return
            }

            notifyJob = viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    ChatSideEffect.Notify(
                        app.getString(R.string.chat_muted_waiting_on_network),
                        notificationLengthLong = false
                    )
                )
                delay(1_000)
            }
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
                        submitSideEffect(
                            ChatSideEffect.Notify(response.message)
                        )
                        delay(2_000)
                    }
                    is Response.Success -> {
                        if (response.value) {
                            submitSideEffect(
                                ChatSideEffect.Notify(
                                    app.getString(R.string.chat_muted_message)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
