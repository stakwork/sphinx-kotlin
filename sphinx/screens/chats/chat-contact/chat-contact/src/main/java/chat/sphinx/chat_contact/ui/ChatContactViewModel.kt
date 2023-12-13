package chat.sphinx.chat_contact.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.chat_common.ui.ChatSideEffect
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.concept_link_preview.LinkPreviewHandler
import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.route.RouteSuccessProbabilityDto
import chat.sphinx.concept_network_query_lightning.model.route.isRouteAvailable
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.getColorKey
import chat.sphinx.wrapper_contact.isEncrypted
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.PodcastClip
import chat.sphinx.wrapper_message.ThreadUUID
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val ChatContactFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

internal inline val ChatContactFragmentArgs.contactId: ContactId
    get() = ContactId(argContactId)

@HiltViewModel
internal class ChatContactViewModel @Inject constructor(
    app: Application,
    dispatchers: CoroutineDispatchers,
    memeServerTokenHandler: MemeServerTokenHandler,
    contactChatNavigator: ContactChatNavigator,
    repositoryMedia: RepositoryMedia,
    feedRepository: FeedRepository,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    actionsRepository: ActionsRepository,
    repositoryDashboard: RepositoryDashboardAndroid<Any>,
    networkQueryLightning: NetworkQueryLightning,
    networkQueryPeople: NetworkQueryPeople,
    mediaCacheHandler: MediaCacheHandler,
    savedStateHandle: SavedStateHandle,
    cameraViewModelCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    linkPreviewHandler: LinkPreviewHandler,
    memeInputStreamHandler: MemeInputStreamHandler,
    moshi: Moshi,
    LOG: SphinxLogger,
): ChatViewModel<ChatContactFragmentArgs>(
    app,
    dispatchers,
    memeServerTokenHandler,
    contactChatNavigator,
    repositoryMedia,
    feedRepository,
    chatRepository,
    contactRepository,
    messageRepository,
    actionsRepository,
    repositoryDashboard,
    networkQueryLightning,
    networkQueryPeople,
    mediaCacheHandler,
    savedStateHandle,
    cameraViewModelCoordinator,
    linkPreviewHandler,
    memeInputStreamHandler,
    moshi,
    LOG,
) {
    override val args: ChatContactFragmentArgs by savedStateHandle.navArgs()
    private var _chatId: ChatId? = args.chatId
    override val chatId: ChatId?
        get() = _chatId
    override val contactId: ContactId = args.contactId

    private val contactSharedFlow: SharedFlow<Contact?> = flow {
        emitAll(contactRepository.getContactById(contactId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        chatId?.let { chatId ->
            emitAll(chatRepository.getChatById(chatId))
        } ?: chatRepository.getConversationByContactId(contactId).collect { chat ->
            _chatId = chat?.id
            emit(chat)
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1
    )

    override val headerInitialHolderSharedFlow: SharedFlow<InitialHolderViewState> = flow {
        contactSharedFlow.collect { contact ->
            if (contact != null) {
                contact.photoUrl?.let { photoUrl ->
                    emit(
                        InitialHolderViewState.Url(photoUrl)
                    )
                } ?: contact.alias?.let { alias ->

                    emit(
                        InitialHolderViewState.Initials(
                            alias.value.getInitials(),
                            contact.getColorKey()
                        )
                    )
                } ?: emit(
                    InitialHolderViewState.None
                )
            }
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        replay = 1
    )

    override suspend fun getChatInfo(): Triple<ChatName?, PhotoUrl?, String>? {
        contactSharedFlow.replayCache.firstOrNull()?.let { contact ->
            return Triple(
                contact.alias?.value?.let { ChatName(it) },
                contact.photoUrl?.value?.let { PhotoUrl(it) },
                contact.getColorKey()
            )
        } ?: contactSharedFlow.firstOrNull()?.let { contact ->
            return Triple(
                contact.alias?.value?.let { ChatName(it) },
                contact.photoUrl?.value?.let { PhotoUrl(it) },
                contact.getColorKey()
            )
        } ?: let {
            var alias: ContactAlias? = null
            var photoUrl: PhotoUrl? = null
            var colorKey: String = app.getRandomHexCode()

            try {
                contactSharedFlow.collect { contact ->
                    if (contact != null) {
                        alias = contact.alias
                        photoUrl = contact.photoUrl
                        colorKey = contact.getColorKey()
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}
            delay(25L)

            return Triple(
                alias?.value?.let { ChatName(it) },
                photoUrl?.value?.let { PhotoUrl(it) },
                colorKey
            )
        }
    }

    override val threadSharedFlow: SharedFlow<List<Message>>?
        get() = null

    override fun forceKeyExchange() {
        viewModelScope.launch(io) {
            contactSharedFlow.firstOrNull()?.let { nnContact ->
                if (!nnContact.isEncrypted()) {
                    contactRepository.forceKeyExchange(nnContact.id)
                }
            }
        }
    }

    override suspend fun shouldStreamSatsFor(podcastClip: PodcastClip, messageUUID: MessageUUID?) {
        TODO("Not yet implemented")
    }

    override suspend fun getInitialHolderViewStateForReceivedMessage(message: Message, owner: Contact): InitialHolderViewState {
        if (message.sender == owner.id) {
            owner.photoUrl?.let { photoUrl ->
                return InitialHolderViewState.Url(photoUrl)
            } ?: owner.alias?.let { alias ->
                return InitialHolderViewState.Initials(
                    alias.value.getInitials(),
                    owner.getColorKey()
                )
            }
        }

        headerInitialHolderSharedFlow.replayCache.firstOrNull()?.let { initialHolder ->
            if (initialHolder !is InitialHolderViewState.None) {
                return initialHolder
            }
        }

        headerInitialHolderSharedFlow.firstOrNull()?.let { initialHolder ->
            if (initialHolder !is InitialHolderViewState.None) {
                return initialHolder
            }
        }

        var initialHolder: InitialHolderViewState? = null

        try {
            headerInitialHolderSharedFlow.collect {
                initialHolder = it
                throw Exception()
            }
        } catch (e: Exception) {}
        delay(25L)

        return initialHolder ?: InitialHolderViewState.None
    }

    override val checkRoute: Flow<LoadResponse<Boolean, ResponseError>> = flow {
        emit(LoadResponse.Loading)

        val networkFlow: Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>>? = let {
            emit(LoadResponse.Loading)

            var contact: Contact? = contactSharedFlow.replayCache.firstOrNull()
                ?: contactSharedFlow.firstOrNull()

            if (contact == null) {
                try {
                    contactSharedFlow.collect {
                        if (contact != null) {
                            contact = it
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {}
                delay(25L)
            }

            contact?.let { nnContact ->
                nnContact.nodePubKey?.let { pubKey ->

                    nnContact.routeHint?.let { hint ->

                        networkQueryLightning.checkRoute(pubKey, hint)

                    } ?: networkQueryLightning.checkRoute(pubKey)

                }
            }
        }

        networkFlow?.let { flow ->
            flow.collect { response ->
                @Exhaustive
                when (response) {
                    LoadResponse.Loading -> {}
                    is Response.Error -> {
                        emit(response)
                    }
                    is Response.Success -> {
                        emit(
                            Response.Success(response.value.isRouteAvailable)
                        )
                    }
                }
            }
        } ?: emit(Response.Error(
            ResponseError("Contact and chatId were null, unable to check route")
        ))
    }

    private suspend fun getContact() : Contact? {
        var contact: Contact? = contactSharedFlow.replayCache.firstOrNull()
            ?: contactSharedFlow.firstOrNull()

        if (contact == null) {
            try {
                contactSharedFlow.collect {
                    if (contact != null) {
                        contact = it
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}
            delay(25L)
        }

        return contact
    }

    override fun readMessages() {
        val idResolved: ChatId? = chatId ?: chatSharedFlow.replayCache.firstOrNull()?.id
        if (idResolved != null) {
            viewModelScope.launch(mainImmediate) {
                messageRepository.readMessages(idResolved)
            }
        }
    }

    override fun reloadPinnedMessage() {}

    override fun getThreadUUID(): ThreadUUID? {
        return null
    }

    override fun isThreadChat(): Boolean {
        return false
    }

    override suspend fun sendMessage(builder: SendMessage.Builder): SendMessage? {

        builder.setContactId(contactId)
        builder.setChatId(chatId)

        return super.sendMessage(builder)
    }

    override fun navigateToChatDetailScreen() {
        viewModelScope.launch(mainImmediate) {
            (chatNavigator as ContactChatNavigator).toEditContactDetail(contactId)
        }
    }

    override fun navigateToNotificationLevel() {}

    override fun onSmallProfileImageClick(message: Message) {
        navigateToChatDetailScreen()
    }
}
