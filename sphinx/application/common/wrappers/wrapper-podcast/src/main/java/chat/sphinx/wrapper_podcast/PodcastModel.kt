package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.FeedModelSuggested
import chat.sphinx.wrapper_feed.FeedModelType

data class PodcastModel(
    val type: FeedModelType,
    val suggested: FeedModelSuggested,
    val podcastId: FeedId,
)