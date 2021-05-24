package chat.sphinx.concept_network_query_message.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostMessageDto(
    val chat_id: Long?,
    val contact_id: Long?,
    val amount: Long,
    val reply_uuid: String?,
    val text: String,
    val remote_text_map: Map<String, String>
) {
    init {
        require(!(chat_id == null && contact_id == null)) {
            "A chat_id and/or contact_id is required"
        }

        require(remote_text_map.isNotEmpty()) {
            "remote_text_map cannot be empty"
        }

        require(text.isNotEmpty()) {
            "text cannot be empty"
        }
    }
}