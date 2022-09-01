package chat.sphinx.add_tribe_member.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.add_tribe_member.navigation.AddTribeMemberNavigator
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.wrapper_contact.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@HiltViewModel
internal class AddTribeMemberViewModel @Inject constructor(
    private val app: Application,
    dispatchers: CoroutineDispatchers,
    val navigator: AddTribeMemberNavigator,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val networkQueryContact: NetworkQueryContact,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        AddTribeMemberSideEffect,
        AddTribeMemberViewState
        >(
    dispatchers,
    AddTribeMemberViewState.Idle
)
{
    private val args: AddTribeMemberFragmentArgs by savedStateHandle.navArgs()

    init {
//        viewModelScope.launch(mainImmediate) {
//            loadTribeMembers()
//        }
    }

    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)

                resolvedOwner!!
            }
        }
    }
}
