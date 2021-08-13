package chat.sphinx.tribe_members_list.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.tribe_members_list.R
import chat.sphinx.tribe_members_list.navigation.TribeMembersListNavigator
import chat.sphinx.tribe_members_list.ui.viewstate.TribeMemberHolderViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_message.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@Suppress("NOTHING_TO_INLINE")
inline fun <TribeMembersListViewState> ArrayList<TribeMembersListViewState>.hasNoPendingTribeMemberHeader(): Boolean {
    return count { tribeMemberHolderViewState ->
        tribeMemberHolderViewState is TribeMemberHolderViewState.PendingTribeMemberHeader
    } == 0
}

@Suppress("NOTHING_TO_INLINE")
inline fun <TribeMembersListViewState> ArrayList<TribeMembersListViewState>.hasNoTribeMemberHeader(): Boolean {
    return count { tribeMemberHolderViewState ->
        tribeMemberHolderViewState is TribeMemberHolderViewState.TribeMemberHeader
    } == 0
}

@HiltViewModel
internal class TribeMembersListViewModel @Inject constructor(
    private val app: Application,
    dispatchers: CoroutineDispatchers,
    val navigator: TribeMembersListNavigator,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val networkQueryContact: NetworkQueryContact,
//    private val networkQueryChat: NetworkQueryChat,
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

    private fun processMembers(
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

    suspend fun processMemberRequest(
        contactId: ContactId,
        type: MessageType.GroupAction
    ): LoadResponse<Any, ResponseError> {
        var response: LoadResponse<Any, ResponseError>  = Response.Error(ResponseError(("")))

        viewModelScope.launch(mainImmediate) {
            messageRepository.getTribeMembershipRequestMessageByContactId(contactId).collect { message ->
                message?.let {
                    response = messageRepository.processMemberRequest(contactId, message.id, type)
                }
            }
        }.join()

        return response
    }

    fun showFailedToProcessMemberMessage(type: MessageType.GroupAction) {
        viewModelScope.launch(mainImmediate) {
            // TODO: submitSideEffect
            if (type is MessageType.GroupAction.MemberApprove) {
                app.getString(R.string.failed_to_approve_member)
            } else if (type is MessageType.GroupAction.MemberReject) {
                app.getString(R.string.failed_to_reject_member)
            }
        }
    }
}
