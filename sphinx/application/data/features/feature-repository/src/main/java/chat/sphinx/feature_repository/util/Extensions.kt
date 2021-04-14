package chat.sphinx.feature_repository.util

import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.invite.toInviteStatus
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_invite.InviteString
import chat.sphinx.wrapper_rsa.RsaPublicKey

/**
 * Always use [SphinxDatabaseQueries.transaction] with this extension function.
 * */
@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertContact(dto: ContactDto) {

    if (dto.fromGroupActual) {
        return
    }

    contactUpsert(
        dto.route_hint?.toLightningRouteHint(),
        dto.public_key?.toLightningNodePubKey(),
        dto.node_alias?.toLightningNodeAlias(),
        dto.alias.toContactAlias(),
        dto.photo_url?.toPhotoUrl(),
        dto.privatePhotoActual.toPrivatePhoto(),
        dto.status.toContactStatus(),
        dto.contact_key?.let { RsaPublicKey(it.toCharArray()) },
        dto.device_id?.toDeviceId(),
        dto.updated_at.toDateTime(),
        dto.notification_sound?.toNotificationSound(),
        dto.tip_amount?.toSat(),
        dto.invite?.id?.let { InviteId(it) },
        dto.invite?.status?.toInviteStatus(),
        ContactId(dto.id),
        dto.isOwnerActual.toOwner(),
        dto.created_at.toDateTime()
    )
    dto.invite?.let { inviteDto ->
        inviteUpsert(
            InviteString(inviteDto.invite_string),
            inviteDto.invoice?.toLightningPaymentRequest(),
            inviteDto.status.toInviteStatus(),
            inviteDto.price?.toSat(),
            InviteId(inviteDto.id),
            ContactId(inviteDto.contact_id),
            inviteDto.created_at.toDateTime(),
        )
    }
}
