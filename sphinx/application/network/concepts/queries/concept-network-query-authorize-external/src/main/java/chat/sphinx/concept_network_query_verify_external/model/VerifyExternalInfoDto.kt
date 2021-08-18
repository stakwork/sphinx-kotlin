package chat.sphinx.concept_network_query_verify_external.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerifyExternalInfoDto(
    val price_to_meet: Long?,
    val jwt: String?,
    val photo_url: String?,
    val contact_key: String?,
    val route_hint: String?,
    val pubkey: String?,
    val alias: String?,
) {

    var url: String? = null

    @Json(name = "verification_signature")
    var verificationSignature: String? = null
}