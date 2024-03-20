package chat.sphinx.onboard_connect.ui

import android.app.Application
import android.content.Context
import android.text.InputType
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_crypter.NetworkQueryCrypter
import chat.sphinx.concept_repository_connect_manager.ConnectManagerRepository
import chat.sphinx.concept_signer_manager.SignerHardwareCallback
import chat.sphinx.concept_signer_manager.SignerManager
import chat.sphinx.concept_signer_manager.SignerPhoneCallback
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_phone_signer_method.PhoneSignerMethodMenuHandler
import chat.sphinx.menu_bottom_phone_signer_method.PhoneSignerMethodMenuViewModel
import chat.sphinx.menu_bottom_signer.SignerMenuHandler
import chat.sphinx.menu_bottom_signer.SignerMenuViewModel
import chat.sphinx.onboard_connect.navigation.OnBoardConnectNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_invite.toValidInviteStringOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import chat.sphinx.onboard_common.model.RedemptionCode
import chat.sphinx.onboard_connect.R
import chat.sphinx.onboard_connect.viewstate.MnemonicDialogViewState
import chat.sphinx.onboard_connect.viewstate.MnemonicWordsViewState
import chat.sphinx.onboard_connect.viewstate.OnBoardConnectSubmitButtonViewState
import chat.sphinx.onboard_connect.viewstate.OnBoardConnectViewState
import chat.sphinx.resources.MnemonicLanguagesUtils
import com.squareup.moshi.Moshi
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject


internal inline val OnBoardConnectFragmentArgs.newUser: Boolean
    get() = argNewUser

@HiltViewModel
internal class OnBoardConnectViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    private val walletDataHandler: WalletDataHandler,
    private val networkQueryCrypter: NetworkQueryCrypter,
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val connectManagerRepository: ConnectManagerRepository,
    private val app: Application,
    val moshi: Moshi,
    val navigator: OnBoardConnectNavigator
    ): SideEffectViewModel<
        Context,
        OnBoardConnectSideEffect,
        OnBoardConnectViewState
        >(dispatchers, OnBoardConnectViewState.Idle),
    SignerMenuViewModel,
    PhoneSignerMethodMenuViewModel,
    SignerHardwareCallback,
    SignerPhoneCallback
{

    private val args: OnBoardConnectFragmentArgs by handle.navArgs()
    private lateinit var signerManager: SignerManager

    companion object {
        const val BITCOIN_NETWORK_REG_TEST = "regtest"
        const val BITCOIN_NETWORK_MAIN_NET = "mainnet"
    }

    val submitButtonViewStateContainer: ViewStateContainer<OnBoardConnectSubmitButtonViewState> by lazy {
        ViewStateContainer(OnBoardConnectSubmitButtonViewState.Disabled)
    }

    val mnemonicWordsViewStateContainer: ViewStateContainer<MnemonicWordsViewState> by lazy {
        ViewStateContainer(MnemonicWordsViewState.Closed)
    }

    val mnemonicDialogViewStateContainer: ViewStateContainer<MnemonicDialogViewState> by lazy {
        ViewStateContainer(MnemonicDialogViewState.Idle)
    }

    init {
        updateViewState(
            if (args.newUser) {
                OnBoardConnectViewState.NewUser
            } else {
                OnBoardConnectViewState.ExistingUser
            }
        )
    }

    fun validateCode(code: String) {
        val vs = currentViewState
        val redemptionCode = RedemptionCode.decode(code)
        var isValid = false

        if (vs is OnBoardConnectViewState.NewUser) {
            if (code.toValidInviteStringOrNull() != null) {
                isValid = true
            }
            if (redemptionCode != null &&
                redemptionCode is RedemptionCode.NodeInvite) {
                isValid = true
            }
            if (redemptionCode != null &&
                redemptionCode is RedemptionCode.SwarmConnect) {
                isValid = true
            }
            if (redemptionCode != null &&
                redemptionCode is RedemptionCode.SwarmClaim) {
                isValid = true
            }
            if (redemptionCode != null &&
                redemptionCode is RedemptionCode.Glyph) {
                signerManager.setSeedFromGlyph(
                    redemptionCode.mqtt,
                    redemptionCode.network,
                    redemptionCode.relay
                )
                isValid = true
            }
            if (redemptionCode != null &&
                redemptionCode is RedemptionCode.NewInvite) {
                isValid = true
            }

        } else if (vs is OnBoardConnectViewState.ExistingUser) {
            if (redemptionCode != null &&
                redemptionCode is RedemptionCode.AccountRestoration) {
                isValid = true
            }
            if (redemptionCode != null &&
                redemptionCode is RedemptionCode.MnemonicRestoration) {
                isValid = true
            }
        }

        submitButtonViewStateContainer.updateViewState(
            if (isValid) {
                OnBoardConnectSubmitButtonViewState.Enabled
            } else {
                OnBoardConnectSubmitButtonViewState.Disabled
            }
        )
    }

    fun navigateToScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest()
            )
            if (response is Response.Success) {
                submitSideEffect(OnBoardConnectSideEffect.FromScanner(response.value))

                val code = response.value.value
                val redemptionCode = RedemptionCode.decode(code)
                validateCode(code)

                if (submitButtonViewStateContainer.value is OnBoardConnectSubmitButtonViewState.Enabled) {

                    if (redemptionCode is RedemptionCode.Glyph) {
                        signerManager.setSeedFromGlyph(
                            redemptionCode.mqtt,
                            redemptionCode.network,
                            redemptionCode.relay
                        )
                        signerMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
                    }
                    if (redemptionCode is RedemptionCode.NewInvite) {
                        connectManagerRepository.setInviteCode(code)
                        presentLoginModal()
                    }
                    else {
                        viewModelScope.launch(mainImmediate) {
                            navigator.toOnBoardConnectingScreen(code)
                        }
                    }
                }
            }
        }
    }

    fun continueToConnectingScreen(code: String) {
        val submitButtonVS = submitButtonViewStateContainer.value
        val redemptionCode = RedemptionCode.decode(code)

        if (submitButtonVS is OnBoardConnectSubmitButtonViewState.Enabled) {

            viewModelScope.launch(mainImmediate) {

                if (redemptionCode is RedemptionCode.Glyph) {
                    signerManager.setSeedFromGlyph(
                        redemptionCode.mqtt,
                        redemptionCode.network,
                        redemptionCode.relay
                    )
                    navigator.toOnBoardConnectingScreen(code)

                    signerMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
                }
                if (redemptionCode is RedemptionCode.NewInvite) {
                    connectManagerRepository.setInviteCode(code)
                    presentLoginModal()
                }

                if (redemptionCode is RedemptionCode.MnemonicRestoration) {
                    connectManagerRepository.setMnemonicWords(redemptionCode.mnemonic)
                    presentLoginModal()
                }
            }
        } else {
            viewModelScope.launch(mainImmediate) {
                val vs = currentViewState

                submitSideEffect(OnBoardConnectSideEffect.Notify(
                    msg = if (vs is OnBoardConnectViewState.NewUser) {
                        "Code is not a connection or invite code"
                    } else  {
                        "Code is not an account restore code"
                    }
                ))
            }
        }
    }

    private var loginJob: Job? = null
    fun presentLoginModal(
    ) {
        if (loginJob?.isActive == true) {
            return
        }

        loginJob = viewModelScope.launch(mainImmediate) {
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
                        navigator.toOnBoardConnectingScreen(null)
                    }
                    is AuthenticationResponse.Success.Key -> {
                        // will never be returned
                    }
                }
            }
        }
    }

    fun setSignerManager(signerManager: SignerManager) {
        signerManager.setWalletDataHandler(walletDataHandler)
        signerManager.setMoshi(moshi)
        signerManager.setNetworkQueryCrypter(networkQueryCrypter)

        this.signerManager = signerManager
    }

    override val signerMenuHandler: SignerMenuHandler by lazy {
        SignerMenuHandler()
    }

    override val phoneSignerMethodMenuHandler: PhoneSignerMethodMenuHandler by lazy {
        PhoneSignerMethodMenuHandler()
    }

    ///Signing Hardware device
    override fun setupHardwareSigner() {
        signerManager.setupSignerHardware(this)
    }

    override fun checkNetwork(callback: (Boolean) -> Unit) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(OnBoardConnectSideEffect.CheckNetwork {
                callback.invoke(true)
            })
        }
    }

    override fun signingDeviceNetwork(
        callback: (String) -> Unit
    ) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(OnBoardConnectSideEffect.SigningDeviceInfo(
                app.getString(R.string.network_name_title),
                app.getString(R.string.network_name_message)
            ) { networkName ->
                if (networkName == null) {
                    viewModelScope.launch(mainImmediate) {
                        submitSideEffect(OnBoardConnectSideEffect.FailedToSetupSigningDevice("Network can not be empty"))
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
            submitSideEffect(OnBoardConnectSideEffect.SigningDeviceInfo(
                app.getString(R.string.network_password_title),
                app.getString(
                    R.string.network_password_message,
                    networkName ?: "-"
                ),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ) { networkPass ->
                viewModelScope.launch(mainImmediate) {
                    if (networkPass == null) {
                        submitSideEffect(OnBoardConnectSideEffect.FailedToSetupSigningDevice("Network password can not be empty"))
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
            submitSideEffect(OnBoardConnectSideEffect.SigningDeviceInfo(
                app.getString(R.string.lightning_node_url_title),
                app.getString(R.string.lightning_node_url_message),
            ) { lightningNodeUrl ->
                viewModelScope.launch(mainImmediate) {
                    if (lightningNodeUrl == null) {
                        submitSideEffect(OnBoardConnectSideEffect.FailedToSetupSigningDevice("Lightning node URL can not be empty"))
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
            submitSideEffect(OnBoardConnectSideEffect.CheckBitcoinNetwork(
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
            submitSideEffect(OnBoardConnectSideEffect.FailedToSetupSigningDevice(message))
        }
    }

    override fun showMnemonicToUser(message: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(OnBoardConnectSideEffect.ShowMnemonicToUser(message) {
                callback.invoke(true)
            })
        }
    }

    override fun sendingSeedToHardware() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(OnBoardConnectSideEffect.SendingSeedToHardware)
        }
    }

    override fun signingDeviceSuccessfullySet() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(OnBoardConnectSideEffect.SigningDeviceSuccessfullySet)
        }
    }

    ///Phone Signer setup
    override fun setupPhoneSigner() {
        phoneSignerMethodMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
    }

    override fun generateSeed() {
        signerManager.setupPhoneSigner(null, this)
    }

    override fun importSeed() {
        mnemonicWordsViewStateContainer.updateViewState(MnemonicWordsViewState.Open)
    }

    fun validateSeed(seed: String) {
        viewModelScope.launch(mainImmediate) {
            val mnemonicUtils = MnemonicLanguagesUtils(app.applicationContext)
            val words = seed.lowercase().trim().split(" ")

            if (words.size != 12 && words.size != 24) {
                submitSideEffect(
                    OnBoardConnectSideEffect.Notify(
                        app.getString(R.string.mnemonic_incorrect_length)
                    )
                )
            }

            if (mnemonicUtils.validateWords(words)) {
                signerManager.setupPhoneSigner(
                    seed.lowercase().trim(),
                    this@OnBoardConnectViewModel
                )
                mnemonicDialogViewStateContainer.updateViewState(MnemonicDialogViewState.Loading)
            } else {
                submitSideEffect(
                    OnBoardConnectSideEffect.Notify(
                        app.getString(R.string.mnemonic_invalid_word)
                    )
                )
            }
        }
    }

    override fun phoneSignerSuccessfullySet() {
        viewModelScope.launch(mainImmediate) {
            navigator.toOnBoardConnectingScreen(null)
        }
    }

    override fun phoneSignerSetupError() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(
                OnBoardConnectSideEffect.Notify(
                    app.getString(R.string.phone_signer_error)
                )
            )
            mnemonicDialogViewStateContainer.updateViewState(MnemonicDialogViewState.Idle)
        }
    }

}