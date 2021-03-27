package chat.sphinx.feature_repository.mappers.chat

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_common.toSeen
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import java.text.ParseException

internal class ChatDtoDboMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<ChatDto, ChatDbo>(dispatchers) {

    @Throws(
        IllegalArgumentException::class,
        ParseException::class
    )
    override fun mapFrom(value: ChatDto): ChatDbo {
        return ChatDbo(
            ChatId(value.id),
            ChatUUID(value.uuid),
            value.name?.toChatName(),
            value.photo_url?.toPhotoUrl(),
            value.type.toChatType(),
            value.status.toChatStatus(),
            value.contact_ids.map { ContactId(it) },
            value.is_muted.toChatMuted(),
            value.created_at.toDateTime(),
            value.group_key?.toChatGroupKey(),
            value.host?.toChatHost(),
            value.price_per_message?.toSat(),
            value.escrow_amount?.toSat(),
            value.unlisted.toChatUnlisted(),
            value.private.toChatPrivate(),
            value.owner_pub_key?.toLightningNodePubKey(),
            value.seen.toSeen(),
            value.meta?.toChatMetaDataOrNull(),
            value.my_photo_url?.toPhotoUrl(),
            value.my_alias?.toChatAlias(),
            value.pending_contact_ids?.map { ContactId(it) }
        )
    }

    @Throws(IllegalArgumentException::class)
    override fun mapTo(value: ChatDbo): ChatDto {
        throw IllegalArgumentException("Going from a ChatDbo to ChatDto is not allowed")
    }
}