package chat.sphinx.concept_network_query_message.model

import chat.sphinx.concept_network_query_chat.model.ChatDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PutMemberResponseDto(
    val chat: ChatDto,
    val message: MessageDto,
)