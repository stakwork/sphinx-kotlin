package chat.sphinx.concept_repository_actions

import chat.sphinx.wrapper_action_track.action_wrappers.ContentConsumedHistoryItem
import chat.sphinx.wrapper_common.feed.FeedId
import kotlinx.coroutines.flow.MutableStateFlow

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

    fun trackMediaContentConsumed(
        feedItemId: FeedId,
        history: ArrayList<ContentConsumedHistoryItem>
    )

    fun trackRecommendationsConsumed(
        feedItemId: FeedId,
        history: ArrayList<ContentConsumedHistoryItem>
    )

    fun trackMessageContent(
        keywords: List<String>
    )

    fun syncActions()

    val appLogsStateFlow: MutableStateFlow<String>
    fun setAppLog(log: String)
}
