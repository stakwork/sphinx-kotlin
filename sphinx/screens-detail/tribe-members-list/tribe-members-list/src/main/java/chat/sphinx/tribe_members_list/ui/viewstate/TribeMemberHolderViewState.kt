package chat.sphinx.tribe_members_list.ui.viewstate

import chat.sphinx.concept_network_query_contact.model.ContactDto

internal sealed class TribeMemberHolderViewState(
    val contactDto: ContactDto? = null,
    val showInitial: Boolean = false
) {

    class Loader : TribeMemberHolderViewState()
    class PendingTribeMemberHeader : TribeMemberHolderViewState()
    class TribeMemberHeader : TribeMemberHolderViewState()

    class Pending(
        contact: ContactDto,
        showInitial: Boolean
    ) : TribeMemberHolderViewState(
        contact,
        showInitial
    )

    class Member(
        contact: ContactDto,
        showInitial: Boolean
    ) : TribeMemberHolderViewState(
        contact,
        showInitial
    )
}
