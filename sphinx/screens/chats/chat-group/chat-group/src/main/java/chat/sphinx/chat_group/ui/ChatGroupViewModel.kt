package chat.sphinx.chat_group.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_group.navigation.GroupChatNavigator
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
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.resources.getRandomColor
import chat.sphinx.send_attachment_view_model_coordinator.request.SendAttachmentRequest
import chat.sphinx.send_attachment_view_model_coordinator.response.SendAttachmentResponse
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val ChatGroupFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
class ChatGroupViewModel @Inject constructor(
    app: Application,
    dispatchers: CoroutineDispatchers,
    memeServerTokenHandler: MemeServerTokenHandler,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    networkQueryLightning: NetworkQueryLightning,
    savedStateHandle: SavedStateHandle,
    sendAttachmentViewModelCoordinator: ViewModelCoordinator<SendAttachmentRequest, SendAttachmentResponse>,
    LOG: SphinxLogger,
): ChatViewModel<ChatGroupFragmentArgs>(
    app,
    dispatchers,
    memeServerTokenHandler,
    chatRepository,
    contactRepository,
    messageRepository,
    networkQueryLightning,
    savedStateHandle,
    sendAttachmentViewModelCoordinator,
    LOG,
) {
    override val args: ChatGroupFragmentArgs by savedStateHandle.navArgs()

    @Inject
    protected lateinit var chatGroupNavigator: GroupChatNavigator
    override val chatNavigator: ChatNavigator
        get() = chatGroupNavigator

    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(args.chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1
    )

    override val headerInitialHolderSharedFlow: SharedFlow<InitialHolderViewState> = flow {
        chatSharedFlow.collect { chat ->
            chat?.photoUrl?.let {
                emit(
                    InitialHolderViewState.Url(it)
                )
            } ?: chat?.name?.let {
                emit(
                    InitialHolderViewState.Initials(
                        it.value.getInitials(),
                        headerInitialsTextViewColor
                    )
                )
            } ?: emit(
                InitialHolderViewState.None
            )
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        replay = 1,
    )

    override suspend fun getChatNameIfNull(): ChatName? {
        return null
    }

    override suspend fun getInitialHolderViewStateForReceivedMessage(
        message: Message
    ): InitialHolderViewState {
        return message.senderPic?.let { url ->
            InitialHolderViewState.Url(url)
        } ?: message.senderAlias?.let { alias ->
            InitialHolderViewState.Initials(alias.value.getInitials(), app.getRandomColor())
        } ?: InitialHolderViewState.None
    }

    override val checkRoute: Flow<LoadResponse<Boolean, ResponseError>> = flow {
        emit(Response.Success(true))
    }

    override fun readMessages() {
        viewModelScope.launch(mainImmediate) {
            messageRepository.readMessages(args.chatId)
        }
    }

    override fun sendMessage(builder: SendMessage.Builder): SendMessage? {
        builder.setChatId(args.chatId)
        return super.sendMessage(builder)
    }

    override fun shouldShowActionsMenu() {
        showAactionsMenu(false)
    }
}
