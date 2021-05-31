package chat.sphinx.onboard_ready.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.onboard_ready.navigation.OnBoardReadyNavigator
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_lightning.NodeBalanceAll
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
): SideEffectViewModel<
        Context,
        OnBoardReadySideEffect,
        OnBoardReadyViewState
        >(dispatchers, OnBoardReadyViewState.Idle)
{
    fun saveInviterAndFinish(nickname: String, pubkey: String, routeHint: String?) {
        viewModelScope.launch(mainImmediate) {
            val alias = ContactAlias(nickname)
            val pubKey = LightningNodePubKey(pubkey)
            val routeHint = routeHint?.let { LightningRouteHint(it) }

            contactRepository.createContact(
                alias,
                pubKey,
                routeHint
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
                        viewModelScope.launch(mainImmediate) {
                            navigator.toDashboardScreen()
                        }
                    }
                }
            }
        }
    }

    suspend fun getBalances(): Flow<LoadResponse<NodeBalanceAll, ResponseError>> {
        return lightningRepository.getAccountBalanceAll()
    }
}
