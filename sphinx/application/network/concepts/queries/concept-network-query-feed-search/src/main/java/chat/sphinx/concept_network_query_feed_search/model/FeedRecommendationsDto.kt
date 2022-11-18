package chat.sphinx.concept_network_query_feed_search.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedRecommendationsDto(
    val pub_key: String,
    val type: String,
    val ref_id: String,
    val topics: List<String>,
    val weight: Int,
    val description: String,
    val date: Long,
    val show_title: String,
    val boost: Long,
    val keyword: Boolean,
    val image_url: String,
    val node_type: String,
    val hosts: List<Hosts>,
    val guests: List<String>,
    val text: String,
    val timestamp: String,
    val episode_title: String,
    val guest_profiles: List<Any>,
    val link: String,
)

@JsonClass(generateAdapter = true)
data class Hosts(
    val name: String,
    val twitter_handle: String,
    val profile_picture: String
)