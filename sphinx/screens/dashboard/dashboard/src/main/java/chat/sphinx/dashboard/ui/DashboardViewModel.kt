package chat.sphinx.dashboard.ui

import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_service_notification.PushNotificationRegistrar
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_socket_io.SocketIOState
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.adapter.DashboardChat
import chat.sphinx.dashboard.ui.viewstates.ChatFilter
import chat.sphinx.dashboard.ui.viewstates.ChatViewState
import chat.sphinx.dashboard.ui.viewstates.ChatViewStateContainer
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.isConfirmed
import chat.sphinx.wrapper_contact.isTrue
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.sideeffect.SideEffect
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

internal suspend inline fun DashboardViewModel.collectChatViewState(
    crossinline action: suspend (value: ChatViewState) -> Unit
): Unit =
    chatViewStateContainer.collect { action(it) }

internal val DashboardViewModel.currentChatViewState: ChatViewState
    get() = chatViewStateContainer.value

internal suspend inline fun DashboardViewModel.updateChatListFilter(filter: ChatFilter) {
    chatViewStateContainer.updateDashboardChats(null, filter)
}

@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    val navBarNavigator: DashboardBottomNavBarNavigator,
    val navDrawerNavigator: DashboardNavDrawerNavigator,

    dispatchers: CoroutineDispatchers,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    private val messageRepository: MessageRepository,

    private val pushNotificationRegistrar: PushNotificationRegistrar,

    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    private val socketIOManager: SocketIOManager,
): MotionLayoutViewModel<
        Any,
        Nothing,
        SideEffect<Nothing>,
        NavDrawerViewState
        >(dispatchers, NavDrawerViewState.Closed)
{
    fun toScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(filter = null)
            )
            // TODO: Do something with response
        }
    }

    val chatViewStateContainer: ChatViewStateContainer by lazy {
        ChatViewStateContainer(dispatchers)
    }

    private val _accountOwnerStateFlow: MutableStateFlow<Contact?> by lazy {
        MutableStateFlow(null)
    }

    val accountOwnerStateFlow: StateFlow<Contact?>
        get() = _accountOwnerStateFlow.asStateFlow()

    suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        lightningRepository.getAccountBalance()

    private val _contactsStateFlow: MutableStateFlow<List<Contact>> by lazy {
        MutableStateFlow(emptyList())
    }

    private val collectionLock = Mutex()

    private var contactsCollectionInitialized: Boolean = false
    private var chatsCollectionInitialized: Boolean = false

    init {
        viewModelScope.launch(mainImmediate) {
            contactRepository.getAllContacts.distinctUntilChanged().collect { contacts ->
                collectionLock.withLock {
                    contactsCollectionInitialized = true

                    if (contacts.isEmpty()) {
                        return@withLock
                    }

                    val newList = ArrayList<Contact>(contacts.size)
                    val contactIds = ArrayList<ContactId>(contacts.size)

                    withContext(default) {
                        for (contact in contacts) {
                            if (contact.isOwner.isTrue()) {
                                _accountOwnerStateFlow.value = contact
                                continue
                            }

                            contactIds.add(contact.id)
                            newList.add(contact)
                        }
                    }

                    _contactsStateFlow.value = newList.toList()

                    // Don't push update to chat view state, let it's collection do it.
                    if (!chatsCollectionInitialized) {
                        return@withLock
                    }

                    withContext(default) {
                        val currentChats = currentChatViewState.list.toMutableList()
                        val chatContactIds = mutableListOf<ContactId>()

                        var updateChatViewState = false
                        for (chat in currentChatViewState.list) {

                            val contact: Contact? = when (chat) {
                                is DashboardChat.Active.Conversation -> {
                                    chat.contact
                                }
                                is DashboardChat.Active.GroupOrTribe -> {
                                    null
                                }
                                is DashboardChat.Inactive.Conversation -> {
                                    chat.contact
                                }
                            }

                            contact?.let {
                                chatContactIds.add(it.id)
                                // if the id of the currently displayed chat is not contained
                                // in the list collected here, it's either a new contact w/o
                                // a chat, or a contact that was deleted which we need to remove
                                // from the list of chats.

                                if (!contactIds.contains(it.id)) {
                                    //Contact deleted
                                    updateChatViewState = true
                                    currentChats.remove(chat)
                                    chatContactIds.remove(it.id)
                                }

                                if (contactRepository.updatedContactIds.contains(it.id)) {
                                    //Contact updated
                                    currentChats.remove(chat)
                                    chatContactIds.remove(it.id)
                                }
                            }
                        }

                        for (contact in _contactsStateFlow.value) {
                            if (contact.status.isConfirmed() && !chatContactIds.contains(contact.id)) {
                                updateChatViewState = true

                                currentChats.add(
                                    DashboardChat.Inactive.Conversation(contact)
                                )
                            }
                        }

                        if (updateChatViewState) {
                            chatViewStateContainer.updateDashboardChats(currentChats.toList())
                            contactRepository.updatedContactIds = mutableListOf()
                        }
                    }
                }
            }
        }

        viewModelScope.launch(mainImmediate) {
            delay(25L)
            chatRepository.getAllChats.distinctUntilChanged().collect { chats ->
                collectionLock.withLock {
                    chatsCollectionInitialized = true
                    val newList = ArrayList<DashboardChat>(chats.size)
                    val contactsAdded = mutableListOf<ContactId>()

                    withContext(default) {
                        for (chat in chats) {
                            val message: Message? = chat.latestMessageId?.let {
                                messageRepository.getMessageById(it).firstOrNull()
                            }

                            if (chat.type.isConversation()) {
                                val contactId: ContactId = chat.contactIds.lastOrNull() ?: continue

                                val contact: Contact = contactRepository.getContactById(contactId)
                                    .firstOrNull() ?: continue

                                contactsAdded.add(contactId)

                                newList.add(
                                    DashboardChat.Active.Conversation(
                                        chat,
                                        message,
                                        contact,
                                        chatRepository.getUnseenMessagesByChatId(chat),
                                    )
                                )
                            } else {
                                newList.add(
                                    DashboardChat.Active.GroupOrTribe(
                                        chat,
                                        message,
                                        chatRepository.getUnseenMessagesByChatId(chat)
                                    )
                                )
                            }
                        }
                    }

                    if (contactsCollectionInitialized) {
                        withContext(default) {
                            for (contact in _contactsStateFlow.value) {

                                if (contact.status.isConfirmed() && !contactsAdded.contains(contact.id)) {
                                    newList.add(
                                        DashboardChat.Inactive.Conversation(contact)
                                    )
                                }

                            }
                        }
                    }

                    chatViewStateContainer.updateDashboardChats(newList)
                }
            }
        }

        // Prime it
        viewModelScope.launch(mainImmediate) {
            contactRepository.accountOwner.firstOrNull()
        }
    }

    private val _networkStateFlow: MutableStateFlow<LoadResponse<Boolean, ResponseError>> by lazy {
        MutableStateFlow(LoadResponse.Loading)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            socketIOManager.socketIOStateFlow.collect { state ->
                if (state is SocketIOState.Uninitialized) {
                    socketIOManager.connect()
                }
            }
        }
    }

    val networkStateFlow: StateFlow<LoadResponse<Boolean, ResponseError>>
        get() = _networkStateFlow.asStateFlow()

    private var pushNotificationRegistrationUpdated: Boolean = false
    private var jobNetworkRefresh: Job? = null
    fun networkRefresh() {
        if (jobNetworkRefresh?.isActive == true) {
            return
        }

        jobNetworkRefresh = viewModelScope.launch(mainImmediate) {
            lightningRepository.networkRefreshBalance.collect { response ->
                @Exhaustive
                when (response) {
                    is LoadResponse.Loading,
                    is Response.Error -> {
                        _networkStateFlow.value = response
                    }
                    is Response.Success -> {}
                }
            }

            if (_networkStateFlow.value is Response.Error) {
                jobNetworkRefresh?.cancel()
            }

            contactRepository.networkRefreshContacts.collect { response ->
                @Exhaustive
                when (response) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        _networkStateFlow.value = response
                    }
                    is Response.Success -> {}
                }
            }

            if (_networkStateFlow.value is Response.Error) {
                jobNetworkRefresh?.cancel()
            }

            if (!pushNotificationRegistrationUpdated) {
                pushNotificationRegistrar.register().let { response ->
                    @Exhaustive
                    when (response) {
                        is Response.Error -> {
                            // TODO: Handle on the UI
                        }
                        is Response.Success -> {
                            pushNotificationRegistrationUpdated = true
                        }
                    }
                }
            }

            messageRepository.networkRefreshMessages.collect { response ->
                _networkStateFlow.value = response
            }
        }
    }

    override suspend fun onMotionSceneCompletion(value: Any) {
        // Unused
    }
}
