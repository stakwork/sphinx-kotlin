package chat.sphinx.feature_repository.mappers.feed.podcast

import chat.sphinx.conceptcoredb.FeedDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.FeedSearchResult
import io.matthewnelson.concept_coroutines.CoroutineDispatchers


internal class FeedDboFeedSearchResultPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<FeedDbo, FeedSearchResult>(dispatchers) {
    override suspend fun mapFrom(value: FeedDbo): FeedSearchResult {
        return FeedSearchResult(
            value.id.value,
            value.feed_type.value.toLong(),
            value.title.value,
            value.feed_url.value,
            value.description?.value,
            value.author?.value,
            value.generator?.value,
            value.image_url?.value,
            value.owner_url?.value,
            value.link?.value,
            value.date_published?.time,
            value.date_published?.time,
            value.content_type?.value,
            value.language?.value,
            value.chat_id.value,
        )
    }

    override suspend fun mapTo(value: FeedSearchResult): FeedDbo {
        return FeedDbo(
            id = FeedId(value.id),
            feed_type = FeedType.Podcast,
            title = FeedTitle(value.title),
            description = FeedDescription(value.description ?: ""),
            feed_url = FeedUrl(value.url),
            author = FeedAuthor(value.author ?: ""),
            generator = FeedGenerator(value.generator ?: ""),
            image_url = PhotoUrl(value.imageUrl ?: ""),
            owner_url = FeedUrl(value.ownerUrl ?: ""),
            link = FeedUrl(value.link ?: ""),
            date_published = value.datePublished?.toDateTime(),
            date_updated = value.dateUpdated?.toDateTime(),
            content_type = FeedContentType(value.contentType ?: ""),
            language = FeedLanguage(value.language ?: ""),
            items_count = FeedItemsCount(0),
            current_item_id = null,
            chat_id = ChatId(ChatId.NULL_CHAT_ID.toLong()),
            subscribed = false.toSubscribed(),
            last_played = null
        )
    }
}