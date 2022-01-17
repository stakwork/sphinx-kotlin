package chat.sphinx.concept_network_query_message.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetMessagesResponse(
    val new_messages: List<MessageDto>,
    val confirmed_messages: List<MessageDto>?,
    val new_messages_total: Int?,
)
