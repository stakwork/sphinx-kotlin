package chat.sphinx.concept_network_query_crypter.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CrypterPublicKeyResultDto(
    val pubkey: String
)