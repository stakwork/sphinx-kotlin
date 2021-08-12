package chat.sphinx.concept_network_query_version.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppVersionsDto(
    val ios: Long,
    val android: Long,
    val windows: Long,
    val linux: Long,
    val mac: Long,
    val kotlin: Long,
)