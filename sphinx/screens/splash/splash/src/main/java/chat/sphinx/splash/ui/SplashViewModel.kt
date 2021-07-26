package chat.sphinx.splash.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_invite.model.RedeemInviteDto
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.key_restore.KeyRestore
import chat.sphinx.key_restore.KeyRestoreResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.splash.model.RedemptionCode
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.splash.navigation.SplashNavigator
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_invite.InviteString
import chat.sphinx.wrapper_invite.toValidInviteStringOrNull
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.isOnionAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.PasswordGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    dispatchers: CoroutineDispatchers,
    private val keyRestore: KeyRestore,
    private val lightningRepository: LightningRepository,
    private val navigator: SplashNavigator,
    private val networkQueryInvite: NetworkQueryInvite,
    private val networkQueryContact: NetworkQueryContact,
    private val onBoardStepHandler: OnBoardStepHandler,
    private val relayDataHandler: RelayDataHandler,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    private val torManager: TorManager,
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
            val onBoardStep: OnBoardStep? = onBoardStepHandler.retrieveOnBoardStep()

            backgroundLoginHandler.attemptBackgroundLogin(
                updateLastLoginTimeOnSuccess = true
            )?.let {

                @Exhaustive
                when (onBoardStep) {
                    is OnBoardStep.Step1 -> {
                        navigator.toOnBoardScreen(onBoardStep)
                    }
                    is OnBoardStep.Step2 -> {
                        navigator.toOnBoardNameScreen(onBoardStep)
                    }
                    is OnBoardStep.Step3 -> {
                        navigator.toOnBoardReadyScreen(onBoardStep)
                    }
                    null -> {
                        navigator.toDashboardScreen(
                            // No need as it was already updated
                            updateBackgroundLoginTime = false
                        )
                    }
                }

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

                                @Exhaustive
                                when (onBoardStep) {
                                    is OnBoardStep.Step1 -> {
                                        navigator.toOnBoardScreen(onBoardStep)
                                    }
                                    is OnBoardStep.Step2 -> {
                                        navigator.toOnBoardNameScreen(onBoardStep)
                                    }
                                    is OnBoardStep.Step3 -> {
                                        navigator.toOnBoardReadyScreen(onBoardStep)
                                    }
                                    null -> {
                                        navigator.toDashboardScreen(updateBackgroundLoginTime = true)
                                    }
                                }
                            }
                            is AuthenticationResponse.Success.Key -> {
                                // will never be returned
                            }
                        }
                    }

                } else {

                    @Exhaustive
                    when (onBoardStep) {
                        is OnBoardStep.Step1 -> {
                            navigator.toOnBoardScreen(onBoardStep)
                        }
                        is OnBoardStep.Step2 -> {
                            navigator.toOnBoardNameScreen(onBoardStep)
                        }
                        is OnBoardStep.Step3 -> {
                            navigator.toOnBoardReadyScreen(onBoardStep)
                        }
                        null -> {
                            // Display OnBoard
                            delay(100L) // need a slight delay for window to fully hand over to splash
                            updateViewState(SplashViewState.Transition_Set2_ShowWelcome)
                        }
                    }

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
                            if (data.toValidInviteStringOrNull() != null) {
                                return Response.Success(Any())
                            }

                            if (RedemptionCode.decode(data) != null) {
                                return Response.Success(Any())
                            }

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

    private var processConnectionCodeJob: Job? = null
    fun processConnectionCode(input: String?) {
        if (processConnectionCodeJob?.isActive == true) {
            return
        }

        processConnectionCodeJob = viewModelScope.launch(mainImmediate) {

            if (input == null || input.isEmpty()) {
                updateViewState(SplashViewState.HideLoadingWheel)
                submitSideEffect(SplashSideEffect.InputNullOrEmpty)
                return@launch
            }

            // Maybe we can have a SignupStyle class to reflect this? Since there's a lot of decoding
            // going on in different classes
            // Invite Code
            input.toValidInviteStringOrNull()?.let { inviteString ->
                redeemInvite(inviteString)
                return@launch
            }

            RedemptionCode.decode(input)?.let { code ->
                @Exhaustive
                when (code) {
                    is RedemptionCode.AccountRestoration -> {
                        updateViewState(
                            SplashViewState.Transition_Set3_DecryptKeys(code)
                        )
                    }
                    is RedemptionCode.NodeInvite -> {
                        registerTokenAndStartOnBoard(
                            ip = code.ip,
                            nodePubKey = null,
                            password = code.password,
                            redeemInviteDto = null
                        )
                    }
                }

                return@launch
            }

            updateViewState(SplashViewState.HideLoadingWheel)
            submitSideEffect(SplashSideEffect.InvalidCode)
        }
    }

    private suspend fun redeemInvite(input: InviteString) {
        networkQueryInvite.redeemInvite(input).collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {}
                is Response.Error -> {
                    updateViewState(SplashViewState.HideLoadingWheel)
                    submitSideEffect(SplashSideEffect.InvalidCode)
                }
                is Response.Success -> {
                    val inviteResponse = loadResponse.value.response

                    inviteResponse?.invite?.let { invite ->
                        registerTokenAndStartOnBoard(
                            ip = RelayUrl(inviteResponse.ip),
                            nodePubKey = inviteResponse.pubkey,
                            password = null,
                            redeemInviteDto = invite,
                        )
                    }
                }
            }
        }
    }

    private suspend fun registerTokenAndStartOnBoard(
        ip: RelayUrl,
        nodePubKey: String?,
        password: String?,
        redeemInviteDto: RedeemInviteDto?,
    ) {
        @OptIn(RawPasswordAccess::class)
        val authToken = AuthorizationToken(
            PasswordGenerator(passwordLength = 20).password.value.joinToString("")
        )

        val relayUrl = relayDataHandler.formatRelayUrl(ip)
        torManager.setTorRequired(relayUrl.isOnionAddress)

        val inviterData: OnBoardInviterData? = redeemInviteDto?.let { dto ->
            OnBoardInviterData(
                dto.nickname,
                dto.pubkey?.toLightningNodePubKey(),
                dto.route_hint,
                dto.message,
                dto.action,
                dto.pin
            )
        }

        val step1: OnBoardStep.Step1? = onBoardStepHandler.persistOnBoardStep1Data(
            relayUrl,
            authToken,
            inviterData
        )

        networkQueryContact.generateToken(relayUrl, authToken, password, nodePubKey).collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {}
                is Response.Error -> {
                    updateViewState(SplashViewState.HideLoadingWheel)
                    submitSideEffect(SplashSideEffect.GenerateTokenFailed)
                }
                is Response.Success -> {

                    if (step1 == null) {
                        updateViewState(SplashViewState.HideLoadingWheel)
                        submitSideEffect(SplashSideEffect.GenerateTokenFailed)
                    } else {
                        navigator.toOnBoardScreen(step1)
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

                val decryptedCode: RedemptionCode.AccountRestoration.DecryptedRestorationCode =
                    viewState.restoreCode.decrypt(pin, dispatchers)

                // TODO: Ask to use Tor before any network calls go out.
                // TODO: Hit relayUrl to verify creds work

                var success: KeyRestoreResponse.Success? = null
                keyRestore.restoreKeys(
                    privateKey = decryptedCode.privateKey,
                    publicKey = decryptedCode.publicKey,
                    userPin = pin,
                    relayUrl = decryptedCode.relayUrl,
                    authorizationToken = decryptedCode.authorizationToken,
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
                    SplashViewState.Set3_DecryptKeys(viewState.restoreCode)
                ).also {
                    submitSideEffect(SplashSideEffect.InvalidPin)
                }

            } catch (e: Exception) {
                decryptionJobException = e
            }
        }

        viewModelScope.launch(mainImmediate) {
            decryptionJob?.join()
            decryptionJobException?.let { exception ->
                updateViewState(
                    // reset view state
                    SplashViewState.Set3_DecryptKeys(viewState.restoreCode)
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
