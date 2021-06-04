package chat.sphinx.new_contact.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.new_contact.navigation.NewContactNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.toContactAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class NewContactViewModel @Inject constructor(
    val navigator: NewContactNavigator,
    dispatchers: CoroutineDispatchers,
    private val contactRepository: ContactRepository,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>
): SideEffectViewModel<
        Context,
        NewContactSideEffect,
        NewContactViewState
        >(dispatchers, NewContactViewState.Idle)
{
    fun requestScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(
                    filter = object : ScannerFilter() {
                        override suspend fun checkData(data: String): Response<Any, String> {
                            if (data.trim().matches(NewContactFragment.PASTE_REGEX.toRegex())) {
                                return Response.Success(Any())
                            }
                            if (data.toLightningNodePubKey() != null) {
                                return Response.Success(Any())
                            }

                            return Response.Error("QR code is not a Lightning Node Public Key")
                        }
                    }
                )
            )
            if (response is Response.Success) {
                submitSideEffect(NewContactSideEffect.FromScanner(response.value))
            }
        }
    }

    fun addContact(
        contactAlias: String,
        lightningNodePubKey: String,
        lightningRouteHint: String?,
    ) {
        viewModelScope.launch(mainImmediate) {
            val alias: ContactAlias = contactAlias.trim().toContactAlias() ?: let {
                submitSideEffect(NewContactSideEffect.Notify.NicknameAndAddressRequired)
                return@launch
            }
            val pubKey: LightningNodePubKey = lightningNodePubKey.trim().toLightningNodePubKey() ?: let {
                submitSideEffect(NewContactSideEffect.Notify.InvalidLightningNodePublicKey)
                return@launch
            }
            val routeHint: LightningRouteHint? = lightningRouteHint?.trim()?.let {
                if (it.isEmpty()) {
                    // can potentially be passed an empty string if the EditText
                    // in which case we will treat it as null.
                    null
                } else {
                    val hint = it.toLightningRouteHint()
                    if (hint == null) {
                        submitSideEffect(NewContactSideEffect.Notify.InvalidRouteHint)
                        return@launch
                    } else {
                        hint
                    }
                }
            }

            contactRepository.createContact(
                alias,
                pubKey,
                routeHint
            ).collect { loadResponse ->
                @Exhaustive
                when(loadResponse) {
                    LoadResponse.Loading ->
                        viewStateContainer.updateViewState(NewContactViewState.Saving)
                    is Response.Error ->
                        viewStateContainer.updateViewState(NewContactViewState.Error)
                    is Response.Success ->
                        viewStateContainer.updateViewState(NewContactViewState.Saved)
                }
            }
        }
    }
}
