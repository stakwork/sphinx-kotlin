package chat.sphinx.feature_network_query_message.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostPayAttachmentDto(
    val chat_id: Long,
    val contact_id: Long?,
    val amount: Long,
    val media_token: String,
)