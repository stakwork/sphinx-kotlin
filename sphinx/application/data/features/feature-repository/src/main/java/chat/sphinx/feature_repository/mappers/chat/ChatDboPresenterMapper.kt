package chat.sphinx.feature_repository.mappers.chat

import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_chat.*

import java.text.ParseException

internal class ChatDboPresenterMapper: ClassMapper<ChatDbo, Chat>() {

    @Throws(
        IllegalArgumentException::class,
        ParseException::class
    )
    override fun mapFrom(value: ChatDbo): Chat {
        return Chat(
            value.id,
            value.uuid,
            value.name,
            value.photo_url,
            value.type,
            value.status,
            value.contact_ids,
            value.is_muted,
            value.created_at,
            value.group_key,
            value.host,
            value.price_per_message,
            value.escrow_amount,
            value.unlisted,
            value.private_tribe,
            value.owner_pub_key,
            value.seen,
            value.meta_data,
            value.my_photo_url,
            value.my_alias,
            value.pending_contact_ids
        )
    }

    override fun mapTo(value: Chat): ChatDbo {
        return ChatDbo(
            value.id,
            value.uuid,
            value.name,
            value.photoUrl,
            value.type,
            value.status,
            value.contactIds,
            value.isMuted,
            value.createdAt,
            value.groupKey,
            value.host,
            value.pricePerMessage,
            value.escrowAmount,
            value.unlisted,
            value.privateTribe,
            value.ownerPubKey,
            value.seen,
            value.metaData,
            value.myPhotoUrl,
            value.myAlias,
            value.pendingContactIds,
        )
    }
}
