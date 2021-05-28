package chat.sphinx.join_tribe.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_common.tribe.TribeHost
import chat.sphinx.wrapper_common.tribe.TribeUUID
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink
import chat.sphinx.wrapper_contact.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class JoinTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: JoinTribeNavigator,
    private val contactRepository: ContactRepository,
    private val networkQueryChat: NetworkQueryChat,
): BaseViewModel<JoinTribeViewState>(dispatchers, JoinTribeViewState.LoadingTribeInfo)
{
    private val args: JoinTribeFragmentArgs by savedStateHandle.navArgs()

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
                networkQueryChat.getTribeInfo(TribeHost(tribeJoinLink.tribeHost), TribeUUID(tribeJoinLink.tribeUUID)).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                            viewStateContainer.updateViewState(JoinTribeViewState.LoadingTribeInfo)
                        }
                        is Response.Error -> {
                            viewStateContainer.updateViewState(JoinTribeViewState.LoadingTribeFailed)
                        }
                        is Response.Success -> {
                            viewStateContainer.updateViewState(JoinTribeViewState.TribeInfo(loadResponse.value))
                        }
                    }
                }
            }
        } ?: run {
            viewStateContainer.updateViewState(JoinTribeViewState.LoadingTribeFailed)
        }
    }
}
