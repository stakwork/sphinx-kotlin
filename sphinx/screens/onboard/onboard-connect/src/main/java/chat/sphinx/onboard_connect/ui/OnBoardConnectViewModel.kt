package chat.sphinx.onboard_connect.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_signer.SignerMenuHandler
import chat.sphinx.menu_bottom_signer.SignerMenuViewModel
import chat.sphinx.onboard_connect.navigation.OnBoardConnectNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
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
import chat.sphinx.onboard_connect.model.Glyph
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.inject.Inject


internal inline val OnBoardConnectFragmentArgs.newUser: Boolean
    get() = argNewUser

@HiltViewModel
internal class OnBoardConnectViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    val navigator: OnBoardConnectNavigator,
): SideEffectViewModel<
        Context,
        OnBoardConnectSideEffect,
        OnBoardConnectViewState
        >(dispatchers, OnBoardConnectViewState.Idle), SignerMenuViewModel
{

    private val args: OnBoardConnectFragmentArgs by handle.navArgs()
    lateinit var glyph: Glyph

    val submitButtonViewStateContainer: ViewStateContainer<OnBoardConnectSubmitButtonViewState> by lazy {
        ViewStateContainer(OnBoardConnectSubmitButtonViewState.Disabled)
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
                isValid = true
            }

        } else if (vs is OnBoardConnectViewState.ExistingUser) {
            if (redemptionCode != null &&
                redemptionCode is RedemptionCode.AccountRestoration) {
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
                        glyph = Glyph(
                            redemptionCode.mqtt,
                            redemptionCode.network,
                            redemptionCode.relay
                        )

                        signerMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
                    } else {
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

            if (redemptionCode is RedemptionCode.Glyph) {
                glyph = Glyph(
                    redemptionCode.mqtt,
                    redemptionCode.network,
                    redemptionCode.relay
                )

                signerMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
            } else {
                viewModelScope.launch(mainImmediate) {
                    navigator.toOnBoardConnectingScreen(code)
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

    override val signerMenuHandler: SignerMenuHandler by lazy {
        SignerMenuHandler()
    }

    override fun setupHardwareSigner() {
        TODO("Not yet implemented")
    }

    override fun setupPhoneSigner() {
        TODO("Not yet implemented")
    }
}