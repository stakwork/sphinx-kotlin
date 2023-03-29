package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.FeedDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_common.feed.FeedType
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class FeedDboPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<FeedDbo, Feed>(dispatchers) {
    override suspend fun mapFrom(value: FeedDbo): Feed {
        return Feed(
            value.id,
            value.feed_type,
            value.title,
            value.description,
            value.feed_url,
            value.author,
            value.generator,
            value.image_url,
            value.owner_url,
            value.link,
            value.date_published,
            value.date_updated,
            value.content_type,
            value.language,
            value.items_count,
            value.current_item_id,
            value.chat_id,
            value.subscribed,
            value.last_played
        )
    }

    override suspend fun mapTo(value: Feed): FeedDbo {
        return FeedDbo(
            value.id,
            value.feedType,
            value.title,
            value.description,
            value.feedUrl,
            value.author,
            value.generator,
            value.imageUrl,
            value.ownerUrl,
            value.link,
            value.datePublished,
            value.dateUpdated,
            value.contentType,
            value.language,
            value.itemsCount,
            value.currentItemId,
            value.chatId,
            value.subscribed,
            value.lastPlayed
        )
    }
}