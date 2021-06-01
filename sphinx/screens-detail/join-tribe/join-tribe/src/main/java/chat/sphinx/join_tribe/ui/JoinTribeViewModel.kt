package chat.sphinx.join_tribe.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.toContactAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class JoinTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: JoinTribeNavigator,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    private val networkQueryChat: NetworkQueryChat,
): SideEffectViewModel<
        Context,
        JoinTribeSideEffect,
        JoinTribeViewState
        >(dispatchers, JoinTribeViewState.LoadingTribe)
{
    private val args: JoinTribeFragmentArgs by savedStateHandle.navArgs()
    private var tribeInfo : TribeDto? = null

    private val _accountOwnerStateFlow: MutableStateFlow<Contact?> by lazy {
        MutableStateFlow(null)
    }

    val accountOwnerStateFlow: StateFlow<Contact?>
        get() = _accountOwnerStateFlow.asStateFlow()

    init {
        viewModelScope.launch(mainImmediate) {
            contactRepository.accountOwner.collect { contact ->
                _accountOwnerStateFlow.value = contact
            }
        }
    }

    fun loadTribeData() {
        args.argTribeLink.toTribeJoinLink()?.let { tribeJoinLink ->
            viewModelScope.launch(mainImmediate) {
                networkQueryChat.getTribeInfo(ChatHost(tribeJoinLink.tribeHost), ChatUUID(tribeJoinLink.tribeUUID)).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading ->
                            viewStateContainer.updateViewState(JoinTribeViewState.LoadingTribe)
                        is Response.Error -> {
                            submitSideEffect(JoinTribeSideEffect.Notify.InvalidTribe)
                            viewStateContainer.updateViewState(JoinTribeViewState.ErrorLoadingTribe)
                        }
                        is Response.Success -> {
                            if (loadResponse.value is TribeDto) {
                                tribeInfo = loadResponse.value
                                tribeInfo?.set(tribeJoinLink.tribeHost, tribeJoinLink.tribeUUID)
                                viewStateContainer.updateViewState(JoinTribeViewState.TribeLoaded(tribeInfo!!))
                            } else {
                                viewStateContainer.updateViewState(JoinTribeViewState.ErrorLoadingTribe)
                            }
                        }
                    }
                }
            }
        } ?: run {
            viewStateContainer.updateViewState(JoinTribeViewState.ErrorLoadingTribe)
        }
    }

    fun joinTribe(myAlias: String) {
        viewModelScope.launch(mainImmediate) {
            val myAlias: ContactAlias = myAlias.trim().toContactAlias() ?: let {
                submitSideEffect(JoinTribeSideEffect.Notify.AliasRequired)
                return@launch
            }

            var tribeInfo = tribeInfo ?: let {
                submitSideEffect(JoinTribeSideEffect.Notify.InvalidTribe)
                return@launch
            }
            tribeInfo.my_alias = myAlias.value
            tribeInfo.amount = tribeInfo.price_to_join

            chatRepository.joinTribe(tribeInfo).collect { loadResponse ->
                @Exhaustive
                when(loadResponse) {
                    LoadResponse.Loading ->
                        viewStateContainer.updateViewState(JoinTribeViewState.JoiningTribe)
                    is Response.Error -> {
                        submitSideEffect(JoinTribeSideEffect.Notify.ErrorJoining)
                        viewStateContainer.updateViewState(JoinTribeViewState.ErrorJoiningTribe)
                    }
                    is Response.Success ->
                        viewStateContainer.updateViewState(JoinTribeViewState.TribeJoined)
                }

            }
        }
    }
}
