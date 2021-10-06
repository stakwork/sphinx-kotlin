package chat.sphinx.onboard_connect.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
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
        >(dispatchers, OnBoardConnectViewState.Idle)
{

    private val args: OnBoardConnectFragmentArgs by handle.navArgs()

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
        submitButtonViewStateContainer.updateViewState(
            OnBoardConnectSubmitButtonViewState.Enabled
        )
    }

    fun navigateToScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(
                    filter = object : ScannerFilter() {
                        override suspend fun checkData(data: String): Response<Any, String> {
                            return Response.Success(Any())
//                            if (data.toValidInviteStringOrNull() != null) {
//                                return Response.Success(Any())
//                            }
//
//                            if (RedemptionCode.decode(data) != null) {
//                                return Response.Success(Any())
//                            }
//
//                            return Response.Error("QR code is not an account restore code")
                        }
                    }
                )
            )
            if (response is Response.Success) {
                submitSideEffect(OnBoardConnectSideEffect.FromScanner(response.value))
            }
        }
    }

    fun continueToConnectingScreen() {

    }
}