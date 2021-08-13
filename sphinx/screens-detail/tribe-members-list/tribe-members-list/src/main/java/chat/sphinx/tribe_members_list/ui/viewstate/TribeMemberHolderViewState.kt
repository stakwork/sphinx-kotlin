package chat.sphinx.tribe_members_list.ui.viewstate

import chat.sphinx.concept_network_query_contact.model.ContactDto

internal sealed class TribeMemberHolderViewState(
    val memberId: Long? = null,
    val memberAlias: String? = null,
    val memberPhotoUrl: String? = null,
    val showInitial: Boolean = false
) {

    object Loader : TribeMemberHolderViewState()
    object PendingTribeMemberHeader : TribeMemberHolderViewState()
    object TribeMemberHeader : TribeMemberHolderViewState()

    class Pending(
        memberId: Long,
        memberAlias: String?,
        memberPhotoUrl: String?,
        showInitial: Boolean
    ) : TribeMemberHolderViewState(
        memberId,
        memberAlias,
        memberPhotoUrl,
        showInitial
    ) {
        companion object {
            operator fun invoke(
                contactDto: ContactDto,
                showInitial: Boolean
            ): Pending =
                Pending(
                    contactDto.id,
                    contactDto.alias,
                    contactDto.photo_url,
                    showInitial
                )
        }
    }

    class Member(
        memberId: Long,
        memberAlias: String?,
        memberPhotoUrl: String?,
        showInitial: Boolean
    ) : TribeMemberHolderViewState(
        memberId,
        memberAlias,
        memberPhotoUrl,
        showInitial
    ) {
        companion object {
            operator fun invoke(
                contactDto: ContactDto,
                showInitial: Boolean
            ): Member =
                Member(
                    contactDto.id,
                    contactDto.alias,
                    contactDto.photo_url,
                    showInitial
                )
        }
    }
}
