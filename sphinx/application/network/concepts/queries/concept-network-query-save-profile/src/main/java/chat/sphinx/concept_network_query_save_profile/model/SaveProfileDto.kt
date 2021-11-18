package chat.sphinx.concept_network_query_save_profile.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SaveProfileDto(
    val key: String,
    val body: String,
    val path: String,
    val method: String,
)