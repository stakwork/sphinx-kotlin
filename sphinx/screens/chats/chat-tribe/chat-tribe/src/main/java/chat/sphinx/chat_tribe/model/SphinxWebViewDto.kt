package chat.sphinx.chat_tribe.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SphinxWebViewDto(
    val application: String,
    val type: String
)