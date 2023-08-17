package chat.sphinx.concept_network_query_people.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KnownBadgesPostDto(
    val ids: List<Long>,
)