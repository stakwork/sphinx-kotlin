package chat.sphinx.concept_network_query_lightning.model.webview

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayLsatDto(
    val lsat: String?,
)