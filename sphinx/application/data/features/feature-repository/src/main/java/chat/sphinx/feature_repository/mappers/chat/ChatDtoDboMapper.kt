package chat.sphinx.feature_repository.mappers.chat

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_common.toSeen
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import java.text.ParseException

internal class ChatDtoDboMapper(
    dispatchers: CoroutineDispatchers,
    private val moshi: Moshi
): ClassMapper<ChatDto, ChatDbo>(dispatchers) {

    @Throws(
        IllegalArgumentException::class,
        ParseException::class
    )
    override suspend fun mapFrom(value: ChatDto): ChatDbo {
        return ChatDbo(
            id = ChatId(value.id),
            uuid = ChatUUID(value.uuid),
            name = value.name?.toChatName(),
            photo_url = value.photo_url?.toPhotoUrl(),
            type = value.type.toChatType(),
            status = value.status.toChatStatus(),
            contact_ids = value.contact_ids.map { ContactId(it) },
            is_muted = value.is_muted.toChatMuted(),
            created_at = value.created_at.toDateTime(),
            group_key = value.group_key?.toChatGroupKey(),
            host = value.host?.toChatHost(),
            price_per_message = value.price_per_message?.toSat(),
            escrow_amount = value.escrow_amount?.toSat(),
            unlisted = value.unlisted.toChatUnlisted(),
            private_tribe = value.private.toChatPrivate(),
            owner_pub_key = value.owner_pub_key?.toLightningNodePubKey(),
            seen = value.seen.toSeen(),
            meta_data = value.meta?.toChatMetaDataOrNull(moshi),
            my_photo_url = value.my_photo_url?.toPhotoUrl(),
            my_alias = value.my_alias?.toChatAlias(),
            pending_contact_ids = value.pending_contact_ids?.map { ContactId(it) },
            latest_message_id = null
        )
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun mapTo(value: ChatDbo): ChatDto {
        throw IllegalArgumentException("Going from a ChatDbo to ChatDto is not allowed")
    }
}