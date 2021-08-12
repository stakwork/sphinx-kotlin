package chat.sphinx.tribe_members_list.ui.viewstate

import chat.sphinx.concept_network_query_contact.model.ContactDto

internal sealed class TribeMemberHolderViewState(
    val contactDto: ContactDto? = null
) {

    class Loader : TribeMemberHolderViewState()
    class PendingTribeMemberHeader : TribeMemberHolderViewState()
    class TribeMemberHeader : TribeMemberHolderViewState()

    class Pending(
        contact: ContactDto
    ) : TribeMemberHolderViewState(
        contact
    )

    class Member(
        contact: ContactDto
    ) : TribeMemberHolderViewState(
        contact
    )
}
