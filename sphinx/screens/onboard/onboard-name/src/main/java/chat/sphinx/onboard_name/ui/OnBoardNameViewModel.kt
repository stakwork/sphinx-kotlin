package chat.sphinx.onboard_name.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.onboard_name.navigation.OnBoardNameNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import app.cash.exhaustive.Exhaustive
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
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

    private var ownerUpdateJob: Job? = null
    fun updateOwner(name: String, inviterData: OnBoardInviterData) {
        if (ownerUpdateJob?.isActive == true) {
            return
        }

        ownerUpdateJob = viewModelScope.launch(mainImmediate) {
            authenticationCoordinator.submitAuthenticationRequest(
                AuthenticationRequest.GetEncryptionKey()
            ).firstOrNull().let { authenticationResponse ->
                @Exhaustive
                when (authenticationResponse) {
                    null,
                    is AuthenticationResponse.Failure,
                    is AuthenticationResponse.Success.Authenticated -> {
                        // never returned
                    }
                    is AuthenticationResponse.Success.Key -> {

                        contactRepository.networkRefreshContacts.collect { refreshResponse ->

                            @Exhaustive
                            when (refreshResponse) {
                                is LoadResponse.Loading -> {}
                                is Response.Error -> {
                                    updateViewState(OnBoardNameViewState.Error)
                                    submitSideEffect(OnBoardNameSideEffect.GetContactsFailed)
                                }
                                is Response.Success -> {

                                    contactRepository.updateOwnerNameAndKey(
                                        name,
                                        authenticationResponse.encryptionKey.publicKey
                                    ).let { updateOwnerResponse ->

                                        @Exhaustive
                                        when (updateOwnerResponse) {
                                            is Response.Error -> {
                                                updateViewState(OnBoardNameViewState.Error)
                                                submitSideEffect(OnBoardNameSideEffect.UpdateOwnerFailed)
                                            }
                                            is Response.Success -> {
                                                val step3: OnBoardStep.Step3? = onBoardStepHandler.persistOnBoardStep3Data(inviterData)

                                                if (step3 != null) {
                                                    navigator.toOnBoardReadyScreen(step3)
                                                } else {
                                                    // TODO: Handle Error
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                    }
                }
            }
        }
    }
}
