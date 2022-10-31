package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_common.message.MessageId
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PutPinMessageDto(
    val pin: String? = ""
)