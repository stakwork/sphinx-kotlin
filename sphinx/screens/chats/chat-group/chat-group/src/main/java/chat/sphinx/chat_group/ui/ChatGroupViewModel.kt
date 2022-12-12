package chat.sphinx.chat_group.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import chat.sphinx.concept_link_preview.LinkPreviewHandler
import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_chat.getColorKey
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.PodcastClip
import chat.sphinx.wrapper_message.getColorKey
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
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
    chatNavigator: GroupChatNavigator,
    repositoryMedia: RepositoryMedia,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    actionsRepository: ActionsRepository,
    networkQueryLightning: NetworkQueryLightning,
    networkQueryPeople: NetworkQueryPeople,
    mediaCacheHandler: MediaCacheHandler,
    savedStateHandle: SavedStateHandle,
    cameraViewModelCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    linkPreviewHandler: LinkPreviewHandler,
    memeInputStreamHandler: MemeInputStreamHandler,
    LOG: SphinxLogger,
): ChatViewModel<ChatGroupFragmentArgs>(
    app,
    dispatchers,
    memeServerTokenHandler,
    chatNavigator,
    repositoryMedia,
    chatRepository,
    contactRepository,
    messageRepository,
    actionsRepository,
    networkQueryLightning,
    networkQueryPeople,
    mediaCacheHandler,
    savedStateHandle,
    cameraViewModelCoordinator,
    linkPreviewHandler,
    memeInputStreamHandler,
    LOG,
) {
    override val args: ChatGroupFragmentArgs by savedStateHandle.navArgs()
    override var chatId: ChatId = args.chatId
    override val contactId: ContactId?
        get() = null

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
                        chat.getColorKey()
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

    override suspend fun getChatInfo(): Triple<ChatName?, PhotoUrl?, String>? {
        return null
    }

    override fun forceKeyExchange() { }

    override suspend fun shouldStreamSatsFor(podcastClip: PodcastClip, messageUUID: MessageUUID?) {
        TODO("Not yet implemented")
    }

    override suspend fun getInitialHolderViewStateForReceivedMessage(
        message: Message,
        owner: Contact
    ): InitialHolderViewState {
        return message.senderPic?.let { url ->
            InitialHolderViewState.Url(url)
        } ?: message.senderAlias?.let { alias ->
            InitialHolderViewState.Initials(alias.value.getInitials(), message.getColorKey())
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

    override suspend fun sendMessage(builder: SendMessage.Builder): SendMessage? {
        builder.setChatId(args.chatId)
        return super.sendMessage(builder)
    }

    override fun navigateToChatDetailScreen() {
        // We do nothing because Chat Groups are being aren't supported anymore
    }

    override fun navigateToNotificationLevel() {
        // We do nothing because Chat Groups are being aren't supported anymore
    }
}
