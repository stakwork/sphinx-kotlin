package chat.sphinx.concept_network_query_people.model

import com.squareup.moshi.JsonClass

@Suppress("NOTHING_TO_INLINE")
inline fun GetExternalRequestDto.isProfilePath(): Boolean =
    path == "profile"

@Suppress("NOTHING_TO_INLINE")
inline fun GetExternalRequestDto.isSaveMethod(): Boolean =
    method == "POST"

@Suppress("NOTHING_TO_INLINE")
inline fun GetExternalRequestDto.isDeleteMethod(): Boolean =
    method == "DELETE"

@Suppress("NOTHING_TO_INLINE")
inline fun GetExternalRequestDto.isClaimOnLiquidPath(): Boolean =
    path == "claim_on_liquid"

@JsonClass(generateAdapter = true)
data class GetExternalRequestDto(
    val key: String,
    val body: String,
    val path: String,
    val method: String,
)
