package chat.sphinx.tribe_members_list.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.tribe_members_list.navigation.TribeMembersListNavigator
import chat.sphinx.tribe_members_list.ui.viewstate.TribeMemberHolderViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

inline fun <TribeMembersListViewState> ArrayList<TribeMembersListViewState>.hasNoPendingTribeMemberHeader(): Boolean {
    return count { tribeMemberHolderViewState ->
        tribeMemberHolderViewState is TribeMemberHolderViewState.PendingTribeMemberHeader
    } == 0
}

inline fun <TribeMembersListViewState> ArrayList<TribeMembersListViewState>.hasNoTribeMemberHeader(): Boolean {
    return count { tribeMemberHolderViewState ->
        tribeMemberHolderViewState is TribeMemberHolderViewState.TribeMemberHeader
    } == 0
}

@HiltViewModel
internal class TribeMembersListViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: TribeMembersListNavigator,
    private val contactRepository: ContactRepository,
    private val networkQueryContact: NetworkQueryContact,
    savedStateHandle: SavedStateHandle,
): BaseViewModel<TribeMembersListViewState>(
    dispatchers,
    TribeMembersListViewState.ListMode(
        listOf(),
        loading = true,
        firstPage = true
    )
)
{
    private val args: TribeMembersListFragmentArgs by savedStateHandle.navArgs()

    private var page: Int = 0
    private var loading: Boolean = false
    private val itemsPerPage: Int = 50

    init {
        viewModelScope.launch(mainImmediate) {
            loadTribeMembers()
        }
    }

    suspend fun loadMoreTribeMembers() {
        if (loading) {
            return
        }

        loading = true
        page += 1

        loadTribeMembers()

        loading = false
    }

    private suspend fun loadTribeMembers() {
        networkQueryContact.getTribeMembers(
            chatId = ChatId(args.argChatId),
            offset = page * itemsPerPage,
            limit = itemsPerPage
        ).collect{ loadResponse ->
            val firstPage = (page == 0)

            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {
                    updateViewState(
                        TribeMembersListViewState.ListMode(listOf(), true, firstPage)
                    )
                }
                is Response.Error -> {
                    updateViewState(
                        TribeMembersListViewState.ListMode(listOf(), false, firstPage)
                    )
                }
                is Response.Success -> {
                    if (loadResponse.value.contacts.isNotEmpty()) {
                        updateViewState(
                            TribeMembersListViewState.ListMode(
                                processMembers(loadResponse.value.contacts),
                                false,
                                firstPage
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun processMembers(
        contacts: List<ContactDto>
    ): List<TribeMemberHolderViewState> {

        val tribeMemberHolderViewStates = ArrayList<TribeMemberHolderViewState>(contacts.size)

        for (contact in contacts) {
            if (contact.pending == true) {
                if (!tribeMemberHolderViewStates.hasNoPendingTribeMemberHeader()) {
                    tribeMemberHolderViewStates.add(
                        TribeMemberHolderViewState.PendingTribeMemberHeader()
                    )
                }
                tribeMemberHolderViewStates.add(
                    TribeMemberHolderViewState.Pending(
                        contact,
                    )
                )
            } else {
                if (tribeMemberHolderViewStates.hasNoTribeMemberHeader()) {
                    tribeMemberHolderViewStates.add(
                        TribeMemberHolderViewState.TribeMemberHeader()
                    )
                }
                tribeMemberHolderViewStates.add(
                    TribeMemberHolderViewState.Member(
                        contact,
                    )
                )
            }

        }

        if (contacts.size == itemsPerPage) {
            tribeMemberHolderViewStates.add(
                TribeMemberHolderViewState.Loader()
            )
        }

        return tribeMemberHolderViewStates
    }
}
