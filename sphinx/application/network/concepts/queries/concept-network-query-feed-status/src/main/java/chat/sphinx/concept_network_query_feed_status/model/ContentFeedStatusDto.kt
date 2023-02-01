package chat.sphinx.concept_network_query_feed_status.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentFeedStatusDto(
    val feed_id: String,
    val feed_url: String,
    val subscription_status: Boolean,
    val chat_id: Long,
    val item_id: String,
    val episodes_status: List<EpisodeIdDto>,
    val sats_per_minute: Int,
    val player_speed: Double
)

@JsonClass(generateAdapter = true)
data class EpisodeIdDto(
    val duration: Int,
    val current_time: Int
)