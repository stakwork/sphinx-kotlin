package chat.sphinx.concept_repository_podcast

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastSearchResultRow
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    fun getPodcastByChatId(chatId: ChatId): Flow<Podcast?>
    fun getPodcastById(feedId: FeedId): Flow<Podcast?>
    fun searchPodcastBy(searchTerm: String): Flow<List<PodcastSearchResultRow>>
}