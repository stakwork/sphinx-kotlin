package chat.sphinx.concept_repository_actions

import chat.sphinx.wrapper_common.feed.FeedId
import kotlinx.coroutines.flow.Flow

interface ActionsRepository {

    suspend fun trackFeedBoostAction(
        boost: Long,
        feedItemId: FeedId,
        topics: ArrayList<String>
    )
    suspend fun trackFeedSearchAction(searchTerm: String)
    suspend fun trackPodcastClipComments(
        feedItemId: FeedId,
        timestamp: Long,
        topics: ArrayList<String>
    )

}
