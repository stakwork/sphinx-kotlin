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
): ClassMapper<MessageDto, MessageDbo>(dispatchers) {

    companion object {
        const val NULL_CHAT_ID: Int = Int.MAX_VALUE
    }

    override suspend fun mapFrom(value: MessageDto): MessageDbo {

        // This handles the old method for sending boost payments (they were sent as
        // type 0 [MESSAGE]). Will update the MessageType to the correct value and
        // store the feed data properly for display.
        var type: MessageType = value.type.toMessageType()
        val decryptedContent: String? = value.messageContentDecrypted?.let { decrypted ->
            if (decrypted.contains("boost::{\"feedID\":")) {
                type = MessageType.Boost
                decrypted.split("::")[1]
            } else {
                decrypted
            }
        }

        return MessageDbo(
            id = MessageId(value.id),
            uuid = value.uuid?.toMessageUUID(),

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
            chat_id = value.chat_id?.let {
                ChatId(it)
            } ?: value.chat?.id?.let {
                ChatId(it)
            } ?: ChatId(NULL_CHAT_ID.toLong()),

            type = type,
            sender = ContactId(value.sender),
            receiver = value.receiver?.let { ContactId(it) },
            amount = Sat(value.amount),
            payment_hash = value.payment_hash?.toLightningPaymentHash(),
            payment_request = value.payment_request?.toLightningPaymentRequest(),
            date = value.date.toDateTime(),
            expiration_date = value.expiration_date?.toDateTime(),
            message_content = value.message_content?.toMessageContent(),

            // Messages coming off the wire are decrypted if the Key is available (User
            // is logged in). If that is not the case (Key is not available), the message
            // is decrypted will be decrypted when going from a MessageDbo to a Message
            message_content_decrypted = decryptedContent?.toMessageContentDecrypted(),

            status = value.status.toMessageStatus(),
            seen = value.seen.toSeen(),
            sender_alias = value.sender_alias?.toSenderAlias(),
            sender_pic = value.sender_pic?.toPhotoUrl(),
            original_muid = value.original_muid?.toMessageMUID(),
            reply_uuid = value.reply_uuid?.toReplyUUID()
        )
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun mapTo(value: MessageDbo): MessageDto {
        throw IllegalArgumentException("Going from a ChatDbo to ChatDto is not allowed")
    }
}