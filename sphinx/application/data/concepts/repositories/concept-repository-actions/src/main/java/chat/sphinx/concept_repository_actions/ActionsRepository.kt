package chat.sphinx.concept_repository_actions

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

}
