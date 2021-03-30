package chat.sphinx.feature_repository.mappers.message

import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.feature_repository.SphinxRepository
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_message.Message
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class MessageDboPresenterMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<MessageDbo, Message>(dispatchers) {
    override suspend fun mapFrom(value: MessageDbo): Message {
        return Message(
            value.id,
            value.uuid,
            value.chat_id,
            value.type,
            value.sender,
            value.receiver,
            value.amount,
            value.payment_hash,
            value.payment_request,
            value.date,
            value.expiration_date,
            value.message_content,
            value.message_content_decrypted,
            value.status,
            value.status_map?.toMap(),
            value.media_key,
            value.media_type,
            value.media_token,
            value.seen,
            value.sender_alias,
            value.sender_pic,
            value.original_muid,
            value.reply_uuid
        )
    }

    override suspend fun mapTo(value: Message): MessageDbo {
        return MessageDbo(
            value.id,
            value.uuid,
            value.chatId,
            value.type,
            value.sender,
            value.receiver,
            value.amount,
            value.paymentHash,
            value.paymentRequest,
            value.date,
            value.expirationDate,
            value.messageContent,
            value.messageContentDecrypted,
            value.status,
            value.statusMap?.toList(),
            value.mediaKey,
            value.mediaType,
            value.mediaToken,
            value.seen,
            value.senderAlias,
            value.senderPic,
            value.originalMUID,
            value.replyUUID
        )
    }
}