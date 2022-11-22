package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.PhotoUrl

data class FeedRecommendation(
    val id: String,
    val feedType: String,
    val description: String,
    val imageUrl: PhotoUrl,
    val link: String,
    val title: String
)




