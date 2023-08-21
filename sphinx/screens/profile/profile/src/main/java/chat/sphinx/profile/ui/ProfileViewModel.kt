package chat.sphinx.profile.ui

import android.app.Application
import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toEntropy
import cash.z.ecc.android.bip39.toSeed
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_network_query_crypter.NetworkQueryCrypter
import chat.sphinx.concept_network_query_crypter.model.SendSeedDto
import chat.sphinx.concept_network_query_relay_keys.NetworkQueryRelayKeys
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import chat.sphinx.menu_bottom_profile_pic.UpdatingImageViewState
import chat.sphinx.profile.R
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.SphinxCallLink
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.PrivatePhoto
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.isOnionAddress
import chat.sphinx.wrapper_relay.toRelayUrl
import chat.sphinx.wrapper_rsa.RsaPublicKey
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPackDynamicSerializer
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
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.clazzes.clear
import io.matthewnelson.crypto_common.extensions.encodeToByteArray
import io.matthewnelson.crypto_common.extensions.toHex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.base64.encodeBase64
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import uniffi.sphinxrs.deriveSharedSecret
import uniffi.sphinxrs.encrypt
import uniffi.sphinxrs.pubkeyFromSecretKey
import java.security.SecureRandom
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import uniffi.sphinxrs.Keys
import uniffi.sphinxrs.VlsResponse
import uniffi.sphinxrs.makeAuthToken
import uniffi.sphinxrs.nodeKeys
import java.util.Date

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
    private val networkQueryCrypter: NetworkQueryCrypter,
    private val relayDataHandler: RelayDataHandler,
    private val torManager: TorManager,
    private val LOG: SphinxLogger,
    private val walletDataHandler: WalletDataHandler,
    private val moshi: Moshi
    ): SideEffectViewModel<
        Context,
        ProfileSideEffect,
        ProfileViewState>(dispatchers, ProfileViewState.Basic),
    PictureMenuViewModel
{
    companion object {
        const val SIGNING_DEVICE_SHARED_PREFERENCES = "general_settings"
        const val SIGNING_DEVICE_SETUP_KEY = "signing-device-setup"

        const val BITCOIN_NETWORK_REG_TEST = "regtest"
        const val BITCOIN_NETWORK_MAIN_NET = "mainnet"
    }

    val storageBarViewStateContainer: ViewStateContainer<StorageBarViewState> by lazy {
        ViewStateContainer(StorageBarViewState.Loading)
    }

    val updatingImageViewStateContainer: ViewStateContainer<UpdatingImageViewState> by lazy {
        ViewStateContainer(UpdatingImageViewState.Idle)
    }

    val clientID = "asdkjahsdkajshdkjsadh"
    val lssN = generateRandomBytes().take(32)
    val muts: MutableMap<String, ByteArray> = mutableMapOf()
    val sequence: UShort? = null

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

    private var setupSigningDeviceJob: Job? = null
    private var seedDto = SendSeedDto()

    private fun resetSeedDto() {
        seedDto = SendSeedDto()
    }

    fun setupSigningDevice() {
//        if (setupSigningDeviceJob?.isActive == true) return
//
//        setupSigningDeviceJob = viewModelScope.launch(mainImmediate) {
//            submitSideEffect(ProfileSideEffect.CheckNetwork {
//                viewModelScope.launch(mainImmediate) {
//                    submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
//                        app.getString(R.string.network_name_title),
//                        app.getString(R.string.network_name_message)
//                    ) { networkName ->
//                        viewModelScope.launch(mainImmediate) {
//                            if (networkName == null) {
//                                submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Network can not be empty"))
//                                return@launch
//                            }
//
//                            seedDto.ssid = networkName
//
//                            submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
//                                app.getString(R.string.network_password_title),
//                                app.getString(
//                                    R.string.network_password_message,
//                                    networkName ?: "-"
//                                ),
//                                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
//                            ) { networkPass ->
//                                viewModelScope.launch(mainImmediate) {
//                                    if (networkPass == null) {
//                                        submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Network password can not be empty"))
//                                        return@launch
//                                    }
//
//                                    seedDto.pass = networkPass
//
//                                    submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
//                                        app.getString(R.string.lightning_node_url_title),
//                                        app.getString(R.string.lightning_node_url_message),
//                                    ) { lightningNodeUrl ->
//                                        viewModelScope.launch(mainImmediate) {
//                                            if (lightningNodeUrl == null) {
//                                                submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Lightning node URL can not be empty"))
//                                                return@launch
//                                            }
//
//                                            seedDto.lightningNodeUrl = lightningNodeUrl
//
//                                            submitSideEffect(ProfileSideEffect.CheckBitcoinNetwork(
//                                                regTestCallback = {
//                                                    seedDto.network = BITCOIN_NETWORK_REG_TEST
//                                                }, mainNetCallback = {
//                                                    seedDto.network = BITCOIN_NETWORK_MAIN_NET
//                                                }, callback = {
//                                                    viewModelScope.launch(mainImmediate) {
//                                                        linkSigningDevice()
//                                                    }
//                                                }
//                                            ))
//                                        }
//                                    })
//                                }
//                            })
//                        }
//                    })
//                }
//            })
//        }
        start()
    }

    private fun start() {
        viewModelScope.launch(mainImmediate) {
            val (seed, mnemonic) = generateAndPersistMnemonic()

            val keys: Keys? = try {
                nodeKeys(net = "regtest", seed = seed!!)
            } catch (e: Exception) {
                println(e.message)
                null
            }

            val password: String? = try {
                makeAuthToken(ts = (Date().time / 1000).toUInt(), secret = keys?.secret!!)
            } catch (e: Exception) {
                println(e.message)
                null
            }

            if (keys != null && password != null) {
                connectToMQTTWith(keys, password)
            }
        }
}

    object Topics {
        const val VLS = "vls"
        const val VLS_RES = "vls-res"
        const val CONTROL = "control"
        const val CONTROL_RES = "control-res"
        const val PROXY = "proxy"
        const val PROXY_RES = "proxy-res"
        const val ERROR = "error"
        const val INIT_1_MSG = "init-1-msg"
        const val INIT_1_RES = "init-1-res"
        const val INIT_2_MSG = "init-2-msg"
        const val INIT_2_RES = "init-2-res"
        const val LSS_MSG = "lss-msg"
        const val LSS_RES = "lss-res"
        const val HELLO = "hello"
        const val BYE = "bye"
    }

    private fun connectToMQTTWith(keys: Keys, password: String) {
        val serverURI = "tcp://192.168.0.199:1883"
        val mqttClient = MqttClient(serverURI, clientID, null)

        val options = MqttConnectOptions().apply {
            this.userName = keys.pubkey
            this.password = password.toCharArray()
        }

        try {
            mqttClient.connect(options)

            if (mqttClient.isConnected) {
                Log.d("MQTT", "Connected!")

                val topics = arrayOf(
                    "${clientID}/${Topics.VLS}",
                    "${clientID}/${Topics.INIT_1_MSG}",
                    "${clientID}/${Topics.INIT_2_MSG}",
                    "${clientID}/${Topics.LSS_MSG}"
                )
                val qos = IntArray(topics.size) { 1 }

                mqttClient.subscribe(topics, qos)

                Log.d("MQTT", "Subscribed!")

                val topic = "$clientID/${Topics.HELLO}"
                val message = MqttMessage()

                mqttClient.publish(topic, message)


            } else {
                Log.d("MQTT", "Failed to connect!")
            }

            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.d("MQTT", "Connection lost!")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.payload ?: byteArrayOf()
                    Log.d(
                        "MQTT",
                        "Message received in topic $topic with payload ${String(payload)}"
                    )
                    val modifiedTopic = topic?.replace("${clientID}/", "") ?: ""

                    processMessage(modifiedTopic, payload, mqttClient)

                    if (topic?.contains("init-2-msg") == true) {
                        Log.d("MQTT", "init-2-msg received")
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {

                }
            })

        } catch (e: MqttException) {
            e.printStackTrace()
            Log.d("MQTT", "connectToMQTTWith: Error ${e.printStackTrace()}")
        }
    }

    fun processMessage(topic: String, payload: ByteArray, mqttClient: MqttClient) {
        viewModelScope.launch(mainImmediate) {
            val (args, state) = argsAndState()

            var ret: VlsResponse? =
                try {
                    uniffi.sphinxrs.run(
                        topic,
                        args,
                        state,
                        payload,
                        sequence,
                    )
                } catch (e: Exception) {
                    println(e.message)
                    Log.d("MQTT", "processMessage: Error ${e.message}")
                    null
                }
            Log.d("MQTT", "params on run: topic: ${topic} args: ${args} state: ${state} payload: ${payload}")

            ret?.let {
                storeMutations(it.state)
                mqttClient.publish("${clientID}/${it.topic}", MqttMessage(it.bytes))

                Log.d("MQTT", "PUBLISH WITH TOPIC: ${it.topic}")

                //Increment sequence if needed
            }
        }
    }

    private suspend fun argsAndState(): Pair<String, ByteArray> {
        val args = makeArgs()
        val stringArgs = argsToJson(args) ?: ""

        Log.d("MQTT", "MQTT JSON: $stringArgs ")

        val sta: Map<String, ByteArray> = loadMuts()

        Log.d("MQTT", "LOADMUTS $sta ")

        val state = MsgPack.encodeToByteArray(MsgPackDynamicSerializer, sta)

        return Pair(stringArgs, state)
    }

    private fun loadMuts(): Map<String, ByteArray> {
        return muts
    }

    private fun storeMutations(inc: ByteArray) {
        viewModelScope.launch(mainImmediate) {
            try {
                val decoded = MsgPack.decodeFromByteArray(MsgPackDynamicSerializer, inc)

                (decoded as? MutableMap<String, ByteArray>)?.let {
                    muts.putAll(it)

                    Log.d("MQTT", "mutStateFlow is: $it")
                } ?: run {
                    Log.d("MQTT", "Decoded data doesn't fit the expected types.")
                }
            } catch (e: Exception) {
                Log.d("MQTT", "Error during decoding or casting: ${e.message}")
            }
        }
    }

    private suspend fun makeArgs(): Map<String, Any>? {
        val seedBytes = generateAndPersistMnemonic().first?.encodeToByteArray()?.take(32)

        if (seedBytes == null) {
            Log.d("MQTT", "Seed vacio")
            return null
        }

        val defaultPolicy = mapOf(
            "msat_per_interval" to 21000000000L,
            "interval" to "daily",
            "htlc_limit_msat" to 1000000000L
        )

        val args = mapOf(
            "seed" to seedBytes,
            "network" to "regtest",
            "policy" to defaultPolicy,
            "allowlist" to emptyList<Any>(),
            "timestamp" to Date().time / 1000L,
            "lss_nonce" to lssN.map { it.toInt() }
        )

        Log.d("MQTT", "The args are: $args")

        return args
    }

    private fun argsToJson(map: Map<String, Any>?): String? {
        val adapter = moshi.adapter(Map::class.java)
        return adapter.toJson(map)
    }

    private fun generateRandomBytes(): UByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val uByteArray = UByteArray(32)
        for (i in bytes.indices) {
            uByteArray[i] = bytes[i].toUByte()
        }
        return uByteArray
    }

    private suspend fun linkSigningDevice() {
        val secKey = ByteArray(32)
        SecureRandom().nextBytes(secKey)

        val sk1 = secKey.toHex()
        val pk1 = pubkeyFromSecretKey(sk1)

        var pk2 : String? = null

        if (pk1 == null) {
            submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("error generating secret key"))
            resetSeedDto()
            return
        }

        seedDto.pubkey = pk1

        if (
            seedDto.lightningNodeUrl == null ||
            seedDto.lightningNodeUrl?.isEmpty() == true
        ) {
            resetSeedDto()
            submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("lightning node URL can't be empty"))
            return
        }

        networkQueryCrypter.getCrypterPubKey().collect { loadResponse ->
            when (loadResponse) {
                is LoadResponse.Loading -> {}
                is Response.Error -> {
                    resetSeedDto()
                    submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("error getting public key from hardware"))
                }
                is Response.Success -> {
                    pk2 = loadResponse.value.pubkey
                }
            }
        }

        pk2?.let { nnPk2 ->
            val sec1 = deriveSharedSecret(nnPk2, sk1)
            val seedAndMnemonic = generateAndPersistMnemonic()

            seedAndMnemonic.second?.let { mnemonic ->
                submitSideEffect(ProfileSideEffect.ShowMnemonicToUser(
                    mnemonic.value
                ) {
                    seedAndMnemonic.first?.let { seed ->
                        viewModelScope.launch(mainImmediate) {
                            encryptAndSendSeed(seed, sec1)
                        }
                    }
                })
            }
        }
    }

    private suspend fun generateAndPersistMnemonic() : Pair<String?, WalletMnemonic?> {
        var walletMnemonic: WalletMnemonic? = null
        var seed: String? = null

        viewModelScope.launch(mainImmediate) {
            walletMnemonic = walletDataHandler.retrieveWalletMnemonic() ?: run {
                val entropy = (Mnemonics.WordCount.COUNT_12).toEntropy()

                Mnemonics.MnemonicCode(entropy).use { mnemonicCode ->
                    val wordsArray:MutableList<String> = mutableListOf()
                    mnemonicCode.words.forEach { word ->
                        wordsArray.add(word.joinToString(""))
                    }
                    val words = wordsArray.joinToString(" ")

                    words.toWalletMnemonic()?.let { walletMnemonic ->
                        if (walletDataHandler.persistWalletMnemonic(walletMnemonic)) {
                            LOG.d("MNEMONIC WORDS SAVED" , words)
                            LOG.d("MNEMONIC WORDS SAVED" , words)
                        }
                        walletMnemonic
                    }
                }
            }

            walletMnemonic?.value?.toCharArray()?.let { words ->
                val mnemonic = Mnemonics.MnemonicCode(words)

                val seedData = mnemonic.toSeed().take(32).toByteArray()
                seed = seedData.toHex()
            }
        }.join()

        return Pair(seed, walletMnemonic)
    }

    private suspend fun encryptAndSendSeed(
        seed: String,
        sec1: String
    ) {
        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)

        encrypt(seed, sec1, nonce.toHex()).let { cipher ->
            if (cipher.isNotEmpty()) {
                seedDto.seed = cipher

                submitSideEffect(ProfileSideEffect.SendingSeedToHardware)

                networkQueryCrypter.sendEncryptedSeed(seedDto).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {
                            resetSeedDto()
                            submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("error sending seed to hardware"))
                        }

                        is Response.Success -> {
                            submitSideEffect(ProfileSideEffect.SigningDeviceSuccessfullySet)

                            setSigningDeviceSetupDone {
                                switchTabTo(false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isSigningDeviceSetupDone(): Boolean {
        val appContext: Context = app.applicationContext
        val sharedPreferences = appContext.getSharedPreferences(SIGNING_DEVICE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

        return sharedPreferences.getBoolean(
            SIGNING_DEVICE_SETUP_KEY,
            false
        )
    }

    private suspend fun setSigningDeviceSetupDone(
        callback: () -> Unit
    ) {
        val appContext: Context = app.applicationContext
        val sharedPreferences = appContext.getSharedPreferences(SIGNING_DEVICE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

        withContext(dispatchers.io) {
            sharedPreferences.edit().putBoolean(SIGNING_DEVICE_SETUP_KEY, true)
                .let { editor ->
                    if (!editor.commit()) {
                        editor.apply()
                    }
                    callback.invoke()
                }
        }
    }
}
