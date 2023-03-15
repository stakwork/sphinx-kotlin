package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.feed.FeedId

data class FeedItemDetail(
    val feedId: FeedId?,
    val header: String,
    val image: String,
    val episodeTypeImage: Int,
    val episodeTypeText: String,
    val episodeDate: String,
    val episodeDuration: String,
    val downloaded: Boolean?,
    var isDownloadInProgress: Boolean?,
    val link: String?,
    val played: Boolean,
    val podcastName: String?
)