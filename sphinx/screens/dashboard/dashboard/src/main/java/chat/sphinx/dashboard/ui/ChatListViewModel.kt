package chat.sphinx.dashboard.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_verify_external.NetworkQueryAuthorizeExternal
import chat.sphinx.concept_network_query_version.NetworkQueryVersion
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_service_notification.PushNotificationRegistrar
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_socket_io.SocketIOState
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.adapter.DashboardChat
import chat.sphinx.dashboard.ui.viewstates.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


internal suspend inline fun ChatListViewModel.collectChatViewState(
    crossinline action: suspend (value: ChatViewState) -> Unit
): Unit =
    chatViewStateContainer.collect { action(it) }

internal val ChatListViewModel.currentChatViewState: ChatViewState
    get() = chatViewStateContainer.value

internal suspend inline fun ChatListViewModel.updateChatListFilter(filter: ChatFilter) {
    chatViewStateContainer.updateDashboardChats(null, filter)
}

@HiltViewModel
internal class ChatListViewModel @Inject constructor(
    private val app: Application,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    handler: SavedStateHandle,

    private val accountOwner: StateFlow<Contact?>,

    val dashboardNavigator: DashboardNavigator,
    val navBarNavigator: DashboardBottomNavBarNavigator,
    val navDrawerNavigator: DashboardNavDrawerNavigator,

    private val buildConfigVersionCode: BuildConfigVersionCode,
    dispatchers: CoroutineDispatchers,

    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,

    private val networkQueryLightning: NetworkQueryLightning,
    private val networkQueryVersion: NetworkQueryVersion,
    private val networkQueryAuthorizeExternal: NetworkQueryAuthorizeExternal,

    private val pushNotificationRegistrar: PushNotificationRegistrar,

    private val relayDataHandler: RelayDataHandler,

    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    private val socketIOManager: SocketIOManager,
): MotionLayoutViewModel<
        Any,
        Context,
        ChatListSideEffect,
        NavDrawerViewState
        >(dispatchers, NavDrawerViewState.Closed)
{

    private val args: ChatListFragmentArgs by handler.navArgs()

    val newVersionAvailable: MutableStateFlow<Boolean> by lazy(LazyThreadSafetyMode.NONE) {
        MutableStateFlow<Boolean>(false)
    }

    val currentVersion: MutableStateFlow<String> by lazy(LazyThreadSafetyMode.NONE) {
        MutableStateFlow("-")
    }

    init {
        if (args.updateBackgroundLoginTime) {
            viewModelScope.launch(default) {
                backgroundLoginHandler.updateLoginTime()
            }
        }

        checkAppVersion()
        handleDeepLink(args.argDeepLink)
    }

    fun handleDeepLink(deepLink: String?) {
        viewModelScope.launch(mainImmediate) {
            delay(100L)

            deepLink?.toTribeJoinLink()?.let { tribeJoinLink ->
                handleTribeJoinLink(tribeJoinLink)
            } ?: deepLink?.toExternalAuthorizeLink()?.let { externalAuthorizeLink ->
                handleExternalAuthorizeLink(externalAuthorizeLink)
            } ?: deepLink?.toPeopleConnectLink()?.let { peopleConnectLink ->
                handlePeopleConnectLink(peopleConnectLink)
            }
        }
    }

    private suspend fun handleTribeJoinLink(tribeJoinLink: TribeJoinLink) {
        val chat: Chat? = try {
            chatRepository.getChatByUUID(
                ChatUUID(tribeJoinLink.tribeUUID)
            ).firstOrNull()
        } catch (e: IllegalArgumentException) {
            null
        }

        if (chat != null) {
            dashboardNavigator.toChatTribe(chat.id)
        } else {
            dashboardNavigator.toJoinTribeDetail(tribeJoinLink)
        }
    }

    private suspend fun goToContactChat(contact: Contact) {
        chatRepository.getConversationByContactId(contact.id).firstOrNull()?.let { chat ->

            dashboardNavigator.toChatContact(chat.id, contact.id)

        } ?: dashboardNavigator.toChatContact(null, contact.id)
    }

    private fun handleExternalAuthorizeLink(link: ExternalAuthorizeLink) {
        deepLinkPopupViewStateContainer.updateViewState(
            DeepLinkPopupViewState.ExternalAuthorizePopup(link)
        )
    }

    private suspend fun handlePeopleConnectLink(link: PeopleConnectLink) {
        link.publicKey.toLightningNodePubKey()?.let { lightningNodePubKey ->
            contactRepository.getContactByPubKey(lightningNodePubKey).firstOrNull()?.let { contact ->

                goToContactChat(contact)

            } ?: loadPeopleConnectPopup(link)
        }
    }

    private suspend fun loadPeopleConnectPopup(link: PeopleConnectLink) {
        deepLinkPopupViewStateContainer.updateViewState(
            DeepLinkPopupViewState.PeopleConnectPopupLoadingPersonInfo
        )

        networkQueryAuthorizeExternal.getPersonInfo(link.host, link.publicKey).collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {}

                is Response.Error -> {

                    deepLinkPopupViewStateContainer.updateViewState(
                        DeepLinkPopupViewState.PopupDismissed
                    )

                    submitSideEffect(
                        ChatListSideEffect.Notify(
                            app.getString(R.string.dashboard_connect_retrieve_person_data_error)
                        )
                    )

                }
                is Response.Success -> {
                    deepLinkPopupViewStateContainer.updateViewState(
                        DeepLinkPopupViewState.PeopleConnectPopup(
                            loadResponse.value.owner_alias,
                            loadResponse.value.description ?: app.getString(R.string.dashboard_connect_description_missing),
                            loadResponse.value.price_to_meet ?: 0,
                            loadResponse.value.img,
                            loadResponse.value
                        )
                    )
                }
            }
        }
    }

    val deepLinkPopupViewStateContainer: ViewStateContainer<DeepLinkPopupViewState> by lazy {
        ViewStateContainer(DeepLinkPopupViewState.PopupDismissed)
    }

    val createTribeButtonViewStateContainer: ViewStateContainer<CreateTribeButtonViewState> by lazy {
        ViewStateContainer(CreateTribeButtonViewState.Hidden)
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
        repositoryDashboard.getAccountBalance()

    private fun checkAppVersion() {
        viewModelScope.launch(mainImmediate) {
            networkQueryVersion.getAppVersions().collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                    }
                    is Response.Success -> {
                        newVersionAvailable.value = loadResponse.value.kotlin > buildConfigVersionCode.value.toLong()
                        currentVersion.value = "VERSION: ${buildConfigVersionCode.value}"
                    }
                }
            }
        }
    }

    private val _contactsStateFlow: MutableStateFlow<List<Contact>> by lazy {
        MutableStateFlow(emptyList())
    }

    private val collectionLock = Mutex()

    private var contactsCollectionInitialized: Boolean = false
    private var chatsCollectionInitialized: Boolean = false

    init {

        if (args.argChatListType == ChatType.CONVERSATION) {
            viewModelScope.launch(mainImmediate) {
                repositoryDashboard.getAllContacts.distinctUntilChanged().collect { contacts ->
                    updateChatListContacts(contacts)
                }
            }

        }

        viewModelScope.launch(mainImmediate) {
            delay(25L)

            val allChats = when (args.argChatListType) {
                ChatType.CONVERSATION -> {
                    repositoryDashboard.getAllContactChats.distinctUntilChanged()
                }
                ChatType.TRIBE -> {
                    repositoryDashboard.getAllTribeChats.distinctUntilChanged()
                }
                else -> {
                    repositoryDashboard.getAllChats.distinctUntilChanged()
                }
            }

            allChats.collect { chats ->
                collectionLock.withLock {
                    chatsCollectionInitialized = true
                    val newList = ArrayList<DashboardChat>(chats.size)
                    val contactsAdded = mutableListOf<ContactId>()

                    withContext(default) {
                        for (chat in chats) {
                            val message: Message? = chat.latestMessageId?.let {
                                repositoryDashboard.getMessageById(it).firstOrNull()
                            }

                            if (chat.type.isConversation()) {
                                val contactId: ContactId = chat.contactIds.lastOrNull() ?: continue

                                val contact: Contact = repositoryDashboard.getContactById(contactId)
                                    .firstOrNull() ?: continue

                                contactsAdded.add(contactId)

                                newList.add(
                                    DashboardChat.Active.Conversation(
                                        chat,
                                        message,
                                        contact,
                                        repositoryDashboard.getUnseenMessagesByChatId(chat.id),
                                    )
                                )
                            } else {
                                newList.add(
                                    DashboardChat.Active.GroupOrTribe(
                                        chat,
                                        message,
                                        accountOwnerStateFlow.value,
                                        repositoryDashboard.getUnseenMessagesByChatId(chat.id)
                                    )
                                )
                            }
                        }
                    }

                    if (contactsCollectionInitialized) {
                        withContext(default) {
                            for (contact in _contactsStateFlow.value) {

                                if (!contactsAdded.contains(contact.id)) {
                                    if (contact.isInviteContact()) {
                                        var contactInvite: Invite? = null

                                        contact.inviteId?.let { inviteId ->
                                            contactInvite = withContext(io) {
                                                repositoryDashboard.getInviteById(inviteId).firstOrNull()
                                            }
                                        }
                                        if (contactInvite != null) {
                                            newList.add(
                                                DashboardChat.Inactive.Invite(contact, contactInvite)
                                            )
                                            continue
                                        }
                                    }
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

        if (args.argChatListType == ChatType.CONVERSATION) {
            viewModelScope.launch(mainImmediate) {
                delay(50L)
                repositoryDashboard.getAllInvites.distinctUntilChanged().collect {
                    updateChatListContacts(_contactsStateFlow.value)
                }
            }
        }


        viewModelScope.launch(mainImmediate) {
            val owner = getOwner()

            createTribeButtonViewStateContainer.updateViewState(
                if (owner.isOnVirtualNode()) {
                    CreateTribeButtonViewState.Hidden
                } else {
                    CreateTribeButtonViewState.Visible
                }
            )
        }
    }

    private suspend fun getOwner(): Contact {
        return accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    accountOwner.collect { ownerContact ->
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

    private suspend fun updateChatListContacts(contacts: List<Contact>) {
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
                val currentChats = currentChatViewState.originalList.toMutableList()
                val chatContactIds = mutableListOf<ContactId>()

                var updateChatViewState = false
                for (chat in currentChatViewState.originalList) {

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
                        is DashboardChat.Inactive.Invite -> {
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

                        if (repositoryDashboard.updatedContactIds.contains(it.id)) {
                            //Contact updated
                            currentChats.remove(chat)
                            chatContactIds.remove(it.id)
                        }
                    }
                }

                for (contact in _contactsStateFlow.value) {
                    if (!chatContactIds.contains(contact.id)) {
                        updateChatViewState = true

                        if (contact.isInviteContact()) {
                            var contactInvite: Invite? = null

                            contact.inviteId?.let { inviteId ->
                                contactInvite = withContext(io) {
                                    repositoryDashboard.getInviteById(inviteId).firstOrNull()
                                }
                            }
                            if (contactInvite != null) {
                                currentChats.add(
                                    DashboardChat.Inactive.Invite(contact, contactInvite)
                                )
                                continue
                            }
                        }

                        var updatedContactChat: DashboardChat = DashboardChat.Inactive.Conversation(contact)

                        for (chat in currentChatViewState.originalList) {
                            if (chat is DashboardChat.Active.Conversation) {
                                if (chat.contact.id == contact.id) {
                                    updatedContactChat = DashboardChat.Active.Conversation(
                                        chat.chat,
                                        chat.message,
                                        contact,
                                        chat.unseenMessageFlow
                                    )
                                }
                            }
                        }

                        currentChats.add(updatedContactChat)
                    }
                }

                if (updateChatViewState) {
                    chatViewStateContainer.updateDashboardChats(currentChats.toList())
                    repositoryDashboard.updatedContactIds = mutableListOf()
                }
            }
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

    private var jobNetworkRefresh: Job? = null
    private var jobPushNotificationRegistration: Job? = null
    fun networkRefresh() {
        if (jobNetworkRefresh?.isActive == true) {
            return
        }

        jobNetworkRefresh = viewModelScope.launch(mainImmediate) {
            repositoryDashboard.networkRefreshBalance.collect { response ->
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

            repositoryDashboard.networkRefreshContacts.collect { response ->
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

            // must occur after contacts have been retrieved such that
            // an account owner is available, otherwise it just suspends
            // until it is.
            if (jobPushNotificationRegistration == null) {
                jobPushNotificationRegistration = launch(mainImmediate) {
                    pushNotificationRegistrar.register().let { response ->
                        @Exhaustive
                        when (response) {
                            is Response.Error -> {
                                // TODO: Handle on the UI
                            }
                            is Response.Success -> {}
                        }
                    }
                }
            }

            repositoryDashboard.networkRefreshMessages.collect { response ->
                _networkStateFlow.value = response
            }
        }
    }

    suspend fun payForInvite(invite: Invite) {
        getAccountBalance().firstOrNull()?.let { balance ->
            if (balance.balance.value < (invite.price?.value ?: 0)) {
                submitSideEffect(
                    ChatListSideEffect.Notify(app.getString(R.string.pay_invite_balance_too_low))
                )
                return
            }
        }

        submitSideEffect(
            ChatListSideEffect.AlertConfirmPayInvite(invite.price?.value ?: 0) {
                viewModelScope.launch(mainImmediate) {
                    repositoryDashboard.payForInvite(invite)
                }
            }
        )
    }

    suspend fun deleteInvite(invite: Invite) {
        submitSideEffect(
            ChatListSideEffect.AlertConfirmDeleteInvite() {
                viewModelScope.launch(mainImmediate) {
                    repositoryDashboard.deleteInvite(invite)
                }
            }
        )
    }

    override suspend fun onMotionSceneCompletion(value: Any) {
        // Unused
    }

}
