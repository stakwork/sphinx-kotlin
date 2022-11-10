package chat.sphinx.concept_network_query_people.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PeopleProfileDto(
    val id: Int,
    val host: String,
    val owner_alias: String,
    val description: String,
    val img: String,
    val tags: List<String>?,
    val price_to_meet: Int,
    val extras: Any,
)