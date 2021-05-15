package chat.sphinx.feature_repository.mappers.message

import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_message.*
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.extensions.decodeToString
import kotlinx.coroutines.withContext
import okio.base64.decodeBase64ToArray

internal class MessageDboPresenterMapper(
    dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
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
            status = value.status,
            seen = value.seen,
            senderAlias = value.sender_alias,
            senderPic = value.sender_pic,
            originalMUID = value.original_muid,
            replyUUID = value.reply_uuid
        ).also { message ->
            value.message_content_decrypted?.let { decrypted ->
                if (message.type.isMessage()) {
                    when {
                        decrypted.value.contains("boost::") -> {
                            withContext(default) {
                                decrypted.value.split("::")
                                    .elementAtOrNull(1)
                                    ?.toPodBoostOrNull(moshi)
                                    ?.let { podBoost ->
                                        message.setPodBoost(podBoost)
                                    }
                            }
                        }
                        decrypted.value.contains("giphy::") -> {
                            withContext(default) {
                                decrypted.value.split("::")
                                    .elementAtOrNull(1)
                                    ?.decodeBase64ToArray()
                                    ?.decodeToString()
                                    ?.toGiphyDataOrNull(moshi)
                                    ?.let { giphy ->
                                        message.setGiphyData(giphy)
                                    }
                            }
                        }
                    }
                }

                message.setMessageContentDecrypted(decrypted)
            }
        }
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
            seen = value.seen,
            sender_alias = value.senderAlias,
            sender_pic = value.senderPic,
            original_muid = value.originalMUID,
            reply_uuid = value.replyUUID
        )
    }
}