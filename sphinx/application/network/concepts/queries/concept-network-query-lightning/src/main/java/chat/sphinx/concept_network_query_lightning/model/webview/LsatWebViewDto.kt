package chat.sphinx.concept_network_query_lightning.model.webview

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LsatWebViewDto(
    val paymentRequest: String?,
    val macaroon: String?,
    val issuer: String?
)