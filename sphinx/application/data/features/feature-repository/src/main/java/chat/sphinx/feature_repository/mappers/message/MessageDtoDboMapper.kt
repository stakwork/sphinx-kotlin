package chat.sphinx.feature_repository.mappers.message

import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toLightningPaymentHash
import chat.sphinx.wrapper_common.lightning.toLightningPaymentRequest
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_common.toSeen
import chat.sphinx.wrapper_message.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class MessageDtoDboMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<Pair<MessageDto, MessageContentDecrypted?>, MessageDbo>(dispatchers) {

    override suspend fun mapFrom(value: Pair<MessageDto, MessageContentDecrypted?>): MessageDbo {
        return MessageDbo(
            id = MessageId(value.first.id),
            uuid = value.first.uuid?.toMessageUUID(),

            // Messages off the wire with MessageType.Repayment (code 18) have a bug where
            // the ChatID is null. Repayment types are never shown to the user from the
            // Chat screen, but are show from the Transactions Screen (which queries based off
            // of message Type).
            //
            // If that is the case, it is stored in the DB with a ChatID of Int.MAX_VALUE
            // in order to preserve the non null field for MessageDbo, and Message objects.
            //
            // It is highly doubtful that a user will reach 2147483647 chats; even if it is the
            // case (where it will be associated with an actual chat) MessageType.Repayment is
            // not displayed on the Chat screen.
            chat_id = value.first.chat_id?.let {
                ChatId(it)
            } ?: value.first.chat?.id?.let {
                ChatId(it)
            } ?: ChatId(Int.MAX_VALUE.toLong()),

            type = value.first.type.toMessageType(),
            sender = ContactId(value.first.sender),
            receiver = value.first.receiver?.let { ContactId(it) },
            amount = Sat(value.first.amount),
            payment_hash = value.first.payment_hash?.toLightningPaymentHash(),
            payment_request = value.first.payment_request?.toLightningPaymentRequest(),
            date = value.first.date.toDateTime(),
            expiration_date = value.first.expiration_date?.toDateTime(),
            message_content = value.first.message_content?.toMessageContent(),

            // Messages coming off the wire are decrypted if the Key is available (User
            // is logged in). If that is not the case (Key is not available), the message
            // is decrypted will be decrypted when going from a MessageDbo to a Message
            message_content_decrypted = value.second,

            status = value.first.status.toMessageStatus(),
            status_map = value.first.status_map?.toMessageStatusMap()?.toList(),
            media_key = value.first.mediaKey?.toMediaKey(),
            media_type = value.first.mediaType?.toMediaType(),
            media_token = value.first.mediaToken?.toMediaToken(),
            seen = value.first.seen.toSeen(),
            sender_alias = value.first.sender_alias?.toSenderAlias(),
            sender_pic = value.first.sender_pic?.toPhotoUrl(),
            original_muid = value.first.original_muid?.toMessageMUID(),
            reply_uuid = value.first.reply_uuid?.toReplyUUID()
        )
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun mapTo(value: MessageDbo): Pair<MessageDto, MessageContentDecrypted?> {
        throw IllegalArgumentException("Going from a ChatDbo to ChatDto is not allowed")
    }
}