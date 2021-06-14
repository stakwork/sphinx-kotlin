package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PutTribeDto(
    val name: String,
    val img: String = ""
)
