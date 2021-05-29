package chat.sphinx.profile.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.PrivatePhoto
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_relay.PINTimeout
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    private val relayDataHandler: RelayDataHandler,
): BaseViewModel<ProfileViewState>(dispatchers, ProfileViewState.Basic)
{

    suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        lightningRepository.getAccountBalance()

    suspend fun updateOwner(
        alias: String?,
        privatePhoto: PrivatePhoto?,
        tipAmount: Sat?
    ): Response<Any, ResponseError> =
        contactRepository.updateOwner(alias, privatePhoto, tipAmount)

    suspend fun updatePINTimeout(progress: Int) {
        relayDataHandler.persistPINTimeout(PINTimeout(progress))
    }

    private val _relayUrlStateFlow: MutableStateFlow<String?> by lazy {
        MutableStateFlow(null)
    }
    private val _pinTimeoutStateFlow: MutableStateFlow<Int?> by lazy {
        MutableStateFlow(null)
    }

    val relayUrlStateFlow: StateFlow<String?>
        get() = _relayUrlStateFlow.asStateFlow()
    val pinTimeoutStateFlow: StateFlow<Int?>
        get() = _pinTimeoutStateFlow.asStateFlow()
    val accountOwnerStateFlow: StateFlow<Contact?>
        get() = contactRepository.accountOwner

    init {
        viewModelScope.launch(mainImmediate) {
            _relayUrlStateFlow.value = relayDataHandler.retrieveRelayUrl()?.value
            _pinTimeoutStateFlow.value = relayDataHandler.retrievePINTimeout()?.value
        }
    }
}
