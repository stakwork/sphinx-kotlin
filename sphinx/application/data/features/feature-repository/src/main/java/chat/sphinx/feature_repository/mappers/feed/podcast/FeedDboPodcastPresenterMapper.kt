package chat.sphinx.feature_repository.mappers.feed.podcast

import chat.sphinx.conceptcoredb.FeedDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.Podcast
import io.matthewnelson.concept_coroutines.CoroutineDispatchers


internal class FeedDboPodcastPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<FeedDbo, Podcast>(dispatchers) {
    override suspend fun mapFrom(value: FeedDbo): Podcast {
        return Podcast(
            id = value.id,
            title = value.title,
            description = value.description,
            author = value.author,
            image = value.image_url,
            datePublished = value.date_published,
            chatId = value.chat_id,
            feedUrl = value.feed_url,
            subscribed = value.subscribed,
        )
    }

    override suspend fun mapTo(value: Podcast): FeedDbo {
        return FeedDbo(
            id = value.id,
            feed_type = FeedType.Podcast,
            title = value.title,
            description = value.description,
            feed_url = value.feedUrl,
            author = value.author,
            generator = null,
            image_url = value.image,
            owner_url = null,
            link = null,
            date_published = value.datePublished,
            date_updated = null,
            content_type = null,
            language = null,
            items_count = FeedItemsCount(value.episodes.count().toLong()),
            current_item_id = value.episodeId?.toFeedId(),
            chat_id = value.chatId,
            subscribed = value.subscribed,
            last_played = null
        )
    }
}