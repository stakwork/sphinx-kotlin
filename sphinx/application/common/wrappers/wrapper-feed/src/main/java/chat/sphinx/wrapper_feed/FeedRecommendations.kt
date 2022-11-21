package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey

data class FeedRecommendations (
    val pubKey: String,
    val feedType: String,
    val refId: String,
    val topics: List<String>,
    val weight: Float,
    val description: FeedDescription?,
    val date: Long,
    val title: FeedTitle,
    val boost: Long,
    val keyword: Any?,
    val imageUrl: PhotoUrl,
    val nodeType: String,
    val hosts: List<Hosts>,
    val guests: List<String>,
    val text: String,
    val timestamp: String,
    val episodeTitle: String,
    val guestProfiles: List<Any>,
    val link: String,
    ) {

    var chat: Chat? = null

    var imageUrlToShow: PhotoUrl? = null
        get() {
            imageUrl.let {
                return it
            }
        }
}

data class Hosts(
    val name: String,
    val twitterHandle: String,
    val profilePicture: String
)

