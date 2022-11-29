package chat.sphinx.concept_repository_actions

import chat.sphinx.wrapper_action_track.action_wrappers.ContentConsumedHistoryItem
import chat.sphinx.wrapper_common.feed.FeedId

interface ActionsRepository {

    fun trackFeedBoostAction(
        boost: Long,
        feedItemId: FeedId,
        topics: ArrayList<String>
    )

    fun trackFeedSearchAction(searchTerm: String)

    fun trackPodcastClipComments(
        feedItemId: FeedId,
        timestamp: Long,
        topics: ArrayList<String>
    )

    fun trackNewsletterConsumed(
        feedItemId: FeedId
    )

    fun trackVideoConsumed(
        feedItemId: FeedId,
        history: java.util.ArrayList<ContentConsumedHistoryItem>
    )

    fun trackPodcastConsumed(
        feedItemId: FeedId,
        history: java.util.ArrayList<ContentConsumedHistoryItem>
    )

    fun trackMessageContent(
        keywords: List<String>
    )

    fun syncActions()
}
