package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class TribeMemberDto(
    val chat_id: Long,
    val alias: String,
    val photo_url: String? = null,
    val pub_key: String,
    val route_hint: String? = null,
    val contact_key: String,
)
