package chat.sphinx.feature_repository.mappers.contact

import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.conceptcoredb.ContactDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.lightning.toLightningNodeAlias
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_rsa.RsaPublicKey
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class ContactDtoDboMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<ContactDto, ContactDbo>(dispatchers) {

    @Throws(IllegalArgumentException::class)
    override suspend fun mapFrom(value: ContactDto): ContactDbo {

        if (value.from_group.toContactFromGroup().isTrue()) {
            throw IllegalArgumentException("Contacts that are from a group _cannot_ be saved to the DB")
        }

        return ContactDbo(
            ContactId(value.id),
            value.route_hint?.toLightningRouteHint(),
            value.public_key?.toLightningNodePubKey(),
            value.node_alias?.toLightningNodeAlias(),
            value.alias.toContactAlias(),
            value.photo_url?.toPhotoUrl(),
            value.private_photo.toPrivatePhoto(),
            value.is_owner.toOwner(),
            value.status.toContactStatus(),
            value.contact_key?.let { RsaPublicKey(it.toCharArray()) },
            value.device_id?.toDeviceId(),
            value.created_at.toDateTime(),
            value.updated_at.toDateTime(),
            value.notification_sound?.toNotificationSound(),
            value.tip_amount?.toSat(),
            value.invite?.id?.let { InviteId(it) }
        )
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun mapTo(value: ContactDbo): ContactDto {
        throw IllegalArgumentException("Going from a ContactDbo to ContactDto is not allowed")
    }
}
