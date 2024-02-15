package chat.sphinx.tribe_members_list.ui.viewstate

import chat.sphinx.example.wrapper_mqtt.TribeMember


internal sealed class TribeMemberHolderViewState(
    val pubkey: String? = null,
    val alias: String? = null,
    val photo_url: String? = null,
    val person: String? = null,
    val route_hint: String? = null,
    val contact_key: String? = null,
    val showInitial: Boolean = false
) {

    object Loader : TribeMemberHolderViewState()
    object PendingTribeMemberHeader : TribeMemberHolderViewState()
    object TribeMemberHeader : TribeMemberHolderViewState()

    class Pending(
        pubkey: String?,
        alias: String?,
        photo_url: String?,
        person: String?,
        route_hint: String?,
        contact_key: String?,
        showInitial: Boolean
    ) : TribeMemberHolderViewState(
        pubkey,
        alias,
        photo_url,
        person,
        route_hint,
        contact_key,
        showInitial
    ) {
        companion object {
            operator fun invoke(
                tribeMember: TribeMember,
                showInitial: Boolean
            ): Pending =
                Pending(
                    tribeMember.pubkey,
                    tribeMember.alias,
                    tribeMember.photo_url,
                    tribeMember.person,
                    tribeMember.route_hint,
                    tribeMember.contact_key,
                    showInitial
                )
        }
    }

    class Member(
        pubkey: String?,
        alias: String?,
        photo_url: String?,
        person: String?,
        route_hint: String?,
        contact_key: String?,
        showInitial: Boolean
    ) : TribeMemberHolderViewState(
        pubkey,
        alias,
        photo_url,
        person,
        route_hint,
        contact_key,
        showInitial
    ) {
        companion object {
            operator fun invoke(
                tribeMember: TribeMember,
                showInitial: Boolean
            ): Member =
                Member(
                    tribeMember.pubkey,
                    tribeMember.alias,
                    tribeMember.photo_url,
                    tribeMember.person,
                    tribeMember.route_hint,
                    tribeMember.contact_key,
                    showInitial
                )
        }
    }
}
