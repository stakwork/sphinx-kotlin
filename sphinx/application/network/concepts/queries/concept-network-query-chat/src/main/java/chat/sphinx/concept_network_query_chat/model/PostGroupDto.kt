package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostGroupDto(
    val name: String,
    val description: String,
    val is_tribe: Boolean? = true,
    val price_per_message: Long? = 0L,
    val price_to_join: Long? = 0L,
    val escrow_amount: Long? = 0L,
    val escrow_millis: Long? = 0L,
    val img: String? = null,
    val tags: Array<String> = arrayOf(),
    val unlisted: Boolean? = false,
    val private: Boolean? = false,
    val app_url: String? = null,
    val feed_url: String? = null,
    val feed_type: Long? = 0L,
)
