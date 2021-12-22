package chat.sphinx.concept_repository_feed

import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.FeedSearchResultRow
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getPodcastByChatId(chatId: ChatId): Flow<Podcast?>
    fun getPodcastById(feedId: FeedId): Flow<Podcast?>

    fun searchFeedsBy(
        searchTerm: String,
        feedType: FeedType?,
    ): Flow<List<FeedSearchResultRow>>

    suspend fun updateFeedContent(
        chatId: ChatId,
        host: ChatHost,
        feedUrl: FeedUrl,
        chatUUID: ChatUUID?,
        subscribed: Subscribed,
        currentEpisodeId: FeedId?
    )

    fun getFeedByChatId(chatId: ChatId): Flow<Feed?>
    fun getFeedById(feedId: FeedId): Flow<Feed?>

    suspend fun toggleFeedSubscribeState(feedId: FeedId, currentSubscribeState: Subscribed)
}