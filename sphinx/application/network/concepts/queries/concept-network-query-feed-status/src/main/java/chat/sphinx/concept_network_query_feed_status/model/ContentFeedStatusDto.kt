package chat.sphinx.concept_network_query_feed_status.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentFeedStatusDto(
    val feed_id: String,
    val feed_url: String,
    val subscription_status: Boolean,
    val chat_id: Long?,
    val item_id: String?,
    val sats_per_minute: Long?,
    val player_speed: Double?,
    val episodes_status: List<Map<String, EpisodeStatusDto>>?,
)

@JsonClass(generateAdapter = true)
data class EpisodeStatusDto(
    val duration: Long,
    val current_time: Long
)