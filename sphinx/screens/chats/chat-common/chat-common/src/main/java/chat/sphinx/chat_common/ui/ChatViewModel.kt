package chat.sphinx.chat_common.ui

import android.app.Application
import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavArgs
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.header.ChatHeaderFooterViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.BubbleBackground
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.chat_common.ui.viewstate.selected.SelectedMessageViewState
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.SendMessage
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.message
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.resources.getRandomColor
import chat.sphinx.send_attachment_view_model_coordinator.request.SendAttachmentRequest
import chat.sphinx.send_attachment_view_model_coordinator.response.SendAttachmentResponse
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatActionType
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.isDeleted
import chat.sphinx.wrapper_message.isGroupAction
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@JvmSynthetic
@Suppress("NOTHING_TO_INLINE")
internal inline fun <ARGS: NavArgs> ChatViewModel<ARGS>.isMessageSelected(): Boolean =
    getSelectedMessageViewStateFlow().value is SelectedMessageViewState.SelectedMessage

abstract class ChatViewModel<ARGS: NavArgs>(
    protected val app: Application,
    dispatchers: CoroutineDispatchers,
    val memeServerTokenHandler: MemeServerTokenHandler,
    protected val chatRepository: ChatRepository,
    protected val contactRepository: ContactRepository,
    protected val messageRepository: MessageRepository,
    protected val networkQueryLightning: NetworkQueryLightning,
    protected val savedStateHandle: SavedStateHandle,
    protected val sendAttachmentCoordinator: ViewModelCoordinator<SendAttachmentRequest, SendAttachmentResponse>,
    protected val LOG: SphinxLogger,
): SideEffectViewModel<
        Context,
        ChatSideEffect,
        ChatHeaderFooterViewState
        >(dispatchers, ChatHeaderFooterViewState.Idle)
{
    abstract val args: ARGS

    protected abstract val chatNavigator: ChatNavigator

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

    private inner class ChatHeaderViewStateContainer: ViewStateContainer<ChatHeaderFooterViewState>(ChatHeaderFooterViewState.Idle) {
        override val viewStateFlow: StateFlow<ChatHeaderFooterViewState> = flow<ChatHeaderFooterViewState> {
            chatSharedFlow.collect { chat ->
                emit(
                    ChatHeaderFooterViewState.Initialized(
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
            ChatHeaderFooterViewState.Idle
        )
    }

    override val viewStateContainer: ViewStateContainer<ChatHeaderFooterViewState> by lazy {
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

    private var notify200Limit: Boolean = false
    internal val messageHolderViewStateFlow: StateFlow<List<MessageHolderViewState>> = flow {
        val chat = getChat()
        val chatName = getChatNameIfNull()
        val owner: Contact = contactRepository.accountOwner.value.let { contact ->
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
                } catch (e: Exception) {}
                delay(25L)

                resolvedOwner!!
            }
        }

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
                                background =  when {
                                    message.status.isDeleted() -> {
                                        BubbleBackground.Gone(setSpacingEqual = false)
                                    }
                                    message.type.isGroupAction() -> {
                                        BubbleBackground.Gone(setSpacingEqual = true)
                                    }
                                    else -> {
                                        BubbleBackground.First.Isolated
                                    }
                                },
                                replyMessageSenderName = { replyMessage ->
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
                                },
                                accountOwner = { owner }
                            )
                        )
                    } else {

                        val isDeleted = message.status.isDeleted()

                        newList.add(
                            MessageHolderViewState.Received(
                                message,
                                chat,
                                background = when {
                                    isDeleted -> {
                                        BubbleBackground.Gone(setSpacingEqual = false)
                                    }
                                    message.type.isGroupAction() -> {
                                        BubbleBackground.Gone(setSpacingEqual = true)
                                    }
                                    else -> {
                                        BubbleBackground.First.Isolated
                                    }
                                },
                                initialHolder = when {
                                    isDeleted ||
                                    message.type.isGroupAction() -> {
                                        InitialHolderViewState.None
                                    }
                                    else -> {
                                        getInitialHolderViewStateForReceivedMessage(message)
                                    }
                                },
                                replyMessageSenderName = { replyMessage ->
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
                                },
                                accountOwner = { owner }
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
    private var notifyJob: Job? = null
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

    private inner class SelectedMessageViewStateContainer: ViewStateContainer<SelectedMessageViewState>(
        SelectedMessageViewState.None
    )

    private val selectedMessageContainer by lazy {
        SelectedMessageViewStateContainer()
    }

    @JvmSynthetic
    internal fun getSelectedMessageViewStateFlow(): StateFlow<SelectedMessageViewState> =
        selectedMessageContainer.viewStateFlow

    @JvmSynthetic
    internal fun updateSelectedMessageViewState(selectedMessageViewState: SelectedMessageViewState?) {
        if (selectedMessageViewState == null) return

        selectedMessageContainer.updateViewState(selectedMessageViewState)
    }

    fun boostMessage(messageUUID: MessageUUID?) {
        if (messageUUID == null) return

        viewModelScope.launch(mainImmediate) {
            val chat = getChat()
            val response = messageRepository.boostMessage(
                chat.id,
                chat.pricePerMessage ?: Sat(0),
                chat.escrowAmount ?: Sat(0),
                messageUUID,
            )

            @Exhaustive
            when (response) {
                is Response.Error -> {
                    submitSideEffect(
                        ChatSideEffect.Notify(app.getString(R.string.notify_boost_failure))
                    )
                }
                is Response.Success -> {}
            }
        }
    }

    abstract fun shouldShowSendAttachmentMenu()

    fun showSendAttachmentMenu(isConversation: Boolean = false, contactId: ContactId? = null) {
        viewModelScope.launch(mainImmediate) {
            val response = sendAttachmentCoordinator.submitRequest(
                SendAttachmentRequest(isConversation)
            )
            if (response is Response.Success) {
                when (response.value.actionType) {
                    is ChatActionType.CancelAction -> {
                        //Menu dismissed. Nothing to do
                    }
                    is ChatActionType.OpenCamera -> {
                        submitSideEffect(
                            ChatSideEffect.Notify("Camera not implemented yet")
                        )
                    }
                    is ChatActionType.OpenPhotoLibrary -> {
                        submitSideEffect(
                            ChatSideEffect.Notify("Photo library not implemented yet")
                        )
                    }
                    is ChatActionType.OpenGifSearch -> {
                        submitSideEffect(
                            ChatSideEffect.Notify("Giphy search not implemented yet")
                        )
                    }
                    is ChatActionType.OpenFileLibrary -> {
                        submitSideEffect(
                            ChatSideEffect.Notify("File library not implemented yet")
                        )
                    }
                    is ChatActionType.OpenPaidMessageScreen -> {
                        submitSideEffect(
                            ChatSideEffect.Notify("Paid message editor not implemented yet")
                        )
                    }
                    is ChatActionType.RequestAmount -> {
                        submitSideEffect(
                            ChatSideEffect.Notify("Request amount not implemented yet")
                        )
                    }

                    is ChatActionType.SendPayment -> {
                        contactId?.let { contactId ->
                            delay(250L)
                            chatNavigator.toPaymentSendDetail(contactId)
                        }
                    }
                }
            }
        }
    }
}
