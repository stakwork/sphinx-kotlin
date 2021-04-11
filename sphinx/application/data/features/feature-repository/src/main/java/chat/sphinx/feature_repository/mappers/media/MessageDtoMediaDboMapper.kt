package chat.sphinx.feature_repository.mappers.media

import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.conceptcoredb.MessageMediaDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.feature_repository.mappers.message.MessageDtoDboMapper
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_message.media.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class MessageDtoMediaDboMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<MessageDto, MessageMediaDbo?>(dispatchers) {
    override suspend fun mapFrom(value: MessageDto): MessageMediaDbo? {
        return value.media_token?.let { token ->
            value.media_type?.let { type ->
                if (token.isEmpty() || type.isEmpty()) return null

                MessageMediaDbo(
                    MessageId(value.id),
                    ChatId(value.chat_id ?: MessageDtoDboMapper.NULL_CHAT_ID.toLong()),
                    value.media_key?.toMediaKey(),
                    value.mediaKeyDecrypted?.toMediaKeyDecrypted(),
                    MediaType(type),
                    MediaToken(token)
                )
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun mapTo(value: MessageMediaDbo?): MessageDto {
        throw IllegalArgumentException("Going from a MessageMediaDbo to MessageDto is not allowed")
    }
}