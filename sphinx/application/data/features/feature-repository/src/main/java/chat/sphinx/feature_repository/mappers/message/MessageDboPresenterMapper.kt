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
            id = value.id,
            uuid = value.uuid,
            chatId = value.chat_id,
            type = value.type,
            sender = value.sender,
            receiver = value.receiver,
            amount = value.amount,
            paymentHash = value.payment_hash,
            paymentRequest = value.payment_request,
            date = value.date,
            expirationDate = value.expiration_date,
            messageContent = value.message_content,
            messageContentDecrypted = value.message_content_decrypted,
            status = value.status,
            statusMap = value.status_map?.toMap(),
            mediaKey = value.media_key,
            mediaType = value.media_type,
            mediaToken = value.media_token,
            seen = value.seen,
            senderAlias = value.sender_alias,
            senderPic = value.sender_pic,
            originalMUID = value.original_muid,
            replyUUID = value.reply_uuid
        )
    }

    override suspend fun mapTo(value: Message): MessageDbo {
        return MessageDbo(
            id = value.id,
            uuid = value.uuid,
            chat_id = value.chatId,
            type = value.type,
            sender = value.sender,
            receiver = value.receiver,
            amount = value.amount,
            payment_hash = value.paymentHash,
            payment_request = value.paymentRequest,
            date = value.date,
            expiration_date = value.expirationDate,
            message_content = value.messageContent,
            message_content_decrypted = value.messageContentDecrypted,
            status = value.status,
            status_map = value.statusMap?.toList(),
            media_key = value.mediaKey,
            media_type = value.mediaType,
            media_token = value.mediaToken,
            seen = value.seen,
            sender_alias = value.senderAlias,
            sender_pic = value.senderPic,
            original_muid = value.originalMUID,
            reply_uuid = value.replyUUID
        )
    }
}