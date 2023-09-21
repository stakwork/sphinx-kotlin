package chat.sphinx.onboard_ready.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.onboard_ready.navigation.OnBoardReadyNavigator
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_lightning.NodeBalanceAll
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class OnBoardReadyViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardReadyNavigator,

    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    private val networkQueryInvite: NetworkQueryInvite,

    private val onBoardStepHandler: OnBoardStepHandler,
): SideEffectViewModel<
        Context,
        OnBoardReadySideEffect,
        OnBoardReadyViewState
        >(dispatchers, OnBoardReadyViewState.Idle)
{

    fun saveInviterAndFinish(
        nickname: String,
        pubkey: String,
        routeHint: String?,
        inviteString: String? = null
    ) {
        viewModelScope.launch(mainImmediate) {
            val alias = ContactAlias(nickname)
            val pubKey = LightningNodePubKey(pubkey)
            val lightningRouteHint = routeHint?.toLightningRouteHint()

            contactRepository.createContact(
                alias,
                pubKey,
                lightningRouteHint
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    LoadResponse.Loading ->
                        viewStateContainer.updateViewState(OnBoardReadyViewState.Saving)
                    is Response.Error -> {
                        viewStateContainer.updateViewState(OnBoardReadyViewState.Error)

                        submitSideEffect(OnBoardReadySideEffect.CreateInviterFailed)
                    }
                    is Response.Success -> {
                        if (inviteString != null && inviteString.isNotEmpty()) {
                            finishInvite(inviteString)
                        } else {
                            finishSignup()
                        }
                    }
                }
            }
        }
    }

    fun finishInvite(inviteString: String) {
        viewModelScope.launch(mainImmediate) {
            networkQueryInvite.finishInvite(inviteString).collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {}

                    is Response.Error -> {
                        finishSignup()
                    }
                    is Response.Success -> {
                        finishSignup()
                    }
                }
            }
        }
    }

    fun finishSignup() {
        viewModelScope.launch(mainImmediate) {
            goToDashboard()
        }
    }

    private suspend fun goToDashboard() {
        viewModelScope.launch(mainImmediate) {
            onBoardStepHandler.finishOnBoardSteps()
            navigator.toDashboardScreen()
        }
    }

    suspend fun getBalances(): Flow<LoadResponse<NodeBalanceAll, ResponseError>> {
        return lightningRepository.getAccountBalanceAll()
    }
}
