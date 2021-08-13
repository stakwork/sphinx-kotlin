package chat.sphinx.tribe_members_list.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_repository_chat.ChatRepository
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
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
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
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val networkQueryContact: NetworkQueryContact,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        TribeMembersListSideEffect,
        TribeMembersListViewState
        >(
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

    fun loadMoreTribeMembers() {
        if (loading) {
            return
        }

        loading = true
        page += 1

        viewModelScope.launch(mainImmediate) {
            loadTribeMembers()
        }

        loading = false
    }

    private suspend fun reloadTribeMembers() {
        page = 0
        loadTribeMembers()
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
                                processMembers(
                                    loadResponse.value.contacts,
                                    getOwner()
                                ),
                                false,
                                firstPage
                            )
                        )
                    }
                }
            }
        }
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

    private fun processMembers(
        contacts: List<ContactDto>,
        owner: Contact
    ): List<TribeMemberHolderViewState> {
        val tribeMemberHolderViewStates = ArrayList<TribeMemberHolderViewState>(contacts.size)

        var lastInitial = ""

        for (contact in contacts) {
            if (owner.id.value == contact.id) {
                continue
            }

            val contactInitial = contact.alias?.firstOrNull()?.toString() ?: ""
            val shouldShowInitial = contactInitial != lastInitial

            lastInitial = contactInitial

            if (contact.pendingActual) {
                if (tribeMemberHolderViewStates.hasNoPendingTribeMemberHeader()) {
                    tribeMemberHolderViewStates.add(
                        TribeMemberHolderViewState.PendingTribeMemberHeader
                    )
                }
                tribeMemberHolderViewStates.add(
                    TribeMemberHolderViewState.Pending(
                        contact,
                        shouldShowInitial
                    )
                )
            } else {
                if (tribeMemberHolderViewStates.hasNoTribeMemberHeader()) {
                    tribeMemberHolderViewStates.add(
                        TribeMemberHolderViewState.TribeMemberHeader
                    )
                }
                tribeMemberHolderViewStates.add(
                    TribeMemberHolderViewState.Member(
                        contact,
                        shouldShowInitial
                    )
                )
            }

        }

        if (contacts.size >= itemsPerPage) {
            tribeMemberHolderViewStates.add(
                TribeMemberHolderViewState.Loader
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
            val message = messageRepository.getTribeLastMemberRequestByContactId(
                contactId,
                ChatId(args.argChatId)
            ).firstOrNull()

            if (message != null) {
                response = messageRepository.processMemberRequest(contactId, message.id, type)
            }
        }.join()

        if (response is Response.Success) {
            reloadTribeMembers()
        }

        return response
    }

    fun showFailedToProcessMemberMessage(type: MessageType.GroupAction) {
        viewModelScope.launch(mainImmediate) {
            if (type is MessageType.GroupAction.MemberApprove) {
                submitSideEffect(
                    TribeMembersListSideEffect.Notify(
                        app.getString(R.string.failed_to_approve_member)
                    )
                )
            } else if (type is MessageType.GroupAction.MemberReject) {
                submitSideEffect(
                    TribeMembersListSideEffect.Notify(
                        app.getString(R.string.failed_to_reject_member)
                    )
                )
            }
        }
    }

    fun kickMemberFromTribe(contactId: ContactId) {
        viewModelScope.launch(mainImmediate) {
            chatRepository.kickMemberFromTribe(ChatId(args.argChatId), contactId)
        }
    }
}
