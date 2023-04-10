package chat.sphinx.episode_description.model

import chat.sphinx.wrapper_common.feed.FeedType

data class FeedItemDescription(
    val feedItemTitle: String,
    val feedTitle: String?,
    val feedType: FeedType?,
    val itemDate: String,
    val itemDuration: String,
    val downloaded: Boolean,
    var downloading: Boolean,
    val played: Boolean,
    var playing: Boolean,
    val link: String?,
    val description: String,
    val descriptionExpanded: Boolean,
    val image: String,
    val isRecommendation: Boolean,
    val headerVisible: Boolean
)
