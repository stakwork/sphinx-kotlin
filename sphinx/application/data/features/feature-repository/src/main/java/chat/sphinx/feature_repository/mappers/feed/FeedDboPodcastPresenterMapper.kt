package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.FeedDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
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
            feedUrl = value.feed_url
        )
    }

    override suspend fun mapTo(value: Podcast): FeedDbo {
        return FeedDbo(
            value.id,
            FeedType.Podcast,
            value.title,
            value.description,
            value.feedUrl,
            value.author,
            null,
            value.image,
            null,
            null,
            value.datePublished,
            null,
            null,
            null,
            value.chatId
        )
    }
}