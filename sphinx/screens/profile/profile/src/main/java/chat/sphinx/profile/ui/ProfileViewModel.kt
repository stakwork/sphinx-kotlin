package chat.sphinx.profile.ui

import android.app.Application
import android.content.Context
import android.text.InputType
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
import chat.sphinx.wrapper_common.FeedRecommendationsToggle
import chat.sphinx.wrapper_common.PreviewsEnabled
import chat.sphinx.wrapper_common.isTrue
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.SphinxCallLink
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.PrivatePhoto
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import chat.sphinx.wrapper_relay.*
import chat.sphinx.wrapper_rsa.RsaPublicKey
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
import io.matthewnelson.crypto_common.clazzes.*
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
    private val networkQueryRelayKeys: NetworkQueryRelayKeys,
    private val networkQueryCrypter: NetworkQueryCrypter,
    private val relayDataHandler: RelayDataHandler,
    private val torManager: TorManager,
    private val LOG: SphinxLogger,
    private val walletDataHandler: WalletDataHandler,
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

    val updatingImageViewStateContainer: ViewStateContainer<UpdatingImageViewState> by lazy {
        ViewStateContainer(UpdatingImageViewState.Idle)
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
        if (setupSigningDeviceJob?.isActive == true) return

        setupSigningDeviceJob = viewModelScope.launch(mainImmediate) {
            submitSideEffect(ProfileSideEffect.CheckNetwork {
                viewModelScope.launch(mainImmediate) {
                    submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
                        app.getString(R.string.network_name_title),
                        app.getString(R.string.network_name_message)
                    ) { networkName ->
                        viewModelScope.launch(mainImmediate) {
                            if (networkName == null) {
                                submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Network can not be empty"))
                                return@launch
                            }

                            seedDto.ssid = networkName

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
                                    }

                                    seedDto.pass = networkPass

                                    submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
                                        app.getString(R.string.lightning_node_ip_title),
                                        app.getString(R.string.lightning_node_ip_message),
                                    ) { lightningNodeIP ->
                                        viewModelScope.launch(mainImmediate) {
                                            if (lightningNodeIP == null) {
                                                submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Lightning node IP can not be empty"))
                                                return@launch
                                            }

                                            seedDto.lightningNodeIP = lightningNodeIP

                                            submitSideEffect(ProfileSideEffect.SigningDeviceInfo(
                                                app.getString(R.string.lightning_node_port_title),
                                                app.getString(R.string.lightning_node_port_message),
                                                "1883"
                                            ) { lightningNodePort ->

                                                viewModelScope.launch(mainImmediate) {

                                                    if (lightningNodePort == null) {
                                                        submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("Lightning node port can not be empty"))
                                                        return@launch
                                                    }

                                                    seedDto.lightningNodePort = lightningNodePort

                                                    submitSideEffect(ProfileSideEffect.CheckBitcoinNetwork(
                                                        regTestCallback = {
                                                            seedDto.network = BITCOIN_NETWORK_REG_TEST
                                                        }, mainNetCallback = {
                                                            seedDto.network = BITCOIN_NETWORK_MAIN_NET
                                                        }, callback = {
                                                            viewModelScope.launch(mainImmediate) {
                                                                linkSigningDevice()
                                                            }
                                                        }
                                                    ))
                                                }
                                            })
                                        }
                                    })
                                }
                            })
                        }
                    })
                }
            })
        }
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
            seedDto.lightningNodeIP == null ||
            seedDto.lightningNodeIP?.isEmpty() == true
        ) {
            resetSeedDto()
            submitSideEffect(ProfileSideEffect.FailedToSetupSigningDevice("lightning node IP can't be empty"))
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

        encrypt(seed, sec1, nonce.toHex())?.let { cipher ->
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
