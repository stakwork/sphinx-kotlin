package chat.sphinx.concept_network_query_contact.model

import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostContactDto(
    val alias: String,
    val public_key: String,
    val route_hint: String?,
    val status: Int
)
