package chat.sphinx.onboard_name.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.onboard_name.navigation.OnBoardNameNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class OnBoardNameViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardNameNavigator,
    private val authenticationManager: AuthenticationCoreManager,
    private val networkQueryContact: NetworkQueryContact,
    private val contactRepository: ContactRepository,
): SideEffectViewModel<
        Context,
        OnBoardNameSideEffect,
        OnBoardNameViewState
        >(dispatchers, OnBoardNameViewState.Idle)
{
    fun updateOwner(name: String) {
        authenticationManager.getEncryptionKey()?.let { key ->
            viewModelScope.launch(mainImmediate) {
                contactRepository.networkRefreshContacts.collect { response ->
                    @Exhaustive
                    when (response) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {
                            updateViewState(OnBoardNameViewState.Error)

                            submitSideEffect(OnBoardNameSideEffect.GetContactsFailed)
                        }
                        is Response.Success -> {
                            contactRepository.updateOwnerNameAndKey(name, key.publicKey).let { response ->
                                @Exhaustive
                                when (response) {
                                    is Response.Error -> {
                                        updateViewState(OnBoardNameViewState.Error)

                                        submitSideEffect(OnBoardNameSideEffect.UpdateOwnerFailed)
                                    }
                                    is Response.Success -> {
                                        navigator.toOnBoardReadyScreen()
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
