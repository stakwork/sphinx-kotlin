package chat.sphinx.concept_network_query_save_profile.model

import com.squareup.moshi.JsonClass

@Suppress("NOTHING_TO_INLINE")
inline fun GetPeopleProfileDto.isProfilePath(): Boolean =
    path == "profile"

@Suppress("NOTHING_TO_INLINE")
inline fun GetPeopleProfileDto.isSaveMethod(): Boolean =
    method == "POST"

@Suppress("NOTHING_TO_INLINE")
inline fun GetPeopleProfileDto.isDeleteMethod(): Boolean =
    method == "DELETE"

@JsonClass(generateAdapter = true)
data class GetPeopleProfileDto(
    val key: String,
    val body: String,
    val path: String,
    val method: String,
)