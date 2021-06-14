package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PutChatDto(
    val my_alias: String? = null,
    val my_photo_url: String? = null,
    val meta: String? = null,
)
