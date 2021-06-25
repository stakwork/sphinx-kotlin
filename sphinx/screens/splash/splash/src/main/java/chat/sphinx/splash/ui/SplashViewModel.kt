package chat.sphinx.splash.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_invite.model.RedeemInviteDto
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.key_restore.KeyRestore
import chat.sphinx.key_restore.KeyRestoreResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.splash.navigation.SplashNavigator
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.extensions.decodeToString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.base64.decodeBase64ToArray
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    dispatchers: CoroutineDispatchers,
    private val keyRestore: KeyRestore,
    private val lightningRepository: LightningRepository,
    private val memeServerTokenHandler: MemeServerTokenHandler,
    private val navigator: SplashNavigator,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    private val networkQueryInvite: NetworkQueryInvite,
    private val networkQueryContact: NetworkQueryContact,
    private val torManager: TorManager,
    private val app: Application,
): MotionLayoutViewModel<
        Any,
        Context,
        SplashSideEffect,
        SplashViewState
        >(dispatchers, SplashViewState.Start_ShowIcon)
{

    private var screenInit: Boolean = false
    fun screenInit() {
        if (screenInit) {
            return
        } else {
            screenInit = true
        }

        // prime the account balance retrieval from SharePrefs
        viewModelScope.launch(mainImmediate) {
            lightningRepository.getAccountBalance().firstOrNull()
        }

        viewModelScope.launch(mainImmediate) {
            backgroundLoginHandler.attemptBackgroundLogin(
                updateLastLoginTimeOnSuccess = true
            )?.let {
                navigator.toDashboardScreen(
                    // No need as it was already updated
                    updateBackgroundLoginTime = false
                )
            } ?: let {
                if (authenticationCoordinator.isAnEncryptionKeySet()) {
                    authenticationCoordinator.submitAuthenticationRequest(
                        AuthenticationRequest.LogIn()
                    ).firstOrNull().let { response ->
                        @Exhaustive
                        when (response) {
                            null,
                            is AuthenticationResponse.Failure -> {
                                // will not be returned as back press for handling
                                // a LogIn request minimizes the application until
                                // User has authenticated
                            }
                            is AuthenticationResponse.Success.Authenticated -> {
                                navigator.toDashboardScreen(updateBackgroundLoginTime = true)
                            }
                            is AuthenticationResponse.Success.Key -> {
                                // will never be returned
                            }
                        }
                    }
                } else {
                    // Display OnBoard
                    delay(100L) // need a slight delay for window to fully hand over to splash
                    updateViewState(SplashViewState.Transition_Set2_ShowWelcome)
                }
            }
        }
    }

    // TODO: Use coordinator pattern and limit
    fun navigateToScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(
                    filter = object : ScannerFilter() {
                        override suspend fun checkData(data: String): Response<Any, String> {
                            if (data.isInviteCode) {
                                return Response.Success(Any())
                            }

                            data.decodeBase64ToArray()
                                ?.decodeToString()
                                ?.split("::")
                                ?.let { decodedSplit ->

                                    if (decodedSplit.size == 3) {
                                        if (decodedSplit[0] == "ip") {
                                            return Response.Success(Any())
                                        }
                                    }

                                }

                            // TODO: make better
                            return Response.Error("QR code is not an account restore code")
                        }
                    }
                )
            )
            if (response is Response.Success) {
                submitSideEffect(SplashSideEffect.FromScanner(response.value))
            }
        }
    }

    private inline val String.isInviteCode: Boolean
        get() = matches("^[A-F0-9a-f]{40}\$".toRegex())

    fun processConnectionCode(input: String?) {
        if (input.isNullOrEmpty()) {
            viewModelScope.launch(mainImmediate) {
                updateViewState(SplashViewState.HideLoadingWheel)
                submitSideEffect(SplashSideEffect.InputNullOrEmpty)
            }
            return
        }

        // Maybe we can have a SignupStyle class to reflect this? Since there's a lot of decoding
        // going on in different classes
        // Invite Code
        if (input.isInviteCode) {
            viewModelScope.launch(mainImmediate) {
                redeemInvite(input)
            }
            return
        }

        input.decodeBase64ToArray()?.decodeToString()?.split("::")?.let { decodedSplit ->
            if (decodedSplit.size == 3) {
                //Token coming from Umbrel for example.
                if (decodedSplit.elementAt(0) == "ip") {
                    viewModelScope.launch(mainImmediate) {
                        storeTemporaryInvite()

                        val ip = decodedSplit.elementAt(1)
                        val password = decodedSplit.elementAt(2)
                        generateToken(ip, null, password)
                    }
                    return
                }
            } else if (decodedSplit.size == 2) {
                if (decodedSplit.elementAt(0) == "keys") {
                    decodedSplit.elementAt(1).decodeBase64ToArray()?.let { toDecryptByteArray ->
                        updateViewState(
                            SplashViewState.Transition_Set3_DecryptKeys(toDecryptByteArray)
                        )
                        return
                    }
                }

            } // input not properly formatted `type::data`
        }

        viewModelScope.launch(mainImmediate) {
            updateViewState(SplashViewState.HideLoadingWheel)
            submitSideEffect(SplashSideEffect.InvalidCode)
        }
    }

    private fun redeemInvite(input: String) {
        viewModelScope.launch(mainImmediate) {
            networkQueryInvite.redeemInvite(input).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        updateViewState(SplashViewState.HideLoadingWheel)
                        submitSideEffect(SplashSideEffect.InvalidCode)
                    }
                    is Response.Success -> {
                        val inviteResponse = loadResponse.value.response

                        inviteResponse?.invite?.let { invite ->
                            storeTemporaryInvite(invite)

                            generateToken(inviteResponse.ip, inviteResponse.pubkey, null)
                        }
                    }
                }
            }
        }
    }

    private fun generateToken(ip: String, nodePubKey: String? = null, password: String? = null) {
        viewModelScope.launch(mainImmediate) {
            val authToken = generateRandomToken()
            val relayUrl = parseRelayUrl(RelayUrl(ip))

            storeToken(authToken.value, relayUrl.value)

            networkQueryContact.generateToken(relayUrl, authToken, password, nodePubKey).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        updateViewState(SplashViewState.HideLoadingWheel)
                        submitSideEffect(SplashSideEffect.GenerateTokenFailed)
                    }
                    is Response.Success -> {
                        viewModelScope.launch(mainImmediate) {
                            navigator.toOnBoardScreen()
                        }
                    }
                }
            }
        }
    }

    // I had to copy this from the RelayDataHandlerImpl class, I needed this methods outside
    // but I wasn't sure where to put them
    private inline val String.isOnionAddress: Boolean
        get() = matches("([a-z2-7]{56}).onion.*".toRegex())

    private suspend fun parseRelayUrl(relayUrl: RelayUrl): RelayUrl {
        return try {
            val httpUrl = relayUrl.value.toHttpUrl()
            torManager.setTorRequired(httpUrl.host.isOnionAddress)

            // is a valid url with scheme
            relayUrl
        } catch (e: IllegalArgumentException) {

            // does not contain http, https... check if it's an onion address
            if (relayUrl.value.isOnionAddress) {
                // only use http if it is an onion address
                torManager.setTorRequired(true)
                RelayUrl("http://${relayUrl.value}")
            } else {
                torManager.setTorRequired(false)
                RelayUrl("https://${relayUrl.value}")
            }
        }
    }

    private fun generateRandomToken(): AuthorizationToken {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val token = (1..20)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")

        return AuthorizationToken(token)
    }

    private fun storeTemporaryInvite(invite: RedeemInviteDto? = null) {
        // I needed a way to store this to transition between fragments
        // but I wasn't sure what to use besides this
        app.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE).let {
                sharedPrefs ->
            sharedPrefs?.edit()?.let { editor ->
                invite?.let { invite ->
                    // Signing up with invite code. Inviter is coming from relay
                    editor
                        .putString("sphinx_temp_inviter_nickname", invite.nickname)
                        .putString("sphinx_temp_inviter_pubkey", invite.pubkey)
                        .putString("sphinx_temp_inviter_route_hint", invite.route_hint)
                        .putString("sphinx_temp_invite_message", invite.message)
                        .putString("sphinx_temp_invite_action", invite.action)
                        .let { editor ->
                            if (!editor.commit()) {
                                editor.apply()
                            }
                        }
                } ?: run {
                    // Signing up relay connection string. Using default inviter
                    editor
                        .putString("sphinx_temp_inviter_nickname", "Sphinx Support")
                        .putString("sphinx_temp_inviter_pubkey", "023d70f2f76d283c6c4e58109ee3a2816eb9d8feb40b23d62469060a2b2867b77f")
                        .putString("sphinx_temp_invite_message", "Welcome to Sphinx")
                        .let { editor ->
                            if (!editor.commit()) {
                                editor.apply()
                            }
                        }
                }
            }
        }
    }

    private fun storeToken(token: String, ip: String) {
        // I needed a way to store this to transition between fragments
        // but I wasn't sure what to use besides this
        app.getSharedPreferences("sphinx_temp_prefs", Context.MODE_PRIVATE).let { sharedPrefs ->

            sharedPrefs?.edit()?.let { editor ->
                editor
                    .putString("sphinx_temp_ip", ip)
                    .putString("sphinx_temp_auth_token", token)
                    .let { editor ->
                        if (!editor.commit()) {
                            editor.apply()
                        }
                    }
            }
        }
    }

    private var decryptionJob: Job? = null
    fun decryptInput(viewState: SplashViewState.Set3_DecryptKeys) {
        // TODO: Replace with automatic launching upon entering the 6th PIN character
        //  when Authentication View's Layout gets incorporated
        if (viewState.pinWriter.size() != 6 /*TODO: https://github.com/stakwork/sphinx-kotlin/issues/9*/) {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(SplashSideEffect.InvalidPinLength)
            }
        }

        if (decryptionJob?.isActive == true) {
            return
        }

        var decryptionJobException: Exception? = null
        decryptionJob = viewModelScope.launch(default) {
            try {
                val pin = viewState.pinWriter.toCharArray()
                val decryptedSplit = AES256JNCryptor()
                    .decryptData(viewState.toDecrypt, pin)
                    .decodeToString()
                    .split("::")

                if (decryptedSplit.size != 4) {
                    throw IllegalArgumentException("Decrypted keys do not contain enough arguments")
                }

                // TODO: Ask to use Tor before any network calls go out.
                // TODO: Hit relayUrl to verify creds work

                var success: KeyRestoreResponse.Success? = null
                keyRestore.restoreKeys(
                    privateKey = Password(decryptedSplit[0].toCharArray()),
                    publicKey = Password(decryptedSplit[1].toCharArray()),
                    userPin = pin,
                    relayUrl = RelayUrl(decryptedSplit[2]),
                    authorizationToken = AuthorizationToken(decryptedSplit[3]),
                ).collect { flowResponse ->
                    // TODO: Implement in Authentication View when it get's built/refactored
                    if (flowResponse is KeyRestoreResponse.Success) {
                        success = flowResponse
                    }
                }

                success?.let { _ ->
                    // Overwrite PIN
                    viewState.pinWriter.reset()
                    repeat(6) {
                        viewState.pinWriter.append('0')
                    }

                    navigator.toDashboardScreen(updateBackgroundLoginTime = true)

                } ?: updateViewState(
                    SplashViewState.Set3_DecryptKeys(viewState.toDecrypt)
                ).also {
                    submitSideEffect(SplashSideEffect.InvalidPin)
                }

                // TODO: on success, show snackbar to clear clipboard
            } catch (e: CryptorException) {
                decryptionJobException = e
            } catch (e: IllegalArgumentException) {
                decryptionJobException = e
            }
        }

        viewModelScope.launch(mainImmediate) {
            decryptionJob?.join()
            decryptionJobException?.let { exception ->
                updateViewState(
                    // reset view state
                    SplashViewState.Set3_DecryptKeys(viewState.toDecrypt)
                )
                exception.printStackTrace()
                submitSideEffect(SplashSideEffect.DecryptionFailure)
            }
        }
    }

    // Unused
    override suspend fun onMotionSceneCompletion(value: Any) {
        return
    }
}
