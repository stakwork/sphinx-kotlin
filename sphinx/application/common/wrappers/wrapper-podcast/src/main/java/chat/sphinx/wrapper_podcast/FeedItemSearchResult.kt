package chat.sphinx.wrapper_podcast

data class FeedItemSearchResult(
    val id: String,
    val title: String,
    val description: String,
    val datePublished: Long,
    val dateUpdated: Long,
    val author: String,
    val enclosureUrl: String,
    val enclosureType: String,
    val duration: Long,
    val imageUrl: String,
    val thumbnailUrl: String,
    val link: String,
    val feedId: String,
    val feedType: Long,
    val url: String
)

