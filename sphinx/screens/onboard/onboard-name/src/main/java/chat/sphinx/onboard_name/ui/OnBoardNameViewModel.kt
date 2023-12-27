package chat.sphinx.onboard_name.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.onboard_name.navigation.OnBoardNameNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.wrapper_contact.toContactAlias
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
internal class OnBoardNameViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardNameNavigator,
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val contactRepository: ContactRepository,
    private val onBoardStepHandler: OnBoardStepHandler,
): SideEffectViewModel<
        Context,
        OnBoardNameSideEffect,
        OnBoardNameViewState
        >(dispatchers, OnBoardNameViewState.Idle)
{

    fun updateOwnerAlias(alias: String) {
        viewModelScope.launch(mainImmediate){
            alias.toContactAlias()?.let {
                contactRepository.updateOwnerAlias(it)
                navigator.toOnBoardPictureScreen(null)
            }
        }
    }

    /** Sphinx V1 (likely to be removed) **/
//
//    private var ownerUpdateJob: Job? = null
//    fun updateOwner(name: String, inviterData: OnBoardInviterData) {
//        if (ownerUpdateJob?.isActive == true) {
//            return
//        }
//
//        ownerUpdateJob = viewModelScope.launch(mainImmediate) {
//            if (name.isEmpty()) {
//                updateViewState(OnBoardNameViewState.Error)
//                return@launch
//            }
//
//            authenticationCoordinator.submitAuthenticationRequest(
//                AuthenticationRequest.GetEncryptionKey()
//            ).firstOrNull().let { authenticationResponse ->
//                @Exhaustive
//                when (authenticationResponse) {
//                    null,
//                    is AuthenticationResponse.Failure,
//                    is AuthenticationResponse.Success.Authenticated -> {
//                        // never returned
//                    }
//                    is AuthenticationResponse.Success.Key -> {
//
//                        contactRepository.networkRefreshContacts.collect { refreshResponse ->
//
//                            @Exhaustive
//                            when (refreshResponse) {
//                                is LoadResponse.Loading -> {}
//                                is Response.Error -> {
//                                    updateViewState(OnBoardNameViewState.Error)
//                                    submitSideEffect(OnBoardNameSideEffect.GetContactsFailed)
//                                }
//                                is Response.Success -> {
//
//                                    contactRepository.updateOwnerNameAndKey(
//                                        name,
//                                        authenticationResponse.encryptionKey.publicKey
//                                    ).let { updateOwnerResponse ->
//
//                                        @Exhaustive
//                                        when (updateOwnerResponse) {
//                                            is Response.Error -> {
//                                                updateViewState(OnBoardNameViewState.Error)
//                                                submitSideEffect(OnBoardNameSideEffect.UpdateOwnerFailed)
//                                            }
//                                            is Response.Success -> {
//                                                val step3: OnBoardStep.Step3_Picture? = onBoardStepHandler.persistOnBoardStep3Data(inviterData)
//
//                                                if (step3 != null) {
//                                                    navigator.toOnBoardPictureScreen(step3)
//                                                } else {
//                                                    // TODO: Handle Error
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//
//
//                    }
//                }
//            }
//        }
//    }
}
