package chat.sphinx.concept_repository_feed

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedDescription
import chat.sphinx.wrapper_feed.FeedItem
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
        searchResultDescription: FeedDescription? = null,
        searchResultImageUrl: PhotoUrl? = null,
        chatUUID: ChatUUID?,
        subscribed: Subscribed,
        currentEpisodeId: FeedId?
    ): Response<FeedId, ResponseError>

    fun getFeedByChatId(chatId: ChatId): Flow<Feed?>
    fun getFeedById(feedId: FeedId): Flow<Feed?>
    fun getFeedItemById(feedItemId: FeedId): Flow<FeedItem?>

    suspend fun toggleFeedSubscribeState(feedId: FeedId, currentSubscribeState: Subscribed)
}