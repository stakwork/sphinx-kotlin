package chat.sphinx.dashboard.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.invoice.PayRequestDto
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
import chat.sphinx.dashboard.ui.viewstates.ChatFilter
import chat.sphinx.dashboard.ui.viewstates.ChatViewState
import chat.sphinx.dashboard.ui.viewstates.ChatViewStateContainer
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import chat.sphinx.wrapper_common.tribe.isValidTribeJoinLink
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.isInviteContact
import chat.sphinx.wrapper_contact.isTrue
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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
    private val app: Application,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    handler: SavedStateHandle,

    val dashboardNavigator: DashboardNavigator,
    val navBarNavigator: DashboardBottomNavBarNavigator,
    val navDrawerNavigator: DashboardNavDrawerNavigator,

    dispatchers: CoroutineDispatchers,

    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,

    protected val networkQueryLightning: NetworkQueryLightning,

    private val pushNotificationRegistrar: PushNotificationRegistrar,

    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    private val socketIOManager: SocketIOManager,
): MotionLayoutViewModel<
        Any,
        Context,
        DashboardSideEffect,
        NavDrawerViewState
        >(dispatchers, NavDrawerViewState.Closed)
{
    init {
        if (handler.navArgs<DashboardFragmentArgs>().value.updateBackgroundLoginTime) {
            viewModelScope.launch(default) {
                backgroundLoginHandler.updateLoginTime()
            }
        }
    }

    fun toScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(
                    filter = object : ScannerFilter() {
                        override suspend fun checkData(data: String): Response<Any, String> {
                            return when {
                                data.isValidTribeJoinLink -> {
                                    Response.Success(Any())
                                }
                                data.isValidLightningPaymentRequest -> {
                                    Response.Success(Any())
                                }
                                else -> {
                                    Response.Error(app.getString(R.string.not_valid_invoice_or_tribe_link))
                                }
                            }
                        }
                    },
                    showBottomView = true,
                    scannerModeLabel = app.getString(R.string.paste_invoice_of_tribe_link)
                )
            )
            if (response is Response.Success) {

                val code = response.value.value

                if (code.isValidTribeJoinLink) {
                    dashboardNavigator.toJoinTribeDetail(TribeJoinLink(code))
                } else if (code.isValidLightningPaymentRequest) {
                    code.toLightningPaymentRequest()?.toBolt11()?.let { bolt11 ->
                        val amount = bolt11.getSatsAmount()

                        if (amount != null) {
                            submitSideEffect(
                                DashboardSideEffect.AlertConfirmPayLightningPaymentRequest(amount.value, bolt11.getMemo()) {
                                    payLightningPaymentRequest(code.toLightningPaymentRequest()!!)
                                }
                            )
                        } else {
                            submitSideEffect(DashboardSideEffect.Notify(app.getString(R.string.payment_request_missing_amount), true))
                        }
                    }
                }
            }
        }
    }

//    @Volatile
//    private var pagerFlow: Flow<PagingData<DashboardItem>>? = null
//    private val pagerFlowLock = Mutex()
//
//    fun dashboardPagingDataFlow(): Flow<PagingData<DashboardItem>> = flow {
//        val flow: Flow<PagingData<DashboardItem>> = pagerFlow ?: pagerFlowLock.withLock {
//            pagerFlow ?: repositoryDashboard
//                .getDashboardItemPagingSource()
//                .let { sourceWrapper ->
//                    sourceWrapper.pagingDataFlow.map { pagingData ->
//                        pagingData.map { item ->
//
//                        }
//                        pagingData.insertSeparators { item: DashboardItem?, item2: DashboardItem? ->
//
//                        }
//                    }
//                    sourceWrapper.pagingDataFlow
//                        .cachedIn(viewModelScope)
//                        .also { pagerFlow = it }
//                }
//        }
//
//        emitAll(flow)
//    }

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

    private val _contactsStateFlow: MutableStateFlow<List<Contact>> by lazy {
        MutableStateFlow(emptyList())
    }

    private val collectionLock = Mutex()

    private var contactsCollectionInitialized: Boolean = false
    private var chatsCollectionInitialized: Boolean = false

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryDashboard.getAllContacts.distinctUntilChanged().collect { contacts ->
                updateChatListContacts(contacts)
            }
        }

        viewModelScope.launch(mainImmediate) {
            delay(25L)
            repositoryDashboard.getAllChats.distinctUntilChanged().collect { chats ->
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

        viewModelScope.launch(mainImmediate) {
            delay(50L)
            repositoryDashboard.getAllInvites.distinctUntilChanged().collect {
                updateChatListContacts(_contactsStateFlow.value)
            }
        }

        // Prime it...
        viewModelScope.launch(mainImmediate) {
            try {
                repositoryDashboard.accountOwner.collect {
                    if (it != null) {
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun payLightningPaymentRequest(lightningPaymentRequest: LightningPaymentRequest) {
        viewModelScope.launch(mainImmediate) {
            val payLightningPaymentRequestDto = PayRequestDto(lightningPaymentRequest.value)
            networkQueryLightning.putLightningPaymentRequest(payLightningPaymentRequestDto).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                        submitSideEffect(
                            DashboardSideEffect.Notify(app.getString(R.string.attempting_payment_request), true)
                        )
                    }
                    is Response.Error -> {
                        submitSideEffect(
                            DashboardSideEffect.Notify(app.getString(R.string.failed_to_pay_request), true)
                        )
                    }
                    is Response.Success -> {
                        submitSideEffect(
                            DashboardSideEffect.Notify(app.getString(R.string.successfully_paid_invoice), true)
                        )
                    }
                }
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
                        currentChats.add(
                            DashboardChat.Inactive.Conversation(contact)
                        )
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

    val networkStateFlow: StateFlow<LoadResponse<Boolean, ResponseError>>
        get() = _networkStateFlow.asStateFlow()

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
                    DashboardSideEffect.Notify(app.getString(R.string.pay_invite_balance_too_low))
                )
                return
            }
        }

        submitSideEffect(
            DashboardSideEffect.AlertConfirmPayInvite(invite.price?.value ?: 0) {
                viewModelScope.launch(mainImmediate) {
                    repositoryDashboard.payForInvite(invite)
                }
            }
        )
    }

    suspend fun deleteInvite(invite: Invite) {
        submitSideEffect(
            DashboardSideEffect.AlertConfirmDeleteInvite() {
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
