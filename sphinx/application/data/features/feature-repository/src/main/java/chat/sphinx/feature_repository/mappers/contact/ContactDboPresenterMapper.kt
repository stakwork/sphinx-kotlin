package chat.sphinx.feature_repository.mappers.contact

import chat.sphinx.conceptcoredb.ContactDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactFromGroup
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

@Suppress("NOTHING_TO_INLINE")
inline fun ContactDbo.toContact(): Contact =
    Contact(
        id,
        route_hint,
        node_pub_key,
        node_alias,
        alias,
        photo_url,
        private_photo,
        owner,
        status,
        public_key,
        device_id,
        created_at,
        updated_at,

        // Contacts from tribes are not stored in the DB
        ContactFromGroup.False,

        notification_sound,
        tip_amount,
        invite_id,
        invite_status,
        blocked
    )

internal class ContactDboPresenterMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<ContactDbo, Contact>(dispatchers) {
    override suspend fun mapFrom(value: ContactDbo): Contact {
        return value.toContact()
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
            value.inviteId,
            value.inviteStatus,
            value.blocked,
        )
    }
}
