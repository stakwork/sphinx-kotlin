package chat.sphinx.profile.ui

import android.app.Application
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.InputType
import android.webkit.URLUtil
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_network_query_crypter.NetworkQueryCrypter
import chat.sphinx.concept_network_query_relay_keys.NetworkQueryRelayKeys
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_signer_manager.SignerCallback
import chat.sphinx.concept_signer_manager.SignerManager
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import chat.sphinx.menu_bottom_profile_pic.UpdatingImageViewState
import chat.sphinx.menu_bottom_signer.SignerMenuHandler
import chat.sphinx.menu_bottom_signer.SignerMenuViewModel
import chat.sphinx.profile.R
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.SphinxCallLink
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.PrivatePhoto
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.isOnionAddress
import chat.sphinx.wrapper_relay.toRelayUrl
import chat.sphinx.wrapper_rsa.RsaPublicKey
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_authentication.coordinator.ConfirmedPinListener
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.concept_views.sideeffect.SideEffectContainer
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.clazzes.clear
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.base64.encodeBase64
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import javax.inject.Inject
import kotlinx.serialization.Serializable

@Serializable
data class SampleClass(
    val elements: List<SampleClassElement>
)

@Serializable
data class SampleClassElement(
    val key: String,
    val value: SampleClassSubElement
)

@Serializable
data class SampleClassSubElement(
    val key: Int,
    val value: List<SampleClassSubSubElement>
)

@Serializable
data class SampleClassSubSubElement(
    val key1: UInt,
    val key2: UInt,
    val key3: UInt
)

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    private val networkQueryRelayKeys: NetworkQueryRelayKeys,
    val networkQueryCrypter: NetworkQueryCrypter,
    private val relayDataHandler: RelayDataHandler,
    private val torManager: TorManager,
    private val LOG: SphinxLogger,
    val walletDataHandler: WalletDataHandler,
    val moshi: Moshi
    ): SideEffectViewModel<
        Context,
        ProfileSideEffect,
        ProfileViewState>(dispatchers, ProfileViewState.Basic),
    PictureMenuViewModel, SignerMenuViewModel, SignerCallback
{
    companion object {
        const val SIGNING_DEVICE_SHARED_PREFERENCES = "general_settings"
        const val SIGNING_DEVICE_SETUP_KEY = "signing-device-setup"

        const val BITCOIN_NETWORK_REG_TEST = "regtest"
        const val BITCOIN_NETWORK_MAIN_NET = "mainnet"
    }

    private lateinit var signerManager: SignerManager

    fun setSignerManager(signerManager: SignerManager) {
        signerManager.setWalletDataHandler(walletDataHandler)
        signerManager.setMoshi(moshi)
        signerManager.setNetworkQueryCrypter(networkQueryCrypter)

        this.signerManager = signerManager
    }

    val storageBarViewStateContainer: ViewStateContainer<StorageBarViewState> by lazy {
        ViewStateContainer(StorageBarViewState.Loading)
    }

    val updatingImageViewStateContainer: ViewStateContainer<UpdatingImageViewState> by lazy {
        ViewStateContainer(UpdatingImageViewState.Idle)
    }

    override val pictureMenuHandler: PictureMenuHandler by lazy {
        PictureMenuHandler(
            app = app,
            cameraCoordinator = cameraCoordinator,
            dispatchers = this,
            viewModel = this,
            callback = { streamProvider, mediaType, fileName, contentLength, file ->

                updatingImageViewStateContainer.updateViewState(
                    UpdatingImageViewState.UpdatingImage
                )

                viewModelScope.launch(mainImmediate) {
                    val response = contactRepository.updateProfilePic(
                        stream = streamProvider,
                        mediaType = mediaType,
                        fileName = fileName,
                        contentLength = contentLength,
                    )

                    @Exhaustive
                    when (response) {
                        is Response.Error -> {
                            updatingImageViewStateContainer.updateViewState(
                                UpdatingImageViewState.UpdatingImageFailed
                            )
                        }
                        is Response.Success -> {
                            updatingImageViewStateContainer.updateViewState(
                                UpdatingImageViewState.UpdatingImageSucceed
                            )
                        }
                    }

                    try {
                        file?.delete()
                    } catch (e: Exception) {}
                }
            }
        )
    }

    override val signerMenuHandler: SignerMenuHandler by lazy {
        SignerMenuHandler()
    }

    override fun setupHardwareSigner() {
        signerManager.setupSignerHardware(this)
    }

    override fun setupPhoneSigner() {
        signerManager.setupPhoneSigner(null, this)
    }

    private fun setUpManageStorage(){
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getStorageDataInfo().collect { storageData ->
                val totalStorage = getTotalStorage()
                val usedStorage = storageData.usedStorage
                val freeStorage = (totalStorage - usedStorage.value).toFileSize()
                val modifiedStorageDataInfo = storageData.copy(freeStorage = freeStorage)
                val storagePercentage = calculateStoragePercentage(modifiedStorageDataInfo)

                storageBarViewStateContainer.updateViewState(
                    StorageBarViewState.StorageData(
                        storagePercentage,
                        usedStorage.calculateSize(),
                        totalStorage.toFileSize()?.calculateSize() ?: "0 Bytes"
                    )
                )
            }
        }
    }

    private fun getTotalStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

    private var resetPINJob: Job? = null
    fun resetPIN() {
        if (resetPINJob?.isActive == true) return

        resetPINJob = viewModelScope.launch(mainImmediate) {
            authenticationCoordinator.submitAuthenticationRequest(
                AuthenticationRequest.ResetPassword()
            ).firstOrNull()?.let { response ->
                @Exhaustive
                when (response) {
                    is AuthenticationResponse.Failure -> {
                        // handle
                    }
                    is AuthenticationResponse.Success.Authenticated -> {
                        // handle
                    }
                    is AuthenticationResponse.Success.Key -> {}
                }
            }
        }
    }

    private var setGithubPATJob: Job? = null
    fun setGithubPAT() {
        if (setGithubPATJob?.isActive == true) return

        setGithubPATJob = viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.GithubPATSet { pat ->
                pat?.let {
                    viewModelScope.launch(mainImmediate) {
                        when (contactRepository.setGithubPat(pat)) {
                            is Response.Error -> {
                                submitSideEffect(ProfileSideEffect.FailedToSetGithubPat)
                            }
                            is Response.Success -> {
                                submitSideEffect(ProfileSideEffect.GithubPATSuccessfullySet)
                            }
                        }
                    }
                }
            })
        }
    }

    fun switchTabTo(basicTab: Boolean) {
        if (basicTab) {
            updateViewState(ProfileViewState.Basic)
        } else {
            updateViewState(
                ProfileViewState.Advanced(
                    if (isSigningDeviceSetupDone()) {
                        app.getString(R.string.configure_signing_device)
                    } else {
                        app.getString(R.string.setup_signing_device)
                    }
                )
            )
        }
    }

    suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        lightningRepository.getAccountBalance()

    suspend fun updateOwner(
        alias: String?,
        privatePhoto: PrivatePhoto?,
        tipAmount: Sat?
    ): Response<Any, ResponseError> =
        contactRepository.updateOwner(alias, privatePhoto, tipAmount)

    suspend fun updateMeetingServer(url: String?) {
        _meetingServerUrlStateFlow.value = url

        delay(50L)

        if (url == null || url.isEmpty() || !URLUtil.isValidUrl(url)) {
            submitSideEffect(ProfileSideEffect.InvalidMeetingServerUrl)
            setServerUrls()
            return
        }

        val appContext: Context = app.applicationContext
        val serverUrlsSharedPreferences = appContext.getSharedPreferences("server_urls", Context.MODE_PRIVATE)

        withContext(dispatchers.io) {
            serverUrlsSharedPreferences.edit().putString(SphinxCallLink.CALL_SERVER_URL_KEY, url)
                .let { editor ->
                    if (!editor.commit()) {
                        editor.apply()
                    }
                }
        }
    }

    suspend fun updateLinkPreviewsEnabled(enabled: Boolean) {
        _linkPreviewsEnabledStateFlow.value = enabled

        delay(50L)

        val appContext: Context = app.applicationContext
        val generalSettingsSharedPreferences = appContext.getSharedPreferences(PreviewsEnabled.LINK_PREVIEWS_SHARED_PREFERENCES, Context.MODE_PRIVATE)

        withContext(dispatchers.io) {
            generalSettingsSharedPreferences.edit().putBoolean(PreviewsEnabled.LINK_PREVIEWS_ENABLED_KEY, enabled)
                .let { editor ->
                    if (!editor.commit()) {
                        editor.apply()
                    }
                }
        }
    }

    suspend fun updateFeedRecommendationsToggle(enabled: Boolean) {
        _feedRecommendationsStateFlow.value = enabled
        feedRepository.setRecommendationsToggle(enabled)

        delay(50L)

        val appContext: Context = app.applicationContext
        val generalSettingsSharedPreferences = appContext.getSharedPreferences(FeedRecommendationsToggle.FEED_RECOMMENDATIONS_SHARED_PREFERENCES, Context.MODE_PRIVATE)

        withContext(dispatchers.io) {
            generalSettingsSharedPreferences.edit().putBoolean(FeedRecommendationsToggle.FEED_RECOMMENDATIONS_ENABLED_KEY, enabled)
                .let { editor ->
                    if (!editor.commit()) {
                        editor.apply()
                    }
                }
        }
    }

    suspend fun updateRelayUrl(url: String?)  {
        if (url == null || url.isEmpty() || url == _relayUrlStateFlow.value) {
            return
        }

        _relayUrlStateFlow.value = url

        // Block updating to an onion address if Tor is not already
        // required (which means it is not running, and thus would leak
        // the onion address to the device DNS provider
        if (url.isOnionAddress && torManager.isTorRequired() != true) {
            _relayUrlStateFlow.value = relayDataHandler.retrieveRelayUrl()?.value
            submitSideEffect(ProfileSideEffect.RelayUrlUpdateToTorNotSupported)
            return
        }

        url.toRelayUrl()?.let { relayUrl ->
            relayDataHandler.retrieveAuthorizationToken()?.let { authorizationToken ->
                if (relayUrl.value.startsWith("http://") && !relayUrl.isOnionAddress) {
                    submitSideEffect(
                        ProfileSideEffect.RelayUrlHttpConfirmation(
                            relayUrl = relayUrl,
                            callback = { url ->
                                testAndPersistRelayUrl(url, authorizationToken)
                            }
                        )
                    )
                } else {
                    testAndPersistRelayUrl(relayUrl, authorizationToken)
                }
                return
            }
        }
        testAndPersistRelayUrl(null, null)
    }

    private fun testAndPersistRelayUrl(relayUrl: RelayUrl?, authorizationToken: AuthorizationToken?) {
        viewModelScope.launch(mainImmediate) {
            var success = false

            if (relayUrl != null && authorizationToken != null) {
                _relayUrlStateFlow.value = relayUrl.value

                submitSideEffect(ProfileSideEffect.UpdatingRelayUrl)

                var transportKey: RsaPublicKey? = null

                networkQueryRelayKeys.getRelayTransportKey(relayUrl).collect { loadResponse ->
                    @javax.annotation.meta.Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {}

                        is Response.Success -> {
                            transportKey = RsaPublicKey(loadResponse.value.transport_key.toCharArray())
                        }
                    }
                }

                val transportToken = relayDataHandler.retrieveRelayTransportToken(
                    authorizationToken,
                    transportKey
                )

                lightningRepository.getAccountBalanceAll(
                    Triple(Pair(authorizationToken, transportToken), null, relayUrl)
                ).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            success = false
                        }
                        is Response.Success -> {
                            transportKey?.let { key ->
                                relayDataHandler.persistRelayTransportKey(key)
                            }
                            success = relayDataHandler.persistRelayUrl(relayUrl)
                        }
                    }
                }
            }

            _relayUrlStateFlow.value = relayDataHandler.retrieveRelayUrl()?.value

            val sideEffect = if (success) {
                ProfileSideEffect.RelayUrlUpdatedSuccessfully
            } else {
                ProfileSideEffect.FailedToUpdateRelayUrl
            }

            submitSideEffect(sideEffect)
        }
    }

    fun persistPINTimeout() {
        _pinTimeoutStateFlow.value?.let { timeout ->
            viewModelScope.launch(mainImmediate) {
                if (!backgroundLoginHandler.updateSetting(timeout)) {
                    _pinTimeoutStateFlow.value = backgroundLoginHandler.getTimeOutSetting()
                    // TODO: Side effect, failed to persist setting
                }
            }
        }
    }

    fun updatePINTimeOutStateFlow(progress: Int) {
        _pinTimeoutStateFlow.value = progress
    }

    fun backupKeys() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.BackupKeysPinNeeded)

            var passwordPin: Password? = null

            authenticationCoordinator.submitAuthenticationRequest(
                AuthenticationRequest.ConfirmPin(object : ConfirmedPinListener() {
                    override suspend fun doWithConfirmedPassword(password: Password) {
                        passwordPin = password
                    }
                })
            ).firstOrNull().let { response ->
                @Exhaustive
                when (response) {
                    null,
                    is AuthenticationResponse.Failure -> {
                        submitSideEffect(ProfileSideEffect.WrongPIN)
                    }
                    is AuthenticationResponse.Success.Authenticated -> {
                        authenticationCoordinator.submitAuthenticationRequest(
                            AuthenticationRequest.GetEncryptionKey()
                        ).firstOrNull().let { keyResponse ->
                            @Exhaustive
                            when (keyResponse) {
                                null,
                                is AuthenticationResponse.Failure -> {
                                    submitSideEffect(ProfileSideEffect.BackupKeysFailed)
                                }
                                is AuthenticationResponse.Success.Authenticated -> {

                                }
                                is AuthenticationResponse.Success.Key -> {
                                    passwordPin?.let {
                                        encryptKeysAndExport(keyResponse.encryptionKey, it)
                                    }
                                }
                            }
                        }
                    }
                    is AuthenticationResponse.Success.Key -> { }
                }

                passwordPin?.clear()
            }
        }
    }

    @OptIn(RawPasswordAccess::class)
    private suspend fun encryptKeysAndExport(encryptionKey: EncryptionKey, passwordPin: Password) {
        val relayUrl = relayDataHandler.retrieveRelayUrl()?.value
        val authToken = relayDataHandler.retrieveAuthorizationToken()?.value
        val privateKey = String(encryptionKey.privateKey.value)
        val publicKey = String(encryptionKey.publicKey.value)

        val keysString = "$privateKey::$publicKey::${relayUrl}::${authToken}"

        try {
            val encryptedString = AES256JNCryptor()
                .encryptData(keysString.toByteArray(), passwordPin.value)
                .encodeBase64()

            val finalString = "keys::${encryptedString}"
                .toByteArray()
                .encodeBase64()

            submitSideEffect(ProfileSideEffect.CopyBackupToClipboard(finalString))
        } catch (e: CryptorException) {
            submitSideEffect(ProfileSideEffect.BackupKeysFailed)
        } catch (e: IllegalArgumentException) {
            submitSideEffect(ProfileSideEffect.BackupKeysFailed)
        }
    }

    private val _relayUrlStateFlow: MutableStateFlow<String?> by lazy {
        MutableStateFlow(null)
    }
    private val _pinTimeoutStateFlow: MutableStateFlow<Int?> by lazy {
        MutableStateFlow(null)
    }
    private val _meetingServerUrlStateFlow: MutableStateFlow<String?> by lazy {
        MutableStateFlow(null)
    }
    private val _linkPreviewsEnabledStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(true)
    }

    private val _feedRecommendationsStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    val relayUrlStateFlow: StateFlow<String?>
        get() = _relayUrlStateFlow.asStateFlow()
    val pinTimeoutStateFlow: StateFlow<Int?>
        get() = _pinTimeoutStateFlow.asStateFlow()
    val meetingServerUrlStateFlow: StateFlow<String?>
        get() = _meetingServerUrlStateFlow.asStateFlow()
    val accountOwnerStateFlow: StateFlow<Contact?>
        get() = contactRepository.accountOwner
    val linkPreviewsEnabledStateFlow: StateFlow<Boolean>
        get() = _linkPreviewsEnabledStateFlow.asStateFlow()
    val feedRecommendationsStateFlow: StateFlow<Boolean>
        get() = _feedRecommendationsStateFlow.asStateFlow()

    init {
        viewModelScope.launch(mainImmediate) {
            _relayUrlStateFlow.value =  relayDataHandler.retrieveRelayUrl()?.value
            _pinTimeoutStateFlow.value = backgroundLoginHandler.getTimeOutSetting()

            setServerUrls()
            setLinkPreviewsEnabled()
            setFeedRecommendationsToggle()
            setUpManageStorage()
        }
    }

    private fun setServerUrls() {
        val appContext: Context = app.applicationContext
        val serverUrlsSharedPreferences = appContext.getSharedPreferences("server_urls", Context.MODE_PRIVATE)

        val meetingServerUrl = serverUrlsSharedPreferences.getString(
            SphinxCallLink.CALL_SERVER_URL_KEY,
            SphinxCallLink.DEFAULT_CALL_SERVER_URL
        ) ?: SphinxCallLink.DEFAULT_CALL_SERVER_URL

        _meetingServerUrlStateFlow.value = meetingServerUrl
    }

    private fun setLinkPreviewsEnabled() {
        val appContext: Context = app.applicationContext
        val serverUrlsSharedPreferences = appContext.getSharedPreferences(PreviewsEnabled.LINK_PREVIEWS_SHARED_PREFERENCES, Context.MODE_PRIVATE)

        val linkPreviewsEnabled = serverUrlsSharedPreferences.getBoolean(
            PreviewsEnabled.LINK_PREVIEWS_ENABLED_KEY,
            PreviewsEnabled.True.isTrue()
        )

        _linkPreviewsEnabledStateFlow.value = linkPreviewsEnabled
    }

    private fun setFeedRecommendationsToggle() {
        val appContext: Context = app.applicationContext
        val sharedPreferences = appContext.getSharedPreferences(FeedRecommendationsToggle.FEED_RECOMMENDATIONS_SHARED_PREFERENCES, Context.MODE_PRIVATE)

        val feedRecommendationsToggle = sharedPreferences.getBoolean(
            FeedRecommendationsToggle.FEED_RECOMMENDATIONS_ENABLED_KEY, false
        )
        feedRepository.setRecommendationsToggle(feedRecommendationsToggle)
        _feedRecommendationsStateFlow.value = feedRecommendationsToggle
    }

    private fun isSigningDeviceSetupDone(): Boolean {
        val appContext: Context = app.applicationContext
        val sharedPreferences = appContext.getSharedPreferences(SIGNING_DEVICE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

        return sharedPreferences.getBoolean(
            SIGNING_DEVICE_SETUP_KEY,
            false
        )
    }


    override fun checkNetwork(callback: (Boolean) -> Unit) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.CheckNetwork {
                callback.invoke(true)
            })
        }
    }

    override fun signingDeviceNetwork(
        callback: (String) -> Unit
    ) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
                app.getString(R.string.network_name_title),
                app.getString(R.string.network_name_message)
            ) { networkName ->
                if (networkName == null) {
                    viewModelScope.launch(mainImmediate) {
                        submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Network can not be empty"))
                        return@launch
                    }
                } else {
                    callback.invoke(networkName)
                }
            })
        }
    }

    override fun signingDevicePassword(networkName: String, callback: (String) -> Unit) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
                app.getString(R.string.network_password_title),
                app.getString(
                    R.string.network_password_message,
                    networkName ?: "-"
                ),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ) { networkPass ->
                viewModelScope.launch(mainImmediate) {
                    if (networkPass == null) {
                        submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Network password can not be empty"))
                        return@launch
                    } else {
                        callback.invoke(networkPass)
                    }
                }
            })
        }
    }

    override fun signingDeviceLightningNodeUrl(callback: (String) -> Unit) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
                app.getString(R.string.lightning_node_url_title),
                app.getString(R.string.lightning_node_url_message),
            ) { lightningNodeUrl ->
                viewModelScope.launch(mainImmediate) {
                    if (lightningNodeUrl == null) {
                        submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Lightning node URL can not be empty"))
                        return@launch
                    }
                    else {
                        callback.invoke(lightningNodeUrl)
                    }
                }
            })
        }
    }

    override fun signingDeviceCheckBitcoinNetwork(network: (String) -> Unit, linkSigningDevice: (Boolean) -> Unit) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.CheckBitcoinNetwork(
                regTestCallback = {
                    network.invoke(BITCOIN_NETWORK_REG_TEST)
                }, mainNetCallback = {
                    network.invoke(BITCOIN_NETWORK_MAIN_NET)
                }, callback = {
                    viewModelScope.launch(mainImmediate) {
                        linkSigningDevice.invoke(true)
                    }
                }
            ))
        }
    }

    override fun failedToSetupSigningDevice(message: String) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice(message))
        }
    }

    override fun showMnemonicToUser(message: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.ShowMnemonicToUser(message) {
            callback.invoke(true)
            })
        }
    }

    override fun sendingSeedToHardware() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.SendingSeedToHardware)
        }
    }

    override fun signingDeviceSuccessfullySet() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.SigningDeviceSuccessfullySet)
            switchTabTo(false)
        }
    }


    override val sideEffectContainer: SideEffectContainer<Context, ProfileSideEffect>
        get() = super.sideEffectContainer
}
