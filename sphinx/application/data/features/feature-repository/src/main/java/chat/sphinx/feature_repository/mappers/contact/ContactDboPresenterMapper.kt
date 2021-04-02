package chat.sphinx.feature_repository.mappers.contact

import chat.sphinx.conceptcoredb.ContactDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactFromGroup
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class ContactDboPresenterMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<ContactDbo, Contact>(dispatchers) {
    override suspend fun mapFrom(value: ContactDbo): Contact {
        return Contact(
            value.id,
            value.route_hint,
            value.node_pub_key,
            value.node_alias,
            value.alias,
            value.photo_url,
            value.private_photo,
            value.owner,
            value.status,
            value.public_key,
            value.device_id,
            value.created_at,
            value.updated_at,

            // Contacts from tribes are not stored in the DB
            ContactFromGroup.False,

            value.notification_sound,
            value.tip_amount,
            value.invite_id
        )
    }

    override suspend fun mapTo(value: Contact): ContactDbo {
        return ContactDbo(
            value.id,
            value.routeHint,
            value.nodePubKey,
            value.nodeAlias,
            value.alias,
            value.photoUrl,
            value.privatePhoto,
            value.isOwner,
            value.status,
            value.rsaPublicKey,
            value.deviceId,
            value.createdAt,
            value.updatedAt,
            value.notificationSound,
            value.tipAmount,
            value.inviteId
        )
    }
}
