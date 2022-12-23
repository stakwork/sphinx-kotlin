package chat.sphinx.dashboard.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.invoice.PayRequestDto
import chat.sphinx.concept_network_query_lightning.model.invoice.PostRequestPaymentDto
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_network_query_people.model.isClaimOnLiquidPath
import chat.sphinx.concept_network_query_people.model.isDeleteMethod
import chat.sphinx.concept_network_query_people.model.isProfilePath
import chat.sphinx.concept_network_query_people.model.isSaveMethod
import chat.sphinx.concept_network_query_verify_external.NetworkQueryAuthorizeExternal
import chat.sphinx.concept_network_query_version.NetworkQueryVersion
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_service_notification.PushNotificationRegistrar
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_socket_io.SocketIOState
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.*
import chat.sphinx.kotlin_response.*
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.RestoreProgress
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import chat.sphinx.wrapper_common.tribe.isValidTribeJoinLink
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
internal class DashboardViewModel @Inject constructor(
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
    private val feedRepository: FeedRepository,
    private val actionsRepository: ActionsRepository,
    private val networkQueryLightning: NetworkQueryLightning,

    private val networkQueryVersion: NetworkQueryVersion,
    private val networkQueryAuthorizeExternal: NetworkQueryAuthorizeExternal,
    private val networkQueryPeople: NetworkQueryPeople,
    private val pushNotificationRegistrar: PushNotificationRegistrar,

    private val relayDataHandler: RelayDataHandler,

    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,

    private val socketIOManager: SocketIOManager,
) : MotionLayoutViewModel<
        Any,
        Context,
        ChatListSideEffect,
        DashboardMotionViewState
        >(dispatchers, DashboardMotionViewState.DrawerCloseNavBarVisible) {

    private val args: DashboardFragmentArgs by handler.navArgs()

    val newVersionAvailable: MutableStateFlow<Boolean> by lazy(LazyThreadSafetyMode.NONE) {
        MutableStateFlow(false)
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

        syncFeedRecommendationsState()

        getRelayKeys()
        checkAppVersion()
        handleDeepLink(args.argDeepLink)

        actionsRepository.syncActions()
    }
    
    private fun getRelayKeys() {
        repositoryDashboard.getAndSaveTransportKey()
        repositoryDashboard.getOrCreateHMacKey()
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
            } ?: deepLink?.toExternalRequestLink()?.let { externalRequestLink ->
                handleExternalRequestLink(externalRequestLink)
            } ?: deepLink?.toStakworkAuthorizeLink()?.let { stakworkAuthorizeLink ->
                handleStakworkAuthorizeLink(stakworkAuthorizeLink)
            } ?: deepLink?.toCreateInvoiceLink()?.let { createInvoiceLink ->
                handleCreateInvoiceLink(createInvoiceLink)
            }
        }
    }

    private fun syncFeedRecommendationsState() {
        val appContext: Context = app.applicationContext
        val sharedPreferences = appContext.getSharedPreferences(FeedRecommendationsToggle.FEED_RECOMMENDATIONS_SHARED_PREFERENCES, Context.MODE_PRIVATE)

        val feedRecommendationsToggle = sharedPreferences.getBoolean(
            FeedRecommendationsToggle.FEED_RECOMMENDATIONS_ENABLED_KEY, false
        )
        feedRepository.setRecommendationsToggle(feedRecommendationsToggle)
    }

    fun toScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(
                    filter = object : ScannerFilter() {
                        override suspend fun checkData(data: String): Response<Any, String> {
                            return when {
                                data.isValidTribeJoinLink ||
                                data.isValidExternalAuthorizeLink ||
                                data.isValidPeopleConnectLink ||
                                data.isValidLightningPaymentRequest ||
                                data.isValidLightningNodePubKey ||
                                data.isValidVirtualNodeAddress ||
                                data.isValidExternalRequestLink ->
                                {
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

                code.toTribeJoinLink()?.let { tribeJoinLink ->

                    handleTribeJoinLink(tribeJoinLink)

                } ?: code.toExternalAuthorizeLink()?.let { externalAuthorizeLink ->

                    handleExternalAuthorizeLink(externalAuthorizeLink)

                } ?: code.toExternalRequestLink()?.let { externalRequestLink ->

                    handleExternalRequestLink(externalRequestLink)

                } ?: code.toPeopleConnectLink()?.let { peopleConnectLink ->

                    handlePeopleConnectLink(peopleConnectLink)

                } ?: code.toLightningNodePubKey()?.let { lightningNodePubKey ->

                    handleContactLink(lightningNodePubKey, null)

                } ?: code.toVirtualLightningNodeAddress()?.let { virtualNodeAddress ->

                    virtualNodeAddress.getPubKey()?.let { lightningNodePubKey ->

                        handleContactLink(
                            lightningNodePubKey,
                            virtualNodeAddress.getRouteHint()
                        )

                    }

                } ?: code.toLightningPaymentRequestOrNull()?.let { lightningPaymentRequest ->
                    try {
                        val bolt11 = Bolt11.decode(lightningPaymentRequest)
                        val amount = bolt11.getSatsAmount()

                        if (amount != null) {
                            submitSideEffect(
                                ChatListSideEffect.AlertConfirmPayLightningPaymentRequest(
                                    amount.value,
                                    bolt11.getMemo()
                                ) {
                                    payLightningPaymentRequest(lightningPaymentRequest)
                                }
                            )
                        } else {
                            submitSideEffect(
                                ChatListSideEffect.Notify(
                                    app.getString(R.string.payment_request_missing_amount),
                                    true
                                )
                            )
                        }
                    } catch (e: Exception) {}
                }
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

    private suspend fun handleContactLink(pubKey: LightningNodePubKey, routeHint: LightningRouteHint?) {
        contactRepository.getContactByPubKey(pubKey).firstOrNull()?.let { contact ->

            goToContactChat(contact)

        } ?: dashboardNavigator.toAddContactDetail(pubKey, routeHint)
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

    private fun handleStakworkAuthorizeLink(link: StakworkAuthorizeLink) {
        deepLinkPopupViewStateContainer.updateViewState(
            DeepLinkPopupViewState.StakworkAuthorizePopup(link)
        )
    }

    private fun handleCreateInvoiceLink(link: CreateInvoiceLink) {
        viewModelScope.launch(mainImmediate) {
            val postRequestPaymentDto = PostRequestPaymentDto(
                link.amount.toLong(),
            )

            networkQueryLightning.postRequestPayment(postRequestPaymentDto)
                .collect { loadResponse ->
                    @javax.annotation.meta.Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {
                            submitSideEffect(
                                ChatListSideEffect.Notify(
                                    app.getString(R.string.failed_to_request_payment)
                                )
                            )
                        }
                        is Response.Success -> {
                            dashboardNavigator.toQRCodeDetail(
                                loadResponse.value.invoice,
                                app.getString(R.string.payment_request),
                                app.getString(R.string.amount_n_sats, link.amount.toLong()),
                            )
                        }
                    }
                }
        }
    }

    private suspend fun handleExternalRequestLink(link: ExternalRequestLink) {
        deepLinkPopupViewStateContainer.updateViewState(
            DeepLinkPopupViewState.LoadingExternalRequestPopup
        )

        networkQueryPeople.getExternalRequestByKey(
            link.host,
            link.key
        ).collect { loadResponse ->

            when(loadResponse){
                is LoadResponse.Loading -> {}

                is Response.Error -> {
                    deepLinkPopupViewStateContainer.updateViewState(
                        DeepLinkPopupViewState.PopupDismissed
                    )

                    submitSideEffect(
                        ChatListSideEffect.Notify(
                            app.getString(R.string.dashboard_save_profile_generic_error)
                        )
                    )
                }

                is Response.Success -> {
                    if (loadResponse.value.isProfilePath()) {
                        if (loadResponse.value.isDeleteMethod()) {
                            deepLinkPopupViewStateContainer.updateViewState(
                                DeepLinkPopupViewState.DeletePeopleProfilePopup(
                                    link.host,
                                    loadResponse.value.body
                                )
                            )
                        } else if (loadResponse.value.isSaveMethod()) {
                            deepLinkPopupViewStateContainer.updateViewState(
                                DeepLinkPopupViewState.SaveProfilePopup(
                                    link.host,
                                    loadResponse.value.body,
                                )
                            )
                        }
                    } else if (loadResponse.value.isClaimOnLiquidPath()){
                        if(loadResponse.value.isSaveMethod()){
                            deepLinkPopupViewStateContainer.updateViewState(
                                DeepLinkPopupViewState.RedeemTokensPopup(
                                    link.host,
                                    loadResponse.value.body,
                                )
                            )
                        }
                    }
                }
            }

        }
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

    fun connectToContact(message: String?) {
        val viewState = deepLinkPopupViewStateContainer.viewStateFlow.value

        viewModelScope.launch(mainImmediate) {

            if (message.isNullOrEmpty()) {
                submitSideEffect(
                    ChatListSideEffect.Notify(
                        app.getString(R.string.dashboard_connect_message_empty)
                    )
                )

                return@launch
            }

            deepLinkPopupViewStateContainer.updateViewState(
                DeepLinkPopupViewState.PeopleConnectPopupProcessing
            )

            var errorMessage = app.getString(R.string.dashboard_connect_generic_error)

            if (viewState is DeepLinkPopupViewState.PeopleConnectPopup) {
                val alias = viewState.personInfoDto.owner_alias.toContactAlias() ?: ContactAlias(app.getString(R.string.unknown))
                val priceToMeet = viewState.personInfoDto.price_to_meet?.toSat() ?: Sat(0)
                val routeHint = viewState.personInfoDto.owner_route_hint?.toLightningRouteHint()
                val photoUrl = viewState.personInfoDto.img?.toPhotoUrl()

                viewState.personInfoDto.owner_pubkey.toLightningNodePubKey()?.let { pubKey ->
                    viewState.personInfoDto.owner_contact_key.toContactKey()?.let { contactKey ->
                        val response = contactRepository.connectToContact(
                            alias,
                            pubKey,
                            routeHint,
                            contactKey,
                            message,
                            photoUrl,
                            priceToMeet
                        )

                        when (response) {
                            is Response.Error -> {
                                errorMessage = response.cause.message
                            }
                            is Response.Success -> {
                                response.value?.let { contactId ->
                                    dashboardNavigator.toChatContact(null, contactId)
                                }

                                deepLinkPopupViewStateContainer.updateViewState(
                                    DeepLinkPopupViewState.PopupDismissed
                                )

                                return@launch
                            }
                        }
                    }
                }
            }

            submitSideEffect(
                ChatListSideEffect.Notify(errorMessage)
            )

            deepLinkPopupViewStateContainer.updateViewState(
                DeepLinkPopupViewState.PopupDismissed
            )
        }
    }

    fun authorizeExternal() {
        val viewState = deepLinkPopupViewStateContainer.viewStateFlow.value

        viewModelScope.launch(mainImmediate) {

            if (viewState is DeepLinkPopupViewState.ExternalAuthorizePopup) {

                deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.ExternalAuthorizePopupProcessing
                )

                val relayUrl: RelayUrl = relayDataHandler.retrieveRelayUrl() ?: return@launch

                val response = repositoryDashboard.authorizeExternal(
                    relayUrl.value,
                    viewState.link.host,
                    viewState.link.challenge
                )

                when (response) {
                    is Response.Error -> {
                        submitSideEffect(
                            ChatListSideEffect.Notify(response.cause.message)
                        )
                    }
                    is Response.Success -> {
                        val i = Intent(Intent.ACTION_VIEW)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        i.data = Uri.parse(
                            "https://${viewState.link.host}?challenge=${viewState.link.challenge}"
                        )
                        app.startActivity(i)
                    }
                }
            } else if (viewState is DeepLinkPopupViewState.StakworkAuthorizePopup) {
                deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.ExternalAuthorizePopupProcessing
                )

                val response = repositoryDashboard.authorizeStakwork(
                    viewState.link.host,
                    viewState.link.id,
                    viewState.link.challenge
                )

                when (response) {
                    is Response.Error -> {
                        submitSideEffect(
                            ChatListSideEffect.Notify(response.cause.message)
                        )
                    }
                    is Response.Success -> {
                        val i = Intent(Intent.ACTION_VIEW)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        i.data = Uri.parse(response.value)
                        app.startActivity(i)
                    }
                }
            } else {
                submitSideEffect(
                    ChatListSideEffect.Notify(
                        app.getString(R.string.dashboard_authorize_generic_error)
                    )
                )
            }

            deepLinkPopupViewStateContainer.updateViewState(
                DeepLinkPopupViewState.PopupDismissed
            )
        }
    }

    suspend fun updatePeopleProfile() {
        val viewState = deepLinkPopupViewStateContainer.viewStateFlow.value

        deepLinkPopupViewStateContainer.updateViewState(
            DeepLinkPopupViewState.ExternalRequestPopupProcessing
        )

        when (viewState) {
            is DeepLinkPopupViewState.RedeemTokensPopup -> {
                redeemBadgeToken(viewState.body)
            }
            is DeepLinkPopupViewState.SaveProfilePopup -> {
                savePeopleProfile(viewState.body)
            }
            is DeepLinkPopupViewState.DeletePeopleProfilePopup -> {
                deletePeopleProfile(viewState.body)
            }
            else -> {}
        }
    }

    private suspend fun deletePeopleProfile(body: String){
        viewModelScope.launch(mainImmediate) {
            when (repositoryDashboard.deletePeopleProfile(body)) {
                is Response.Error -> {
                    submitSideEffect(
                        ChatListSideEffect.Notify(
                            app.getString(R.string.dashboard_delete_profile_generic_error)
                        )
                    )
                }
                is Response.Success -> {
                    submitSideEffect(
                        ChatListSideEffect.Notify(
                            app.getString(R.string.dashboard_delete_profile_success)
                        )
                    )
                }
            }
        }.join()

        deepLinkPopupViewStateContainer.updateViewState(
            DeepLinkPopupViewState.PopupDismissed
        )
    }

    private suspend fun savePeopleProfile(body: String) {
        viewModelScope.launch(mainImmediate) {
            val response = repositoryDashboard.savePeopleProfile(
                body
            )

            when (response) {
                is Response.Error -> {
                    submitSideEffect(
                        ChatListSideEffect.Notify(
                            app.getString(R.string.dashboard_save_profile_generic_error)
                        )
                    )
                }
                is Response.Success -> {
                    submitSideEffect(
                        ChatListSideEffect.Notify(
                            app.getString(R.string.dashboard_save_profile_success)
                        )
                    )
                }
            }
        }.join()

        deepLinkPopupViewStateContainer.updateViewState(
            DeepLinkPopupViewState.PopupDismissed
        )
    }

    private suspend fun redeemBadgeToken(body: String) {
            viewModelScope.launch(mainImmediate) {
                val response = repositoryDashboard.redeemBadgeToken(
                    body
                )

                when (response) {
                    is Response.Error -> {
                        submitSideEffect(
                            ChatListSideEffect.Notify(
                                app.getString(R.string.dashboard_redeem_badge_token_generic_error)
                            )
                        )
                    }
                    is Response.Success -> {
                        submitSideEffect(
                            ChatListSideEffect.Notify(
                                app.getString(R.string.dashboard_redeem_badge_token_success)
                            )
                        )
                    }
                }
            }.join()

            deepLinkPopupViewStateContainer.updateViewState(
                DeepLinkPopupViewState.PopupDismissed
            )
    }

    val deepLinkPopupViewStateContainer: ViewStateContainer<DeepLinkPopupViewState> by lazy {
        ViewStateContainer(DeepLinkPopupViewState.PopupDismissed)
    }

    val chatListFooterButtonsViewStateContainer: ViewStateContainer<ChatListFooterButtonsViewState> by lazy {
        ViewStateContainer(ChatListFooterButtonsViewState.Idle)
    }

    val tabsViewStateContainer: ViewStateContainer<DashboardTabsViewState> by lazy {
        ViewStateContainer(DashboardTabsViewState.Idle)
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

    private var messagesCountJob: Job? = null
    fun screenInit() {
        messagesCountJob?.cancel()
        messagesCountJob = viewModelScope.launch(mainImmediate) {
            repositoryDashboard.getUnseenActiveConversationMessagesCount()
                .collect { unseenConversationMessagesCount ->
                    updateTabsState(
                        friendsBadgeVisible = (unseenConversationMessagesCount ?: 0) > 0
                    )
                }
        }
    }

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryDashboard.getUnseenTribeMessagesCount()
                .collect { unseenTribeMessagesCount ->
                    updateTabsState(
                        tribesBadgeVisible = (unseenTribeMessagesCount ?: 0) > 0
                    )
                }
        }

        viewModelScope.launch(mainImmediate) {
            val owner = getOwner()

            chatListFooterButtonsViewStateContainer.updateViewState(
                ChatListFooterButtonsViewState.ButtonsVisibility(
                    addFriendVisible = true,
                    createTribeVisible = !owner.isOnVirtualNode()
                )
            )
        }
    }

    fun updateTabsState(
        feedActive: Boolean? = null,
        friendsActive: Boolean? = null,
        tribesActive: Boolean? = null,
        friendsBadgeVisible: Boolean? = null,
        tribesBadgeVisible: Boolean? = null
    ) {
        val currentState = tabsViewStateContainer.viewStateFlow.value

        tabsViewStateContainer.updateViewState(
            if (currentState is DashboardTabsViewState.TabsState) {
                DashboardTabsViewState.TabsState(
                    feedActive = feedActive ?: currentState.feedActive,
                    friendsActive = friendsActive ?: currentState.friendsActive,
                    tribesActive = tribesActive ?: currentState.tribesActive,
                    friendsBadgeVisible = friendsBadgeVisible ?: currentState.friendsBadgeVisible,
                    tribesBadgeVisible = tribesBadgeVisible ?: currentState.tribesBadgeVisible
                )
            } else {
                DashboardTabsViewState.TabsState(
                    feedActive = feedActive ?: false,
                    friendsActive = friendsActive ?: true,
                    tribesActive = tribesActive ?: false,
                    friendsBadgeVisible = friendsBadgeVisible ?: false,
                    tribesBadgeVisible = tribesBadgeVisible ?: false
                )
            }
        )
    }

    fun getCurrentPagePosition() : Int {
        val currentState = tabsViewStateContainer.viewStateFlow.value
        if (currentState is DashboardTabsViewState.TabsState) {
            return when {
                currentState.feedActive -> {
                    DashboardFragmentsAdapter.FEED_TAB_POSITION
                }
                currentState.friendsActive -> {
                    DashboardFragmentsAdapter.FRIENDS_TAB_POSITION
                }
                currentState.tribesActive -> {
                    DashboardFragmentsAdapter.TRIBES_TAB_POSITION
                }
                else -> DashboardFragmentsAdapter.FRIENDS_TAB_POSITION
            }
        }
        return DashboardFragmentsAdapter.FRIENDS_TAB_POSITION
    }

    private suspend fun getOwner(): Contact {
        val owner = accountOwner.value.let { contact ->
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
        _accountOwnerStateFlow.value = owner

        return owner
    }

    private fun payLightningPaymentRequest(lightningPaymentRequest: LightningPaymentRequest) {
        viewModelScope.launch(mainImmediate) {
            val payLightningPaymentRequestDto = PayRequestDto(lightningPaymentRequest.value)
            networkQueryLightning.putLightningPaymentRequest(payLightningPaymentRequestDto).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                        submitSideEffect(
                            ChatListSideEffect.Notify(app.getString(R.string.attempting_payment_request), true)
                        )
                    }
                    is Response.Error -> {
                        submitSideEffect(
                            ChatListSideEffect.Notify(app.getString(R.string.failed_to_pay_request), true)
                        )
                    }
                    is Response.Success -> {
                        submitSideEffect(
                            ChatListSideEffect.Notify(app.getString(R.string.successfully_paid_invoice), true)
                        )
                    }
                }
            }
        }
    }

    private val _networkStateFlow: MutableStateFlow<LoadResponse<Boolean, ResponseError>> by lazy {
        MutableStateFlow(LoadResponse.Loading)
    }

    private val _restoreStateFlow: MutableStateFlow<RestoreProgress?> by lazy {
        MutableStateFlow(null)
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

    val restoreStateFlow: StateFlow<RestoreProgress?>
        get() = _restoreStateFlow.asStateFlow()

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

            repositoryDashboard.networkRefreshLatestContacts.collect { response ->
                @Exhaustive
                when (response) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        _networkStateFlow.value = response
                    }
                    is Response.Success -> {
                        val restoreProgress = response.value

                        if (restoreProgress.restoring) {
                            _restoreStateFlow.value = restoreProgress
                        }
                    }
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
                @Exhaustive
                when (response) {
                    is Response.Success -> {
                        val restoreProgress = response.value

                        if (restoreProgress.restoring && restoreProgress.progress < 100) {
                            _restoreStateFlow.value = restoreProgress
                        } else {
                            _restoreStateFlow.value = null

                            _networkStateFlow.value = Response.Success(true)
                        }
                    }
                    is Response.Error -> {
                        _networkStateFlow.value = response
                    }
                    is LoadResponse.Loading -> {
                        _networkStateFlow.value = response
                    }
                }
            }
        }
    }

    fun cancelRestore() {
        jobNetworkRefresh?.cancel()

        viewModelScope.launch(mainImmediate) {

            _networkStateFlow.value = Response.Success(true)
            _restoreStateFlow.value = null

            repositoryDashboard.didCancelRestore()
        }
    }

    fun goToAppUpgrade() {
        val i = Intent(Intent.ACTION_VIEW)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        i.data = Uri.parse("https://github.com/stakwork/sphinx-kotlin/releases")
        app.startActivity(i)
    }

    override suspend fun onMotionSceneCompletion(value: Any) {
        // Unused
    }

    fun toastIfNetworkConnected(){
        viewModelScope.launch(mainImmediate){
            submitSideEffect(
                ChatListSideEffect.Notify(
                    app.getString(
                        if (_networkStateFlow.value is Response.Error) {
                            R.string.dashboard_network_disconnected_node_toast
                        } else {
                            R.string.dashboard_network_connected_node_toast
                        }
                    )
                )
            )
        }
    }

    fun sendAppLog(appLog: String) {
        actionsRepository.setAppLog(appLog)
    }
}

