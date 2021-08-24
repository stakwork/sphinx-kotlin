package chat.sphinx.concept_network_query_verify_external.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PersonInfoDto(
    val img: String?,
    val owner_alias: String?,
    val description: String?,
    val price_to_meet: Long?
)