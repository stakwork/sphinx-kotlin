package chat.sphinx.onboard_ready.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.onboard_ready.navigation.OnBoardReadyNavigator
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink
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
    private val chatRepository: ChatRepository,
    private val networkQueryChat: NetworkQueryChat,
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
                        loadAndJoinDefaultTribeData()
                    }
                }
            }
        }
    }

    fun loadAndJoinDefaultTribeData() {
        val planetSphinxTribeQuery = "sphinx.chat://?action=tribe&uuid=X3IWAiAW5vNrtOX5TLEJzqNWWr3rrUaXUwaqsfUXRMGNF7IWOHroTGbD4Gn2_rFuRZcsER0tZkrLw3sMnzj4RFAk_sx0&host=tribes.sphinx.chat"

        planetSphinxTribeQuery.toTribeJoinLink()?.let { tribeJoinLink ->
            viewModelScope.launch(mainImmediate) {
                networkQueryChat.getTribeInfo(
                    ChatHost(tribeJoinLink.tribeHost),
                    ChatUUID(tribeJoinLink.tribeUUID)
                ).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}

                        is Response.Error -> {
                            goToDashboard()
                        }
                        is Response.Success -> {
                            if (loadResponse.value is TribeDto) {
                                val tribeInfo = loadResponse.value
                                tribeInfo?.set(tribeJoinLink.tribeHost, tribeJoinLink.tribeUUID)
                                joinDefaultTribe(tribeInfo)
                            } else {
                                goToDashboard()
                            }
                        }
                    }
                }
            }
        } ?: run {
            goToDashboard()
        }
    }

    private fun joinDefaultTribe(tribeInfo: TribeDto) {
        viewModelScope.launch(mainImmediate) {
            tribeInfo.amount = tribeInfo.price_to_join

            chatRepository.joinTribe(tribeInfo).collect { loadResponse ->
                @Exhaustive
                when(loadResponse) {
                    LoadResponse.Loading -> {}

                    is Response.Error -> {
                        goToDashboard()
                    }
                    is Response.Success ->
                        goToDashboard()
                }

            }
        }
    }

    private fun goToDashboard() {
        viewModelScope.launch(mainImmediate) {
            navigator.toDashboardScreen()
        }
    }

    suspend fun getBalances(): Flow<LoadResponse<NodeBalanceAll, ResponseError>> {
        return lightningRepository.getAccountBalanceAll()
    }
}
