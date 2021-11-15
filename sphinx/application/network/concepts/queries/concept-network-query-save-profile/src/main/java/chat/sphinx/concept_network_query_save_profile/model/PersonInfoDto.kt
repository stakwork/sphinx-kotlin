package chat.sphinx.concept_network_query_save_profile.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PersonInfoDto(
    val id: Int,
    val host: String,
    val owner_alias: String,
    val description: String,
    val img: String,
    val tag: String?,
    val price_to_meet: Int,
    val extras: Extras,
)

@JsonClass(generateAdapter = true)
data class Extras(
    val tribes: List<String>
)