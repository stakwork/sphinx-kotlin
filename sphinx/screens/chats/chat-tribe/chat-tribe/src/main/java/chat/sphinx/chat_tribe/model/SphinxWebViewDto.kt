package chat.sphinx.chat_tribe.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SphinxWebViewDto(
    val application: String,
    val type: String,
    val challenge: String?,
    val paymentRequest: String?,
    val macaroon: String?,
    val issuer: String?,
    val dest: String?,
    val amt: Int?

) {
    companion object {
        const val APPLICATION_NAME = "Sphinx"

        const val TYPE_AUTHORIZE = "AUTHORIZE"
        const val TYPE_LSAT = "LSAT"
        const val TYPE_KEYSEND = "KEYSEND"
    }
}